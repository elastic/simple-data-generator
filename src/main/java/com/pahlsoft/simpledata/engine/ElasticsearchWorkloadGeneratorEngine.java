package com.pahlsoft.simpledata.engine;

import co.elastic.apm.api.CaptureSpan;
import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pahlsoft.simpledata.clients.ElasticsearchClientUtil;
import com.pahlsoft.simpledata.generator.WorkloadGenerator;
import com.pahlsoft.simpledata.interfaces.Engine;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ElasticsearchWorkloadGeneratorEngine implements Engine {

    static Logger log = LoggerFactory.getLogger(ElasticsearchWorkloadGeneratorEngine.class);

    private Workload workload;
    private static ElasticsearchClient esClient;

    public ElasticsearchWorkloadGeneratorEngine(Configuration configuration, Workload workload) {
        this.workload = workload;
        this.esClient = ElasticsearchClientUtil.createClient(configuration, workload);
    }

    @Override
    public void run() {
        if (log.isInfoEnabled()) {
            log.info(MessageFormat.format( "Thread[{1}] Initiating Elasticsearch Workload: {0}", workload.getWorkloadName(), Thread.currentThread().getId()));
            log.info("Thread[" + Thread.currentThread() + "] Workload Thread Count: " + workload.getWorkloadThreads());
            log.info("Thread[" + Thread.currentThread() + "] Workload Sleep Time (ms): " + workload.getWorkloadSleep());
            log.info("Thread[" + Thread.currentThread() + "] Workload Index Primary Shard Count: " + workload.getPrimaryShardCount());
            log.info("Thread[" + Thread.currentThread() + "] Workload Index Replica Shard Count: " + workload.getReplicaShardCount());
            log.info("Thread[" + Thread.currentThread() + "] Purge on Start Setting: " + workload.getPurgeOnStart().toString());
            log.info("Thread[" + Thread.currentThread() + "] Target Index Name: " + workload.getIndexName());
            log.info("Thread[" + Thread.currentThread() + "] Bulk Queue Depth: " + workload.getElasticsearchBulkQueueDepth());
        }

        //////////////////////////
        // MAIN ENGINE LOOP BELOW
        //////////////////////////
        boolean engineRun = true;
        while (engineRun) {
            try {
                try {
                    //Bulk Docs
                    if (workload.getElasticsearchBulkQueueDepth() > 0) {
                        // Elastic APM
                        Transaction transaction = ElasticApm.startTransaction();
                        setTransactionInfo(transaction,"BulkIndexRequest");
                        transaction.setLabel("BulkIndexRequest: ", workload.getIndexName());
                        Span span = transaction.startSpan();
                        span.setName("Build Bulk Request");
                        BulkRequest.Builder br = new BulkRequest.Builder();
                        ObjectMapper objectMapper = new ObjectMapper();
                        InputStream input;

                        for (int bulkItems = 0; bulkItems < workload.getElasticsearchBulkQueueDepth(); bulkItems++) {
                            input = new ByteArrayInputStream(objectMapper.writeValueAsString(WorkloadGenerator.buildDocument(workload)).getBytes());
                            JsonData jsonp = readJson(input, esClient);
                            br.operations(op -> op
                                    .index(idx -> idx
                                            .index(workload.getIndexName())
                                            .document(jsonp)
                                    )
                            );
                        }
                        BulkResponse response = null;
                        try {
                            response = esClient.bulk(br.build());
                        } catch (Exception e) {
                            System.out.println("Looking HERE AJ!!!");
                            e.printStackTrace();
                        }
                        log.debug(response.items().size() + " Documents Bulk Indexed in " + response.took() + "ms");
                        span.end();
                        transaction.end();
                    // Single Doc
                    } else {
                        // Elastic APM
                        Transaction transaction = ElasticApm.startTransaction();
                        setTransactionInfo(transaction,"SingleIndexRequest");
                        transaction.setLabel("SingleIndexRequest: ", workload.getIndexName());
                        Span span = transaction.startSpan();
                        span.setName("Build Single Request");

                        ObjectMapper objectMapper = new ObjectMapper();
                        String json = objectMapper.writeValueAsString(WorkloadGenerator.buildDocument(workload));
                        Reader input = new StringReader(json);
                        IndexRequest<JsonData> request = IndexRequest.of(i -> i
                                .index(workload.getIndexName())
                                .withJson(input)
                        );
                        IndexResponse response = esClient.index(request);
                        log.debug("Document " + response.id() + " Indexed with version " + response.version());
                        span.end();
                        transaction.end();
                    }

                    //TODO: This is where the periodicity/peak-spike logic goes
                    Thread.sleep(calculateSleepDuration());
                } catch (Exception e) {
                    log.debug("Indexing Error:" + e.getMessage());
                    if (log.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception ioe) {
                engineRun = false;
                ioe.printStackTrace();
                log.warn(ioe.getMessage());
            }
        }
    }

    private void setTransactionInfo(Transaction transaction, String transactionType) {
        transaction.setName(transactionType);
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

    private int calculateSleepDuration() {
        if (workload.getPeakTime().isEmpty()) {  // Peak time is priority over static time.
            log.debug("Static Sleep Used");
            return workload.getWorkloadSleep();
        } else {
            // Get the current time
            LocalTime currentTime = LocalTime.now();

            LocalTime peakTime = LocalTime.parse(workload.getPeakTime(),DateTimeFormatter.ISO_LOCAL_TIME);

            // Calculate the time difference in seconds
            long timeDifference = ChronoUnit.SECONDS.between(currentTime, peakTime);

            // Assuming the maximum time difference is 1 day (86400 seconds) for the scaling
            double maxTimeDifference = 86400.0;

            // Calculate the position in the sine wave based on the time difference
            double position = (Math.PI / 2) * (timeDifference / maxTimeDifference);

            // Adjust the position for past peak time
            if (timeDifference < 0) {
                position = Math.PI - position;
            }

            // Calculate the sine wave value based on the position
            double sineValue = Math.sin(position) * Math.cos(position);

            // Scale the sine value to the range of 1 to 10000
            int value = (int) (1 + (9999 * sineValue));
            log.debug("Peak Time used to calculate Sleep at " +value + "ms)");
            return value;
        }
    }

}
