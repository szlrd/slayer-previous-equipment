package com.slayerprevious;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SlayerPreviousTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SlayerPreviousPlugin.class);
		RuneLite.main(args);
	}
}