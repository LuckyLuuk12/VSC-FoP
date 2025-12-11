import * as fs from 'fs';
import * as path from 'path';
import * as xml2js from 'xml2js';

export class SubTreePreprocessor {
    private parser = new xml2js.Parser();
    private builder = new xml2js.Builder();

    async preprocess(modelPath: string): Promise<string> {
        const xml = fs.readFileSync(modelPath, "utf8");
        const json = await this.parser.parseStringPromise(xml);

        await this.expandSubTrees(json, path.dirname(modelPath));

        return this.builder.buildObject(json);
    }

    private async expandSubTrees(node: any, basePath: string): Promise<void> {
        if (typeof node !== "object" || node === null) return;

        if (node.subtree && Array.isArray(node.subtree)) {
            const expandedFeatures: any[] = [];

            for (const subtree of node.subtree) {
                const name = subtree.$.name;
                const subtreePath = path.join(basePath, name, "model.xml");

                if (!fs.existsSync(subtreePath)) {
                    throw new Error(`SubTree "${name}" not found at ${subtreePath}`);
                }

                const xml = fs.readFileSync(subtreePath, "utf8");
                const json = await this.parser.parseStringPromise(xml);

                const subtreeStruct = json.featureModel?.struct;
                if (!subtreeStruct) {
                    throw new Error(`SubTree "${name}" has no <struct> section`);
                }

                expandedFeatures.push(...subtreeStruct);
            }

            delete node.subtree;
            node.feature = [...(node.feature || []), ...expandedFeatures];
        }

        for (const key of Object.keys(node)) {
            const child = node[key];
            if (Array.isArray(child)) {
                for (const c of child) {
                    await this.expandSubTrees(c, basePath);
                }
            } else if (typeof child === "object") {
                await this.expandSubTrees(child, basePath);
            }
        }
    }
}
