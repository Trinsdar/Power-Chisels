package trinsdar.powerchisels;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import ic2.api.item.ElectricItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import team.chisel.api.IChiselGuiType;
import team.chisel.api.IChiselItem;
import team.chisel.api.carving.ICarvingVariation;
import team.chisel.api.carving.IChiselMode;
import team.chisel.common.init.ChiselTabs;
import team.chisel.common.util.NBTUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemFluxedChisel extends Item implements IChiselItem {

    public static final int MAX_ENERGY = 40000;

    public ItemFluxedChisel() {
        super();
        setMaxStackSize(1);
        setRegistryName("fluxed_chisel");
        setUnlocalizedName(PowerChisels.MODID + "." + "fluxedChisel");
        setCreativeTab(ChiselTabs.tab);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    public int getCost(){
        return 80;
    }

    public boolean hasEnoughEnergy(ItemStack stack){

        return stack.getCapability(CapabilityEnergy.ENERGY, null).getEnergyStored() >= getCost();
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            ItemStack empty = new ItemStack(this, 1, 0);
            ItemStack full = new ItemStack(this, 1, 0);
            full.getCapability(CapabilityEnergy.ENERGY, null).extractEnergy(MAX_ENERGY, false);
            full.getCapability(CapabilityEnergy.ENERGY, null).receiveEnergy(MAX_ENERGY, false);
            items.add(empty);
            items.add(full);
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        return 1.0D - (double)getEnergyStored(stack) / MAX_ENERGY;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.areItemsEqual(oldStack, newStack);
    }

    @Override
    public boolean canOpenGui(World world, EntityPlayer player, EnumHand hand) {
        return hasEnoughEnergy(player.getHeldItem(hand));
    }

    @Override
    public IChiselGuiType getGuiType(World world, EntityPlayer player, EnumHand hand) {
        return IChiselGuiType.ChiselGuiType.NORMAL;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List list, ITooltipFlag flag) {
        String base = "item.chisel.chisel.desc.";
        list.add(I18n.format(base + "gui", TextFormatting.AQUA, TextFormatting.GRAY));
        list.add(I18n.format(base + "lc1", TextFormatting.AQUA, TextFormatting.GRAY));
        list.add(I18n.format(base + "lc2", TextFormatting.AQUA, TextFormatting.GRAY));
        list.add("");
        list.add(I18n.format(base + "modes"));
        list.add(I18n.format(base + "modes.selected", TextFormatting.GREEN + I18n.format(NBTUtil.getChiselMode(stack).getUnlocName() + ".name")));
        list.add(I18n.format(base + "delete", TextFormatting.RED, TextFormatting.GRAY));
        if (stack.hasCapability(CapabilityEnergy.ENERGY, null))
        {
            IEnergyStorage energyStorage = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energyStorage != null)
            {
                double p = getDurabilityForDisplay(stack) * 100;
                list.add("L: " + (int) p + "%");
                list.add("E: " + energyStorage.getEnergyStored() + "/" + energyStorage.getMaxEnergyStored() + " FE");
            }
        }
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            if (hasEnoughEnergy(stack)){
                multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Chisel Damage", 2, 0));
            }
        }
        return multimap;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (hasEnoughEnergy(stack)){
            extractEnergy(stack, getCost(), false);
        }
        return super.hitEntity(stack, attacker, target);
    }

    @Override
    public boolean canChisel(World world, EntityPlayer player, ItemStack chisel, ICarvingVariation target) {
        return hasEnoughEnergy(chisel);
    }

    @Override
    public ItemStack craftItem(ItemStack chisel, ItemStack source, ItemStack target, EntityPlayer player) {
        if (chisel.isEmpty()) return ItemStack.EMPTY;
        int toCraft = Math.min(source.getCount(), target.getMaxStackSize());
        if (hasEnoughEnergy(chisel)) {
            int damageLeft = (MAX_ENERGY - (MAX_ENERGY - getEnergyStored(chisel)))/getCost();
            toCraft = Math.min(toCraft, damageLeft);
            extractEnergy(chisel,toCraft*getCost(), false);
        }
        ItemStack res = target.copy();
        source.shrink(toCraft);
        res.setCount(toCraft);
        return res;
    }

    @Override
    public boolean onChisel(World world, EntityPlayer player, ItemStack chisel, ICarvingVariation target) {
        return true;
    }

    @Override
    public boolean canChiselBlock(World world, EntityPlayer player, EnumHand hand, BlockPos pos, IBlockState state) {
        return hasEnoughEnergy(player.getHeldItem(hand));
    }

    @Override
    public boolean supportsMode(EntityPlayer player, ItemStack chisel, IChiselMode mode) {
        return true;
    }



    /* IEnergyStorage */

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt)
    {
        return new ICapabilitySerializable<NBTBase>()
        {
            EnergyStorage buffer = new EnergyStorage(MAX_ENERGY);

            @Override
            public NBTBase serializeNBT()
            {
                return CapabilityEnergy.ENERGY.writeNBT(buffer, null);
            }

            @Override
            public void deserializeNBT(NBTBase nbt)
            {
                CapabilityEnergy.ENERGY.readNBT(buffer, null, nbt);
            }

            @Override
            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing)
            {
                if (capability == CapabilityEnergy.ENERGY)
                    return true;
                return false;
            }

            @SuppressWarnings("unchecked")
            @Nullable
            @Override
            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing)
            {
                if (capability == CapabilityEnergy.ENERGY)
                    return (T)buffer;
                return null;
            }
        };
    }

    public int extractEnergy(ItemStack chisel, int maxExtract, boolean simulate) {
        return chisel.getCapability(CapabilityEnergy.ENERGY, null).extractEnergy(maxExtract, simulate);
    }

    public int getEnergyStored(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY, null).getEnergyStored();
    }



    /* Registery */
    public static final ItemFluxedChisel fluxedChisel = new ItemFluxedChisel();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(fluxedChisel);
    }
    public static void initRecipe(){

    }

    public void initModel(){
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }
}