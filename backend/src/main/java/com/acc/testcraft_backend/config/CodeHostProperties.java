package com.acc.testcraft_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "code-host")
public class CodeHostProperties {
    private final Github github = new Github();
    private final Bitbucket bitbucket = new Bitbucket();

    public Github getGithub() { return github; }
    public Bitbucket getBitbucket() { return bitbucket; }

    public static class Github {
        private String token = "";
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    public static class Bitbucket {
        private String token = "";
        private String username = "";
        private String appPassword = "";
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAppPassword() { return appPassword; }
        public void setAppPassword(String appPassword) { this.appPassword = appPassword; }
    }
}
