package cn.davidma.tinymobfarm.common.item;

import java.util.List;

import cn.davidma.tinymobfarm.common.TinyMobFarm;
import cn.davidma.tinymobfarm.core.ConfigTinyMobFarm;
import cn.davidma.tinymobfarm.core.Reference;
import cn.davidma.tinymobfarm.core.util.EntityHelper;
import cn.davidma.tinymobfarm.core.util.Msg;
import cn.davidma.tinymobfarm.core.util.NBTHelper;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemLasso extends Item {

	public ItemLasso() {
		super();
		this.setRegistryName(Reference.MOD_ID + ":lasso");
		this.setUnlocalizedName(this.getRegistryName().toString());
		this.setMaxStackSize(1);
		this.setMaxDamage(ConfigTinyMobFarm.LASSO_DURABILITY);
		this.setCreativeTab(TinyMobFarm.creativeTabTinyMobFarm);
	}
	
	@Override
	public int getItemStackLimit(ItemStack stack) {
		return 1;
	}
	
	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
		if (NBTHelper.hasMob(stack) || target.isDead || !(target instanceof EntityLiving)) return false;
		
		NBTTagCompound nbt = NBTHelper.getBaseTag(stack)
		
		// Blacklist.
		if (EntityHelper.isMobBlacklisted((EntityLiving) target)) {
			if (!player.world.isRemote) {
				Msg.tellPlayer(player, "tinymobfarm.error.blacklisted_mob");
			}
			return true;
		}
		
		if (!player.world.isRemote) {
			NBTTagCompound mobData = target.serializeNBT();
			mobData.removeTag("Rotation");
			nbt.setTag(NBTHelper.MOB_DATA, mobData);
			nbt.setString(NBTHelper.MOB_NAME, target.getName());
			nbt.setString(NBTHelper.MOB_LOOTTABLE_LOCATION, EntityHelper.getLootTableLocation((EntityLiving) target));
			nbt.setDouble(NBTHelper.MOB_HEALTH, Math.round(target.getHealth() * 10) / 10.0);
			nbt.setDouble(NBTHelper.MOB_MAX_HEALTH, target.getMaxHealth());
			nbt.setBoolean(NBTHelper.MOB_HOSTILE, target.isCreatureType(EnumCreatureType.MONSTER, false));
			
			NBTHelper.setBaseTag(stack, nbt);
			
			if (player.isCreative()) {
				ItemStack newLasso = new ItemStack(this);
				NBTHelper.setBaseTag(newLasso, nbt);
				player.addItemStackToInventory(newLasso);
			}
			
			target.setDead();
			player.inventory.markDirty();
		}
		
		return true;
	}
		
	@Override
	public EnumActionResult onItemUse (EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.inventory.getCurrentItem();
		
		if (!NBTHelper.hasMob(stack)) return EnumActionResult.FAIL;
		
		if (!player.canPlayerEdit(pos, facing, stack)) return EnumActionResult.FAIL;
		
		if (!world.isRemote) {
			NBTTagCompound nbt = NBTHelper.getBaseTag(stack);
			NBTTagCompound mobData = nbt.getCompoundTag(NBTHelper.MOB_DATA);
			
			BlockPos newPos = pos.offset(facing);
			
			NBTTagDouble x = new NBTTagDouble(newPos.getX() + 0.5);
			NBTTagDouble y = new NBTTagDouble(newPos.getY());
			NBTTagDouble z = new NBTTagDouble(newPos.getZ() + 0.5);
			NBTTagList mobPos = NBTHelper.createNBTList(x, y, z);
			mobData.setTag("Pos", mobPos);
			mobData.setInteger("Dimension", world.provider.getDimension());
			
			Entity mob = EntityList.createEntityFromNBT(mobData, world);
			if (mob != null) world.spawnEntity(mob);
			
			NBTHelper.getOrCreateTag(stack).removeTag(NBTHelper.MOB);
			
			stack.damageItem(1, player);
			player.inventory.markDirty();
		}
		
		return EnumActionResult.SUCCESS;
	}
	
	@Override
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		if (NBTHelper.hasMob(stack)) {
			NBTTagCompound nbt = NBTHelper.getBaseTag(stack);
			String name = nbt.getString(NBTHelper.MOB_NAME);
			double health = nbt.getDouble(NBTHelper.MOB_HEALTH);
			double maxHealth = nbt.getDouble(NBTHelper.MOB_MAX_HEALTH);
			
			tooltip.add(I18n.format("tinymobfarm.tooltip.release_mob"));
			tooltip.add(I18n.format("tinymobfarm.tooltip.mob_name", name));
			tooltip.add(I18n.format("tinymobfarm.tooltip.health", health, maxHealth));
			if (nbt.getBoolean(NBTHelper.MOB_HOSTILE)) tooltip.add(I18n.format("tinymobfarm.tooltip.hostile"));
		} else {
			tooltip.add(I18n.format("tinymobfarm.tooltip.capture"));
		}
	}
	
	@Override
	public boolean hasEffect(ItemStack stack) {
		return NBTHelper.hasMob(stack);
	}
}
