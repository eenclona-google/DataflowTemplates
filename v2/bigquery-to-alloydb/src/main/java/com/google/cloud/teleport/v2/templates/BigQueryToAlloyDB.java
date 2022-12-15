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


import com.google.cloud.teleport.metadata.Template;
import com.google.cloud.teleport.metadata.TemplateCategory;
import com.google.cloud.teleport.metadata.TemplateParameter;
import com.google.cloud.teleport.v2.transforms.BigQueryConverters.AvroToMutation;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
//own imports
import org.apache.beam.sdk.io.TextIO;
import com.google.cloud.teleport.v2.transforms.BigQueryConverters.ReadBigQuery;
import com.google.cloud.teleport.v2.transforms.BigQueryConverters.TableRowToJsonFn;
import com.google.cloud.teleport.v2.transforms.BigQueryConverters.BigQueryReadOptions;
import org.apache.beam.sdk.transforms.ParDo;
import com.google.cloud.teleport.v2.options.BigQueryToJdbcOptions;
import org.apache.beam.sdk.values.PCollection;
import com.google.cloud.teleport.v2.io.DynamicJdbcIO;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.google.cloud.teleport.v2.utils.KMSEncryptedNestedValue;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import com.google.cloud.teleport.v2.coders.FailsafeElementCoder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Splitter;
import java.sql.Types;
import org.apache.beam.sdk.coders.StringUtf8Coder;


/**
 * Dataflow template which reads BigQuery data and writes it to AlloyDB. The source data can be
 * either a BigQuery table or an SQL query.
 */
@Template(
    name = "BigQuery_to_AlloyDB",
    category = TemplateCategory.BATCH,
    displayName = "BigQuery to AlloyDB",
    description = "A pipeline to export a BigQuery table into AlloyDB.",
    optionsClass = BigQueryToJdbcOptions.class,
    flexContainerName = "bigquery-to-alloydb",
    contactInformation = "https://cloud.google.com/support")
public class BigQueryToAlloyDB {

  /* Logger for class.*/
  private static final Logger LOG = LoggerFactory.getLogger(BigQueryToAlloyDB.class);

  public static final FailsafeElementCoder<String, String> FAILSAFE_ELEMENT_CODER =
      FailsafeElementCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of());

  /**
   * Runs a pipeline which reads data from BigQuery and writes it to AlloyDB.
   *
   * @param args arguments to the pipeline
   */
  public static void main(String[] args) {

    // BigQueryToAlloyDBOptions options =
    //     PipelineOptionsFactory.fromArgs(args).withValidation().as(BigQueryToAlloyDBOptions.class);
    
    BigQueryToJdbcOptions options = PipelineOptionsFactory.fromArgs(args)
        .withValidation().as(BigQueryToJdbcOptions.class);

    Pipeline pipeline = Pipeline.create(options);

    PCollection<String> tableDataString = pipeline
        .apply(
            "ReadFromBigQuery",
            ReadBigQuery.newBuilder()
                .setOptions(options)
                .build())
        .apply("Convert to json", ParDo.of(new TableRowToJsonFn()));

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


    tableDataString
            .apply(
                "writeToJdbc",
                DynamicJdbcIO.<String>write()
                    .withDataSourceConfiguration(dataSourceConfiguration)
                    .withStatement(options.getStatement())
                    .withPreparedStatementSetter(
                        new MapJsonStringToQuery(getKeyOrder(options.getStatement()))))
            .setCoder(FAILSAFE_ELEMENT_CODER);

  
 


        // tableDataString.apply(
        //     "Write to a text file",
        //     TextIO.write().to("gs://eenclona-sandbox-project1-bq-alloy/output"));

    pipeline.run();

       
    /* 
   
    */
    
  }

   /** The {@link JdbcIO.PreparedStatementSetter} implementation for mapping json string to query. */
   public static class MapJsonStringToQuery implements JdbcIO.PreparedStatementSetter<String> {

    List<String> keyOrder;

    public MapJsonStringToQuery(List<String> keyOrder) {
      this.keyOrder = keyOrder;
    }

    public void setParameters(String element, PreparedStatement query) throws SQLException {
      try {
        JSONObject object = new JSONObject(element);
        for (int i = 0; i < keyOrder.size(); i++) {
          String key = keyOrder.get(i);
          if (!object.has(key) || object.get(key) == JSONObject.NULL) {
            query.setNull(i + 1, Types.NULL);
          } else {
            query.setObject(i + 1, object.get(key));
          }
        }
      } catch (Exception e) {
        LOG.error("Error while mapping BigQuery strings to JDBC: {} with element {}", e.getMessage(), element);
      }
    }
  }

  private static KMSEncryptedNestedValue maybeDecrypt(String unencryptedValue, String kmsKey) {
    return new KMSEncryptedNestedValue(unencryptedValue, kmsKey);
  }

  private static List<String> getKeyOrder(String statement) {
    int startIndex = statement.indexOf("(");
    int endIndex = statement.indexOf(")");
    String data = statement.substring(startIndex + 1, endIndex);
    return Splitter.on(',').splitToList(data);
  }



  /*
   * 
   * Notes:
   * Edge cases not tested:
   * - Nested, structs
   * - Null values
   * 
   * 
   * Limitations:
   * - Need to put in type of data in --statement ?::(type) - especially for numeric values
   */
}
