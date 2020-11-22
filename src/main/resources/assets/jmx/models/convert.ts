// @ts-ignore
import { walk } from "https://deno.land/std@0.78.0/fs/mod.ts";

function updateJmx(jmx: any): any {
    if (jmx.materials && Array.isArray(jmx.materials)) {
        function make(base: string): any[] {
            return jmx.materials.map((material: any) => {
                return Object.fromEntries(Object.entries(material).map(([key, value]: [string, any]) => {
                    return [key.replace("mat", base), value.replace("mat", base)];
                }));
            });
        }

        jmx.tags = make("tag");
        jmx.colors = make("color");
    }
    return jmx;
}

// const entry = {path: "block/2/jmx_crop.json"};
for await (const entry of walk("block", {includeDirs: false, exts: ["json"]})) {
    const model = JSON.parse(await Deno.readTextFile(entry.path));

    if (model.elements) {
        model.elements = model.elements.map((element: any) => {
            return {
                ...element,
                faces: Object.fromEntries(Object.entries(element.faces).map(([dir, face]: [string, any]) => {
                    const newFace = {...face};

                    if (newFace.jmx_layers) {
                        newFace.jmx_layers = newFace.jmx_layers.map((layer: any) => {
                            function make(base: string): string {
                                return layer.texture.replace("tex", base);
                            }
                            return {
                                ...layer,
                                tag: make("tag"),
                                color: make("color"),
                            };
                        });
                    }

                    return [dir, newFace];
                })),
            }
        });
    }

    if (model.jmx) {
        model.jmx = updateJmx(model.jmx);
    }
    if (model.frex) {
        model.frex = updateJmx(model.frex);
    }

    // console.log(Deno.inspect(model, {depth: 100}));
    await Deno.writeTextFile(entry.path, JSON.stringify(model, null, 4));
}
