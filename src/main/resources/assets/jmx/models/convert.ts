function removeJmx(s: string): string {
    return s.startsWith("jmx_") ? s.substr(4) : s;
}

function replace(text: string, replacement: string): (s: string) => string {
    return s => s === text ? replacement : text;
}

function updateFormat(val: any): any {
    if (val.elements) {
        val.elements = val.elements.map((element: any) => {
            return {
                ...element,
                faces: Object.fromEntries(Object.entries(element.faces).map(([dir, face]: [string, any]) => {
                    const newFace = {...face};
                    if (newFace.layered_textures) {
                        newFace.jmx_layers = newFace.layered_textures.map((layer: any) => {
                            return {
                                ...layer.mapKeys(replace("jmx_tex", "texture")).mapKeys(removeJmx),
                                material: face.jmx_material,
                            };
                        });
                    }
                    delete newFace.layered_textures;
                    delete newFace.jmx_material;
                    return [dir, newFace];
                })),
            };
        });
    }
    return val;
}

// @ts-ignore
import { walk, move, exists } from "https://deno.land/std@0.78.0/fs/mod.ts";

// @ts-ignore
Object.prototype.mapKeys = function(f: (key: string) => string): Object {
    return Object.fromEntries(Object.entries(this).map(([key, val]: [string, any]) => {
        return [f(key), val];
    }));
}

if (Deno.args[0] === "update") {
    for await (const entry of walk("block", {includeDirs: false, exts: ["json"]})) {
        const model = JSON.parse(await Deno.readTextFile(entry.path));
        const backup = entry.path + ".bak";
        if (!await exists(backup)) {
            await move(entry.path, entry.path + ".bak");
        }
        await Deno.writeTextFile(entry.path, JSON.stringify(updateFormat(model), null, 4));
    }
} else if (Deno.args[0] === "undo") {
    for await (const entry of walk("block", {includeDirs: false, exts: ["bak"]})) {
        await move(entry.path, entry.path.replaceAll(".bak", ""), {overwrite: true});
    }
} else if (Deno.args[0] === "find") {
    for await(const entry of walk("block", {includeDirs: false, exts: ["json"]})) {
        const model = JSON.parse(await Deno.readTextFile(entry.path));
        if (model.jmx) {
            if (model.jmx.materials) {
                console.log(entry.path);
            }
        } else if (model.frex) {
            if (model.frex.materials) {
                console.log(entry.path);
            }
        }
    }
}
