package net.gitko.blockactivators;

import com.mojang.authlib.GameProfile;
import dev.cafeteria.fakeplayerapi.server.FakePlayerBuilder;
import dev.cafeteria.fakeplayerapi.server.FakeServerPlayer;
import dev.cafeteria.fakeplayerapi.server.FakeServerPlayerFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.gitko.blockactivators.block.ModBlocks;
import net.gitko.blockactivators.gui.BlockActivatorScreenHandler;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockActivators implements ModInitializer {
	public static final String MOD_ID = "blockactivators";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Block Activator screen
	public static final ScreenHandlerType<BlockActivatorScreenHandler> BLOCK_ACTIVATOR_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(BlockActivatorScreenHandler::new);

	@Override
	public void onInitialize() {
		LOGGER.info("[Block Activators] Mod initialized.");

		ModBlocks.registerModBlocks();
		Registry.register(Registries.SCREEN_HANDLER, new Identifier("block_activator_screen_handler"), BLOCK_ACTIVATOR_SCREEN_HANDLER);
	}

	public static FakePlayerBuilder createFakePlayerBuilder() {
		return new FakePlayerBuilder(new Identifier(BlockActivators.MOD_ID, "fake_player"), (new FakeServerPlayerFactory() {
			@Override
			public FakeServerPlayer create(FakePlayerBuilder builder, MinecraftServer server, ServerWorld world, GameProfile profile) {
				return new FakeServerPlayer(builder, server, world, profile) {
					@Override
					public boolean isCreative() {
						return false;
					}

					@Override
					public boolean isSpectator() {
						return false;
					}

					@Override
					public void attack(Entity target) {
						// IMPORTANT: GENERIC_ATTACK_DAMAGE NOT being set automatically by game, so using mixin for LivingEntity to allow me
						// to run sendEquipmentChanges() to update the GENERIC_ATTACK_DAMAGE

						// last attack ticks is set to 1000 just to ensure MC thinks it has been a long time since the block activator has last attacked
						this.lastAttackedTicks = 1000;

						((EntityEquipmentChanges) this).gitko_sendEquipmentChanges();
						// if you open this file up, the line above gives an error about this not being castable to EntityEquipmentChanges.
						// the only reason it is attempting to cast to it is to allow me to run the mod :D
						// otherwise, it gives me a no method found error which keeps me from running the mod.
						// the no method found error doesn't matter when the mod is in production bc the method is added
						// by a mixin which IntelliJ Idea has no clue about

						super.attack(target);
					}
				};
			}
		}));
	}
}
