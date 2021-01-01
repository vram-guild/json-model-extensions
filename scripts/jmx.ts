// @ts-ignore
import {Array, Partial, String, Dictionary, Number, Static, Boolean} from "https://raw.githubusercontent.com/alex5nader/runtypes/master/src/index.ts";
import {
    Face,
    FacesOf,
    ModelElementOf,
    ModelOf,
} from "./vanilla.ts";

export type Layers = Static<typeof Layers>;
export const Layers = Array(Dictionary(String));

export type JmxTextures = Static<typeof JmxTextures>;
export const JmxTextures = Dictionary(String).And(Partial({
    layered_textures: Layers,
}));

export type JmxModelExt = Static<typeof JmxModelExt>;
export const JmxModelExt = Partial({
    textures: JmxTextures,
    materials: Layers,
    tags: Layers,
    colors: Layers,
});

export type FaceLayer = Static<typeof FaceLayer>;
export const FaceLayer = Partial({
    texture: String,
    material: String,
    tag: String,
    color: String,
});

export type JmxLayers = Static<typeof JmxLayers>;
export const JmxLayers = Array(FaceLayer);

export type FaceExt = Static<typeof FaceExt>;
export const FaceExt = Face.And(Partial({
    jmx_layers: JmxLayers,
}));

export type JmxModel = Static<typeof JmxModel>;
export const JmxModel = ModelOf(ModelElementOf(FacesOf(FaceExt))).And(Partial({
    jmx_version: Number,
    jmx: JmxModelExt,
}));
