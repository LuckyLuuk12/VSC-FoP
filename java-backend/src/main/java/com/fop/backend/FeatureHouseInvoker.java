package com.fop.backend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FeatureHouseInvoker {

    public static String buildVariant(
            String configFilePath,
            String featuresFolderPath,
            String outputFolderPath) {
        File configFile = new File(configFilePath);
        File featuresFolder = new File(featuresFolderPath);

        if (!configFile.exists()) {
            return "Cannot find config file";
        }

        if (!featuresFolder.isDirectory()) {
            return "Cannot find features folder";
        }

        // Generate unique temporary folder name with timestamp-based UUID
        String tmpUuid = System.currentTimeMillis() + "_" + (int) (Math.random() * 100000);
        String tmpFilePath = featuresFolderPath + File.separator + tmpUuid + ".features";
        File tmpFile = new File(tmpFilePath);
        String tmpFolderPath = featuresFolderPath + File.separator + tmpUuid;

        System.out.println("Temp file path: " + tmpFilePath);
        System.out.println("Temp folder path: " + tmpFolderPath);

        try {
            ConfigHandler.makeFeatureFileFromConfig(
                    configFile, tmpFile);
        } catch (Exception e) {
            return "Error making the feature file:\n" + e;
        }

        // Call FeatureHouse directly instead of spawning a process
        try {
            // Create the temporary output directory if it doesn't exist
            File tmpFolder = new File(tmpFolderPath);
            if (!tmpFolder.exists()) {
                tmpFolder.mkdirs();
                System.out.println("Created temporary output directory: " + tmpFolderPath);
            }

            String[] fhArgs = {
                    "--expression", tmpFilePath,
                    "--base-directory", featuresFolderPath
            };

            System.out.println("Calling FeatureHouse with args:");
            System.out.println("  --expression: " + tmpFilePath);
            System.out.println("  --base-directory: " + featuresFolderPath);
            System.out.println("  Expected output directory: " + tmpFolderPath);

            // Invoke FeatureHouse main method
            composer.FSTGenComposer.main(fhArgs);

            // If we get here, FeatureHouse succeeded
            System.out.println("FeatureHouse completed successfully");

            // Check if tmp folder was created and has contents
            File tmpFolderCheck = new File(tmpFolderPath);
            if (tmpFolderCheck.exists()) {
                System.out.println("Temp folder exists: " + tmpFolderPath);
                File[] contents = tmpFolderCheck.listFiles();
                if (contents != null) {
                    System.out.println("Temp folder contains " + contents.length + " items:");
                    for (File f : contents) {
                        System.out.println("  - " + f.getName());
                    }
                } else {
                    System.out.println("WARNING: Temp folder is empty or cannot be read");
                }
            } else {
                System.out.println("WARNING: Temp folder does not exist: " + tmpFolderPath);
            }

            boolean tmpFileDeleted = tmpFile.delete();
            System.out.println("Deleted temp .features file: " + tmpFileDeleted);

            System.out.println("Starting move from " + tmpFolderPath + " to " + outputFolderPath);
            moveFolder(tmpFolderPath, outputFolderPath);

            return "Built Variant Successfully";

        } catch (Exception e) {
            // Build detailed error message
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("FeatureHouse error: ").append(e.getClass().getName());
            if (e.getMessage() != null) {
                errorMsg.append("\nMessage: ").append(e.getMessage());
            }
            if (e.getCause() != null) {
                errorMsg.append("\nCause: ").append(e.getCause().getMessage());
            }
            return errorMsg.toString();
        }
    }

    private static void moveFolder(String sourcePath, String targetPath) throws Exception {
        Path sourceDir = Paths.get(sourcePath).toAbsolutePath();
        Path targetDir = Paths.get(targetPath).toAbsolutePath();

        System.out.println("moveFolder called:");
        System.out.println("  Source: " + sourceDir);
        System.out.println("  Target: " + targetDir);
        System.out.println("  Source exists: " + Files.exists(sourceDir));
        System.out.println("  Target exists: " + Files.exists(targetDir));

        if (!Files.exists(targetDir)) {
            System.out.println("Creating target directory...");
            Files.createDirectories(targetDir);
        }

        // Give FeatureHouse time to release file handles (Windows file locking issue)
        System.out.println("Waiting for file locks to release...");
        Thread.sleep(500);

        // Retry logic for Windows file locks
        int maxRetries = 10;
        int retryDelay = 200; // 200 milliseconds

        try (var stream = Files.list(sourceDir)) {
            var files = stream.collect(java.util.stream.Collectors.toList());
            System.out.println("Found " + files.size() + " files/folders to move");

            if (files.isEmpty()) {
                System.out.println("WARNING: No files found in source directory!");
            }

            for (Path src : files) {
                boolean copied = false;
                for (int attempt = 1; attempt <= maxRetries && !copied; attempt++) {
                    try {
                        Path dest = targetDir.resolve(src.getFileName());
                        System.out.println("Attempting to copy: " + src + " -> " + dest);

                        // Use copy instead of move - works better with file locks
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                        copied = true;
                        System.out.println("✓ Successfully copied: " + src.getFileName());
                    } catch (IOException e) {
                        if (attempt < maxRetries) {
                            System.out.println("⚠ Retry " + attempt + "/" + maxRetries + " for " + src.getFileName()
                                    + " (file locked: " + e.getMessage() + ")");
                            Thread.sleep(retryDelay);
                        } else {
                            System.out.println(
                                    "✗ FAILED to copy " + src.getFileName() + " after " + maxRetries + " attempts");
                            throw new RuntimeException(
                                    "Failed to copy " + src.getFileName() + " after " + maxRetries + " attempts", e);
                        }
                    }
                }
            }
        }

        // Clean up temporary files after copying
        System.out.println("Cleaning up temporary files...");
        try (var cleanupStream = Files.list(sourceDir)) {
            var cleanupFiles = cleanupStream.collect(java.util.stream.Collectors.toList());
            for (Path file : cleanupFiles) {
                try {
                    Files.deleteIfExists(file);
                    System.out.println("✓ Deleted: " + file.getFileName());
                } catch (IOException e) {
                    System.out.println("⚠ Could not delete: " + file.getFileName() + " (" + e.getMessage() + ")");
                }
            }
        }

        System.out.println("Attempting to delete source directory: " + sourceDir);
        try {
            Files.delete(sourceDir);
            System.out.println("✓ Source directory deleted successfully");
        } catch (IOException e) {
            System.out.println("⚠ WARNING: Could not delete source directory: " + e.getMessage());
            System.out.println("   You may need to manually delete: " + sourceDir);
        }

        System.out.println("✓ Successfully completed copying files from " + sourcePath + " to " + targetPath);
    }

}
