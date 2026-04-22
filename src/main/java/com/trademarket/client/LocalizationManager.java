package com.trademarket.client;

import com.trademarket.TradeMarketMod;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Управляет локализацией мода Trade Market.
 * Поддерживает русский (ru) и английский (en) языки.
 */
public class LocalizationManager {
    
    private static LocalizationManager instance;
    
    public static final String LANG_RU = "ru";
    public static final String LANG_EN = "en";
    
    private String currentLanguage = LANG_EN; // По умолчанию английский
    
    private final Map<String, String> ruStrings = new HashMap<>();
    private final Map<String, String> enStrings = new HashMap<>();
    
    // Текстуры флагов
    public static final Identifier TEXTURE_LANG_EN = Identifier.of("trademarket", "textures/gui/lang_en.png");
    public static final Identifier TEXTURE_LANG_RU = Identifier.of("trademarket", "textures/gui/lang_ru.png");
    
    private LocalizationManager() {
        initStrings();
    }
    
    public static LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }
    
    /**
     * Переключает язык между RU и EN
     */
    public void toggleLanguage() {
        if (currentLanguage.equals(LANG_RU)) {
            currentLanguage = LANG_EN;
        } else {
            currentLanguage = LANG_RU;
        }
        TradeMarketMod.LOGGER.info("Language switched to: " + currentLanguage);
    }
    
    /**
     * Устанавливает язык
     */
    public void setLanguage(String lang) {
        if (LANG_RU.equals(lang) || LANG_EN.equals(lang)) {
            currentLanguage = lang;
        }
    }
    
    /**
     * Возвращает текущий язык
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Проверяет, является ли текущий язык русским
     */
    public boolean isRussian() {
        return LANG_RU.equals(currentLanguage);
    }
    
    /**
     * Возвращает текстуру флага для текущего языка (показывает на какой можно переключить)
     */
    public Identifier getNextLanguageTexture() {
        return isRussian() ? TEXTURE_LANG_EN : TEXTURE_LANG_RU;
    }
    
    /**
     * Возвращает текстуру флага текущего языка
     */
    public Identifier getCurrentLanguageTexture() {
        return isRussian() ? TEXTURE_LANG_RU : TEXTURE_LANG_EN;
    }
    
    /**
     * Получить локализованную строку по ключу
     */
    public String get(String key) {
        Map<String, String> strings = isRussian() ? ruStrings : enStrings;
        return strings.getOrDefault(key, key);
    }
    
    /**
     * Получить локализованную строку с подстановкой параметров
     * Использует %s, %d и т.д. для подстановки
     */
    public String get(String key, Object... args) {
        String template = get(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
    
    /**
     * Проверяет, содержит ли сообщение определенные ключевые слова (для определения типа сообщения)
     */
    public boolean isErrorMessage(String message) {
        if (message == null) return false;
        return message.contains(get("error")) || 
               message.contains(get("forbidden")) || 
               message.contains(get("ban")) ||
               message.contains("Error") ||
               message.contains("Ошибка") ||
               message.contains("запрещено") ||
               message.contains("forbidden");
    }
    
    public boolean isSuccessMessage(String message) {
        if (message == null) return false;
        return message.contains(get("success")) || 
               message.contains(get("created")) ||
               message.contains(get("deleted")) ||
               message.contains(get("closed")) ||
               message.contains(get("removed")) ||
               message.contains(get("copied")) ||
               message.contains("успешно") ||
               message.contains("создан") ||
               message.contains("удален") ||
               message.contains("закрыт") ||
               message.contains("сняты") ||
               message.contains("скопирован");
    }
    
    private void initStrings() {
        // ===== ОБЩИЕ =====
        ruStrings.put("loading", "Загрузка...");
        enStrings.put("loading", "Loading...");
        
        ruStrings.put("error", "Ошибка");
        enStrings.put("error", "Error");
        
        ruStrings.put("error_connection", "Ошибка подключения");
        enStrings.put("error_connection", "Connection error");
        
        ruStrings.put("error_generic", "Ошибка: %s");
        enStrings.put("error_generic", "Error: %s");
        
        ruStrings.put("success", "успешно");
        enStrings.put("success", "successfully");
        
        ruStrings.put("created", "создан");
        enStrings.put("created", "created");
        
        ruStrings.put("deleted", "удален");
        enStrings.put("deleted", "deleted");
        
        ruStrings.put("closed", "закрыт");
        enStrings.put("closed", "closed");
        
        ruStrings.put("removed", "сняты");
        enStrings.put("removed", "removed");
        
        ruStrings.put("copied", "скопирован");
        enStrings.put("copied", "copied");
        
        ruStrings.put("forbidden", "запрещено");
        enStrings.put("forbidden", "forbidden");
        
        ruStrings.put("ban", "бан");
        enStrings.put("ban", "ban");
        
        // ===== ЗАГОЛОВКИ И ВКЛАДКИ =====
        ruStrings.put("title", "TRADE MARKET");
        enStrings.put("title", "TRADE MARKET");
        
        ruStrings.put("tab_all_listings", "Все лоты");
        enStrings.put("tab_all_listings", "All Listings");
        
        ruStrings.put("tab_my_listings", "Мои лоты");
        enStrings.put("tab_my_listings", "My Listings");
        
        ruStrings.put("tab_sell", "Продать");
        enStrings.put("tab_sell", "Sell");
        
        ruStrings.put("tab_favorites", "Избранное");
        enStrings.put("tab_favorites", "Favorites");
        
        ruStrings.put("tab_users", "Пользователи");
        enStrings.put("tab_users", "Users");
        
        ruStrings.put("tab_messages", "Чаты");
        enStrings.put("tab_messages", "Chats");
        
        // ===== ВКЛАДКА СООБЩЕНИЙ =====
        ruStrings.put("messages_title", "Чаты с непрочитанными сообщениями");
        enStrings.put("messages_title", "Chats with unread messages");
        
        ruStrings.put("no_unread_messages", "Нет непрочитанных сообщений");
        enStrings.put("no_unread_messages", "No unread messages");
        
        ruStrings.put("unread_count", "%d непрочитанных");
        enStrings.put("unread_count", "%d unread");
        
        ruStrings.put("open_chat", "Открыть чат");
        enStrings.put("open_chat", "Open chat");
        
        // ===== ВКЛАДКА ПОЛЬЗОВАТЕЛЕЙ (АДМИН) =====
        ruStrings.put("online_users", "Онлайн пользователи");
        enStrings.put("online_users", "Online Users");
        
        ruStrings.put("no_online_users", "Нет онлайн пользователей");
        enStrings.put("no_online_users", "No online users");
        
        ruStrings.put("thanks_for_using", "Спасибо, что используете Trade Market!");
        enStrings.put("thanks_for_using", "Thanks for using Trade Market!");
        
        ruStrings.put("write_message", "Написать");
        enStrings.put("write_message", "Write");
        
        ruStrings.put("chat_with_user", "Чат с %s");
        enStrings.put("chat_with_user", "Chat with %s");
        
        ruStrings.put("support_label", "Поддержка");
        enStrings.put("support_label", "Support");
        
        ruStrings.put("messages_from_support", "Сообщения от поддержки");
        enStrings.put("messages_from_support", "Messages from Support");
        
        ruStrings.put("from_support_admin", "От: %s");
        enStrings.put("from_support_admin", "From: %s");
        
        ruStrings.put("chat_with_support", "Чат с поддержкой");
        enStrings.put("chat_with_support", "Chat with Support");
        
        ruStrings.put("admin_only_feature", "Только для админов");
        enStrings.put("admin_only_feature", "Admin only feature");
        
        ruStrings.put("server", "Сервер: %s");
        enStrings.put("server", "Server: %s");
        
        ruStrings.put("mod_version_label", "Версия мода: %s");
        enStrings.put("mod_version_label", "Mod version: %s");
        
        // ===== ДЕТАЛИ ЛОТА =====
        ruStrings.put("listing_details", "ДЕТАЛИ ЛОТА");
        enStrings.put("listing_details", "LISTING DETAILS");
        
        ruStrings.put("back", "< Назад");
        enStrings.put("back", "< Back");
        
        ruStrings.put("seller", "Продавец: %s");
        enStrings.put("seller", "Seller: %s");
        
        ruStrings.put("price", "Цена: %s ");
        enStrings.put("price", "Price: %s ");
        
        ruStrings.put("conditions", "Условия: ");
        enStrings.put("conditions", "Conditions: ");
        
        ruStrings.put("characteristics", "Характеристики");
        enStrings.put("characteristics", "Characteristics");
        
        ruStrings.put("quantity", "Кол-во: %d");
        enStrings.put("quantity", "Qty: %d");
        
        ruStrings.put("no_effects", "Без эффектов");
        enStrings.put("no_effects", "No effects");
        
        // ===== ЧАТ =====
        ruStrings.put("chat_with_seller", "Чат с продавцом:");
        enStrings.put("chat_with_seller", "Chat with seller:");
        
        ruStrings.put("you", "Вы: ");
        enStrings.put("you", "You: ");
        
        ruStrings.put("seller_prefix", "Продавец: ");
        enStrings.put("seller_prefix", "Seller: ");
        
        ruStrings.put("no_messages", "Нет сообщений. Начните диалог!");
        enStrings.put("no_messages", "No messages. Start the conversation!");
        
        ruStrings.put("enter_message", "Введите сообщение...");
        enStrings.put("enter_message", "Enter message...");
        
        ruStrings.put("send", "Отправить");
        enStrings.put("send", "Send");
        
        ruStrings.put("chat_banned", "Вам запрещено писать в чат!");
        enStrings.put("chat_banned", "You are banned from chat!");
        
        // ===== АДМИН-ПАНЕЛЬ =====
        ruStrings.put("admin_panel", "Админ-панель");
        enStrings.put("admin_panel", "Admin Panel");
        
        ruStrings.put("admin", "Админ");
        enStrings.put("admin", "Admin");
        
        ruStrings.put("close", "Закрыть");
        enStrings.put("close", "Close");
        
        ruStrings.put("player", "Игрок: %s");
        enStrings.put("player", "Player: %s");
        
        ruStrings.put("delete_listing", "Удалить лот");
        enStrings.put("delete_listing", "Delete listing");
        
        ruStrings.put("listing_deleted", "Лот удален!");
        enStrings.put("listing_deleted", "Listing deleted!");
        
        ruStrings.put("deleted_by_admin", "Удалено администратором");
        enStrings.put("deleted_by_admin", "Deleted by administrator");
        
        ruStrings.put("ban_issued_by_admin", "Выдано администратором %s");
        enStrings.put("ban_issued_by_admin", "Issued by administrator %s");
        
        ruStrings.put("ban_issued", "Бан выдан: %s");
        enStrings.put("ban_issued", "Ban issued: %s");
        
        ruStrings.put("all_bans_removed", "Все баны сняты!");
        enStrings.put("all_bans_removed", "All bans removed!");
        
        ruStrings.put("blocks", "Блокировки:");
        enStrings.put("blocks", "Bans:");
        
        ruStrings.put("ban_listing", "Запрет продаж");
        enStrings.put("ban_listing", "Listing ban");
        
        ruStrings.put("ban_buying", "Запрет покупок");
        enStrings.put("ban_buying", "Buying ban");
        
        ruStrings.put("ban_chat", "Запрет чата");
        enStrings.put("ban_chat", "Chat ban");
        
        ruStrings.put("ban_tickets", "Запрет тикетов");
        enStrings.put("ban_tickets", "Ticket ban");
        
        ruStrings.put("ban_full", "Полный бан");
        enStrings.put("ban_full", "Full ban");
        
        ruStrings.put("remove_all_bans", "Снять все баны");
        enStrings.put("remove_all_bans", "Remove all bans");
        
        // ===== ПОДДЕРЖКА =====
        ruStrings.put("support", "Поддержка / Support");
        enStrings.put("support", "Support");
        
        ruStrings.put("support_title", "Поддержка");
        enStrings.put("support_title", "Support");
        
        ruStrings.put("online", "Онлайн");
        enStrings.put("online", "Online");
        
        ruStrings.put("offline", "Офлайн");
        enStrings.put("offline", "Offline");
        
        ruStrings.put("no_active_tickets", "Нет активных тикетов");
        enStrings.put("no_active_tickets", "No active tickets");
        
        ruStrings.put("create_new_ticket", "Создайте новый тикет");
        enStrings.put("create_new_ticket", "Create a new ticket");
        
        ruStrings.put("to_contact_support", "для связи с поддержкой");
        enStrings.put("to_contact_support", "to contact support");
        
        ruStrings.put("new_ticket", "Новый тикет");
        enStrings.put("new_ticket", "New Ticket");
        
        ruStrings.put("ticket_subject", "Тема обращения:");
        enStrings.put("ticket_subject", "Subject:");
        
        ruStrings.put("describe_problem", "Опишите проблему...");
        enStrings.put("describe_problem", "Describe the problem...");
        
        ruStrings.put("describe_your_problem", "Опишите вашу проблему");
        enStrings.put("describe_your_problem", "Describe your problem");
        
        ruStrings.put("as_detailed_as_possible", "максимально подробно.");
        enStrings.put("as_detailed_as_possible", "in as much detail as possible.");
        
        ruStrings.put("we_will_respond", "Мы ответим как можно");
        enStrings.put("we_will_respond", "We will respond as soon");
        
        ruStrings.put("as_soon_as_possible", "скорее.");
        enStrings.put("as_soon_as_possible", "as possible.");
        
        ruStrings.put("create_ticket", "Создать тикет");
        enStrings.put("create_ticket", "Create Ticket");
        
        ruStrings.put("creating_ticket", "Создание тикета...");
        enStrings.put("creating_ticket", "Creating ticket...");
        
        ruStrings.put("ticket_created", "Тикет создан!");
        enStrings.put("ticket_created", "Ticket created!");
        
        ruStrings.put("ticket_closed", "Тикет закрыт!");
        enStrings.put("ticket_closed", "Ticket closed!");
        
        ruStrings.put("tickets_banned", "Вам запрещено создавать тикеты!");
        enStrings.put("tickets_banned", "You are banned from creating tickets!");
        
        ruStrings.put("messages_banned", "Вам запрещено отправлять сообщения!");
        enStrings.put("messages_banned", "You are banned from sending messages!");
        
        ruStrings.put("send_error", "Ошибка отправки: %s");
        enStrings.put("send_error", "Send error: %s");
        
        ruStrings.put("from", "От: ");
        enStrings.put("from", "From: ");
        
        ruStrings.put("close_ticket", "Закрыть");
        enStrings.put("close_ticket", "Close");
        
        ruStrings.put("write_message_to_start", "Напишите сообщение");
        enStrings.put("write_message_to_start", "Write a message");
        
        ruStrings.put("to_start_dialog", "для начала диалога");
        enStrings.put("to_start_dialog", "to start the dialog");
        
        ruStrings.put("support_sender", "Поддержка");
        enStrings.put("support_sender", "Support");
        
        ruStrings.put("write_message_placeholder", "Написать сообщение...");
        enStrings.put("write_message_placeholder", "Write a message...");
        
        // ===== ИНФОРМАЦИЯ / СОЦСЕТИ =====
        ruStrings.put("info", "Информация / Info");
        enStrings.put("info", "Info");
        
        ruStrings.put("contacts", "Контакты");
        enStrings.put("contacts", "Contacts");
        
        ruStrings.put("warning_line1", "Автор мода не несёт");
        enStrings.put("warning_line1", "The mod author is not");
        
        ruStrings.put("warning_line2", "ответственности за");
        enStrings.put("warning_line2", "responsible for");
        
        ruStrings.put("warning_line3", "мошенничество и обман.");
        enStrings.put("warning_line3", "fraud and scams.");
        
        ruStrings.put("warning_line4", "Будьте осторожны!");
        enStrings.put("warning_line4", "Be careful!");
        
        ruStrings.put("respect_line1", "Давайте уважать");
        enStrings.put("respect_line1", "Let's respect");
        
        ruStrings.put("respect_line2", "друг друга и не портить");
        enStrings.put("respect_line2", "each other and not ruin");
        
        ruStrings.put("respect_line3", "друг другу игру!");
        enStrings.put("respect_line3", "the game for each other!");
        
        ruStrings.put("click_to_copy", "Нажмите для копирования");
        enStrings.put("click_to_copy", "Click to copy");
        
        // ===== ПРОДАЖА =====
        ruStrings.put("select_item", "Выберите предмет для продажи:");
        enStrings.put("select_item", "Select item to sell:");
        
        ruStrings.put("selected_item", "Выбранный предмет:");
        enStrings.put("selected_item", "Selected item:");
        
        ruStrings.put("price_label", "Цена:");
        enStrings.put("price_label", "Price:");
        
        ruStrings.put("enter_price", "Введите цену...");
        enStrings.put("enter_price", "Enter price...");
        
        ruStrings.put("conditions_label", "Условия сделки:");
        enStrings.put("conditions_label", "Trade conditions:");
        
        ruStrings.put("enter_conditions", "Опишите условия...");
        enStrings.put("enter_conditions", "Describe conditions...");
        
        ruStrings.put("create_listing", "Создать лот");
        enStrings.put("create_listing", "Create listing");
        
        ruStrings.put("save_changes", "Сохранить");
        enStrings.put("save_changes", "Save");
        
        ruStrings.put("cancel_edit", "Отмена");
        enStrings.put("cancel_edit", "Cancel");
        
        ruStrings.put("listing_created", "Лот успешно создан!");
        enStrings.put("listing_created", "Listing created successfully!");
        
        ruStrings.put("listing_updated", "Лот обновлен!");
        enStrings.put("listing_updated", "Listing updated!");
        
        ruStrings.put("listing_banned", "Вам запрещено создавать лоты!");
        enStrings.put("listing_banned", "You are banned from creating listings!");
        
        ruStrings.put("buying_banned", "Вам запрещено покупать!");
        enStrings.put("buying_banned", "You are banned from buying!");
        
        ruStrings.put("deal_already_exists", "Сделка уже начата");
        enStrings.put("deal_already_exists", "Deal already started");
        
        // ===== ИНСТРУКЦИЯ ПО СДЕЛКЕ =====
        ruStrings.put("trade_instruction_title", "Как провести сделку:");
        enStrings.put("trade_instruction_title", "How to trade:");
        
        ruStrings.put("trade_instruction_hint", "Как провести сделку?");
        enStrings.put("trade_instruction_hint", "How to trade?");
        
        ruStrings.put("trade_step_1", "1. Договоритесь в чате о цене, месте и сервере");
        enStrings.put("trade_step_1", "1. Agree on price, location and server in chat");
        
        ruStrings.put("trade_step_2", "2. Нажмите ESC -> Вернуться в хаб");
        enStrings.put("trade_step_2", "2. Press ESC -> Return to hub");
        
        ruStrings.put("trade_step_3", "3. В хабе выберите одинаковый сервер (ПКМ)");
        enStrings.put("trade_step_3", "3. In hub select same server (RMB)");
        
        ruStrings.put("trade_step_4", "4. Встретьтесь в условленном месте");
        enStrings.put("trade_step_4", "4. Meet at the agreed location");
        
        ruStrings.put("trade_step_5", "5. Введите /trade для ��бмена предметами");
        enStrings.put("trade_step_5", "5. Type /trade to exchange items");
        
        ruStrings.put("trade_step_6", "6. После обмена подтвердите получение здесь");
        enStrings.put("trade_step_6", "6. After exchange confirm receipt here");
        
        ruStrings.put("max_listings", "Максимум %d лотов!");
        enStrings.put("max_listings", "Maximum %d listings!");
        
        ruStrings.put("select_item_first", "Выберите предмет!");
        enStrings.put("select_item_first", "Select an item!");
        
        ruStrings.put("enter_price_first", "Введите цену!");
        enStrings.put("enter_price_first", "Enter a price!");
        
        ruStrings.put("item_not_sellable", "Нельзя продать");
        enStrings.put("item_not_sellable", "Cannot sell");
        
        // ===== МОИ ЛОТЫ =====
        ruStrings.put("edit", "Ред.");
        enStrings.put("edit", "Edit");
        
        ruStrings.put("delete", "Удалить");
        enStrings.put("delete", "Delete");
        
        ruStrings.put("no_listings", "У вас нет активных лотов");
        enStrings.put("no_listings", "You have no active listings");
        
        ruStrings.put("go_to_sell_tab", "Перейдите на вкладку \"Продать\"");
        enStrings.put("go_to_sell_tab", "Go to the \"Sell\" tab");
        
        ruStrings.put("to_create_listing", "чтобы создать лот");
        enStrings.put("to_create_listing", "to create a listing");
        
        // ===== ВСЕ ЛОТЫ =====
        ruStrings.put("no_listings_available", "Нет доступных лотов");
        enStrings.put("no_listings_available", "No listings available");
        
        ruStrings.put("be_first_to_sell", "Будьте первым - создайте лот!");
        enStrings.put("be_first_to_sell", "Be the first - create a listing!");
        
        // ===== ВРЕМЯ =====
        ruStrings.put("just_now", "только что");
        enStrings.put("just_now", "just now");
        
        ruStrings.put("minutes_ago", "%d мин. назад");
        enStrings.put("minutes_ago", "%d min. ago");
        
        ruStrings.put("hours_ago", "%d ч. назад");
        enStrings.put("hours_ago", "%d h. ago");
        
        ruStrings.put("days_ago", "%d дн. назад");
        enStrings.put("days_ago", "%d d. ago");
        
        // ===== ПАГИНАЦИЯ =====
        ruStrings.put("page", "Стр. %d/%d");
        enStrings.put("page", "Page %d/%d");
        
        // ===== ЯЗЫК =====
        ruStrings.put("switch_to_english", "English");
        enStrings.put("switch_to_russian", "Русский");
        
        ruStrings.put("language", "Язык");
        enStrings.put("language", "Language");
        
        // ===== СТАТУСЫ ТИКЕТОВ =====
        ruStrings.put("ticket_open", "[Открыть]");
        enStrings.put("ticket_open", "[Open]");
        
        ruStrings.put("ticket_pending", "[Ожидает]");
        enStrings.put("ticket_pending", "[Pending]");
        
        ruStrings.put("ticket_closed_status", "[Закрыт]");
        enStrings.put("ticket_closed_status", "[Closed]");
        
        // ===== ДОПОЛНИТЕЛЬНЫЕ =====
        ruStrings.put("from_seller", "от %s");
        enStrings.put("from_seller", "by %s");
        
        ruStrings.put("open", "Открыть");
        enStrings.put("open", "Open");
        
        ruStrings.put("remove", "Снять");
        enStrings.put("remove", "Remove");
        
        // ===== ПРОДАЖА - ДОПОЛНИТЕЛЬНЫЕ =====
        ruStrings.put("additional_conditions", "Доп. условия...");
        enStrings.put("additional_conditions", "Additional terms...");
        
        ruStrings.put("list_for_sale", "Выставить на продажу");
        enStrings.put("list_for_sale", "List for Sale");
        
        ruStrings.put("limit_full", "Лимит: %d/%d лотов");
        enStrings.put("limit_full", "Limit: %d/%d listings");
        
        ruStrings.put("listings_count", "Лотов: %d/%d");
        enStrings.put("listings_count", "Listings: %d/%d");
        
        ruStrings.put("cancel", "Отмена");
        enStrings.put("cancel", "Cancel");
        
        // ===== ТИКЕТЫ - ДОПОЛНИТЕЛЬНЫЕ =====
        ruStrings.put("as_detailed", "максимально подробно.");
        enStrings.put("as_detailed", "as detailed as possible.");
        
        ruStrings.put("we_will_reply", "Мы ответим как можнно");
        enStrings.put("we_will_reply", "We will reply as soon");
        
        ruStrings.put("as_soon", "скорее.");
        enStrings.put("as_soon", "as possible.");
        
        // ===== ВРЕМЯ =====
        ruStrings.put("time_days_ago", "%d дн. назад");
        enStrings.put("time_days_ago", "%d days ago");
        
        ruStrings.put("time_hours_ago", "%d ч. назад");
        enStrings.put("time_hours_ago", "%d hours ago");
        
        ruStrings.put("time_minutes_ago", "%d мин. назад");
        enStrings.put("time_minutes_ago", "%d min ago");
        
        ruStrings.put("time_just_now", "только что");
        enStrings.put("time_just_now", "just now");
        
        // ===== ЧАТ БЛОКИРОВКА =====
        ruStrings.put("chat_blocked", "Вы заблокированы в чате");
        enStrings.put("chat_blocked", "You are blocked from chat");
        
        // ===== ВВОД ЦЕНЫ =====
        ruStrings.put("enter_price", "Введите цену...");
        enStrings.put("enter_price", "Enter price...");
        
        // ===== БАН ПРОДАЖ =====
        ruStrings.put("listing_banned", "Вам запрещено выставлять лоты!");
        enStrings.put("listing_banned", "You are banned from listing items!");
        
        // ===== ИЗБРАННОЕ =====
        ruStrings.put("favorites", "Избранное");
        enStrings.put("favorites", "Favorites");
        
        ruStrings.put("add_to_favorites", "В избранное");
        enStrings.put("add_to_favorites", "Add to favorites");
        
        ruStrings.put("remove_from_favorites", "Убрать из избранного");
        enStrings.put("remove_from_favorites", "Remove from favorites");
        
        ruStrings.put("no_favorites", "Нет избранных лотов");
        enStrings.put("no_favorites", "No favorite listings");
        
        ruStrings.put("added_to_favorites", "Добавлено в избранное");
        enStrings.put("added_to_favorites", "Added to favorites");
        
        ruStrings.put("removed_from_favorites", "Удалено из избранного");
        enStrings.put("removed_from_favorites", "Removed from favorites");
        
        // ===== РЕЙТИНГ ПРОДАВЦА =====
        ruStrings.put("no_ratings", "Нет оценок");
        enStrings.put("no_ratings", "No ratings");
        
        // ===== ИСТОРИЯ ТРАНЗАКЦИЙ =====
        ruStrings.put("history", "История");
        enStrings.put("history", "History");
        
        ruStrings.put("transaction_history", "История транзакций");
        enStrings.put("transaction_history", "Transaction History");
        
        ruStrings.put("no_transactions", "Нет транзакций");
        enStrings.put("no_transactions", "No transactions");
        
        ruStrings.put("bought", "Куплено");
        enStrings.put("bought", "Bought");
        
        ruStrings.put("sold", "Продано");
        enStrings.put("sold", "Sold");
        
        ruStrings.put("transaction_completed", "Сделка заверше��а");
        enStrings.put("transaction_completed", "Transaction completed");
        
        ruStrings.put("buyer", "Покупатель");
        enStrings.put("buyer", "Buyer");
        
        // Подтверждение сделки
        ruStrings.put("pending_confirmation", "Ожидает подтверждения");
        enStrings.put("pending_confirmation", "Pending confirmation");
        
        ruStrings.put("confirm_transaction", "Подтвердить получение");
        enStrings.put("confirm_transaction", "Confirm receipt");
        
        ruStrings.put("transaction_confirmed", "Сделка подтверждена!");
        enStrings.put("transaction_confirmed", "Transaction confirmed!");
        
        ruStrings.put("need_confirmed_transaction", "Для оценки нужна подтвержденная сделка");
        enStrings.put("need_confirmed_transaction", "Confirmed transaction required to rate");
        
        ruStrings.put("pending_transactions", "Ожидают подтверждения");
        enStrings.put("pending_transactions", "Pending confirmation");
        
        ruStrings.put("no_pending_transactions", "Нет сделок для подтверждения");
        enStrings.put("no_pending_transactions", "No pending transactions");
        
        ruStrings.put("start_deal", "Начать сделку");
        enStrings.put("start_deal", "Start deal");
        
        ruStrings.put("deal_started", "Сделка создана! Ожидайте подтверждения.");
        enStrings.put("deal_started", "Deal created! Waiting for confirmation.");
        
        ruStrings.put("deal_already_exists", "Сделка уже существует");
        enStrings.put("deal_already_exists", "Deal already exists");
        
        // ===== УВЕДОМЛЕНИЯ =====
        ruStrings.put("notifications", "Уведомления");
        enStrings.put("notifications", "Notifications");
        
        ruStrings.put("no_notifications", "Нет уведомлений");
        enStrings.put("no_notifications", "No notifications");
        
        ruStrings.put("new_message", "Новое сообщение");
        enStrings.put("new_message", "New message");
        
        ruStrings.put("item_sold", "Предмет продан!");
        enStrings.put("item_sold", "Item sold!");
        
        ruStrings.put("new_offer", "Новое предложение");
        enStrings.put("new_offer", "New offer");
        
        ruStrings.put("mark_read", "Прочитано");
        enStrings.put("mark_read", "Mark as read");
        
        ruStrings.put("clear_all", "Очистить все");
        enStrings.put("clear_all", "Clear all");
        
        // ===== ФИЛЬТРЫ И ПОИСК =====
        ruStrings.put("search", "Поиск...");
        enStrings.put("search", "Search...");
        
        ruStrings.put("filter", "Фильтр");
        enStrings.put("filter", "Filter");
        
        ruStrings.put("sort_by", "Сортировка");
        enStrings.put("sort_by", "Sort by");
        
        ruStrings.put("sort_newest", "Сначала новые");
        enStrings.put("sort_newest", "Newest first");
        
        ruStrings.put("sort_oldest", "Сначала старые");
        enStrings.put("sort_oldest", "Oldest first");
        
        ruStrings.put("sort_price_low", "Цена: по возрастанию");
        enStrings.put("sort_price_low", "Price: low to high");
        
        ruStrings.put("sort_price_high", "Цена: по убыванию");
        enStrings.put("sort_price_high", "Price: high to low");
        
        ruStrings.put("sort_seller", "По продавцу");
        enStrings.put("sort_seller", "By seller");
        
        ruStrings.put("clear_filters", "Сбросить");
        enStrings.put("clear_filters", "Clear");
        
        // ===== РЕПУТАЦИЯ =====
        ruStrings.put("reputation", "Репутация");
        enStrings.put("reputation", "Reputation");
        
        ruStrings.put("seller_rating", "Рейтинг продавца");
        enStrings.put("seller_rating", "Seller rating");
        
        ruStrings.put("successful_trades", "Успешных сделок");
        enStrings.put("successful_trades", "Successful trades");
        
        ruStrings.put("rate_seller", "Оценить продавца");
        enStrings.put("rate_seller", "Rate seller");
        
        ruStrings.put("rating_excellent", "Отлично");
        enStrings.put("rating_excellent", "Excellent");
        
        ruStrings.put("rating_good", "Хорошо");
        enStrings.put("rating_good", "Good");
        
        ruStrings.put("rating_neutral", "Нейтрально");
        enStrings.put("rating_neutral", "Neutral");
        
        ruStrings.put("rating_bad", "Плохо");
        enStrings.put("rating_bad", "Bad");
        
        ruStrings.put("thanks_for_rating", "Спасибо за оценку!");
        enStrings.put("thanks_for_rating", "Thanks for rating!");
        
        ruStrings.put("already_rated", "Вы уже оценили");
        enStrings.put("already_rated", "Already rated");
        
        ruStrings.put("new_seller", "Новый продавец");
        enStrings.put("new_seller", "New seller");
        
        ruStrings.put("trusted_seller", "Надежный продавец");
        enStrings.put("trusted_seller", "Trusted seller");
        
        // ===== УВЕДОМЛЕНИЯ (ТОСТЫ) =====
        ruStrings.put("toast_new_message", "Новое сообщение");
        enStrings.put("toast_new_message", "New Message");
        
        ruStrings.put("toast_message_from", "От: %s");
        enStrings.put("toast_message_from", "From: %s");
        
        ruStrings.put("toast_support_reply", "Ответ поддержки");
        enStrings.put("toast_support_reply", "Support Reply");
        
        ruStrings.put("toast_support_answered", "Поддержка ответила!");
        enStrings.put("toast_support_answered", "Support answered!");
        
        ruStrings.put("toast_deal_request", "Запрос на сделку");
        enStrings.put("toast_deal_request", "Deal Request");
        
        ruStrings.put("toast_deal_request_from", "От: %s");
        enStrings.put("toast_deal_request_from", "From: %s");
        
        ruStrings.put("toast_deal_accepted", "Сделка принята!");
        enStrings.put("toast_deal_accepted", "Deal Accepted!");
        
        ruStrings.put("toast_deal_accepted_by", "Продавец: %s");
        enStrings.put("toast_deal_accepted_by", "Seller: %s");
        
        ruStrings.put("toast_deal_rejected", "Сделка отклонена");
        enStrings.put("toast_deal_rejected", "Deal Rejected");
        
        ruStrings.put("toast_deal_rejected_by", "Продавцом: %s");
        enStrings.put("toast_deal_rejected_by", "By seller: %s");
        
        ruStrings.put("toast_deal_completed", "Сделка завершена!");
        enStrings.put("toast_deal_completed", "Deal Completed!");
        
        ruStrings.put("toast_deal_with", "С: %s");
        enStrings.put("toast_deal_with", "With: %s");
        
        ruStrings.put("toast_new_rating", "Новая оценка!");
        enStrings.put("toast_new_rating", "New Rating!");
        
        ruStrings.put("toast_listing_sold", "Ваш лот куплен!");
        enStrings.put("toast_listing_sold", "Your listing sold!");
        
        ruStrings.put("toast_sold_to", "Покупатель: %s");
        enStrings.put("toast_sold_to", "Buyer: %s");
        
        ruStrings.put("toast_price_drop", "Снижение цены!");
        enStrings.put("toast_price_drop", "Price Drop!");
        
        ruStrings.put("toast_favorite_updated", "В избранном: %s");
        enStrings.put("toast_favorite_updated", "In favorites: %s");
        
        ruStrings.put("toast_admin_action", "Администратор");
        enStrings.put("toast_admin_action", "Administrator");
        
        // Приветственное уведомление
        ruStrings.put("toast_welcome_title", "Trade Market");
        enStrings.put("toast_welcome_title", "Trade Market");
        
        ruStrings.put("toast_welcome_key", "Нажмите F8 чтобы открыть меню");
        enStrings.put("toast_welcome_key", "Press F8 to open menu");
        
        // ===== ЭКРАН ОБНОВЛЕНИЙ =====
        ruStrings.put("update_checking", "Проверяю обновления");
        enStrings.put("update_checking", "Checking for updates");
        
        ruStrings.put("update_please_wait", "Подожди секунду...");
        enStrings.put("update_please_wait", "Just a moment...");
        
        ruStrings.put("update_available", "Вышла новая версия!");
        enStrings.put("update_available", "New version available!");
        
        ruStrings.put("update_changelog", "Что нового:");
        enStrings.put("update_changelog", "What's new:");
        
        ruStrings.put("update_up_to_date", "У тебя последняя версия!");
        enStrings.put("update_up_to_date", "You're up to date!");
        
        ruStrings.put("update_current_version", "Текущая версия: %s");
        enStrings.put("update_current_version", "Current version: %s");
        
        ruStrings.put("update_check_failed", "Не удалось проверить обновления");
        enStrings.put("update_check_failed", "Couldn't check for updates");
        
        ruStrings.put("update_continue_anyway", "Но ты все равно можешь зайти");
        enStrings.put("update_continue_anyway", "But you can still continue");
        
        ruStrings.put("update_download", "Скачать");
        enStrings.put("update_download", "Download");
        
        ruStrings.put("update_continue", "Продолжить");
        enStrings.put("update_continue", "Continue");
        
        // ===== УВЕДОМЛЕНИЯ ДЛЯ АДМИНОВ =====
        ruStrings.put("toast_new_ticket", "Новый тикет!");
        enStrings.put("toast_new_ticket", "New Ticket!");
        
        ruStrings.put("toast_ticket_from", "От: %s");
        enStrings.put("toast_ticket_from", "From: %s");
        
        ruStrings.put("toast_user_message", "Сообщение в тикете");
        enStrings.put("toast_user_message", "Ticket Message");
        
        ruStrings.put("toast_ticket_closed", "Тикет закрыт");
        enStrings.put("toast_ticket_closed", "Ticket Closed");
        
        ruStrings.put("toast_ticket_assigned", "Тикет назначен");
        enStrings.put("toast_ticket_assigned", "Ticket Assigned");
        
        // ===== УВЕДОМЛЕНИЯ ОТ АДМИНОВ ДЛЯ ПОЛЬЗОВАТЕЛЕЙ =====
        ruStrings.put("toast_admin_message", "Сообщение от поддержки");
        enStrings.put("toast_admin_message", "Message from Support");
        
        ruStrings.put("toast_admin_message_from", "От администратора: %s");
        enStrings.put("toast_admin_message_from", "From admin: %s");
        
        // ===== ВАЛИДАЦИЯ ПОЛЕЙ ПРОДАЖИ =====
        ruStrings.put("fill_price_field", "Заполните поле цены!");
        enStrings.put("fill_price_field", "Fill in the price field!");
        
        ruStrings.put("fill_conditions_field", "Заполните условия сделки!");
        enStrings.put("fill_conditions_field", "Fill in trade conditions!");
        
        ruStrings.put("fill_all_fields", "Заполните все поля!");
        enStrings.put("fill_all_fields", "Fill in all fields!");
    }
}
