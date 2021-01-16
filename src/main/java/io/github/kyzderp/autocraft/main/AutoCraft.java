package io.github.kyzderp.autocraft.main;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.github.kyzderp.autocraft.settings.Settings;
import io.github.kyzderp.autocraft.utils.AutoInventory;
import io.github.kyzderp.autocraft.utils.AutoWorkbench;
import io.github.kyzderp.autocraft.utils.Click;
import io.github.kyzderp.autocraft.utils.Updater;
import net.labymod.addon.AddonLoader;
import net.labymod.api.LabyModAddon;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.ServerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class AutoCraft extends LabyModAddon {

	public static final String VERSION = "2.0.0";
	private static AutoCraft autoCraft;

	private AutoInventory autoInv;
	private AutoWorkbench autoBench;
	private Settings settings;
	private Updater updater;
	
	private int msgcooldown;
	private String message;
	private boolean isError;
	private LinkedList<Click> clickQueue;
	private int currClickCooldown;

	@Override
	public void onEnable() {
		autoCraft = this;

		this.settings = new Settings();
		this.updater = new Updater();
		this.currClickCooldown = 0;
		this.msgcooldown = 40;
		this.clickQueue = new LinkedList<Click>();

		getApi().registerForgeListener(this);
		getApi().getEventManager().registerOnJoin(new Consumer<ServerData>() {
			@Override
			public void accept(ServerData serverData) {
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						AutoCraft.this.autoInv = new AutoInventory(AutoCraft.this);
						AutoCraft.this.autoBench = new AutoWorkbench(AutoCraft.this);
					}
				}, 1000);
			}
		});
	}

	@Override
	public void loadConfig() {
		this.settings.loadConfig();
		this.updater.setAddonJar(AddonLoader.getFiles().get(about.uuid));
	}

	@Override
	protected void fillSettings(List<SettingsElement> settings) {
		this.settings.fillSettings(settings);
	}

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent e) {
		if(e.phase == TickEvent.Phase.END) {
			if (!getApi().isIngame())
				return;

			if (this.msgcooldown > 0) {
				this.msgcooldown--;
				this.displayMessage(this.message, this.isError);
			}

			while (!this.clickQueue.isEmpty() && this.currClickCooldown == 0) {
				Click currClick = this.clickQueue.pop();
			
			/*String thing = "";
			if (currClick.getAction() == 0)
			{
				if (currClick.getData() == 0)
					thing = "left Click";
				else
					thing = "right click";
			}*/

				Minecraft.getMinecraft().playerController.windowClick(currClick.getWindowID(),
						currClick.getSlot(), currClick.getData(), currClick.getAction(),
						Minecraft.getMinecraft().thePlayer);

				this.currClickCooldown = this.settings.getClickCooldown();
				// For right clicks to not mess up
				if (this.settings.getClickCooldown() < 2 && currClick.getData() == 1
						&& currClick.getAction() == 0)
					this.currClickCooldown = 2;
			}
			if (this.currClickCooldown > 0)
				this.currClickCooldown--;
			if (Minecraft.getMinecraft().thePlayer.openContainer != null
					&& Minecraft.getMinecraft().currentScreen instanceof GuiInventory) {
				if (Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
						this.autoInv.storeCrafting();
					else if (this.clickQueue.isEmpty())
						this.autoInv.craft();
				}
			} else if (Minecraft.getMinecraft().thePlayer.openContainer != null
					&& Minecraft.getMinecraft().currentScreen instanceof GuiCrafting) {
				if (Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
					if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
						this.autoBench.storeCrafting();
					else if (this.clickQueue.isEmpty())
						this.autoBench.craft();
				}
			}
			// TODO: delay?
			// TODO: auto-combine stuff still needs better algorithm
		}
	}

	// Init in singleplayer
	@SubscribeEvent
	public void onJoinGame(EntityJoinWorldEvent e) {
		if(!e.world.isRemote && e.entity instanceof EntityPlayer) {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					AutoCraft.this.autoInv = new AutoInventory(AutoCraft.this);
					AutoCraft.this.autoBench = new AutoWorkbench(AutoCraft.this);
				}
			}, 1000);
		}
	}

	public void message(String message, boolean isError)
	{
		this.message = message;
		this.isError = isError;
		this.msgcooldown = 40;
	}

	private void displayMessage(String message, boolean isError)
	{
		int color = 0xFF5555;
		if (!isError)
			color = 0x55FF55;
		FontRenderer fontRender = Minecraft.getMinecraft().fontRendererObj;
		fontRender.drawStringWithShadow(message, 
				Minecraft.getMinecraft().displayWidth/4 - fontRender.getStringWidth(message)/2,
				Minecraft.getMinecraft().displayHeight/4 - 100, color);
	}

	public void queueClicks(LinkedList<Click> queue)
	{
		this.clickQueue.addAll(queue);
	}

	public static AutoCraft getAutoCraft() {
		return autoCraft;
	}

	public Settings getSettings() {
		return settings;
	}

	public Updater getUpdater() {
		return updater;
	}
}
