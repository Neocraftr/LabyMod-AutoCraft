package io.github.kyzderp.autocraft;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.ServerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class AutoCraft extends LabyModAddon {

	public static final String VERSION = "1.4.0";
	private static AutoCraft autoCraft;

	private AutoInventory autoInv;
	private AutoWorkbench autoBench;
	private CraftSettings settings;
	
	private int msgcooldown;
	private String message;
	private boolean isError;
	private LinkedList<Click> clickQueue;
	private int currClickCooldown;
	private int maxClickCooldown;

	@Override
	public void onEnable() {
		autoCraft = this;

		this.settings = new CraftSettings();
		this.maxClickCooldown = this.settings.getMaxClickCooldown();
		this.currClickCooldown = 0;
		this.msgcooldown = 40;
		this.clickQueue = new LinkedList<Click>();

		getApi().registerForgeListener(this);
		getApi().getEventManager().register(new MessageSendEvent() {
			@Override
			public boolean onSend(String msg) {
				return AutoCraft.this.onSendChatMessage(msg);
			}
		});
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
	public void loadConfig() {}

	@Override
	protected void fillSettings(List<SettingsElement> list) {}

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent e) {
		if(e.phase == TickEvent.Phase.START) {
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

				this.currClickCooldown = this.maxClickCooldown;
				// For right clicks to not mess up
				if (this.maxClickCooldown < 2 && currClick.getData() == 1
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

	private boolean onSendChatMessage(String message) {
		String[] tokens = message.split(" ");
		if (tokens[0].equalsIgnoreCase("/autocraft"))
		{
			if (tokens.length == 1)
			{
				AutoCraft.logMessage("\u00A72AutoCraft \u00A78[\u00A72v" + VERSION + "\u00A78] \u00A7aby Kyzeragon", false);
				AutoCraft.logMessage("Type \u00A72/autocraft help \u00A7afor commands.", false);
			}
			else if (tokens[1].equalsIgnoreCase("delay"))
			{
				if (tokens.length == 2)
					AutoCraft.logMessage("Current crafting delay is " + this.maxClickCooldown, true);
				else if (!tokens[2].matches("[0-9]+"))
					AutoCraft.logError("Must be an integer. Recommended 0~4");
				else
				{
					this.maxClickCooldown = Integer.parseInt(tokens[2]);
					this.settings.setMaxClickCooldown(this.maxClickCooldown);
					AutoCraft.logMessage("Crafting delay set to " + this.maxClickCooldown, true);
				}
			}
			else if (tokens[1].equalsIgnoreCase("help"))
			{
				String[] commands = {"delay <number> - Sets the delay (in ticks) for craft clicking",
						"help - Displays this help message"};
				AutoCraft.logMessage("\u00A72AutoCraft \u00A78[\u00A72v" + VERSION + "\u00A78] \u00A7acommands:", false);
				for (String command: commands)
					AutoCraft.logMessage("/autocraft " + command, false);
			}
			else
			{
				AutoCraft.logMessage("\u00A72AutoCraft \u00A78[\u00A72v" + VERSION + "\u00A78] \u00A7aby Kyzeragon", false);
				AutoCraft.logMessage("Type \u00A72/autocraft help \u00A7afor commands.", false);
			}
			return true;
		}
		return false;
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
	
	/**
	 * Queues a click to be run later
	 * @param queue The click queue
	 */
	public void queueClicks(LinkedList<Click> queue)
	{
		this.clickQueue.addAll(queue);
	}

	/**
	 * Logs the message to the user
	 * @param message The message to log
	 * @param addPrefix Whether to add the mod-specific prefix or not
	 */
	public static void logMessage(String message, boolean addPrefix)
	{// "\u00A78[\u00A72\u00A78] \u00A7a"
		if (addPrefix)
			message = "\u00A78[\u00A72AutoCraft\u00A78] \u00A7a" + message;
		ChatComponentText displayMessage = new ChatComponentText(message);
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.GREEN));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Logs the error message to the user
	 * @param message The error message to log
	 */
	public static void logError(String message) {
		ChatComponentText displayMessage = new ChatComponentText("\u00A78[\u00A74!\u00A78] \u00A7c" + message + " \u00A78[\u00A74!\u00A78]");
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.RED));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	public static AutoCraft getAutoCraft() {
		return autoCraft;
	}
}
