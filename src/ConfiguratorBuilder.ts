import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';

interface FeatureNode {
    name: string;
    type: string;
    mandatory: boolean;
    abstract: boolean;
    children: FeatureNode[];
}

interface ConfigFeature {
    name: string;
    automatic?: string;
    manual?: string;
}

interface FeatureSelection {
    manual: boolean;
    automatic: boolean;
    automaticUnselected?: boolean;
}

interface FeatureMetadata {
    name: string;
    type: string;
    mandatory: boolean;
    abstract: boolean;
    level: number;
    parent?: string;
    children: string[];
}

export class ConfiguratorBuilder {
    private panel: vscode.WebviewPanel | undefined;
    private modelData: any;
    private currentConfigPath: string | undefined;
    private extensionPath: string;

    constructor(extensionPath: string) {
        this.extensionPath = extensionPath;
    }

    setModel(modelData: any): void {
        this.modelData = modelData;
    }

    async openConfig(configPath?: string): Promise<void> {
        if (!this.modelData || this.modelData.status !== "ok") {
            vscode.window.showErrorMessage("Please load a feature model first.");
            return;
        }

        this.currentConfigPath = configPath;
        
        // Parse existing config if provided
        let selectedFeatures: Map<string, FeatureSelection> = new Map();
        if (configPath && fs.existsSync(configPath)) {
            selectedFeatures = await this.parseConfig(configPath);
        }

        // Create or show the webview panel
        if (this.panel) {
            this.panel.reveal(vscode.ViewColumn.One);
        } else {
            this.panel = vscode.window.createWebviewPanel(
                'configuratorBuilder',
                'Feature Configurator',
                vscode.ViewColumn.One,
                {
                    enableScripts: true,
                    retainContextWhenHidden: true
                }
            );

            this.panel.onDidDispose(() => {
                this.panel = undefined;
            });

            // Handle messages from the webview
            this.panel.webview.onDidReceiveMessage(
                async (message) => {
                    switch (message.command) {
                        case 'save':
                            await this.saveConfig(message.features, message.path);
                            break;
                        case 'saveAs':
                            await this.saveConfigAs(message.features);
                            break;
                    }
                }
            );
        }

        // Update the webview content
        this.panel.webview.html = this.getWebviewContent(selectedFeatures);
    }

    private async parseConfig(configPath: string): Promise<Map<string, FeatureSelection>> {
        const selectedFeatures = new Map<string, FeatureSelection>();
        
        try {
            const content = fs.readFileSync(configPath, 'utf-8');
            
            // Simple XML parsing for feature elements
            const featureRegex = /<feature\s+([^>]+)\/?>|<feature\s+([^>]+)>.*?<\/feature>/g;
            let match;
            
            while ((match = featureRegex.exec(content)) !== null) {
                const attributes = match[1] || match[2];
                const nameMatch = attributes.match(/name="([^"]+)"/);
                const automaticMatch = attributes.match(/automatic="([^"]+)"/);
                const manualMatch = attributes.match(/manual="([^"]+)"/);
                
