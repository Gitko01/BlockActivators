package net.gitko.blockactivators.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.gitko.blockactivators.BlockActivators;
import net.gitko.blockactivators.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup TAB = FabricItemGroup.builder(new Identifier(BlockActivators.MOD_ID, "tab"))
            .displayName(Text.literal("Block Activators"))
            .icon(() -> new ItemStack(ModBlocks.BLOCK_ACTIVATOR.asItem()))
            .build();
}
