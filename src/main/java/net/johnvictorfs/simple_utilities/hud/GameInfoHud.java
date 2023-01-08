package net.johnvictorfs.simple_utilities.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.autoconfig.AutoConfig;
import net.johnvictorfs.simple_utilities.config.SimpleUtilitiesConfig;
import net.johnvictorfs.simple_utilities.helpers.Colors;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.johnvictorfs.simple_utilities.mixin.GameClientMixin;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

import java.util.*;

@Environment(EnvType.CLIENT)
public class GameInfoHud {
    private final MinecraftClient client;
    private final TextRenderer fontRenderer;
    private ClientPlayerEntity player;
    private MatrixStack matrixStack;
    private final ItemRenderer itemRenderer;
    private SimpleUtilitiesConfig config;

    public GameInfoHud(MinecraftClient client) {
        this.client = client;
        this.fontRenderer = client.textRenderer;
        this.itemRenderer = client.getItemRenderer();

        this.config = AutoConfig.getConfigHolder(SimpleUtilitiesConfig.class).getConfig();

        AutoConfig.getConfigHolder(SimpleUtilitiesConfig.class).registerSaveListener((manager, data) -> {
            // Update local config when new settings are saved
            this.config = data;
            return ActionResult.SUCCESS;
        });
    }

    public void draw(MatrixStack matrixStack) {
        if (!config.uiConfig.toggleSimpleUtilitiesHUD) return;

        this.player = this.client.player;

        this.matrixStack = matrixStack;

        RenderSystem.enableBlend();

        this.drawInfos();

        this.client.getProfiler().pop();
    }

    private void drawInfos() {
        // Draw lines of Array of Game info in the screen
        List<String> gameInfo = getGameInfo();

        if (config.uiConfig.toggleEquipmentStatus && !(this.client.currentScreen instanceof ChatScreen)) {
            drawEquipmentInfo();
        }


        // Get the longest string in the array
        int longestString = 0;
        int BoxWidth = 0;
        for (String s : gameInfo) {
            if (s.length() > longestString) {
                longestString = s.length();
                BoxWidth = this.fontRenderer.getWidth(s);
            }
        }

        int lineHeight = this.fontRenderer.fontHeight + 2;
        int configXPosition = config.statusElements.Xcords;
        int yAxis = (((this.client.getWindow().getScaledHeight()) - ((lineHeight + 4) * gameInfo.size())) + (lineHeight + 4)) * (config.statusElements.Ycords) / 100;
        int xAxis = (((this.client.getWindow().getScaledWidth() - 4) - 4) - (BoxWidth)) * configXPosition / 100;

        // Add Padding to left of the screen
        if (xAxis <= 4) {
            xAxis = 4;
        }

        for (String line : gameInfo) {
            int offset = 0;
            if (configXPosition >= 50) {
                int lineLength = this.fontRenderer.getWidth(line);
                offset = (BoxWidth - lineLength);
            }

            this.fontRenderer.drawWithShadow(this.matrixStack, line, xAxis + offset, yAxis + 4, config.uiConfig.textColor);
            yAxis += lineHeight;
        }

        if (config.uiConfig.toggleSprintStatus && (this.client.options.sprintKey.isPressed() || this.player.isSprinting())) {
            this.drawSprintingInfo();
        }
    }

    private void drawSprintingInfo() {
        final String sprintingText = (Text.translatable("text.hud.simple_utilities.sprinting")).getString();

        int yAxis = (((this.client.getWindow().getScaledHeight() - this.fontRenderer.fontHeight + 2) - 4) * (config.uiConfig.sprintStatusLocationY) / 100);
        int xAxis = (((this.client.getWindow().getScaledWidth() - this.fontRenderer.getWidth(sprintingText)) - 8) * (config.uiConfig.sprintStatusLocationX) / 100);

        // Add Padding to left of the screen
        if (xAxis <= 4) {
            xAxis = 4;
        }

        // Add Padding to top of the screen
        if (yAxis <= 4) {
            yAxis = 4;
        }

        // Sprinting Info
        this.fontRenderer.drawWithShadow(this.matrixStack, sprintingText, xAxis, yAxis, config.uiConfig.textColor);
    }

