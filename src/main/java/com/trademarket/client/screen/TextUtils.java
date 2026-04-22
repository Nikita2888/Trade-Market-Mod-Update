package com.trademarket.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Утилиты для работы с текстом
 * Содержит методы для обработки текста, включая WynnCraft-специфичные символы
 */
public final class TextUtils {

    private TextUtils() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    /**
     * Очищает текст от WynnCraft alignment символов (surrogate pairs для выравнивания)
     * Сохраняет все остальное форматирование и специальные символы
     * Основано на WynnTils StyledText.stripAlignment()
     */
    public static String stripWynnAlignment(String input) {
        if (input == null || input.isEmpty()) return "";
        
        // WynnTils: ITEM_NAME_MARKER = "\uDAFC\uDC00" - маркер границы названия
        String cleaned = input.replace("\uDAFC\uDC00", "");
        
        // WynnTils normalizeBadString: удаляем À и ֎, заменяем ' на '
        cleaned = cleaned.replace("ÀÀÀ", " ");
        cleaned = cleaned.replace("À", "");
        cleaned = cleaned.replace("\u00C0", ""); // À
        cleaned = cleaned.replace("֎", "");
        cleaned = cleaned.replace("\u058E", ""); // ֎
        cleaned = cleaned.replace("'", "'"); // RIGHT SINGLE QUOTATION MARK -> apostrophe
        cleaned = cleaned.replace("\u2019", "'");
        
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            
            // Пропускаем все surrogate pairs (специальные WynnCraft символы)
            if (Character.isHighSurrogate(ch)) {
                // Пропускаем high surrogate и следующий low surrogate
                if (i + 1 < cleaned.length() && Character.isLowSurrogate(cleaned.charAt(i + 1))) {
                    i++; // Пропускаем low surrogate
                }
                continue;
            }
            if (Character.isLowSurrogate(ch)) continue;
            
            // Пропускаем Private Use Area символы (иконки WynnCraft)
            if (ch >= 0xE000 && ch <= 0xF8FF) continue;
            
            // Пропускаем непечатаемые символы
            if (ch < 0x20 && ch != '\n' && ch != '\r' && ch != '\t') continue;
            
            // Пропускаем Minecraft форматирующий символ §
            if (ch == '\u00A7') {
                // Пропускаем и следующий символ (код цвета)
                if (i + 1 < cleaned.length()) i++;
                continue;
            }
            
            builder.append(ch);
        }
        
        return builder.toString().trim();
    }

    /**
     * Проверяет, является ли строка "пустой" (содержит только alignment символы)
     */
    public static boolean isWynnLineEmpty(String input) {
        if (input == null || input.isEmpty()) return true;
        String stripped = stripWynnAlignment(input);
        return stripped.isEmpty() || stripped.isBlank();
    }

    /**
     * Получает отображаемое имя предмета с сохранением форматирования
     */
    public static Text getItemDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Text.literal("Unknown Item");
        }
        
        // Получаем имя предмета
        Text name = stack.getName();
        
        // Если имя содержит WynnCraft символы - очищаем
        String nameStr = name.getString();
        if (nameStr.contains(String.valueOf(POSITIVE_SPACE_HIGH_SURROGATE)) ||
            nameStr.contains(String.valueOf(NEGATIVE_SPACE_HIGH_SURROGATE)) ||
            nameStr.contains(String.valueOf(WYNN_MARGIN_CHAR))) {
            
            String cleaned = stripWynnAlignment(nameStr);
            if (!cleaned.isEmpty()) {
                // Пытаемся сохранить стили
                if (name instanceof MutableText mutableText) {
                    return Text.literal(cleaned).setStyle(mutableText.getStyle());
                }
                return Text.literal(cleaned);
            }
        }
        
        return name;
    }

    /**
     * Форматирует время в читаемый формат (относительное время)
     */
    public static String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) { // Меньше минуты
            return "just now";
        } else if (diff < 3600000) { // Меньше часа
            int minutes = (int) (diff / 60000);
            return minutes + "m ago";
        } else if (diff < 86400000) { // Меньше дня
            int hours = (int) (diff / 3600000);
            return hours + "h ago";
        } else {
            int days = (int) (diff / 86400000);
            return days + "d ago";
        }
    }
    
    /**
     * Форматирует время сообщения (HH:mm)
     */
    public static String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
    
    /**
     * Форматирует дату для отображения (dd.MM HH:mm)
     */
    public static String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }

    /**
     * Форматирует цену для отображения
     */
    public static String formatPrice(String price) {
        if (price == null || price.isEmpty()) {
            return "0";
        }
        
        try {
            long value = Long.parseLong(price);
            if (value >= 1000000) {
                return String.format("%.1fM", value / 1000000.0);
            } else if (value >= 1000) {
                return String.format("%.1fK", value / 1000.0);
            }
            return price;
        } catch (NumberFormatException e) {
            return price;
        }
    }

    /**
     * Обрезает текст с добавлением многоточия
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Проверяет, является ли текст числом
     */
    public static boolean isNumeric(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Обрезает текст до заданной ширины в пикселях
     */
    public static String trimTextToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        // Бинарный поиск оптимальной длины
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (textRenderer.getWidth(text.substring(0, mid)) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low);
    }
}
