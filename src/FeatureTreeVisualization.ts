import * as vscode from 'vscode';
import * as path from 'path';

// Helper for syntax highlighting
const html = (strings: TemplateStringsArray, ...values: any[]) => 
    strings.reduce((result, str, i) => result + str + (values[i] ?? ''), '');

interface FeatureNode {
    name: string;
    type: string;
    mandatory: boolean;
    abstract: boolean;
    children: FeatureNode[];
}

export class FeatureTreeVisualization {
    private panel: vscode.WebviewPanel | undefined;
    private model: any = null;
    private modelPath: string | undefined;
    private javaBridge: any;

    constructor(private extensionPath: string, javaBridge: any) {
        this.javaBridge = javaBridge;
    }

    public setModel(modelData: any, modelPath?: string) {
        this.model = modelData;
        this.modelPath = modelPath;
        if (this.panel) {
            this.updateWebview();
        }
    }

    public show() {
        if (this.panel) {
            this.panel.reveal();
        } else {
            this.panel = vscode.window.createWebviewPanel(
                'fopTreeVisualization',
                'Feature Model Tree',
                vscode.ViewColumn.One,
                {
                    enableScripts: true,
                    retainContextWhenHidden: true
                }
            );

            this.panel.onDidDispose(() => {
                this.panel = undefined;
            });

            // Handle messages from webview
            this.panel.webview.onDidReceiveMessage(
                async message => {
                    switch (message.command) {
                        case 'updateNode':
                            await this.handleUpdateNode(message.nodePath, message.updates);
                            break;
                        case 'addChild':
                            await this.handleAddChild(message.parentPath, message.childData);
                            break;
                        case 'removeNode':
                            await this.handleRemoveNode(message.nodePath);
                            break;
                    }
                },
                undefined,
                []
            );

            this.updateWebview();
        }
    }

