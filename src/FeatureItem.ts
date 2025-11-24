import * as vscode from 'vscode';

export class FeatureItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState
    ) {
        super(label, collapsibleState);
        // Scaffold only - no logic
    }
}
