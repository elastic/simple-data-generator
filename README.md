# Simple Data Generator for Elasticsearch Indices
The purpose of this project is to ingest some sample data into Elasticsearch. If you have a model in mind, but aren't ready to build out piplines with Logstash or Beats, or write code to insert data into Elasticsearch, this project might help you.

It's multi-threaded so you can generate (depending on system resources) a fair amount of load.
It's a refactor of ajpahl1008/sample-data-generator which required development for each new workload type.
This version is completely YAML driven. No more coding Yay!!!

Note: This code should not be used as a benchmarking tool for your Elasticsearch instances. You should use a tool like Elastic's Rally (https://github.com/elastic/rally).  This tool, is for just loading a **bunch** of random data (closer to what YOU are going to put in Production) so you can become familiar with the query DSL, or Kibana or Maps or Aggregations. **OR!!!** just demonstrate how effective your data analysis will be streaming to the Elastic stack.

## Requirements
* Java OpenJDK
* Gradle

# Setup 

## Step 1: (Optional,Not Optional) Create a Keystore. 

This used to be optional but now that Security (TLS/Authentication) is in the Basic subscription (FREE) there's no reason to not be secure from the start.  _Granted, you _can_ setup the Elastic Stack without security but why?_
```
# ./build_keystore.bash <keystore_password> <elasticsearch_host> <elasticsearch_port>
```
Arguments: 
  * keystore_password: something you arbirarily set when you create they keystore for the first time.
  * elasticsearch_host: exclude the http/https it's just the FQDN that resolves to your cluster.
  * elasticsearch_port: whatever port you've specified for Elasticsearch.  If you're on Elasticsearch Service (cloud.elastic.co) it's 9243.
  
Depending how you'll access Elasticsearch, you can run this a few times [changing the elasticsearch_host] to grab the TLS keys for ALL the Elasticsearch servers. 

## Step 2: Create A configuration YAML (yml) file.

There's a couple of examples in the example directory but here's the basic structure.
```
elasticsearchScheme: https
elasticsearchHost: someclusterid.cloud.elastic.co
elasticsearchPort: 9243
elasticsearchUser: elastic
elasticsearchPassword: somePassword
keystoreLocation: keystore.jks
keystorePassword: yourkeystorePassword
workloads:
  - workloadName: workload_1
    workloadThreads: 1
    workloadSleep: 1000
    indexName: index-1
    fields:
      - name: account_number
        type: int

      - name: state
        type: state

      - name: balance
        type: float
        range: 1,20000

```
### Multiple Workload Structure
```
elasticsearchScheme: https
elasticsearchHost: someclusterid.cloud.elastic.co
elasticsearchPort: 9243
elasticsearchUser: elastic
elasticsearchPassword: somePassword
keystoreLocation: keystore.jks
keystorePassword: yourkeystorePassword
workloads:
  - workloadName: workload_1
    workloadThreads: 1
    workloadSleep: 1000
    indexName: index-1
    fields:
      - name: account_number
        type: int
   ...
   - workloadName: workload_2
    workloadThreads: 1
    workloadSleep: 1000
    indexName: index-2
    fields:
      - name: inventory_part_number
        type: int
   ...
```
Documentation on the different types: https://github.com/ajpahl1008/simple-data-generator/blob/master/docs/supported_fields.md 

## Step 3 Compile project
```
gradle clean; gradle build fatJar
```

## Step 4 Run Project
```
java -jar build/libs/simple-data-generator-all-1.0-SNAPSHOT.jar your_yaml.yml
OR
complete the <NEED_THIS> tagged fields in the runme.bash script
```

## Step 4.5 Running with Elastic Application Performance Monitoring (APM)
```
complete the <NEED_THIS> fields in the runme_apm.bash script
Simply, you need the URL for your APM server and the token provided by APM.

Additionally, there's a debug script runme_apm_debug.bash if things get confusing or not going smoothly.
```
