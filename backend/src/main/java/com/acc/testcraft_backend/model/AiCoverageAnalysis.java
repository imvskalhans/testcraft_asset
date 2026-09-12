package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class AiCoverageAnalysis {

    private boolean mockMode;
    private String error;
    private String summary;
    private List<AiCoverageMapping> mappings = new ArrayList<>();
    private List<String> criticalGaps = new ArrayList<>();
    private List<AiRecommendedTestCase> recommendedTestCases = new ArrayList<>();

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AiCoverageMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<AiCoverageMapping> mappings) {
        this.mappings = mappings == null ? new ArrayList<>() : mappings;
    }

    public List<String> getCriticalGaps() {
        return criticalGaps;
    }

    public void setCriticalGaps(List<String> criticalGaps) {
        this.criticalGaps = criticalGaps == null ? new ArrayList<>() : criticalGaps;
    }

    public List<AiRecommendedTestCase> getRecommendedTestCases() {
        return recommendedTestCases;
    }

    public void setRecommendedTestCases(List<AiRecommendedTestCase> recommendedTestCases) {
        this.recommendedTestCases = recommendedTestCases == null ? new ArrayList<>() : recommendedTestCases;
    }
}
