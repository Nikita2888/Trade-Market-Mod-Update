package com.trademarket.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trademarket.TradeMarketMod;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Менеджер удалённого логирования действий пользователей.
 * Собирает логи и отправляет их на сервер пакетами для анализа.
 */
public class RemoteLogger {
    
    private static RemoteLogger instance;
    
    // Уровни логирования
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }
    
    // Категории логов
    public enum Category {
        SYSTEM,     // Системные события (запуск, остановка)
        AUTH,       // Авторизация, вход на сервер
        MENU,       // Открытие/закрытие меню
        LISTING,    // Действия с листингами (создание, покупка, удаление)
        CHAT,       // Сообщения в чатах листингов
        TICKET,     // Тикеты поддержки
        FAVORITE,   // Избранное
        ERROR       // Ошибки
    }
    
    // Конфигурация
    private static final int BATCH_SIZE = 10;           // Отправлять пакетами по 10 логов
    private static final int FLUSH_INTERVAL_SECONDS = 30; // Или каждые 30 секунд
    private static final int MAX_QUEUE_SIZE = 100;      // Максимум логов в очереди
    
    // Очередь логов для пакетной отправки
    private final ConcurrentLinkedQueue<JsonObject> logQueue = new ConcurrentLinkedQueue<>();
    
    // HTTP клиент
    private final HttpClient httpClient;
    
    // Планировщик для периодической отправки
    private final ScheduledExecutorService scheduler;
    
    // Информация о клиенте
    private String playerUUID = "";
    private String playerName = "";
    private String serverIP = "";
    private final String modVersion;
    private final String minecraftVersion;
    
    // Флаг включения логирования
    private boolean enabled = true;
    
    private RemoteLogger() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        this.modVersion = TradeMarketMod.MOD_VERSION;
        this.minecraftVersion = getMinecraftVersion();
        
        // Запускаем периодическую отправку логов
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TradeMarket-RemoteLogger");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(this::flushLogs, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        // Отправляем heartbeat онлайн статуса каждую минуту
        scheduler.scheduleAtFixedRate(this::sendOnlineHeartbeat, 10, 60, TimeUnit.SECONDS);
        
        TradeMarketMod.LOGGER.info("RemoteLogger initialized");
    }
    
    public static RemoteLogger getInstance() {
        if (instance == null) {
            instance = new RemoteLogger();
        }
        return instance;
    }
    
    /**
     * Установить информацию о текущем игроке
     */
    public void setPlayerInfo(String uuid, String name) {
        this.playerUUID = uuid != null ? uuid : "";
        this.playerName = name != null ? name : "";
    }
    
    /**
     * Установить IP сервера
     */
    public void setServerIP(String ip) {
        this.serverIP = ip != null ? ip : "";
    }
    
    /**
     * Включить/выключить удалённое логирование
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Сбросить информацию (при выходе из игры)
     */
    public void reset() {
        this.playerUUID = "";
        this.playerName = "";
        this.serverIP = "";
    }
    
    /**
     * Отправить heartbeat онлайн статуса
     */
    private void sendOnlineHeartbeat() {
        if (playerUUID.isEmpty() || playerName.isEmpty()) return;
        
        try {
            String apiUrl = SupabaseClient.getInstance().getApiUrl();
            
            JsonObject body = new JsonObject();
            body.addProperty("player_uuid", playerUUID);
            body.addProperty("player_name", playerName);
            body.addProperty("server_ip", serverIP);
            body.addProperty("mod_version", modVersion);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/users/heartbeat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .exceptionally(e -> {
                        TradeMarketMod.LOGGER.debug("Online heartbeat error: " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            TradeMarketMod.LOGGER.debug("Online heartbeat error: " + e.getMessage());
        }
    }
    
    // ==================== МЕТОДЫ ЛОГИРОВАНИЯ ====================
    
    /**
     * Основной метод логирования
     */
    public void log(Level level, Category category, String action, String message, JsonObject metadata) {
        if (!enabled) return;
        
        JsonObject logEntry = new JsonObject();
        logEntry.addProperty("player_uuid", playerUUID);
        logEntry.addProperty("player_name", playerName);
        logEntry.addProperty("log_level", level.name());
        logEntry.addProperty("category", category.name());
        logEntry.addProperty("action", action);
        logEntry.addProperty("message", message != null ? message : "");
        logEntry.add("metadata", metadata != null ? metadata : new JsonObject());
        logEntry.addProperty("mod_version", modVersion);
        logEntry.addProperty("minecraft_version", minecraftVersion);
        logEntry.addProperty("server_ip", serverIP);
        
        // Добавляем в очередь
        if (logQueue.size() < MAX_QUEUE_SIZE) {
            logQueue.add(logEntry);
        }
        
        // Если накопилось достаточно - отправляем
        if (logQueue.size() >= BATCH_SIZE) {
            flushLogs();
        }
        
        // Также пишем в локальный лог для отладки
        if (level == Level.ERROR || level == Level.WARN) {
            TradeMarketMod.LOGGER.warn("[RemoteLog] {} | {} | {} | {}", level, category, action, message);
        }
    }
    
    /**
     * Упрощённые методы логирования
     */
    public void info(Category category, String action, String message) {
        log(Level.INFO, category, action, message, null);
    }
    
    public void info(Category category, String action, String message, JsonObject metadata) {
        log(Level.INFO, category, action, message, metadata);
    }
    
    public void warn(Category category, String action, String message) {
        log(Level.WARN, category, action, message, null);
    }
    
    public void warn(Category category, String action, String message, JsonObject metadata) {
        log(Level.WARN, category, action, message, metadata);
    }
    
    public void error(Category category, String action, String message) {
        log(Level.ERROR, category, action, message, null);
    }
    
    public void error(Category category, String action, String message, JsonObject metadata) {
        log(Level.ERROR, category, action, message, metadata);
    }
    
    public void error(Category category, String action, String message, Throwable throwable) {
        JsonObject metadata = new JsonObject();
        if (throwable != null) {
            metadata.addProperty("exception", throwable.getClass().getName());
            metadata.addProperty("exception_message", throwable.getMessage());
            
            // Добавляем stack trace (первые 5 строк)
            StringBuilder stackTrace = new StringBuilder();
            StackTraceElement[] stack = throwable.getStackTrace();
            for (int i = 0; i < Math.min(5, stack.length); i++) {
                stackTrace.append(stack[i].toString()).append("\n");
            }
            metadata.addProperty("stack_trace", stackTrace.toString());
        }
        log(Level.ERROR, category, action, message, metadata);
    }
    
    // ==================== СПЕЦИАЛИЗИРОВАННЫЕ МЕТОДЫ ====================
    
    /**
     * Логирование входа на сервер
     */
    public void logServerJoin(String serverIP) {
        setServerIP(serverIP);
        JsonObject meta = new JsonObject();
        meta.addProperty("server_ip", serverIP);
        info(Category.AUTH, "SERVER_JOIN", "Игрок подключился к серверу", meta);
    }
    
    /**
     * Логирование выхода с сервера
     */
    public void logServerLeave() {
        info(Category.AUTH, "SERVER_LEAVE", "Игрок отключился от сервера");
        flushLogs(); // Сразу отправляем при выходе
    }
    
    /**
     * Логирование открытия меню
     */
    public void logMenuOpen() {
        info(Category.MENU, "MENU_OPEN", "Открыто главное меню Trade Market");
    }
    
    /**
     * Логирование закрытия меню
     */
    public void logMenuClose() {
        info(Category.MENU, "MENU_CLOSE", "Закрыто меню Trade Market");
    }
    
    /**
     * Логирование переключения вкладки
     */
    public void logTabSwitch(String tabName) {
        JsonObject meta = new JsonObject();
        meta.addProperty("tab", tabName);
        info(Category.MENU, "TAB_SWITCH", "Переключение на вкладку: " + tabName, meta);
    }
    
    /**
     * Логирование создания листинга
     */
    public void logListingCreate(String itemName, String price, int quantity) {
        JsonObject meta = new JsonObject();
        meta.addProperty("item_name", itemName);
        meta.addProperty("price", price != null ? price : "");
        meta.addProperty("quantity", quantity);
        info(Category.LISTING, "LISTING_CREATE", "Создан листинг: " + itemName, meta);
    }
    
    /**
     * Логирование покупки
     */
    public void logListingPurchase(String itemName, String sellerName, int price) {
        JsonObject meta = new JsonObject();
        meta.addProperty("item_name", itemName);
        meta.addProperty("seller_name", sellerName);
        meta.addProperty("price", price);
        info(Category.LISTING, "LISTING_PURCHASE", "Покупка: " + itemName + " у " + sellerName, meta);
    }
    
    /**
     * Логирование удаления листинга
     */
    public void logListingDelete(String listingId, String itemName) {
        JsonObject meta = new JsonObject();
        meta.addProperty("listing_id", listingId);
        meta.addProperty("item_name", itemName);
        info(Category.LISTING, "LISTING_DELETE", "Удалён листинг: " + itemName, meta);
    }
    
    /**
     * Логирование отправки сообщения в чат листинга
     */
    public void logChatMessage(String listingId, String itemName) {
        JsonObject meta = new JsonObject();
        meta.addProperty("listing_id", listingId);
        meta.addProperty("item_name", itemName);
        info(Category.CHAT, "CHAT_MESSAGE_SENT", "Отправлено сообщение в чат листинга", meta);
    }
    
    /**
     * Логирование создания тикета
     */
    public void logTicketCreate(String ticketId, String subject) {
        JsonObject meta = new JsonObject();
        meta.addProperty("ticket_id", ticketId);
        meta.addProperty("subject", subject);
        info(Category.TICKET, "TICKET_CREATE", "Создан тикет: " + subject, meta);
    }
    
    /**
     * Логирование отправки сообщения в тикет
     */
    public void logTicketMessage(String ticketId) {
        JsonObject meta = new JsonObject();
        meta.addProperty("ticket_id", ticketId);
        info(Category.TICKET, "TICKET_MESSAGE_SENT", "Отправлено сообщение в тикет", meta);
    }
    
    /**
     * Логирование закрытия тикета
     */
    public void logTicketClose(String ticketId) {
        JsonObject meta = new JsonObject();
        meta.addProperty("ticket_id", ticketId);
        info(Category.TICKET, "TICKET_CLOSE", "Тикет закрыт", meta);
    }
    
    /**
     * Логирование добавления в избранное
     */
    public void logFavoriteAdd(String listingId, String itemName) {
        JsonObject meta = new JsonObject();
        meta.addProperty("listing_id", listingId);
        meta.addProperty("item_name", itemName);
        info(Category.FAVORITE, "FAVORITE_ADD", "Добавлено в избранное: " + itemName, meta);
    }
    
    /**
     * Логирование удаления из избранного
     */
    public void logFavoriteRemove(String listingId, String itemName) {
        JsonObject meta = new JsonObject();
        meta.addProperty("listing_id", listingId);
        meta.addProperty("item_name", itemName);
        info(Category.FAVORITE, "FAVORITE_REMOVE", "Удалено из избранного: " + itemName, meta);
    }
    
    /**
     * Логирование ошибки API
     */
    public void logApiError(String endpoint, int statusCode, String errorMessage) {
        JsonObject meta = new JsonObject();
        meta.addProperty("endpoint", endpoint);
        meta.addProperty("status_code", statusCode);
        error(Category.ERROR, "API_ERROR", "Ошибка API " + endpoint + ": " + errorMessage, meta);
    }
    
    /**
     * Логирование ошибки соединения
     */
    public void logConnectionError(String endpoint, String errorMessage) {
        JsonObject meta = new JsonObject();
        meta.addProperty("endpoint", endpoint);
        error(Category.ERROR, "CONNECTION_ERROR", "Ошибка соединения: " + errorMessage, meta);
    }
    
    // ==================== ОТПРАВКА ЛОГОВ ====================
    
    /**
     * Отправить накопленные логи на сервер
     */
    private synchronized void flushLogs() {
        if (logQueue.isEmpty()) return;
        
        // Собираем логи для отправки
        JsonArray logsArray = new JsonArray();
        JsonObject log;
        int count = 0;
        
        while ((log = logQueue.poll()) != null && count < BATCH_SIZE * 2) {
            logsArray.add(log);
            count++;
        }
        
        if (logsArray.size() == 0) return;
        
        // Отправляем асинхронно
        JsonObject payload = new JsonObject();
        payload.add("logs", logsArray);
        
        String apiUrl = SupabaseClient.getInstance().getApiUrl();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/logs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 201) {
                        TradeMarketMod.LOGGER.warn("Failed to send logs: HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    TradeMarketMod.LOGGER.debug("Error sending logs: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Принудительная отправка всех логов (вызывать при выходе)
     */
    public void forceFlush() {
        flushLogs();
    }
    
    /**
     * Остановить логгер (вызывать при выключении мода)
     */
    public void shutdown() {
        flushLogs();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    private String getMinecraftVersion() {
        try {
            return MinecraftClient.getInstance().getGameVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
