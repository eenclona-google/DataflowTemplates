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
    a. Enable Analytics Hub API
    b. Search for Google Community Mobility Reports dataset under Google Cloud Public Datasets
    c. Add dataset to project
3. Create AlloyDB instance.
    a. Create a cluster
    b. Use default network and allow private services access connection
4. Setup a compute engine instance to connect to AlloyDB.
    a. Create a compute engine instance
    b. Under access scopes, enable Allow full access to all Cloud APIs.
    c. Once installed, ssh into it and install psql tool via 'sudo apt-get update' then 'sudo apt-get install postgresql-client'
    d. Connect to AlloyDB instance via 'psql -h IP_ADDRESS -U USERNAME' using private IP of alloy db instance and 'postgres' as the username
5. Create a table with the folowing command:
```
CREATE TABLE bq_dataset_1 (
  country_region_code VARCHAR(255),
  date DATE,
  residential_percent_change_from_baseline INTEGER
);
```
6. Create a CGS bucket on us-central1. 
7. Setup IAM permissions
    a. Grant the Compute Engine default service account the following roles at the project level:
        - BigQuery User
        - BigQuery Data Viewer
        - Dataflow Admin
        - Dataflow Worker
        - Storage Admin
8. Clone repo to the Compute Engine instance.
    a. First install git - `sudo apt-get install git-all`
    b. Clone repo with command `git clone https://github.com/eenclona-google/DataflowTemplates.git`
9. Run code.
    a. Install maven with `sudo apt install maven`
    b. cd into the root directory via `cd DataflowTemplates`
    c. Compile code with `mvn compile -pl v2 -am`. If error is thrown from syneo, compile with `mvn compile -pl v2 -P-oss-build -am`
    d. C





## Known Limitations
1. This code was not yet tested on tables with nested data in BigQuery.
2. Data from BigQuery will be read as a String, so we must specify the actual data type when inserting them into AlloyDB using SQL's ?::(type) syntax


## TO DO
1. Update documentation above for running code as a flex template
2. Determine if connectionProperties is a required parameter when exec class
3. Incorporate DLP module
