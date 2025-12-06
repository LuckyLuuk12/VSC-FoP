import { spawn } from "child_process";
import * as fs from "fs";
import { SubTreePreprocessor } from "./SubTreePreprocessor";

export class JavaBridge {
    constructor(private jarPath: string) {
        console.log(`[JavaBridge] Initialized with jar path: ${jarPath}`);
    }

    call(args: string[]): Promise<string> {
        return new Promise((resolve, reject) => {
            // Check if jar exists
            if (!fs.existsSync(this.jarPath)) {
                reject(new Error(`JAR file not found: ${this.jarPath}. Run 'cd java-backend && gradlew build' to build it.`));
                return;
            }

            console.log(`[JavaBridge] Executing: java -jar ${this.jarPath} ${args.join(' ')}`);
            const proc = spawn("java", ["-jar", this.jarPath, ...args]);

            let output = "";
            let errorOutput = "";
            
            proc.stdout.on("data", (d: any) => {
                const text = d.toString();
                output += text;
                console.log(`[JAVA stdout] ${text}`);
            });
            
            proc.stderr.on("data", (d: any) => {
                const text = d.toString();
                errorOutput += text;
                console.error(`[JAVA stderr] ${text}`);
            });

            proc.on("error", (err) => {
                console.error(`[JavaBridge] Process error:`, err);
                reject(err);
            });
            
            proc.on("close", (code) => {
                console.log(`[JavaBridge] Process exited with code ${code}`);
                if (code !== 0) {
                    reject(new Error(`Java process exited with code ${code}. Error: ${errorOutput}`));
                } else {
                    resolve(output.trim());
                }
            });
        });
    }

async loadModel(modelPath: string): Promise<any> {
    const preprocessor = new SubTreePreprocessor();
    const processedXML = await preprocessor.preprocess(modelPath);

    const tempPath = modelPath + ".merged.xml";
    fs.writeFileSync(tempPath, processedXML, "utf8");

    return await this.call(["loadModel", tempPath]);
}
}
