package com.pahlsoft.simpledata.runner;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.pahlsoft.simpledata.generator.WorkloadGenerator;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;
import com.pahlsoft.simpledata.engine.WorkloadGeneratorEngine;
import com.pahlsoft.simpledata.threader.WorkloadGeneratorEngineThreader;
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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class EngineRunner {
    static Logger log = LoggerFactory.getLogger(EngineRunner.class);

    private static Configuration configuration;
    private static KeyStore truststore = null;
    private static SSLContextBuilder sslBuilder = null;
    private static SSLContext sslContext = null;
    private static ElasticsearchTransport transport = null;

    private static RestClient restClient = null;

    private static ElasticsearchClient esClient = null;

    public static void main(String[] args) {
       validateArguments(args);
        loadConfig(args[0]);
        esClient = buildClient();

        try {
            // Create the transport with a Jackson mapper
            transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            // And create the API client
            esClient = buildClient();

            Iterator iterator = configuration.getWorkloads().iterator();

            //Check to see if we need to purge existing run and clean it up
            if (configuration.getPurgeOnStart()) {
                purgeOnStart();
                setupTemplates();
            } else {
                setupTemplates();
            }

            // Create Engine for each workload
            while (iterator.hasNext()) {
                Workload workload;
                workload = (Workload) iterator.next();
                WorkloadGeneratorEngineThreader.runEngine(workload.getWorkloadThreads(), new WorkloadGeneratorEngine(configuration,workload,esClient));
            }
            if (log.isDebugEnabled()) {
                debugConfiguration();
            }
            log.info("Workloads Started");
            System.out.println("Workloads Started.");

        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Initialization Error: access to yml is restricted or incorrectly configured");
        }

    }


    private static ElasticsearchClient buildClient () {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(configuration.getElasticsearchUser(),configuration.getElasticsearchPassword()));

        if (configuration.getElasticsearchScheme().contentEquals("https")) {
            sslBuilder = buildSSLContext();
            try {
                 sslContext = sslBuilder.build();
            } catch (NoSuchAlgorithmException | KeyManagementException exception) {
                log.error("No Such Algorithm");
            }
            if (configuration.getElasticsearchApiKeyEnabled()){
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
        return new ElasticsearchClient(transport);
    }

    private static void debugConfiguration() {
        log.debug("Elasticsearch Endpoint: " + configuration.getElasticsearchHost() );
        log.debug("Elasticsearch Endpoint HTTP Scheme: " + configuration.getElasticsearchScheme() );
        log.debug("Elasticsearch Port: " + configuration.getElasticsearchPort() );
        log.debug("ApiKeyID: " + configuration.getElasticsearchApiKeyId());
        log.debug("ApiKeySecret: " + configuration.getElasticsearchApiKeySecret() );
        log.debug("Elasticsearch User: " + configuration.getElasticsearchUser() );
        log.debug("Elasticsearch Password: " + configuration.getElasticsearchPassword() );
        log.debug("Elasticsearch Bulk Queue Depth: " + configuration.getElasticsearchBulkQueueDepth() );
        log.debug("Number of Workloads " + configuration.getWorkloads().size());
    }

    private static void loadConfig(String args) {
        try {
            try
            {
                Yaml yaml = new Yaml(new Constructor(Configuration.class));
                InputStream inputStream = new FileInputStream(new File(args));
                configuration = yaml.load(inputStream);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } catch ( Exception e) {
            System.out.println("Initialization Error: Unable to Load YML model file");
            log.error("Initialization Error: Unable to Load YML model file");
            System.exit(1);
        }
        log.info("Configuration Loaded");
        System.out.println("Configuration Loaded.");
    }

    private static void validateArguments(String[] args) {
        if (args.length !=1 ) {
            System.out.println("Error - Improper Usage, try: sample-data-generator-all*.jar your_config_file.yml ");
            log.error("Error - Improper Usage, try: sample-data-generator-all*.jar your_config_file.yml ");
            System.exit(1);
        }
    }

    private static void purgeOnStart() {
        //TODO:  Put elimination logic here so we're not walking over each other in the threads
        // Iterate through each workload and delete target indices.

        Iterator iterator = configuration.getWorkloads().iterator();

        while (iterator.hasNext()) {
            Workload workload;
            workload = (Workload) iterator.next();
            deleteIndex(workload);
            deleteTemplate(workload);
            createIndexTemplate(workload);
        }
    }

    private static void setupTemplates() {
        Iterator iterator = configuration.getWorkloads().iterator();

        while (iterator.hasNext()) {
            Workload workload;
            workload = (Workload) iterator.next();
            createIndexTemplate(workload);
        }
    }

    private static void createIndexTemplate(Workload workload) {
        JSONObject indexTemplate;
        JSONObject indexTemplateWrapper;
        JSONObject indexMapping;
        JSONObject indexSettings;
        String jsonTemplateString;
        InputStream jsonTemplateStream;

        indexTemplate = new JSONObject();
        indexTemplateWrapper = new JSONObject();
        try {

            indexMapping = WorkloadGenerator.buildMapping(workload);
            indexSettings = WorkloadGenerator.buildSettings(workload);
            String patternString =  workload.getIndexName() +"*";
            indexTemplate.append("index_patterns", patternString);

            // Build the JSON for mapping information
            indexTemplateWrapper.put("settings",indexSettings);
            indexTemplateWrapper.put("mappings",indexMapping);
            indexTemplate.put("template", indexTemplateWrapper);
            jsonTemplateString =  indexTemplate.toString();
            jsonTemplateStream = new ByteArrayInputStream(jsonTemplateString.getBytes());

            GetIndexTemplateResponse getIndexTemplateResponse = esClient.indices().getIndexTemplate(builder -> builder
                    .name(workload.getIndexName()+"_template"));

            if (getIndexTemplateResponse.indexTemplates().isEmpty()) {
                PutIndexTemplateResponse putIndexTemplate = esClient.indices().putIndexTemplate(builder -> builder
                        .name(workload.getIndexName()+"_template")
                        .withJson(jsonTemplateStream)
                );

                if (putIndexTemplate.acknowledged()) {
                    log.info("Created Index Template " +workload.getIndexName()+"_template ");
                }

            } else {
                log.info("Index Template " +workload.getIndexName()+"_template already exits.");
            }

        } catch (JSONException | IOException e) {
            log.error("Error Creating Index Template " +workload.getIndexName()+"_template ");
            log.error("Thread[ " + Thread.currentThread() + "] Template JSON : " + indexTemplate.toString());
        }
    }

    private static void deleteTemplate(Workload workload) {
        // Check for Index Template
        try {
            BooleanResponse templateResponse = esClient.indices().existsIndexTemplate(builder -> builder.name(workload.getIndexName() + "_template"));
            if (templateResponse.value()) {
                if (configuration.getPurgeOnStart()) {
                    log.info("Index Template " + workload.getIndexName() + "_template exists. Purging.");
                    DeleteIndexTemplateResponse deleteIndexTemplateResponse = esClient.indices().deleteIndexTemplate(builder -> builder.name(workload.getIndexName() + "_template"));
                    if (deleteIndexTemplateResponse.acknowledged()) {
                        log.info("Index Template " + workload.getIndexName() + "_template Purged.");
                    }
                }
            } else {
                log.info("Index Template " + workload.getIndexName() + "_template does not exist. Skipping.");
            }

        } catch (IOException ioe) {
            log.error("Error Deleting Index template " + workload.getIndexName() + "_template.");
        }

    }

    private static void deleteIndex(Workload workload) {
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
        }

    private static SSLContextBuilder buildSSLContext() {
        try {
            truststore = KeyStore.getInstance("jks");
        } catch (KeyStoreException e) {
            log.warn(e.getMessage());
        }
        try (InputStream is = Files.newInputStream(Paths.get(configuration.getKeystoreLocation()))) {
            truststore.load(is, configuration.getKeystorePassword().toCharArray());
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

    private static RestClientBuilder getSecureClient(CredentialsProvider credentialsProvider, SSLContext sslContext) {
        return RestClient.builder(
                        new HttpHost(configuration.getElasticsearchHost(), configuration.getElasticsearchPort(), configuration.getElasticsearchScheme()))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext);
                    }
                });
    }

    private static RestClientBuilder getSecureApiClient(SSLContext sslContext) {
        String apiKeyAuth = Base64.getEncoder().encodeToString((configuration.getElasticsearchApiKeyId() + ":" + configuration.getElasticsearchApiKeySecret()).getBytes(StandardCharsets.UTF_8));
        Collection<Header> defaultHeaders = Collections.singleton((new BasicHeader("Authorization", "ApiKey " + apiKeyAuth)));

        //return new RestHighLevelClient(
        return      RestClient.builder(
                        new HttpHost(configuration.getElasticsearchHost(), configuration.getElasticsearchPort(), configuration.getElasticsearchScheme()))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setSSLContext(sslContext).setDefaultHeaders(defaultHeaders);
                    }
                });
    }

    private static RestClientBuilder getClient(CredentialsProvider credentialsProvider) {
        //return new RestHighLevelClient(
        return      RestClient.builder(
                        new HttpHost(configuration.getElasticsearchHost(),configuration.getElasticsearchPort(), configuration.getElasticsearchScheme()))
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
