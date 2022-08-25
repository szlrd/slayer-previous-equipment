package com.slayerprevious;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
TODO
- tasks with switches (e.g. Demonic Gorillas)
- highlight items in bank (and/or in inventory while bank interface is up)

IDEAS
- bank tab
- cannon
- clear all
 */
@Slf4j
@PluginDescriptor(
        name = "Slayer Previous Equipment"
)
@PluginDependency(SlayerPlugin.class)
public class SlayerPreviousPlugin extends Plugin {
    public static final String CONFIG_GROUP = "slayerprevious";
    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private SlayerPreviousConfig config;

    @Inject
    private SlayerPluginService slayerPluginService;

    private final Set<NPC> taggedNpcs = new HashSet<>();

    @Override
    protected void startUp() throws Exception {
        log.info("Slayer previous equipment started!");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Slayer previous equipment stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        Actor actor = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.getHitsplatType() == HitsplatID.DAMAGE_ME && slayerPluginService.getTargets().contains(actor)) {
            NPC npcActor = (NPC) actor;
            ItemContainer equipmentItemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
            if (equipmentItemContainer != null) {
                List<Item> equippedItems = Arrays.asList(equipmentItemContainer.getItems());
                Set<Integer> equippedItemIds = equippedItems.stream()
                        .map(Item::getId)
                        .filter(id -> id != -1)
                        .collect(Collectors.toSet());

                Set<Integer> savedItemIds = getItemListFromConfiguration(npcActor);

                if (equippedItemIds.size() > 0 && !equippedItemIds.equals(savedItemIds)) {
                    log.info("Unsaved items, saving items: " + StringUtils.join(savedItemIds, ','));
                    configManager.setConfiguration(CONFIG_GROUP, "NPC_" + npcActor.getName(), StringUtils.join(equippedItemIds, ","));
                } else if (equippedItemIds.size() == 0) {
                    log.info("No items to save.");
                } else {
                    log.info("Items already saved.");
                }
            }
        }
    }

    private Set<Integer> getItemListFromConfiguration(NPC npc) {
        return Arrays.stream(
                configManager.getConfiguration(CONFIG_GROUP, "NPC_" + npc.getName())
                        .split(","))
                .map((Integer::parseInt))
                .collect(Collectors.toSet());
    }

//	@Subscribe
//	public void onActorDeath(ActorDeath actorDeath)
//	{
//		Actor actor = actorDeath.getActor();
//		if (taggedNpcs.contains(actor))
//		{
//			NPC npcActor = (NPC) actor;
//			configManager.setConfiguration(CONFIG_GROUP, "NPC_" + npcActor.getName(), "test");
//			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Slayer Previous", "Killed a task monster.", "Slayer Previous2");
//		}
//	}

    @Provides
    SlayerPreviousConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SlayerPreviousConfig.class);
    }
}
