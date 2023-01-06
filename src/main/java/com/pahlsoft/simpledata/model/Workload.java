package com.pahlsoft.simpledata.model;

import java.util.List;
import java.util.Map;

public class Workload {
    private String workloadName;
    private int workloadThreads;
    private int workloadSleep;
    private String indexName;
    private List<Map<String,Object>> fields;

    private int backendBulkQueueDepth;

    private Boolean purgeOnStart;

    private int primaryShardCount;
    private int replicaShardCount;

    public Workload() {
    }

    public int getPrimaryShardCount() {
        return primaryShardCount;
    }

    public void setPrimaryShardCount(int primaryShardCount) {
        this.primaryShardCount = primaryShardCount;
    }

    public int getReplicaShardCount() {
        return replicaShardCount;
    }

    public void setReplicaShardCount(int replicaShardCount) {
        this.replicaShardCount = replicaShardCount;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public void setWorkloadName(String workloadName) {
        this.workloadName = workloadName;
    }

    public int getWorkloadThreads() {
        return workloadThreads;
    }

    public void setWorkloadThreads(int workloadThreads) {
        this.workloadThreads = workloadThreads;
    }

    public int getWorkloadSleep() {
        return workloadSleep;
    }

    public void setWorkloadSleep(int workloadSleep) {
        this.workloadSleep = workloadSleep;
    }

    public List<Map<String, Object>> getFields() {
        return fields;
    }

    public void setFields(List<Map<String, Object>> fields) {
        this.fields = fields;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public int getBackendBulkQueueDepth() {
        return backendBulkQueueDepth;
    }

    public void setBackendBulkQueueDepth(int backendBulkQueueDepth) {
        this.backendBulkQueueDepth = backendBulkQueueDepth;
    }

    public Boolean getPurgeOnStart() {
        return purgeOnStart;
    }

    public void setPurgeOnStart(Boolean purgeOnStart) {
        this.purgeOnStart = purgeOnStart;
    }
}
