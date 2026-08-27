package com.acc.testcraft_backend.model;

public class JenkinsLogResponse {
    private boolean success;
    private String jobUrl;
    private String buildUrl;
    private String jobName;
    private String buildNumber;
    private String buildResult;
    private String buildTimestamp;
    private String duration;
    private int totalLines;
    private int chunksAnalyzed;
    private String summary;
    private boolean mockMode;
    private String error;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String value) { jobUrl = value; }
    public String getBuildUrl() { return buildUrl; }
    public void setBuildUrl(String value) { buildUrl = value; }
    public String getJobName() { return jobName; }
    public void setJobName(String value) { jobName = value; }
    public String getBuildNumber() { return buildNumber; }
    public void setBuildNumber(String value) { buildNumber = value; }
    public String getBuildResult() { return buildResult; }
    public void setBuildResult(String value) { buildResult = value; }
    public String getBuildTimestamp() { return buildTimestamp; }
    public void setBuildTimestamp(String value) { buildTimestamp = value; }
    public String getDuration() { return duration; }
    public void setDuration(String value) { duration = value; }
    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int value) { totalLines = value; }
    public int getChunksAnalyzed() { return chunksAnalyzed; }
    public void setChunksAnalyzed(int value) { chunksAnalyzed = value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary = value; }
    public boolean isMockMode() { return mockMode; }
    public void setMockMode(boolean value) { mockMode = value; }
    public String getError() { return error; }
    public void setError(String value) { error = value; }
}
