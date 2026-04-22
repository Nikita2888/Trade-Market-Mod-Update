package com.trademarket.client;

import com.trademarket.TradeMarketMod;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Фоновая проверка уведомлений для показа тостов даже когда GUI закрыт.
 * Проверяет новые сообщения в тикетах поддержки, чатах и новые запросы на сделку.
 */
public class NotificationChecker {
    
    private static NotificationChecker instance;
    
    // Интервалы проверки (в миллисекундах)
    private static final long CHECK_INTERVAL = 30000; // 30 секунд
    private static final long SUPPORT_CHECK_INTERVAL = 15000; // 15 секунд для поддержки
    private static final long CHAT_CHECK_INTERVAL = 5000; // 5 секунд для чатов
    private static final long ADMIN_CHECK_INTERVAL = 10000; // 10 секунд для админ-проверок
    private static final long ADMIN_MESSAGES_CHECK_INTERVAL = 10000; // 10 секунд для проверки сообщений от админов
    
    // Последние проверки
    private long lastSupportCheck = 0;
    private long lastDealCheck = 0;
    private long lastChatCheck = 0;
    private long lastAdminCheck = 0;
    private long lastAdminMessagesCheck = 0;
    
    // Отслеживаем уже показанные уведомления
    private final Set<String> shownSupportMessageIds = new HashSet<>();
    private final Set<UUID> shownDealRequestIds = new HashSet<>();
    private final Set<UUID> shownChatMessageIds = new HashSet<>();
    private final Set<String> shownAdminTicketIds = new HashSet<>();
    private final Set<String> shownAdminUserMessageIds = new HashSet<>();
    private final Set<String> shownAdminMessagesToUserIds = new HashSet<>(); // Для сообщений от админов обычным юзерам
    
    // Флаг инициализации чатов пользователя с админами
    private boolean userAdminChatsInitialized = false;
    
    // Счетчики для отслеживания изменений
    private int lastKnownDealRequestCount = -1;
    private int lastKnownTicketMessageCount = -1;
    private String currentActiveTicketId = null;
    
    // Флаг первой загрузки чатов
    private boolean chatInitialized = false;
    private boolean adminTicketsInitialized = false;
    
    // Кеширование статуса админа
    private Boolean isAdmin = null;
    private long lastAdminStatusCheck = 0;
    private static final long ADMIN_STATUS_CACHE_TIME = 60000; // 1 минута
    
    private boolean isEnabled = true;
    
    private NotificationChecker() {}
    
    public static NotificationChecker getInstance() {
        if (instance == null) {
            instance = new NotificationChecker();
        }
        return instance;
    }
    
