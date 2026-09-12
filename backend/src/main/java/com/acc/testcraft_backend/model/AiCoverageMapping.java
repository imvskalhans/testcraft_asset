package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class AiCoverageMapping {

    private String requirement;
    private String coverage;
    private List<String> testCases = new ArrayList<>();
    private String notes;

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public List<String> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<String> testCases) {
        this.testCases = testCases == null ? new ArrayList<>() : testCases;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
