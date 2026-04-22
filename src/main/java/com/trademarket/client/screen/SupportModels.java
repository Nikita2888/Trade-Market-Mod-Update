package com.trademarket.client.screen;

/**
 * Модели данных для системы поддержки
 * Содержит классы для тикетов, сообщений тикетов и чатов с админами
 */
public final class SupportModels {

    private SupportModels() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    /**
     * Тикет поддержки
     */
    public static class SupportTicket {
        public String id;
        public String subject;
        public String status;
        public String createdBy;
        public long createdAt;

        public SupportTicket(String id, String subject, String status, String createdBy, long createdAt) {
            this.id = id;
            this.subject = subject;
            this.status = status;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }
    }

    /**
     * Сообщение в тикете
     */
    public static class TicketMessage {
        public String id;
        public String ticketId;
        public String sender;
        public String message;
        public long timestamp;
        public boolean isSupport;

        public TicketMessage(String id, String ticketId, String sender, String message, long timestamp, boolean isSupport) {
            this.id = id;
            this.ticketId = ticketId;
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
            this.isSupport = isSupport;
        }
    }

    /**
     * Чат с админом для обычного пользователя
     */
    public static class AdminChatForUser {
        public String id;
        public String adminName;
        public String userUuid;
        public String userName;
        public String lastMessage;
        public int unreadCount;
        public long updatedAt;
        
        public AdminChatForUser(String id, String adminName, String userUuid, String userName, 
                                String lastMessage, int unreadCount, long updatedAt) {
            this.id = id;
            this.adminName = adminName;
            this.userUuid = userUuid;
            this.userName = userName;
            this.lastMessage = lastMessage;
            this.unreadCount = unreadCount;
            this.updatedAt = updatedAt;
        }
    }

    /**
     * Транзакция с типом (для отображения в панели pending transactions)
     */
    public static class TransactionWithType {
        public final com.trademarket.data.SupabaseClient.Transaction transaction;
        public final boolean isSellerView; // true если это транзакция для продавца

        public TransactionWithType(com.trademarket.data.SupabaseClient.Transaction t, boolean isSellerView) {
            this.transaction = t;
            this.isSellerView = isSellerView;
        }
    }
}
