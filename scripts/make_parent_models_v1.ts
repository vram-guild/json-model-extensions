#!/usr/bin/env -S deno run --allow-read --allow-write --unstable

import {Model} from "./vanilla.ts";
import {JmxModel, Layers} from "./jmx.ts";

import {existsSync, walkSync, ensureFileSync} from "https://deno.land/std@0.82.0/fs/mod.ts";
import {assertEquals} from "https://deno.land/std@0.82.0/testing/asserts.ts";
import {parse} from "https://deno.land/std@0.82.0/flags/mod.ts";

Deno.chdir("../src/main/resources/assets");

const args = parse(Deno.args);
// //region Validate type definitions using known models
if (args.check) {
    function check(path: string) {
        const json = JSON.parse(Deno.readTextFileSync(path));

        try {
            Model.check(json);
        } catch (e) {
            console.log('not valid: ' + path);
            console.log(e);
            console.log();
            return;
        }

        let layered_textures = undefined;
        if (json.jmx && json.jmx.textures && json.jmx.textures.layered_textures) {
            layered_textures = json.jmx.textures.layered_textures;
            delete json.jmx.textures.layered_textures;
        }

        try {
            JmxModel.check(json);
            if (layered_textures) {
                Layers.check(layered_textures);
            }
        } catch (e) {
            console.log('not jmx valid: ' + path);
            console.log(e);
            console.log();
            return;
        }

        const model = json as JmxModel;

        if (layered_textures) {
            // @ts-ignore // guaranteed to be present from above
            model.jmx.textures.layered_textures = layered_textures;
        }
    }

    function checkAll(entries: IterableIterator<{ isFile: boolean, path: string }>) {
        for (const entry of entries) {
            if (!entry.isFile) {
                continue;
            }

            check(entry.path);
        }
    }

    checkAll(walkSync("vanilla_mc/models/block"));
    checkAll(walkSync("jmx/models/block/v1"));
}
//endregion

