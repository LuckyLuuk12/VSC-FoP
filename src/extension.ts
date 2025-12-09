import * as vscode from 'vscode';
import { FeatureTreeProvider } from './FeatureTreeProvider';
import { FeatureTreeVisualization } from './FeatureTreeVisualization';
import { JavaBridge } from './JavaBridge';
import * as path from 'path';
import * as fs from 'fs';

export async function activate(context: vscode.ExtensionContext) {
    let currentModelPath: string | undefined;

    //  Workspace validation 
    const workspace = vscode.workspace.workspaceFolders?.[0];
    if (!workspace) {
        vscode.window.showErrorMessage("Open a workspace folder to use this extension.");
        return;
    }

    const hasJavaFiles = (await vscode.workspace.findFiles("**/*.java", "**/node_modules/**")).length > 0;
    if (!hasJavaFiles) {
        console.log("[FOP] No Java files detected in workspace.");
    }

    const gradle = (await vscode.workspace.findFiles("**/build.gradle")).length > 0;
    const maven = (await vscode.workspace.findFiles("**/pom.xml")).length > 0;

    if (!gradle && !maven && hasJavaFiles) {
        vscode.window.showWarningMessage("No Gradle or Maven build found. Some FOP tasks may not work.");
    }

    //  Java backend bridge 
    const javaBridge = new JavaBridge(
        path.join(context.extensionPath, "java-backend", "build", "libs", "backend-1.0.0.jar")
    );

    //  Tree provider 
    const featureTreeProvider = new FeatureTreeProvider();
    vscode.window.registerTreeDataProvider('featureTreeView', featureTreeProvider);
    
    //  Tree visualization webview
    const treeVisualization = new FeatureTreeVisualization(context.extensionPath, javaBridge);

    // Helper function to detect and load FOP model
    async function detectAndLoadModel(): Promise<boolean> {
        // Look for model.xml in workspace root
        console.log("[FOP] Searching for model.xml in workspace root...");
        const modelFiles = await vscode.workspace.findFiles("model.xml", null);
        
        console.log(`[FOP] Found ${modelFiles.length} model.xml file(s)`);
        if (modelFiles.length === 0) {
            console.log("[FOP] No model.xml found in workspace root");
            return false;
        }
        
        modelFiles.forEach((file, index) => {
            console.log(`[FOP] Model ${index + 1}: ${file.fsPath}`);
        });

        try {
            const modelPath = modelFiles[0].fsPath;
            console.log(`[FOP] Loading model from: ${modelPath}`);
            const modelData = await javaBridge.loadModel(modelPath);
            
            if (modelData.status === "ok") {
                currentModelPath = modelPath;
                featureTreeProvider.setModel(modelData);
                treeVisualization.setModel(modelData, modelPath);
                treeVisualization.show();
                vscode.window.showInformationMessage(`FOP model loaded from ${path.basename(path.dirname(modelPath))}`);
                return true;
            } else {
                vscode.window.showErrorMessage(`Failed to load model: ${modelData.message}`);
                return false;
            }
        } catch (error) {
            console.error("[FOP] Error loading model:", error);
            vscode.window.showErrorMessage(`Error loading model: ${error}`);
            return false;
        }
    }

    // Auto-load model on activation
    await detectAndLoadModel();

    //  Commands 
    const loadModel = vscode.commands.registerCommand("fop.loadModel", async () => {
        const file = await vscode.window.showOpenDialog({
            canSelectFiles: true,
            canSelectFolders: false,
            filters: { XML: ["xml"], JSON: ["json"] },
            title: "Select Feature Model"
        });
        if (!file) return;

        try {
            const modelData = await javaBridge.loadModel(file[0].fsPath);
            
            if (modelData.status === "ok") {
                currentModelPath = file[0].fsPath;
                featureTreeProvider.setModel(modelData);
                treeVisualization.setModel(modelData, file[0].fsPath);
                vscode.window.showInformationMessage("Model loaded successfully.");
            } else {
                vscode.window.showErrorMessage(`Failed to load model: ${modelData.message}`);
            }
        } catch (error) {
            vscode.window.showErrorMessage(`Error loading model: ${error}`);
        }
    });

    const refreshModel = vscode.commands.registerCommand("fop.refreshModel", async () => {
        if (currentModelPath) {
            try {
                const modelData = await javaBridge.loadModel(currentModelPath);

                if (modelData.status === "ok") {
                    featureTreeProvider.setModel(modelData);
                    treeVisualization.setModel(modelData, currentModelPath);
                    vscode.window.showInformationMessage("Model reloaded successfully.");
                } else {
                    vscode.window.showErrorMessage(`Failed to reload model: ${modelData.message}`);
                }
            } catch (error) {
                vscode.window.showErrorMessage(`Error reloading model: ${error}`);
            }
        } else {
            const loaded = await detectAndLoadModel();
            if (!loaded) {
                vscode.window.showWarningMessage("No model.xml found in workspace. Use 'Load Feature Model' to select manually.");
            }
        }
    });

    const showTreeVisualization = vscode.commands.registerCommand("fop.showTreeVisualization", () => {
        treeVisualization.show();
    });

    const buildVariant = vscode.commands.registerCommand("fop.buildVariant", async () => {
        
        if (!currentModelPath) {
            const file = await vscode.window.showOpenDialog({
                canSelectFiles: true,
                canSelectFolders: false,
                filters: { XML: ["xml"], JSON: ["json"] },
                title: "Select Feature Model"
            });
            if (!file) return;
            currentModelPath = file[0].fsPath;
        }

        const featfolder = await vscode.window.showOpenDialog({
            canSelectFiles: false,
            canSelectFolders: true,
            title: "Select Features Folder"
        });
        if (!featfolder) return;
        const featureFolder = featfolder[0].fsPath;

        const outfolder = await vscode.window.showOpenDialog({
            canSelectFiles: false,
            canSelectFolders: true,
            title: "Select Output Folder"
        });
        if (!outfolder) return;
        const outputFolder = outfolder[0].fsPath;

        try {
            console.log(`building model: ${currentModelPath}\nfeature folder: ${featureFolder}\noutputFolder: ${outputFolder}` );
            const result = await javaBridge.call(["buildVariant", currentModelPath, featureFolder, outputFolder]);
            vscode.window.showInformationMessage("Backend result:\n" + result);
        } catch (error) {
            vscode.window.showErrorMessage(`Error building variant: ${error}`);
        }
    });

    context.subscriptions.push(loadModel, refreshModel, showTreeVisualization, buildVariant);
}

export function deactivate() {}
