package net.gitko.blockactivators.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
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
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModBlocks {
    // Blocks
    public static final Block BLOCK_ACTIVATOR = registerBlock("block_activator",
            new BlockActivatorBlock(FabricBlockSettings.of(
                            Material.METAL)
                    .strength(6f, 6f)
                    .requiresTool()
            ), ModItemGroup.TAB, "tooltip.blockactivators.block_activator", 3, true);

    public static final BlockEntityType<BlockActivatorBlockEntity> BLOCK_ACTIVATOR_BLOCK_ENTITY = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(BlockActivators.MOD_ID, "block_activator_block_entity"),
            FabricBlockEntityTypeBuilder.create(BlockActivatorBlockEntity::new, BLOCK_ACTIVATOR).build()
    );

    // Registry stuff
    private static Block registerBlock(String name, Block block, ItemGroup group, String tooltipKey, Integer tooltipLineAmount, Boolean holdDownShift) {
        registerBlockItem(name, block, group, tooltipKey, tooltipLineAmount, holdDownShift);
        return Registry.register(Registry.BLOCK, new Identifier(BlockActivators.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group, String tooltipKey, Integer tooltipLineAmount, Boolean holdDownShift) {
        return Registry.register(Registry.ITEM, new Identifier(BlockActivators.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(group)) {
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
                });
    }

    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new Identifier(BlockActivators.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        return Registry.register(Registry.ITEM, new Identifier(BlockActivators.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(group)));
    }

    public static void registerModBlocks() {
        BlockActivators.LOGGER.info("Registering ModBlocks for " + BlockActivators.MOD_ID);
    }
}
