package io.github.kyzderp.autocraft;

import com.google.gson.JsonObject;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.NumberElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Material;

import java.util.List;

public class Settings {
    private TextElement infoText;
    private boolean autoUpdateAddon = true;
    private int clickCooldown = 10;

    public void loadConfig() {
        if(getConfig().has("autoUpdateAddon")) {
            autoUpdateAddon = getConfig().get("autoUpdateAddon").getAsBoolean();
        }
        if(getConfig().has("clickCooldown")) {
            clickCooldown = getConfig().get("clickCooldown").getAsInt();
        }
    }

    public void fillSettings(List<SettingsElement> settings) {
        BooleanElement autoUpdateAddonBtn = new BooleanElement("Addon beim start aktualisieren", new ControlElement.IconData("labymod/textures/settings/settings/serverlistliveview.png"), value -> {
            autoUpdateAddon = value;
            updateInfoText();
            getConfig().addProperty("autoUpdateAddon", value);
            saveConfig();
        }, autoUpdateAddon);
        settings.add(autoUpdateAddonBtn);

        NumberElement clickCooldownSetting = new NumberElement("Click Cooldown", new ControlElement.IconData(Material.WATCH), clickCooldown);
        clickCooldownSetting.setMinValue(1);
        clickCooldownSetting.setMaxValue(20);
        clickCooldownSetting.addCallback(value -> {
            clickCooldown = value;
            getConfig().addProperty("clickCooldown", value);
            saveConfig();
        });
        settings.add(clickCooldownSetting);

        infoText = new TextElement("");
        updateInfoText();
        settings.add(infoText);
    }

    private JsonObject getConfig() {
        return AutoCraft.getAutoCraft().getConfig();
    }
    private void saveConfig() {
        AutoCraft.getAutoCraft().saveConfig();
    }

    private void updateInfoText() {
        String text = "§7GitHub: §ahttps://github.com/Neocraftr/AutoCraftMod/\n";
        text += "§7Version: §a"+AutoCraft.VERSION;
        if(AutoCraft.getAutoCraft().getUpdater().isUpdatePending()) {
            text += " §c(Update ausstehend. Neustart erforderlich)";
        } else if(AutoCraft.getAutoCraft().getUpdater().isUpdateAvailable()) {
            text += " §c(Update verfügbar)";
        }
        infoText.setText(text);
    }

    public boolean isAutoUpdateAddon() {
        return autoUpdateAddon;
    }

    public int getClickCooldown() {
        return clickCooldown;
    }
}
