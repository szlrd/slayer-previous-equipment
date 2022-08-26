package com.slayerprevious;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/*
TODO
- tasks with switches (e.g. Demonic Gorillas)
- better way of overlay toggling (perhaps a bank tab?)

IDEAS
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
    private final static Set<Integer> ALL_SLAYER_ITEMS = ImmutableSet.of(
            ItemID.SLAYER_HELMET,
            ItemID.SLAYER_HELMET_I,
            ItemID.SLAYER_HELMET_I_25177,
            ItemID.BLACK_SLAYER_HELMET,
            ItemID.BLACK_SLAYER_HELMET_I,
            ItemID.BLACK_SLAYER_HELMET_I_25179,
            ItemID.GREEN_SLAYER_HELMET,
            ItemID.GREEN_SLAYER_HELMET_I,
            ItemID.GREEN_SLAYER_HELMET_I_25181,
            ItemID.PURPLE_SLAYER_HELMET,
            ItemID.PURPLE_SLAYER_HELMET_I,
            ItemID.PURPLE_SLAYER_HELMET_I_25185,
            ItemID.RED_SLAYER_HELMET,
            ItemID.RED_SLAYER_HELMET_I,
            ItemID.RED_SLAYER_HELMET_I_25183,
            ItemID.TURQUOISE_SLAYER_HELMET,
            ItemID.TURQUOISE_SLAYER_HELMET_I,
            ItemID.TURQUOISE_SLAYER_HELMET_I_25187,
            ItemID.TWISTED_SLAYER_HELMET,
            ItemID.TWISTED_SLAYER_HELMET_I,
            ItemID.TWISTED_SLAYER_HELMET_I_25191,
            ItemID.HYDRA_SLAYER_HELMET,
            ItemID.HYDRA_SLAYER_HELMET_I,
            ItemID.HYDRA_SLAYER_HELMET_I_25189,
            ItemID.TZTOK_SLAYER_HELMET,
            ItemID.TZTOK_SLAYER_HELMET_I,
            ItemID.TZTOK_SLAYER_HELMET_I_25902,
            ItemID.VAMPYRIC_SLAYER_HELMET,
            ItemID.VAMPYRIC_SLAYER_HELMET_I,
            ItemID.VAMPYRIC_SLAYER_HELMET_I_25908,
            ItemID.TZKAL_SLAYER_HELMET,
            ItemID.TZKAL_SLAYER_HELMET_I,
            ItemID.TZKAL_SLAYER_HELMET_I_25914
            );

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SlayerPreviousOverlay slayerPreviousOverlay;

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private SlayerPreviousConfig config;

    @Inject
    private SlayerPluginService slayerPluginService;

    private boolean isBankOpen;

    @Override
    protected void startUp() throws Exception {
        log.info("Slayer previous equipment started!");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Slayer previous equipment stopped!");
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == WidgetID.BANK_GROUP_ID) {
            isBankOpen = true;

            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
            if (inventory != null && itemContainerContainsSlayerHelm(inventory)) {
                overlayManager.add(slayerPreviousOverlay);
            }
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed widgetClosed) {
        if (widgetClosed.getGroupId() == WidgetID.BANK_GROUP_ID) {
            isBankOpen = false;

            overlayManager.remove(slayerPreviousOverlay);
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
        if (isBankOpen && itemContainerChanged.getContainerId() == InventoryID.INVENTORY.getId()) {
            if (itemContainerContainsSlayerHelm(itemContainerChanged.getItemContainer())) {
                overlayManager.add(slayerPreviousOverlay);
            } else {
                overlayManager.remove(slayerPreviousOverlay);
            }
        }
    }

    private boolean itemContainerContainsSlayerHelm(ItemContainer itemContainer) {
        return Arrays.stream(itemContainer.getItems()).anyMatch(item -> ALL_SLAYER_ITEMS.contains(item.getId()));
    }


    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        Actor actor = hitsplatApplied.getActor();
        Hitsplat hitsplat = hitsplatApplied.getHitsplat();
        if (hitsplat.getHitsplatType() == HitsplatID.DAMAGE_ME && slayerPluginService.getTargets().contains(actor)) {
            NPC npcActor = (NPC) actor;
            ItemContainer equipmentItemContainer = client.getItemContainer(InventoryID.EQUIPMENT);
            if (equipmentItemContainer != null) {
                Set<Integer> equippedItemIds = Arrays.stream(equipmentItemContainer.getItems())
                        .map(Item::getId)
                        .filter(id -> id != -1)
                        .collect(Collectors.toSet());

                Set<Integer> savedItemIds = getCurrentTaskSavedItems();

                if (equippedItemIds.size() > 0 && !equippedItemIds.equals(savedItemIds)) {
                    log.info("Unsaved items, saving items: " + StringUtils.join(savedItemIds, ','));
                    configManager.setConfiguration(CONFIG_GROUP, "NPC_" + npcActor.getName(), StringUtils.join(equippedItemIds, ","));
                }
            }
        }
    }

    protected Set<Integer> getCurrentTaskSavedItems() {
        String configuration = configManager.getConfiguration(CONFIG_GROUP, "NPC_" + slayerPluginService.getTask());
        if (configuration == null || StringUtils.isBlank(configuration)) {
            return Collections.emptySet();
        }
        return Arrays.stream(
                        configManager.getConfiguration(CONFIG_GROUP, "NPC_" + slayerPluginService.getTask())
                                .split(","))
                .map((Integer::parseInt))
                .collect(Collectors.toSet());
    }

    @Provides
    SlayerPreviousConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SlayerPreviousConfig.class);
    }
}
