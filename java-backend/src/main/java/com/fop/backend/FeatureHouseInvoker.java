package com.fop.backend;

import java.io.*;
import java.nio.file.*;

public class FeatureHouseInvoker {

    public static String buildVariant(
            String configFilePath, 
            String featuresFolderPath, 
            String outputFolderPath) {
        
        if (!featuresFolderPath.endsWith("/")) {
            featuresFolderPath = featuresFolderPath + "/";
        }

        String tmpFilePath = featuresFolderPath + "tmp_58131bc547fb87af94cebdaf3102321f.features";
        String tmpFolderPath = featuresFolderPath + "tmp_58131bc547fb87af94cebdaf3102321f";

        ConfigHandler ch = new ConfigHandler();
        try {
            String out = ch.makeFeatureFileFromConfig(
                    configFilePath, tmpFilePath);
        } catch (Exception e) {
            return "Error making the feature file:\n" + e;
        }

        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", 
            "./lib/FeatureHouse.jar", 
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
                    deleteFile(tmpFilePath);
                    moveFolder(tmpFolderPath, outputFolderPath);
                    return "Built Variant Succesfully";
                case 1:
                    return "FH_FAILURE";
                default:
                    return "FH_UNKNOWN_ERRCODE";
            }
        } catch (Exception e) { 
            return "FH_INTERRUPTED:" + e;
        }
    }

    private static void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath();
            Files.delete(path);  

            System.out.println("removed: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    private static void moveFolder(String sourcePath, String targetPath) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
