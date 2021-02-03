package cn.davidma.tinymobfarm.common.tileentity;

import java.util.List;

import javax.annotation.Nullable;

import cn.davidma.tinymobfarm.common.block.BlockMobFarm;
import cn.davidma.tinymobfarm.core.ConfigTinyMobFarm;
import cn.davidma.tinymobfarm.core.EnumMobFarm;
import cn.davidma.tinymobfarm.core.Reference;
import cn.davidma.tinymobfarm.core.util.EntityHelper;
import cn.davidma.tinymobfarm.core.util.FakePlayerHelper;
import cn.davidma.tinymobfarm.core.util.NBTHelper;

import com.sun.javafx.scene.traversal.Direction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class TileEntityMobFarm extends TileEntity implements ITickable {
	
	private ItemStackHandler inventory = new ItemStackHandler(1);
	private EnumMobFarm mobFarmData;
	private EntityLiving model;
	private EnumFacing modelFacing;
	private int currProgress;
	private boolean powered;
	private boolean shouldUpdate;

	@Override
	public void update() {
		if (this.shouldUpdate) {
			this.updateModel();
			this.updateRedstone();
			this.shouldUpdate = false;
		}
		if (this.isWorking()) {
			if (!(ConfigTinyMobFarm.CHEST_SPACE_CHECK)) {
				this.currProgress++;
				if (!this.world.isRemote && this.mobFarmData != null) {
					if (this.currProgress >= this.mobFarmData.getMaxProgress()) {
						this.currProgress = 0;

						this.generateDrops();

						FakePlayer daniel = FakePlayerHelper.getPlayer((WorldServer) world);
						this.getLasso().damageItem(this.mobFarmData.getRandomDamage(this.world.rand), daniel);

						this.saveAndSync();
					}
				}
			}
			if (ConfigTinyMobFarm.CHEST_SPACE_CHECK) {
				final IItemHandler inventory = getInv(this.world, this.pos.down(1));
				if (inventory == null) {
					this.currProgress = 0;
				}else if (inventory != null) {
					this.currProgress++;
					if (!this.world.isRemote && this.mobFarmData != null) {
						if (this.currProgress >= this.mobFarmData.getMaxProgress()) {
							this.currProgress = 0;

							this.generateDrops();

							FakePlayer daniel = FakePlayerHelper.getPlayer((WorldServer) world);
							this.getLasso().damageItem(this.mobFarmData.getRandomDamage(this.world.rand), daniel);

							this.saveAndSync();
						}
					}
				}
			}
		} else {
			this.currProgress = 0;
		}
	}
	
	private void generateDrops() {
		ItemStack lasso = this.getLasso();
		String lootTableLocation = NBTHelper.getBaseTag(lasso).getString(NBTHelper.MOB_LOOTTABLE_LOCATION);
		if (lootTableLocation.isEmpty()) return;

		List<ItemStack> drops = EntityHelper.generateLoot(new ResourceLocation(lootTableLocation), this.world);
		for (EnumFacing facing: EnumFacing.values()) {
			TileEntity tileEntity = this.world.getTileEntity(this.pos.offset(facing));
			if (tileEntity != null) {
				
				if (tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
					IItemHandler itemHandler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
					for (int i = 0; i < drops.size(); i++) {
						ItemStack remain = ItemHandlerHelper.insertItemStacked(itemHandler, drops.get(i), false);
						if (remain.isEmpty()) {
							drops.remove(i);
							i--;
						}
					}
				}
				
				if (drops.isEmpty()) return;
			}
		}
		
		for (ItemStack stack: drops) {
			EntityItem entityItem = new EntityItem(this.world, this.pos.getX() + 0.5, this.pos.getY() + 1, this.pos.getZ() + 0.5, stack);
			this.world.spawnEntity(entityItem);
		}
		
	}

	// Check each side of the MobFarm for Item Handler capability
	private IItemHandler getInv(World world, BlockPos pos){
		for (EnumFacing facing: EnumFacing.VALUES) {
			final TileEntity mobFarm = this.world.getTileEntity(this.pos.offset(facing));
			if (mobFarm != null) {
				final IItemHandler invCap = mobFarm.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
				if (invCap == null) return EmptyHandler.INSTANCE;
				return invCap;
			}
		}
		return null;
	}
	
	private void updateModel() {
		if (this.world.isRemote) {
			if (this.getLasso().isEmpty()) {
				this.model = null;
			} else {
				NBTTagCompound nbt = NBTHelper.getBaseTag(this.getLasso());
				String mobName = nbt.getString(NBTHelper.MOB_NAME);
				if (this.model == null || !this.model.getName().equals(mobName)) {
					NBTTagCompound entityData = nbt.getCompoundTag(NBTHelper.MOB_DATA);
					Entity newModel = EntityList.createEntityFromNBT(entityData, this.world);
					
					if (newModel != null && newModel instanceof EntityLiving) {
						this.model = (EntityLiving) newModel;
						this.modelFacing = this.world.getBlockState(this.pos).getValue(BlockMobFarm.FACING);
					}
				}
			}
		}
	}
	
	public boolean isWorking() {
		if (this.mobFarmData == null || this.getLasso().isEmpty() || this.isPowered()) return false;
		return this.mobFarmData.isLassoValid(this.getLasso());
	}
	
	public void updateRedstone() {
		this.powered = this.world.isBlockPowered(this.pos);
	}
	
	public ItemStack getLasso() {
		return this.inventory.getStackInSlot(0);
	}
	
	public void setMobFarmData(EnumMobFarm mobFarmData) {
		this.mobFarmData = mobFarmData;
	}
	
	public boolean isPowered() {
		return this.powered;
	}
	
	@Deprecated
	public ItemStackHandler getInventory() {
		return this.inventory;
	}
	
	public double getScaledProgress() {
		if (this.mobFarmData == null) return 0;
		return this.currProgress / (double) this.mobFarmData.getMaxProgress();
	}
	
	public EntityLiving getModel() {
		return this.model;
	}
	
	public EnumFacing getModelFacing() {
		return this.modelFacing;
	}
	
	public String getUnlocalizedName() {
		if (this.mobFarmData == null) return "block." + Reference.MOD_ID + ".default_mob_farm";
		return this.mobFarmData.getUnlocalizedName();
	}
	
	public void saveAndSync() {
		IBlockState state = this.world.getBlockState(this.pos);
		this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
		this.world.notifyBlockUpdate(pos, state, state, 3);
		this.markDirty();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.mobFarmData = EnumMobFarm.values()[nbt.getInteger(NBTHelper.MOB_FARM_DATA)];
		this.currProgress = nbt.getInteger(NBTHelper.CURR_PROGRESS);
		this.inventory.deserializeNBT(nbt.getCompoundTag(NBTHelper.INVENTORY));
		this.shouldUpdate = true;
		
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (this.mobFarmData == null) return nbt;
		nbt.setInteger(NBTHelper.MOB_FARM_DATA, this.mobFarmData.ordinal());
		nbt.setInteger(NBTHelper.CURR_PROGRESS, this.currProgress);
		nbt.setTag(NBTHelper.INVENTORY, this.inventory.serializeNBT());
		return super.writeToNBT(nbt);
	}
	
	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(this.getPos(), 0, this.getUpdateTag());
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		this.readFromNBT(packet.getNbtCompound());
	}
	
	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}
	
	@Override
	public void handleUpdateTag(NBTTagCompound nbt) {
		this.readFromNBT(nbt);
	}

}
