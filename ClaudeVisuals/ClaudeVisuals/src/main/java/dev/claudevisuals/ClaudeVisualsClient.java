package dev.claudevisuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClaudeVisualsClient implements ClientModInitializer {
    public static final String ID = "claudevisuals";
    public static KeyMapping menuKey, zoomKey, nvKey, sprintKey, hudKey;
    public static boolean hudHidden;

    private static KeyMapping key(String name, int code, KeyMapping.Category cat) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key." + ID + "." + name, InputConstants.Type.KEYSYM, code, cat));
    }

    @Override
    public void onInitializeClient() {
        Mods.load();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        menuKey = key("menu", InputConstants.KEY_RIGHT_SHIFT, cat);
        zoomKey = key("zoom", InputConstants.KEY_C, cat);
        nvKey = key("nightvision", InputConstants.KEY_N, cat);
        sprintKey = key("sprint", InputConstants.KEY_V, cat);
        hudKey = key("hud", InputConstants.KEY_H, cat);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Feat.tick(mc);
            while (menuKey.consumeClick()) if (mc.screen == null) mc.setScreen(new MenuScreen(null));
            while (nvKey.consumeClick()) toggle(mc, "nv", "Ночное зрение");
            while (sprintKey.consumeClick()) toggle(mc, "sprint", "Авто-спринт");
            while (hudKey.consumeClick()) hudHidden = !hudHidden;
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(ID, "hud"), Hud::render);
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, orig -> (g, dt) -> Hud.crosshairElement(orig, g, dt));
        HudElementRegistry.replaceElement(VanillaHudElements.PLAYER_LIST, orig -> (g, dt) -> Hud.tabElement(orig, g, dt));

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) TitleFx.attach(client, screen);
        });
    }

    private static void toggle(Minecraft mc, String id, String name) {
        Mods.Mod m = Mods.get(id);
        m.on = !m.on;
        Mods.save();
        mc.gui.setOverlayMessage(Component.literal(name + ": " + (m.on ? "ВКЛ" : "ВЫКЛ")), false);
    }
}
