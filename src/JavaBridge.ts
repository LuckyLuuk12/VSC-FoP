import { spawn } from "child_process";

export class JavaBridge {
    constructor(private jarPath: string) {}

    call(args: string[]): Promise<string> {
        return new Promise((resolve, reject) => {
            const proc = spawn("java", ["-jar", this.jarPath, ...args]);

            let output = "";
            proc.stdout.on("data", (d: any) => output += d.toString());
            proc.stderr.on("data", (d: any) => console.error("[JAVA]", d.toString()));

            proc.on("error", reject);
            proc.on("close", () => resolve(output.trim()));
        });
    }
}
