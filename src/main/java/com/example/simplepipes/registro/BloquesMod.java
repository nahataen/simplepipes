package com.example.simplepipes.registro;

import com.example.simplepipes.SimplePipesMod;
import com.example.simplepipes.bloques.ExtractionTubeBlock;
import com.example.simplepipes.bloques.FilterTubeBlock;
import com.example.simplepipes.bloques.FilterTubeBlockItem;
import com.example.simplepipes.bloques.TubeBlock;
import com.example.simplepipes.entidades.ExtractionTubeBlockEntity;
import com.example.simplepipes.entidades.FilterTubeBlockEntity;
import com.example.simplepipes.entidades.TubeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registra bloques, items y block entities del mod SimplePipes.
 */
public class BloquesMod {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SimplePipesMod.MOD_ID, path);
    }

    // BLOQUES
    public static final Block TUBE = Registry.register(
            BuiltInRegistries.BLOCK, id("tube"),
            new TubeBlock(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().requiresCorrectToolForDrops()
                    .setId(ResourceKey.create(Registries.BLOCK, id("tube"))))
    );

    public static final Block FILTER_TUBE = Registry.register(
            BuiltInRegistries.BLOCK, id("filter_tube"),
            new FilterTubeBlock(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().requiresCorrectToolForDrops()
                    .setId(ResourceKey.create(Registries.BLOCK, id("filter_tube"))))
    );

    public static final Block EXTRACTION_TUBE = Registry.register(
            BuiltInRegistries.BLOCK, id("extraction_tube"),
            new ExtractionTubeBlock(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().requiresCorrectToolForDrops()
                    .setId(ResourceKey.create(Registries.BLOCK, id("extraction_tube"))))
    );

    public static final Block EXPLORER_CHEST = Registry.register(
            BuiltInRegistries.BLOCK, id("explorer_chest"),
            new com.example.simplepipes.bloques.ExplorerChestBlock(BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops()
                    .setId(ResourceKey.create(Registries.BLOCK, id("explorer_chest"))))
    );

    // ITEMS
    public static final Item TUBE_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id("tube"),
            new BlockItem(TUBE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("tube"))))
    );

    public static final Item FILTER_TUBE_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id("filter_tube"),
            new FilterTubeBlockItem(FILTER_TUBE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("filter_tube"))))
    );

    public static final Item EXTRACTION_TUBE_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id("extraction_tube"),
            new BlockItem(EXTRACTION_TUBE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("extraction_tube"))))
    );

    public static final Item EXPLORER_CHEST_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id("explorer_chest"),
            new BlockItem(EXPLORER_CHEST, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("explorer_chest"))))
    );

    // BLOCK ENTITIES
    public static final BlockEntityType<TubeBlockEntity> TUBE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("tube"),
            FabricBlockEntityTypeBuilder.create(TubeBlockEntity::new, TUBE).build()
    );

    public static final BlockEntityType<FilterTubeBlockEntity> FILTER_TUBE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("filter_tube"),
            FabricBlockEntityTypeBuilder.create(FilterTubeBlockEntity::new, FILTER_TUBE).build()
    );

    public static final BlockEntityType<ExtractionTubeBlockEntity> EXTRACTION_TUBE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("extraction_tube"),
            FabricBlockEntityTypeBuilder.create(ExtractionTubeBlockEntity::new, EXTRACTION_TUBE).build()
    );

    public static final BlockEntityType<com.example.simplepipes.entidades.ExplorerChestBlockEntity> EXPLORER_CHEST_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("explorer_chest"),
            FabricBlockEntityTypeBuilder.create(com.example.simplepipes.entidades.ExplorerChestBlockEntity::new, EXPLORER_CHEST).build()
    );

    // MENU / SCREEN HANDLER
    public static final net.minecraft.world.inventory.MenuType<com.example.simplepipes.gui.ExplorerChestMenu> EXPLORER_CHEST_MENU = Registry.register(
            BuiltInRegistries.MENU, id("explorer_chest"),
            new net.minecraft.world.inventory.MenuType<>(com.example.simplepipes.gui.ExplorerChestMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
    );

    public static void init() {
        // Carga estática de la clase y sus registros
    }
}
