package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class StoryTestCycle {

    private String storyKey;
    private String storySummary;
    private int totalTestCycles;
    private int totalTestCases;
    private List<TestCycle> testCycles = new ArrayList<>();

    public String getStoryKey() {
        return storyKey;
    }

    public void setStoryKey(String storyKey) {
        this.storyKey = storyKey;
    }

    public String getStorySummary() {
        return storySummary;
    }

    public void setStorySummary(String storySummary) {
        this.storySummary = storySummary;
    }

    public int getTotalTestCycles() {
        return totalTestCycles;
    }

    public void setTotalTestCycles(int totalTestCycles) {
        this.totalTestCycles = totalTestCycles;
    }

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(int totalTestCases) {
        this.totalTestCases = totalTestCases;
    }

    public List<TestCycle> getTestCycles() {
        return testCycles;
    }

    public void setTestCycles(List<TestCycle> testCycles) {
        this.testCycles = testCycles;
    }
}
