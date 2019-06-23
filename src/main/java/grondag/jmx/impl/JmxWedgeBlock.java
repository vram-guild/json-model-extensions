//package grondag.jmx.impl;
// TODO: WIP
//import grondag.brocade.connect.api.model.BlockEdge;
//import grondag.fermion.world.WorldHelper;
//import net.minecraft.block.Block;
//import net.minecraft.block.BlockPlacementEnvironment;
//import net.minecraft.block.BlockState;
//import net.minecraft.block.Waterloggable;
//import net.minecraft.entity.EntityContext;
//import net.minecraft.fluid.FluidState;
//import net.minecraft.fluid.Fluids;
//import net.minecraft.item.ItemPlacementContext;
//import net.minecraft.state.StateFactory;
//import net.minecraft.state.property.BooleanProperty;
//import net.minecraft.state.property.EnumProperty;
//import net.minecraft.state.property.Properties;
//import net.minecraft.util.BlockMirror;
//import net.minecraft.util.BlockRotation;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.util.shape.VoxelShape;
//import net.minecraft.util.shape.VoxelShapes;
//import net.minecraft.world.BlockView;
//
//public class JmxWedgeBlock extends Block implements Waterloggable {
//    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
//    public static final EnumProperty<BlockEdge> EDGE = EnumProperty.of("edge", BlockEdge.class);
//
//    private static final VoxelShape[] SHAPES = new VoxelShape[BlockEdge.values().length];
//
//    static {
//        for(BlockEdge edge : BlockEdge.values()) {
//            double xMin = 0;
//            double xMax = 0.25;
//            double yMin = 0;
//            double yMax = 0.25;
//            double zMin = 0;
//            double zMax = 0.25;
//            
//            final double xMinInc = 0.25;
//            final double xMaxInc = 0.25;
//            final double yMinInc = 0.25;
//            final double yMaxInc = 0.25;
//            final double zMinInc = 0.25;
//            final double zMaxInc = 0.25;
//            
//            VoxelShape shape = null;
//            for(int i = 0; i < 4; i++) {
//                final VoxelShape part = VoxelShapes.cuboid(xMin, yMin, zMin, xMax, yMax, zMax);
//                shape = i == 0 ? part : VoxelShapes.union(shape, part);
//                
//                xMin += xMinInc;
//                xMax += xMaxInc;
//                yMin += yMinInc;
//                yMax += yMaxInc;
//                zMin += zMinInc;
//                zMax += zMaxInc;
//            }
//            
//            SHAPES[edge.ordinal()] = shape;
//        }
//    }
//
//    public JmxWedgeBlock(Settings blockSettings) {
//        super(blockSettings);
//    }
//
//    @Override
//    public boolean hasSidedTransparency(BlockState blockState) {
//        return true;
//    }
//
//    @Override
//    public VoxelShape getOutlineShape(BlockState blockState, BlockView blockView, BlockPos blockPos, EntityContext entityContext) {
//        return SHAPES[blockState.get(EDGE).ordinal()];
//    }
//
//    @Override
//    public BlockState getPlacementState(ItemPlacementContext context) {
//        final Direction side = context.getSide();
//        final Vec3d hitPos = context.getHitPos();
//        final BlockPos pos = context.getBlockPos();
//        final Direction nearest = WorldHelper.closestAdjacentFace(side, (float)(hitPos.x - pos.getX()), (float)(hitPos.y - pos.getY()), (float)(hitPos.z - pos.getZ()));
//        final FluidState fluidState = context.getWorld().getFluidState(pos);
//        return getDefaultState().with(EDGE, BlockEdge.find(side, nearest)).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
//    }
//
//    /**
//     * Method doesn't afford us a way to know the player's intent/orientation,
//     * so we always rotate around the Y-axis.
//     */
//    @Override
//    public BlockState rotate(BlockState blockState, BlockRotation rotation) {
//        BlockEdge edge = blockState.get(EDGE);
//
//        //TODO: move this to Brocade-Connect library
//        switch(edge.face1) {
//        case DOWN:
//        case UP:
//            edge = BlockEdge.find(edge.face1, rotation.rotate(edge.face2));
//            break;
//
//        default:
//            edge = BlockEdge.find(rotation.rotate(edge.face1), rotation.rotate(edge.face2));
//            break;
//        }
//        return (BlockState)blockState.with(EDGE, edge);
//    }
//
//    @Override
//    public BlockState mirror(BlockState blockState, BlockMirror blockMirror) {
//        BlockEdge edge = blockState.get(EDGE);
//        switch(blockMirror) {
//        case LEFT_RIGHT: //Z
//            if (edge.parallelAxis == Direction.Axis.Y) {
//                return blockState.with(EDGE, BlockEdge.find(edge.face1.getOpposite(), edge.face2));
//            } else if(edge.parallelAxis == Direction.Axis.X) {
//                return blockState.with(EDGE, BlockEdge.find(edge.face1, edge.face2.getOpposite()));
//            }
//            break;
//        case FRONT_BACK:  //X
//            if (edge.parallelAxis == Direction.Axis.Y || edge.parallelAxis == Direction.Axis.Z) {
//                return blockState.with(EDGE, BlockEdge.find(edge.face1, edge.face2.getOpposite()));
//            }
//            break;
//
//        default:
//        }
//        return blockState;
//    }
//
//    @Override
//    protected void appendProperties(StateFactory.Builder<Block, BlockState> builder) {
//        builder.add(EDGE, WATERLOGGED);
//    }
//
//    @Override
//    public FluidState getFluidState(BlockState blockState) {
//        return (Boolean)blockState.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(blockState);
//    }
//
//    @Override
//    public boolean canPlaceAtSide(BlockState blockState, BlockView blockView, BlockPos blockPos, BlockPlacementEnvironment blockPlacementEnvironment) {
//        return false;
//    }
//}
