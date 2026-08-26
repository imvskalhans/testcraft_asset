package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class CreateTestCyclesRequest {

    private String crKey;
    private boolean createFolders;
    private String cycleType;
    private List<String> cycleTypes;
    private String owner;

    public String getCrKey() {
        return crKey;
    }

    public void setCrKey(String crKey) {
        this.crKey = crKey;
    }

    public boolean isCreateFolders() {
        return createFolders;
    }

    public void setCreateFolders(boolean createFolders) {
        this.createFolders = createFolders;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public List<String> getCycleTypes() {
        return cycleTypes;
    }

    public void setCycleTypes(List<String> cycleTypes) {
        this.cycleTypes = cycleTypes;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> resolveCycleTypes() {
        List<String> types = new ArrayList<>();
        if (cycleTypes != null) {
            for (String type : cycleTypes) {
                if (type != null && !type.isBlank() && !types.contains(type.trim())) {
                    types.add(type.trim());
                }
            }
        }
        if (cycleType != null && !cycleType.isBlank() && !types.contains(cycleType.trim())) {
            types.add(cycleType.trim());
        }
        if (types.isEmpty()) {
            types.add("Functional");
        }
        return types;
    }
}
