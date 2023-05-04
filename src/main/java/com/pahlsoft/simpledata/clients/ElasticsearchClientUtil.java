package com.pahlsoft.simpledata.clients;

import co.elastic.apm.api.CaptureSpan;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.pahlsoft.simpledata.generator.WorkloadGenerator;
import com.pahlsoft.simpledata.interfaces.ClientUtil;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class ElasticsearchClientUtil implements ClientUtil {

    static Logger log = LoggerFactory.getLogger(ElasticsearchClientUtil.class);
    private static KeyStore trustStore = null;
    private static SSLContextBuilder sslBuilder = null;
    private static SSLContext sslContext = null;
    private static ElasticsearchTransport transport = null;
    private static RestClient restClient = null;
    private static ElasticsearchClient esClient = null;

    @CaptureSpan
    public static ElasticsearchClient createClient(final Configuration configuration, final Workload workload) {
       setupElasticsearch(configuration, workload);
       return esClient;
   }
    public static void setupElasticsearch(Configuration configuration, Workload workload) {
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // And create the API client
        esClient = buildElasticsearchClient(configuration);

        //Check to see if we need to purge existing run and clean it up
        if (workload.getPurgeOnStart()) {
            //TODO: May have created a threading issue with purgeOnStart... Might have to create a some logic in caller
            purgeOnStart(workload);
        } else {
            createElasticsearchIndexTemplate(workload);
        }
    }

    private static ElasticsearchClient buildElasticsearchClient(Configuration configuration) {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(configuration.getElasticsearchUser(),configuration.getElasticsearchPassword()));

        if (configuration.getElasticsearchScheme().contentEquals("https")) {
            sslBuilder = buildSSLContext(configuration);
            try {
                sslContext = sslBuilder.build();
            } catch (NoSuchAlgorithmException | KeyManagementException exception) {
                log.error("No Such Algorithm");
            }
            if (configuration.getElasticsearchApiKeyEnabled()){
                restClient = getSecureApiClient(sslContext, configuration).build();
            } else {
                restClient = getSecureClient(credentialsProvider, sslContext, configuration).build();
            }
        } else {
            restClient = getClient(credentialsProvider,configuration).build();
        }

        // Create the transport with a Jackson mapper
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // And create the API client
        return new ElasticsearchClient(transport);
    }

    private static void purgeOnStart(Workload workload) {
            deleteIndex(workload);
            deleteElasticsearchTemplate(workload);
            createElasticsearchIndexTemplate(workload);
    }

    private static void createElasticsearchIndexTemplate(Workload workload) {
        JSONObject indexTemplate;
        JSONObject indexTemplateWrapper;
        JSONObject indexMapping;
        JSONObject indexSettings;
        String jsonTemplateString;
        InputStream jsonTemplateStream;
        BooleanResponse getIndexTemplateResponse = new BooleanResponse(false);
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
            try {
                 getIndexTemplateResponse = esClient.indices().existsIndexTemplate((builder -> builder
                        .name(workload.getIndexName() + "_template")));
            } catch (Exception e) {
                log.info("Template {0} does not exist ",workload.getIndexName()+"_template creating...");
            }

            if ( getIndexTemplateResponse.value() == false) {
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

    private static void deleteElasticsearchTemplate(Workload workload) {
        // Check for Index Template
        try {
            BooleanResponse templateResponse = esClient.indices().existsIndexTemplate(builder -> builder.name(workload.getIndexName() + "_template"));
            if (templateResponse.value()) {
                if (workload.getPurgeOnStart()) {
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
        //TODO: Make this modular for other backends
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

    private static SSLContextBuilder buildSSLContext(Configuration configuration) {
        try {
            trustStore = KeyStore.getInstance("jks");
        } catch (KeyStoreException e) {
            log.warn(e.getMessage());
        }
        try (InputStream is = Files.newInputStream(Paths.get(configuration.getKeystoreLocation()))) {
            trustStore.load(is, configuration.getKeystorePassword().toCharArray());
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        try {
            sslBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            log.warn(e.getMessage());
        }
        return sslBuilder;
    }

    private static RestClientBuilder getSecureClient(CredentialsProvider credentialsProvider, SSLContext sslContext, Configuration configuration) {
        return RestClient.builder(
                        new HttpHost(configuration.getElasticsearchHost(), configuration.getElasticsearchPort(), configuration.getElasticsearchScheme()))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext);
                    }
                });
    }

    private static RestClientBuilder getSecureApiClient(SSLContext sslContext, Configuration configuration) {
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

    private static RestClientBuilder getClient(CredentialsProvider credentialsProvider, Configuration configuration) {
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

}
