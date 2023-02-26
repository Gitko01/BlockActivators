package net.gitko.blockactivators.block.custom;

import com.mojang.authlib.GameProfile;
import dev.cafeteria.fakeplayerapi.server.FakeServerPlayer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.gitko.blockactivators.block.ModBlocks;
import net.gitko.blockactivators.gui.BlockActivatorScreenHandler;
import net.gitko.blockactivators.util.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import static net.gitko.blockactivators.BlockActivators.createFakePlayerBuilder;

public class BlockActivatorBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory {
    public BlockActivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLOCK_ACTIVATOR_BLOCK_ENTITY, pos, state);
    }

    private int tickCount = 0;
    private int destroyTickCount = 0;
    private int tickInterval = 10;
    private final static int ENERGY_DECRESE_PER_TICK_INTERVAL = 25;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(9, ItemStack.EMPTY);

    private int lastSelectedItem = -1;

    private boolean roundRobin = false;

    // Create energy storage for block activator
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(100000, 2500, 0) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    // Sync energy amount to the screen
    private final PropertyDelegate energyAmountPropertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return (int) energyStorage.getAmount();
            } else {
                return (int) (ENERGY_DECRESE_PER_TICK_INTERVAL / tickInterval);
            }
        }

        @Override
        public void set(int index, int value) {
            energyStorage.amount = value;
        }

        @Override
        public int size() {
            return 2;
        }
    };

    private int mode = 0;

    private Integer id = 0;

    private static final DefaultedList<BlockState> blocksBeingBroken = DefaultedList.ofSize(0);
    // Int in the hashtable below is the ID of the block entity
    private static final DefaultedList<Hashtable<Integer, Double>> blocksBeingBrokenProgresses = DefaultedList.ofSize(0);
    private static final DefaultedList<BlockPos> blocksBeingBrokenPositions = DefaultedList.ofSize(0);
    private static final Hashtable<ItemStack, BlockPos> itemsBeingUsedToBreakBlocks = new Hashtable<>();
    private static final Hashtable<ItemStack, BlockPos> itemsBeingUsedToBreakBlocksBlockEntity = new Hashtable<>();

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // We provide *this* to the screenHandler as our class Implements Inventory
        // Only the Server has the Inventory at the start, this will be synced to the client in the ScreenHandler
        return new BlockActivatorScreenHandler(syncId, playerInventory, this, this.energyAmountPropertyDelegate);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        // used in BlockActivatorScreenHandler
        packetByteBuf.writeBlockPos(pos);
        packetByteBuf.writeInt(mode);
        packetByteBuf.writeBoolean(roundRobin);
    }

    private FakeServerPlayer fakeServerPlayer = null;

    public static void tick(World world, BlockPos pos, BlockState state, BlockActivatorBlockEntity be) {
        // MAJOR MEMORY LEAK --------------------------------------------------DONE
        // HAVE TO DAMAGE ALL TOOLS BEING USED---------------------------------DONE
        // ensure two tools with same stats != each other----------------------DONE
        // ADD IN A CHECK TO MAKE SURE BLOCK BEING BROKEN IS SAME POS----------DONE
        // GUI-----------------------------------------------------------------DONE
        // ENTITY CLICKING ----------------------------------------------------DONE
        // ENERGY CONSUMPTION--------------------------------------------------DONE
        // MAKE SURE ONLY ONE FASTEST TOOL, AND ALSO ENSURE ONLY ONE ACTIVATOR IS UPDATING BLOCK DAMAGE----------DONE
        // BETTER TOOLTIPS FOR BLOCK ACTIVATORS--------------------------------DONE
        // ROUND ROBIN---------------------------------------------------------DONE
        // BLOCK PLACE SETTING
        // SPLASH POTION / THROW MODE
        // ENCHANTS AFFECTING MINING

        if (!world.isClient()) {
            if (world.isReceivingRedstonePower(pos))
                return;

            // mode 0: right click
            // mode 1: left click
            // mode 2: right click entity
            // mode 3: left click entity

            be.setTickCount(be.getTickCount() + 1);

            if (be.fakeServerPlayer == null) {
                int randomInt = Random.create().nextInt();
                UUID randUUID = UUID.randomUUID();

                // Player was slain by a block activator
                be.fakeServerPlayer = createFakePlayerBuilder().create(
                        world.getServer(), (ServerWorld) world, new GameProfile(randUUID, "a block activator")
                );

                be.fakeServerPlayer.setId(randomInt);
                be.fakeServerPlayer.setUuid(UUID.randomUUID());
                be.id = randomInt;
            }

            Direction facing = BlockActivatorBlock.getFacing(state);

            BlockPos posToHit = pos.offset(facing);
            Vec3d posToHitVec3d = Vec3d.of(posToHit);

            be.fakeServerPlayer.setPosition(Vec3d.of(pos));

            // reset the left click
            if (be.mode == 1) {
                BlockState blockState = world.getBlockState(posToHit);
                Float blockHardness = blockState.getHardness(world, posToHit);

                if (!breakable(blockState, blockHardness)) {
                    be.setDestroyTickCount(0);
                    world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, -1);
                }
            }

            if (be.getTickCount() >= be.getTickInterval() && be.energyStorage.amount >= ENERGY_DECRESE_PER_TICK_INTERVAL) {
                be.setTickCount(0);

                be.energyStorage.amount -= ENERGY_DECRESE_PER_TICK_INTERVAL;
                be.markDirty();

                // Handle clicking with items
                // Could possibly add a system that filters through every item and tries to use it
                ItemStack itemToClickWith = ItemStack.EMPTY;
                DefaultedList<ItemStack> items = be.getItems();

                int lastNonAirItem = 0;
                boolean allAir = true;

                for (ItemStack itemStack : items) {
                    if (itemStack != ItemStack.EMPTY && itemStack != Items.AIR.getDefaultStack() && itemStack.getCount() > 0) {
                        if (be.roundRobin) {
                            lastNonAirItem = items.indexOf(itemStack);
                            allAir = false;
                        }
                    }
                }

                if (allAir) {
                    be.lastSelectedItem = -1;
                }

                int slot = -1;

                for (ItemStack itemStack : items) {
                    slot += 1;
                    if (itemStack != ItemStack.EMPTY && itemStack != Items.AIR.getDefaultStack() && itemStack.getCount() > 0) {
                        if (!be.roundRobin) {
                            itemToClickWith = itemStack;
                            be.lastSelectedItem = items.indexOf(itemStack);

                            break;
                        } else {
                            if (be.lastSelectedItem == lastNonAirItem) {
                                if (items.indexOf(itemStack) < be.lastSelectedItem) {
                                    itemToClickWith = itemStack;
                                    be.lastSelectedItem = items.indexOf(itemStack);
                                    break;
                                }
                            } else if (items.indexOf(itemStack) > be.lastSelectedItem) {
                                itemToClickWith = itemStack;
                                be.lastSelectedItem = items.indexOf(itemStack);
                                break;
                            }
                        }
                    }
                }

                // add the item to the fake player's inventory
                be.fakeServerPlayer.getInventory().main.set(0, itemToClickWith);
                be.fakeServerPlayer.getInventory().selectedSlot = 0;

                if (be.mode == 0) {
                    // right click

                    // Animation (slowly open)
                    if (world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) <= 1) {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), 4));
                    } else {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) - 1));
                    }

                    DamageSource dmgSource = DamageSource.player(be.fakeServerPlayer);
                    List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, Box.from(posToHitVec3d), e -> (
                            //e.getType() != EntityType.PLAYER &&
                            //e.getType() != EntityType.ARMOR_STAND &&
                            !e.isInvulnerableTo(dmgSource) &&
                                    !e.isDead()
                    ));

                    if (entities.isEmpty()) {
                        // not an entity, try clicking on a block
                        clickRight(be, posToHitVec3d, itemToClickWith, world, posToHit);
                    } else {
                        clickEntityRight(be, entities);
                    }


                } else if (be.mode == 1) {
                    // left click

                    // Animation (slowly close)
                    if (world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) >= 4) {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), 1));
                    } else {
                        world.setBlockState(pos, state.with(IntProperty.of("anim", 1, 4), world.getBlockState(pos).get(IntProperty.of("anim", 1, 4)) + 1));
                    }

                    DamageSource dmgSource = DamageSource.player(be.fakeServerPlayer);
                    List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, Box.from(posToHitVec3d), e -> (
                            !e.isInvulnerableTo(dmgSource) &&
                            !e.isDead()
                    ));

                    if (entities.isEmpty()) {
                        // not an entity, try clicking on a block
                        clickLeft(itemToClickWith, world, pos, posToHit, be);
                    } else {
                        clickEntityLeft(entities, be);
                    }
                }

                ItemStack fakeInventoryItem = be.fakeServerPlayer.getInventory().main.get(0);

                if (fakeInventoryItem == null)
                    return;

                if (fakeInventoryItem.getItem() == Items.AIR)
                    return;

                items.set(slot, fakeInventoryItem);
            }
        }
    }

    public static void clickLeft(ItemStack itemToClickWith, World world, BlockPos pos, BlockPos posToHit, BlockActivatorBlockEntity be) {
        // left-click on a block
        BlockState blockState = world.getBlockState(posToHit);
        Float blockHardness = blockState.getHardness(world, posToHit);

        if (!breakable(blockState, blockHardness)) {
            be.setDestroyTickCount(0);
            world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, -1);

            // remove old blocks that were removed by an external source
            try {
                for (BlockPos blocksBeingBrokenPos : blocksBeingBrokenPositions) {
                    if (posToHit.equals(blocksBeingBrokenPos)) {
                        blocksBeingBrokenProgresses.remove(blocksBeingBrokenPositions.indexOf(blocksBeingBrokenPos));
                        blocksBeingBroken.remove(blocksBeingBrokenPositions.indexOf(blocksBeingBrokenPos));
                        blocksBeingBrokenPositions.remove(blocksBeingBrokenPos);
                    }
                }
            } catch (Exception ignored) {}
        }

        Float breakSpeed = itemToClickWith.getMiningSpeedMultiplier(blockState);
        double destroyProgress = be.getDestroyTickCount() * 0.001 * (breakSpeed / blockHardness) * 150;
        double globalDestroyProgress = 0;

        be.setDestroyTickCount(be.getDestroyTickCount() + 1);

        // set item being used to break blocks
        // key = item
        // value = block pos

        // reset items to ensure the block activator doesn't try to use 2+ items at once
        try {
            for (ItemStack item : itemsBeingUsedToBreakBlocksBlockEntity.keySet()) {
                if (itemsBeingUsedToBreakBlocksBlockEntity.get(item).equals(pos)) {
                    itemsBeingUsedToBreakBlocksBlockEntity.remove(item);
                    itemsBeingUsedToBreakBlocks.remove(item);
                }
            }
        } catch (Exception ignored) {}

        // add item to click with into the array of items being used
        if (!itemsBeingUsedToBreakBlocks.containsKey(itemToClickWith)) {
            itemsBeingUsedToBreakBlocks.put(itemToClickWith, posToHit);
            itemsBeingUsedToBreakBlocksBlockEntity.put(itemToClickWith, pos);
        }

        // check to make sure block pos hasn't been updated
        if (blocksBeingBroken.contains(blockState)) {
            if (!blocksBeingBrokenPositions.get(blocksBeingBroken.indexOf(blockState)).equals(posToHit)) {
                blocksBeingBrokenProgresses.remove(blocksBeingBroken.indexOf(blockState));
                blocksBeingBrokenPositions.remove(blocksBeingBroken.indexOf(blockState));
                blocksBeingBroken.remove(blockState);
            }
        }

        if (!blocksBeingBroken.contains(blockState)) {
            blocksBeingBroken.add(blockState);
            blocksBeingBrokenProgresses.add(new Hashtable<>());
            blocksBeingBrokenPositions.add(posToHit);
        }

        // find destroy progress from other block activators
        if (blocksBeingBroken.contains(blockState)) {
            if (blocksBeingBrokenPositions.get(blocksBeingBroken.indexOf(blockState)).equals(posToHit)) {
                Hashtable<Integer, Double> progressList = blocksBeingBrokenProgresses.get(blocksBeingBroken.indexOf(blockState));

                if (!progressList.contains(be.id)) {
                    progressList.put(be.id, destroyProgress);
                } else {
                    progressList.remove(be.id);
                    progressList.put(be.id, destroyProgress);
                }

                // grab each destroy progress reported by each block activator, place it into one variable
                for (Double aDouble : blocksBeingBrokenProgresses.get(blocksBeingBroken.indexOf(blockState)).values()) {
                    globalDestroyProgress += aDouble;
                }
            }
        }

