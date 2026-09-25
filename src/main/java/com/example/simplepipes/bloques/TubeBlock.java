package com.example.simplepipes.bloques;

import com.example.simplepipes.entidades.TubeBlockEntity;
import com.example.simplepipes.registro.BloquesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * Bloque de tubería común que conecta visualmente con otras tuberías o inventarios.
 * Implementa `EntityBlock` para el ticking de transporte e items en tránsito.
 */
public class TubeBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public TubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    // ─── VoxelShape (colisión, sombras, hitbox) ───────────────────────────
    // Centro: 4×4×4 (píxels 6..10)
    private static final VoxelShape SHAPE_CENTER = box(6, 6, 6, 10, 10, 10);
    // Brazos: 4×4×6 (se extienden desde el centro hasta el borde de la cara)
    private static final VoxelShape SHAPE_NORTH = box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SHAPE_SOUTH = box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape SHAPE_EAST  = box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape SHAPE_WEST  = box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape SHAPE_UP    = box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_DOWN  = box(6, 0, 6, 10, 6, 10);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = SHAPE_CENTER;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, SHAPE_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SHAPE_SOUTH);
        if (state.getValue(EAST))  shape = Shapes.or(shape, SHAPE_EAST);
        if (state.getValue(WEST))  shape = Shapes.or(shape, SHAPE_WEST);
        if (state.getValue(UP))    shape = Shapes.or(shape, SHAPE_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, SHAPE_DOWN);
        return shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return makeConnections(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        return makeConnections(level, currentPos);
    }

    private BlockState makeConnections(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return defaultBlockState()
                .setValue(NORTH, canConnectTo(level, pos.north(), Direction.NORTH))
                .setValue(SOUTH, canConnectTo(level, pos.south(), Direction.SOUTH))
                .setValue(EAST, canConnectTo(level, pos.east(), Direction.EAST))
                .setValue(WEST, canConnectTo(level, pos.west(), Direction.WEST))
                .setValue(UP, canConnectTo(level, pos.above(), Direction.UP))
                .setValue(DOWN, canConnectTo(level, pos.below(), Direction.DOWN));
    }

    private boolean canConnectTo(LevelReader level, BlockPos neighborPos, Direction direction) {
        return TubeConnectionHelper.canConnectToAny(level, neighborPos, direction);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof TubeBlockEntity tubeBE) {
                TubeBlockEntity.tick(lvl, pos, st, tubeBE);
            }
        };
    }
}
