package me.pollux28.ultimatestacker.config;
//Code used from Simplex Terrain <https://github.com/SuperCoder7979/simplexterrain>, with permission from SuperCoder79

import me.pollux28.ultimatestacker.UltimateStackerFabric;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static MainConfigData init() {
        MainConfigData configData = null;
        try {
            Path configDir = Paths.get("", "config", "ultimatestackerfabric.json");
            if (Files.exists(configDir)) {
                configData = ConfigUtil.gson.fromJson(new FileReader(configDir.toFile()), MainConfigData.class);
                //save new values
                if (!configData.configVersion.equals(UltimateStackerFabric.VERSION)) {
                    configData.configVersion = UltimateStackerFabric.VERSION;
                    BufferedWriter writer = new BufferedWriter(new FileWriter(configDir.toFile()));
                    writer.write(ConfigUtil.gson.toJson(configData));
                    writer.close();
                }
            } else {
                configData = new MainConfigData();
                Paths.get("", "config").toFile().mkdirs();
                BufferedWriter writer = new BufferedWriter(new FileWriter(configDir.toFile()));
                writer.write(ConfigUtil.gson.toJson(configData));

                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configData;
    }
}
