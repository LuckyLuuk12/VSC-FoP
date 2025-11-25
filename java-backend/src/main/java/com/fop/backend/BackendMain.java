package com.fop.backend;

public class BackendMain {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No command given.");
            return;
        }

        String command = args[0];

        switch (command) {
            case "loadModel":
                if (args.length < 2) {
                    System.out.println("Missing model path.");
                    return;
                }
                String modelPath = args[1];
                String modelJson = ModelHandler.loadModel(modelPath);
                System.out.println(modelJson);
                break;

            case "saveModel":
                if (args.length < 3) {
                    System.out.println("Missing model path or data.");
                    return;
                }
                String savePath = args[1];
                String jsonData = args[2];
                String saveResult = ModelHandler.saveModel(savePath, jsonData);
                System.out.println(saveResult);
                break;

            case "buildVariant":
                if (args.length < 2) {
                    System.out.println("Missing config file");
                    return;
                }
                String configFile = args[1];
                String result = FeatureHouseInvoker.buildVariant(configFile);
                System.out.println(result);
                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }
}
