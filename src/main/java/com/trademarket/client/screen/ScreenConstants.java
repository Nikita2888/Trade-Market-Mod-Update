package com.trademarket.client.screen;

import net.minecraft.util.Identifier;

/**
 * Константы для TradeMarketScreen
 * Содержит размеры GUI, цвета темы, текстуры и другие неизменяемые значения
 */
public final class ScreenConstants {

    private ScreenConstants() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    // ==================== РАЗМЕРЫ ОКНА ====================
    
    public static final int GUI_WIDTH = 520;
    public static final int GUI_HEIGHT = 420;
    public static final int DETAILS_WIDTH = 720;
    public static final int ITEM_PANEL_WIDTH = 220;
    public static final int LISTINGS_PER_PAGE = 5;
    public static final int ITEM_HEIGHT = 40;
    
    // Боковая панель навигации
    public static final int SIDEBAR_WIDTH = 52;

    // ==================== ЦВЕТА ТЕМЫ ====================
    // Современный App-стиль маркетплейса (чистый, минималистичный)
    
    public static final int COLOR_GOLD = 0xFF6366F1;              // Основной акцент - индиго/фиолетовый
    public static final int COLOR_GOLD_LIGHT = 0xFF818CF8;        // Светлый индиго
    public static final int COLOR_GOLD_DARK = 0xFF4F46E5;         // Темный индиго
    public static final int COLOR_BG_DARK = 0xFF0F0F14;           // Глубокий темный фон
    public static final int COLOR_BG_PANEL = 0xFF18181B;          // Фон панелей (zinc-900)
    public static final int COLOR_BG_ITEM = 0xFF1F1F23;           // Фон элементов списка
    public static final int COLOR_BG_ITEM_HOVER = 0xFF27272A;     // Hover элементов (zinc-800)
    public static final int COLOR_BORDER = 0xFF27272A;            // Границы (subtle)
    public static final int COLOR_TAB_ACTIVE = 0xFF18181B;        // Активная вкладка
    public static final int COLOR_TAB_INACTIVE = 0xFF0F0F14;      // Неактивная вкладка
    public static final int COLOR_TEXT_TITLE = 0xFFFAFAFA;        // Заголовки - белый
    public static final int COLOR_TEXT_NORMAL = 0xFFA1A1AA;       // Обычный текст (zinc-400)
    public static final int COLOR_TEXT_MUTED = 0xFF71717A;        // Приглушенный текст (zinc-500)
    public static final int COLOR_GREEN = 0xFF22C55E;             // Зеленый - успех (green-500)
    public static final int COLOR_RED = 0xFFEF4444;               // Красный - ошибка (red-500)
    public static final int COLOR_BLUE = 0xFF3B82F6;              // Синий - действия (blue-500)
    public static final int COLOR_INPUT_BG = 0xFF0F0F14;          // Фон полей ввода
    public static final int COLOR_INPUT_BORDER = 0xFF3F3F46;      // Граница полей ввода (zinc-700)
    public static final int COLOR_BUTTON_BG = 0xFF27272A;         // Фон кнопок
    public static final int COLOR_BUTTON_HOVER = 0xFF3F3F46;      // Hover кнопок
    public static final int COLOR_BUTTON_BORDER = 0xFF52525B;     // Граница кнопок (zinc-600)
    public static final int COLOR_SLOT_BG = 0xFF18181B;           // Фон слотов инвентаря
    public static final int COLOR_SLOT_BORDER = 0xFF3F3F46;       // Граница слотов
    public static final int COLOR_CHAT_BG = 0xFF18181B;           // Фон чата
    public static final int COLOR_CHAT_MY_MSG = 0xFF14532D;       // Мои сообщения - зеленоватые
    public static final int COLOR_CHAT_OTHER_MSG = 0xFF1E3A5F;    // Чужие сообщения - синеватые
    public static final int COLOR_TEXT = 0xFFA1A1AA;              // Alias для COLOR_TEXT_NORMAL
    public static final int COLOR_ACCENT_GRADIENT_START = 0xFF6366F1; // Для градиентных эффектов
    public static final int COLOR_ACCENT_GRADIENT_END = 0xFF8B5CF6;   // Фиолетовый
    
    // ==================== WYNNCRAFT СИМВОЛЫ ====================
    
    // WynnCraft alignment surrogate characters (используются для выравнивания текста)
    public static final char POSITIVE_SPACE_HIGH_SURROGATE = '\uDB00';
    public static final char NEGATIVE_SPACE_HIGH_SURROGATE = '\uDAFF';
    public static final char WYNN_MARGIN_CHAR = '\u00C0'; // À

    // ==================== ЗАКЛАДКА СОЦСЕТЕЙ ====================
    
    public static final int BOOKMARK_WIDTH = 24;
    public static final int BOOKMARK_HEIGHT = 120;
    public static final String DISCORD_USERNAME = "@qwerty_5555";
    public static final String TELEGRAM_USERNAME = "@noname22444";
    public static final int COLOR_DISCORD = 0xFF5865F2;
    public static final int COLOR_TELEGRAM = 0xFF229ED9;

    // ==================== ТЕКСТУРЫ ====================
    
    public static final Identifier TEXTURE_DISCORD = Identifier.of("trademarket", "textures/gui/discord.png");
    public static final Identifier TEXTURE_TELEGRAM = Identifier.of("trademarket", "textures/gui/telegram.png");
    public static final Identifier TEXTURE_BOOK_OPEN = Identifier.of("trademarket", "textures/gui/book_open.png");
    public static final Identifier TEXTURE_BOOK_CLOSED = Identifier.of("trademarket", "textures/gui/book_closed.png");
    public static final Identifier TEXTURE_EMERALD = Identifier.of("trademarket", "textures/gui/emerald.png");
    public static final Identifier TEXTURE_SUPPORT = Identifier.of("trademarket", "textures/gui/support.png");
    public static final Identifier TEXTURE_LANG_EN = Identifier.of("trademarket", "textures/gui/lang_en.png");
    public static final Identifier TEXTURE_LANG_RU = Identifier.of("trademarket", "textures/gui/lang_ru.png");
    public static final Identifier TEXTURE_TITLE = Identifier.of("trademarket", "textures/gui/trade-market-texture.png");

    // ==================== ИНТЕРВАЛЫ ОБНОВЛЕНИЯ ====================
    
    public static final long CHAT_REFRESH_INTERVAL = 3000;
    public static final long SUPABASE_REFRESH_INTERVAL = 3000;
    public static final long SUPPORT_STATUS_CHECK_INTERVAL = 5000;
    public static final long TICKET_MESSAGES_REFRESH_INTERVAL = 3000;
    public static final long ADMIN_HEARTBEAT_INTERVAL = 60000;
    public static final long STATUS_MESSAGE_DURATION = 3000;
    public static final long ONLINE_USERS_REFRESH_INTERVAL = 10000;
    public static final long ADMIN_USER_MESSAGES_REFRESH_INTERVAL = 3000;
    public static final long USER_CHATS_REFRESH_INTERVAL = 5000;
    public static final long UNREAD_MARKET_CHECK_INTERVAL = 10000;
    public static final long PENDING_TRANSACTIONS_REFRESH_INTERVAL = 10000;

    // ==================== ЛИМИТЫ ====================
    
    public static final int MAX_LISTINGS_PER_USER = 3;
    public static final int CHAT_MSG_HEIGHT = 20;
    
    // ==================== РЕЖИМЫ СОРТИРОВКИ ====================
    
    public static final String[] SORT_MODES = {"sort_newest", "sort_oldest", "sort_price_low", "sort_price_high"};
}
