package com.fop.backend;

import java.io.*;
import java.nio.file.*;

public class FeatureHouseInvoker {

    public static String buildVariant(
            String configFilePath, 
            String featuresFolderPath, 
            String outputFolderPath) {
        File configFile     = new File(configFilePath);
        File featuresFolder = new File(featuresFolderPath);
        File outputFolder   = new File(outputFolderPath);

        if(!configFile.exists()) {
            return "Cannot find config file";
        }

        if(!featuresFolder.isDirectory()) {
            return "Cannot find features folder";
        }
        

        String tmpFilePath = featuresFolderPath + "/tmp_58131bc547fb87af94cebdaf3102321f.features";
        File tmpFile = new File(tmpFilePath);
        String tmpFolderPath = featuresFolderPath + "/tmp_58131bc547fb87af94cebdaf3102321f";

        ConfigHandler ch = new ConfigHandler();
        try {
            String out = ch.makeFeatureFileFromConfig(
                    configFile, tmpFile);
        } catch (Exception e) {
            return "Error making the feature file:\n" + e;
        }

        Path test = Paths.get("../java-backend/lib/FeatureHouse.jar");
        System.out.println(test.toAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", 
            "../java-backend/lib/FeatureHouse.jar", 
            "--expression", tmpFilePath,
            "--base-directory", featuresFolderPath
        );

        pb.redirectErrorStream(true); // merge stdout & stderr
        Process process;
        try {
            process = pb.start();
            // Read output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line; 
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            return "FH_IO_ERROR:" + e;
        }                            


        try {
            switch (process.waitFor()) {
                case 0:
                    tmpFile.delete();
                    moveFolder(tmpFolderPath, outputFolderPath);
                    return "Built Variant Succesfully";
                case 1:
                    return "Feature House failed with error code 1.";
                default:
                    return "FH_UNKNOWN_ERRCODE";
            }
        } catch (Exception e) { 
            return "Feature House was interrupted:" + e;
        }
    }

    private static void moveFolder(String sourcePath, String targetPath) throws Exception {
        Path sourceDir = Paths.get(sourcePath).toAbsolutePath();
        Path targetDir = Paths.get(targetPath).toAbsolutePath();
        
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try (var stream = Files.list(sourceDir)) {
            stream.forEach(src -> {
                try {
                    Files.move(
                        src,
                        targetDir.resolve(src.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Files.delete(sourceDir);

        System.out.println(sourcePath + " -> " + targetPath);
    }

}
