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
                String modelJson = ModelLoader.loadModel(modelPath);
                System.out.println(modelJson);
                break;

            case "buildVariant":
                String result = FeatureHouseInvoker.buildVariant();
                System.out.println(result);
                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }
}
