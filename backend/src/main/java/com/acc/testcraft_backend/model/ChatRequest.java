package com.acc.testcraft_backend.model;

public class ChatRequest {
    private String message;
    private ChatWorkspaceContext context;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ChatWorkspaceContext getContext() { return context; }
    public void setContext(ChatWorkspaceContext context) { this.context = context; }
}
