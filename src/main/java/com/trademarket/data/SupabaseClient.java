package com.trademarket.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trademarket.TradeMarketMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Клиент для работы с Supabase REST API
 */
public class SupabaseClient {
    
    // Edge Function URL - ключ Supabase теперь хранится на сервере, а не в моде
    private static final String API_URL = "https://erxijnqrxnwfoesptgzo.supabase.co/functions/v1/trade-api";
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    // Данные текущего игрока для авторизации запросов
    private String currentPlayerUUID = null;
    private String currentPlayerName = "";
    
    private static SupabaseClient instance;
    
    public static SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }
    
    private SupabaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * Установить данные текущего игрока для авторизации запросов
     * Вызывать при входе игрока в игру
     */
    public void setCurrentPlayer(String uuid, String name) {
        this.currentPlayerUUID = uuid;
        this.currentPlayerName = name;
        TradeMarketMod.LOGGER.info("TradeMarket: Player set - " + name + " (" + uuid + ")");
    }
    
    /**
     * Проверяет, готов ли клиент к отправке запросов (установлен ли UUID игрока)
     */
    public boolean isReady() {
        return currentPlayerUUID != null && !currentPlayerUUID.isEmpty();
    }
    
    /**
     * Сбросить данные игрока (при выходе из игры)
     */
    public void clearCurrentPlayer() {
        this.currentPlayerUUID = null;
        this.currentPlayerName = "";
        TradeMarketMod.LOGGER.info("TradeMarket: Player data cleared");
    }
    
    /**
     * Получить URL API (для RemoteLogger)
     */
    public String getApiUrl() {
        return API_URL;
    }
    
    /**
     * Создать базовый запрос с заголовками авторизации
     */
    private HttpRequest.Builder createRequest(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + endpoint))
                .header("Content-Type", "application/json");
        
        // Добавляем заголовки игрока только если данные установлены
        if (currentPlayerUUID != null && !currentPlayerUUID.isEmpty()) {
            builder.header("x-player-uuid", currentPlayerUUID);
        }
        if (currentPlayerName != null && !currentPlayerName.isEmpty()) {
            builder.header("x-player-name", currentPlayerName);
        }
        
        return builder;
    }
    
    /**
     * Получить все активные лоты
     */
    public void fetchAllListings(Consumer<List<MarketListing>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/listings")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<MarketListing> listings = parseListings(response.body());
                        onSuccess.accept(listings);
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить лоты конкретного продавца
     */
    public void fetchSellerListings(String sellerId, Consumer<List<MarketListing>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/listings?seller_id=" + sellerId)
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<MarketListing> listings = parseListings(response.body());
                        onSuccess.accept(listings);
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить лот по ID
     */
    public void getListingById(UUID listingId, Consumer<MarketListing> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/listings?id=" + listingId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<MarketListing> listings = parseListings(response.body());
                        if (!listings.isEmpty()) {
                            onSuccess.accept(listings.get(0));
                        } else {
                            onSuccess.accept(null);
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Создать новый лот
     */
    public void createListing(MarketListing listing, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("id", listing.getListingId().toString());
        json.addProperty("seller_id", listing.getSellerId().toString());
        json.addProperty("seller_name", listing.getSellerName());
        json.addProperty("item_id", listing.getItemId());
        json.addProperty("item_count", listing.getItemCount());
        json.addProperty("item_nbt", listing.getItemNbt());
        json.addProperty("item_display_name", listing.getItemDisplayName());
        json.addProperty("price", listing.getPrice());
        json.addProperty("description", listing.getDescription());
        json.addProperty("active", listing.isActive());
        
        HttpRequest request = createRequest("/listings")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        TradeMarketMod.LOGGER.info("Listing created successfully: " + listing.getListingId());
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Удалить лот (деактивировать)
     */
    public void removeListing(UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/listings/" + listingId.toString())
                .DELETE()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        TradeMarketMod.LOGGER.info("Listing removed: " + listingId);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Обновить лот (цену и условия)
     */
    public void updateListing(UUID listingId, String price, String description, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("price", price != null ? price : "");
        json.addProperty("description", description != null ? description : "");
        
        HttpRequest request = createRequest("/listings/" + listingId.toString())
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        TradeMarketMod.LOGGER.info("Listing updated: " + listingId);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Полностью удалить лот из БД
     */
    public void deleteListing(UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/listings/" + listingId.toString())
                .DELETE()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        TradeMarketMod.LOGGER.info("Listing deleted: " + listingId);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ ЧАТА
    // =====================================================
    
    /**
     * Класс сообщения чата
     */
    public static class ChatMessage {
        public final UUID id;
        public final UUID listingId;
        public final UUID senderId;
        public final String senderName;
        public final String message;
        public final long createdAt;
        
        public ChatMessage(UUID id, UUID listingId, UUID senderId, String senderName, String message, long createdAt) {
            this.id = id;
            this.listingId = listingId;
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Получить сообщения для лота
     */
    public void fetchMessages(UUID listingId, Consumer<List<ChatMessage>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/messages?listing_id=" + listingId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<ChatMessage> messages = parseMessages(response.body());
                        onSuccess.accept(messages);
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Отправить сообщение
     */
    public void sendMessage(UUID listingId, UUID senderId, String senderName, String message, 
                           Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("listing_id", listingId.toString());
        json.addProperty("sender_id", senderId.toString());
        json.addProperty("sender_name", senderName);
        json.addProperty("message", message);
        
        HttpRequest request = createRequest("/messages")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Отправить сообщение в чат с проверкой бана
     * Проверяет ban_type: 'chat' или 'full'
     */
    public void sendChatMessage(UUID listingId, UUID senderId, String senderName, String message, 
                                Runnable onSuccess, Consumer<String> onError) {
        // Сначала проверяем бан на чат
        checkUserBans(senderName, bans -> {
            // Проверяем наличие активного бана на чат или полного бана
            boolean hasChatBan = false;
            String banReason = null;
            
            for (UserBan ban : bans) {
                if (ban.isActive && !ban.isExpired()) {
                    if ("chat".equals(ban.banType) || "full".equals(ban.banType)) {
                        hasChatBan = true;
                        banReason = ban.reason;
                        break;
                    }
                }
            }
            
            if (hasChatBan) {
                // Пользователь з��банен в чате
                String errorMsg = com.trademarket.client.LocalizationManager.getInstance().get("chat_blocked");
                if (banReason != null && !banReason.isEmpty()) {
                    errorMsg += ": " + banReason;
                }
                onError.accept(errorMsg);
                return;
            }
            
            // Бана нет - отправляем сообщение
            sendMessage(listingId, senderId, senderName, message, onSuccess, onError);
            
        }, error -> {
            // Ошибка проверки бана - всё равно пробуем отпр��вить
            TradeMarketMod.LOGGER.warn("Failed to check chat ban for " + senderName + ": " + error);
            sendMessage(listingId, senderId, senderName, message, onSuccess, onError);
        });
    }
    
    /**
     * Парсинг сообщений
     */
    private List<ChatMessage> parseMessages(String jsonResponse) {
        List<ChatMessage> messages = new ArrayList<>();
        
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                UUID id = UUID.fromString(obj.get("id").getAsString());
                UUID listingId = UUID.fromString(obj.get("listing_id").getAsString());
                UUID senderId = UUID.fromString(obj.get("sender_id").getAsString());
                String senderName = obj.get("sender_name").getAsString();
                String message = obj.get("message").getAsString();
                
                long createdAt = System.currentTimeMillis();
                if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
                    String timestamp = obj.get("created_at").getAsString();
                    try {
                        createdAt = java.time.Instant.parse(timestamp).toEpochMilli();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                messages.add(new ChatMessage(id, listingId, senderId, senderName, message, createdAt));
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing messages: " + e.getMessage());
        }
        
        return messages;
    }
    
    /**
     * Получить все активные чаты пользователя с непрочитанными сообщениями
     * Возвращает список listing_id с количеством непрочитанных и последним сообщением
     */
    public void getUserActiveChats(UUID userId, Consumer<List<ActiveChat>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/messages/chats?user_id=" + userId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<ActiveChat> chats = parseActiveChats(response.body(), userId);
                        onSuccess.accept(chats);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Активный чат с последним сообщением
     */
    public static class ActiveChat {
        public final UUID listingId;
        public final UUID lastMessageId;
        public final UUID lastSenderId;
        public final String lastSenderName;
        public final String lastMessage;
        public final long lastMessageTime;
        public int unreadCount; // Изменяемое поле для непрочитанных
        
        public ActiveChat(UUID listingId, UUID lastMessageId, UUID lastSenderId, 
                         String lastSenderName, String lastMessage, long lastMessageTime, int unreadCount) {
            this.listingId = listingId;
            this.lastMessageId = lastMessageId;
            this.lastSenderId = lastSenderId;
            this.lastSenderName = lastSenderName;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
        }
    }
    
    /**
     * Получить количество непрочитанных сообщений в чатах продавец-покупатель
     */
    public void getUnreadMarketMessagesCount(UUID userId, Consumer<Integer> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/messages/unread?user_id=" + userId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                            int count = obj.has("count") ? obj.get("count").getAsInt() : 0;
                            onSuccess.accept(count);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Пометить сообщения как прочитанные для конкретного листинга
     */
    public void markMarketMessagesAsRead(UUID listingId, UUID userId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/messages/mark-read?listing_id=" + listingId.toString() + "&user_id=" + userId.toString())
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Парсинг активных чатов из API ответа
     */
    private List<ActiveChat> parseActiveChats(String json, UUID currentUserId) {
        List<ActiveChat> chats = new java.util.ArrayList<>();
        
        try {
            JsonArray array = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
            for (com.google.gson.JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                
                UUID listingId = UUID.fromString(obj.get("listing_id").getAsString());
                String lastSenderName = obj.has("last_sender_name") ? obj.get("last_sender_name").getAsString() : "";
                String lastMessage = obj.has("last_message") ? obj.get("last_message").getAsString() : "";
                int unreadCount = obj.has("unread_count") ? obj.get("unread_count").getAsInt() : 0;
                
                long createdAt = System.currentTimeMillis();
                if (obj.has("last_message_time") && !obj.get("last_message_time").isJsonNull()) {
                    String timestamp = obj.get("last_message_time").getAsString();
                    try {
                        createdAt = java.time.Instant.parse(timestamp).toEpochMilli();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                chats.add(new ActiveChat(listingId, null, null, lastSenderName, lastMessage, createdAt, unreadCount));
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing active chats: " + e.getMessage());
        }
        
        return chats;
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ ТРЕЙДА
    // =====================================================
    
    /**
     * Класс трейд-сессии
     */
    public static class TradeSession {
        public final UUID id;
        public final UUID initiatorId;
        public final String initiatorName;
        public final UUID targetId;
        public final String targetName;
        public final String initiatorItems;
        public final int initiatorEmeralds;
        public final boolean initiatorConfirmed;
        public final String targetItems;
        public final int targetEmeralds;
        public final boolean targetConfirmed;
        public final String status;
        public final long createdAt;
        
        public TradeSession(UUID id, UUID initiatorId, String initiatorName, 
                           UUID targetId, String targetName,
                           String initiatorItems, int initiatorEmeralds, boolean initiatorConfirmed,
                           String targetItems, int targetEmeralds, boolean targetConfirmed,
                           String status, long createdAt) {
            this.id = id;
            this.initiatorId = initiatorId;
            this.initiatorName = initiatorName;
            this.targetId = targetId;
            this.targetName = targetName;
            this.initiatorItems = initiatorItems;
            this.initiatorEmeralds = initiatorEmeralds;
            this.initiatorConfirmed = initiatorConfirmed;
            this.targetItems = targetItems;
            this.targetEmeralds = targetEmeralds;
            this.targetConfirmed = targetConfirmed;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Создать трейд-сессию (отправить приглашение)
     */
    public void createTradeSession(UUID initiatorId, String initiatorName, 
                                   UUID targetId, String targetName,
                                   Consumer<UUID> onSuccess, Consumer<String> onError) {
        UUID sessionId = UUID.randomUUID();
        
        JsonObject json = new JsonObject();
        json.addProperty("id", sessionId.toString());
        json.addProperty("initiator_id", initiatorId.toString());
        json.addProperty("initiator_name", initiatorName);
        json.addProperty("target_id", targetId.toString());
        json.addProperty("target_name", targetName);
        json.addProperty("status", "pending");
        
        HttpRequest request = createRequest("/trades")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        onSuccess.accept(sessionId);
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить активные трейды для игрока
     */
    public void fetchPlayerTrades(UUID playerId, Consumer<List<TradeSession>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/trades")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<TradeSession> trades = parseTradeSessions(response.body());
                        onSuccess.accept(trades);
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить трейд-сессию по ID
     */
    public void fetchTradeSession(UUID sessionId, Consumer<TradeSession> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/trades/" + sessionId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<TradeSession> trades = parseTradeSessions(response.body());
                        if (!trades.isEmpty()) {
                            onSuccess.accept(trades.get(0));
                        } else {
                            onError.accept("Trade session not found");
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Обновить трейд-сессию
     */
    public void updateTradeSession(UUID sessionId, String items, int emeralds, boolean confirmed, 
                                   boolean isInitiator, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        if (isInitiator) {
            json.addProperty("initiator_items", items);
            json.addProperty("initiator_emeralds", emeralds);
            json.addProperty("initiator_confirmed", confirmed);
        } else {
            json.addProperty("target_items", items);
            json.addProperty("target_emeralds", emeralds);
            json.addProperty("target_confirmed", confirmed);
        }
        
        HttpRequest request = createRequest("/trades/" + sessionId.toString())
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Принять приглашение (активировать трейд)
     */
    public void acceptTrade(UUID sessionId, Runnable onSuccess, Consumer<String> onError) {
        updateTradeStatus(sessionId, "active", onSuccess, onError);
    }
    
    /**
     * Отменить/завершить трейд
     */
    public void updateTradeStatus(UUID sessionId, String status, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("status", status);
        
        HttpRequest request = createRequest("/trades/" + sessionId.toString())
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Парсинг трейд-сессий
     */
    private List<TradeSession> parseTradeSessions(String jsonResponse) {
        List<TradeSession> sessions = new ArrayList<>();
        
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                UUID id = UUID.fromString(obj.get("id").getAsString());
                UUID initiatorId = UUID.fromString(obj.get("initiator_id").getAsString());
                String initiatorName = obj.get("initiator_name").getAsString();
                UUID targetId = UUID.fromString(obj.get("target_id").getAsString());
                String targetName = obj.get("target_name").getAsString();
                
                String initiatorItems = obj.has("initiator_items") && !obj.get("initiator_items").isJsonNull()
                        ? obj.get("initiator_items").getAsString() : "[]";
                int initiatorEmeralds = obj.has("initiator_emeralds") ? obj.get("initiator_emeralds").getAsInt() : 0;
                boolean initiatorConfirmed = obj.has("initiator_confirmed") && obj.get("initiator_confirmed").getAsBoolean();
                
                String targetItems = obj.has("target_items") && !obj.get("target_items").isJsonNull()
                        ? obj.get("target_items").getAsString() : "[]";
                int targetEmeralds = obj.has("target_emeralds") ? obj.get("target_emeralds").getAsInt() : 0;
                boolean targetConfirmed = obj.has("target_confirmed") && obj.get("target_confirmed").getAsBoolean();
                
                String status = obj.get("status").getAsString();
                
                long createdAt = System.currentTimeMillis();
                if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
                    try {
                        createdAt = java.time.Instant.parse(obj.get("created_at").getAsString()).toEpochMilli();
                    } catch (Exception e) {}
                }
                
                sessions.add(new TradeSession(id, initiatorId, initiatorName, targetId, targetName,
                        initiatorItems, initiatorEmeralds, initiatorConfirmed,
                        targetItems, targetEmeralds, targetConfirmed, status, createdAt));
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing trade sessions: " + e.getMessage());
        }
        
        return sessions;
    }
    
    // ==================== ADMIN SYSTEM ====================
    
    /**
     * Проверить, является ли игрок админом
     */
    public void checkIsAdmin(String username, Consumer<AdminInfo> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/admin/check?username=" + urlEncode(username))
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonElement element = JsonParser.parseString(response.body());
                            JsonObject obj;
                            if (element.isJsonArray()) {
                                JsonArray array = element.getAsJsonArray();
                                if (array.size() > 0) {
                                    obj = array.get(0).getAsJsonObject();
                                } else {
                                    onSuccess.accept(new AdminInfo(username, null, false));
                                    return;
                                }
                            } else {
                                obj = element.getAsJsonObject();
                            }
                            
                            if (obj.has("role") && !obj.get("role").isJsonNull()) {
                                String role = obj.get("role").getAsString();
                                onSuccess.accept(new AdminInfo(username, role, true));
                            } else {
                                onSuccess.accept(new AdminInfo(username, null, false));
                            }
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onSuccess.accept(new AdminInfo(username, null, false));
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Обновляет last_active для админа (вызывать при действиях админа)
     */
    public void updateAdminLastActive(String username) {
        if (username == null || username.isEmpty()) return;
        
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        
        HttpRequest request = createRequest("/admin/heartbeat")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        TradeMarketMod.LOGGER.debug("Admin heartbeat sent for: " + username);
                    } else {
                        TradeMarketMod.LOGGER.warn("Admin heartbeat failed: HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    TradeMarketMod.LOGGER.debug("Admin heartbeat error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Проверить блокировки пользователя
     */
    public void checkUserBans(String username, Consumer<List<UserBan>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/bans/check?username=" + urlEncode(username))
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<UserBan> bans = parseUserBans(response.body());
                        onSuccess.accept(bans);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Выдать блокировку пользователю
     */
    public void banUser(String targetUsername, String banType, String reason, String adminUsername, 
                        Long expiresAt, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("minecraft_username", targetUsername);
        json.addProperty("ban_type", banType);
        json.addProperty("reason", reason);
        json.addProperty("banned_by", adminUsername);
        if (expiresAt != null) {
            json.addProperty("expires_at", java.time.Instant.ofEpochMilli(expiresAt).toString());
        }
        json.addProperty("is_active", true);
        
        // Используем UPSERT с on_conflict для обновления существующего бана
        HttpRequest request = createRequest("/bans")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200 || response.statusCode() == 204) {
                        logAdminAction(adminUsername, "ban_user", targetUsername, null, 
                                "Ban type: " + banType + ", Reason: " + reason);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Снять блокировку пользователя
     */
    public void unbanUser(String targetUsername, String banType, String adminUsername, 
                          Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("is_active", false);
        
        HttpRequest request = createRequest("/bans?username=" + urlEncode(targetUsername) + "&ban_type=" + banType)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        logAdminAction(adminUsername, "unban_user", targetUsername, null, 
                                "Unban type: " + banType);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить все блокировки пользователя (для админ-панели)
     */
    public void getUserBansAdmin(String username, Consumer<List<UserBan>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/bans?username=" + urlEncode(username))
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<UserBan> bans = parseUserBans(response.body());
                        onSuccess.accept(bans);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // ==================== SUPPORT TICKETS ====================
    
    /**
     * Создать тикет поддержки
     */
    public void createSupportTicket(String subject, String createdBy, 
                                    Consumer<SupportTicket> onSuccess, Consumer<String> onError) {
        String ticketNumber = "T" + System.currentTimeMillis() % 100000000;
        
        JsonObject json = new JsonObject();
        json.addProperty("ticket_number", ticketNumber);
        json.addProperty("subject", subject);
        json.addProperty("status", "open");
        json.addProperty("created_by", createdBy);
        
        HttpRequest request = createRequest("/tickets")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Edge Function возвращает 200, а не 201
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            if (array.size() > 0) {
                                SupportTicket ticket = parseSupportTicket(array.get(0).getAsJsonObject());
                                onSuccess.accept(ticket);
                            } else {
                                onError.accept("Empty response from server");
                            }
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить тикеты пользователя
     */
    public void getUserTickets(String username, Consumer<List<SupportTicket>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/tickets?created_by=" + urlEncode(username))
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<SupportTicket> tickets = parseSupportTickets(response.body());
                        onSuccess.accept(tickets);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить все открытые тикеты (для админов)
     */
    public void getAllOpenTickets(Consumer<List<SupportTicket>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/tickets?status=open")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<SupportTicket> tickets = parseSupportTickets(response.body());
                        onSuccess.accept(tickets);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Закрыть тикет (для админов)
     */
    public void closeTicket(String ticketId, String adminUsername, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("status", "closed");
        json.addProperty("closed_by", adminUsername);
        json.addProperty("closed_at", java.time.Instant.now().toString());
        
        HttpRequest request = createRequest("/tickets/" + ticketId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        logAdminAction(adminUsername, "close_ticket", null, ticketId, "Ticket closed");
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Отправить сообщение в тикет
     */
    public void sendTicketMessage(String ticketId, String sender, String message, boolean isSupport,
                                  Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("ticket_id", ticketId);
        json.addProperty("sender", sender);
        json.addProperty("message", message);
        json.addProperty("is_support", isSupport);
        
        HttpRequest request = createRequest("/tickets/" + ticketId + "/messages")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Edge Function возвращает 200, а не 201
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        // Обновляем updated_at тикета
                        updateTicketTimestamp(ticketId);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить сообщения тикета
     */
    public void getTicketMessages(String ticketId, Consumer<List<TicketMessage>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/tickets/" + ticketId + "/messages")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<TicketMessage> messages = parseTicketMessages(response.body());
                        onSuccess.accept(messages);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Удалить лот (админ)
     */
    public void deleteListingAdmin(String listingId, String adminUsername, String reason,
                                   Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("active", false);
        
        HttpRequest request = createRequest("/listings/" + listingId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        logAdminAction(adminUsername, "delete_listing", null, listingId, "Reason: " + reason);
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // ==================== HELPER METHODS ====================
    
    private void updateTicketTimestamp(String ticketId) {
        JsonObject json = new JsonObject();
        json.addProperty("updated_at", java.time.Instant.now().toString());
        
        HttpRequest request = createRequest("/tickets/" + ticketId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
    
    private void logAdminAction(String adminUsername, String action, String targetUsername, 
                                String targetId, String details) {
        JsonObject json = new JsonObject();
        json.addProperty("admin_username", adminUsername);
        json.addProperty("action", action);
        if (targetUsername != null) json.addProperty("target_username", targetUsername);
        if (targetId != null) json.addProperty("target_id", targetId);
        if (details != null) {
            JsonObject detailsJson = new JsonObject();
            detailsJson.addProperty("info", details);
            json.add("details", detailsJson);
        }
        
        HttpRequest request = createRequest("/admin/logs")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
    
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
    
    private List<UserBan> parseUserBans(String jsonResponse) {
        List<UserBan> bans = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                UserBan ban = new UserBan(
                    obj.get("id").getAsString(),
                    obj.get("minecraft_username").getAsString(),
                    obj.get("ban_type").getAsString(),
                    obj.has("reason") && !obj.get("reason").isJsonNull() ? obj.get("reason").getAsString() : "",
                    obj.get("banned_by").getAsString(),
                    obj.has("expires_at") && !obj.get("expires_at").isJsonNull() ? 
                        java.time.Instant.parse(obj.get("expires_at").getAsString()).toEpochMilli() : null,
                    obj.get("is_active").getAsBoolean()
                );
                bans.add(ban);
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing user bans: " + e.getMessage());
        }
        return bans;
    }
    
    private List<SupportTicket> parseSupportTickets(String jsonResponse) {
        List<SupportTicket> tickets = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                tickets.add(parseSupportTicket(array.get(i).getAsJsonObject()));
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing support tickets: " + e.getMessage());
        }
        return tickets;
    }
    
    private SupportTicket parseSupportTicket(JsonObject obj) {
        return new SupportTicket(
            obj.get("id").getAsString(),
            obj.get("ticket_number").getAsString(),
            obj.get("subject").getAsString(),
            obj.get("status").getAsString(),
            obj.get("created_by").getAsString(),
            obj.has("assigned_to") && !obj.get("assigned_to").isJsonNull() ? obj.get("assigned_to").getAsString() : null,
            obj.has("closed_by") && !obj.get("closed_by").isJsonNull() ? obj.get("closed_by").getAsString() : null,
            obj.has("created_at") && !obj.get("created_at").isJsonNull() ? 
                java.time.Instant.parse(obj.get("created_at").getAsString()).toEpochMilli() : System.currentTimeMillis()
        );
    }
    
    private List<TicketMessage> parseTicketMessages(String jsonResponse) {
        List<TicketMessage> messages = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                TicketMessage msg = new TicketMessage(
                    obj.get("id").getAsString(),
                    obj.get("ticket_id").getAsString(),
                    obj.get("sender").getAsString(),
                    obj.get("message").getAsString(),
                    obj.has("created_at") && !obj.get("created_at").isJsonNull() ? 
                        java.time.Instant.parse(obj.get("created_at").getAsString()).toEpochMilli() : System.currentTimeMillis(),
                    obj.get("is_support").getAsBoolean()
                );
                messages.add(msg);
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing ticket messages: " + e.getMessage());
        }
        return messages;
    }
    
    // ==================== DATA CLASSES ====================
    
    public static class AdminInfo {
        public final String username;
        public final String role;
        public final boolean isAdmin;
        
        public AdminInfo(String username, String role, boolean isAdmin) {
            this.username = username;
            this.role = role;
            this.isAdmin = isAdmin;
        }
        
        public boolean canBanPermanent() {
            return "admin".equals(role) || "super_admin".equals(role);
        }
        
        public boolean canManageAdmins() {
            return "super_admin".equals(role);
        }
        
        public boolean canDeleteListings() {
            return "admin".equals(role) || "super_admin".equals(role);
        }
    }
    
    public static class UserBan {
        public final String id;
        public final String username;
        public final String banType;
        public final String reason;
        public final String bannedBy;
        public final Long expiresAt;
        public final boolean isActive;
        
        public UserBan(String id, String username, String banType, String reason, 
                       String bannedBy, Long expiresAt, boolean isActive) {
            this.id = id;
            this.username = username;
            this.banType = banType;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.expiresAt = expiresAt;
            this.isActive = isActive;
        }
        
        public boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() > expiresAt;
        }
    }
    
    public static class SupportTicket {
        public final String id;
        public final String ticketNumber;
        public final String subject;
        public final String status;
        public final String createdBy;
        public final String assignedTo;
        public final String closedBy;
        public final long createdAt;
        
        public SupportTicket(String id, String ticketNumber, String subject, String status,
                            String createdBy, String assignedTo, String closedBy, long createdAt) {
            this.id = id;
            this.ticketNumber = ticketNumber;
            this.subject = subject;
            this.status = status;
            this.createdBy = createdBy;
            this.assignedTo = assignedTo;
            this.closedBy = closedBy;
            this.createdAt = createdAt;
        }
    }
    
    public static class TicketMessage {
        public final String id;
        public final String ticketId;
        public final String sender;
        public final String message;
        public final long timestamp;
        public final boolean isSupport;
        
        public TicketMessage(String id, String ticketId, String sender, String message, 
                            long timestamp, boolean isSupport) {
            this.id = id;
            this.ticketId = ticketId;
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
            this.isSupport = isSupport;
        }
    }
    
    /**
     * Проверяет, есть ли администраторы/поддержка онлайн.
     * Проверяет поле last_active в таблице admins. 
     * Если поля нет - считаем что поддержка доступна если есть хотя бы один админ.
     */
    public void checkSupportOnline(Consumer<Boolean> onResult) {
        // Проверяем есть ли хотя бы один админ с недавней активностью
        HttpRequest request = createRequest("/admin/online")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonElement element = JsonParser.parseString(response.body());
                            JsonObject obj;
                            
                            if (element.isJsonArray()) {
                                JsonArray array = element.getAsJsonArray();
                                if (array.size() > 0) {
                                    obj = array.get(0).getAsJsonObject();
                                } else {
                                    onResult.accept(false);
                                    return;
                                }
                            } else {
                                obj = element.getAsJsonObject();
                            }
                            
                            // Edge Function возвращает {"online": true/false}
                            if (obj.has("online")) {
                                boolean online = obj.get("online").getAsBoolean();
                                TradeMarketMod.LOGGER.debug("Support online status from API: " + online);
                                onResult.accept(online);
                                return;
                            }
                            
                            // Fallback: проверяем last_active если есть
                            if (obj.has("last_active") && !obj.get("last_active").isJsonNull()) {
                                String timestampStr = obj.get("last_active").getAsString();
                                try {
                                    java.time.Instant lastActive = parseSupabaseTimestamp(timestampStr);
                                    java.time.Instant fiveMinutesAgo = java.time.Instant.now().minusSeconds(300);
                                    onResult.accept(lastActive.isAfter(fiveMinutesAgo));
                                    return;
                                } catch (Exception parseEx) {
                                    TradeMarketMod.LOGGER.error("Error parsing last_active: " + parseEx.getMessage());
                                }
                            }
                            onResult.accept(true);
                        } catch (Exception e) {
                            TradeMarketMod.LOGGER.error("Error parsing support status: " + e.getMessage());
                            onResult.accept(false);
                        }
                    } else {
                        onResult.accept(false);
                    }
                })
                .exceptionally(e -> {
                    onResult.accept(false);
                    return null;
                });
    }
    
    /**
     * Парсит timestamp из Supabase в разных форматах
     */
    private java.time.Instant parseSupabaseTimestamp(String timestampStr) {
        // Убираем микросекунды если есть (оставляем только 3 знака после то��ки)
        if (timestampStr.contains(".")) {
            int dotIndex = timestampStr.indexOf(".");
            int plusIndex = timestampStr.indexOf("+", dotIndex);
            if (plusIndex == -1) plusIndex = timestampStr.indexOf("Z", dotIndex);
            if (plusIndex == -1) plusIndex = timestampStr.length();
            
            String beforeDot = timestampStr.substring(0, dotIndex);
            String afterDot = timestampStr.substring(dotIndex + 1, Math.min(dotIndex + 4, plusIndex));
            String timezone = plusIndex < timestampStr.length() ? timestampStr.substring(plusIndex) : "";
            
            while (afterDot.length() < 3) afterDot += "0";
            timestampStr = beforeDot + "." + afterDot + timezone;
        }
        
        timestampStr = timestampStr.replace("+00:00", "Z").replace("+00", "Z");
        if (!timestampStr.endsWith("Z") && !timestampStr.contains("+")) {
            timestampStr = timestampStr + "Z";
        }
        timestampStr = timestampStr.replace(" ", "T");
        
        return java.time.Instant.parse(timestampStr);
    }
    
    /**
     * Парсит JSON ответ в список MarketListing
     */
    private List<MarketListing> parseListings(String jsonResponse) {
        List<MarketListing> listings = new ArrayList<>();
        
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                UUID listingId = UUID.fromString(obj.get("id").getAsString());
                UUID sellerId = UUID.fromString(obj.get("seller_id").getAsString());
                String sellerName = obj.get("seller_name").getAsString();
                String itemId = obj.get("item_id").getAsString();
                int itemCount = obj.get("item_count").getAsInt();
                String itemNbt = obj.has("item_nbt") && !obj.get("item_nbt").isJsonNull() 
                        ? obj.get("item_nbt").getAsString() : "";
                String itemDisplayName = obj.has("item_display_name") && !obj.get("item_display_name").isJsonNull()
                        ? obj.get("item_display_name").getAsString() : "";
                String price = obj.has("price") && !obj.get("price").isJsonNull()
                        ? obj.get("price").getAsString() : "";
                String description = obj.has("description") && !obj.get("description").isJsonNull()
                        ? obj.get("description").getAsString() : "";
                boolean active = obj.get("active").getAsBoolean();
                
                long createdAt = System.currentTimeMillis();
                if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
                    // Парсим ISO timestamp
                    String timestamp = obj.get("created_at").getAsString();
                    try {
                        createdAt = java.time.Instant.parse(timestamp).toEpochMilli();
                    } catch (Exception e) {
                        // Используем текущее время если не удалось распарсить
                    }
                }
                
                MarketListing listing = new MarketListing(
                        listingId, sellerId, sellerName,
                        itemId, itemCount, itemNbt,
                        itemDisplayName, price, description, createdAt, active
                );
                listings.add(listing);
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing listings: " + e.getMessage());
        }
        
        return listings;
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ ИЗБРАННОГО
    // =====================================================
    
    /**
     * Класс записи избранного
     */
    public static class FavoriteEntry {
        public final UUID id;
        public final UUID listingId;
        public final UUID userId;
        public final long createdAt;
        
        public FavoriteEntry(UUID id, UUID listingId, UUID userId, long createdAt) {
            this.id = id;
            this.listingId = listingId;
            this.userId = userId;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Получить избранное пользователя
     */
    public void fetchFavorites(UUID userId, Consumer<List<UUID>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/favorites?user_id=" + userId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<UUID> listingIds = new ArrayList<>();
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            for (int i = 0; i < array.size(); i++) {
                                JsonElement element = array.get(i);
                                // Поддержка двух форматов: объект с listing_id или просто строка UUID
                                if (element.isJsonObject()) {
                                    JsonObject obj = element.getAsJsonObject();
                                    listingIds.add(UUID.fromString(obj.get("listing_id").getAsString()));
                                } else if (element.isJsonPrimitive()) {
                                    listingIds.add(UUID.fromString(element.getAsString()));
                                }
                            }
                        } catch (Exception e) {
                            TradeMarketMod.LOGGER.error("Error parsing favorites: " + e.getMessage());
                        }
                        onSuccess.accept(listingIds);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Добавить в избранное
     */
    public void addToFavorites(UUID userId, UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("id", UUID.randomUUID().toString());
        json.addProperty("user_id", userId.toString());
        json.addProperty("listing_id", listingId.toString());
        
        HttpRequest request = createRequest("/favorites")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Удалить из избранного
     */
    public void removeFromFavorites(UUID userId, UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/favorites/" + listingId.toString())
                .DELETE()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ ИСТОРИИ ТРАНЗАКЦИЙ
    // =====================================================
    
    /**
     * Класс транзакции
     * status: "pending" - ожидает подтверждения, "confirmed" - подтверждена покупателем
     */
    public static class Transaction {
        public final UUID id;
        public final UUID listingId;
        public final UUID sellerId;
        public final String sellerName;
        public final UUID buyerId;
        public final String buyerName;
        public final String itemId;
        public final String itemDisplayName;
        public final int itemCount;
        public final String price;
        public final String status; // "pending" или "confirmed"
        public final long completedAt;
        
        public Transaction(UUID id, UUID listingId, UUID sellerId, String sellerName,
                          UUID buyerId, String buyerName, String itemId, String itemDisplayName,
                          int itemCount, String price, String status, long completedAt) {
            this.id = id;
            this.listingId = listingId;
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
            this.itemId = itemId;
            this.itemDisplayName = itemDisplayName;
            this.itemCount = itemCount;
            this.price = price;
            this.status = status != null ? status : "pending";
            this.completedAt = completedAt;
        }
        
        public boolean isConfirmed() {
            return "confirmed".equals(status);
        }
        
        public boolean isPending() {
            return "pending".equals(status);
        }
    }
    
    /**
     * Получить историю транзакций пользователя
     */
    public void fetchTransactionHistory(UUID userId, Consumer<List<Transaction>> onSuccess, Consumer<String> onError) {
        // Получаем транзакции где пользователь - продавец или покупатель
        HttpRequest request = createRequest("/transactions?user_id=" + userId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<Transaction> transactions = parseTransactions(response.body());
                        onSuccess.accept(transactions);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Записать транзакцию (статус "pending" - ож��дает подтверждения покупателем)
     */
    public void recordTransaction(UUID listingId, UUID sellerId, String sellerName,
                                  UUID buyerId, String buyerName, String itemId,
                                  String itemDisplayName, int itemCount, String price,
                                  Runnable onSuccess, Consumer<String> onError) {
        
        JsonObject json = new JsonObject();
        json.addProperty("id", UUID.randomUUID().toString());
        json.addProperty("listing_id", listingId.toString());
        json.addProperty("seller_id", sellerId.toString());
        json.addProperty("seller_name", sellerName);
        json.addProperty("buyer_id", buyerId.toString());
        json.addProperty("buyer_name", buyerName);
        json.addProperty("item_id", itemId);
        json.addProperty("item_display_name", itemDisplayName);
        json.addProperty("item_count", itemCount);
        json.addProperty("price", price);
        json.addProperty("status", "pending"); // Ожидает подтверждения покупателем
        
        HttpRequest request = createRequest("/transactions")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    private List<Transaction> parseTransactions(String jsonResponse) {
        List<Transaction> transactions = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                UUID id = UUID.fromString(obj.get("id").getAsString());
                UUID listingId = UUID.fromString(obj.get("listing_id").getAsString());
                UUID sellerId = UUID.fromString(obj.get("seller_id").getAsString());
                String sellerName = obj.get("seller_name").getAsString();
                UUID buyerId = UUID.fromString(obj.get("buyer_id").getAsString());
                String buyerName = obj.get("buyer_name").getAsString();
                String itemId = obj.has("item_id") ? obj.get("item_id").getAsString() : "";
                String itemDisplayName = obj.has("item_display_name") ? obj.get("item_display_name").getAsString() : "";
                int itemCount = obj.has("item_count") ? obj.get("item_count").getAsInt() : 1;
                String price = obj.has("price") ? obj.get("price").getAsString() : "";
                String status = obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "pending";
                
                long completedAt = System.currentTimeMillis();
                if (obj.has("completed_at") && !obj.get("completed_at").isJsonNull()) {
                    try {
                        completedAt = java.time.Instant.parse(obj.get("completed_at").getAsString()).toEpochMilli();
                    } catch (Exception e) { }
                }
                
                transactions.add(new Transaction(id, listingId, sellerId, sellerName,
                        buyerId, buyerName, itemId, itemDisplayName, itemCount, price, status, completedAt));
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing transactions: " + e.getMessage());
        }
        return transactions;
    }
    
    /**
     * Подтвердить транзакцию покупателем (меняет статус на "confirmed")
     */
    public void confirmTransaction(UUID transactionId, UUID buyerId, Runnable onSuccess, Consumer<String> onError) {
        // Проверяем что транзакция принадлежит этому покупателю
        HttpRequest request = createRequest("/transactions/" + transactionId.toString() + "/confirm")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить ожидающие подтверждения транзакции для покупателя
     */
    public void fetchPendingTransactions(UUID buyerId, Consumer<List<Transaction>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/transactions?buyer_id=" + buyerId.toString() + "&status=pending")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<Transaction> transactions = parseTransactions(response.body());
                        onSuccess.accept(transactions);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Проверить, есть ли подтвержденная транзакция на конкретный лот
     * (нужно для возможности оценки продавца)
     */
    public void hasConfirmedTransaction(UUID buyerId, UUID sellerId, UUID listingId, Consumer<Boolean> onResult, Consumer<String> onError) {
        HttpRequest request = createRequest("/transactions/check?buyer_id=" + buyerId.toString() + 
                "&seller_id=" + sellerId.toString() + 
                "&listing_id=" + listingId.toString() +
                "&status=confirmed")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            onResult.accept(array.size() > 0);
                        } catch (Exception e) {
                            onResult.accept(false);
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Проверить, существует ли уже транзакция (pending или completed) для данного лота от данного покупателя
     * Это предотвращает абуз - покупатель может начать сделку на товар только один раз
     */
    public void checkPendingTransactionExists(UUID listingId, UUID buyerId, 
                                              Consumer<Boolean> onResult, Consumer<String> onError) {
        // Проверяем и pending и completed транзакции - чтобы предотвратить повторные сделки
        HttpRequest request = createRequest("/transactions/exists?listing_id=" + listingId.toString() + 
                "&buyer_id=" + buyerId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            onResult.accept(array.size() > 0);
                        } catch (Exception e) {
                            onResult.accept(false);
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить ожидающие подтверждения транзакции для ПРОДАВЦА (где он является seller)
     */
    public void fetchSellerPendingTransactions(UUID sellerId, Consumer<List<Transaction>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/transactions?seller_id=" + sellerId.toString() + "&status=pending")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<Transaction> transactions = parseTransactions(response.body());
                        onSuccess.accept(transactions);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ УВЕДОМЛЕНИЙ
    // =====================================================
    
    /**
     * Класс уведомлен��я
     */
    public static class Notification {
        public final UUID id;
        public final UUID userId;
        public final String type; // "message", "sold", "offer"
        public final String title;
        public final String message;
        public final UUID relatedListingId;
        public final boolean isRead;
        public final long createdAt;
        
        public Notification(UUID id, UUID userId, String type, String title, String message,
                           UUID relatedListingId, boolean isRead, long createdAt) {
            this.id = id;
            this.userId = userId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.relatedListingId = relatedListingId;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Получить уведомления пользователя
     */
    public void fetchNotifications(UUID userId, Consumer<List<Notification>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/notifications?user_id=" + userId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        List<Notification> notifications = parseNotifications(response.body());
                        onSuccess.accept(notifications);
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Создать уведомление
     */
    public void createNotification(UUID userId, String type, String title, String message,
                                   UUID relatedListingId, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("id", UUID.randomUUID().toString());
        json.addProperty("user_id", userId.toString());
        json.addProperty("type", type);
        json.addProperty("title", title);
        json.addProperty("message", message);
        if (relatedListingId != null) {
            json.addProperty("related_listing_id", relatedListingId.toString());
        }
        json.addProperty("is_read", false);
        
        HttpRequest request = createRequest("/notifications")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        if (onError != null) onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    if (onError != null) onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Пометить уведомление как прочитанное
     */
    public void markNotificationRead(UUID notificationId, Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("is_read", true);
        
        HttpRequest request = createRequest("/notifications/" + notificationId.toString())
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        if (onError != null) onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    if (onError != null) onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Удалить все уведомления пользователя
     */
    public void clearAllNotifications(UUID userId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/notifications/clear")
                .DELETE()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        if (onError != null) onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    if (onError != null) onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    private List<Notification> parseNotifications(String jsonResponse) {
        List<Notification> notifications = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                UUID id = UUID.fromString(obj.get("id").getAsString());
                UUID userId = UUID.fromString(obj.get("user_id").getAsString());
                String type = obj.has("type") ? obj.get("type").getAsString() : "message";
                String title = obj.has("title") ? obj.get("title").getAsString() : "";
                String message = obj.has("message") ? obj.get("message").getAsString() : "";
                UUID relatedListingId = null;
                if (obj.has("related_listing_id") && !obj.get("related_listing_id").isJsonNull()) {
                    relatedListingId = UUID.fromString(obj.get("related_listing_id").getAsString());
                }
                boolean isRead = obj.has("is_read") && obj.get("is_read").getAsBoolean();
                
                long createdAt = System.currentTimeMillis();
                if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
                    try {
                        createdAt = java.time.Instant.parse(obj.get("created_at").getAsString()).toEpochMilli();
                    } catch (Exception e) { }
                }
                
                notifications.add(new Notification(id, userId, type, title, message,
                        relatedListingId, isRead, createdAt));
            }
        } catch (Exception e) {
            TradeMarketMod.LOGGER.error("Error parsing notifications: " + e.getMessage());
        }
        return notifications;
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ РЕПУТАЦИИ ПРОДАВЦОВ
    // =====================================================
    
    /**
     * Класс рейтинга
     */
    public static class SellerRating {
        public final UUID id;
        public final UUID sellerId;
        public final String sellerName;
        public final UUID raterId;
        public final String raterName;
        public final int rating; // 1-5
        public final String comment;
        public final long createdAt;
        
        public SellerRating(UUID id, UUID sellerId, String sellerName, UUID raterId, String raterName,
                           int rating, String comment, long createdAt) {
            this.id = id;
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.raterId = raterId;
            this.raterName = raterName;
            this.rating = rating;
            this.comment = comment;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Класс агрегированной репутации продавца
     */
    public static class SellerReputation {
        public final UUID sellerId;
        public final String sellerName;
        public final double averageRating;
        public final int totalRatings;
        public final int successfulTrades;
        
        public SellerReputation(UUID sellerId, String sellerName, double averageRating, 
                               int totalRatings, int successfulTrades) {
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.averageRating = averageRating;
            this.totalRatings = totalRatings;
            this.successfulTrades = successfulTrades;
        }
        
        public String getRatingStars() {
            int fullStars = (int) averageRating;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fullStars; i++) sb.append("\u2605"); // Filled star
            for (int i = fullStars; i < 5; i++) sb.append("\u2606"); // Empty star
            return sb.toString();
        }
    }
    
    /**
     * Получить репутацию продавца
     */
    public void fetchSellerReputation(UUID sellerId, Consumer<SellerReputation> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/ratings?seller_id=" + sellerId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            final int count = array.size();
                            double totalRating = 0;
                            String foundSellerName = "";
                            
                            for (int i = 0; i < count; i++) {
                                JsonObject obj = array.get(i).getAsJsonObject();
                                totalRating += obj.get("rating").getAsInt();
                                if (foundSellerName.isEmpty() && obj.has("seller_name")) {
                                    foundSellerName = obj.get("seller_name").getAsString();
                                }
                            }
                            
                            final double avgRating = count > 0 ? totalRating / count : 0;
                            final String finalSellerName = foundSellerName;
                            
                            // Получаем количество успешных сделок
                            fetchTransactionCount(sellerId, successfulTrades -> {
                                onSuccess.accept(new SellerReputation(sellerId, finalSellerName, avgRating, count, successfulTrades));
                            }, error -> {
                                onSuccess.accept(new SellerReputation(sellerId, finalSellerName, avgRating, count, 0));
                            });
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить количество успешных (подтвержденных) сделок продавца
     */
    private void fetchTransactionCount(UUID sellerId, Consumer<Integer> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/transactions/count?seller_id=" + sellerId.toString() + "&status=confirmed")
                .header("Prefer", "count=exact")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            onSuccess.accept(array.size());
                        } catch (Exception e) {
                            onSuccess.accept(0);
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Добавить оценку продавцу (только если была подтвержденная сделка на конкретный лот)
     */
    public void rateSeller(UUID sellerId, String sellerName, UUID raterId, String raterName, UUID listingId,
                          int rating, String comment, Runnable onSuccess, Consumer<String> onError) {
        // Сначала проверяем, была ли подтвержденная сделка на этот конкретный лот
        hasConfirmedTransaction(raterId, sellerId, listingId, hasTransaction -> {
            if (!hasTransaction) {
                onError.accept(com.trademarket.client.LocalizationManager.getInstance().get("need_confirmed_transaction"));
                return;
            }
            
                // Проверяем, не оценивал ли уже этот пользователь
                checkIfAlreadyRated(sellerId, raterId, alreadyRated -> {
                if (alreadyRated) {
                    onError.accept(com.trademarket.client.LocalizationManager.getInstance().get("already_rated"));
                    return;
                }
            
            JsonObject json = new JsonObject();
            json.addProperty("id", UUID.randomUUID().toString());
            json.addProperty("seller_id", sellerId.toString());
            json.addProperty("seller_name", sellerName);
            json.addProperty("rater_id", raterId.toString());
            json.addProperty("rater_name", raterName);
            json.addProperty("rating", rating);
            json.addProperty("comment", comment != null ? comment : "");
            
            HttpRequest request = createRequest("/ratings")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() == 201 || response.statusCode() == 200) {
                                onSuccess.run();
                            } else {
                                onError.accept("HTTP " + response.statusCode());
                            }
                        })
                        .exceptionally(e -> {
                            onError.accept("Connection error: " + e.getMessage());
                            return null;
                        });
            }, onError);
        }, onError);
    }
    
    /**
     * Проверить, оценивал ли польз��в��тель уже этого продавца
     */
    public void checkIfAlreadyRated(UUID sellerId, UUID raterId, Consumer<Boolean> onResult, Consumer<String> onError) {
        HttpRequest request = createRequest("/ratings/check?seller_id=" + sellerId.toString() + "&rater_id=" + raterId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            onResult.accept(array.size() > 0);
                        } catch (Exception e) {
                            onResult.accept(false);
                        }
                    } else {
                        onResult.accept(false);
                    }
                })
                .exceptionally(e -> {
                    onResult.accept(false);
                    return null;
                });
    }
    
    /**
     * Получить количество непрочитанных уведомлений
     */
    public void fetchUnreadNotificationCount(UUID userId, Consumer<Integer> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/notifications/unread?user_id=" + userId.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            onSuccess.accept(array.size());
                        } catch (Exception e) {
                            onSuccess.accept(0);
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // ==================== USER LOGS (Админ-функции) ====================
    
    /**
     * Получить логи пользователей (только для админов)
     */
    public void getUserLogs(int limit, int offset, String level, String category, String playerName,
                           Consumer<JsonObject> onSuccess, Consumer<String> onError) {
        StringBuilder url = new StringBuilder("/logs?limit=" + limit + "&offset=" + offset);
        if (level != null && !level.isEmpty()) {
            url.append("&level=").append(level);
        }
        if (category != null && !category.isEmpty()) {
            url.append("&category=").append(category);
        }
        if (playerName != null && !playerName.isEmpty()) {
            url.append("&player_name=").append(playerName);
        }
        
        HttpRequest request = createRequest(url.toString())
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                            onSuccess.accept(result);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // ==================== ONLINE USERS & ADMIN CHATS ====================
    
    /**
     * Данные об онлайн пользователе
     */
    public static class OnlineUser {
        public final String playerUUID;
        public final String playerName;
        public final String serverIP;
        public final String modVersion;
        public final String lastHeartbeat;
        
        public OnlineUser(String playerUUID, String playerName, String serverIP, String modVersion, String lastHeartbeat) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.serverIP = serverIP;
            this.modVersion = modVersion;
            this.lastHeartbeat = lastHeartbeat;
        }
    }
    
    /**
     * Данные о чате админ-юзер
     */
    public static class AdminUserChat {
        public final String id;
        public final String adminName;
        public final String userUuid;
        public final String userName;
        public final String updatedAt;
        public int unreadCount;
        
        public AdminUserChat(String id, String adminName, String userUuid, String userName, String updatedAt, int unreadCount) {
            this.id = id;
            this.adminName = adminName;
            this.userUuid = userUuid;
            this.userName = userName;
            this.updatedAt = updatedAt;
            this.unreadCount = unreadCount;
        }
    }
    
    /**
     * Сообщение в чате админ-юзер
     */
    public static class AdminUserMessage {
        public final String id;
        public final String chatId;
        public final String senderName;
        public final String senderType; // "admin" или "user"
        public final String content;
        public final boolean isRead;
        public final String createdAt;
        
        public AdminUserMessage(String id, String chatId, String senderName, String senderType, 
                               String content, boolean isRead, String createdAt) {
            this.id = id;
            this.chatId = chatId;
            this.senderName = senderName;
            this.senderType = senderType;
            this.content = content;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Отправить heartbeat для отслеживания онлайн статуса пользователя
     */
    public void sendUserHeartbeat(String playerUUID, String playerName, String serverIP, String modVersion,
                                  Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("player_uuid", playerUUID);
        json.addProperty("player_name", playerName);
        json.addProperty("server_ip", serverIP != null ? serverIP : "");
        json.addProperty("mod_version", modVersion != null ? modVersion : "");
        
        HttpRequest request = createRequest("/users/heartbeat")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить список онлайн пользователей
     */
    public void getOnlineUsers(Consumer<List<OnlineUser>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/users/online")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            List<OnlineUser> users = new ArrayList<>();
                            
                            for (int i = 0; i < array.size(); i++) {
                                JsonObject obj = array.get(i).getAsJsonObject();
                                users.add(new OnlineUser(
                                    getJsonString(obj, "player_uuid"),
                                    getJsonString(obj, "player_name"),
                                    getJsonString(obj, "server_ip"),
                                    getJsonString(obj, "mod_version"),
                                    getJsonString(obj, "last_heartbeat")
                                ));
                            }
                            
                            onSuccess.accept(users);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Создать или получить чат с пользователем
     */
    public void createOrGetAdminChat(String adminName, String userUuid, String userName,
                                     Consumer<AdminUserChat> onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("admin_name", adminName);
        json.addProperty("user_uuid", userUuid);
        json.addProperty("user_name", userName);
        
        HttpRequest request = createRequest("/admin-chats")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        try {
                            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                            AdminUserChat chat = new AdminUserChat(
                                getJsonString(obj, "id"),
                                getJsonString(obj, "admin_name"),
                                getJsonString(obj, "user_uuid"),
                                getJsonString(obj, "user_name"),
                                getJsonString(obj, "updated_at"),
                                0
                            );
                            onSuccess.accept(chat);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить сообщения чата
     */
    public void getAdminChatMessages(String chatId, Consumer<List<AdminUserMessage>> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/admin-chats/" + chatId + "/messages")
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            List<AdminUserMessage> messages = new ArrayList<>();
                            
                            for (int i = 0; i < array.size(); i++) {
                                JsonObject obj = array.get(i).getAsJsonObject();
                                messages.add(new AdminUserMessage(
                                    getJsonString(obj, "id"),
                                    getJsonString(obj, "chat_id"),
                                    getJsonString(obj, "sender_name"),
                                    getJsonString(obj, "sender_type"),
                                    getJsonString(obj, "content"),
                                    obj.has("is_read") && obj.get("is_read").getAsBoolean(),
                                    getJsonString(obj, "created_at")
                                ));
                            }
                            
                            onSuccess.accept(messages);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Отправить сообщение в чат админ-юзер
     */
    public void sendAdminChatMessage(String chatId, String senderName, String senderType, String content,
                                     Runnable onSuccess, Consumer<String> onError) {
        JsonObject json = new JsonObject();
        json.addProperty("sender_name", senderName);
        json.addProperty("sender_type", senderType); // "admin" или "user"
        json.addProperty("content", content);
        
        HttpRequest request = createRequest("/admin-chats/" + chatId + "/messages")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить чаты пользователя с админами (для обычных юзеров)
     */
    public void getUserAdminChats(String userUuid, Consumer<JsonArray> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/user-admin-messages?user_uuid=" + userUuid)
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray result = JsonParser.parseString(response.body()).getAsJsonArray();
                            onSuccess.accept(result);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Получить количество непрочитанных сообщений от админов
     */
    public void getUnreadAdminMessagesCount(String userUuid, Consumer<Integer> onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/user-admin-messages/unread?user_uuid=" + userUuid)
                .GET()
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                            int count = obj.has("count") ? obj.get("count").getAsInt() : 0;
                            onSuccess.accept(count);
                        } catch (Exception e) {
                            onError.accept("Parse error: " + e.getMessage());
                        }
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Пометить все сообщения от админа в чате как прочитанные
     */
    public void markAdminMessagesAsRead(String chatId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/user-admin-messages/mark-read?chat_id=" + chatId)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    /**
     * Пометить все сообщения от пользователя как прочитанные (для админа)
     */
    public void markUserMessagesAsReadForAdmin(String chatId, Runnable onSuccess, Consumer<String> onError) {
        HttpRequest request = createRequest("/admin-chats/mark-read?chat_id=" + chatId)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        onSuccess.run();
                    } else {
                        onError.accept("HTTP " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    onError.accept("Connection error: " + e.getMessage());
                    return null;
                });
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Безопасное получение строки из JSON объекта
     */
    private String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString();
    }
}
