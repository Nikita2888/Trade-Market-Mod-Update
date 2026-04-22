package com.trademarket;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeMarketMod implements ModInitializer {
    public static final String MOD_ID = "trademarket";
    // Автоматически получаем версию из fabric.mod.json
    public static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Trade Market mod initializing...");
        
        // Мод работает полностью на клиенте через Supabase
        // Серверная логика не требуется - все данные хранятся в облаке
        
        LOGGER.info("Trade Market mod initialized!");
        LOGGER.info("Data is stored in Supabase cloud database");
    }
}
