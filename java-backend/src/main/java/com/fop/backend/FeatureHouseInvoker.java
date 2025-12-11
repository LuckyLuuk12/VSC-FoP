package com.fop.backend;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FeatureHouseInvoker {

    public static String buildVariant(
            String configFilePath,
            String featuresFolderPath,
            String outputFolderPath) {
        File configFile = new File(configFilePath);
        File featuresFolder = new File(featuresFolderPath);

        Path outputFolderPathObj = Paths.get(outputFolderPath);
        outputFolderPath = outputFolderPathObj.toAbsolutePath().toString();
        int i = outputFolderPath.lastIndexOf(File.separator);
        System.out.println("Path Seperator: " + File.separator + " outputFolderPath: " + outputFolderPath);
        System.out.println("Output path split at index: " + i );
        String outputPath = outputFolderPath.substring(0, i);
        System.out.println("Output path: " + outputPath);
        String outputFolder = outputFolderPath.substring(i);
        System.out.println("Output folder: " + outputFolder);

        if (!configFile.exists()) {
            return "Cannot find config file";
        }

        if (!featuresFolder.isDirectory()) {
            return "Cannot find features folder";
        }
        File outputPathFile = new File(outputPath);
        if (!outputPathFile.exists()) {
            outputPathFile.mkdirs();
        }

        // Generate unique temporary folder name with timestamp-based UUID
        String tmpFilePath = outputFolderPath + ".features";
        File tmpFile = new File(tmpFilePath);

        System.out.println("Temp file path: " + tmpFilePath);

        try {
            ConfigHandler.makeFeatureFileFromConfig(
                    configFile, tmpFile);
        } catch (Exception e) {
            return "Error making the feature file:\n" + e;
        }

        // Call FeatureHouse directly instead of spawning a process
        try {

            String[] fhArgs = {
                    "--expression", tmpFilePath,
                    "--base-directory", featuresFolderPath,
                    "--output-directory", outputPath
            };

            System.out.println("Calling FeatureHouse with args:");
            System.out.println("  --expression: " + tmpFilePath);
            System.out.println("  --base-directory: " + featuresFolderPath);
            System.out.println("  --output-directory: " + outputPath);

            // Invoke FeatureHouse main method
            composer.FSTGenComposer.main(fhArgs);

            // If we get here, FeatureHouse succeeded
            System.out.println("FeatureHouse completed successfully");

            boolean tmpFileDeleted = tmpFile.delete();
            System.out.println("Deleted temp .features file: " + tmpFileDeleted);

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
}
