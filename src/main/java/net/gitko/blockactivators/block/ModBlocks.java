package net.gitko.blockactivators.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.gitko.blockactivators.BlockActivators;
import net.gitko.blockactivators.block.custom.BlockActivatorBlock;
import net.gitko.blockactivators.block.custom.BlockActivatorBlockEntity;
import net.gitko.blockactivators.item.ModItemGroup;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModBlocks {
    // Blocks
    public static final Block BLOCK_ACTIVATOR = registerBlock("block_activator",
            new BlockActivatorBlock(FabricBlockSettings.of(
                            Material.METAL)
                    .strength(5f, 6f)
                    .requiresTool()
            ), ModItemGroup.TAB, "tooltip.blockactivators.block_activator", 3, true);

    public static final BlockEntityType<BlockActivatorBlockEntity> BLOCK_ACTIVATOR_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(BlockActivators.MOD_ID, "block_activator_block_entity"),
            FabricBlockEntityTypeBuilder.create(BlockActivatorBlockEntity::new, BLOCK_ACTIVATOR).build()
    );

    // Registry stuff
    private static Block registerBlock(String name, Block block, ItemGroup group, String tooltipKey, Integer tooltipLineAmount, Boolean holdDownShift) {
        registerBlockItem(name, block, group, tooltipKey, tooltipLineAmount, holdDownShift);
        return Registry.register(Registries.BLOCK, new Identifier(BlockActivators.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group, String tooltipKey, Integer tooltipLineAmount, Boolean holdDownShift) {
        BlockItem blockItem = new BlockItem(block, new FabricItemSettings()) {
            @Override
            public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                if (holdDownShift) {
                    if (Screen.hasShiftDown()) {
                        Integer currentLine = 1;

                        while (tooltipLineAmount >= currentLine) {
                            tooltip.add(Text.translatable(tooltipKey + "_" + currentLine.toString()));
                            currentLine += 1;
                        }
                    } else {
                        tooltip.add(Text.translatable("tooltip.blockactivators.hold_shift"));
                    }
                } else {
                    Integer currentLine = 1;

                    while (tooltipLineAmount >= currentLine) {
                        tooltip.add(Text.translatable(tooltipKey + "_" + currentLine.toString()));
                        currentLine += 1;
                    }
                }
            }
        };
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(blockItem));

        return Registry.register(Registries.ITEM, new Identifier(BlockActivators.MOD_ID, name), blockItem);
    }

    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registries.BLOCK, new Identifier(BlockActivators.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        BlockItem blockItem = new BlockItem(block, new FabricItemSettings());
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(blockItem));

        return Registry.register(Registries.ITEM, new Identifier(BlockActivators.MOD_ID, name), blockItem);
    }

    public static void registerModBlocks() {
        BlockActivators.LOGGER.info("Registering ModBlocks for " + BlockActivators.MOD_ID);
    }
}