    /**
     * Включить/выключить фоновую проверку
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }
    
    /**
     * Вызывается каждый тик для проверки уведомлений
     */
    public void tick() {
        if (!isEnabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        // Проверяем что SupabaseClient готов к отправке запросов (UUID игрока установлен)
        if (!SupabaseClient.getInstance().isReady()) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Проверяем запросы на сделку (для продавцов)
        if (currentTime - lastDealCheck > CHECK_INTERVAL) {
            checkDealRequests(client);
            lastDealCheck = currentTime;
        }
        
        // Проверяем ответы поддержки
        if (currentTime - lastSupportCheck > SUPPORT_CHECK_INTERVAL) {
            checkSupportReplies(client);
            lastSupportCheck = currentTime;
        }
        
        // Проверяем новые сообщения в чатах
        if (currentTime - lastChatCheck > CHAT_CHECK_INTERVAL) {
            checkChatMessages(client);
            lastChatCheck = currentTime;
        }
        
        // Проверяем уведомления для админов
        if (currentTime - lastAdminCheck > ADMIN_CHECK_INTERVAL) {
            checkAdminNotifications(client);
            lastAdminCheck = currentTime;
        }
        
        // Проверяем новые сообщения от админов (для обычных пользователей)
        if (currentTime - lastAdminMessagesCheck > ADMIN_MESSAGES_CHECK_INTERVAL) {
            checkAdminMessagesToUser(client);
            lastAdminMessagesCheck = currentTime;
        }
    }
    
    /**
     * Проверка новых сообщений от админов для обычных пользователей
     */
    private void checkAdminMessagesToUser(MinecraftClient client) {
        if (client.player == null) return;
        
        // Не проверяем для админов - у них своя система
        if (Boolean.TRUE.equals(isAdmin)) return;
        
        String userUuid = client.player.getUuidAsString();
        
        SupabaseClient.getInstance().getUserAdminChats(userUuid,
            chatsJson -> {
                // Первая загрузка - запоминаем все сообщения
                if (!userAdminChatsInitialized) {
                    for (int i = 0; i < chatsJson.size(); i++) {
                        com.google.gson.JsonObject chatObj = chatsJson.get(i).getAsJsonObject();
                        if (chatObj.has("last_message") && !chatObj.get("last_message").isJsonNull()) {
                            com.google.gson.JsonObject lastMsg = chatObj.get("last_message").getAsJsonObject();
                            if (lastMsg.has("id")) {
                                shownAdminMessagesToUserIds.add(lastMsg.get("id").getAsString());
                            }
                        }
                    }
                    userAdminChatsInitialized = true;
                    return;
                }
                
                // Проверяем новые сообщения
                for (int i = 0; i < chatsJson.size(); i++) {
                    com.google.gson.JsonObject chatObj = chatsJson.get(i).getAsJsonObject();
                    
                    // Проверяем есть ли непрочитанные
                    int unreadCount = chatObj.has("unread_count") ? chatObj.get("unread_count").getAsInt() : 0;
                    if (unreadCount <= 0) continue;
                    
                    // Получаем последнее сообщение
                    if (chatObj.has("last_message") && !chatObj.get("last_message").isJsonNull()) {
                        com.google.gson.JsonObject lastMsg = chatObj.get("last_message").getAsJsonObject();
                        String msgId = lastMsg.has("id") ? lastMsg.get("id").getAsString() : "";
                        String senderType = lastMsg.has("sender_type") ? lastMsg.get("sender_type").getAsString() : "";
                        
                        // Показываем уведомление только если сообщение от админа и мы его еще не показывали
                        if ("admin".equals(senderType) && !msgId.isEmpty() && !shownAdminMessagesToUserIds.contains(msgId)) {
                            String adminName = chatObj.has("admin_name") ? chatObj.get("admin_name").getAsString() : "Support";
                            ToastNotificationManager.getInstance().showAdminMessageToUser(adminName);
                            shownAdminMessagesToUserIds.add(msgId);
                        }
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.debug("NotificationChecker: Failed to check admin messages: " + error)
        );
    }
    
    /**
     * Проверка уведомлений для администраторов
     */
    private void checkAdminNotifications(MinecraftClient client) {
        if (client.player == null) return;
        
        String playerName = client.player.getName().getString();
        long currentTime = System.currentTimeMillis();
        
        // Проверяем статус админа (с кешированием)
        if (isAdmin == null || currentTime - lastAdminStatusCheck > ADMIN_STATUS_CACHE_TIME) {
            SupabaseClient.getInstance().checkIsAdmin(
                playerName,
                adminInfo -> {
                    isAdmin = adminInfo.isAdmin;
                    lastAdminStatusCheck = System.currentTimeMillis();
                    
                    if (adminInfo.isAdmin) {
                        // Проверяем новые тикеты и сообщения от пользователей
                        checkNewTicketsForAdmin();
                        checkNewUserMessagesForAdmin();
                    }
                },
                error -> TradeMarketMod.LOGGER.debug("NotificationChecker: Failed to check admin status: " + error)
            );
        } else if (Boolean.TRUE.equals(isAdmin)) {
            // Уже знаем что админ - проверяем уведомления
            checkNewTicketsForAdmin();
            checkNewUserMessagesForAdmin();
        }
    }
    
    /**
     * Проверка новых тикетов (для админов)
     */
    private void checkNewTicketsForAdmin() {
        SupabaseClient.getInstance().getAllOpenTickets(
            tickets -> {
                // Первая загрузка - запоминаем все тикеты
                if (!adminTicketsInitialized) {
                    for (SupabaseClient.SupportTicket ticket : tickets) {
                        shownAdminTicketIds.add(ticket.id);
                    }
                    adminTicketsInitialized = true;
                    return;
                }
                
                // Проверяем новые тикеты
                for (SupabaseClient.SupportTicket ticket : tickets) {
                    if (!shownAdminTicketIds.contains(ticket.id)) {
                        // Показываем уведомление о новом тикете
                        ToastNotificationManager.getInstance().showNewTicketForAdmin(
                            ticket.createdBy, ticket.subject
                        );
                        shownAdminTicketIds.add(ticket.id);
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.debug("NotificationChecker: Failed to check admin tickets: " + error)
        );
    }
    
    /**
     * Проверка новых сообщений от пользователей в тикетах (для админов)
     */
    private void checkNewUserMessagesForAdmin() {
        SupabaseClient.getInstance().getAllOpenTickets(
            tickets -> {
                for (SupabaseClient.SupportTicket ticket : tickets) {
                    // Проверяем сообщения в каждом открытом тикете
                    checkTicketForNewUserMessages(ticket);
                }
            },
            error -> {} // Тихо игнорируем
        );
    }
    
    /**
     * Проверка тикета на новые сообщения от пользователя (не от поддержки)
     */
    private void checkTicketForNewUserMessages(SupabaseClient.SupportTicket ticket) {
        SupabaseClient.getInstance().getTicketMessages(
            ticket.id,
            messages -> {
                for (SupabaseClient.TicketMessage msg : messages) {
                    // Проверяем только сообщения НЕ от поддержки (от пользователя)
                    if (!msg.isSupport && !shownAdminUserMessageIds.contains(msg.id)) {
                        ToastNotificationManager.getInstance().showUserMessageForAdmin(
                            ticket.createdBy, msg.message
                        );
                        shownAdminUserMessageIds.add(msg.id);
                        // Показываем только одно уведомление за раз
                        break;
                    }
                }
            },
            error -> {} // Тихо игнорируем
        );
    }
    
    /**
     * Проверка новых сообщений в чатах с продавцами/покупателями
     */
    private void checkChatMessages(MinecraftClient client) {
        if (client.player == null) return;
        
        UUID playerId = client.player.getUuid();
        
        SupabaseClient.getInstance().getUserActiveChats(
            playerId,
            chats -> {
                // Первая загрузка - просто запоминаем все чаты
                if (!chatInitialized) {
                    for (SupabaseClient.ActiveChat chat : chats) {
                        // Запоминаем ключ чата (listingId + lastSenderName)
                        String chatKey = chat.listingId + "_" + chat.lastSenderName + "_" + chat.lastMessageTime;
                        shownChatMessageIds.add(UUID.nameUUIDFromBytes(chatKey.getBytes()));
                    }
                    chatInitialized = true;
                    return;
                }
                
                // Проверяем новые непрочитанные сообщения
                for (SupabaseClient.ActiveChat chat : chats) {
                    // Если есть непрочитанные сообщения
                    if (chat.unreadCount > 0) {
                        // Создаем уникальный ключ для этого уведомления
                        String chatKey = chat.listingId + "_" + chat.lastSenderName + "_" + chat.lastMessageTime;
                        UUID notificationKey = UUID.nameUUIDFromBytes(chatKey.getBytes());
                        
                        // Если еще не показывали уведомление
                        if (!shownChatMessageIds.contains(notificationKey)) {
                            ToastNotificationManager.getInstance().showNewChatMessage(chat.lastSenderName);
                            shownChatMessageIds.add(notificationKey);
                        }
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.debug("NotificationChecker: Failed to check chats: " + error)
        );
    }
    
    /**
     * Проверка новых запросов на сделку (для продавцов)
     */
    private void checkDealRequests(MinecraftClient client) {
        if (client.player == null) return;
        
        SupabaseClient.getInstance().fetchSellerPendingTransactions(
            client.player.getUuid(),
            transactions -> {
                // Первая загрузка - просто запоминаем количество
                if (lastKnownDealRequestCount < 0) {
                    lastKnownDealRequestCount = transactions.size();
                    for (SupabaseClient.Transaction tx : transactions) {
                        shownDealRequestIds.add(tx.id);
                    }
                    return;
                }
                
                // Проверяем новые запросы
                for (SupabaseClient.Transaction tx : transactions) {
                    if (!shownDealRequestIds.contains(tx.id)) {
                        // Показываем уведомление
                        ToastNotificationManager.getInstance().showDealRequest(
                            tx.buyerName, tx.itemDisplayName
                        );
                        shownDealRequestIds.add(tx.id);
                    }
                }
                
                lastKnownDealRequestCount = transactions.size();
            },
            error -> TradeMarketMod.LOGGER.debug("NotificationChecker: Failed to check deal requests: " + error)
        );
    }
    
    /**
     * Проверка ответов от поддержки
     */
    private void checkSupportReplies(MinecraftClient client) {
        if (client.player == null) return;
        
        String playerName = client.player.getName().getString();
        
        // Получаем открытые тикеты пользователя
        SupabaseClient.getInstance().getUserTickets(
            playerName,
            tickets -> {
                // Проверяем сообщения в каждом открытом тикете
                for (SupabaseClient.SupportTicket ticket : tickets) {
                    if ("open".equals(ticket.status)) {
                        checkTicketForNewSupportMessages(ticket.id);
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.debug("NotificationChecker: Failed to check tickets: " + error)
        );
    }
    
    /**
     * Пров��рка конкретного тикета на новые сообщения от поддержки
     */
    private void checkTicketForNewSupportMessages(String ticketId) {
        SupabaseClient.getInstance().getTicketMessages(
            ticketId,
            messages -> {
                for (SupabaseClient.TicketMessage msg : messages) {
                    // Проверяем только сообщения от поддержки
                    if (msg.isSupport && !shownSupportMessageIds.contains(msg.id)) {
                        ToastNotificationManager.getInstance().showSupportReply();
                        shownSupportMessageIds.add(msg.id);
                        // Показываем только одно уведомление за раз
                        break;
                    }
                }
            },
            error -> {} // Тихо игнорируем ошибки
        );
    }
    
    /**
     * Сбросить кэш (при входе в игру или при открытии GUI)
     */
    public void resetCache() {
        lastKnownDealRequestCount = -1;
        lastKnownTicketMessageCount = -1;
        // НЕ сбрасываем shownSupportMessageIds и shownDealRequestIds
        // чтобы не показывать уже показанные уведомления
    }
    
    /**
     * Полный сброс (при выходе из игры)
     */
    public void fullReset() {
        lastKnownDealRequestCount = -1;
        lastKnownTicketMessageCount = -1;
        shownSupportMessageIds.clear();
        shownDealRequestIds.clear();
        shownChatMessageIds.clear();
        shownAdminTicketIds.clear();
        shownAdminUserMessageIds.clear();
        shownAdminMessagesToUserIds.clear();
        currentActiveTicketId = null;
        chatInitialized = false;
        adminTicketsInitialized = false;
        userAdminChatsInitialized = false;
        isAdmin = null;
    }
    
    /**
     * Отметить сообщение как прочитанное (чтобы не показывать повторно)
     */
    public void markSupportMessageAsRead(String messageId) {
        shownSupportMessageIds.add(messageId);
    }
    
    /**
     * Отметить запрос на сделку как прочитанный
     */
    public void markDealRequestAsRead(UUID transactionId) {
        shownDealRequestIds.add(transactionId);
    }
    
    /**
     * Отметить сообщение чата как прочитанное
     */
    public void markChatMessageAsRead(UUID messageId) {
        shownChatMessageIds.add(messageId);
    }
}
