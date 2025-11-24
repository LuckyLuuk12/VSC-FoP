import * as vscode from 'vscode';
import { FeatureTreeProvider } from './FeatureTreeProvider';
import { JavaBridge } from './JavaBridge';

export function activate(context: vscode.ExtensionContext) {
    // Initialize tree view provider
    const featureTreeProvider = new FeatureTreeProvider();
    vscode.window.registerTreeDataProvider('featureTreeView', featureTreeProvider);

    // Initialize Java bridge
    const javaBridge = new JavaBridge();

    // Register loadModel command
    const loadModelCommand = vscode.commands.registerCommand('fop.loadModel', async () => {
        // Scaffold only - no logic
    });

    // Register buildVariant command
    const buildVariantCommand = vscode.commands.registerCommand('fop.buildVariant', async () => {
        // Scaffold only - no logic
    });

    context.subscriptions.push(loadModelCommand, buildVariantCommand);
}

export function deactivate() {
    // Scaffold only - no logic
}
