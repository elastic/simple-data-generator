package com.pahlsoft.simpledata.model;

import java.util.List;

public class Configuration {
    private String backendType;
    private String backendScheme;
    private String backendHost;
    private int backendPort;
    private String backendUser;
    private String backendPassword;
    private Boolean backendApiKeyEnabled;
    private String backendApiKeyId;
    private String backendApiKeySecret;
    private String keystoreLocation;
    private String keystorePassword;
    private List<Workload> workloads;

    public String getBackendScheme() {
        return backendScheme;
    }

    public void setBackendScheme(String backendScheme) {
        this.backendScheme = backendScheme;
    }

    public String getBackendHost() {
        return backendHost;
    }

    public void setBackendHost(String backendHost) {
        this.backendHost = backendHost;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public void setBackendPort(int backendPort) {
        this.backendPort = backendPort;
    }

    public String getBackendUser() {
        return backendUser;
    }

    public void setBackendUser(String backendUser) {
        this.backendUser = backendUser;
    }

    public String getBackendPassword() {
        return backendPassword;
    }

    public void setBackendPassword(String backendPassword) {
        this.backendPassword = backendPassword;
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

    public String getBackendApiKeySecret() {
        return backendApiKeySecret;
    }

    public void setBackendApiKeySecret(String backendApiKeySecret) {
        this.backendApiKeySecret = backendApiKeySecret;
    }

    public String getBackendApiKeyId() {
        return backendApiKeyId;
    }

    public void setBackendApiKeyId(String backendApiKeyId) {
        this.backendApiKeyId = backendApiKeyId;
    }

    public Boolean getBackendApiKeyEnabled() {
        return backendApiKeyEnabled;
    }

    public void setBackendApiKeyEnabled(Boolean backendApiKeyEnabled) {
        this.backendApiKeyEnabled = backendApiKeyEnabled;
    }

    public String getBackendType() {
        return backendType;
    }

    public void setBackendType(String backendType) {
        this.backendType = backendType;
    }
}
