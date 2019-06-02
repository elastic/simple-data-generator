package com.pahlsoft.simpledata.model;

import java.util.List;
import java.util.Map;

public class Workload {
    private String workloadName;
    private int workloadThreads;
    private int workloadSleep;
    private String indexName;
    private List<Map<String,Object>> fields;

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
}
