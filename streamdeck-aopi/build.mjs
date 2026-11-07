import { build } from "esbuild";

await build({
    entryPoints: ["src/plugin.ts"],
    bundle: true,
    platform: "node",
    target: "node24",
    format: "esm",
    banner: { js: 'import { createRequire } from "node:module"; const require = createRequire(import.meta.url);' },
    outfile: "com.aetherone.streamdeck.sdPlugin/bin/plugin.mjs"
});
