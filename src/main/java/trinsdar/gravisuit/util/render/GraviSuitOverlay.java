package trinsdar.gravisuit.util.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import ic2.api.items.electric.ElectricItem;
import ic2.core.item.wearable.armor.electric.ElectricPackArmor;
import ic2.core.item.wearable.base.IC2ElectricJetpackBase;
import ic2.core.item.wearable.base.IC2JetpackBase;
import ic2.core.item.wearable.base.IC2ModularElectricArmor;
import ic2.core.utils.helpers.StackUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import trinsdar.gravisuit.items.armor.*;
import trinsdar.gravisuit.util.GravisuitConfig;

public class GraviSuitOverlay extends Gui {

	/**
	 *  TODO 1: Finish it
	 *  TODO 2: Re-ad IC2 armor. If needed, remove {@link IHasOverlay} and check by instance.
	 *  TODO 3: Re-implement Config values
	 * */

	public static Minecraft mc;
	public static Font fontRenderer;

	static int offset = 3;
	int xPos = offset;
	int yPos1 = offset;
	int yPos2, yPos3, yPos4;

	public GraviSuitOverlay(Minecraft mc, ItemRenderer itemRenderer) {
		super(mc, itemRenderer);

		GraviSuitOverlay.mc = mc;
		fontRenderer = mc.font;
	}

	@SubscribeEvent
	public void renderOverlay(RenderGuiOverlayEvent e) {

		Window window = e.getWindow();
		PoseStack matrix = e.getPoseStack();

		Player player = mc.player;
		assert player != null;
		ItemStack stackArmor = player.getItemBySlot(EquipmentSlot.CHEST);
		Item itemArmor = stackArmor.getItem();

		if (GravisuitConfig.CLIENT.POSITIONS == GravisuitConfig.Client.Positions.BOTTOMLEFT || GravisuitConfig.CLIENT.POSITIONS == GravisuitConfig.Client.Positions.BOTTOMRIGHT) {
			yPos1 = window.getGuiScaledHeight() - ((fontRenderer.lineHeight * 2) + 5);
		}

		yPos2 = yPos1 + fontRenderer.lineHeight + 2;
		yPos3 = yPos2 + fontRenderer.lineHeight + 2;
		yPos4 = yPos3 + fontRenderer.lineHeight + 2;

		if (itemArmor instanceof IHasOverlay overlay && overlay.isEnabled(stackArmor)) {

			CompoundTag tag = overlay.getArmorNBT(stackArmor, true);

			if (itemArmor instanceof IC2ModularElectricArmor armor){
				IC2JetpackBase base = armor.getJetpack(stackArmor);
				if (base != null){
					itemArmor = base;
				}
			}

			// Energy stats starts

			int currentCharge = ElectricItem.MANAGER.getCharge(stackArmor);
			int maxCapacity = ElectricItem.MANAGER.getCapacity(stackArmor);
			int energyLevel = (int) Math.round((double) currentCharge / (double) maxCapacity * 100);

			/** ENERGY STATS GENERAL */
			String energyString = "message.info.energy";
			Component energyToDisplay = formatComplexMessage(ChatFormatting.YELLOW, energyString, getEnergyTextColor(energyLevel), energyLevel + "%");

			// Jetpack Engine Starts

			boolean isEngineOn = !tag.getBoolean("disabled");
			String engineStatus = isEngineOn ? "message.info.on" : "message.info.off";
			ChatFormatting engineStatusColor = isEngineOn ? ChatFormatting.GREEN : ChatFormatting.RED;

			/** ENGINE STATUS GENERAL */

			String engineString = "message.info.jetpack.engine";
			Component engineToDisplay = formatComplexMessage(ChatFormatting.YELLOW, engineString, engineStatusColor, engineStatus);

			// Hover Start

			String hoverModeS = getWorkStatus(stackArmor);
			ChatFormatting hoverModeC = getWorkStatusColor(stackArmor);

			/** HOVER STATUS GENERAL */

			String hoverString = "message.info.jetpack.hover";
			Component hoverToDisplay = formatComplexMessage(ChatFormatting.YELLOW, hoverString, hoverModeC, hoverModeS);

			// Gravi Engine Starts

			boolean isGraviEngineOn = tag.getBoolean("engine_on");
			String graviEngineStatus = isGraviEngineOn ? "message.info.on" : "message.info.off";
			ChatFormatting graviEngineStatusColor = isGraviEngineOn ? ChatFormatting.GREEN : ChatFormatting.RED;

			/** ENGINE STATUS GENERAL */

			String graviEngineString = "message.info.gravitation";
			Component graviEngineToDisplay = formatComplexMessage(ChatFormatting.AQUA, graviEngineString, graviEngineStatusColor, graviEngineStatus);

			if (itemArmor instanceof ElectricPackArmor) {
				fontRenderer.drawShadow(matrix, energyToDisplay, getXOffset(energyToDisplay.getString(), window), yPos1, 0);
			}
			if (itemArmor instanceof IC2ElectricJetpackBase) {
				fontRenderer.drawShadow(matrix, energyToDisplay, getXOffset(energyToDisplay.getString(), window), yPos1, 0);
				fontRenderer.drawShadow(matrix, engineToDisplay, getXOffset(engineToDisplay.getString(), window), yPos2, 0);
				fontRenderer.drawShadow(matrix, hoverToDisplay, getXOffset(hoverToDisplay.getString(), window), yPos3, 0);
			}
			if (itemArmor instanceof IGravitationJetpack) {
				fontRenderer.drawShadow(matrix, graviEngineToDisplay, getXOffset(graviEngineToDisplay.getString(), window), yPos4, 0);
			}
		}
	}

