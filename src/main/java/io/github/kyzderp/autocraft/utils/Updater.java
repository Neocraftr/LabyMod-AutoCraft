package io.github.kyzderp.autocraft.utils;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.kyzderp.autocraft.main.AutoCraft;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Updater {

    private static final String UPDATE_URL = "https://api.github.com/repos/Neocraftr/LabyMod-AutoCraft/releases/latest";

    private boolean updateAvailable = false;
    private String downloadUrl = null;
    private String latestVersion = null;
    private File addonJar = null;

    public Updater() {
        checkForUpdates();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(updateAvailable && AutoCraft.getAutoCraft().getSettings().isAutoUpdateAddon()) {
                update();
            }
        }));
    }

    private void checkForUpdates() {
        try {
            BufferedReader reader = Resources.asCharSource(new URL(UPDATE_URL), StandardCharsets.UTF_8).openBufferedStream();
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            if(json.has("tag_name") && json.has("assets")) {
                latestVersion = json.get("tag_name").getAsString().replace("v", "");
                if(!AutoCraft.VERSION.equals(latestVersion)) {
                    JsonArray assets = json.get("assets").getAsJsonArray();
                    if(assets.size() > 0)  {
                        JsonObject scammerListAsset = assets.get(0).getAsJsonObject();
                        if(scammerListAsset.has("browser_download_url")) {
                            downloadUrl = scammerListAsset.get("browser_download_url").getAsString();
                            updateAvailable = true;
                        }
                    }
                }
            } else {
                System.out.println("[AutoCraft] Could not check for updates: Invalid response.");
            }
        } catch (IOException | IllegalStateException | JsonSyntaxException e) {
            System.out.println("[AutoCraft] Could not check for updates: "+e.getMessage());
        }
    }

    private void update() {
        if(!canDoUpdate()) return;
        addonJar.delete();
        try {
            FileUtils.copyURLToFile(new URL(downloadUrl), addonJar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean canDoUpdate() {
        return addonJar != null && addonJar.isFile();
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUpdatePending() {
        return updateAvailable && AutoCraft.getAutoCraft().getSettings().isAutoUpdateAddon() && canDoUpdate();
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void setAddonJar(File addonJar) {
        this.addonJar = addonJar;
    }
}
