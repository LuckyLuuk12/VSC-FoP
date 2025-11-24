import * as vscode from 'vscode';

interface FeatureData {
    name: string;
    type: string;
    mandatory: boolean;
    abstract: boolean;
    children: FeatureData[];
}

export class FeatureItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly featureData: FeatureData
    ) {
        super(label, collapsibleState);
        
        // Set description based on feature type
        const tags: string[] = [];
        
        if (featureData.abstract) {
            tags.push('abstract');
        }
        
        if (featureData.mandatory) {
            tags.push('mandatory');
        }
        
        // Show feature composition type
        if (featureData.type === 'or') {
            tags.push('OR');
        } else if (featureData.type === 'alt') {
            tags.push('XOR');
        } else if (featureData.type === 'and') {
            tags.push('AND');
        }
        
        if (tags.length > 0) {
            this.description = tags.join(', ');
        }
        
        // Set icon based on feature type
        if (featureData.abstract) {
            this.iconPath = new vscode.ThemeIcon('symbol-interface');
        } else if (featureData.type === 'or') {
            this.iconPath = new vscode.ThemeIcon('symbol-enum');
        } else if (featureData.type === 'alt') {
            this.iconPath = new vscode.ThemeIcon('symbol-method');
        } else {
            this.iconPath = new vscode.ThemeIcon('symbol-field');
        }
        
        this.contextValue = 'feature';
        this.tooltip = this.generateTooltip();
    }
    
    private generateTooltip(): string {
        const lines: string[] = [
            `Feature: ${this.featureData.name}`,
            `Type: ${this.featureData.type}`,
            `Mandatory: ${this.featureData.mandatory ? 'Yes' : 'No'}`,
            `Abstract: ${this.featureData.abstract ? 'Yes' : 'No'}`
        ];
        
        if (this.featureData.children && this.featureData.children.length > 0) {
            lines.push(`Children: ${this.featureData.children.length}`);
        }
        
        return lines.join('\n');
    }
}
