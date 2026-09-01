package com.acc.testcraft_backend.model;

public class AiAttachment {
    private String name;
    private String mimeType;
    private String data;
    private String url;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
