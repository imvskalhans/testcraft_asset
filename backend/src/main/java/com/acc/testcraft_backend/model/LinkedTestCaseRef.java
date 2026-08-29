package com.acc.testcraft_backend.model;

public class LinkedTestCaseRef {

    private String key;
    private String name;
    private String status;
    private String url;
    private boolean inCycle;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isInCycle() {
        return inCycle;
    }

    public void setInCycle(boolean inCycle) {
        this.inCycle = inCycle;
    }
}
