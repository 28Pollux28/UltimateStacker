package me.pollux28.ultimatestacker;

import me.pollux28.ultimatestacker.config.Config;
import me.pollux28.ultimatestacker.config.ConfigUtil;
import me.pollux28.ultimatestacker.config.MainConfigData;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;


public class UltimateStackerFabric implements ModInitializer {
    public static final String VERSION = "1.0";
    public static final String MODID = "ultimate_stacker_fabric";
    public static final Logger logger = LogManager.getLogger();
    public static MainConfigData CONFIG;

    public static void refreshConfig() {
        CONFIG = ConfigUtil.getFromConfig(MainConfigData.class, Paths.get("", "config", "ultimatestackerfabric.json"));
    }

    public static void saveConfig() {
        ConfigUtil.configToFile(CONFIG);
    }

    @Override
    public void onInitialize() {
        CONFIG = Config.init();
    }
}