                if (nameMatch) {
                    const featureName = nameMatch[1];
                    const selection: FeatureSelection = {
                        manual: !!(manualMatch && manualMatch[1] === 'selected'),
                        automatic: !!(automaticMatch && automaticMatch[1] === 'selected'),
                        automaticUnselected: !!(automaticMatch && automaticMatch[1] === 'unselected')
                    };
                    
                    // Add if any attribute is set
                    if (selection.manual || selection.automatic || selection.automaticUnselected) {
                        selectedFeatures.set(featureName, selection);
                    }
                }
            }
        } catch (error) {
            console.error('Error parsing config:', error);
        }
        
        return selectedFeatures;
    }

    private async saveConfig(features: any[], configPath?: string): Promise<void> {
        const savePath = configPath || this.currentConfigPath;
        
        if (!savePath) {
            await this.saveConfigAs(features);
            return;
        }

        try {
            const xml = this.generateConfigXml(features);
            fs.writeFileSync(savePath, xml, 'utf-8');
            vscode.window.showInformationMessage(`Configuration saved to ${path.basename(savePath)}`);
            this.currentConfigPath = savePath;
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to save configuration: ${error}`);
        }
    }

    private async saveConfigAs(features: any[]): Promise<void> {
        const uri = await vscode.window.showSaveDialog({
            filters: { 'XML Files': ['xml'] },
            defaultUri: vscode.Uri.file(path.join(vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || '', 'config.xml'))
        });

        if (uri) {
            await this.saveConfig(features, uri.fsPath);
        }
    }

    private generateConfigXml(features: any[]): string {
        let xml = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n<configuration>\n';
        
        for (const feature of features) {
            const attrs = [`name="${feature.name}"`];
            
            // Handle automatic unselected first
            if (feature.automaticUnselected) {
                attrs.push('automatic="unselected"');
                xml += `\t<feature ${attrs.join(' ')}/>\n`;
            } else if (feature.isManual || feature.isAutomatic) {
                // Handle selected features
                if (feature.isManual) {
                    attrs.push('manual="selected"');
                }
                if (feature.isAutomatic) {
                    attrs.push('automatic="selected"');
                }
                xml += `\t<feature ${attrs.join(' ')}/>\n`;
            } else {
                // Unselected features without attributes
                xml += `\t<feature ${attrs.join(' ')}/>\n`;
            }
        }
        
        xml += '</configuration>\n';
        return xml;
    }

    private buildParentMap(node: FeatureNode, parentMap: Map<string, string> = new Map(), parentName?: string): Map<string, string> {
        if (parentName) {
            parentMap.set(node.name, parentName);
        }
        
        if (node.children) {
            for (const child of node.children) {
                this.buildParentMap(child, parentMap, node.name);
            }
        }
        
        return parentMap;
    }

    private buildFeatureMetadata(node: FeatureNode, metadata: Map<string, FeatureMetadata> = new Map(), parentName?: string, level: number = 0): Map<string, FeatureMetadata> {
        const childNames = node.children ? node.children.map(c => c.name) : [];
        
        metadata.set(node.name, {
            name: node.name,
            type: node.type,
            mandatory: node.mandatory,
            abstract: node.abstract,
            level: level,
            parent: parentName,
            children: childNames
        });
        
        if (node.children) {
            for (const child of node.children) {
                this.buildFeatureMetadata(child, metadata, node.name, level + 1);
            }
        }
        
        return metadata;
    }

    private getMandatoryFeatures(node: FeatureNode, mandatoryFeatures: string[] = []): string[] {
        if (node.mandatory) {
            mandatoryFeatures.push(node.name);
        }
        
        if (node.children) {
            for (const child of node.children) {
                this.getMandatoryFeatures(child, mandatoryFeatures);
            }
        }
        
        return mandatoryFeatures;
    }

    private getAllFeatures(node: FeatureNode, features: any[] = [], parentPath: string = ''): any[] {
        const currentPath = parentPath ? `${parentPath} > ${node.name}` : node.name;
        
        features.push({
            name: node.name,
            type: node.type,
            mandatory: node.mandatory,
            abstract: node.abstract,
            path: currentPath,
            level: parentPath.split(' > ').filter(s => s).length
        });

        if (node.children) {
            for (const child of node.children) {
                this.getAllFeatures(child, features, currentPath);
            }
        }

        return features;
    }

    private getWebviewContent(selectedFeatures: Map<string, FeatureSelection>): string {
        const features = this.getAllFeatures(this.modelData.root);
        const featureMetadata = this.buildFeatureMetadata(this.modelData.root);
        const mandatoryFeatures = this.getMandatoryFeatures(this.modelData.root);
        const configName = this.currentConfigPath ? path.basename(this.currentConfigPath) : 'New Configuration';
        
        // If no config is loaded, initialize mandatory features
        if (selectedFeatures.size === 0) {
            for (const featureName of mandatoryFeatures) {
                selectedFeatures.set(featureName, { manual: false, automatic: true });
            }
        }

        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Feature Configurator</title>
    <style>
        body {
            font-family: var(--vscode-font-family);
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
            padding: 20px;
            margin: 0;
        }
        
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }
        
        h1 {
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }
        
        .config-name {
            color: var(--vscode-descriptionForeground);
            font-size: 14px;
            margin-top: 5px;
        }
        
        .actions {
            display: flex;
            gap: 10px;
        }
        
        button {
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            padding: 8px 16px;
            cursor: pointer;
            border-radius: 2px;
            font-size: 13px;
            font-family: var(--vscode-font-family);
        }
        
        button:hover {
            background-color: var(--vscode-button-hoverBackground);
        }
        
        button.secondary {
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
        }
        
        button.secondary:hover {
            background-color: var(--vscode-button-secondaryHoverBackground);
        }
        
        .features-container {
            max-width: 900px;
        }
        
        .feature-item {
            display: flex;
            align-items: center;
            padding: 8px 0;
            border-bottom: 1px solid var(--vscode-widget-border);
        }
        
        .feature-item:hover {
            background-color: var(--vscode-list-hoverBackground);
        }
        
        .feature-item.disabled {
            opacity: 0.5;
        }
        
        .feature-item.disabled:hover {
            background-color: transparent;
        }
        
        .feature-checkbox {
            margin-right: 10px;
            cursor: pointer;
            width: 16px;
            height: 16px;
        }
        
        .feature-checkbox:disabled {
            cursor: not-allowed;
            opacity: 0.5;
        }
        
        .feature-label {
            flex: 1;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .feature-name {
            font-weight: 500;
        }
        
        .feature-badge {
            font-size: 11px;
            padding: 2px 6px;
            border-radius: 3px;
            background-color: var(--vscode-badge-background);
            color: var(--vscode-badge-foreground);
        }
        
        .feature-badge.mandatory {
            background-color: #1e88e5;
        }
        
        .feature-badge.abstract {
            background-color: #7e57c2;
        }
        
        .feature-badge.or {
            background-color: #fb8c00;
        }
        
        .feature-badge.alt {
            background-color: #e53935;
        }
        
        .feature-badge.manual-selected {
            background-color: #43a047;
        }
        
        .feature-badge.automatic-selected {
            background-color: #5e35b1;
        }
        
        .feature-badge.automatic-unselected {
            background-color: #d32f2f;
        }
        
        .feature-indent {
            display: inline-block;
        }
        
        .selection-badges {
            margin-left: 10px;
            display: flex;
            gap: 5px;
            align-items: center;
        }
        
        .info-panel {
            background-color: var(--vscode-textBlockQuote-background);
            border-left: 4px solid var(--vscode-textBlockQuote-border);
            padding: 12px;
            margin-bottom: 20px;
            font-size: 13px;
        }
        
        .info-panel ul {
            margin: 8px 0 0 0;
            padding-left: 20px;
        }
        
        .info-panel li {
            margin: 4px 0;
        }
        
        .config-stats {
            background-color: var(--vscode-textBlockQuote-background);
            border-left: 4px solid var(--vscode-textBlockQuote-border);
            padding: 12px;
            margin-bottom: 20px;
            font-size: 13px;
            display: flex;
            gap: 30px;
            align-items: center;
        }
        
        .stat-item {
            display: flex;
            flex-direction: column;
        }
        
        .stat-label {
            color: var(--vscode-descriptionForeground);
            font-size: 11px;
            text-transform: uppercase;
            margin-bottom: 4px;
        }
        
        .stat-value {
            font-size: 20px;
            font-weight: 600;
            color: var(--vscode-textLink-foreground);
        }
        
        .validation-status {
            background-color: var(--vscode-textBlockQuote-background);
            border-left: 4px solid var(--vscode-textBlockQuote-border);
            padding: 12px;
            margin-bottom: 20px;
            font-size: 13px;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .validation-status.valid {
            border-left-color: #43a047;
            background-color: rgba(67, 160, 71, 0.1);
        }
        
        .validation-status.invalid {
            border-left-color: #e53935;
            background-color: rgba(229, 57, 53, 0.1);
        }
        
        .validation-icon {
            font-size: 18px;
            font-weight: bold;
        }
        
        .validation-message {
            flex: 1;
        }
        
        .validation-errors {
            margin-top: 8px;
            padding-left: 20px;
        }
        
        .validation-errors li {
            margin: 4px 0;
            color: var(--vscode-errorForeground);
        }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1>Feature Configurator</h1>
            <div class="config-name">${configName}</div>
        </div>
        <div class="actions">
            <button class="secondary" onclick="saveAs()">Save As...</button>
            <button onclick="save()">Save</button>
        </div>
    </div>
    
    <div class="validation-status" id="validation-status">
        <div class="validation-icon" id="validation-icon">⚠</div>
        <div class="validation-message">
            <div id="validation-text">Checking configuration...</div>
            <ul class="validation-errors" id="validation-errors" style="display: none;"></ul>
        </div>
    </div>
    
    <div class="config-stats">
        <div class="stat-item">
            <div class="stat-label">Possible Configurations</div>
            <div class="stat-value" id="possible-configs">-</div>
        </div>
    </div>
    
    <div class="features-container">
        ${features.map(feature => this.renderFeature(feature, selectedFeatures)).join('')}
    </div>
    
    <script>
        const vscode = acquireVsCodeApi();
        const featureMetadata = ${JSON.stringify(Object.fromEntries(featureMetadata))};
        
        // Feature state: tracks manual, automatic, and automaticUnselected for each feature
        const featureState = new Map();
        
        // Initialize feature state from rendered data
        function initializeState() {
            document.querySelectorAll('.feature-item').forEach(item => {
                const name = item.dataset.name;
                const isManual = item.dataset.manual === 'true';
                const isAutomatic = item.dataset.automatic === 'true';
                const isAutomaticUnselected = item.dataset.automaticUnselected === 'true';
                
                featureState.set(name, {
                    manual: isManual,
                    automatic: isAutomatic,
                    automaticUnselected: isAutomaticUnselected
                });
            });
        }
        
        function getFeatureData() {
            const features = [];
            featureState.forEach((state, name) => {
                features.push({
                    name: name,
                    isManual: state.manual,
                    isAutomatic: state.automatic,
                    automaticUnselected: state.automaticUnselected
                });
            });
            return features;
        }
        
        function save() {
            const features = getFeatureData();
            vscode.postMessage({
                command: 'save',
                features: features,
                path: '${this.currentConfigPath || ''}'
            });
        }
        
        function saveAs() {
            const features = getFeatureData();
            vscode.postMessage({
                command: 'saveAs',
                features: features
            });
        }
        
        function updateUI() {
            featureState.forEach((state, name) => {
                const item = document.querySelector(\`.feature-item[data-name="\${name}"]\`);
                if (!item) return;
                
                const checkbox = item.querySelector('.feature-checkbox');
                const badgeContainer = item.querySelector('.selection-badges');
                
                // Update checkbox state
                const isSelected = (state.manual || state.automatic) && !state.automaticUnselected;
                checkbox.checked = isSelected;
                checkbox.disabled = state.automaticUnselected;
                
                // Update disabled class
                if (state.automaticUnselected) {
                    item.classList.add('disabled');
                } else {
                    item.classList.remove('disabled');
                }
                
                // Update badges
                let badges = '';
                if (state.manual) {
                    badges += '<span class="feature-badge manual-selected">Manual</span>';
                }
                if (state.automatic) {
                    badges += '<span class="feature-badge automatic-selected">Automatic</span>';
                }
                if (state.automaticUnselected) {
                    badges += '<span class="feature-badge automatic-unselected">Excluded</span>';
                }
                badgeContainer.innerHTML = badges;
            });
            
            // Update configuration counts
            updateConfigurationCounts();
        }
        
        function countConfigurationsForFeature(featureName, currentSelections) {
            const metadata = featureMetadata[featureName];
            if (!metadata) return 1;
            
            const state = currentSelections.get(featureName);
            const isSelected = state && (state.manual || state.automatic) && !state.automaticUnselected;
            const isExcluded = state && state.automaticUnselected;
            
            // If feature is excluded, it contributes 0 configurations
            if (isExcluded) {
                return 0;
            }
            
            // Start with base count
            let count = 1;
            
            // Handle children if this feature has any
            if (metadata.children && metadata.children.length > 0) {
                if (metadata.type === 'alt') {
                    // ALT group: exactly one child must be selected
                    let selectedChild = null;
                    for (const childName of metadata.children) {
                        const childState = currentSelections.get(childName);
                        if (childState && (childState.manual || childState.automatic) && !childState.automaticUnselected) {
                            selectedChild = childName;
                            break;
                        }
                    }
                    
                    if (selectedChild) {
                        // One child is selected, only count that branch
                        count *= countConfigurationsForFeature(selectedChild, currentSelections);
                    } else {
                        // No child selected yet, sum all possible branches
                        // For each branch, count as if it were selected (not optional)
                        let altCount = 0;
                        for (const childName of metadata.children) {
                            const childState = currentSelections.get(childName);
                            if (!childState || !childState.automaticUnselected) {
                                // Create a temporary selection state with this child selected
                                const tempSelections = new Map(currentSelections);
                                tempSelections.set(childName, { manual: true, automatic: false, automaticUnselected: false });
                                altCount += countConfigurationsForFeature(childName, tempSelections);
                            }
                        }
                        count *= altCount;
                    }
                } else if (metadata.type === 'or') {
                    // OR group: at least one child must be selected when parent is selected
                    if (isSelected || metadata.mandatory) {
                        // Parent is selected, process children
                        let product = 1;
                        let hasUnselectedChildren = false;
                        for (const childName of metadata.children) {
                            const childState = currentSelections.get(childName);
                            const childSelected = childState && (childState.manual || childState.automatic) && !childState.automaticUnselected;
                            const childExcluded = childState && childState.automaticUnselected;
                            
                            if (childExcluded) {
                                // Excluded child doesn't contribute
                                continue;
                            } else if (childSelected) {
                                // Child is selected, count its configurations (choice made)
                                const childCount = countConfigurationsForFeature(childName, currentSelections);
                                product *= childCount;
                            } else {
                                // Child not selected yet, can be selected or not
                                hasUnselectedChildren = true;
                                const childCount = countConfigurationsForFeature(childName, currentSelections);
                                product *= (1 + childCount);
                            }
                        }
                        // Subtract 1 to exclude the case where no children are selected (OR constraint)
                        // But only if there are unselected children (possibilities remaining)
                        if (hasUnselectedChildren) {
                            count *= (product - 1);
                        } else {
                            count *= product;
                        }
                    } else {
                        // Parent not selected, return count of configurations when parent IS selected
                        // The parent will handle adding the "not selected" case
                        let product = 1;
                        for (const childName of metadata.children) {
                            const childState = currentSelections.get(childName);
                            if (!childState || !childState.automaticUnselected) {
                                const childCount = countConfigurationsForFeature(childName, currentSelections);
                                product *= (1 + childCount);
                            }
                        }
                        count = product - 1; // At least one child must be selected (OR constraint)
                    }
                } else {
                    // AND group: children are independent
                    if (isSelected || metadata.mandatory) {
                        // Parent is selected or mandatory, process children
                        for (const childName of metadata.children) {
                            const childMetadata = featureMetadata[childName];
                            const childState = currentSelections.get(childName);
                            const childSelected = childState && (childState.manual || childState.automatic) && !childState.automaticUnselected;
                            const childExcluded = childState && childState.automaticUnselected;
                            
                            if (childExcluded) {
                                // Excluded child doesn't contribute
                                continue;
                            } else if (childMetadata && childMetadata.mandatory) {
                                // Mandatory child, always selected
                                count *= countConfigurationsForFeature(childName, currentSelections);
                            } else if (childSelected) {
                                // Optional child is selected (choice made)
                                const childCount = countConfigurationsForFeature(childName, currentSelections);
                                count *= childCount;
                            } else {
                                // Optional child not selected yet (can be selected or not)
                                const childCount = countConfigurationsForFeature(childName, currentSelections);
                                count *= (1 + childCount);
                            }
                        }
                    } else {
                        // Parent is optional and not selected, return count of configurations when parent IS selected
                        // The parent will handle adding the "not selected" case
                        count = 1;
                        for (const childName of metadata.children) {
                            const childMetadata = featureMetadata[childName];
                            const childState = currentSelections.get(childName);
                            const childExcluded = childState && childState.automaticUnselected;
                            
                            if (childExcluded) {
                                continue;
                            } else if (childMetadata && childMetadata.mandatory) {
                                // Mandatory child when parent is selected
                                count *= countConfigurationsForFeature(childName, currentSelections);
                            } else {
                                // Optional child
                                const childCount = countConfigurationsForFeature(childName, currentSelections);
                                count *= (1 + childCount);
                            }
                        }
                    }
                }
            } else {
                // Leaf feature with no children
                // Return 1 for the feature itself (parent will decide if it needs to account for selection choice)
                // If selected or mandatory: count = 1 (already set)
            }
            
            return count;
        }
        
        function validateConfiguration() {
            const errors = [];
            
            // Check all features for constraint violations
            featureState.forEach((state, featureName) => {
                const metadata = featureMetadata[featureName];
                if (!metadata) return;
                
                const isSelected = (state.manual || state.automatic) && !state.automaticUnselected;
                
                // Check ALT groups: if parent is selected/mandatory, exactly one child must be selected
                if (metadata.type === 'alt' && metadata.children && metadata.children.length > 0) {
                    if (isSelected || metadata.mandatory) {
                        let selectedCount = 0;
                        metadata.children.forEach(childName => {
                            const childState = featureState.get(childName);
                            if (childState && (childState.manual || childState.automatic) && !childState.automaticUnselected) {
                                selectedCount++;
                            }
                        });
                        
                        if (selectedCount === 0) {
                            errors.push(featureName + ' (ALT group): exactly one child must be selected');
                        } else if (selectedCount > 1) {
                            errors.push(featureName + ' (ALT group): only one child can be selected, but ' + selectedCount + ' are selected');
                        }
                    }
                }
                
                // Check OR groups: if parent is selected, at least one child must be selected
                if (metadata.type === 'or' && metadata.children && metadata.children.length > 0) {
                    if (isSelected) {
                        let selectedCount = 0;
                        metadata.children.forEach(childName => {
                            const childState = featureState.get(childName);
                            if (childState && (childState.manual || childState.automatic) && !childState.automaticUnselected) {
                                selectedCount++;
                            }
                        });
                        
                        if (selectedCount === 0) {
                            errors.push(featureName + ' (OR group): at least one child must be selected when parent is selected');
                        }
                    }
                }
                
                // Check AND groups: if parent is selected, all mandatory children must be selected
                if (metadata.type === 'and' && metadata.children && metadata.children.length > 0) {
                    if (isSelected) {
                        metadata.children.forEach(childName => {
                            const childMetadata = featureMetadata[childName];
                            const childState = featureState.get(childName);
                            
                            if (childMetadata && childMetadata.mandatory) {
                                const childSelected = childState && (childState.manual || childState.automatic) && !childState.automaticUnselected;
                                if (!childSelected) {
                                    errors.push(childName + ' is mandatory but not selected (parent ' + featureName + ' is selected)');
                                }
                            }
                        });
                    }
                }
            });
            
            return {
                isValid: errors.length === 0,
                errors: errors
            };
        }
        
        function updateValidationStatus() {
            const validation = validateConfiguration();
            const statusElement = document.getElementById('validation-status');
            const iconElement = document.getElementById('validation-icon');
            const textElement = document.getElementById('validation-text');
            const errorsElement = document.getElementById('validation-errors');
            
            if (validation.isValid) {
                statusElement.className = 'validation-status valid';
                iconElement.textContent = '✓';
                textElement.textContent = 'Configuration is valid';
                errorsElement.style.display = 'none';
            } else {
                statusElement.className = 'validation-status invalid';
                iconElement.textContent = '✗';
                textElement.textContent = 'Configuration is invalid';
                errorsElement.style.display = 'block';
                errorsElement.innerHTML = validation.errors.map(err => '<li>' + err + '</li>').join('');
            }
            
            return validation.isValid;
        }
        
        function updateConfigurationCounts() {
            // Find root feature
            let rootFeature = null;
            for (const [name, metadata] of Object.entries(featureMetadata)) {
                if (!metadata.parent) {
                    rootFeature = name;
                    break;
                }
            }
            
            if (rootFeature) {
                // Check if configuration is valid
                const isValid = updateValidationStatus();
                
                // Calculate possible configurations with current selections
                // Only show count if configuration is valid
                if (isValid) {
                    const possibleConfigs = countConfigurationsForFeature(rootFeature, featureState);
                    document.getElementById('possible-configs').textContent = possibleConfigs.toLocaleString();
                } else {
                    document.getElementById('possible-configs').textContent = '0';
                }
            }
        }
        
        function selectParents(featureName) {
            const metadata = featureMetadata[featureName];
            if (!metadata || !metadata.parent) return;
            
            const parentName = metadata.parent;
            const parentState = featureState.get(parentName);
            
            if (parentState) {
                // Set automatic selection on parent
                parentState.automatic = true;
                parentState.automaticUnselected = false;
                
                // Recursively select parent's parents
                selectParents(parentName);
            }
        }
        
        function isParentNeededByChildren(parentName) {
            const metadata = featureMetadata[parentName];
            if (!metadata || !metadata.children) return false;
            
            // Check if any child is selected (manual or automatic, but not unselected)
            for (const childName of metadata.children) {
                const childState = featureState.get(childName);
                if (childState && (childState.manual || childState.automatic) && !childState.automaticUnselected) {
                    return true;
                }
                
                // Recursively check if any descendant needs this parent
                const childMetadata = featureMetadata[childName];
                if (childMetadata && childMetadata.children && childMetadata.children.length > 0) {
                    if (isParentNeededByChildren(childName)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        function cleanupParentAutomatic(featureName) {
            const metadata = featureMetadata[featureName];
            if (!metadata || !metadata.parent) return;
            
            const parentName = metadata.parent;
            const parentState = featureState.get(parentName);
            const parentMetadata = featureMetadata[parentName];
            
            if (parentState && parentMetadata) {
                // Check if parent is mandatory - if so, keep automatic
                if (parentMetadata.mandatory) {
                    return;
                }
                
                // Check if any children still need this parent
                if (!isParentNeededByChildren(parentName)) {
                    // No children need it, remove automatic (but keep manual if set)
                    parentState.automatic = false;
                    
                    // Recursively clean up parent's parents
                    cleanupParentAutomatic(parentName);
                }
            }
        }
        
        // Recursively mark a feature and all its descendants as automaticUnselected
        function markDescendantsAsUnselected(featureName) {
            const metadata = featureMetadata[featureName];
            if (!metadata) return;
            
            const state = featureState.get(featureName);
            if (state) {
                state.automaticUnselected = true;
                state.automatic = false;
            }
            
            // Recursively mark all descendants
            if (metadata.children && metadata.children.length > 0) {
                metadata.children.forEach(childName => {
                    markDescendantsAsUnselected(childName);
                });
            }
        }
        
        // Recursively unmark a feature and all its descendants from automaticUnselected
        function unmarkDescendantsAsUnselected(featureName) {
            const metadata = featureMetadata[featureName];
            if (!metadata) return;
            
            const state = featureState.get(featureName);
            if (state) {
                state.automaticUnselected = false;
            }
            
            // Recursively unmark all descendants
            if (metadata.children && metadata.children.length > 0) {
                metadata.children.forEach(childName => {
                    unmarkDescendantsAsUnselected(childName);
                });
            }
        }
        
        // Comprehensive constraint evaluation - checks all XOR/ALT, OR, and AND groups in the entire tree
        function evaluateAllConstraints() {
            // Iterate through all features and check if they are part of a constraint group
            featureState.forEach((state, featureName) => {
                const metadata = featureMetadata[featureName];
                if (!metadata) return;
                
                // Check if this feature is an XOR/ALT parent
                if (metadata.type === 'alt' && metadata.children && metadata.children.length > 0) {
                    // Find which child (if any) is selected
                    let selectedChild = null;
                    
                    metadata.children.forEach(childName => {
                        const childState = featureState.get(childName);
                        if (childState && (childState.manual || childState.automatic) && !childState.automaticUnselected) {
                            selectedChild = childName;
                        }
                    });
                    
                    // Apply constraints based on selection
                    if (selectedChild) {
                        // One child is selected, mark all siblings and their descendants as unselected
                        metadata.children.forEach(siblingName => {
                            if (siblingName !== selectedChild) {
                                markDescendantsAsUnselected(siblingName);
                            }
                        });
                    } else {
                        // No child is selected, unmark all siblings and their descendants
                        metadata.children.forEach(siblingName => {
                            unmarkDescendantsAsUnselected(siblingName);
                        });
                    }
                }
                
                // Check if this feature is an OR parent
                if (metadata.type === 'or' && metadata.children && metadata.children.length > 0) {
                    // Check if the OR parent itself is selected
                    const parentState = featureState.get(featureName);
                    const isParentSelected = parentState && (parentState.manual || parentState.automatic) && !parentState.automaticUnselected;
                    
                    if (isParentSelected) {
                        // Count how many children are selected
                        let selectedCount = 0;
                        metadata.children.forEach(childName => {
                            const childState = featureState.get(childName);
                            if (childState && (childState.manual || childState.automatic) && !childState.automaticUnselected) {
                                selectedCount++;
                            }
                        });
                        
                        // If no children are selected, we need at least one
                        // For now, we don't auto-select a child, but this could be enhanced
                        // The user will need to manually select at least one child
                    }
                    // Note: OR groups don't exclude siblings like ALT does
                    // Multiple children can be selected simultaneously
                }
                
                // Check if this feature is an AND parent
                if (metadata.type === 'and' && metadata.children && metadata.children.length > 0) {
                    // Check if the AND parent itself is selected
                    const parentState = featureState.get(featureName);
                    const isParentSelected = parentState && (parentState.manual || parentState.automatic) && !parentState.automaticUnselected;
                    
                    if (isParentSelected) {
                        // Auto-select all mandatory children
                        metadata.children.forEach(childName => {
                            const childMetadata = featureMetadata[childName];
                            const childState = featureState.get(childName);
                            
                            if (childMetadata && childMetadata.mandatory && childState) {
                                // Mandatory child must be selected when parent is selected
                                childState.automatic = true;
                                childState.automaticUnselected = false;
                            }
                        });
                    }
                    // Note: AND groups allow all children to be selected independently
                    // No mutual exclusion between children
                }
            });
        }
        
        function onFeatureToggle(checkbox) {
            const item = checkbox.closest('.feature-item');
            const featureName = item.dataset.name;
            const state = featureState.get(featureName);
            
            if (!state || state.automaticUnselected) {
                checkbox.checked = false;
                return;
            }
            
            if (checkbox.checked) {
                // User is selecting the feature
                state.manual = true;
                state.automaticUnselected = false;
                
                // Select all parents with automatic
                selectParents(featureName);
            } else {
                // User is deselecting the feature
                
                const metadata = featureMetadata[featureName];
                if (metadata && metadata.parent) {
                    const parentMetadata = featureMetadata[metadata.parent];
                    const parentState = featureState.get(metadata.parent);
                    
                    // Check if this is a mandatory child of an AND group
                    if (parentMetadata && parentMetadata.type === 'and' && metadata.mandatory &&
                        parentState && (parentState.manual || parentState.automatic) && !parentState.automaticUnselected) {
                        
                        // Prevent deselection of mandatory children when AND parent is selected
                        checkbox.checked = true;
                        alert('Cannot deselect: This is a mandatory feature in an AND group and its parent is selected.');
                        return;
                    }
                    
                    // Check if parent is an OR group and is selected
                    if (parentMetadata && parentMetadata.type === 'or' && 
                        parentState && (parentState.manual || parentState.automatic) && !parentState.automaticUnselected) {
                        
                        // Count how many siblings (including this feature) are currently selected
                        let selectedSiblings = 0;
                        parentMetadata.children.forEach(siblingName => {
                            const siblingState = featureState.get(siblingName);
                            if (siblingState && (siblingState.manual || siblingState.automatic) && !siblingState.automaticUnselected) {
                                selectedSiblings++;
                            }
                        });
                        
                        // If this is the last selected child
                        if (selectedSiblings === 1) {
                            // If parent is mandatory, prevent deselection
                            if (parentMetadata.mandatory) {
                                checkbox.checked = true;
                                alert('Cannot deselect: At least one child must be selected in an OR group when the parent is mandatory and selected.');
                                return;
                            } else {
                                // Parent is optional, allow deselection
                                // Only clear automatic flag, preserve manual flag if user manually selected parent
                                parentState.automatic = false;
                            }
                        }
                    }
                }
                
                state.manual = false;
                
                // Clean up parent automatic attributes if they're no longer needed
                cleanupParentAutomatic(featureName);
            }
            
            // Comprehensively evaluate all constraints after any state change
            evaluateAllConstraints();
            
            updateUI();
        }
        
        // Initialize
        initializeState();
        updateUI();
    </script>
</body>
</html>`;
    }

    private renderFeature(feature: any, selectedFeatures: Map<string, FeatureSelection>): string {
        const indent = '&nbsp;&nbsp;&nbsp;&nbsp;'.repeat(feature.level);
        const selection = selectedFeatures.get(feature.name);
        const isManual = selection?.manual || false;
        const isAutomatic = selection?.automatic || false;
        const isAutomaticUnselected = selection?.automaticUnselected || false;
        const isSelected = (isManual || isAutomatic) && !isAutomaticUnselected;
        
        let typeBadges = '';
        if (feature.mandatory) {
            typeBadges += '<span class="feature-badge mandatory">Mandatory</span>';
        }
        if (feature.abstract) {
            typeBadges += '<span class="feature-badge abstract">Abstract</span>';
        }
        if (feature.type === 'or') {
            typeBadges += '<span class="feature-badge or">OR</span>';
        }
        if (feature.type === 'alt') {
            typeBadges += '<span class="feature-badge alt">ALT</span>';
        }
        
        let selectionBadges = '';
        if (isManual) {
            selectionBadges += '<span class="feature-badge manual-selected">Manual</span>';
        }
        if (isAutomatic) {
            selectionBadges += '<span class="feature-badge automatic-selected">Automatic</span>';
        }
        if (isAutomaticUnselected) {
            selectionBadges += '<span class="feature-badge automatic-unselected">Excluded</span>';
        }
        
        const disabledClass = isAutomaticUnselected ? ' disabled' : '';
        const disabledAttr = isAutomaticUnselected ? ' disabled' : '';
        
        return `
        <div class="feature-item${disabledClass}" 
             data-name="${feature.name}"
             data-manual="${isManual}"
             data-automatic="${isAutomatic}"
             data-automatic-unselected="${isAutomaticUnselected}">
            <input type="checkbox" 
                   class="feature-checkbox" 
                   id="feature-${feature.name}" 
                   ${isSelected ? 'checked' : ''}
                   ${disabledAttr}
                   onchange="onFeatureToggle(this)">
            <label class="feature-label" for="feature-${feature.name}">
                <span class="feature-indent">${indent}</span>
                <span class="feature-name">${feature.name}</span>
                ${typeBadges}
            </label>
            <div class="selection-badges">
                ${selectionBadges}
            </div>
        </div>`;
    }
}
