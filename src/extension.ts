import * as vscode from 'vscode';
import { FeatureTreeProvider } from './FeatureTreeProvider';
import { JavaBridge } from './JavaBridge';
import * as path from 'path';

export async function activate(context: vscode.ExtensionContext) {
    //  Workspace validation 
    const workspace = vscode.workspace.workspaceFolders?.[0];
    if (!workspace) {
        vscode.window.showErrorMessage("Open a workspace folder to use this extension.");
        return;
    }

    const hasJavaFiles = (await vscode.workspace.findFiles("**/*.java", "**/node_modules/**")).length > 0;
    if (!hasJavaFiles) {
        vscode.window.showWarningMessage("No Java files detected. FOP tools only apply to Java projects.");
        return;
    }

    const gradle = (await vscode.workspace.findFiles("**/build.gradle")).length > 0;
    const maven = (await vscode.workspace.findFiles("**/pom.xml")).length > 0;

    if (!gradle && !maven) {
        vscode.window.showWarningMessage("No Gradle or Maven build found. Some FOP tasks may not work.");
    }

    //  Tree provider 
    const featureTreeProvider = new FeatureTreeProvider();
    vscode.window.registerTreeDataProvider('featureTreeView', featureTreeProvider);

    //  Java backend bridge 
    const javaBridge = new JavaBridge(
        path.join(context.extensionPath, "java-backend", "build", "libs", "backend.jar")
    );

    //  Commands 
    const loadModel = vscode.commands.registerCommand("fop.loadModel", async () => {
        const file = await vscode.window.showOpenDialog({
            canSelectFiles: true,
            canSelectFolders: false,
            filters: { XML: ["xml"], JSON: ["json"] },
            title: "Select Feature Model"
        });
        if (!file) return;

        const result = await javaBridge.call(["loadModel", file[0].fsPath]);
        featureTreeProvider.refreshWithModel(result);
        vscode.window.showInformationMessage("Model loaded.");
    });

    const buildVariant = vscode.commands.registerCommand("fop.buildVariant", async () => {
        const result = await javaBridge.call(["buildVariant"]);
        vscode.window.showInformationMessage("Variant built:\n" + result);
    });

    context.subscriptions.push(loadModel, buildVariant);
}

export function deactivate() {}
