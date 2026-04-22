package com.trademarket.client.screen;

import com.trademarket.TradeMarketMod;
import com.trademarket.client.LocalizationManager;
import com.trademarket.data.MarketDataManager;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.MinecraftClient;

import java.util.UUID;

/**
 * Загрузчик данных для TradeMarketScreen
 * Содержит все методы для загрузки данных из Supabase и обновления состояния
 */
public class DataLoader {

    private final MinecraftClient client;
    private final ScreenState state;

    public DataLoader(MinecraftClient client, ScreenState state) {
        this.client = client;
        this.state = state;
    }

    /**
     * Загружает все данные из Supabase
     */
    public void loadFromSupabase() {
        if (state.isLoading) return;
        
        state.isLoading = true;
        state.lastSupabaseRefresh = System.currentTimeMillis();
        
        // Загружаем админ-информацию
        loadAdminInfo();
        
        // Загружаем блокировки текущего пользователя
        loadUserBans();
        
        // Загружаем тикеты
        loadUserTickets();
        
        // Проверяем статус поддержки
        checkSupportStatus();
        
        MarketDataManager.getInstance().forceRefresh(
            () -> {
                state.isLoading = false;
                state.setStatusMessage(null);
                refreshListings();
            },
            error -> {
                state.isLoading = false;
                state.setStatusMessage(LocalizationManager.getInstance().get("error_connection"));
                TradeMarketMod.LOGGER.error("Supabase error: " + error);
            }
        );
    }

    /**
     * Загружает информацию об админе
     */
    public void loadAdminInfo() {
        if (client.player == null) return;
        String playerName = client.player.getName().getString();
        
        SupabaseClient.getInstance().checkIsAdmin(playerName,
            adminInfo -> {
                state.currentAdminInfo = adminInfo;
                if (adminInfo.isAdmin) {
                    TradeMarketMod.LOGGER.info("Admin logged in: " + playerName + " (" + adminInfo.role + ")");
                    SupabaseClient.getInstance().updateAdminLastActive(playerName);
                    state.lastAdminHeartbeat = System.currentTimeMillis();
                }
            },
            error -> TradeMarketMod.LOGGER.error("Error checking admin status: " + error)
        );
    }

