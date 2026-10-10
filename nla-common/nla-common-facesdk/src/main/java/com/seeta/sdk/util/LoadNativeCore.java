package com.seeta.sdk.util;

import com.seeta.sdk.SeetaDevice;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Loads externally installed libraries in manifest order, before any JNI call. */
public final class LoadNativeCore {
    private static final Set<Path> LOADED_LIBRARIES = new LinkedHashSet<>();
    private static List<Path> selectedLibraries;
    private static boolean loaded;
    private LoadNativeCore() { }

    public static synchronized void LOAD_NATIVE(String dllPath, SeetaDevice device) {
        if (dllPath == null || dllPath.isBlank()) throw new IllegalArgumentException("face.dll-path is required");
        List<Path> libraries = resolveLibraries(Path.of(dllPath), System.getProperty("os.name"),
            System.getProperty("os.arch"), device);
        if (selectedLibraries != null && !selectedLibraries.equals(libraries))
            throw new IllegalStateException("SeetaFace libraries/device cannot be changed after native loading starts");
        if (loaded) return;
        selectedLibraries = libraries;
        for (Path library : libraries) {
            if (LOADED_LIBRARIES.contains(library)) continue;
            try {
                System.load(library.toString());
                LOADED_LIBRARIES.add(library);
            } catch (UnsatisfiedLinkError error) {
                throw new IllegalStateException("Unable to load SeetaFace library: " + library, error);
            }
        }
        loaded = true;
    }

    public static synchronized boolean isLoaded() { return loaded; }

    /** Resolves and validates every library before loading the first one. */
    public static List<Path> resolveLibraries(Path root, String osName, String architecture, SeetaDevice device) {
        Objects.requireNonNull(root, "native root");
        Objects.requireNonNull(device, "device");
        String os = Objects.requireNonNull(osName, "os.name").toLowerCase(Locale.ROOT);
        String platform;
        if (os.startsWith("windows")) platform = "windows";
        else if (os.startsWith("linux")) platform = "linux";
        else throw new IllegalArgumentException("Unsupported SeetaFace OS: " + osName);
        String arch = architecture.toLowerCase(Locale.ROOT);
        if (Set.of("amd64", "x86_64", "x86-64", "x64").contains(arch)) arch = "amd64";
        else if (Set.of("aarch64", "arm64").contains(arch)) arch = "aarch64";
        else if (arch.startsWith("arm")) arch = "arm";
        else throw new IllegalArgumentException("Unsupported SeetaFace architecture: " + architecture);
        if (device == SeetaDevice.SEETA_DEVICE_GPU && !arch.equals("amd64"))
            throw new IllegalArgumentException("GPU libraries require amd64");
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path directory = Files.isRegularFile(normalizedRoot.resolve("dll.properties"))
            ? normalizedRoot : normalizedRoot.resolve(platform).resolve(arch);
        Properties manifest = new Properties();
        try (InputStream input = Files.newInputStream(directory.resolve("dll.properties"))) {
            manifest.load(input);
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to read native manifest: " + directory.resolve("dll.properties"), error);
        }
        List<String> base = new ArrayList<>(), jni = new ArrayList<>();
        for (String key : manifest.stringPropertyNames()) {
            if (key.matches("so\\.base\\.\\d+")) base.add(key);
            else if (key.matches("so\\.\\d+")) jni.add(key);
            else throw new IllegalArgumentException("Unknown native manifest entry: " + key);
        }
        if (base.isEmpty() || jni.isEmpty()) throw new IllegalArgumentException("Native manifest must contain base and JNI libraries");
        Comparator<String> order = Comparator.comparingInt(key -> Integer.parseInt(key.substring(key.lastIndexOf('.') + 1)));
        base.sort(order); jni.sort(order);
        List<Path> result = new ArrayList<>();
        String compute = device == SeetaDevice.SEETA_DEVICE_GPU ? "GPU" : "CPU";
        for (String key : base) {
            String name = manifest.getProperty(key).trim();
            Path parent = directory.resolve("base");
            if (name.toLowerCase(Locale.ROOT).contains("tennis")) parent = parent.resolve(compute);
            result.add(validateLibrary(directory, parent.resolve(name)));
        }
        for (String key : jni) result.add(validateLibrary(directory, directory.resolve(manifest.getProperty(key).trim())));
        return List.copyOf(result);
    }

    private static Path validateLibrary(Path directory, Path library) {
        Path path = library.toAbsolutePath().normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path) || !Files.isReadable(path))
            throw new IllegalArgumentException("Missing or invalid SeetaFace library: " + path);
        return path;
    }
}
