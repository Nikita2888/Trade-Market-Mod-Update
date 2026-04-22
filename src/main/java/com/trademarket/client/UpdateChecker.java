package com.trademarket.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trademarket.TradeMarketMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Проверка обновлений через GitHub Releases API
 */
public class UpdateChecker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeMarketMod.MOD_ID + "_updater");
    private static UpdateChecker instance;
    
    // GitHub repository info - ИЗМЕНИ НА СВОЙ РЕПОЗИТОРИЙ
    private static final String GITHUB_OWNER = "YOUR_GITHUB_USERNAME";
    private static final String GITHUB_REPO = "trademarket-mod";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";
    
    private final HttpClient httpClient;
    
    // Результат проверки
    private UpdateInfo latestUpdate = null;
    private boolean checkInProgress = false;
    private boolean checkComplete = false;
    private String checkError = null;
    
    public static UpdateChecker getInstance() {
        if (instance == null) {
            instance = new UpdateChecker();
        }
        return instance;
    }
    
    private UpdateChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Информация об обновлении
     */
    public static class UpdateInfo {
        public final String version;
        public final String downloadUrl;
        public final String changelog;
        public final String publishedAt;
        public final boolean isNewer;
        
        public UpdateInfo(String version, String downloadUrl, String changelog, String publishedAt, boolean isNewer) {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
            this.publishedAt = publishedAt;
            this.isNewer = isNewer;
        }
    }
    
    /**
     * Запускает асинхронную проверку обновлений
     */
    public void checkForUpdates(Consumer<UpdateInfo> onSuccess, Consumer<String> onError) {
        if (checkInProgress) {
            return;
        }
        
        checkInProgress = true;
        checkComplete = false;
        checkError = null;
        latestUpdate = null;
        
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "TradeMarket-Mod/" + TradeMarketMod.MOD_VERSION)
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    
                    String tagName = json.get("tag_name").getAsString();
                    // Убираем 'v' из версии если есть (v1.2.0 -> 1.2.0)
                    String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                    
                    String changelog = json.has("body") && !json.get("body").isJsonNull() 
                            ? json.get("body").getAsString() 
                            : "";
                    
                    String publishedAt = json.has("published_at") 
                            ? json.get("published_at").getAsString() 
                            : "";
                    
                    // Ищем .jar файл в assets
                    String downloadUrl = "";
                    if (json.has("assets")) {
                        JsonArray assets = json.getAsJsonArray("assets");
                        for (int i = 0; i < assets.size(); i++) {
                            JsonObject asset = assets.get(i).getAsJsonObject();
                            String name = asset.get("name").getAsString();
                            if (name.endsWith(".jar")) {
                                downloadUrl = asset.get("browser_download_url").getAsString();
                                break;
                            }
                        }
                    }
                    
                    boolean isNewer = isVersionNewer(version, TradeMarketMod.MOD_VERSION);
                    
                    latestUpdate = new UpdateInfo(version, downloadUrl, changelog, publishedAt, isNewer);
                    checkComplete = true;
                    checkInProgress = false;
                    
                    LOGGER.info("[UpdateChecker] Текущая версия: {}, последняя: {}, обновление доступно: {}", 
                            TradeMarketMod.MOD_VERSION, version, isNewer);
                    
                    if (onSuccess != null) {
                        onSuccess.accept(latestUpdate);
                    }
                    
                } else if (response.statusCode() == 404) {
                    // Нет релизов
                    checkError = "Релизы не найдены";
                    checkComplete = true;
                    checkInProgress = false;
                    
                    if (onError != null) {
                        onError.accept(checkError);
                    }
                } else {
                    checkError = "HTTP " + response.statusCode();
                    checkComplete = true;
                    checkInProgress = false;
                    
                    if (onError != null) {
                        onError.accept(checkError);
                    }
                }
                
            } catch (Exception e) {
                LOGGER.error("[UpdateChecker] Ошибка проверки обновлений: ", e);
                checkError = e.getMessage();
                checkComplete = true;
                checkInProgress = false;
                
                if (onError != null) {
                    onError.accept(checkError);
                }
            }
        });
    }
    
    /**
     * Сравнивает версии (1.2.3 > 1.2.2)
     */
    private boolean isVersionNewer(String newVersion, String currentVersion) {
        try {
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            
            int maxLength = Math.max(newParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i].replaceAll("[^0-9]", "")) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
                
                if (newPart > currentPart) {
                    return true;
                } else if (newPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Версии равны
        } catch (Exception e) {
            LOGGER.warn("[UpdateChecker] Ошибка сравнения версий: {} vs {}", newVersion, currentVersion);
            return false;
        }
    }
    
    // Геттеры состояния
    public boolean isCheckInProgress() { return checkInProgress; }
    public boolean isCheckComplete() { return checkComplete; }
    public String getCheckError() { return checkError; }
    public UpdateInfo getLatestUpdate() { return latestUpdate; }
    
    /**
     * Сбрасывает состояние для новой проверки
     */
    public void reset() {
        checkInProgress = false;
        checkComplete = false;
        checkError = null;
        latestUpdate = null;
    }
}