    private async handleUpdateNode(nodePath: number[], updates: any) {
        if (!this.model || !this.modelPath) return;

        try {
            // Update the node in memory
            this.updateNodeAtPath(this.model.root, nodePath, updates);
            
            // Save to file
            await this.saveModelToFile();
            
            vscode.window.showInformationMessage('Feature updated and saved');
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to update feature: ${error}`);
        }
    }

    private async handleAddChild(parentPath: number[], childData: any) {
        if (!this.model || !this.modelPath) return;

        try {
            // Find parent and add child
            const parent = this.getNodeAtPath(this.model.root, parentPath);
            if (parent) {
                // Check if we should auto-update parent type BEFORE modifying children
                const shouldUpdateParentType = parent.type === 'feature' && 
                                              (!parent.children || parent.children.length === 0);
                
                if (!parent.children) {
                    parent.children = [];
                }
                
                // Auto-update parent type from 'feature' to 'and' if it was a leaf node
                if (shouldUpdateParentType) {
                    parent.type = 'and';
                    console.log(`[FOP] Auto-updated parent "${parent.name}" type from 'feature' to 'and'`);
                }
                
                parent.children.push({
                    name: childData.name,
                    type: 'feature',
                    mandatory: childData.mandatory || false,
                    abstract: childData.abstract || false,
                    children: []
                });

                // Save to file using the proper save method
                await this.saveModelToFile();
                
                vscode.window.showInformationMessage('Child feature added and saved');
            }
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to add child: ${error}`);
        }
    }

    private getNodeAtPath(node: any, path: number[]): any {
        if (path.length === 0) return node;
        const [index, ...rest] = path;
        if (node.children && node.children[index]) {
            return this.getNodeAtPath(node.children[index], rest);
        }
        return null;
    }

    private updateNodeAtPath(node: any, path: number[], updates: any) {
        if (path.length === 0) {
            Object.assign(node, updates);
            return;
        }
        const [index, ...rest] = path;
        if (node.children && node.children[index]) {
            this.updateNodeAtPath(node.children[index], rest, updates);
        }
    }

    private async handleRemoveNode(nodePath: number[]) {
        console.log('[FOP] handleRemoveNode called with path:', nodePath);
        
        if (!this.model || !this.modelPath) {
            console.error('[FOP] No model or modelPath available');
            return;
        }

        try {
            // Can't remove root node
            if (nodePath.length === 0) {
                console.error('[FOP] Attempted to remove root node');
                vscode.window.showErrorMessage('Cannot remove the root node');
                return;
            }

            // Get parent node and remove the child
            const parentPath = nodePath.slice(0, -1);
            const childIndex = nodePath[nodePath.length - 1];
            const parent = this.getNodeAtPath(this.model.root, parentPath);

            console.log('[FOP] Parent node:', parent);
            console.log('[FOP] Child index to remove:', childIndex);

            if (parent && parent.children && parent.children[childIndex]) {
                const removedNode = parent.children[childIndex];
                console.log('[FOP] Removing node:', removedNode.name);
                
                parent.children.splice(childIndex, 1);

                // If parent now has no children and is not a simple feature, convert it to leaf
                if (parent.children.length === 0 && ['and', 'or', 'alt'].includes(parent.type)) {
                    const oldType = parent.type;
                    parent.type = 'feature';
                    console.log(`[FOP] Auto-updated parent "${parent.name}" type from '${oldType}' to 'feature' (now a leaf)`);
                }

                // Save to file
                console.log('[FOP] Saving model to file...');
                await this.saveModelToFile();

                vscode.window.showInformationMessage(`Node "${removedNode.name}" removed successfully`);
                console.log('[FOP] Node removed successfully');
            } else {
                console.error('[FOP] Could not find node to remove. Parent:', parent, 'Child index:', childIndex);
                vscode.window.showErrorMessage('Could not find node to remove');
            }
        } catch (error) {
            console.error('[FOP] Error removing node:', error);
            vscode.window.showErrorMessage(`Failed to remove node: ${error}`);
        }
    }

    private async saveModelToFile() {
        if (!this.model || !this.modelPath) return;

        // Convert the model back to XML and save
        const result = await this.javaBridge.call(['saveModel', this.modelPath, JSON.stringify(this.model.root)]);
        const saveResult = JSON.parse(result);
        
        if (saveResult.status !== 'ok') {
            throw new Error(saveResult.message || 'Failed to save model');
        }
        
        // Reload the model to keep in sync
        const reloadedModel = await this.javaBridge.loadModel(this.modelPath);
        this.model = reloadedModel;
        
        // Update webview
        this.updateWebview();
    }

    private updateWebview() {
        if (!this.panel) return;

        const html = this.getWebviewContent();
        this.panel.webview.html = html;
    }

    private getWebviewContent(): string {
        const modelJson = this.model ? JSON.stringify(this.model.root) : 'null';
        const hasModel = this.model !== null;
        
        return html`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Feature Model Tree</title>
    <style>
        * {
            box-sizing: border-box;
            user-select: none;
            -webkit-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
        }

        body {
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--vscode-editor-background);
            color: var(--vscode-editor-foreground);
            overflow: hidden;
            height: 100vh;
        }

        #zoom-controls {
            position: fixed;
            bottom: 10px;
            right: 10px;
            z-index: 500;
            display: flex;
            gap: 5px;
            background: var(--vscode-editor-background);
            padding: 5px;
            border-radius: 5px;
            border: 1px solid var(--vscode-editorWidget-border);
        }

        #zoom-controls button {
            padding: 5px 10px;
            background: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            border-radius: 3px;
            cursor: pointer;
        }

        #zoom-controls button:hover {
            background: var(--vscode-button-hoverBackground);
        }

        #zoom-level {
            padding: 5px 10px;
            color: var(--vscode-foreground);
        }

        #tree-wrapper {
            width: 100%;
            height: 100vh;
            overflow: auto;
            position: relative;
        }

        #tree-container {
            display: inline-flex;
            justify-content: center;
            padding: 100px;
            min-height: 100%;
            min-width: 100%;
            transform-origin: top left;
            transition: transform 0.2s ease;
        }

        .tree {
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .node {
            display: flex;
            flex-direction: column;
            align-items: center;
            margin: 10px;
            position: relative;
        }

        .node-content {
            padding: 12px 20px;
            border-radius: 8px;
            border: 2px solid var(--vscode-editorWidget-border);
            background-color: var(--vscode-editor-background);
            text-align: center;
            min-width: 120px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
            position: relative;
            z-index: 2;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }

        .node-content:hover {
            transform: scale(1.05);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }

        .node-content.abstract {
            background-color: var(--vscode-editorWarning-background);
            border-color: var(--vscode-editorWarning-foreground);
            font-style: italic;
        }

        .node-content.mandatory {
            border-width: 3px;
            font-weight: bold;
        }

        .node-name {
            font-size: 14px;
            margin-bottom: 4px;
        }

        .node-badges {
            font-size: 10px;
            color: var(--vscode-descriptionForeground);
            display: flex;
            gap: 6px;
            justify-content: center;
            flex-wrap: wrap;
        }

        .badge {
            padding: 2px 6px;
            border-radius: 3px;
            background-color: var(--vscode-badge-background);
            color: var(--vscode-badge-foreground);
        }

        .badge.type-and {
            background-color: #4a90e2;
        }

        .badge.type-or {
            background-color: #e2a54a;
        }

        .badge.type-alt {
            background-color: #e24a4a;
        }

        .children-container {
            display: flex;
            flex-direction: row;
            gap: 30px;
            margin-top: 40px;
            position: relative;
        }

        .vertical-line {
            position: absolute;
            top: -40px;
            left: 50%;
            width: 2px;
            height: 40px;
            background-color: var(--vscode-editorWidget-border);
            transform: translateX(-50%);
        }

        .horizontal-line {
            position: absolute;
            top: -40px;
            left: 0;
            right: 0;
            height: 2px;
            background-color: var(--vscode-editorWidget-border);
        }

        .child-connector {
            position: absolute;
            top: -40px;
            left: 50%;
            width: 2px;
            height: 40px;
            background-color: var(--vscode-editorWidget-border);
            transform: translateX(-50%);
        }

        .composition-marker {
            position: absolute;
            top: -50px;
            left: 50%;
            transform: translateX(-50%);
            padding: 4px 10px;
            background-color: var(--vscode-editor-background);
            border: 2px solid var(--vscode-editorWidget-border);
            border-radius: 12px;
            font-size: 11px;
            font-weight: bold;
            z-index: 3;
            white-space: nowrap;
        }

        .composition-marker.alt {
            background-color: #e24a4a;
            color: white;
            border-color: #c03030;
        }

        .composition-marker.or {
            background-color: #e2a54a;
            color: white;
            border-color: #c08530;
        }

        .composition-marker.and {
            background-color: #4a90e2;
            color: white;
            border-color: #3070c0;
        }

        .no-model {
            text-align: center;
            padding: 40px;
            font-size: 16px;
            color: var(--vscode-descriptionForeground);
        }

        /* Modal Dialog */
        #modal-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.6);
            z-index: 9998;
            justify-content: center;
            align-items: center;
        }

        #modal-overlay.show {
            display: flex;
        }

        #confirm-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.6);
            z-index: 10000;
            justify-content: center;
            align-items: center;
        }

        #confirm-overlay.show {
            display: flex;
        }

        #modal-dialog {
            background: var(--vscode-editor-background);
            border: 1px solid var(--vscode-editorWidget-border);
            border-radius: 8px;
            padding: 20px;
            min-width: 400px;
            max-width: 600px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
        }

        #modal-dialog h2 {
            margin: 0 0 20px 0;
            color: var(--vscode-foreground);
        }

        .form-group {
            margin-bottom: 15px;
        }

        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: 500;
        }

        .form-group input[type="text"],
        .form-group select {
            width: 100%;
            padding: 8px;
            background: var(--vscode-input-background);
            color: var(--vscode-input-foreground);
            border: 1px solid var(--vscode-input-border);
            border-radius: 4px;
            font-family: inherit;
        }

        .form-group input[type="checkbox"] {
            margin-right: 8px;
        }

        .checkbox-group {
            display: flex;
            align-items: center;
            padding: 8px 0;
        }

        .button-group {
            display: flex;
            gap: 10px;
            margin-top: 20px;
            justify-content: space-between;
        }

        .button-group-left {
            display: flex;
            gap: 10px;
        }

        .button-group-right {
            display: flex;
            gap: 10px;
        }

        .button-group button {
            padding: 8px 16px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-family: inherit;
        }

        .button-primary {
            background: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
        }

        .button-primary:hover {
            background: var(--vscode-button-hoverBackground);
        }

        .button-secondary {
            background: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
        }

        .button-secondary:hover {
            background: var(--vscode-button-secondaryHoverBackground);
        }

        .button-danger {
            background: #d32f2f;
            color: white;
        }

        .button-danger:hover {
            background: #b71c1c;
        }

        hr {
            border: none;
            border-top: 1px solid var(--vscode-editorWidget-border);
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div id="zoom-controls">
        <button id="zoom-out">-</button>
        <span id="zoom-level">100%</span>
        <button id="zoom-in">+</button>
        <button id="zoom-reset">Reset</button>
    </div>

    <div id="tree-wrapper">
        <div id="tree-container"></div>
    </div>

    <div id="modal-overlay">
        <div id="modal-dialog">
            <h2 id="modal-title">Edit Feature</h2>
            <div class="form-group">
                <label for="feature-name">Name:</label>
                <input type="text" id="feature-name" />
            </div>
            <div class="form-group">
                <label for="feature-type">Type:</label>
                <select id="feature-type">
                    <option value="feature">Feature (leaf node)</option>
                    <option value="and">AND (all children)</option>
                    <option value="or">OR (1 or more children)</option>
                    <option value="alt">XOR/ALT (exactly 1 child)</option>
                </select>
            </div>
            <div class="form-group checkbox-group">
                <input type="checkbox" id="feature-mandatory" />
                <label for="feature-mandatory">Mandatory (must be selected with parent)</label>
            </div>
            <div class="form-group checkbox-group">
                <input type="checkbox" id="feature-abstract" />
                <label for="feature-abstract">Abstract (no implementation)</label>
            </div>
            
            <hr />
            
            <h3>Add Child Feature</h3>
            <div class="form-group">
                <label for="child-name">Child Name:</label>
                <input type="text" id="child-name" />
            </div>
            <div class="form-group checkbox-group">
                <input type="checkbox" id="child-mandatory" />
                <label for="child-mandatory">Mandatory</label>
            </div>
            <div class="form-group checkbox-group">
                <input type="checkbox" id="child-abstract" />
                <label for="child-abstract">Abstract</label>
            </div>
            
            <div class="button-group">
                <div class="button-group-left">
                    <button class="button-primary" id="add-child-btn">Add Child</button>
                    <button class="button-danger" id="remove-node-btn">Remove Node</button>
                </div>
                <div class="button-group-right">
                    <button class="button-secondary" id="modal-cancel">Cancel</button>
                    <button class="button-primary" id="modal-save">Save Changes</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Confirmation Dialog -->
    <div id="confirm-overlay">
        <div id="modal-dialog">
            <h2>Confirm Deletion</h2>
            <p id="confirm-message"></p>
            <div class="button-group">
                <div class="button-group-right">
                    <button class="button-secondary" id="confirm-cancel">Cancel</button>
                    <button class="button-danger" id="confirm-delete">Delete</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        const vscode = acquireVsCodeApi();
        const modelData = ${modelJson};
        const hasModel = ${hasModel};
        
        let currentZoom = 1.0;
        let currentEditingNode = null;
        let currentEditingPath = null;

        // Zoom functionality
        const treeContainer = document.getElementById('tree-container');
        const zoomInBtn = document.getElementById('zoom-in');
        const zoomOutBtn = document.getElementById('zoom-out');
        const zoomResetBtn = document.getElementById('zoom-reset');
        const zoomLevel = document.getElementById('zoom-level');

        function updateZoom() {
            treeContainer.style.transform = \`scale(\${currentZoom})\`;
            zoomLevel.textContent = Math.round(currentZoom * 100) + '%';
        }

        zoomInBtn.addEventListener('click', () => {
            currentZoom = Math.min(currentZoom + 0.1, 2.0);
            updateZoom();
        });

        zoomOutBtn.addEventListener('click', () => {
            currentZoom = Math.max(currentZoom - 0.1, 0.3);
            updateZoom();
        });

        zoomResetBtn.addEventListener('click', () => {
            currentZoom = 1.0;
            updateZoom();
        });

        // Mouse wheel zoom
        document.getElementById('tree-wrapper').addEventListener('wheel', (e) => {
            if (e.ctrlKey || e.metaKey) {
                e.preventDefault();
                if (e.deltaY < 0) {
                    currentZoom = Math.min(currentZoom + 0.05, 2.0);
                } else {
                    currentZoom = Math.max(currentZoom - 0.05, 0.3);
                }
                updateZoom();
            }
        });

        // Modal functionality
        const modalOverlay = document.getElementById('modal-overlay');
        const modalCancel = document.getElementById('modal-cancel');
        const modalSave = document.getElementById('modal-save');
        const addChildBtn = document.getElementById('add-child-btn');
        const removeNodeBtn = document.getElementById('remove-node-btn');
        
        // Confirmation dialog
        const confirmOverlay = document.getElementById('confirm-overlay');
        const confirmMessage = document.getElementById('confirm-message');
        const confirmCancel = document.getElementById('confirm-cancel');
        const confirmDelete = document.getElementById('confirm-delete');
        let pendingRemoveAction = null;

        function showModal(feature, path) {
            currentEditingNode = feature;
            currentEditingPath = path;

            document.getElementById('feature-name').value = feature.name;
            document.getElementById('feature-type').value = feature.type;
            document.getElementById('feature-mandatory').checked = feature.mandatory;
            document.getElementById('feature-abstract').checked = feature.abstract;
            
            // Clear child inputs
            document.getElementById('child-name').value = '';
            document.getElementById('child-mandatory').checked = false;
            document.getElementById('child-abstract').checked = false;

            // Disable remove button for root node
            const removeBtn = document.getElementById('remove-node-btn');
            if (removeBtn) {
                if (path.length === 0) {
                    removeBtn.disabled = true;
                    removeBtn.style.opacity = '0.5';
                    removeBtn.style.cursor = 'not-allowed';
                } else {
                    removeBtn.disabled = false;
                    removeBtn.style.opacity = '1';
                    removeBtn.style.cursor = 'pointer';
                }
                console.log('Remove button state:', { disabled: removeBtn.disabled, path: path });
            } else {
                console.error('Remove button not found in showModal');
            }

            modalOverlay.classList.add('show');
        }

        function hideModal() {
            modalOverlay.classList.remove('show');
            currentEditingNode = null;
            currentEditingPath = null;
        }

        modalCancel.addEventListener('click', hideModal);
        modalOverlay.addEventListener('click', (e) => {
            if (e.target === modalOverlay) hideModal();
        });

        modalSave.addEventListener('click', () => {
            if (!currentEditingNode || !currentEditingPath) return;

            const updates = {
                name: document.getElementById('feature-name').value,
                type: document.getElementById('feature-type').value,
                mandatory: document.getElementById('feature-mandatory').checked,
                abstract: document.getElementById('feature-abstract').checked
            };

            vscode.postMessage({
                command: 'updateNode',
                nodePath: currentEditingPath,
                updates: updates
            });

            hideModal();
        });

        // Remove button handler
        console.log('Setting up remove button handler, button exists:', !!removeNodeBtn);
        if (removeNodeBtn) {
            removeNodeBtn.addEventListener('click', function(e) {
                console.log('Remove button clicked!');
                console.log('Button disabled:', removeNodeBtn.disabled);
                console.log('Current editing node:', currentEditingNode);
                console.log('Current editing path:', currentEditingPath);
                
                if (removeNodeBtn.disabled) {
                    console.log('Button is disabled, aborting');
                    return;
                }
                
                if (!currentEditingNode || !currentEditingPath) {
                    console.log('No editing node or path, aborting');
                    return;
                }
                
                // Check if node has children
                const hasChildren = currentEditingNode.children && currentEditingNode.children.length > 0;
                
                let message = 'Are you sure you want to remove "' + currentEditingNode.name + '"?';
                if (hasChildren) {
                    const childCount = currentEditingNode.children.length;
                    const plural = childCount > 1 ? 'ren' : '';
                    message += ' This node has ' + childCount + ' child' + plural + ', which will also be removed.';
                }
                
                // Store the action to perform if confirmed
                pendingRemoveAction = {
                    nodePath: currentEditingPath,
                    nodeName: currentEditingNode.name
                };
                
                console.log('Showing confirmation dialog with message:', message);
                console.log('Confirm overlay element:', confirmOverlay);
                console.log('Confirm message element:', confirmMessage);
                
                // Show confirmation dialog
                confirmMessage.innerHTML = message;
                confirmOverlay.classList.add('show');
                
                console.log('Confirmation dialog should now be visible');
            });
            console.log('Remove button event listener attached successfully');
        } else {
            console.error('Remove button not found!');
        }
        
        // Confirmation dialog handlers
        console.log('Setting up confirmation dialog handlers');
        console.log('Confirm cancel button:', confirmCancel);
        console.log('Confirm delete button:', confirmDelete);
        
        if (confirmCancel) {
            confirmCancel.addEventListener('click', function() {
                console.log('Cancel button clicked');
                confirmOverlay.classList.remove('show');
                pendingRemoveAction = null;
            });
        }
        
        if (confirmDelete) {
            confirmDelete.addEventListener('click', function() {
                console.log('Delete button clicked, pendingRemoveAction:', pendingRemoveAction);
                if (pendingRemoveAction) {
                    console.log('Sending removeNode message with path:', pendingRemoveAction.nodePath);
                    vscode.postMessage({
                        command: 'removeNode',
                        nodePath: pendingRemoveAction.nodePath
                    });
                    confirmOverlay.classList.remove('show');
                    hideModal();
                    pendingRemoveAction = null;
                } else {
                    console.error('No pending remove action!');
                }
            });
        }
        
        // Close confirmation dialog when clicking outside
        if (confirmOverlay) {
            confirmOverlay.addEventListener('click', function(e) {
                if (e.target === confirmOverlay) {
                    confirmOverlay.classList.remove('show');
                    pendingRemoveAction = null;
                }
            });
        }

        addChildBtn.addEventListener('click', () => {
            if (!currentEditingPath) return;

            const childName = document.getElementById('child-name').value.trim();
            if (!childName) {
                alert('Please enter a child name');
                return;
            }

            const childData = {
                name: childName,
                mandatory: document.getElementById('child-mandatory').checked,
                abstract: document.getElementById('child-abstract').checked
            };

            // Check if parent is a leaf node and auto-update to 'and'
            const shouldUpdateParent = currentEditingNode && 
                                      currentEditingNode.type === 'feature' && 
                                      (!currentEditingNode.children || currentEditingNode.children.length === 0);

            vscode.postMessage({
                command: 'addChild',
                parentPath: currentEditingPath,
                childData: childData,
                updateParentToAnd: shouldUpdateParent
            });

            hideModal();
        });

        function createNode(feature, path = [], isRoot = false) {
            const node = document.createElement('div');
            node.className = 'node';

            const content = document.createElement('div');
            content.className = 'node-content';
            
            // Single-click to edit
            content.addEventListener('click', (e) => {
                e.stopPropagation();
                showModal(feature, path);
            });
            
            if (feature.abstract) {
                content.classList.add('abstract');
            }
            if (feature.mandatory) {
                content.classList.add('mandatory');
            }

            const name = document.createElement('div');
            name.className = 'node-name';
            name.textContent = feature.name;
            content.appendChild(name);

            const badges = document.createElement('div');
            badges.className = 'node-badges';

            if (feature.type !== 'feature') {
                const typeBadge = document.createElement('span');
                typeBadge.className = \`badge type-\${feature.type}\`;
                typeBadge.textContent = feature.type.toUpperCase();
                badges.appendChild(typeBadge);
            }

            if (feature.mandatory) {
                const mandatoryBadge = document.createElement('span');
                mandatoryBadge.className = 'badge';
                mandatoryBadge.textContent = 'mandatory';
                badges.appendChild(mandatoryBadge);
            }

            if (feature.abstract) {
                const abstractBadge = document.createElement('span');
                abstractBadge.className = 'badge';
                abstractBadge.textContent = 'abstract';
                badges.appendChild(abstractBadge);
            }

            content.appendChild(badges);
            node.appendChild(content);

            if (feature.children && feature.children.length > 0) {
                const childrenContainer = document.createElement('div');
                childrenContainer.className = 'children-container';

                // Add composition type marker
                if (feature.type === 'alt' || feature.type === 'or' || (feature.type === 'and' && feature.children.length > 1)) {
                    const marker = document.createElement('div');
                    marker.className = \`composition-marker \${feature.type}\`;
                    if (feature.type === 'alt') {
                        marker.textContent = 'XOR (pick 1)';
                    } else if (feature.type === 'or') {
                        marker.textContent = 'OR (pick 1+)';
                    } else {
                        marker.textContent = 'AND (all)';
                    }
                    childrenContainer.appendChild(marker);
                }

                // Add vertical line from parent
                const verticalLine = document.createElement('div');
                verticalLine.className = 'vertical-line';
                childrenContainer.appendChild(verticalLine);

                // Add horizontal line if multiple children
                if (feature.children.length > 1) {
                    const horizontalLine = document.createElement('div');
                    horizontalLine.className = 'horizontal-line';
                    childrenContainer.appendChild(horizontalLine);
                }

                feature.children.forEach((child, index) => {
                    const childPath = [...path, index];
                    const childNode = createNode(child, childPath);
                    
                    // Add connector line for each child
                    if (feature.children.length > 1) {
                        const connector = document.createElement('div');
                        connector.className = 'child-connector';
                        childNode.insertBefore(connector, childNode.firstChild);
                    }
                    
                    childrenContainer.appendChild(childNode);
                });

                node.appendChild(childrenContainer);
            }

            return node;
        }

        function renderTree() {
            const container = document.getElementById('tree-container');
            container.innerHTML = '';
            
            if (!hasModel || !modelData) {
                container.innerHTML = '<div class="no-model">No feature model loaded. Load a model to visualize the tree.</div>';
                return;
            }

            const tree = document.createElement('div');
            tree.className = 'tree';
            tree.appendChild(createNode(modelData, [], true));
            container.appendChild(tree);
        }

        renderTree();
    </script>
</body>
</html>`;
    }
}
