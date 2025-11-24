import * as vscode from 'vscode';
import { FeatureItem } from './FeatureItem';

export class FeatureTreeProvider implements vscode.TreeDataProvider<FeatureItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<FeatureItem | undefined | null | void> = new vscode.EventEmitter<FeatureItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<FeatureItem | undefined | null | void> = this._onDidChangeTreeData.event;

    constructor() {
        // Scaffold only - no logic
    }

    refresh(): void {
        // Scaffold only - no logic
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: FeatureItem): vscode.TreeItem {
        // Scaffold only - no logic
        return element;
    }

    getChildren(element?: FeatureItem): Thenable<FeatureItem[]> {
        // Scaffold only - no logic
        return Promise.resolve([]);
    }
}
