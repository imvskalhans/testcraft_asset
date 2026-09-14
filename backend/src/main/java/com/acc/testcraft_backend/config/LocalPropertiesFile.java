package com.acc.testcraft_backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Updates a single key in application-local.properties without touching other values. */
public final class LocalPropertiesFile {

    private static final String FILE_NAME = "application-local.properties";
    private static final List<Path> CANDIDATES = List.of(
            Path.of("src/main/resources").resolve(FILE_NAME),
            Path.of("backend/src/main/resources").resolve(FILE_NAME)
    );

    private LocalPropertiesFile() {}

    public static Path resolve() {
        for (Path path : CANDIDATES) {
            if (Files.isRegularFile(path)) {
                return path.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    public static boolean setProperty(String key, String value) throws IOException {
        Path path = resolve();
        if (path == null) {
            return false;
        }
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("(?m)^(\\s*" + Pattern.quote(key) + "\\s*=)([^\\r\\n#]*)(.*)$");
        Matcher matcher = pattern.matcher(content);
        String updated;
        if (matcher.find()) {
            String comment = matcher.group(3) == null ? "" : matcher.group(3);
            updated = matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + value + comment));
        } else {
            String suffix = content.endsWith("\n") || content.isEmpty() ? "" : "\n";
            updated = content + suffix + key + "=" + value + "\n";
        }
        Files.writeString(path, updated, StandardCharsets.UTF_8);
        return true;
    }
}
