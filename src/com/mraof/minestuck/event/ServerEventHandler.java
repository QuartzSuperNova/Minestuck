package com.mraof.minestuck.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.entity.underling.EntityUnderling;
import com.mraof.minestuck.network.skaianet.SkaianetHandler;
import com.mraof.minestuck.util.Echeladder;
import com.mraof.minestuck.util.MinestuckPlayerData;
import com.mraof.minestuck.util.PostEntryTask;

import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ServerEventHandler
{
	
	public static final ServerEventHandler instance = new ServerEventHandler();
	
	public static long lastDay;
	
	public static List<PostEntryTask> tickTasks = new ArrayList<PostEntryTask>();
	
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event)
	{
		if(event.phase == TickEvent.Phase.END)
		{
			
			if(!MinestuckConfig.hardMode && event.world.provider.getDimensionId() == 0)
			{
				long time = event.world.getWorldTime() / 24000L;
				if(time != lastDay)
				{
					lastDay = time;
					SkaianetHandler.resetGivenItems();
				}
			}
			
			Iterator<PostEntryTask> iter = tickTasks.iterator();
			while(iter.hasNext())
				if(iter.next().onTick())
					iter.remove();
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=false)
	public void onEntityDeath(LivingDeathEvent event)
	{
		if(event.entity instanceof IMob && event.source.getEntity() instanceof EntityPlayerMP)
		{
			EntityPlayerMP player = (EntityPlayerMP) event.source.getEntity();
			int exp = 0;
			if(event.entity instanceof EntityZombie || event.entity instanceof EntitySkeleton)
				exp = 6;
			else if(event.entity instanceof EntityCreeper || event.entity instanceof EntitySpider || event.entity instanceof EntitySilverfish)
				exp = 5;
			else if(event.entity instanceof EntityEnderman || event.entity instanceof EntityBlaze || event.entity instanceof EntityWitch || event.entity instanceof EntityGuardian)
				exp = 12;
			else if(event.entity instanceof EntitySlime)
				exp = ((EntitySlime) event.entity).getSlimeSize() - 1;
			
			if(exp > 0)
				Echeladder.increaseProgress(player, exp);
		}
	}
	
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=false)
	public void onEntityAttack(LivingHurtEvent event)
	{
		if(event.source.getEntity() != null)
			if(event.entityLiving instanceof EntityUnderling && event.source.getEntity() instanceof EntityPlayerMP)
			{	//Increase damage to underling
				EntityPlayerMP player = (EntityPlayerMP) event.source.getEntity();
				double modifier = MinestuckPlayerData.getData(player).echeladder.getUnderlingDamageModifier();
				event.ammount *= modifier;
				
			} else if(event.entityLiving instanceof EntityPlayerMP && event.source.getEntity() instanceof EntityUnderling)
			{	//Decrease damamge to player
				EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
				double modifier = MinestuckPlayerData.getData(player).echeladder.getUnderlingProtectionModifier();
				event.ammount *= modifier;
			}
	}
}