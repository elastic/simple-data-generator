package com.pahlsoft.simpledata.engine;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahlsoft.simpledata.generator.WorkloadGenerator;
import com.pahlsoft.simpledata.interfaces.Engine;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.text.MessageFormat;


public class WorkloadGeneratorEngine implements Engine {

    static Logger log = LoggerFactory.getLogger(WorkloadGeneratorEngine.class);
    private static Configuration configuration;
    private static Workload workload;
    private static ElasticsearchClient esClient;

    public WorkloadGeneratorEngine(final Configuration configuration, final Workload workload, final ElasticsearchClient esClient) {
        this.configuration = configuration;
        this.workload = workload;
        this.esClient = esClient;
    }

    @Override
    public void run() {
        if (log.isInfoEnabled()) {
            log.info(MessageFormat.format( "Thread[{1}] Initiating Workload: {0}", workload.getWorkloadName(), Thread.currentThread().getId()));
            log.info("Thread[" + Thread.currentThread() + "] Workload Thread Count: " + workload.getWorkloadThreads());
            log.info("Thread[" + Thread.currentThread() + "] Workload Sleep Time (ms): " + workload.getWorkloadSleep());
            log.info("Thread[" + Thread.currentThread() + "] Workload Index Primary Shard Count: " + workload.getPrimaryShardCount());
            log.info("Thread[" + Thread.currentThread() + "] Workload Index Replica Shard Count: " + workload.getReplicaShardCount());
            log.info("Thread[" + Thread.currentThread() + "] Purge on Start Setting: " + configuration.getPurgeOnStart().toString());
            log.info("Thread[" + Thread.currentThread() + "] Target Index Name: " + workload.getIndexName());
        }

        //////////////////////////
        // MAIN ENGINE LOOP BELOW
        /////////////////////////
        boolean engineRun = true;
        while (engineRun) {
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
                    if (configuration.getElasticsearchBulkQueueDepth() >= 1) {
                        BulkRequest.Builder br = new BulkRequest.Builder();
                        ObjectMapper objectMapper = new ObjectMapper();
                        InputStream input;

                        for (int bulkItems = 0; bulkItems < configuration.getElasticsearchBulkQueueDepth(); bulkItems++) {
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

                        log.debug(response.items().size() + " Documents Bulk Indexed in " + response.took() + "ms");
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

    public static JsonData readJson(InputStream input, ElasticsearchClient esClient) {
            JsonpMapper jsonpMapper = esClient._transport().jsonpMapper();
            JsonProvider jsonProvider = jsonpMapper.jsonProvider();
        return JsonData.from(jsonProvider.createParser(input), jsonpMapper);
    }

}