    /**
     * Загружает баны текущего пользователя
     */
    public void loadUserBans() {
        if (client.player == null) return;
        String playerName = client.player.getName().getString();
        
        SupabaseClient.getInstance().checkUserBans(playerName,
            bans -> {
                state.currentUserBans = bans;
                state.isCurrentUserBannedListing = false;
                state.isCurrentUserBannedBuying = false;
                state.isCurrentUserBannedChat = false;
                state.isCurrentUserBannedSupport = false;
                state.isCurrentUserBannedFull = false;
                
                for (SupabaseClient.UserBan ban : bans) {
                    if (ban.isActive && !ban.isExpired()) {
                        switch (ban.banType) {
                            case "listing": state.isCurrentUserBannedListing = true; break;
                            case "buying": state.isCurrentUserBannedBuying = true; break;
                            case "chat": state.isCurrentUserBannedChat = true; break;
                            case "support": state.isCurrentUserBannedSupport = true; break;
                            case "full": state.isCurrentUserBannedFull = true; break;
                        }
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.error("Error loading user bans: " + error)
        );
    }

    /**
     * Загружает тикеты пользователя
     */
    public void loadUserTickets() {
        if (client.player == null) return;
        String playerName = client.player.getName().getString();
        
        // Если админ - загружаем все открытые тикеты
        if (state.currentAdminInfo != null && state.currentAdminInfo.isAdmin) {
            SupabaseClient.getInstance().getAllOpenTickets(
                tickets -> {
                    state.supportTickets.clear();
                    for (SupabaseClient.SupportTicket t : tickets) {
                        state.supportTickets.add(new SupportModels.SupportTicket(t.id, t.subject, t.status, t.createdBy, t.createdAt));
                    }
                },
                error -> TradeMarketMod.LOGGER.error("Error loading tickets: " + error)
            );
        } else {
            // Обычный пользователь - только свои тикеты
            SupabaseClient.getInstance().getUserTickets(playerName,
                tickets -> {
                    state.supportTickets.clear();
                    for (SupabaseClient.SupportTicket t : tickets) {
                        state.supportTickets.add(new SupportModels.SupportTicket(t.id, t.subject, t.status, t.createdBy, t.createdAt));
                    }
                },
                error -> TradeMarketMod.LOGGER.error("Error loading user tickets: " + error)
            );
        }
    }

    /**
     * Проверяет статус онлайн поддержки
     */
    public void checkSupportStatus() {
        state.lastSupportStatusCheck = System.currentTimeMillis();
        SupabaseClient.getInstance().checkSupportOnline(
            online -> state.supportOnline = online
        );
    }

    /**
     * Загружает сообщения чата лота
     */
    public void loadChatMessages(boolean scrollToBottom) {
        if (state.selectedListing == null) return;
        
        state.lastChatRefresh = System.currentTimeMillis();
        final int prevMessageCount = state.chatMessages.size();
        final int maxScrollPrev = Math.max(0, state.chatMessages.size() - state.chatMaxVisible);
        final boolean wasAtBottom = state.chatMessages.isEmpty() || state.chatScrollOffset >= maxScrollPrev - 1;
        
        SupabaseClient.getInstance().fetchMessages(
            state.selectedListing.getListingId(),
            messages -> {
                state.chatMessages = messages;
                
                if (scrollToBottom || (messages.size() > prevMessageCount && wasAtBottom)) {
                    int maxScroll = Math.max(0, messages.size() - state.chatMaxVisible);
                    state.chatScrollOffset = maxScroll;
                }
                
                // Помечаем сообщения как прочитанные
                markMarketChatMessagesAsRead();
            },
            error -> TradeMarketMod.LOGGER.error("Chat load error: " + error)
        );
    }

    /**
     * Помечает сообщения как прочитанные
     */
    private void markMarketChatMessagesAsRead() {
        if (state.selectedListing == null || client.player == null) return;
        
        UUID listingId = state.selectedListing.getListingId();
        SupabaseClient.getInstance().markMarketMessagesAsRead(
            listingId,
            client.player.getUuid(),
            () -> {
                state.listingUnreadCounts.remove(listingId);
                loadUnreadMarketMessagesCount();
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to mark market messages as read: " + error)
        );
    }

    /**
     * Загружает количество непрочитанных сообщений
     */
    public void loadUnreadMarketMessagesCount() {
        if (client.player == null) return;
        
        SupabaseClient.getInstance().getUnreadMarketMessagesCount(
            client.player.getUuid(),
            count -> state.unreadMarketMessagesCount = count,
            error -> TradeMarketMod.LOGGER.debug("Failed to load unread market messages count: " + error)
        );
        
        SupabaseClient.getInstance().getUserActiveChats(
            client.player.getUuid(),
            chats -> {
                state.listingUnreadCounts.clear();
                for (SupabaseClient.ActiveChat chat : chats) {
                    if (chat.unreadCount > 0) {
                        state.listingUnreadCounts.put(chat.listingId, chat.unreadCount);
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load listing unread counts: " + error)
        );
    }

    /**
     * Загружает ожидающие транзакции для покупателя
     */
    public void loadPendingTransactions() {
        if (client.player == null) return;
        
        state.lastPendingTransactionsRefresh = System.currentTimeMillis();
        SupabaseClient.getInstance().fetchPendingTransactions(
            client.player.getUuid(),
            transactions -> state.pendingTransactions = transactions,
            error -> TradeMarketMod.LOGGER.debug("Failed to load pending transactions: " + error)
        );
    }

    /**
     * Загружает запросы на сделку для продавца
     */
    public void loadSellerPendingTransactions() {
        if (client.player == null) return;
        
        SupabaseClient.getInstance().fetchSellerPendingTransactions(
            client.player.getUuid(),
            transactions -> state.sellerPendingTransactions = transactions,
            error -> TradeMarketMod.LOGGER.debug("Failed to load seller pending transactions: " + error)
        );
    }

    /**
     * Загружает сообщения тикета
     */
    public void loadTicketMessages(String ticketId) {
        state.ticketMessages.clear();
        state.supportChatScroll = 0;
        state.lastTicketMessagesRefresh = System.currentTimeMillis();
        
        SupabaseClient.getInstance().getTicketMessages(
            ticketId,
            messages -> {
                state.ticketMessages.clear();
                for (SupabaseClient.TicketMessage msg : messages) {
                    state.ticketMessages.add(new SupportModels.TicketMessage(
                        msg.id,
                        msg.ticketId,
                        msg.sender,
                        msg.message,
                        msg.timestamp,
                        msg.isSupport
                    ));
                }
                state.supportChatScroll = Integer.MAX_VALUE;
            },
            error -> TradeMarketMod.LOGGER.error("Error loading ticket messages: " + error)
        );
    }

    /**
     * Обновляет сообщения тикета без сброса скролла
     */
    public void refreshTicketMessages() {
        if (state.activeTicket == null) return;
        
        state.lastTicketMessagesRefresh = System.currentTimeMillis();
        final int prevMessageCount = state.ticketMessages.size();
        
        SupabaseClient.getInstance().getTicketMessages(
            state.activeTicket.id,
            messages -> {
                if (messages.size() != prevMessageCount) {
                    state.ticketMessages.clear();
                    for (SupabaseClient.TicketMessage msg : messages) {
                        state.ticketMessages.add(new SupportModels.TicketMessage(
                            msg.id,
                            msg.ticketId,
                            msg.sender,
                            msg.message,
                            msg.timestamp,
                            msg.isSupport
                        ));
                    }
                    if (messages.size() > prevMessageCount) {
                        state.supportChatScroll = Integer.MAX_VALUE;
                    }
                }
            },
            error -> {} // Тихо игнорируем ошибки при автообновлении
        );
    }

    /**
     * Загружает онлайн пользователей (для админов)
     */
    public void loadOnlineUsers() {
        state.lastOnlineUsersRefresh = System.currentTimeMillis();
        SupabaseClient.getInstance().getOnlineUsers(
            users -> state.onlineUsers = users,
            error -> TradeMarketMod.LOGGER.debug("Failed to load online users: " + error)
        );
    }

    /**
     * Загружает чаты с админами для пользователя
     */
    public void loadUserAdminChats() {
        if (client.player == null) return;
        
        state.lastUserChatsRefresh = System.currentTimeMillis();
        SupabaseClient.getInstance().getUserAdminChats(
            client.player.getUuid().toString(),
            chatsJson -> {
                state.userAdminChats.clear();
                int totalUnread = 0;
                for (com.google.gson.JsonElement element : chatsJson) {
                    com.google.gson.JsonObject chatObj = element.getAsJsonObject();
                    String id = chatObj.has("id") ? chatObj.get("id").getAsString() : "";
                    String adminName = chatObj.has("admin_name") ? chatObj.get("admin_name").getAsString() : "";
                    String userUuid = chatObj.has("user_uuid") ? chatObj.get("user_uuid").getAsString() : "";
                    String userName = chatObj.has("user_name") ? chatObj.get("user_name").getAsString() : "";
                    String lastMessage = chatObj.has("last_message") ? chatObj.get("last_message").getAsString() : "";
                    int unreadCount = chatObj.has("unread_count") ? chatObj.get("unread_count").getAsInt() : 0;
                    long updatedAt = chatObj.has("updated_at") ? chatObj.get("updated_at").getAsLong() : 0L;
                    
                    state.userAdminChats.add(new SupportModels.AdminChatForUser(
                        id,
                        adminName,
                        userUuid,
                        userName,
                        lastMessage,
                        unreadCount,
                        updatedAt
                    ));
                    totalUnread += unreadCount;
                }
                state.unreadAdminMessagesCount = totalUnread;
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load user admin chats: " + error)
        );
    }

    /**
     * Обновляет отображаемые листинги
     */
    public void refreshListings() {
        state.lastRefreshTime = System.currentTimeMillis();
        if (state.currentTab == 0) {
            if (!state.searchText.isEmpty() || state.currentSortMode != 0) {
                state.displayedListings = MarketDataManager.getInstance().getFilteredListings();
            } else {
                state.displayedListings = MarketDataManager.getInstance().getActiveListings();
            }
        } else if (state.currentTab == 1 && client.player != null) {
            state.displayedListings = MarketDataManager.getInstance()
                    .getListingsBySeller(client.player.getUuid());
        } else if (state.currentTab == 3) {
            state.displayedListings = MarketDataManager.getInstance().getFavoriteListings();
        }
    }

    /**
     * Проверяет, есть ли уже сделка на выбранный лот
     */
    public void checkExistingDealOnListing() {
        if (state.selectedListing == null || client.player == null) return;
        
        if (state.selectedListing.getSellerId().equals(client.player.getUuid())) return;
        
        SupabaseClient.getInstance().checkPendingTransactionExists(
            state.selectedListing.getListingId(),
            client.player.getUuid(),
            exists -> state.hasExistingDealOnListing = exists,
            error -> state.hasExistingDealOnListing = false
        );
    }
}
