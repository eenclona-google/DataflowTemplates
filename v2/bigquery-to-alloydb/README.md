# BigQuery to AlloyDB Dataflow Template

The [BigQueryToAlloyDB](src/main/java/com/google/cloud/teleport/v2/templates/BigQueryToBigtable.java) pipeline exports data from BigQuery using a query into AlloyDB.

NOTE: This template is currently unreleased. If you wish to use it now, you
will need to follow the steps outlined below to add it to and run it from
your own Google Cloud project.

## Getting Started

### Requirements
* Java 11
* Maven
* AlloyDB table exists
* BigQuery table exists

### Demo
1. Create a new GCP project.
2. Create a BigQuery table. For this demo, let's use public dataset: Google Community Mobility Reports.
    1. Enable Analytics Hub API
    2. Search for Google Community Mobility Reports dataset under Google Cloud Public Datasets
    3. Add dataset to project
3. Create AlloyDB instance.
    1. Create a cluster
    2. Use default network and allow private services access connection
4. Setup a compute engine instance to connect to AlloyDB.
    1. Create a compute engine instance
    2. Under access scopes, enable Allow full access to all Cloud APIs.
    3. Once installed, ssh into it and install psql tool via 'sudo apt-get update' then 'sudo apt-get install postgresql-client'
    4. Connect to AlloyDB instance via 'psql -h IP_ADDRESS -U USERNAME' using private IP of alloy db instance and 'postgres' as the username
5. Create a table with the folowing command:
```
CREATE TABLE bq_dataset_1 (
  country_region_code VARCHAR(255),
  date DATE,
  residential_percent_change_from_baseline INTEGER
);
```
6. Create a CGS bucket on us-central1, create a temp directory, and upload JDBC driver file. You can download this from https://jdbc.postgresql.org/download/
7. Setup IAM permissions
    1. Grant the Compute Engine default service account the following roles at the project level:
        - BigQuery User
        - BigQuery Data Viewer
        - Dataflow Admin
        - Dataflow Worker
        - Storage Admin
        - Service Account User
8. Clone repo to the Compute Engine instance.
    1. First install git - `sudo apt-get install git-all`
    2. Clone repo with command `git clone https://github.com/eenclona-google/DataflowTemplates.git`
9. Enable Dataflow API
10. Run code.
    1. Install maven with `sudo apt install maven`
    2. cd into the root directory via `cd DataflowTemplates`
    3. Install dependencies with `mvn clean install -pl v2/bigquery-to-alloydb -P-oss-build -am -Djib.skip -DskipTests`
    4. cd into biquery-to-alloydb with `cd v2/bigquery-to-alloydb`
    5. Compile code with `mvn compile`
    6. Run code with command below. Make sure to modify it to fit your environment's details like project name, gcs bucket, etc.

    ```
    mvn compile exec:java \
    -Dexec.mainClass=com.google.cloud.teleport.v2.templates.BigQueryToAlloyDB \
    -Dexec.args="\
    --query=\"SELECT country_region_code,date,residential_percent_change_from_baseline FROM eenclona-sandbox-project3-prd.google_community_mobility_reports.mobility_report LIMIT 100;\" \
    --tempLocation=gs://eenclona-sandbox-project3-prd-bucket/temp \
    --project=eenclona-sandbox-project3-prd \
    --driverClassName=org.postgresql.Driver \
    --connectionUrl=jdbc:postgresql://10.88.160.2/ \
    --driverJars=gs://eenclona-sandbox-project3-prd-bucket/postgresql-42.5.1.jar \
    --username=postgres \
    --password=password \
    --connectionProperties=username=postgres;password=password \
    --statement=\"INSERT INTO bq_dataset_1 (country_region_code,date,residential_percent_change_from_baseline) VALUES (?, ?::date, ?::integer);\" \
    --runner=DataflowRunner \
    --region=us-central1"
    ```


## Known Limitations
1. This code was not yet tested on tables with nested data in BigQuery.
2. Data from BigQuery will be read as a String, so we must specify the actual data type when inserting them into AlloyDB using SQL's ?::(type) syntax


## To Do
1. Update documentation above for running code as a flex template
2. Determine if connectionProperties is a required parameter when exec class
3. Incorporate DLP module
