package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class GetTestCyclesResponse {

    private boolean success;
    private String crKey;
    private int totalLinkedStories;
    private int storiesWithCycles;
    private List<StoryTestCycle> storyTestCycles = new ArrayList<>();
    private String message;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCrKey() {
        return crKey;
    }

    public void setCrKey(String crKey) {
        this.crKey = crKey;
    }

    public int getTotalLinkedStories() {
        return totalLinkedStories;
    }

    public void setTotalLinkedStories(int totalLinkedStories) {
        this.totalLinkedStories = totalLinkedStories;
    }

    public int getStoriesWithCycles() {
        return storiesWithCycles;
    }

    public void setStoriesWithCycles(int storiesWithCycles) {
        this.storiesWithCycles = storiesWithCycles;
    }

    public List<StoryTestCycle> getStoryTestCycles() {
        return storyTestCycles;
    }

    public void setStoryTestCycles(List<StoryTestCycle> storyTestCycles) {
        this.storyTestCycles = storyTestCycles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
