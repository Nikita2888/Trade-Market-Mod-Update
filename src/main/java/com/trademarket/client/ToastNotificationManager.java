package com.trademarket.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Менеджер уведомлений в стиле достижений Minecraft.
 * Показывает тосты, которые выезжают справа сверху.
 */
public class ToastNotificationManager {
    
    private static ToastNotificationManager instance;
    
    // Типы уведомлений
    public enum NotificationType {
        NEW_CHAT_MESSAGE,       // Новое сообщение в чате с продавцом
        SUPPORT_REPLY,          // Ответ от поддержки
        DEAL_REQUEST,           // Новый запрос на сделку (для продавца)
        DEAL_ACCEPTED,          // Сделка принята
        DEAL_REJECTED,          // Сделка отклонена
        DEAL_COMPLETED,         // Сделка завершена
        NEW_RATING,             // Новая оценка
        LISTING_SOLD,           // Ваш лот куплен
        PRICE_DROP,             // Снижение цены на избранный лот
        ADMIN_ACTION,           // Действие администратора
        INFO,                   // Общая информация
        WELCOME,                // Приветственное уведомление при входе
        NEW_TICKET,             // Новый тикет (для админов)
        USER_MESSAGE,           // Сообщение от пользователя в тикете (для админов)
        ADMIN_MESSAGE_TO_USER   // Сообщение от админа обычному пользователю
    }
    
    // Класс уведомления
    public static class ToastNotification {
        public final NotificationType type;
        public final String title;
        public final String message;
        public final ItemStack icon;
        public final long createdAt;
        public final long duration;
        
        // Анимация
        public float slideProgress = 0f;  // 0 = скрыто, 1 = полностью видно
        public boolean isEntering = true;
        public boolean isExiting = false;
        
        public ToastNotification(NotificationType type, String title, String message, ItemStack icon, long duration) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.icon = icon != null ? icon : getDefaultIcon(type);
            this.createdAt = System.currentTimeMillis();
            this.duration = duration;
        }
        
        private static ItemStack getDefaultIcon(NotificationType type) {
            return switch (type) {
                case NEW_CHAT_MESSAGE -> new ItemStack(Items.WRITABLE_BOOK);
                case SUPPORT_REPLY -> new ItemStack(Items.ENCHANTED_BOOK);
                case DEAL_REQUEST -> new ItemStack(Items.GOLD_INGOT);
                case DEAL_ACCEPTED -> new ItemStack(Items.EMERALD);
                case DEAL_REJECTED -> new ItemStack(Items.BARRIER);
                case DEAL_COMPLETED -> new ItemStack(Items.DIAMOND);
                case NEW_RATING -> new ItemStack(Items.NETHER_STAR);
                case LISTING_SOLD -> new ItemStack(Items.EMERALD_BLOCK);
                case PRICE_DROP -> new ItemStack(Items.ARROW);
                case ADMIN_ACTION -> new ItemStack(Items.COMMAND_BLOCK);
                case INFO -> new ItemStack(Items.PAPER);
                case WELCOME -> new ItemStack(Items.ENDER_CHEST);
                case NEW_TICKET -> new ItemStack(Items.BELL);
                case USER_MESSAGE -> new ItemStack(Items.PLAYER_HEAD);
                case ADMIN_MESSAGE_TO_USER -> new ItemStack(Items.ENCHANTED_BOOK);
            };
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > duration;
        }
        
