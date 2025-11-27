package com.fop.backend;

import java.io.*;
import java.nio.file.*;

public class FeatureHouseInvoker {

    public static String buildVariant(
            String configFilePath, 
            String featuresFolderPath, 
            String outputFolderPath) {
        // TODO: Work on output folder: we need to move the directory that is created in the featuresFolderPath to the specified outputFolderPath
        
        
        if (!featuresFolderPath.endsWith("/")) {
            featuresFolderPath = featuresFolderPath + "/";
        }

        // Can change name of output folder by changing output here:
        String tmpFilePath = featuresFolderPath + "tmp.features";
        //String tmpFolderPath = featuresFolderPath + "tmp";

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
            //TODO catch
            return "FH_IO_ERROR:" + e;
        }                            


        try {
            switch (process.waitFor()) {
                case 0:
                    //moveFolder(tmpFolderPath, outputFolderPath);
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

}
