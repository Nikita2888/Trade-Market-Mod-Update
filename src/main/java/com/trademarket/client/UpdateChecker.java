package com.trademarket.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trademarket.TradeMarketMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Проверка обновлений через GitHub Releases API
 */
public class UpdateChecker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeMarketMod.MOD_ID + "_updater");
    private static UpdateChecker instance;
    
    // GitHub repository info
    private static final String GITHUB_OWNER = "Nikita2888";
    private static final String GITHUB_REPO = "Trade-Market-Mod-Update";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";
    
    // Fallback URL без rate limit (нужно создать файл version.json в репозитории)
    private static final String FALLBACK_URL = "https://raw.githubusercontent.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/main/version.json";
    
    // GitHub Personal Access Token (опционально, но рекомендуется для увеличения rate limit)
    // Без токена: 60 запросов/час, с токеном: 5000 запросов/час
    // Создай токен на https://github.com/settings/tokens (можно без прав, только для публичных репо)
    // Или установи переменную окружения GITHUB_TOKEN
    private static final String GITHUB_TOKEN = getGithubToken();
    
    private static String getGithubToken() {
        // Сначала пробуем переменную окружения
        String envToken = System.getenv("GITHUB_TOKEN");
        if (envToken != null && !envToken.isEmpty()) {
            return envToken;
        }
        // Fallback на жестко заданный токен (для разработки)
        return null; // Установи токен здесь если хочешь: "ghp_xxxx"
    }
    
    // Результат проверки
    private UpdateInfo latestUpdate = null;
    private boolean checkInProgress = false;
    private boolean checkComplete = false;
    private String checkError = null;
    
    // Кэширование для уменьшения запросов к API (один раз за сессию)
    private boolean alreadyCheckedThisSession = false;
    private String cachedETag = null;
    
    public static UpdateChecker getInstance() {
        if (instance == null) {
            instance = new UpdateChecker();
        }
        return instance;
    }
    
    private UpdateChecker() {
        // Конструктор пустой - используем HttpURLConnection
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
        
        // Если уже проверяли в этой сессии и есть результат - возвращаем его
        if (alreadyCheckedThisSession && latestUpdate != null) {
            LOGGER.info("[UpdateChecker] Возвращаем кэшированный результат проверки");
            checkComplete = true;
            if (onSuccess != null) {
                onSuccess.accept(latestUpdate);
            }
            return;
        }
        
        // Если была ошибка в этой сессии - пропускаем повторную проверку
        if (alreadyCheckedThisSession && checkError != null) {
            LOGGER.info("[UpdateChecker] Пропускаем проверку - ошибка уже была в этой сессии: {}", checkError);
            checkComplete = true;
            if (onError != null) {
                onError.accept(checkError);
            }
            return;
        }
        
        checkInProgress = true;
        checkComplete = false;
        checkError = null;
        latestUpdate = null;
        
        LOGGER.info("[UpdateChecker] Начинаю проверку обновлений: {}", API_URL);
        
        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "TradeMarket-Mod/" + TradeMarketMod.MOD_VERSION);
                
                // Авторизация (увеличивает rate limit с 60 до 5000 запросов/час)
                if (GITHUB_TOKEN != null && !GITHUB_TOKEN.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
                    LOGGER.info("[UpdateChecker] Используем GitHub токен для авторизации");
                }
                
                // Условный запрос с ETag для экономии rate limit
                if (cachedETag != null) {
                    connection.setRequestProperty("If-None-Match", cachedETag);
                }
                
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                
                int responseCode = connection.getResponseCode();
                LOGGER.info("[UpdateChecker] HTTP Response: {}", responseCode);
                
                if (responseCode == 200) {
                    // Сохраняем ETag для будущих запросов
                    String etag = connection.getHeaderField("ETag");
                    if (etag != null) {
                        cachedETag = etag;
                    }
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String responseBody = response.toString();
                    LOGGER.info("[UpdateChecker] Получен ответ дли��ой {} символов", responseBody.length());
                    
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    
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
                    alreadyCheckedThisSession = true;
                    
                    LOGGER.info("[UpdateChecker] Текущая версия: {}, последняя: {}, обновление доступно: {}", 
                            TradeMarketMod.MOD_VERSION, version, isNewer);
                    
                    if (onSuccess != null) {
                        onSuccess.accept(latestUpdate);
                    }
                    
                } else if (responseCode == 304) {
                    // Not Modified - данные не изменились, используем кэш
                    LOGGER.info("[UpdateChecker] HTTP 304 - данные не изменились, используем кэшированный результат");
                    
                    if (latestUpdate != null) {
                        // У нас есть кэшированный результат
                        checkComplete = true;
                        checkInProgress = false;
                        alreadyCheckedThisSession = true;
                        
                        if (onSuccess != null) {
                            onSuccess.accept(latestUpdate);
                        }
                    } else {
                        // Нет кэша, делаем запрос без ETag
                        LOGGER.info("[UpdateChecker] Нет кэшированного результата, делаем полный запрос");
                        cachedETag = null;
                        checkInProgress = false;
                        checkForUpdates(onSuccess, onError);
                    }
                    return;
                    
                } else if (responseCode == 404) {
                    // Нет релизов
                    LOGGER.warn("[UpdateChecker] Релизы не найдены (404)");
                    checkError = "Релизы не найдены";
                    checkComplete = true;
                    checkInProgress = false;
                    alreadyCheckedThisSession = true;
                    
                    if (onError != null) {
                        onError.accept(checkError);
                    }
                } else if (responseCode == 403) {
                    // Rate limit - пробуем fallback URL
                    LOGGER.warn("[UpdateChecker] HTTP 403 - Rate limit. Пробуем fallback URL...");
                    connection.disconnect();
                    
                    // Пробуем получить версию из version.json
                    tryFallbackCheck(onSuccess, onError);
                    return; // Выходим, fallback обработает результат
                } else {
                    LOGGER.warn("[UpdateChecker] Неожиданный HTTP код: {}", responseCode);
                    checkError = "HTTP " + responseCode;
                    checkComplete = true;
                    checkInProgress = false;
                    alreadyCheckedThisSession = true;
                    
                    if (onError != null) {
                        onError.accept(checkError);
                    }
                }
                
            } catch (Exception e) {
                LOGGER.error("[UpdateChecker] Ошибка проверки обновлений: {}", e.getMessage());
                e.printStackTrace();
                checkError = e.getMessage();
                checkComplete = true;
                checkInProgress = false;
                alreadyCheckedThisSession = true;
                
                if (onError != null) {
                    onError.accept(checkError);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    /**
     * Fallback проверка через version.json (без rate limit)
     */
    private void tryFallbackCheck(Consumer<UpdateInfo> onSuccess, Consumer<String> onError) {
        LOGGER.info("[UpdateChecker] Пробуем fallback URL: {}", FALLBACK_URL);
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(FALLBACK_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "TradeMarket-Mod/" + TradeMarketMod.MOD_VERSION);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            LOGGER.info("[UpdateChecker] Fallback HTTP Response: {}", responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                
                String version = json.get("version").getAsString();
                String downloadUrl = json.has("download_url") ? json.get("download_url").getAsString() : "";
                String changelog = json.has("changelog") ? json.get("changelog").getAsString() : "";
                
                boolean isNewer = isVersionNewer(version, TradeMarketMod.MOD_VERSION);
                
                latestUpdate = new UpdateInfo(version, downloadUrl, changelog, "", isNewer);
                checkComplete = true;
                checkInProgress = false;
                alreadyCheckedThisSession = true;
                
                LOGGER.info("[UpdateChecker] Fallback успешен. Версия: {}, обновление: {}", version, isNewer);
                
                if (onSuccess != null) {
                    onSuccess.accept(latestUpdate);
                }
            } else {
                LOGGER.warn("[UpdateChecker] Fallback не удался: HTTP {}", responseCode);
                checkError = "rate_limit";
                checkComplete = true;
                checkInProgress = false;
                alreadyCheckedThisSession = true;
                
                if (onError != null) {
                    onError.accept(checkError);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[UpdateChecker] Ошибка fallback: {}", e.getMessage());
            checkError = "rate_limit";
            checkComplete = true;
            checkInProgress = false;
            alreadyCheckedThisSession = true;
            
            if (onError != null) {
                onError.accept(checkError);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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
