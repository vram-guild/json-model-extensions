// @ts-ignore
import {Array, Partial, Record, Tuple, String, Dictionary, Boolean, Number, Union, Literal, Static, Runtype} from "https://raw.githubusercontent.com/alex5nader/runtypes/master/src/index.ts";

export type Vec3 = Static<typeof Vec3>;
export const Vec3 = Tuple(Number, Number, Number);

export type Transformation = Static<typeof Transformation>;
export const Transformation = Partial({
    rotation: Vec3,
    translation: Vec3,
    scale: Vec3,
});

export type ModelTransformation = Static<typeof ModelTransformation>;
export const ModelTransformation = Partial({
    thirdperson_righthand: Transformation,
    thirdperson_lefthand: Transformation,
    firstperson_righthand: Transformation,
    firstperson_lefthand: Transformation,
    gui: Transformation,
    head: Transformation,
    ground: Transformation,
    fixed: Transformation,
});

export type TextureMap = Static<typeof TextureMap>;
export const TextureMap = Partial({
    particle: String,
}).And(Dictionary(String));

export type Rotation = Static<typeof Rotation>;
export const Rotation = Record({
    origin: Vec3,
    axis: Union(Literal("x"), Literal("y"), Literal("z")),
    angle: Union(Literal(-45), Literal(-22.5), Literal(0), Literal(22.5), Literal(45)),
}).And(Partial({
    rescale: Boolean
}));

export type Direction = Static<typeof Direction>;
export const Direction = Union(Literal("down"), Literal("up"), Literal("north"), Literal("south"), Literal("west"), Literal("east"));

export type Face = Static<typeof Face>;
export const Face = Record({
    texture: String,
}).And(Partial({
    uv: Tuple(Number, Number, Number, Number),
    cullface: Direction,
    rotation: Union(Literal(0), Literal(90), Literal(180), Literal(270)),
    tintindex: Number,
}));

export function FacesOf<A extends Runtype>(Face: A) {
    return Partial({
        down: Face,
        up: Face,
        north: Face,
        south: Face,
        west: Face,
        east: Face,
    });
}

export type Faces = Static<typeof Faces>;
export const Faces = FacesOf(Face);

export function ModelElementOf<A extends Runtype>(Faces: A) {
    return Record({
        from: Vec3,
        to: Vec3,
        faces: Faces
    }).And(Partial({
        rotation: Rotation,
        shade: Boolean,
    }));
}

export type ModelElement = Static<typeof ModelElement>;
export const ModelElement = ModelElementOf(Faces);

export function ModelOf<A extends Runtype>(ModelElement: A) {
    return Partial({
        parent: String,
        ambientocclusion: Boolean,
        display: ModelTransformation,
        textures: TextureMap,
        elements: Array(ModelElement),
    });
}

export type Model = Static<typeof Model>;
export const Model = ModelOf(ModelElement);
