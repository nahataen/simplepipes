package com.example.simplepipes.bloques;

import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import com.example.simplepipes.entidades.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import java.util.List;


/**
 * Bloque de tubería con filtro de destino.
 * Detecta cofres adyacentes y apunta hacia uno exclusivo (máx. 1 filtro por cofre).
 */
public class FilterTubeBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public FilterTubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
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
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
        return makeConnections(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return makeConnections(level, currentPos, state);
    }

    private BlockState makeConnections(LevelReader level, BlockPos pos, BlockState currentState) {
        Direction facing = currentState.getValue(FACING);

        // REGLA DE LEALTAD: Si este filtro ya tiene un FACING hacia un cofre real, mantenerlo
        if (isFacingValidChest(level, pos, facing)) {
            return currentState
                    .setValue(NORTH, canConnectTo(level, pos.north(), Direction.NORTH, facing))
                    .setValue(SOUTH, canConnectTo(level, pos.south(), Direction.SOUTH, facing))
                    .setValue(EAST, canConnectTo(level, pos.east(), Direction.EAST, facing))
                    .setValue(WEST, canConnectTo(level, pos.west(), Direction.WEST, facing))
                    .setValue(UP, canConnectTo(level, pos.above(), Direction.UP, facing))
                    .setValue(DOWN, canConnectTo(level, pos.below(), Direction.DOWN, facing));
        }

        // Filtro huérfano: buscar un cofre adyacente LIBRE
        for (Direction d : Direction.values()) {
            BlockPos neighborPos = pos.relative(d);
            if (!isChestAt(level, neighborPos, d)) continue;
            if (!isChestClaimedByAnotherFilter(level, pos, neighborPos)) {
                facing = d;
                break;
            }
        }

        BlockState updatedState = currentState.setValue(FACING, facing);

        return updatedState
                .setValue(NORTH, canConnectTo(level, pos.north(), Direction.NORTH, facing))
                .setValue(SOUTH, canConnectTo(level, pos.south(), Direction.SOUTH, facing))
                .setValue(EAST, canConnectTo(level, pos.east(), Direction.EAST, facing))
                .setValue(WEST, canConnectTo(level, pos.west(), Direction.WEST, facing))
                .setValue(UP, canConnectTo(level, pos.above(), Direction.UP, facing))
                .setValue(DOWN, canConnectTo(level, pos.below(), Direction.DOWN, facing));
    }

    private boolean isFacingValidChest(LevelReader level, BlockPos pos, Direction facing) {
        return TubeConnectionHelper.isChest(level, pos.relative(facing), facing.getOpposite());
    }

    private boolean isChestAt(LevelReader level, BlockPos neighborPos, Direction fromDirection) {
        return TubeConnectionHelper.isChest(level, neighborPos, fromDirection.getOpposite());
    }

    private boolean isChestClaimedByAnotherFilter(LevelReader level, BlockPos selfPos, BlockPos chestPos) {
        for (Direction d : Direction.values()) {
            BlockPos adjacentFilterPos = chestPos.relative(d);
            if (adjacentFilterPos.equals(selfPos)) continue;

            BlockState adjacentState = level.getBlockState(adjacentFilterPos);
            if (adjacentState.getBlock() instanceof FilterTubeBlock otherFilter) {
                Direction otherFacing = adjacentState.getValue(FilterTubeBlock.FACING);
                if (otherFacing == d) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canConnectTo(LevelReader level, BlockPos neighborPos, Direction direction, Direction facing) {
        return TubeConnectionHelper.canConnectTo(level, neighborPos, direction, facing);
    }

    /**
     * Clic derecho CON ítem en la mano: añade/quito ese ítem del whitelist del filtro.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        runFilterAction(level, pos, state, player, heldItem);
        return InteractionResult.SUCCESS;
    }

    /**
     * Clic derecho con MANO VACÍA: limpia toda la whitelist del filtro.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return runFilterAction(level, pos, state, player, ItemStack.EMPTY);
    }

    private InteractionResult runFilterAction(Level level, BlockPos pos, BlockState state,
                                              Player player, ItemStack heldItem) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FilterTubeBlockEntity filterBE)) {
            return InteractionResult.PASS;
        }

        if (heldItem.isEmpty()) {
            filterBE.clearFilters();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Tubo Filtro] Lista de filtros limpiada."));
        } else {
            ItemStack filterCopy = heldItem.copy();
            filterCopy.setCount(1);
            boolean added = filterBE.toggleFilterItem(filterCopy);
            if (added) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Tubo Filtro] Añadido al filtro: §6" + filterCopy.getHoverName().getString()));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[Tubo Filtro] Removido del filtro: §6" + filterCopy.getHoverName().getString()));
            }

            StringBuilder sb = new StringBuilder("§7Filtros activos: ");
            List<ItemStack> currentFilters = filterBE.getFilterItems();
            if (currentFilters.isEmpty()) {
                sb.append("Ninguno (Rechaza todo)");
            } else {
                for (int i = 0; i < currentFilters.size(); i++) {
                    sb.append("§f").append(currentFilters.get(i).getHoverName().getString());
                    if (i < currentFilters.size() - 1) sb.append("§7, ");
                }
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(sb.toString()));
        }

        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterTubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof FilterTubeBlockEntity filterBE) {
                TubeBlockEntity.tick(lvl, pos, st, filterBE);
            }
        };
    }
}