	private static int getXOffset(String value, Window window) {
		return switch (GravisuitConfig.CLIENT.POSITIONS) {
			case TOPLEFT, BOTTOMLEFT -> offset;
			case TOPRIGHT, BOTTOMRIGHT -> window.getGuiScaledWidth() - 3 - fontRenderer.width(value);
			case TOPMIDDLE -> (int) (window.getGuiScaledWidth() * 0.50F) - (fontRenderer.width(value) / 2);
		};
	}

	public boolean or(Item compare, Item... items){
		for (Item item : items){
			if (compare == item){
				return true;
			}
		}
		return false;
	}
		
	public static ChatFormatting getEnergyTextColor(double energyLevel) {
		if (energyLevel == 100) {
			return ChatFormatting.GREEN;
		}
		if ((energyLevel <= 100) && (energyLevel > 50)) {
			return ChatFormatting.GOLD;
		}
		if (energyLevel <= 50) {
			return ChatFormatting.RED;
		}
		return null;
	}

	public static MutableComponent formatSimpleMessage(ChatFormatting color, String text) {
		return Component.translatable(text).withStyle(color);
	}

	public static MutableComponent formatComplexMessage(ChatFormatting color1, String text1, ChatFormatting color2, String text2) {
		return formatSimpleMessage(color1, text1).append(formatSimpleMessage(color2, text2));
	}

	private static IC2JetpackBase.HoverMode getHoverStatus(ItemStack stack) {
		CompoundTag tag = stack.getItem() instanceof IC2ModularElectricArmor ? StackUtil.getNbtData(stack).getCompound("jetpack_data") : StackUtil.getNbtData(stack);
		return IC2JetpackBase.HoverMode.byIndex(tag.getByte("HoverMode"));
	}

	public static String getWorkStatus(ItemStack stack) {
		IC2JetpackBase.HoverMode mode = getHoverStatus(stack);

		if (mode == IC2JetpackBase.HoverMode.BASIC) {
			return "message.info.basic";
		} else if (mode == IC2JetpackBase.HoverMode.ADV) {
			return "message.info.adv";
		} else {
			return "message.info.off";
		}
	}

	// Returns hover status color
	public static ChatFormatting getWorkStatusColor(ItemStack stack) {
		IC2JetpackBase.HoverMode mode = getHoverStatus(stack);

		if (mode == IC2JetpackBase.HoverMode.BASIC) {
			return ChatFormatting.GREEN;
		} else if (mode == IC2JetpackBase.HoverMode.ADV) {
			return ChatFormatting.AQUA;
		} else {
			return ChatFormatting.RED;
		}
	}

	// TODO: Check usability of this method
	@Deprecated
	public static int getCharge(ItemStack stack) {
		CompoundTag nbt = StackUtil.getNbtData(stack);
		int e = nbt.getInt("charge");
		return e;
	}
}
