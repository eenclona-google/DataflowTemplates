/*
 * Copyright (C) 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.teleport.v2.templates;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import com.google.cloud.teleport.metadata.Template;
import com.google.cloud.teleport.metadata.TemplateCategory;
import com.google.cloud.teleport.metadata.TemplateParameter;
import com.google.cloud.teleport.v2.templates.BigQueryToBigtable.BigQueryToBigtableOptions;
import com.google.cloud.teleport.v2.transforms.BigQueryConverters.AvroToMutation;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;

/**
 * Dataflow template which reads BigQuery data and writes it to AlloyDB. The source data can be
 * either a BigQuery table or an SQL query.
 */
@Template(
    name = "BigQuery_to_AlloyDB",
    category = TemplateCategory.BATCH,
    displayName = "BigQuery to AlloyDB",
    description = "A pipeline to export a BigQuery table into AlloyDB.",
    optionsClass = BigQueryToAlloyDBOptions.class,
    flexContainerName = "bigquery-to-alloydb",
    contactInformation = "https://cloud.google.com/support")
public class BigQueryToAlloyDB {

  /**
   * The {@link BigQueryToAlloyDBOptions} class provides the custom execution options passed by the
   * executor at the command-line.
   */
  public interface BigQueryToAlloyDBOptions extends PipelineOptions {

    @TemplateParameter.Text(
        order = 1,
        description = "Input SQL query",
        helpText = "SQL query in standard SQL to pull data from BigQuery")
    String getReadQuery();

    void setReadQuery(String value);

    @TemplateParameter.Text(
        order = 2,
        regexes = {"[A-Za-z_][A-Za-z_0-9]*"},
        description = "Unique identifier column",
        helpText = "Name of the BigQuery column storing the unique identifier of the row")
    String getReadIdColumn();

    void setReadIdColumn(String value);

    @TemplateParameter.ProjectId(
        order = 3,
        description = "Project ID",
        helpText =
            "The ID of the Google Cloud project of the Cloud AlloyDB instance that you want to write data to")
    @Required
    String getAlloyDBWriteProjectId();

    void setAlloyDBWriteProjectId(String value);

    @TemplateParameter.Text(
        order = 4,
        regexes = {"[a-z][a-z0-9\\-]+[a-z0-9]"},
        description = "Instance ID",
        helpText = "The ID of the Cloud AlloyDB instance that contains the table")
    @Required
    String getAlloyDBWriteInstanceId();

    void setAlloyDBWriteInstanceId(String value);

    /*
    TODO: Look at parameters included in python version and add them here.
    include parameters needed for JDBC connection.
    */
  }

  /**
   * Runs a pipeline which reads data from BigQuery and writes it to AlloyDB.
   *
   * @param args arguments to the pipeline
   */
  public static void main(String[] args) {

    BigQueryToAlloyDBOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigQueryToAlloyDBOptions.class);
    
    // CloudAlloyDBTableConfiguration AlloyDBTableConfig =
    //     new CloudAlloyDBTableConfiguration.Builder()
    //         .withProjectId(options.getAlloyDBWriteProjectId())
    //         .withInstanceId(options.getAlloyDBWriteInstanceId())
    //         .withAppProfileId(options.getAlloyDBWriteAppProfile())
    //         .withTableId(options.getAlloyDBWriteTableId())
    //         .build();

    DynamicJdbcIO.DynamicDataSourceConfiguration dataSourceConfiguration =
    DynamicJdbcIO.DynamicDataSourceConfiguration.create(
            options.getDriverClassName(),
            maybeDecrypt(options.getConnectionUrl(), options.getKMSEncryptionKey()))
        .withDriverJars(options.getDriverJars());

    if (options.getUsername() != null) {
      dataSourceConfiguration =
          dataSourceConfiguration.withUsername(
              maybeDecrypt(options.getUsername(), options.getKMSEncryptionKey()));
    }
    if (options.getPassword() != null) {
      dataSourceConfiguration =
          dataSourceConfiguration.withPassword(
              maybeDecrypt(options.getPassword(), options.getKMSEncryptionKey()));
    }
    if (options.getConnectionProperties() != null) {
      dataSourceConfiguration =
          dataSourceConfiguration.withConnectionProperties(options.getConnectionProperties());
    }
    
    "writeToJdbc",
                DynamicJdbcIO.<String>write()
                    .withDataSourceConfiguration(dataSourceConfiguration)
                    .withStatement(options.getStatement())
                    .withPreparedStatementSetter(
                        new MapJsonStringToQuery(getKeyOrder(options.getStatement()))));

    JdbcIO.<KV<Integer, String>>write()
      .withDataSourceConfiguration(JdbcIO.DataSourceConfiguration.create(
            "com.mysql.jdbc.Driver", "jdbc:mysql://hostname:3306/mydb")
          .withUsername("username")
          .withPassword("password"))
      .withStatement("insert into Person values(?, ?)")
      .withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<KV<Integer, String>>() {
        public void setParameters(KV<Integer, String> element, PreparedStatement query)
          throws SQLException {
          query.setInt(1, kv.getKey());
          query.setString(2, kv.getValue());
        }
      }

    Pipeline pipeline = Pipeline.create(options);

    pipeline
        .apply(
            "AvroToMutation",
            BigQueryIO.read(
                    AvroToMutation.newBuilder()
                        .setColumnFamily(options.getAlloyDBWriteColumnFamily())
                        .setRowkey(options.getReadIdColumn())
                        .build())
                .fromQuery(options.getReadQuery())
                .withoutValidation()
                .withTemplateCompatibility()
                .usingStandardSql())
        .apply("WriteToTable", CloudAlloyDBIO.writeToTable(AlloyDBTableConfig));

    pipeline.run();
  }
}
