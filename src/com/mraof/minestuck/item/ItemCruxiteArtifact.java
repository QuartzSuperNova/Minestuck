package com.mraof.minestuck.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.network.skaianet.SkaianetHandler;
import com.mraof.minestuck.util.Debug;
import com.mraof.minestuck.util.ITeleporter;
import com.mraof.minestuck.util.MinestuckAchievementHandler;
import com.mraof.minestuck.util.Teleport;
import com.mraof.minestuck.world.gen.ChunkProviderLands;
import com.mraof.minestuck.world.gen.lands.LandHelper;
import com.mraof.minestuck.world.storage.MinestuckSaveHandler;

public class ItemCruxiteArtifact extends ItemFood implements ITeleporter
{
	
	public ItemCruxiteArtifact(int par2, boolean par3) 
	{
		super(1, par2, par3);
		this.setCreativeTab(Minestuck.tabMinestuck);
		setUnlocalizedName("cruxiteArtifact");
	}
	@Override
	public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
	{
		par3EntityPlayer.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack));

		return par1ItemStack;
	}
	@Override
	protected void onFoodEaten(ItemStack par1ItemStack, World par2World, EntityPlayer player) {
		if(!par2World.isRemote && player.worldObj.provider.dimensionId != -1) {
			
			int destinationId = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getInteger("LandId");
			if(destinationId == 0 || !MinestuckSaveHandler.lands.contains((byte) destinationId))
				destinationId = LandHelper.createLand(player);
			
			if(player.worldObj.provider.dimensionId != destinationId) {
				player.triggerAchievement(MinestuckAchievementHandler.enterMedium);
				Teleport.teleportEntity(player, destinationId, this);
				SkaianetHandler.enterMedium((EntityPlayerMP)player, destinationId);
			}
		}
	}
	public void makeDestination(Entity entity, WorldServer worldserver0, WorldServer worldserver1)
	{
		if(entity instanceof EntityPlayerMP && entity.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getInteger("LandId") == worldserver1.provider.dimensionId)
		{
			int x = (int) entity.posX;
			int y = (int) entity.posY;
			int z = (int) entity.posZ;
			
			for(int chunkX = (x - Minestuck.artifactRange) >> 4; chunkX <= (x + Minestuck.artifactRange) >> 4; chunkX++)	//Prevent anything to generate on the piece that we move
				for(int chunkZ = (z - Minestuck.artifactRange) >> 4; chunkZ <=(z + Minestuck.artifactRange) >> 4; chunkZ++)	//from the overworld.
					worldserver1.theChunkProviderServer.loadChunk(chunkX, chunkZ);
			
			List<?> list = entity.worldObj.getEntitiesWithinAABBExcludingEntity(entity, entity.boundingBox.expand((double)Minestuck.artifactRange, Minestuck.artifactRange, (double)Minestuck.artifactRange));
			Iterator<?> iterator = list.iterator();
			
			while (iterator.hasNext())
			{
				Entity e = (Entity)iterator.next();
				if(Minestuck.entryCrater || e instanceof EntityPlayer || e instanceof EntityItem)
					Teleport.teleportEntity(e, worldserver1.provider.dimensionId, this);
				else	//Copy instead of teleport
				{
					Entity newEntity = EntityList.createEntityByName(EntityList.getEntityString(entity), worldserver1);
					if (newEntity != null)
					{
						newEntity.copyDataFrom(entity, true);
						newEntity.dimension = worldserver1.provider.dimensionId;
						worldserver1.spawnEntityInWorld(newEntity);
					}
				}
			}
			int nextWidth = 0;
			for(int blockX = x - Minestuck.artifactRange; blockX <= x + Minestuck.artifactRange; blockX++)
			{
				int zWidth = nextWidth;
				nextWidth = (int) Math.sqrt(Minestuck.artifactRange * Minestuck.artifactRange - (blockX - x + 1) * (blockX - x + 1));
				for(int blockZ = z - zWidth; blockZ <= z + zWidth; blockZ++)
				{
					double radius = Math.sqrt(((blockX - x) * (blockX - x) + (blockZ - z) * (blockZ - z)) / 2);
					int minY =  y - (int) (Math.sqrt(Minestuck.artifactRange * Minestuck.artifactRange - radius * radius));
					minY = minY < 0 ? 0 : minY;
					for(int blockY = minY; blockY < 256; blockY++)
					{
						Block block = worldserver0.getBlock(blockX, blockY, blockZ);
						int metadata = worldserver0.getBlockMetadata(blockX, blockY, blockZ);
						TileEntity te = worldserver0.getTileEntity(blockX, blockY, blockZ);
						if(block != Blocks.air && blockX < x + Minestuck.artifactRange && blockZ < z + nextWidth && blockZ > z - nextWidth)
							worldserver1.setBlock(blockX + 1, blockY, blockZ, Blocks.dirt, 0, 0);
						if(block != Blocks.air && blockZ < z + zWidth)
							worldserver1.setBlock(blockX, blockY, blockZ + 1, Blocks.stone, 0, 0);
						if(block != Blocks.bedrock)
							worldserver1.setBlock(blockX, blockY, blockZ, block, metadata, 2);
						if((te) != null)
						{
							TileEntity te1 = null;
							try {
								te1 = te.getClass().newInstance();
							} catch (Exception e) {e.printStackTrace();	continue;}
							NBTTagCompound nbt = new NBTTagCompound();
							te.writeToNBT(nbt);
							te1.readFromNBT(nbt);
							te1.yCoord++;//prevents TileEntity from being invalidated
							worldserver1.setTileEntity(blockX, blockY, blockZ, te1);
						};
					}
				}
			}
			
			for(int blockX = x - Minestuck.artifactRange; blockX <= x + Minestuck.artifactRange; blockX++)
			{
				int zWidth = nextWidth;
				nextWidth = (int) Math.sqrt(Minestuck.artifactRange * Minestuck.artifactRange - (blockX - x + 1) * (blockX - x + 1));
				for(int blockZ = z - zWidth; blockZ <= z + zWidth; blockZ++)
				{
					double radius = Math.sqrt(((blockX - x) * (blockX - x) + (blockZ - z) * (blockZ - z)) / 2);
					int minY =  y - (int) (Math.sqrt(Minestuck.artifactRange * Minestuck.artifactRange - radius*radius));
					minY = minY < 0 ? 0 : minY;
					for(int blockY = minY; blockY < 256; blockY++)
					{
						if(Minestuck.entryCrater)
						{
							if(worldserver0.getBlock(blockX, blockY, blockZ) != Blocks.bedrock)
								worldserver0.setBlockToAir(blockX, blockY, blockZ);
						} else
							if(worldserver0.getTileEntity(blockX, blockY, blockZ) != null)
								worldserver0.setBlockToAir(blockX, blockY, blockZ);
					}
				}
			}
			
			if(Minestuck.entryCrater)
			{
				list = entity.worldObj.getEntitiesWithinAABBExcludingEntity(entity, entity.boundingBox.expand((double)Minestuck.artifactRange, Minestuck.artifactRange, (double)Minestuck.artifactRange));
				iterator = list.iterator();
				
				while (iterator.hasNext())
				{
					((Entity)iterator.next()).setDead();
				}
			}
			
			ChunkProviderLands chunkProvider = (ChunkProviderLands) worldserver1.provider.createChunkGenerator();
			chunkProvider.spawnX = x;
			chunkProvider.spawnY = y;
			chunkProvider.spawnZ = z;
			chunkProvider.saveData();
			
			Debug.printf("Respawn location being set to: %d, %d, %d", x, y, z);
		}
	}
	@Override
	public void registerIcons(IIconRegister iconRegister) 
	{
		this.itemIcon = iconRegister.registerIcon("minestuck:CruxiteApple");
	}

}
