package com.example.simplepipes.render;

import com.example.simplepipes.entidades.TravelingItem;
import com.example.simplepipes.entidades.TubeBlockEntity;
import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderizador de bloque cliente para dibujar los items en tránsito 3D moviéndose
 * por las tuberías (funciona tanto para TubeBlockEntity como para FilterTubeBlockEntity).
 */
public class TubeBlockEntityRenderer<T extends TubeBlockEntity> implements BlockEntityRenderer<T, TubeBlockEntityRenderer.TubeRenderState> {

    private final ItemModelResolver itemModelResolver;

    public TubeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public TubeRenderState createRenderState() {
        return new TubeRenderState();
    }

    @Override
    public void extractRenderState(T genericBe, TubeRenderState state, float partialTick, net.minecraft.world.phys.Vec3 offset, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        if (genericBe == null) return;
        BlockEntityRenderState.extractBase(genericBe, state, crumblingOverlay);

        if (genericBe instanceof TubeBlockEntity be) {
            state.isFilterTube = be instanceof FilterTubeBlockEntity;
            state.filterItems.clear();
            if (be instanceof FilterTubeBlockEntity filterBE) {
                for (ItemStack filter : filterBE.getFilterItems()) {
                    state.filterItems.add(filter.copy());
                }
            }

            state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
            state.partialTick = partialTick;
            state.level = be.getLevel();

            state.items.clear();
            for (TravelingItem item : be.getTravelingItems()) {
                if (item.stack.isEmpty()) continue;

                TubeRenderState.ItemInfo info = new TubeRenderState.ItemInfo();
                info.stack = item.stack.copy();
                info.progress = item.progress;

                BlockPos currentPos = item.path.get(item.currentPathIndex);
                BlockPos nextPos;
                if (item.currentPathIndex < item.path.size() - 1) {
                    nextPos = item.path.get(item.currentPathIndex + 1);
                } else {
                    nextPos = item.destinationPos;
                }

                info.x = currentPos.getX() + (nextPos.getX() - currentPos.getX()) * item.progress - be.getBlockPos().getX() + 0.5;
                info.y = currentPos.getY() + (nextPos.getY() - currentPos.getY()) * item.progress - be.getBlockPos().getY() + 0.5;
                info.z = currentPos.getZ() + (nextPos.getZ() - currentPos.getZ()) * item.progress - be.getBlockPos().getZ() + 0.5;

                state.items.add(info);
            }
        }
    }

    @Override
    public void submit(TubeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        ItemStackRenderState itemRenderState = new ItemStackRenderState();

        // 1. Renderizar previsualización de la lista de filtros si es un Tubo de Filtro
        if (state.isFilterTube && !state.filterItems.isEmpty()) {
            int index = (int) ((state.gameTime / 20) % state.filterItems.size());
            ItemStack filterItem = state.filterItems.get(index);

            if (!filterItem.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.scale(0.35F, 0.35F, 0.35F);
                poseStack.mulPose(Axis.YP.rotationDegrees((state.gameTime + state.partialTick) * 2));

                itemModelResolver.updateForTopItem(
                        itemRenderState,
                        filterItem,
                        ItemDisplayContext.GROUND,
                        state.level,
                        null,
                        0
                );
                itemRenderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }

        // 2. Renderizar items en tránsito
        for (TubeRenderState.ItemInfo info : state.items) {
            poseStack.pushPose();
            poseStack.translate(info.x, info.y, info.z);
            poseStack.scale(0.3F, 0.3F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees((state.gameTime + state.partialTick) * 4));

            itemModelResolver.updateForTopItem(
                    itemRenderState,
                    info.stack,
                    ItemDisplayContext.GROUND,
                    state.level,
                    null,
                    0
            );
            itemRenderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public static class TubeRenderState extends BlockEntityRenderState {
        public boolean isFilterTube;
        public final List<ItemStack> filterItems = new ArrayList<>();
        public long gameTime;
        public float partialTick;
        public net.minecraft.world.level.Level level;
        public final List<ItemInfo> items = new ArrayList<>();

        public static class ItemInfo {
            public ItemStack stack;
            public double x;
            public double y;
            public double z;
            public float progress;
        }
    }
}