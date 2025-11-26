package com.fop.backend;

import java.io.*;

public class FeatureHouseInvoker {

    public static String buildVariant(String configFile) {
        // TODO: parse config file and make a .feature file
        //      just xml parsing, order of composition top to bot generate the 
        //      .feature file in the folder we want the composed code to live.
        // TODO: redirect output folder 
        //      make base-dir point towards the folder with the features
        //      then call FH with base-dir and the generated .feature file
        //      this should generate the folder named A with the generated code
        //      when given A.feature
        // TODO: return the output folder path
        //      (I think this is to display it to the user)
        
        String line;
        Process process;
        int exitCode;
        String featureFile = "../test-configs/test.features";
        ConfigHandler ch = new ConfigHandler();
        String out = ch.makeFeatureFileFromConfig(configFile, featureFile);
        System.out.println(out);

        //Right now this does not do anything as the baseDir does not point towards assignment-6, will fix shortly
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", 
            "./lib/FeatureHouse.jar", 
            "--expression", featureFile
            //,"--base-dir", baseDir
        );

        pb.redirectErrorStream(true); // merge stdout & stderr
        
        try {
            process = pb.start();
            // Read output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
        
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            //TODO catch
            return "FH_IO_ERROR:" + e;
        }                            


        try {
            exitCode = process.waitFor();
            switch (exitCode) {
                case 0:
                    return "FH_SUCCESS";
                case 1:
                    return "FH_FAILURE";
                default:
                    return "FH_UNKNOWN_ERRCODE: " + exitCode;
            }
        } catch (InterruptedException e) { 
            return "FH_INTERRUPTED";
        }
    }
}
