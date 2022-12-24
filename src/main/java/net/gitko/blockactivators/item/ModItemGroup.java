package net.gitko.blockactivators.item;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.gitko.blockactivators.BlockActivators;
import net.gitko.blockactivators.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup TAB = FabricItemGroupBuilder.build(new Identifier(BlockActivators.MOD_ID, "tab"),
            () -> new ItemStack(ModBlocks.BLOCK_ACTIVATOR)
    );
}