//region Create JMX versions of models from args
if (args.create) {
    type Identifier = {
        namespace: string,
        path: string[],
    };

    function parseId(id: string): Identifier {
        const parts = id.split(":");
        if (parts.length != 2) {
            return {
                namespace: "minecraft",
                path: id.split("/"),
            };
        } else {
            return {
                namespace: parts[0],
                path: parts[1].split("/"),
            };
        }
    }

    function idStr(id: Identifier): string {
        return `${id.namespace}:${id.path.join("/")}`;
    }

    function modelLocation(id: Identifier): string {
        return `${id.namespace == "minecraft" ? "vanilla_mc" : id.namespace}/models/${id.path.join("/")}.json`;
    }

    function toJmx(id: Identifier): Identifier {
        if (id.namespace == "jmx") {
            return id;
        } else {
            let path = [...id.path];
            path.splice(1, 0, "v1");
            if (path[path.length - 1] != "block" && path[path.length - 1] != "thin_block") {
                path[path.length - 1] = "jmx_" + path[path.length - 1];
            }
            return {
                namespace: "jmx",
                path,
            };
        }
    }

    function makeParentModels(id: Identifier): [Identifier, JmxModel][] {
        const path = modelLocation(id);
        console.log(`visiting ${idStr(id)}`);

        const model = JSON.parse(Deno.readTextFileSync(path)) as Model;

        const parentHasLayers = (() => {
            if (model.parent) {
                const parentId = parseId(model.parent);
                const parentCount = writeParentModels(parentId);
                const jmxParent = toJmx(parentId);
                model.parent = idStr(jmxParent);
                return parentCount > 1;
            } else {
                return false;
            }
        })();

        const makeWithLayerCount: (layerCount: number) => [Identifier, JmxModel] = (layerCount: number) => {
            function extractBase(s: string): string {
                // @ts-ignore
                return /#?(.*)/.exec(s)[1];
            }

            const jmxModel = {...model} as JmxModel;

            if (jmxModel.parent) {
                const parentId = parseId(jmxModel.parent);

                if (parentHasLayers) {
                    parentId.path.splice(2, 0, layerCount.toString());
                    jmxModel.parent = idStr(parentId);
                }
            }

            if (model.textures) {
                const textureCount = Object.entries(model.textures).length;
                // @ts-ignore // only add layers if there's a non-particle texture
                if (textureCount - !!model.textures.particle) {
                    function existing<A, B>(x: B | undefined, _default: B): B {
                        return !!x ? x : _default;
                    }

                    const existingJmx = existing(jmxModel.jmx, {});
                    const existingTextures = existing(existingJmx.textures, {});
                    const existingLayeredTextures = existing(existingTextures.layered_textures, []);
                    const existingMaterials = existing(existingJmx.materials, []);
                    const existingTags = existing(existingJmx.tags, []);
                    const existingColors = existing(existingJmx.colors, []);

                    const layer = (kind: string) =>
                        Object.fromEntries(
                            // @ts-ignore
                            Object.entries(model.textures)
                                .filter(([key, _]) => key !== "particle")
                                .map(([key, value]) => {
                                    const toJmx = (s: string, start: string = "") => {
                                        const base = extractBase(s);
                                        return `${start}jmx_${kind}_${base}`;
                                    }
                                    return [toJmx(key), toJmx(value, "#")];
                                })
                        );

                    const layers = (existing: Layers, base: string): Layers => [...existing].concat(Array(layerCount).fill(layer(base)));

                    jmxModel.jmx = {
                        ...existingJmx,
                        // @ts-ignore
                        textures: {
                            ...existingTextures,
                            layered_textures: layers(existingLayeredTextures, "tex"),
                        },
                        materials: layers(existingMaterials, "mat"),
                        tags: layers(existingTags, "tag"),
                        colors: layers(existingColors, "color"),
                    }
                }

                if (model.textures.particle) {
                    if (!jmxModel.jmx) {
                        jmxModel.jmx = {};
                    }
                    if (!jmxModel.jmx.textures) {
                        jmxModel.jmx.textures = {};
                    }
                    jmxModel.jmx.textures.particle = "#jmx_tex_particle";
                }
            }

            if (model.elements) {
                // @ts-ignore
                jmxModel.elements = jmxModel.elements.map(element => {
                    return {
                        ...element,
                        faces: Object.fromEntries(Object.entries(element.faces).map(([dir, face]) => {
                            // @ts-ignore
                            const base = extractBase(face.texture);

                            // @ts-ignore
                            const layer = base === "texture"
                                ? {
                                    texture: "#jmx_texture",
                                    material: "#jmx_material",
                                    tag: "#jmx_tag",
                                    color: "#jmx_color",
                                }
                                : {
                                    texture: `#jmx_tex_${base}`,
                                    material: `#jmx_mat_${base}`,
                                    tag: `#jmx_tag_${base}`,
                                    color: `#jmx_color_${base}`,
                                };

                            return [dir, {
                                ...face,
                                jmx_layers: Array(layerCount).fill(layer),
                            }];
                        })),
                    };
                });
                let jmxId = toJmx(id);
                jmxId.path.splice(2, 0, layerCount.toString());
            }

            jmxModel.jmx_version = 1;

            let jmxId = toJmx(id);
            jmxId.path.splice(2, 0, layerCount.toString());
            return [jmxId, jmxModel];
        };


        if (model.elements || model.textures) {
            return [2, 3].map(makeWithLayerCount);
        } else {
            const jmxModel = {...model} as JmxModel;
            jmxModel.jmx_version = 1;
            return [[toJmx(id), jmxModel]];
        }
    }

    function writeParentModels(id: Identifier): number {
        const parentModels = makeParentModels(id);
        for (const [modelId, model] of parentModels) {
            const path = modelLocation(modelId);
            ensureFileSync(path);
            Deno.writeTextFileSync(path, JSON.stringify(model, null, 4));
        }
        return parentModels.length;
    }

    const files = [
        "minecraft:block/crop",
        "minecraft:block/cross",
        "minecraft:block/cube",
        "minecraft:block/cube_all",
        "minecraft:block/cube_bottom_top",
        "minecraft:block/cube_column",
        "minecraft:block/cube_directional",
        "minecraft:block/cube_mirrored",
        "minecraft:block/cube_mirrored_all",
        "minecraft:block/cube_top",
        "minecraft:block/door_bottom",
        "minecraft:block/door_bottom_rh",
        "minecraft:block/door_top",
        "minecraft:block/door_top_rh",
        "minecraft:block/fence_inventory",
        "minecraft:block/fence_post",
        "minecraft:block/fence_side",
        "minecraft:block/orientable",
        "minecraft:block/orientable_vertical",
        "minecraft:block/orientable_with_bottom",
        "minecraft:block/slab",
        "minecraft:block/slab_top",
        "minecraft:block/stairs",
        "minecraft:block/stem_fruit",
        "minecraft:block/stem_growth0",
        "minecraft:block/stem_growth1",
        "minecraft:block/stem_growth2",
        "minecraft:block/stem_growth3",
        "minecraft:block/stem_growth4",
        "minecraft:block/stem_growth5",
        "minecraft:block/stem_growth6",
        "minecraft:block/stem_growth7",
        "minecraft:block/template_farmland",
        "minecraft:block/template_fence_gate",
        "minecraft:block/template_fence_gate_open",
        "minecraft:block/template_fence_gate_wall",
        "minecraft:block/template_fence_gate_wall_open",
        "minecraft:block/template_glass_pane_noside",
        "minecraft:block/template_glass_pane_noside_alt",
        "minecraft:block/template_glass_pane_post",
        "minecraft:block/template_glass_pane_side",
        "minecraft:block/template_glass_pane_side_alt",
        "minecraft:block/template_glazed_terracotta",
        "minecraft:block/template_orientable_trapdoor_bottom",
        "minecraft:block/template_orientable_trapdoor_open",
        "minecraft:block/template_orientable_trapdoor_top",
        "minecraft:block/template_piston",
        "minecraft:block/template_piston_head",
        "minecraft:block/template_piston_head_short",
        "minecraft:block/template_rail_raised_ne",
        "minecraft:block/template_rail_raised_sw",
        "minecraft:block/template_torch",
        "minecraft:block/template_trapdoor_bottom",
        "minecraft:block/template_trapdoor_open",
        "minecraft:block/template_trapdoor_top",
        "minecraft:block/template_wall_post",
        "minecraft:block/template_wall_side",
        "minecraft:block/wall_inventory",
        "minecraft:block/template_wall_side_tall",
        "minecraft:block/carpet",
    ];

    if (existsSync("jmx/models")) {
        Deno.removeSync("jmx/models", {recursive: true});
    }

    for (const arg of files) {
        writeParentModels(parseId(arg));
    }
}
//endregion

