package com.slayerprevious;

import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import javax.inject.Inject;
import java.awt.*;

public class SlayerPreviousOverlay extends WidgetItemOverlay {

    @Inject
    private SlayerPreviousPlugin slayerPreviousPlugin;

    public SlayerPreviousOverlay() {
        showOnBank();
        showOnInventory();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem) {
        if (slayerPreviousPlugin.getCurrentTaskSavedItems().stream().anyMatch(integer -> integer == itemId)) {
            Rectangle bounds = widgetItem.getCanvasBounds();
            graphics.draw(bounds);
        }
    }
}