//        if (blocksBeingBroken.contains(blockState)) {
//            BlockActivators.LOGGER.info(blocksBeingBrokenProgresses.get(blocksBeingBroken.indexOf(blockState)).toString());
//            BlockActivators.LOGGER.info(String.valueOf(globalDestroyProgress));
//            BlockActivators.LOGGER.info("-----------\n");
//        }


        // Find the fastest tool under toolsBeingUsedToBreakBlocks, if that tool matches the held tool of
        // the current block entity, then use it.
        ItemStack bestItem = itemToClickWith;

        for (ItemStack item : itemsBeingUsedToBreakBlocks.keySet()) {
            if (itemsBeingUsedToBreakBlocks.get(item).equals(posToHit)) {
                if (item != ItemStack.EMPTY && item != Items.AIR.getDefaultStack() && item.getCount() > 0) {
                    if (item.getMiningSpeedMultiplier(blockState) > bestItem.getMiningSpeedMultiplier(blockState)) {
                        if (item.isDamageable() && bestItem.isDamageable()) {
                            float itemDurability = (float) ((item.getMaxDamage() - item.getDamage()) / item.getMaxDamage());
                            float bestItemDurability = (float) ((bestItem.getMaxDamage() - bestItem.getDamage()) / bestItem.getMaxDamage());

                            if (itemDurability > bestItemDurability) {
                                bestItem = item;
                            }
                        } else {
                            bestItem = item;
                        }
                    }
                }
            }
        }

        // Start of the ACTUAL block destruction
        if (globalDestroyProgress >= 10) {
            be.setDestroyTickCount(0);

            if (bestItem.equals(itemToClickWith)) {
                world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, -1);
                be.fakeServerPlayer.interactionManager.tryBreakBlock(posToHit);

                // do damage to others too
                for (ItemStack item : itemsBeingUsedToBreakBlocks.keySet()) {
                    if (itemsBeingUsedToBreakBlocks.get(item).equals(posToHit)) {
                        if (!item.equals(bestItem)) {
                            if (item.isDamageable()) {
                                item.damage(1, be.fakeServerPlayer, fakeServerPlayer1 -> {
                                });
                            }
                        }
                    }
                }
            }

            // remember to remove block from the block state list when removed!
            if (blocksBeingBroken.contains(blockState)) {
                if (blocksBeingBrokenPositions.get(blocksBeingBroken.indexOf(blockState)).equals(posToHit)) {
                    blocksBeingBrokenProgresses.remove(blocksBeingBroken.indexOf(blockState));
                    blocksBeingBrokenPositions.remove(blocksBeingBroken.indexOf(blockState));
                    blocksBeingBroken.remove(blockState);
                }
            }

            return;
        }

        if (bestItem.equals(itemToClickWith)) {
            world.setBlockBreakingInfo(be.fakeServerPlayer.getId(), posToHit, (int) globalDestroyProgress);
        }
    }

    public static void clickRight(BlockActivatorBlockEntity be, Vec3d posToHitVec3d, ItemStack itemToClickWith, World world, BlockPos posToHit) {
        // right-click on a block
        be.fakeServerPlayer.interactAt(be.fakeServerPlayer, posToHitVec3d, Hand.MAIN_HAND);
        be.fakeServerPlayer.interactionManager.interactBlock(
                be.fakeServerPlayer,
                world,
                itemToClickWith,
                Hand.MAIN_HAND,
                new BlockHitResult(posToHitVec3d, Direction.UP, posToHit, false)
        );
    }

    public static void clickEntityLeft(List<LivingEntity> entities, BlockActivatorBlockEntity be) {
        // left-click on an entity
        for (LivingEntity entity: entities) {
            be.fakeServerPlayer.attack(entity);
            break;
        }
    }

    public static void clickEntityRight(BlockActivatorBlockEntity be, List<LivingEntity> entities) {
        // right-click on an entity
        for (LivingEntity entity: entities) {
            be.fakeServerPlayer.interact(entity, Hand.MAIN_HAND);
            break;
        }
    }

    public static boolean breakable(BlockState blockState, Float blockHardness) {
        return !blockState.getMaterial().isLiquid() && blockState.getBlock() != Blocks.AIR && blockHardness != -1f;
    }

    public int getDestroyTickCount() {
        return destroyTickCount;
    }

    public void setDestroyTickCount(int destroyTickCount) {
        this.destroyTickCount = destroyTickCount;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    public int getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    public void setMode(int modeID) {
        this.mode = modeID;
    }

    public void setRoundRobin(boolean on) {
        this.roundRobin = on;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        this.mode = nbt.getInt("mode");
        this.roundRobin = nbt.getBoolean("roundRobin");
        this.energyStorage.amount = nbt.getLong("energyAmount");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
        nbt.putInt("mode", this.mode);
        nbt.putBoolean("roundRobin", this.roundRobin);
        nbt.putLong("energyAmount", this.energyStorage.amount);
        super.writeNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public void sync() {
        assert world != null;
        if (!world.isClient()) {
            world.markDirty(getPos());
        }
    }

    public void registerEnergyStorage() {
        EnergyStorage.SIDED.registerForBlockEntity((myBlockEntity, direction) -> myBlockEntity.energyStorage, ModBlocks.BLOCK_ACTIVATOR_BLOCK_ENTITY);
    }
}