//region Diff old and new models
if (args.diff) {
    const dir = Deno.cwd();

    Deno.chdir("jmx");

    for (const entry of walkSync("models_old")) {
        if (!entry.isFile) {
            continue;
        }

        const oldPath = entry.path;
        const newPath = entry.path.replaceAll("models_old", "models");
        if (!existsSync(newPath)) {
            continue;
        }

        const oldModel = JSON.parse(Deno.readTextFileSync(oldPath)) as JmxModel;
        const newModel = JSON.parse(Deno.readTextFileSync(newPath)) as JmxModel;

        // some of the original models are wrong
        const broken = [
            "models_old/block/v1/2/jmx_orientable_vertical.json",
            "models_old/block/v1/3/jmx_orientable_vertical.json",
            "models_old/block/v1/2/jmx_template_glazed_terracotta.json",
            "models_old/block/v1/3/jmx_template_glazed_terracotta.json",
            "models_old/block/v1/2/jmx_cube_bottom_top.json",
            "models_old/block/v1/3/jmx_cube_bottom_top.json",
            "models_old/block/v1/2/jmx_orientable_with_bottom.json",
            "models_old/block/v1/3/jmx_orientable_with_bottom.json",
            "models_old/block/v1/2/jmx_template_fence_gate_wall.json",
            "models_old/block/v1/3/jmx_template_fence_gate_wall.json",
            "models_old/block/v1/2/jmx_template_farmland.json",
            "models_old/block/v1/3/jmx_template_farmland.json",
            "models_old/block/v1/2/jmx_cube_top.json",
            "models_old/block/v1/3/jmx_cube_top.json",
            "models_old/block/v1/2/jmx_orientable.json",
            "models_old/block/v1/3/jmx_orientable.json",
            "models_old/block/v1/2/jmx_stem_fruit.json",
            "models_old/block/v1/3/jmx_stem_fruit.json",
            "models_old/block/v1/2/jmx_cube_column.json",
            "models_old/block/v1/3/jmx_cube_column.json",
        ];
        if (broken.includes(entry.path)) {
            continue;
        }

        try {
            assertEquals(oldModel, newModel);
        } catch (e) {
            console.log("==== ERROR =====");
            console.log(`${oldPath} and ${newPath} are different!`);
            throw e;
        }
    }

    Deno.chdir(dir);
}
//endregion

if (!args.check && !args.create && !args.diff) {
    console.log(`
USAGE:
Requires Deno (https://deno.land)

CAUTION:
This script will remove all files in \`src/main/resources/assets/jmx/models\`.
Use git to restore any non-v1 models.

ARGS:
--check      checks type definitions against vanilla and JMX models
--create     creates new parent models based on a variable defined below
--diff       diffs created models with old models
`.trim());
}
