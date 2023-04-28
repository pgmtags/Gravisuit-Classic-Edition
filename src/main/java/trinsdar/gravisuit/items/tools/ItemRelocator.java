package trinsdar.gravisuit.items.tools;

import ic2.api.tiles.teleporter.TeleporterTarget;
import ic2.core.IC2;
import ic2.core.inventory.base.IHasHeldGui;
import ic2.core.inventory.base.IPortableInventory;
import ic2.core.item.base.IC2ElectricItem;
import ic2.core.item.tool.electric.PortableTeleporter;
import ic2.core.platform.registries.IC2Items;
import ic2.core.platform.rendering.IC2Textures;
import ic2.core.platform.rendering.features.item.ISimpleItemModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import trinsdar.gravisuit.GravisuitClassic;
import trinsdar.gravisuit.block.BlockEntityPlasmaPortal;
import trinsdar.gravisuit.entity.PlasmaBall;
import trinsdar.gravisuit.items.container.ItemInventoryRelocator;
import trinsdar.gravisuit.util.GravisuitConfig;
import trinsdar.gravisuit.util.GravisuitLang;
import trinsdar.gravisuit.util.Registry;

public class ItemRelocator extends IC2ElectricItem implements ISimpleItemModel, IHasHeldGui /*BasicElectricItem implements IHandHeldInventory*/ {

    public ItemRelocator() {
        super("relocator");
        Registry.REGISTRY.put(new ResourceLocation(GravisuitClassic.MODID, "relocator"), this);
    }

    @Override
    protected int getEnergyCost(ItemStack itemStack) {
        return 1000000;
    }

    @Override
    public boolean canProvideEnergy(ItemStack itemStack) {
        return false;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return GravisuitConfig.POWER_VALUES.RELOCATOR_STORAGE;
    }

    @Override
    public int getTier(ItemStack itemStack) {
        return 5;
    }

    @Override
    public int getTransferLimit(ItemStack stack) {
        return GravisuitConfig.POWER_VALUES.RELOCATOR_TRANSFER;
    }

    @Override
    public IPortableInventory getInventory(Player player, InteractionHand interactionHand, ItemStack itemStack) {
        return new ItemInventoryRelocator(player, this, itemStack, interactionHand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag nbt = stack.getOrCreateTag();
        if (IC2.PLATFORM.isSimulating() && IC2.KEYBOARD.isModeSwitchKeyDown(player)){
            byte mode = nbt.getByte("mode");
            if (mode == 2) {
                nbt.putByte("mode", (byte) 0);
                player.displayClientMessage(this.translate(GravisuitLang.messageRelocatorPersonal, ChatFormatting.GREEN), false);
            } else if (mode == 0){
                nbt.putByte("mode", (byte) 1);
                player.displayClientMessage(this.translate(GravisuitLang.messageRelocatorTranslocator, ChatFormatting.GOLD), false);
            } else {
                nbt.putByte("mode", (byte) 2);
                player.displayClientMessage(this.translate(GravisuitLang.messageRelocatorPortal, ChatFormatting.AQUA), false);
            }
            return InteractionResultHolder.success(stack);
        }
        if (IC2.PLATFORM.isSimulating())
            if (player.isCrouching() || nbt.getByte("mode") == 0) {
                IC2.PLATFORM.launchGui(player, hand, null, this.getInventory(player, hand, stack));
                return InteractionResultHolder.success(stack);
            } else if (nbt.getByte("mode") == 1){
                if (nbt.contains("DefaultLocation")){
                    if (nbt.contains("Locations")){
                        CompoundTag map = nbt.getCompound("Locations");
                        String name = nbt.getString("DefaultLocation");
                        if (map.contains(name)){
                            PlasmaBall entity = new PlasmaBall(player.level, player, TeleportData.fromNBT(map.getCompound(name), name), hand);
                            level.addFreshEntity(entity);
                            return InteractionResultHolder.success(stack);
                        }
                    }
                }

            }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CompoundTag nbt = stack.getOrCreateTag();
        if (IC2.PLATFORM.isSimulating()){
            if (nbt.getByte("mode") == 2){
                if (nbt.contains("DefaultLocation")){
                    if (nbt.contains("Locations")){
                        CompoundTag map = nbt.getCompound("Locations");
                        String name = nbt.getString("DefaultLocation");
                        if (map.contains(name)){
                            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
                            context.getLevel().setBlock(pos, Registry.PLASMA_PORTAL.defaultBlockState(), 3);
                            BlockEntity be = context.getLevel().getBlockEntity(pos);
                            TeleportData teleportData = TeleportData.fromNBT(map.getCompound(name), name);
                            BlockPos teleportPos = BlockPos.of(teleportData.getPos());
                            TeleportData origin = new TeleportData(pos.asLong(), context.getLevel().dimension().location().toString(), "origin");
                            if (be instanceof BlockEntityPlasmaPortal portal){
                                portal.setOtherEnd(teleportData);
                            }
                            TeleporterTarget teleporterTarget = teleportData.toTeleportTarget();
                            ServerLevel teleportWorld = teleporterTarget.getWorld();
                            teleportWorld.setBlock(teleportPos, Registry.PLASMA_PORTAL.defaultBlockState(), 3);
                            BlockEntity teleportBE = teleportWorld.getBlockEntity(teleportPos);
                            if (teleportBE instanceof BlockEntityPlasmaPortal portal){
                                portal.setOtherEnd(origin);
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }
        return super.useOn(context);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public TextureAtlasSprite getTexture() {
        return IC2Textures.getMappedEntriesItem(GravisuitClassic.MODID, "tools").get("relocator");
    }

    public static void teleportEntity(Player player, CompoundTag teleportData, ItemStack stack){
        ((PortableTeleporter)IC2Items.PORTABLE_TELEPORTER).teleportEntity(player, TeleporterTarget.read(teleportData), player.getMotionDirection(), stack);
    }

    public enum TeleportMode {
        PERSONAL,
        PORTAL,
        TRANSLOCATOR;

        public TeleportMode getNext() {
            return switch (this){
                case PERSONAL -> PORTAL;
                case PORTAL -> TRANSLOCATOR;
                case TRANSLOCATOR -> PERSONAL;
            };
        }
    }

    public static class TeleportData {
        long pos;
        String dimId;
        String name;

        public TeleportData(long pos, String dimId, String name){
            this.pos = pos;
            this.dimId = dimId;
            this.name = name;
        }

        public TeleportData(String name){
            this.pos = 0;
            this.dimId = "minecraft:overworld";
            this.name = name;
        }

        public long getPos() {
            return pos;
        }

        public String getDimId() {
            return dimId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDimId(String dimId) {
            this.dimId = dimId;
        }

        public CompoundTag writeToNBT(CompoundTag compound) {
            compound.putLong("pos", pos);
            compound.putString("id", dimId);
            return compound;
        }

        public static TeleportData fromNBT(CompoundTag tag, String name){
            return new TeleportData(tag.getLong("pos"), tag.getString("id"), name);
        }

        public TeleporterTarget toTeleportTarget(){
            CompoundTag compoundTag = writeToNBT(new CompoundTag());
            return TeleporterTarget.read(compoundTag);
        }
    }
}
