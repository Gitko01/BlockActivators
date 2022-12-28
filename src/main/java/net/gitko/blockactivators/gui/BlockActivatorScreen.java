package net.gitko.blockactivators.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.gitko.blockactivators.BlockActivators;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Handles the rendering side of the GUI (like making sure there is a button image where the ScreenHandler says a button is)
@Environment(EnvType.CLIENT)
public class BlockActivatorScreen extends HandledScreen<BlockActivatorScreenHandler> {
    // old texture
    //private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/dispenser.png");

    // new texture :D
    private static final Identifier TEXTURE = new Identifier(BlockActivators.MOD_ID, "textures/gui/container/block_activator_gui.png");

    BlockActivatorScreenHandler screenHandler;

    private CyclingButtonWidget<Modes> modeButton = null;
    private CyclingButtonWidget<RoundRobinModes> roundRobinButton = null;

    private BlockPos blockPos = null;
    private int mode = -1;
    private boolean roundRobin = false;

    public enum Modes {
        LEFT_CLICK(1, "leftClick"),
        RIGHT_CLICK(0, "rightClick");

        private static final BlockActivatorScreen.Modes[] BY_NAME = (BlockActivatorScreen.Modes[]) Arrays.stream(values()).sorted(Comparator.comparingInt(BlockActivatorScreen.Modes::getId)).toArray(Modes[]::new);
        private final int id;
        private final String name;

        private Modes(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public Text getTranslatableName() {
            return Text.translatable("guiButton.blockactivators.block_activator." + this.name);
        }

        public static BlockActivatorScreen.Modes byOrdinal(int ordinal) {
            return BY_NAME[ordinal % BY_NAME.length];
        }

        @Nullable
        public static BlockActivatorScreen.Modes byName(String name) {
            BlockActivatorScreen.Modes[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BlockActivatorScreen.Modes mode = var1[var3];
                if (mode.name.equals(name)) {
                    return mode;
                }
            }

            return null;
        }
    }

    public enum RoundRobinModes {
        ON(1, "on", true),
        OFF(0, "off", false);

        private static final BlockActivatorScreen.RoundRobinModes[] BY_NAME = (BlockActivatorScreen.RoundRobinModes[]) Arrays.stream(values()).sorted(Comparator.comparingInt(BlockActivatorScreen.RoundRobinModes::getId)).toArray(RoundRobinModes[]::new);
        private final int id;
        private final String name;
        private final boolean on;

        private RoundRobinModes(int id, String name, boolean on) {
            this.id = id;
            this.name = name;
            this.on = on;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public boolean isOn() {
            return this.on;
        }

        public Text getTranslatableName() {
            return Text.translatable("guiButton.blockactivators.block_activator." + this.name);
        }

        public static BlockActivatorScreen.RoundRobinModes byOrdinal(int ordinal) {
            return BY_NAME[ordinal % BY_NAME.length];
        }

        @Nullable
        public static BlockActivatorScreen.RoundRobinModes byName(String name) {
            BlockActivatorScreen.RoundRobinModes[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BlockActivatorScreen.RoundRobinModes mode = var1[var3];
                if (mode.name.equals(name)) {
                    return mode;
                }
            }

            return null;
        }

        @Nullable
        public static BlockActivatorScreen.RoundRobinModes byValue(boolean value) {
            BlockActivatorScreen.RoundRobinModes[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                BlockActivatorScreen.RoundRobinModes mode = var1[var3];
                if (mode.on == value) {
                    return mode;
                }
            }

            return null;
        }
    }

    public BlockActivatorScreen(BlockActivatorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        blockPos = getPos(handler).orElse(null);
        mode = getMode(handler);
        roundRobin = getRoundRobin(handler);
        screenHandler = handler;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);

        // progress bar
        int maxUnitFill = 100000;
        int pBLength = 50;
        int pBHeight = 5;

        int xMargin = 117;
        int yMargin = 74;

        int energy = getEnergyAmount(screenHandler);
        int drainRate = getDrainRate(screenHandler);

        int energyPercentage = Math.round(((float) energy / (float) maxUnitFill) * 100F);

        int amountToMoveUpBy1Pixel = Math.round((float) maxUnitFill / (float) pBLength);

        int fillLength = Math.round((float) energy / (float) amountToMoveUpBy1Pixel);

        if (fillLength >= pBLength) {
            fillLength = pBLength;
        }

        this.drawTexture(matrices, x + xMargin, y + yMargin, 1, 167, fillLength, pBHeight);

