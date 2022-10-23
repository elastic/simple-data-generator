package com.pahlsoft.simpledata.engine;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahlsoft.simpledata.generator.WorkloadGenerator;
import com.pahlsoft.simpledata.interfaces.Engine;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;

import jakarta.json.spi.JsonProvider;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

public class WorkloadGeneratorEngine implements Engine {

    static Logger log = LoggerFactory.getLogger(WorkloadGeneratorEngine.class);
    private static Configuration config_map;
    private static Workload workload;
    KeyStore truststore = null;
    SSLContextBuilder sslBuilder = null;
    RestClient restClient = null;
    ElasticsearchTransport transport = null;
    ElasticsearchClient esClient = null;
    JSONObject indexMapping;
    JSONObject indexSettings;

    String jsonString;

    InputStream jsonStream;

    public WorkloadGeneratorEngine(final Configuration config_map, final Workload workload) {
        this.config_map = config_map;
        this.workload = workload;
    }

    @Override
    public void run() {

        if (log.isInfoEnabled()) {
            log.info(MessageFormat.format("Starting Workload Generation Engine Workload: {0} Thread: {1}", workload.getWorkloadName(), Thread.currentThread().getId()));
            log.info("Workload Name: " + workload.getWorkloadName());
            log.info("Workload Thread Count: " + workload.getWorkloadThreads());
            log.info("Workload Sleep Time (ms): " + workload.getWorkloadSleep());
            log.info("Workload Purge Existing Index: " + workload.getPurgeExistingIndex());
            log.info("Workload Index Primary Shard Count: " + workload.getPrimaryShardCount());
            log.info("Workload Index Replica Shard Count: " + workload.getReplicaShardCount());
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(this.config_map.getElasticsearchUser(),this.config_map.getElasticsearchPassword()));

        try {
            if (this.config_map.getElasticsearchScheme().contentEquals("https")) {
                sslBuilder = buildSSLContext();
                final SSLContext sslContext = sslBuilder.build();
                if (this.config_map.getElasticsearchApiKeyEnabled()){
                    restClient = getSecureApiClient(sslContext).build();
                } else {
                    restClient = getSecureClient(credentialsProvider, sslContext).build();
                }
            } else {
                restClient = getClient(credentialsProvider).build();
            }

            // Create the transport with a Jackson mapper
            transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            // And create the API client
            esClient = new ElasticsearchClient(transport);

            // TODO: extract this logic into methods to clean up readability
            if (workload.getPurgeExistingIndex()) {
                log.info("Purging Existing Index");
                try {
                    BooleanResponse existResponse;
                    existResponse = esClient.indices().exists(b -> b.index(workload.getIndexName()));

                    if (existResponse.value()) {
                        log.info("Index "+ workload.getIndexName() + " deleting");
                        try {
                            DeleteIndexResponse deleteResponse;
                            deleteResponse = esClient.indices().delete(b -> b.index(workload.getIndexName()));
                            if (deleteResponse.acknowledged()) { log.info("Index "+ workload.getIndexName() + " deleted");}
                        } catch (IOException ioe) {
                            log.error("Error Deleting Index: " + workload.getIndexName());
                        }
                    }
                } catch (IOException ioe) {
                    log.error("Error Checking if Index Exists: " + workload.getIndexName());
                }

                // Create a Mapping and settings based on Workload (include settings)
                //TODO: Need to do settings after mapping is figured out
                try {
                    indexMapping = WorkloadGenerator.buildMapping(workload);
                    indexSettings = WorkloadGenerator.buildSettings(workload);

                } catch (JSONException e) {
                    log.error("Unable to build Mapping or Settings for Workload " + workload.getWorkloadName());
                }

                //Create The Index
                try {
                    CreateIndexRequest req = null;
                    // Build the JSON for mapping information
                    jsonString = indexSettings.getJSONObject(String.valueOf("setting")).toString();
                    jsonString += "," + indexMapping.getJSONObject(String.valueOf("mapping")).toString();
                    jsonStream = new ByteArrayInputStream(jsonString.getBytes());
                    // create index based on configuration
                    req = CreateIndexRequest.of(b -> b
                            .index(workload.getIndexName())
                            .withJson(jsonStream)
                    );

                    boolean created = esClient.indices().create(req).acknowledged();
                    if (created) log.info("Mapping created for index: " + workload.getIndexName());

                } catch (Exception e) {
                    log.error("Cannot build mapping for index: " + workload.getIndexName());
                    log.error(e.getMessage());
                }

            } else {
                //  if index exists do nothing but report index already exists with mappings, Skipping
                try {
                    BooleanResponse existResponse;
                    existResponse = esClient.indices().exists(b -> b.index(workload.getIndexName()));
                    if (existResponse.value()) { log.info("Index " + workload.getIndexName() + " exists. Not updating mappings or settings.");}
                } catch (IOException ioe) {
                    log.error("Error Checking if Index Exists: " + workload.getIndexName());
                }
            }

            //////////////////////////
            // MAIN ENGINE LOOP BELOW
            /////////////////////////

            boolean engineRun = true;
            while(engineRun) {
                Transaction transaction = ElasticApm.startTransaction();
                try {
                    //APM
                    setTransactionInfo(transaction);
                    transaction.activate();
                    Span span = transaction.startSpan();
                    setSpanInfo(span, "indexRequest: ", workload.getIndexName());
                    span.activate();

                    try {
                            //Bulk Docs
                            if (config_map.getElasticsearchBulkQueueDepth() >= 1 ) {
                                BulkRequest.Builder br = new BulkRequest.Builder();
                                ObjectMapper objectMapper = new ObjectMapper();
                                InputStream input;

                                for (int bulkItems=0; bulkItems < config_map.getElasticsearchBulkQueueDepth(); bulkItems++) {
                                    input = new ByteArrayInputStream(objectMapper.writeValueAsString(WorkloadGenerator.buildDocument(workload)).getBytes());
                                    JsonData jsonp = readJson(input, esClient);
                                    br.operations(op -> op
                                            .index(idx -> idx
                                                .index(workload.getIndexName())
                                                .document(jsonp)
                                            )
                                    );
                                }
                                BulkResponse response = esClient.bulk(br.build());

                                log.debug( response.items().size() + " Documents Bulk Indexed in " + response.took() + "ms");

                            // Single Doc
                            } else {
                                ObjectMapper objectMapper = new ObjectMapper();
                                String json = objectMapper.writeValueAsString(WorkloadGenerator.buildDocument(workload));
                                Reader input = new StringReader(json);
                                IndexRequest<JsonData> request = IndexRequest.of(i -> i
                                        .index(workload.getIndexName())
                                        .withJson(input)
                                );
                                IndexResponse response = esClient.index(request);
                                log.debug("Document " + response.id() + " Indexed with version " + response.version());
                            }
                            //TODO: This is where the periodicity/peak-spike logic goes
                            Thread.sleep(workload.getWorkloadSleep());
                        } catch (Exception e) {
                            span.captureException(e);
                            log.debug("Indexing Error:" + e.getMessage());
                            if (log.isDebugEnabled()) {
                                e.printStackTrace();
                            }
                        } finally {
                            span.end();
                        }
                    transaction.setResult("complete");

                } catch (Exception ioe) {
                    engineRun = false;
                    ioe.printStackTrace();
                    log.warn(ioe.getMessage());
                    transaction.setResult("failed");
                    transaction.captureException(ioe);
                } finally {
                    transaction.end();
                }
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.warn(e.getMessage());
        }
    }

    private void setSpanInfo(Span span, String s, String indexName) {
        span.setName(s + indexName);
    }

    private void setTransactionInfo(Transaction transaction) {
        transaction.setName(workload.getWorkloadName()+"Transaction");
        transaction.setType(Transaction.TYPE_REQUEST);
        transaction.setLabel("workload",workload.getWorkloadName());
        transaction.setLabel("thread_id",Thread.currentThread().getId());
        transaction.setLabel("sleep_time",workload.getWorkloadSleep());
        transaction.setLabel("total_threads", workload.getWorkloadThreads());
    }

    private SSLContextBuilder buildSSLContext() {
        try {
            truststore = KeyStore.getInstance("jks");
        } catch (KeyStoreException e) {
            log.warn(e.getMessage());
        }
        try (InputStream is = Files.newInputStream(Paths.get(config_map.getKeystoreLocation()))) {
            truststore.load(is, this.config_map.getKeystorePassword().toCharArray());
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        try {
            sslBuilder = SSLContexts.custom()
                    .loadTrustMaterial(truststore, null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            log.warn(e.getMessage());
        }
        return sslBuilder;
    }

    private RestClientBuilder getSecureClient(CredentialsProvider credentialsProvider, SSLContext sslContext) {
          return RestClient.builder(
                        new HttpHost(this.config_map.getElasticsearchHost(), this.config_map.getElasticsearchPort(), this.config_map.getElasticsearchScheme()))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext);
                            }
                        });
    }

    private RestClientBuilder getSecureApiClient(SSLContext sslContext) {
        String apiKeyAuth = Base64.getEncoder().encodeToString((this.config_map.getElasticsearchApiKeyId() + ":" + this.config_map.getElasticsearchApiKeySecret()).getBytes(StandardCharsets.UTF_8));
        Collection<Header> defaultHeaders = Collections.singleton((new BasicHeader("Authorization", "ApiKey " + apiKeyAuth)));

        //return new RestHighLevelClient(
          return      RestClient.builder(
                        new HttpHost(this.config_map.getElasticsearchHost(), this.config_map.getElasticsearchPort(), this.config_map.getElasticsearchScheme()))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setSSLContext(sslContext).setDefaultHeaders(defaultHeaders);
                            }
                        });
    }

    private RestClientBuilder getClient(CredentialsProvider credentialsProvider) {
        //return new RestHighLevelClient(
          return      RestClient.builder(
                        new HttpHost(this.config_map.getElasticsearchHost(),this.config_map.getElasticsearchPort(), this.config_map.getElasticsearchScheme()))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            }
                        });

    }

    public static JsonData readJson(InputStream input, ElasticsearchClient esClient) {
        JsonpMapper jsonpMapper = esClient._transport().jsonpMapper();
        JsonProvider jsonProvider = jsonpMapper.jsonProvider();

        return JsonData.from(jsonProvider.createParser(input), jsonpMapper);
    }
}
