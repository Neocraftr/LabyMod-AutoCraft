package io.github.kyzderp.autocraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import net.minecraft.client.Minecraft;

public class CraftSettings 
{
	private final int defaultCooldown = 2;
	private int maxClickCooldown;
	
	private final File dirs = new File(Minecraft.getMinecraft().mcDataDir, "config" + File.separator + "AutoCraft");
	private final File path = new File(dirs.getPath() + File.separator + "autocraftconfig.txt");

	
	public CraftSettings()
	{
		this.maxClickCooldown = this.defaultCooldown;
		
		if (!path.exists())
		{
			this.dirs.mkdirs();
			if (!this.writeFile())
				System.out.println("[AutoCraft] Cannot write to file!");
			else
				System.out.println("[AutoCraft] Created new configuration file.");
		}
		if (!this.loadFile())
			System.out.println("[AutoCraft] Cannot read from file!");
		else
			System.out.println("[AutoCraft] Configuration loaded.");
	}
	
	private boolean writeFile()
	{
		PrintWriter writer;
		try {
			writer = new PrintWriter(path);
		} catch (FileNotFoundException e) {
			return false;
		}
		writer.println(this.maxClickCooldown);
		writer.close();
		return true;
	}

	private boolean loadFile()
	{
		if (!path.exists())
			return false;
		Scanner scan;
		try {
			scan = new Scanner(path);
		} catch (FileNotFoundException e) {
			return false;
		}
		if (scan.hasNext())
		{
			String line = scan.nextLine();
			try {
				this.maxClickCooldown = Integer.parseInt(line);
			} catch (NumberFormatException e) {
				this.maxClickCooldown = this.defaultCooldown;
				this.writeFile();
			}
		}
		scan.close();
		return true;
	}
	
	public int getMaxClickCooldown()
	{
		this.loadFile();
		return this.maxClickCooldown;
	}
	
	public void setMaxClickCooldown(int cd)
	{
		this.maxClickCooldown = cd;
		this.writeFile();
	}
}
