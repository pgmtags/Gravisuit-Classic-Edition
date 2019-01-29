package trinsdar.gravisuit.items.armor;

import ic2.api.classic.audio.PositionSpec;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.audio.AudioSource;
import ic2.core.item.armor.base.ItemArmorJetpackBase;
import ic2.core.item.armor.base.ItemArmorJetpackBase.IIndirectJetpack;
import ic2.core.item.armor.electric.ItemArmorElectricJetpack;
import ic2.core.item.armor.electric.ItemArmorQuantumSuit;
import ic2.core.platform.lang.storage.Ic2InfoLang;
import ic2.core.platform.player.PlayerHandler;
import ic2.core.platform.registry.Ic2Lang;
import ic2.core.platform.registry.Ic2Sounds;
import ic2.core.util.misc.StackUtil;
import ic2.core.util.obj.ToolTipType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import trinsdar.gravisuit.GravisuitClassic;
import trinsdar.gravisuit.util.GravisuitLang;
import trinsdar.gravisuit.util.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemArmorGravisuit extends ItemArmorQuantumSuit implements IIndirectJetpack {
    public ItemArmorGravisuit.GravisuitJetpack jetpack = new GravisuitJetpack(this);

    public ItemArmorGravisuit() {
        super(44, EntityEquipmentSlot.CHEST);
        this.setRegistryName("gravisuit");
        this.setUnlocalizedName(GravisuitClassic.MODID + ".gravisuit");
        this.maxCharge = 5000000;
        this.transferLimit = 5000;
        this.setCreativeTab(IC2.tabIC2);
    }

    @Override
    public String getTexture() {
        return "ic2:textures/models/armor/quantumjetpack";
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onSortedItemToolTip(ItemStack stack, EntityPlayer player, boolean debugTooltip, List<String> tooltip, Map<ToolTipType, List<String>> sortedTooltip) {
        super.onSortedItemToolTip(stack, player, debugTooltip, tooltip, sortedTooltip);
        this.jetpack.onSortedItemToolTip(stack, player, debugTooltip, tooltip, sortedTooltip);
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
        super.onArmorTick(world, player, stack);
        this.jetpack.onArmorTick(world, player, stack);
    }

    public static boolean hasGravisuit(EntityPlayer player){
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);

        return !chest.isEmpty() && chest.getItem() instanceof ItemArmorGravisuit;
    }
    public static boolean hasQuantumLegs(EntityPlayer player){
        ItemStack legs = player.getItemStackFromSlot(EntityEquipmentSlot.LEGS);

        return !legs.isEmpty() && legs.getItem() instanceof ItemArmorQuantumSuit;
    }

    public ItemArmorJetpackBase getJetpack() {
        return this.jetpack;
    }

    public static class GravisuitJetpack extends ItemArmorElectricJetpack {
        Item item;

        public GravisuitJetpack(Item owner) {
            super();
            this.item = owner;
        }

        public AudioSource createAudioSource(ItemStack stack, EntityPlayer player, ItemArmorJetpackBase.JetpackUseMode mode) {
            if (player.capabilities.isFlying){
                return IC2.audioManager.createSource(player, PositionSpec.Backpack, Ic2Sounds.jetpack, true, false, IC2.audioManager.getDefaultVolume());
            }else {
                return null;
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void onSortedItemToolTip(ItemStack stack, EntityPlayer player, boolean debugTooltip, List<String> tooltip, Map<ToolTipType, List<String>> sortedTooltip) {
            List<String> ctrlTip = sortedTooltip.get(ToolTipType.Ctrl);
            ctrlTip.add(TextFormatting.UNDERLINE + Ic2Lang.pressTo.getLocalizedFormatted(GravisuitLang.doubleJump.getLocalizedFormatted(IC2.keyboard.getKeyName(6)), Ic2InfoLang.jetpackJumpToFly));
            ctrlTip.add(TextFormatting.UNDERLINE + Ic2Lang.pressTo.getLocalizedFormatted(IC2.keyboard.getKeyName(5), GravisuitLang.graviEngineToggle));
        }

        @Override
        public boolean isElectricJetpack(ItemStack itemStack) {
            return true;
        }

        @Override
        public int getMaxHeight(ItemStack itemStack, int worldheight) {
            return (int)((float)worldheight * 1.171875f);
        }

        @Override
        public String getTexture() {
            return "";
        }

        @Override
        public int getTier(ItemStack itemStack) {
            return 3;
        }

        @Override
        public double getTransferLimit(ItemStack itemStack) {
            return 0;
        }

        @Override
        public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
            NBTTagCompound nbt = StackUtil.getOrCreateNbtData(stack);
            boolean disabled = nbt.getBoolean("disabled");
            PlayerHandler handler = PlayerHandler.getHandlerForPlayer(player);
            byte jetpackTicker = nbt.getByte("JetpackTicker");
            Entity entity = player.getLowestRidingEntity();
            boolean server = IC2.platform.isSimulating();
            if (disabled) {
                if (server) {
                    if (jetpackTicker > 0) {
                        --jetpackTicker;
                        nbt.setByte("JetpackTicker", jetpackTicker);
                    } else if (handler.toggleKeyDown) {
                        nbt.setByte("JetpackTicker", (byte)10);
                        nbt.setBoolean("disabled", false);
                        IC2.platform.messagePlayer(player, GravisuitLang.graviEngineOn);
                    }
                }

            } else {
                if (handler.toggleKeyDown && jetpackTicker <= 0) {
                    if (server) {
                        nbt.setBoolean("disabled", true);
                        nbt.setByte("JetpackTicker", (byte)10);
                        IC2.platform.messagePlayer(player, GravisuitLang.graviEngineOff);
                        return;
                    }
                } else if (jetpackTicker > 0) {
                    --jetpackTicker;
                    nbt.setByte("JetpackTicker", jetpackTicker);
                }
            }

            Boolean hasSet = ItemArmorGravisuit.hasGravisuit(player);
            if (!disabled && ElectricItem.manager.getCharge(stack) >= 1024 && hasSet){
                if (handler.quantumArmorBoostSprint && player.isSprinting() && ItemArmorGravisuit.hasQuantumLegs(player)){
                    this.useEnergy(player, stack, 1024);
                }else {
                    this.useEnergy(player, stack, 512);
                }
                player.capabilities.allowFlying = true;
                player.stepHeight = 1.0625F;
                boolean flying = player.capabilities.isFlying;
                if(flying){
                    boolean sneaking = player.isSneaking();

                    float speed = 0.08f
                            * (flying ? 0.6f : 1.0f)
                            * (sneaking ? 0.1f : 1.0f);

                    if (player.moveForward > 0f) {
                        player.moveRelative(0f, 0f, 1f, speed);
                    } else if (player.moveForward < 0f) {
                        player.moveRelative(0f, 0f, 1f, -speed * 0.3f);
                    }

                    if (player.moveStrafing != 0f) {
                        player.moveRelative(1f, 0f, 0f, speed * 0.5f * Math.signum(player.moveStrafing));
                    }
                }
            }else {
                player.stepHeight = 0.6F;
                player.capabilities.allowFlying = false;
                player.capabilities.isFlying = false;
            }
        }

        public static List<String> playersWithSet = new ArrayList<String>();

        public static String playerKey(EntityPlayer player) {
            return player.getGameProfile().getName() + ":" + player.getEntityWorld().isRemote;
        }

        @SubscribeEvent
        public void updatePlayerAbilityStatus(LivingEvent.LivingUpdateEvent event) {
            if (event.getEntityLiving() instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) event.getEntityLiving();
                String key = playerKey(player);
                ItemStack stack = new ItemStack(Registry.gravisuit);
                NBTTagCompound nbt = StackUtil.getOrCreateNbtData(stack);
                boolean disabled = nbt.getBoolean("disabled");

                Boolean hasSet = ItemArmorGravisuit.hasGravisuit(player);
                if (playersWithSet.contains(key)) {
                    if (!hasSet) {
                        player.stepHeight = 0.6F;
                        if (!player.capabilities.isCreativeMode && !player.isSpectator()) {
                            player.capabilities.allowFlying = false;
                            player.capabilities.isFlying = false;
                        }
                        playersWithSet.remove(key);
                    }
                } else if (hasSet) {
                    playersWithSet.add(key);
                }
            }
        }
    }
}