package com.example.simplepipes.bloques;

import com.example.simplepipes.entidades.ExtractionTubeBlockEntity;
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
import org.jetbrains.annotations.Nullable;



/**
 * Bloque de tubería de extracción.
 * Extrae items de inventarios adyacentes de forma periódica.
 */
public class ExtractionTubeBlock extends Block implements EntityBlock {

    /** Un cofre solo puede tener 1 extractor adyacente a la vez. */
    public static final int MAX_EXTRACTORS_PER_CHEST = 1;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public ExtractionTubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    // ─── VoxelShape ───────────────────────────────────────────────────────────────
    private static final VoxelShape SHAPE_CENTER = box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape SHAPE_NORTH = box(6, 6, 0, 10, 10, 5);
    private static final VoxelShape SHAPE_SOUTH = box(6, 6, 11, 10, 10, 16);
    private static final VoxelShape SHAPE_EAST  = box(11, 6, 6, 16, 10, 10);
    private static final VoxelShape SHAPE_WEST  = box(0, 6, 6, 5, 10, 10);
    private static final VoxelShape SHAPE_UP    = box(6, 11, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_DOWN  = box(6, 0, 6, 10, 5, 10);

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

    /**
     * Impide la colocación si algún cofre adyacente ya tiene MAX_EXTRACTORS_PER_CHEST extractores.
     *
     * Regla: un cofre/inventario puede tener como máximo MAX_EXTRACTORS_PER_CHEST extractores
     * pegados a él al mismo tiempo. Si ya los tiene, el bloque no puede colocarse ni sobrevivir.
     *
     * canSurvive es el método correcto en la API de Minecraft — se llama tanto al colocar
     * como al actualizar vecinos, lo que evita estados inválidos.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!(level instanceof Level realLevel)) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            // Comprobar si el vecino es un inventario (cofre, hopper, etc.)
            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    realLevel, neighborPos, dir.getOpposite()
            );
            if (storage == null) continue;

            // Contar cuántos extractores ya están pegados a ese cofre
            // (sin contar el extractor en 'pos' mismo, por si ya está colocado)
            int extractorCount = countExtractorsAdjacentTo(level, neighborPos, pos);
            if (extractorCount >= MAX_EXTRACTORS_PER_CHEST) {
                return false; // Este cofre ya tiene el máximo de extractores permitidos
            }
        }
        return true;
    }

    /**
     * Cuenta cuántos ExtractionTubeBlock hay adyacentes a chestPos,
     * excluyendo excludePos (la posición del extractor que estamos evaluando).
     */
    private static int countExtractorsAdjacentTo(LevelReader level, BlockPos chestPos, BlockPos excludePos) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = chestPos.relative(dir);
            if (neighborPos.equals(excludePos)) continue; // no contar a sí mismo
            if (level.getBlockState(neighborPos).getBlock() instanceof ExtractionTubeBlock) {
                count++;
            }
        }
        return count;
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
        BlockState state = level.getBlockState(neighborPos);
        if (state.getBlock() instanceof TubeBlock || state.getBlock() instanceof FilterTubeBlock || state.getBlock() instanceof ExtractionTubeBlock) {
            return true;
        }

        if (level instanceof Level realLevel) {
            Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    realLevel,
                    neighborPos,
                    direction.getOpposite()
            );
            return storage != null;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtractionTubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof ExtractionTubeBlockEntity extractorBE) {
                ExtractionTubeBlockEntity.tick(lvl, pos, st, extractorBE);
            }
        };
    }
}
