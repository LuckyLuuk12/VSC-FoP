package com.fop.backend;

import java.io.*;

public class FeatureHouseInvoker {

    public static String buildVariant(String configFile) {
        // TODO: parse config file and make a .feature file
        //      just xml parsing, order of composition top to bot
        // TODO: redirect output folder 
        //      make base-dir point towards project while parsed
        //      config file is generated where the files will be generated
        //      then call FH with base-dir and the generated .feature file
        // TODO: return the output folder path
        
        String line;
        Process process;
        int exitCode;

        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", 
            "./lib/FeatureHouse.jar", 
            "--expression", configFile
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
