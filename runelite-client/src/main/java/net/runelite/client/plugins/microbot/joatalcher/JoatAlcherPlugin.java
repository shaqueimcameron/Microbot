package net.runelite.client.plugins.microbot.joatalcher;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "JOAT Alcher Plugin",
        description = "Alchmate plugin!",
        tags = {"performance", "microbot", "test", "gameobject"},
        enabledByDefault = false
)
@Slf4j
public class JoatAlcherPlugin extends Plugin {
    @Inject
    JoatAlcherScript exampleScript;
    @Inject
    JoatAlcherOverlay exampleScriptOverlay;
    @Inject
    OverlayManager overlayManager;
    ConfigManager configManager;


    @Override
    protected void startUp() throws AWTException {
//        overlayManager.add(exampleScriptOverlay);
        exampleScript.run();

    }

    @Override
    protected void shutDown() {
//        overlayManager.remove(exampleScriptOverlay);


        exampleScript.shutdown();
    }

    // on settings change
    @Subscribe
    public void onConfigChanged(final ConfigChanged event) {
    }
}