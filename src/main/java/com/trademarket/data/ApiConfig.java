package com.trademarket.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trademarket.TradeMarketMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Конфигурация API для TradeMarket
 * Позволяет переключаться между Supabase и собственным VPS сервером
 */
public class ApiConfig {
    
    private static ApiConfig instance;
    private static final String CONFIG_FILE = "trademarket_api.json";
    
    // === РЕЖИМ РАБОТЫ ===
    // "vps" - использовать собственный VPS сервер (по умолчанию)
    // "supabase" - использовать Supabase (можно изменить в конфиге)
    private String mode = "vps";
    
    // === SUPABASE НАСТРОЙКИ (старые) ===
    private String supabaseEdgeUrl = "https://erxijnqrxnwfoesptgzo.supabase.co/functions/v1/trade-api";
    private String supabaseRestUrl = "https://erxijnqrxnwfoesptgzo.supabase.co/rest/v1";
    private String supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVyeGlqbnFyeG53Zm9lc3B0Z3pvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYwMTU5MDEsImV4cCI6MjA5MTU5MTkwMX0.H6jcP0er_kbSVs9QKioosL8jkiioeBirXm0l4ZIWesM";
    
    // === VPS НАСТРОЙКИ (новые) ===
    // ВАЖНО: Замените на ваш реальный IP и порт!
    private String vpsApiUrl = "http://45.89.67.137:3000/api";
    private String vpsRestUrl = "http://45.89.67.137:3000/rest/v1";
    // API ключ НЕ НУЖЕН для мода - безопасность через x-player-uuid/x-player-name заголовки
    // Сервер проверяет UUID игрока для операций записи
    private String vpsApiKey = "";
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
            instance.load();
        }
        return instance;
    }
    
    private ApiConfig() {}
    
    /**
     * Загрузить конфигурацию из файла
     */
    public void load() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, CONFIG_FILE);
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                
                if (json.has("mode")) mode = json.get("mode").getAsString();
                
                // Supabase
                if (json.has("supabase_edge_url")) supabaseEdgeUrl = json.get("supabase_edge_url").getAsString();
                if (json.has("supabase_rest_url")) supabaseRestUrl = json.get("supabase_rest_url").getAsString();
                if (json.has("supabase_anon_key")) supabaseAnonKey = json.get("supabase_anon_key").getAsString();
                
                // VPS
                if (json.has("vps_api_url")) vpsApiUrl = json.get("vps_api_url").getAsString();
                if (json.has("vps_rest_url")) vpsRestUrl = json.get("vps_rest_url").getAsString();
                if (json.has("vps_api_key")) vpsApiKey = json.get("vps_api_key").getAsString();
                
                TradeMarketMod.LOGGER.info("TradeMarket API config loaded. Mode: " + mode);
                
            } catch (Exception e) {
                TradeMarketMod.LOGGER.error("Failed to load API config: " + e.getMessage());
                save(); // Создать файл с дефолтными значениями
            }
        } else {
            save(); // Создать файл с дефолтными значениями
            TradeMarketMod.LOGGER.info("TradeMarket API config created with defaults. Mode: " + mode);
        }
    }
    
    /**
     * Сохранить конфигурацию в файл
     */
    public void save() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, CONFIG_FILE);
        
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode);
        
        // Supabase
        json.addProperty("supabase_edge_url", supabaseEdgeUrl);
        json.addProperty("supabase_rest_url", supabaseRestUrl);
        json.addProperty("supabase_anon_key", supabaseAnonKey);
        
        // VPS
        json.addProperty("vps_api_url", vpsApiUrl);
        json.addProperty("vps_rest_url", vpsRestUrl);
        json.addProperty("vps_api_key", vpsApiKey);
        
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            TradeMarketMod.LOGGER.error("Failed to save API config: " + e.getMessage());
        }
    }
    
    // === ГЕТТЕРЫ ===
    
    /**
     * Текущий режим работы: "supabase" или "vps"
     */
    public String getMode() {
        return mode;
    }
    
    /**
     * Использует ли VPS режим
     */
    public boolean isVpsMode() {
        return "vps".equalsIgnoreCase(mode);
    }
    
    /**
     * URL для API операций записи (Edge Functions или Express API)
     */
    public String getApiUrl() {
        return isVpsMode() ? vpsApiUrl : supabaseEdgeUrl;
    }
    
    /**
     * URL для REST операций чтения
     */
    public String getRestUrl() {
        return isVpsMode() ? vpsRestUrl : supabaseRestUrl;
    }
    
    /**
     * API ключ (для Supabase - anon key, для VPS - опциональный ключ)
     */
    public String getApiKey() {
        return isVpsMode() ? vpsApiKey : supabaseAnonKey;
    }
    
    /**
     * Нужно ли добавлять заголовки авторизации Supabase
     */
    public boolean needsSupabaseAuth() {
        return !isVpsMode();
    }
    
    // === СЕТТЕРЫ ===
    
    public void setMode(String mode) {
        this.mode = mode;
        save();
    }
    
    public void setVpsApiUrl(String url) {
        this.vpsApiUrl = url;
        save();
    }
    
    public void setVpsRestUrl(String url) {
        this.vpsRestUrl = url;
        save();
    }
    
    public void setVpsApiKey(String key) {
        this.vpsApiKey = key;
        save();
    }
}
