package com.pahlsoft.simpledata.engine;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;
import com.pahlsoft.simpledata.generator.WorkloadGenerator;
import com.pahlsoft.simpledata.interfaces.Engine;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

public class WorkloadGeneratorEngine implements Engine {

    static Logger log = LoggerFactory.getLogger(WorkloadGeneratorEngine.class);

    private static Configuration config_map;
    private Workload workload;

    KeyStore truststore = null;
    SSLContextBuilder sslBuilder = null;

    public WorkloadGeneratorEngine(final Configuration config_map, final Workload workload) {
        this.config_map = config_map;
        this.workload = workload;
    }

    @Override
    public void run() {

        if (log.isInfoEnabled()) {
            log.info(MessageFormat.format("Starting Workload Generation Engine Workload: {0} Thread: {1}", workload.getWorkloadName(), Thread.currentThread().getId()));
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(this.config_map.getElasticsearchUser(),this.config_map.getElasticsearchPassword()));

        RestHighLevelClient client;
        IndexRequest indexRequest;

        try {
            if (this.config_map.getElasticsearchScheme().contentEquals("https")) {
                sslBuilder = buildSSLContext();
                final SSLContext sslContext = sslBuilder.build();
                client = getSecureClient(credentialsProvider, sslContext);

            } else {
                client = getClient(credentialsProvider);
            }

            boolean engineRun = true;
            while(engineRun) {
                Transaction transaction = ElasticApm.startTransaction();
                try {

                    setTransactionInfo(transaction);
                    transaction.activate();
                    Span span = transaction.startSpan();
                    setSpanInfo(span, "indexRequest: ", workload.getIndexName());
                    span.activate();

                        try {
                            indexRequest = new IndexRequest(workload.getIndexName(), "_doc").source(WorkloadGenerator.buildDocument(workload));
                            client.index(indexRequest, RequestOptions.DEFAULT);
                            log.debug("|"+ workload.getWorkloadName() + "|");
                            Thread.sleep(workload.getWorkloadSleep());
                        } catch (Exception e) {
                            span.captureException(e);
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
        transaction.addLabel("workload",workload.getWorkloadName());
        transaction.addLabel("thread_id",Thread.currentThread().getId());
        transaction.addLabel("sleep_time",workload.getWorkloadSleep());
        transaction.addLabel("total_threads", workload.getWorkloadThreads());
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

    private RestHighLevelClient getSecureClient(CredentialsProvider credentialsProvider, SSLContext sslContext) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(this.config_map.getElasticsearchHost(), this.config_map.getElasticsearchPort(), this.config_map.getElasticsearchScheme()))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext);
                            }
                        }));
    }

    private RestHighLevelClient getClient(CredentialsProvider credentialsProvider) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(this.config_map.getElasticsearchHost(),this.config_map.getElasticsearchPort(), this.config_map.getElasticsearchScheme()))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            }
                        }));

    }
}
