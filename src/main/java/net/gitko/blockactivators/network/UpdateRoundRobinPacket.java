package net.gitko.blockactivators.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gitko.blockactivators.BlockActivators;
import net.gitko.blockactivators.block.custom.BlockActivatorBlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UpdateRoundRobinPacket {
    private static final Identifier UPDATE_ROUND_ROBIN_PACKET_ID = new Identifier(BlockActivators.MOD_ID, "update_round_robin_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_ROUND_ROBIN_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            boolean roundRobin = buf.readBoolean();
            BlockPos pos = buf.readBlockPos();
            World world = player.getWorld();

            server.execute(() -> {
                if (world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16)) {
                    BlockActivatorBlockEntity be = (BlockActivatorBlockEntity) world.getBlockEntity(pos);
                    assert be != null;

                    be.setRoundRobin(roundRobin);
                    be.markDirty();
                    be.sync();
                }
            });
        });
    }

}
