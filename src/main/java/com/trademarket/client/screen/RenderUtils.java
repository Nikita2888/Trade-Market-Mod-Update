package com.trademarket.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Утилиты для рендеринга UI элементов
 * Содержит методы для рисования панелей, границ, кнопок, полей ввода и т.д.
 */
public final class RenderUtils {

    private RenderUtils() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    /**
     * Рисует основную панель с тенью и скругленными углами
     */
    public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        // Современная мягкая тень (blur effect эмуляция)
        for (int i = 8; i >= 1; i--) {
            int alpha = 8 + (8 - i) * 4;
            context.fill(x + i, y + i, x + width + i, y + height + i, (alpha << 24));
        }
        
        // Основная панель - чистый плоский дизайн
        context.fill(x, y, x + width, y + height, COLOR_BG_PANEL);
        
        // Тонкая граница
        drawBorder(context, x, y, width, height, COLOR_BORDER);
        
        // Эмуляция скругленных углов (закрашиваем углы цветом фона за панелью)
        int cornerSize = 3;
        int bgColor = COLOR_BG_DARK;
        
        // Верхний левый угол
        context.fill(x, y, x + cornerSize, y + 1, bgColor);
        context.fill(x, y, x + 1, y + cornerSize, bgColor);
        context.fill(x + 1, y + 1, x + 2, y + 2, bgColor);
        
