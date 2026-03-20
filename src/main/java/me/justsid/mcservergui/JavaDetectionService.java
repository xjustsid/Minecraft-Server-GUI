package me.justsid.mcservergui;

import static me.justsid.mcservergui.OsUtils.IS_MAC;
import static me.justsid.mcservergui.OsUtils.IS_WINDOWS;
import static me.justsid.mcservergui.OsUtils.JAVA_EXE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(JavaDetectionService.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?:java|openjdk) version \"([^\"]+)\"");

    public record JavaCandidate(Path executable, String version, boolean compatible) {}

    public List<JavaCandidate> detect() {
        Set<Path> candidates = new LinkedHashSet<>();

        Path runningJava = Paths.get(System.getProperty("java.home"), "bin", JAVA_EXE);
        if (Files.isRegularFile(runningJava)) {
            candidates.add(runningJava);
        }

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            Path javaExecutable = Paths.get(javaHome, "bin", JAVA_EXE);
            if (Files.isRegularFile(javaExecutable)) {
                candidates.add(javaExecutable);
            }
        }

        for (String commonPath : getCommonJavaPaths()) {
            Path candidate = Paths.get(commonPath);
            if (Files.isRegularFile(candidate)) {
                candidates.add(candidate);
            }
        }

        candidates.addAll(findFromPath());

        List<JavaCandidate> detected = new ArrayList<>();
        for (Path candidate : candidates) {
            JavaCandidate javaCandidate = toCandidate(candidate);
            if (javaCandidate != null) {
                detected.add(javaCandidate);
            }
        }
        return detected;
    }

    public Optional<JavaCandidate> detectBestCandidate() {
        return detect().stream()
            .sorted((left, right) -> Integer.compare(parseMajor(right.version()), parseMajor(left.version())))
            .findFirst();
    }

    public boolean isCompatible(Path javaExecutable) {
        JavaCandidate candidate = toCandidate(javaExecutable);
        return candidate != null && candidate.compatible();
    }

    public boolean isCompatible(String javaCommand) {
        if (javaCommand == null || javaCommand.isBlank()) {
            return false;
        }

        try {
            String version = readJavaVersion(javaCommand.trim());
            return version != null && parseMajor(version) >= 17;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.debug("Konnte Java-Kommando nicht prüfen: {}", javaCommand, e);
            return false;
        }
    }

    private JavaCandidate toCandidate(Path javaExec) {
        try {
            Path normalized = javaExec.toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalized)) {
                return null;
            }
            String version = readJavaVersion(normalized);
            if (version == null) {
                return null;
            }
            return new JavaCandidate(normalized, version, parseMajor(version) >= 17);
        } catch (Exception e) {
            logger.debug("Konnte Java-Kandidat nicht auswerten: {}", javaExec, e);
            return null;
        }
    }

    private String readJavaVersion(Path javaExec) throws IOException, InterruptedException {
        return readJavaVersion(javaExec.toString());
    }

    private String readJavaVersion(String javaCommand) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(javaCommand, "-version")
            .redirectErrorStream(true)
            .start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        process.waitFor();
        Matcher matcher = VERSION_PATTERN.matcher(output.toString());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int parseMajor(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }

        String normalized = version.toLowerCase(Locale.ROOT).replace("_", ".");
        if (normalized.startsWith("1.")) {
            String[] parts = normalized.split("\\.");
            return parts.length > 1 ? parseInt(parts[1]) : 0;
        }

        int delimiter = normalized.indexOf('.');
        String major = delimiter >= 0 ? normalized.substring(0, delimiter) : normalized;
        return parseInt(major);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Path> findFromPath() {
        List<Path> result = new ArrayList<>();
        String[] command = IS_WINDOWS ? new String[] {"where", "java"} : new String[] {"which", "java"};
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        result.add(Paths.get(line.trim()));
                    }
                }
            }
            process.waitFor();
        } catch (IOException e) {
            logger.debug("Java-Suche über PATH nicht verfügbar", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    private String[] getCommonJavaPaths() {
        if (IS_WINDOWS) {
            return new String[] {
                "C:\\Program Files\\Java\\jdk-23\\bin\\java.exe",
                "C:\\Program Files\\Java\\jdk-22\\bin\\java.exe",
                "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe",
                "C:\\Program Files\\Java\\jdk-17\\bin\\java.exe"
            };
        }

        if (IS_MAC) {
            return new String[] {
                "/usr/bin/java",
                "/Library/Java/JavaVirtualMachines/jdk-23.jdk/Contents/Home/bin/java",
                "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java",
                "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/java"
            };
        }

        return new String[] {
            "/usr/bin/java",
            "/usr/lib/jvm/java-23-openjdk-amd64/bin/java",
            "/usr/lib/jvm/java-21-openjdk-amd64/bin/java",
            "/usr/lib/jvm/java-17-openjdk-amd64/bin/java",
            "/usr/lib/jvm/java-21-openjdk/bin/java",
            "/usr/lib/jvm/java-17-openjdk/bin/java"
        };
    }
}