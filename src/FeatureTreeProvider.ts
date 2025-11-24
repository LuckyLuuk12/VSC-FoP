import * as vscode from 'vscode';
import { FeatureItem } from './FeatureItem';

interface FeatureNode {
    name: string;
    type: string;
    mandatory: boolean;
    abstract: boolean;
    children: FeatureNode[];
}

export class FeatureTreeProvider implements vscode.TreeDataProvider<FeatureItem> {
    private model: any = null;
    private _onDidChangeTreeData: vscode.EventEmitter<FeatureItem | undefined | null | void> = new vscode.EventEmitter<FeatureItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<FeatureItem | undefined | null | void> = this._onDidChangeTreeData.event;

    constructor() {}

    setModel(modelData: any): void {
        this.model = modelData;
        this._onDidChangeTreeData.fire(undefined);
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: FeatureItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: FeatureItem): Thenable<FeatureItem[]> {
        if (!this.model || this.model.status !== "ok") {
            return Promise.resolve([]);
        }

        if (!element) {
            // Root level - return the root feature
            const rootFeature = this.model.root;
            return Promise.resolve([this.createFeatureItem(rootFeature)]);
        } else {
            // Return children of the given element
            const children = element.featureData.children || [];
            return Promise.resolve(children.map((child: FeatureNode) => this.createFeatureItem(child)));
        }
    }

    private createFeatureItem(feature: FeatureNode): FeatureItem {
        const hasChildren = feature.children && feature.children.length > 0;
        const collapsibleState = hasChildren 
            ? vscode.TreeItemCollapsibleState.Collapsed 
            : vscode.TreeItemCollapsibleState.None;

        return new FeatureItem(
            feature.name,
            collapsibleState,
            feature
        );
    }

    refreshWithModel(model: string) {
        this.model = JSON.parse(model);
        this._onDidChangeTreeData.fire(undefined);
    }
}