        public float getExpirationProgress() {
            long elapsed = System.currentTimeMillis() - createdAt;
            return Math.min(1f, (float) elapsed / duration);
        }
    }
    
    // Активные уведомления
    private final List<ToastNotification> notifications = new ArrayList<>();
    private static final int MAX_VISIBLE = 5;
    private static final long DEFAULT_DURATION = 5000; // 5 секунд
    private static final long LONG_DURATION = 8000; // 8 секунд для важных
    
    // Размеры и позиции
    private static final int TOAST_WIDTH = 200;
    private static final int TOAST_HEIGHT = 36;
    private static final int TOAST_PADDING = 4;
    private static final int TOAST_MARGIN = 4;
    private static final int ICON_SIZE = 24;
    
    // Цвета
    private static final int COLOR_BG = 0xFF1A1510;
    private static final int COLOR_BORDER = 0xFF4A3F30;
    private static final int COLOR_BORDER_HIGHLIGHT = 0xFFD4AF37;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_MESSAGE = 0xFFCCBBA5;
    private static final int COLOR_PROGRESS_BG = 0xFF2A2318;
    private static final int COLOR_PROGRESS = 0xFF4CAF50;
    
    // Текстура фрейма (если будем использовать)
    private static final Identifier TOAST_FRAME = Identifier.of("trademarket", "textures/gui/toast_frame.png");
    
    private ToastNotificationManager() {}
    
    public static ToastNotificationManager getInstance() {
        if (instance == null) {
            instance = new ToastNotificationManager();
        }
        return instance;
    }
    
    /**
     * Показать уведомление
     */
    public void show(NotificationType type, String title, String message) {
        show(type, title, message, null, DEFAULT_DURATION);
    }
    
    public void show(NotificationType type, String title, String message, ItemStack icon) {
        show(type, title, message, icon, DEFAULT_DURATION);
    }
    
    public void show(NotificationType type, String title, String message, ItemStack icon, long duration) {
        // Ограничиваем количество уведомлений
        if (notifications.size() >= MAX_VISIBLE) {
            // Помечаем самое старое на выход
            if (!notifications.isEmpty()) {
                notifications.get(0).isExiting = true;
            }
        }
        
        ToastNotification toast = new ToastNotification(type, title, message, icon, duration);
        notifications.add(toast);
        
        // Проигрываем звук
        playNotificationSound(type);
    }
    
    /**
     * Быстрые методы для конкретных типов уведомлений
     */
    public void showNewChatMessage(String sellerName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.NEW_CHAT_MESSAGE, 
             lang.get("toast_new_message"), 
             lang.get("toast_message_from", sellerName));
    }
    
    public void showSupportReply() {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.SUPPORT_REPLY, 
             lang.get("toast_support_reply"), 
             lang.get("toast_support_answered"),
             null, LONG_DURATION);
    }
    
    public void showDealRequest(String buyerName, String itemName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.DEAL_REQUEST, 
             lang.get("toast_deal_request"), 
             lang.get("toast_deal_request_from", buyerName),
             null, LONG_DURATION);
    }
    
    public void showDealAccepted(String sellerName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.DEAL_ACCEPTED, 
             lang.get("toast_deal_accepted"), 
             lang.get("toast_deal_accepted_by", sellerName),
             null, LONG_DURATION);
    }
    
    public void showDealRejected(String sellerName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.DEAL_REJECTED, 
             lang.get("toast_deal_rejected"), 
             lang.get("toast_deal_rejected_by", sellerName));
    }
    
    public void showDealCompleted(String partnerName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.DEAL_COMPLETED, 
             lang.get("toast_deal_completed"), 
             lang.get("toast_deal_with", partnerName),
             null, LONG_DURATION);
    }
    
    public void showNewRating(int stars) {
        LocalizationManager lang = LocalizationManager.getInstance();
        String starsStr = "★".repeat(stars) + "☆".repeat(5 - stars);
        show(NotificationType.NEW_RATING, 
             lang.get("toast_new_rating"), 
             starsStr);
    }
    
    public void showListingSold(String itemName, String buyerName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.LISTING_SOLD, 
             lang.get("toast_listing_sold"), 
             lang.get("toast_sold_to", buyerName),
             null, LONG_DURATION);
    }
    
    public void showAdminAction(String action) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.ADMIN_ACTION, 
             lang.get("toast_admin_action"), 
             action,
             null, LONG_DURATION);
    }
    
    public void showInfo(String title, String message) {
        show(NotificationType.INFO, title, message);
    }
    
    /**
     * Приветственное уведомление при входе на сервер
     */
    public void showWelcome(String title, String message) {
        show(NotificationType.WELCOME, title, message, null, LONG_DURATION);
    }
    
    /**
     * Уведомление о новом тикете (для админов)
     */
    public void showNewTicketForAdmin(String username, String subject) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.NEW_TICKET,
             lang.get("toast_new_ticket"),
             String.format(lang.get("toast_ticket_from"), username),
             null, LONG_DURATION);
    }
    
    /**
     * Уведомление о новом сообщении от пользователя в тикете (для админов)
     */
    public void showUserMessageForAdmin(String username, String messagePreview) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.USER_MESSAGE,
             lang.get("toast_user_message"),
             String.format(lang.get("toast_message_from"), username),
             null, DEFAULT_DURATION);
    }
    
    /**
     * Уведомление о новом сообщении от админа для обычного пользователя
     */
    public void showAdminMessageToUser(String adminName) {
        LocalizationManager lang = LocalizationManager.getInstance();
        show(NotificationType.ADMIN_MESSAGE_TO_USER,
             lang.get("toast_admin_message"),
             String.format(lang.get("toast_admin_message_from"), adminName),
             null, LONG_DURATION);
    }
    
    /**
     * Проигрываем звук в зависимости от типа
     */
    private void playNotificationSound(NotificationType type) {
        SoundManager sound = SoundManager.getInstance();
        switch (type) {
            case NEW_CHAT_MESSAGE -> sound.playMessageSound();
            case SUPPORT_REPLY -> sound.playToastSound(); // Звук как достижение
            case DEAL_REQUEST, LISTING_SOLD -> sound.playToastSound(); // Важные события - звук достижения
            case DEAL_ACCEPTED, DEAL_COMPLETED -> sound.playSuccessSound();
            case NEW_RATING -> sound.playSuccessSound();
            case DEAL_REJECTED -> sound.playErrorSound();
            case ADMIN_ACTION -> sound.playNotificationSound();
            case WELCOME -> sound.playToastSound(); // Приветственное уведомление - звук достижения
            case NEW_TICKET -> sound.playToastSound(); // Новый тикет - важное событие для админа
            case USER_MESSAGE -> sound.playMessageSound(); // Сообщение от пользователя
            case ADMIN_MESSAGE_TO_USER -> sound.playToastSound(); // Важное сообщение от админа
            default -> sound.playNotificationSound();
        }
    }
    
    /**
     * Обновление анимации (вызывается каждый тик)
     */
    public void tick() {
        Iterator<ToastNotification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            ToastNotification toast = iterator.next();
            
            // Анимация входа
            if (toast.isEntering) {
                toast.slideProgress += 0.15f;
                if (toast.slideProgress >= 1f) {
                    toast.slideProgress = 1f;
                    toast.isEntering = false;
                }
            }
            
            // Проверяем истечение
            if (!toast.isEntering && !toast.isExiting && toast.isExpired()) {
                toast.isExiting = true;
            }
            
            // Анимация выхода
            if (toast.isExiting) {
                toast.slideProgress -= 0.2f;
                if (toast.slideProgress <= 0f) {
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Рендеринг всех уведомлений
     */
    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (notifications.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        int y = 4; // Начальная позиция сверху
        
        for (ToastNotification toast : notifications) {
            // Позиция с учетом анимации скольжения
            float easeProgress = easeOutCubic(toast.slideProgress);
            int slideOffset = (int) ((1f - easeProgress) * (TOAST_WIDTH + 10));
            int x = screenWidth - TOAST_WIDTH - 4 + slideOffset;
            
            // Рисуем тост
            renderToast(context, textRenderer, toast, x, y);
            
            y += TOAST_HEIGHT + TOAST_MARGIN;
        }
    }
    
    /**
     * Рендеринг одного уведомления
     */
    private void renderToast(DrawContext context, TextRenderer textRenderer, ToastNotification toast, int x, int y) {
        // Фон
        context.fill(x, y, x + TOAST_WIDTH, y + TOAST_HEIGHT, COLOR_BG);
        
        // Рамка с подсветкой в зависимости от типа
        int borderColor = getBorderColor(toast.type);
        drawBorder(context, x, y, TOAST_WIDTH, TOAST_HEIGHT, borderColor);
        
        // Иконка
        int iconX = x + TOAST_PADDING;
        int iconY = y + (TOAST_HEIGHT - 16) / 2;
        context.drawItem(toast.icon, iconX, iconY);
        
        // Текст
        int textX = iconX + 20;
        int textY = y + TOAST_PADDING;
        
        // Заголовок (укороченный если нужно)
        String title = truncateText(textRenderer, toast.title, TOAST_WIDTH - 28);
        context.drawText(textRenderer, title, textX, textY, getTitleColor(toast.type), true);
        
        // Сообщение (укороченное)
        String message = truncateText(textRenderer, toast.message, TOAST_WIDTH - 28);
        context.drawText(textRenderer, message, textX, textY + 11, COLOR_MESSAGE, false);
        
        // Прогресс-бар внизу
        float progress = 1f - toast.getExpirationProgress();
        int progressWidth = (int) ((TOAST_WIDTH - 4) * progress);
        int progressY = y + TOAST_HEIGHT - 3;
        context.fill(x + 2, progressY, x + TOAST_WIDTH - 2, progressY + 2, COLOR_PROGRESS_BG);
        if (progressWidth > 0) {
            context.fill(x + 2, progressY, x + 2 + progressWidth, progressY + 2, getProgressColor(toast.type));
        }
    }
    
    /**
     * Рисуем рамку
     */
    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);                    // Верх
        context.fill(x, y + height - 1, x + width, y + height, color);  // Низ
        context.fill(x, y, x + 1, y + height, color);                   // Лево
        context.fill(x + width - 1, y, x + width, y + height, color);   // Право
    }
    
    /**
     * Цвет рамки в зависимости от типа
     */
    private int getBorderColor(NotificationType type) {
        return switch (type) {
            case NEW_CHAT_MESSAGE -> 0xFF64B5F6;  // Голубой
            case SUPPORT_REPLY -> 0xFF9C27B0;    // Фиолетовый
            case DEAL_REQUEST -> 0xFFFFD700;     // Золотой
            case DEAL_ACCEPTED -> 0xFF4CAF50;    // Зеленый
            case DEAL_REJECTED -> 0xFFE57373;    // Красный
            case DEAL_COMPLETED -> 0xFF00BCD4;   // Циан
            case NEW_RATING -> 0xFFFFC107;       // Янтарный
            case LISTING_SOLD -> 0xFF8BC34A;     // Светло-зеленый
            case PRICE_DROP -> 0xFFFF9800;       // Оранжевый
            case ADMIN_ACTION -> 0xFFF44336;     // Красный
            case INFO -> COLOR_BORDER;
            case WELCOME -> 0xFF00E676;          // Яркий зеленый (для приветствия)
            case NEW_TICKET -> 0xFFFF5722;       // Оранжево-красный (важно для админа)
            case USER_MESSAGE -> 0xFF03A9F4;     // Светло-голубой
            case ADMIN_MESSAGE_TO_USER -> 0xFF9C27B0; // Фиолетовый (как SUPPORT_REPLY)
        };
    }
    
    /**
     * Цвет заголовка
     */
    private int getTitleColor(NotificationType type) {
        return switch (type) {
            case DEAL_REJECTED, ADMIN_ACTION -> 0xFFE57373;
            case DEAL_ACCEPTED, DEAL_COMPLETED, LISTING_SOLD -> 0xFF4CAF50;
            default -> COLOR_TITLE;
        };
    }
    
    /**
     * Цвет прогресс-бара
     */
    private int getProgressColor(NotificationType type) {
        return getBorderColor(type);
    }
    
    /**
     * Укорачиваем текст с многоточием
     */
    private String truncateText(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        
        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (textRenderer.getWidth(sb.toString() + c) + ellipsisWidth > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb.toString() + ellipsis;
    }
    
    /**
     * Функция плавности для анимации
     */
    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }
    
    /**
     * Очистить все уведомления
     */
    public void clear() {
        notifications.clear();
    }
    
    /**
     * Проверить, есть ли активные уведомления
     */
    public boolean hasNotifications() {
        return !notifications.isEmpty();
    }
}