    private static String capitalize(String str) {
        // Capitalize first letter of a String
        if (str == null) return null;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getOffset(Direction facing) {
        String offset = "";

        if (facing.getOffsetX() > 0) {
            offset += "+X";
        } else if (facing.getOffsetX() < 0) {
            offset += "-X";
        }

        if (facing.getOffsetZ() > 0) {
            offset += " +Z";
        } else if (facing.getOffsetZ() < 0) {
            offset += " -Z";
        }

        return offset.trim();
    }

    private String zeroPadding(int number) {
        return (number >= 10) ? Integer.toString(number) : String.format("0%s", number);
    }

    private String secondsToString(int pTime) {
        final int min = pTime / 60;
        final int sec = pTime - (min * 60);

        final String strMin = zeroPadding(min);
        final String strSec = zeroPadding(sec);
        return String.format("%s:%s", strMin, strSec);
    }

    private void drawStatusEffectInfo() {
        if (this.client.player != null) {
            Map<StatusEffect, StatusEffectInstance> effects = this.client.player.getActiveStatusEffects();

            for (Map.Entry<StatusEffect, StatusEffectInstance> effect : effects.entrySet()) {
                String effectName = I18n.translate(effect.getKey().getTranslationKey());

                String duration = secondsToString(effect.getValue().getDuration() / 20);

                int color = effect.getKey().getColor();

                this.fontRenderer.drawWithShadow(this.matrixStack, effectName + " " + duration, 40, 200, color);
            }
        }
    }

    private void drawEquipmentInfo() {
        List<ItemStack> equippedItems = new ArrayList<>();
        PlayerInventory inventory = this.player.getInventory();
        int maxLineHeight = Math.max(10, this.fontRenderer.getWidth(""));

//        int lineHeight = this.fontRenderer.fontHeight + 2;
//        int screenHeight = this.client.getWindow().getScaledHeight();
//        int screenWidth = this.client.getWindow().getScaledWidth();
//        int YScreenPosition = (screenHeight - lineHeight) - 4;
//        int XScreenPosition = (screenWidth - this.fontRenderer.getWidth(sprintingText)) - 8;
//        int configYPosition = config.uiConfig.sprintStatusLocationY;
//        int configXPosition = config.uiConfig.sprintStatusLocationX;
//        int yAxis = (YScreenPosition * configYPosition / 100) + 4;
//        int xAxis = XScreenPosition * configXPosition / 100;
//
//        // Add Padding to left and right of the screen
//        if (xAxis <= 4) {
//            xAxis = 4;
//        } else if (xAxis >= screenWidth - 4) {
//            xAxis = screenWidth - 4;
//        }

        ItemStack mainHandItem = inventory.getMainHandStack();
        maxLineHeight = Math.max(maxLineHeight, this.fontRenderer.getWidth(I18n.translate(mainHandItem.getTranslationKey())));
        equippedItems.add(mainHandItem);

        for (ItemStack secondHandItem : inventory.offHand) {
            maxLineHeight = Math.max(maxLineHeight, this.fontRenderer.getWidth(I18n.translate(secondHandItem.getTranslationKey())));
            equippedItems.add(secondHandItem);
        }

        for (ItemStack armourItem : this.player.getInventory().armor) {
            maxLineHeight = Math.max(maxLineHeight, this.fontRenderer.getWidth(I18n.translate(armourItem.getTranslationKey())));
            equippedItems.add(armourItem);
        }

        maxLineHeight = (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
        int itemTop = this.client.getWindow().getScaledHeight() - maxLineHeight;

        int lineHeight = this.fontRenderer.fontHeight + 6;

        // Draw in order Helmet -> Chestplate -> Leggings -> Boots
        for (ItemStack equippedItem : Lists.reverse(equippedItems)) {
            if (equippedItem.getItem().equals(Blocks.AIR.asItem())) {
                // Skip empty slots
                continue;
            }

            this.itemRenderer.renderInGuiWithOverrides(equippedItem, 2, itemTop - 68);

            if (equippedItem.getMaxDamage() != 0) {
                int currentDurability = equippedItem.getMaxDamage() - equippedItem.getDamage();

                String itemDurability = currentDurability + "/" + equippedItem.getMaxDamage();

                // Default Durability Color
                int color = config.uiConfig.textColor;

                if (currentDurability < equippedItem.getMaxDamage()) {
                    // Start as Green if item has lost at least 1 durability
                    color = Colors.lightGreen;
                }
                if (currentDurability <= (equippedItem.getMaxDamage() / 1.5)) {
                    color = Colors.lightYellow;
                }
                if (currentDurability <= (equippedItem.getMaxDamage() / 2.5)) {
                    color = Colors.lightOrange;
                }
                if (currentDurability <= (equippedItem.getMaxDamage()) / 4) {
                    color = Colors.lightRed;
                }

                // Draw Durability
                this.fontRenderer.drawWithShadow(this.matrixStack, itemDurability, 22, itemTop - 64, color);
            } else {
                int inventoryCount = inventory.count(equippedItem.getItem());
                int count = equippedItem.getCount();

                // Icon
                if (inventoryCount > 1) {
                    String itemCount = count + " (" + inventoryCount + ")";
                    this.fontRenderer.drawWithShadow(this.matrixStack, itemCount, 22, itemTop - 64, config.uiConfig.textColor);
                }
            }

            itemTop += lineHeight;
        }
    }

    private static String parseTime(long time) {
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        String ampm = "AM";

        if (hours >= 12) {
            hours -= 12;
            ampm = "PM";
        }

        if (hours == 0) hours = 12;

        String mm = "0" + minutes;
        mm = mm.substring(mm.length() - 2);

        return hours + ":" + mm + " " + ampm;
    }

    private List<String> getGameInfo() {
        List<String> gameInfo = new ArrayList<>();

        if (config.statusElements.toggleCoordinatesStatus || config.statusElements.toggleDirectionStatus) {
            String coordDirectionStatus = "";
            Direction facing = this.player.getHorizontalFacing();
            String translatedDirection = Text.translatable("text.direction.simple_utilities." + facing.asString()).getString();
            String direction = translatedDirection + " " + getOffset(facing);

            if (config.statusElements.toggleCoordinatesStatus) {
                String coordsFormat = "%d, %d, %d";
                coordDirectionStatus += String.format(coordsFormat, (int) this.player.getX(), (int) this.player.getY(), (int) this.player.getZ());

                if (config.statusElements.toggleDirectionStatus) {
                    coordDirectionStatus += " (" + direction + ")";
                }
            } else if (config.statusElements.toggleDirectionStatus) {
                coordDirectionStatus += direction;
            }

            gameInfo.add(coordDirectionStatus);
        }

        if (config.statusElements.toggleNetherCoordinateConversion) {
            String coordsFormat = "X: %.0f, Z: %.0f";
            if (this.player.getWorld().getRegistryKey().getValue().toString().equals("minecraft:overworld")) {
                gameInfo.add("Nether: " + String.format(coordsFormat, this.player.getX() / 8, this.player.getZ() / 8));
            } else if (this.player.getWorld().getRegistryKey().getValue().toString().equals("minecraft:the_nether")) {
                gameInfo.add("Overworld: " + String.format(coordsFormat, this.player.getX() * 8, this.player.getZ() * 8));
            }
        }

        if (config.statusElements.toggleFpsStatus) {
            // Get everything from fps debug string until the 's' from 'fps'
            // gameInfo.add(client.fpsDebugString.substring(0, client.fpsDebugString.indexOf("s") + 1));
            gameInfo.add(String.format("%d fps", ((GameClientMixin) MinecraftClient.getInstance()).getCurrentFps()));
        }

        // Player Speed
        if (config.statusElements.togglePlayerSpeedStatus) {
            // Calculating Speed
            Vec3d playerPosVec = Objects.requireNonNull(client.player).getPos();
            double travelledX = playerPosVec.x - client.player.prevX;
            double travelledZ = playerPosVec.z - client.player.prevZ;
            double currentSpeed = MathHelper.sqrt((float)(travelledX * travelledX + travelledZ * travelledZ));
            //double currentVertSpeed = playerPosVec.y - client.player.prevY;
            gameInfo.add(String.format("%.2f m/s", currentSpeed / 0.05F));
        }

        // Get translated biome info
        if (client.world != null) {

            // Light Level
            if (config.statusElements.toggleLightLevelStatus) {
                int lightLevel = client.world.getLightLevel(Objects.requireNonNull(client.player).getBlockPos());
                gameInfo.add("Light Level: " + lightLevel);
            }

            if (config.statusElements.toggleBiomeStatus) {
                Optional<RegistryKey<Biome>> biome = this.client.world.getBiome(player.getBlockPos()).getKey();

                if (biome.isPresent()) {
                    String biomeName = Text.translatable("biome." + biome.get().getValue().getNamespace() + "." + biome.get().getValue().getPath()).getString();
                    gameInfo.add(Text.translatable("text.hud.simple_utilities.biome", capitalize(biomeName)).getString());
                }
            }

            if (config.statusElements.toggleGameTimeStatus) {
                // Add current parsed time
                gameInfo.add(parseTime(client.world.getTimeOfDay()));
            }
        }

        if (config.statusElements.togglePlayerName) {
            gameInfo.add(player.getEntityName());
        }

        // Just Don't Create a Text Line if there is no Info
        if (client.getCurrentServerEntry() != null) {
            if (config.statusElements.toggleServerName) {
                String serverName = client.getCurrentServerEntry().name;
                gameInfo.add(serverName);
            }

            if (config.statusElements.toggleServerAddress) {
                String serverIp = client.getCurrentServerEntry().address;
                gameInfo.add(serverIp);
            }
        }



        return gameInfo;
    }
}