        // render a tooltip containing energy amount
        if (mouseX >= x + xMargin && mouseX <= (x + xMargin) + pBLength) {
            if (mouseY >= y + yMargin && mouseY <= (y + yMargin) + pBHeight) {
                DefaultedList<Text> tooltip = DefaultedList.ofSize(0);
                
                if (Screen.hasShiftDown()) {
                    tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", energy, maxUnitFill)));

                    if (energyPercentage <= 10) {
                        tooltip.add(Text.of("§4" + energyPercentage + "% Charged§r"));
                    } else if (energyPercentage <= 75) {
                        tooltip.add(Text.of("§e" + energyPercentage + "% Charged§r"));
                    } else {
                        tooltip.add(Text.of("§a" + energyPercentage + "% Charged§r"));
                    }

                    tooltip.add(Text.of("<----------------->"));

                    tooltip.add(Text.of("§6Max Energy: 100,000 E§r"));
                    tooltip.add(Text.of("§6Max Input Rate: 2,500 E§r"));
                    tooltip.add(Text.of("§6Drain Rate: -" + drainRate + " E/t§r"));
                } else {
                    // §number §r
                    tooltip.add(Text.of(String.format("§6%1$s / %2$s E§r", energy, maxUnitFill)));

                    if (energyPercentage <= 10) {
                        tooltip.add(Text.of("§4" + energyPercentage + "% Charged§r"));
                    } else if (energyPercentage <= 75) {
                        tooltip.add(Text.of("§e" + energyPercentage + "% Charged§r"));
                    } else {
                        tooltip.add(Text.of("§a" + energyPercentage + "% Charged§r"));
                    }

                    tooltip.add(Text.of(""));
                    tooltip.add(Text.translatable("tooltip.blockactivators.hold_shift"));
                }
                renderTooltip(matrices, (List<Text>) tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;

        // Add the switch mode button and the round-robin button
        // center screen: this.width / 2 - 66 (-66 bc width is 64 and 2 offset)
        // top left gui: (width - backgroundWidth) / 2 as X, (height - backgroundHeight) / 2 as Y

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        int xMargin = 10;
        int yMargin = 20;

        if (modeButton == null && mode != -1) {
            modeButton = this.addDrawableChild(createModeButtonWidget(
                    0, x, y, xMargin, yMargin, 96, 20, "guiButton.blockactivators.block_activator.switchMode", client)
            );
        }

        yMargin = 42;
        if (roundRobinButton == null) {
            roundRobinButton = this.addDrawableChild(createRoundRobinButtonWidget(
                    0, x, y, xMargin, yMargin, 96, 20, "guiButton.blockactivators.block_activator.switchRoundRobin", client)
            );
        }

        if (mode == -1) {
            BlockActivators.LOGGER.error("[Block Activators] Mode for a block activator is -1! Big problem!");
        }
    }

    public CyclingButtonWidget<Modes> createModeButtonWidget(int buttonIndex, int x, int y, int xMargin, int yMargin, int buttonWidth, int buttonHeight, String translationKey, MinecraftClient client) {
        return CyclingButtonWidget.builder(Modes::getTranslatableName).values(Modes.values()).initially(Modes.byOrdinal(mode)).build(x + xMargin, y + yMargin, buttonWidth, buttonHeight, Text.translatable(translationKey), (button, mode) -> {
            switchMode(mode, client);
        });
    }

    public CyclingButtonWidget<RoundRobinModes> createRoundRobinButtonWidget(int buttonIndex, int x, int y, int xMargin, int yMargin, int buttonWidth, int buttonHeight, String translationKey, MinecraftClient client) {
        return CyclingButtonWidget.builder(RoundRobinModes::getTranslatableName).values(RoundRobinModes.values()).initially(RoundRobinModes.byValue(roundRobin)).build(x + xMargin, y + yMargin, buttonWidth, buttonHeight, Text.translatable(translationKey), (button, mode) -> {
            switchRoundRobin(mode, client);
        });
    }

    private void switchMode(Modes mode, MinecraftClient client) {
        client.execute(() -> {
            // ORDER
            // int modeID = buf.readInt();
            // BlockPos pos = buf.readBlockPos();
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(mode.getId());
            buf.writeBlockPos(this.blockPos);

            ClientPlayNetworking.send(new Identifier(BlockActivators.MOD_ID, "update_click_mode_packet"), buf);
        });
    }

    private void switchRoundRobin(RoundRobinModes mode, MinecraftClient client) {
        client.execute(() -> {
            // ORDER
            // boolean roundRobin = buf.readBoolean();
            // BlockPos pos = buf.readBlockPos();
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(mode.isOn());
            buf.writeBlockPos(this.blockPos);

            ClientPlayNetworking.send(new Identifier(BlockActivators.MOD_ID, "update_round_robin_packet"), buf);
        });
    }

    private static Optional<BlockPos> getPos(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            BlockPos pos = ((BlockActivatorScreenHandler) handler).getPos();
            return pos != null ? Optional.of(pos) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static int getMode(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getMode();
        } else {
            return -1;
        }
    }

    private static boolean getRoundRobin(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getRoundRobin();
        } else {
            return false;
        }
    }

    private int getEnergyAmount(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getEnergyAmount();
        } else {
            return -1;
        }
    }

    private int getDrainRate(ScreenHandler handler) {
        if (handler instanceof BlockActivatorScreenHandler) {
            return ((BlockActivatorScreenHandler) handler).getDrainRate();
        } else {
            return -1;
        }
    }
}