        // Верхний правый угол
        context.fill(x + width - cornerSize, y, x + width, y + 1, bgColor);
        context.fill(x + width - 1, y, x + width, y + cornerSize, bgColor);
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, bgColor);
        
        // Нижний левый угол
        context.fill(x, y + height - 1, x + cornerSize, y + height, bgColor);
        context.fill(x, y + height - cornerSize, x + 1, y + height, bgColor);
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, bgColor);
        
        // Нижний правый угол
        context.fill(x + width - cornerSize, y + height - 1, x + width, y + height, bgColor);
        context.fill(x + width - 1, y + height - cornerSize, x + width, y + height, bgColor);
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, bgColor);
    }

    /**
     * Рисует границу прямоугольника
     */
    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * Рисует современный декоративный угол
     */
    public static void drawModernCorner(DrawContext context, int x, int y, boolean flipX, boolean flipY) {
        int size = 6;
        // Акцентная точка в углу
        int dotX = flipX ? x + size - 3 : x + 1;
        int dotY = flipY ? y + size - 3 : y + 1;
        context.fill(dotX, dotY, dotX + 2, dotY + 2, COLOR_GOLD);
    }

    /**
     * Рисует кнопку с современным оформлением
     */
    public static void drawButton(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height,
                                  String text, boolean hovered, int accentColor) {
        // Свечение при наведении
        if (hovered) {
            int glowColor = (accentColor & 0x00FFFFFF) | 0x30000000;
            context.fill(x - 1, y - 1, x + width + 1, y + height + 1, glowColor);
        }
        
        // Фон кнопки
        int bgColor = hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Акцентная линия слева
        context.fill(x, y, x + 2, y + height, accentColor);
        
        // Граница
        drawBorder(context, x, y, width, height, hovered ? accentColor : COLOR_BUTTON_BORDER);
        
        // Текст (центрирован)
        int textWidth = textRenderer.getWidth(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal(text), textX, textY, 
                hovered ? 0xFFFFFFFF : COLOR_TEXT_NORMAL);
    }

    /**
     * Рисует поле ввода с плейсхолдером
     */
    public static void drawInputField(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height,
                                      String text, String placeholder, boolean focused, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Фон
        int bgColor = focused ? 0xFF1A1A1F : COLOR_INPUT_BG;
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Граница
        int borderColor = focused ? COLOR_GOLD : (hovered ? COLOR_INPUT_BORDER : COLOR_BORDER);
        drawBorder(context, x, y, width, height, borderColor);
        
        // Акцентная линия снизу при фокусе
        if (focused) {
            context.fill(x, y + height - 2, x + width, y + height - 1, COLOR_GOLD);
        }
        
        // Текст или плейсхолдер
        int textY = y + (height - 8) / 2;
        if (text.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal(placeholder), x + 6, textY, COLOR_TEXT_MUTED);
        } else {
            // Обрезаем текст если он слишком длинный
            String displayText = text;
            int maxTextWidth = width - 12;
            if (textRenderer.getWidth(displayText) > maxTextWidth) {
                while (textRenderer.getWidth(displayText + "...") > maxTextWidth && displayText.length() > 0) {
                    displayText = displayText.substring(1);
                }
                displayText = "..." + displayText;
            }
            context.drawTextWithShadow(textRenderer, Text.literal(displayText), x + 6, textY, COLOR_TEXT_TITLE);
            
            // Курсор при фокусе
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = x + 6 + textRenderer.getWidth(displayText);
                context.fill(cursorX, textY - 1, cursorX + 1, textY + 9, COLOR_GOLD);
            }
        }
    }

    /**
     * Рисует многострочное поле ввода
     */
    public static void drawMultilineInputField(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height,
                                               String text, String placeholder, boolean focused, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Фон
        int bgColor = focused ? 0xFF1A1A1F : COLOR_INPUT_BG;
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Граница
        int borderColor = focused ? COLOR_GOLD : (hovered ? COLOR_INPUT_BORDER : COLOR_BORDER);
        drawBorder(context, x, y, width, height, borderColor);
        
        // Акцентная линия слева при фокусе
        if (focused) {
            context.fill(x, y, x + 2, y + height, COLOR_GOLD);
        }
        
        int padding = 6;
        int lineHeight = 10;
        int maxLines = (height - padding * 2) / lineHeight;
        
        if (text.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal(placeholder), x + padding, y + padding, COLOR_TEXT_MUTED);
        } else {
            // Разбиваем текст на строки
            List<String> lines = wrapText(textRenderer, text, width - padding * 2);
            int startLine = Math.max(0, lines.size() - maxLines);
            
            for (int i = startLine; i < lines.size() && i - startLine < maxLines; i++) {
                int lineY = y + padding + (i - startLine) * lineHeight;
                context.drawTextWithShadow(textRenderer, Text.literal(lines.get(i)), x + padding, lineY, COLOR_TEXT_TITLE);
            }
            
            // Курсор при фокусе
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int lastLineIndex = Math.min(lines.size() - 1, startLine + maxLines - 1);
                String lastLine = lastLineIndex >= 0 && lastLineIndex < lines.size() ? lines.get(lastLineIndex) : "";
                int cursorX = x + padding + textRenderer.getWidth(lastLine);
                int cursorY = y + padding + (Math.min(lines.size(), maxLines) - 1) * lineHeight;
                context.fill(cursorX, cursorY - 1, cursorX + 1, cursorY + 9, COLOR_GOLD);
            }
        }
    }

    /**
     * Вычисляет высоту поля ввода на основе текста
     */
    public static int getInputFieldHeight(TextRenderer textRenderer, String text, int width, int baseHeight) {
        if (text.isEmpty()) return baseHeight;
        
        List<String> lines = wrapText(textRenderer, text, width - 12);
        int lineHeight = 10;
        int calculatedHeight = lines.size() * lineHeight + 10;
        
        return Math.max(baseHeight, Math.min(calculatedHeight, baseHeight * 3));
    }

    /**
     * Разбивает текст на строки по ширине
     */
    public static List<String> wrapText(TextRenderer textRenderer, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");
        
        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (textRenderer.getWidth(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }

    /**
     * Обрезает текст до указанной ширины
     */
    public static String trimTextToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        
        while (textRenderer.getWidth(text + "...") > maxWidth && text.length() > 0) {
            text = text.substring(0, text.length() - 1);
        }
        
        return text;
    }

    /**
     * Рисует красный крест (для запрещенных слотов)
     */
    public static void drawRedCross(DrawContext context, int x, int y, int slotSize) {
        int crossColor = 0xAAFF0000;
        int thickness = 2;
        
        // Диагональ слева направо
        for (int i = 0; i < slotSize; i++) {
            context.fill(x + i, y + i, x + i + thickness, y + i + thickness, crossColor);
        }
        
        // Диагональ справа налево
        for (int i = 0; i < slotSize; i++) {
            context.fill(x + slotSize - i - thickness, y + i, x + slotSize - i, y + i + thickness, crossColor);
        }
    }

    /**
     * Рисует закладку-иконку
     */
    public static void drawBookmarkIcon(DrawContext context, int x, int y, boolean expanded) {
        // Простая иконка закладки
        int color = expanded ? COLOR_GOLD : COLOR_TEXT_MUTED;
        
        // Книжка (контур)
        context.fill(x + 2, y + 2, x + 14, y + 14, color);
        context.fill(x + 3, y + 3, x + 13, y + 13, COLOR_BG_PANEL);
        
        // Линии текста
        context.fill(x + 5, y + 5, x + 11, y + 6, color);
        context.fill(x + 5, y + 8, x + 11, y + 9, color);
        context.fill(x + 5, y + 11, x + 9, y + 12, color);
    }
}
