package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class CycleTraceability {

    private String id;
    private String key;
    private String name;
    private String status;
    private String url;
    private int executionCount;
    private int passedCount;
    private int failedCount;
    private int blockedCount;
    private int notExecutedCount;
    private List<TestExecutionRef> executions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public int getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }
    public int getNotExecutedCount() { return notExecutedCount; }
    public void setNotExecutedCount(int notExecutedCount) { this.notExecutedCount = notExecutedCount; }

    public List<TestExecutionRef> getExecutions() {
        return executions;
    }

    public void setExecutions(List<TestExecutionRef> executions) {
        this.executions = executions;
    }
}
