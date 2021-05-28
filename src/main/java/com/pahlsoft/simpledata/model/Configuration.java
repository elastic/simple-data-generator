package com.pahlsoft.simpledata.model;

import java.util.List;

public class Configuration {
    private String elasticsearchScheme;
    private String elasticsearchHost;
    private int elasticsearchPort;
    private String elasticsearchUser;
    private String elasticsearchPassword;
    private Boolean elasticsearchApiKeyEnabled;
    private String elasticsearchApiKeyId;
    private String elasticsearchApiKeySecret;
    private String keystoreLocation;
    private String keystorePassword;
    private List<Workload> workloads;

    public String getElasticsearchScheme() {
        return elasticsearchScheme;
    }

    public void setElasticsearchScheme(String elasticsearchScheme) {
        this.elasticsearchScheme = elasticsearchScheme;
    }

    public String getElasticsearchHost() {
        return elasticsearchHost;
    }

    public void setElasticsearchHost(String elasticsearchHost) {
        this.elasticsearchHost = elasticsearchHost;
    }

    public int getElasticsearchPort() {
        return elasticsearchPort;
    }

    public void setElasticsearchPort(int elasticsearchPort) {
        this.elasticsearchPort = elasticsearchPort;
    }

    public String getElasticsearchUser() {
        return elasticsearchUser;
    }

    public void setElasticsearchUser(String elasticsearchUser) {
        this.elasticsearchUser = elasticsearchUser;
    }

    public String getElasticsearchPassword() {
        return elasticsearchPassword;
    }

    public void setElasticsearchPassword(String elasticsearchPassword) {
        this.elasticsearchPassword = elasticsearchPassword;
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public void setKeystoreLocation(String keystoreLocation) {
        this.keystoreLocation = keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }


    public List<Workload> getWorkloads() {
        return workloads;
    }

    public void setWorkloads(List<Workload> workloads) {
        this.workloads = workloads;
    }

    public String getElasticsearchApiKeySecret() {
        return elasticsearchApiKeySecret;
    }

    public void setElasticsearchApiKeySecret(String elasticsearchApiKeySecret) {
        this.elasticsearchApiKeySecret = elasticsearchApiKeySecret;
    }

    public String getElasticsearchApiKeyId() {
        return elasticsearchApiKeyId;
    }

    public void setElasticsearchApiKeyId(String elasticsearchApiKeyId) {
        this.elasticsearchApiKeyId = elasticsearchApiKeyId;
    }

    public Boolean getElasticsearchApiKeyEnabled() {
        return elasticsearchApiKeyEnabled;
    }

    public void setElasticsearchApiKeyEnabled(Boolean elasticsearchApiKeyEnabled) {
        this.elasticsearchApiKeyEnabled = elasticsearchApiKeyEnabled;
    }
}
