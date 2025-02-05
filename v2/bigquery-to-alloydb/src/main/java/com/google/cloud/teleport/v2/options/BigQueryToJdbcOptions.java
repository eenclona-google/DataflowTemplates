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
package com.google.cloud.teleport.v2.options;

import com.google.cloud.teleport.metadata.TemplateParameter;
import com.google.cloud.teleport.v2.transforms.BigQueryConverters.BigQueryReadOptions;

/**
 * The {@link BigQueryToJdbcOptions} interface provides the custom execution options passed by the
 * executor at the command-line.
 */
public interface BigQueryToJdbcOptions extends BigQueryReadOptions {

  @TemplateParameter.Text(
      order = 2,
      optional = false,
      regexes = {"^.+$"},
      description = "JDBC driver class name.",
      helpText = "JDBC driver class name to use.",
      example = "com.mysql.jdbc.Driver")
  String getDriverClassName();

  void setDriverClassName(String driverClassName);

  @TemplateParameter.Text(
      order = 3,
      optional = false,
      regexes = {
        "(^jdbc:[a-zA-Z0-9/:@.?_+!*=&-;]+$)|(^([A-Za-z0-9+/]{4}){1,}([A-Za-z0-9+/]{0,3})={0,3})"
      },
      description = "JDBC connection URL string.",
      helpText =
          "Url connection string to connect to the JDBC source. Connection string can "
              + "be passed in as plaintext or as a base64 encoded string encrypted by Google Cloud KMS.",
      example = "jdbc:mysql://some-host:3306/sampledb")
  String getConnectionUrl();

  void setConnectionUrl(String connectionUrl);

  @TemplateParameter.Text(
      order = 4,
      optional = true,
      regexes = {"^.+$"},
      description = "JDBC connection username.",
      helpText =
          "User name to be used for the JDBC connection. User name can be passed in as plaintext "
              + "or as a base64 encoded string encrypted by Google Cloud KMS.")
  String getUsername();

  void setUsername(String username);

  @TemplateParameter.Password(
      order = 5,
      optional = true,
      description = "JDBC connection password.",
      helpText =
          "Password to be used for the JDBC connection. Password can be passed in as plaintext "
              + "or as a base64 encoded string encrypted by Google Cloud KMS.")
  String getPassword();

  void setPassword(String password);

  @TemplateParameter.Text(
      order = 6,
      optional = false,
      regexes = {"^.+$"},
      description = "Cloud Storage paths for JDBC drivers",
      helpText = "Comma separate Cloud Storage paths for JDBC drivers.",
      example = "gs://your-bucket/driver_jar1.jar,gs://your-bucket/driver_jar2.jar")
  String getDriverJars();

  void setDriverJars(String driverJar);

  @TemplateParameter.Text(
      order = 7,
      optional = true,
      regexes = {"^[a-zA-Z0-9_;!*&=@#-:\\/]+$"},
      description = "JDBC connection property string.",
      helpText =
          "Properties string to use for the JDBC connection. Format of the string must be [propertyName=property;]*.",
      example = "unicode=true;characterEncoding=UTF-8")
  String getConnectionProperties();

  void setConnectionProperties(String connectionProperties);

  @TemplateParameter.Text(
      order = 8,
      optional = false,
      regexes = {"^.+$"},
      description = "Statement which will be executed against the database.",
      helpText =
          "SQL statement which will be executed to write to the database. The statement "
              + "must specify the column names of the table in any order. Only the values of the specified "
              + "column names will be read from the json and added to the statement.",
      example = "INSERT INTO tableName (column1, column2) VALUES (?,?)")
  String getStatement();

  void setStatement(String statement);

  @TemplateParameter.KmsEncryptionKey(
      order = 10,
      optional = true,
      description = "Google Cloud KMS encryption key",
      helpText =
          "If this parameter is provided, password, user name and connection string should all be passed in encrypted. Encrypt parameters using the KMS API encrypt endpoint. See: https://cloud.google.com/kms/docs/reference/rest/v1/projects.locations.keyRings.cryptoKeys/encrypt",
      example =
          "projects/{gcp_project}/locations/{key_region}/keyRings/{key_ring}/cryptoKeys/{kms_key_name}")
  String getKMSEncryptionKey();

  void setKMSEncryptionKey(String keyName);
}
