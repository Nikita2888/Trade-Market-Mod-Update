package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BiFunction;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Рендерер интерфейса продажи предметов
 */
public class SellUIRenderer {
    
    /**
     * Состояние UI продажи
     */
    public static class SellUIState {
        public final int selectedInventorySlot;
        public final String priceText;
        public final boolean priceFocused;
        public final String descriptionText;
        public final boolean descriptionFocused;
        public final boolean isEditingListing;
        public final int myListingsCount;
        
        public SellUIState(int selectedInventorySlot, String priceText, boolean priceFocused,
                String descriptionText, boolean descriptionFocused,
                boolean isEditingListing, int myListingsCount) {
            this.selectedInventorySlot = selectedInventorySlot;
            this.priceText = priceText;
            this.priceFocused = priceFocused;
            this.descriptionText = descriptionText;
            this.descriptionFocused = descriptionFocused;
            this.isEditingListing = isEditingListing;
            this.myListingsCount = myListingsCount;
        }
    }
    
    /**
     * Интерфейс для рендеринга слота инвентаря
     */
    public interface InventorySlotRenderer {
        void renderSlot(DrawContext context, int slotX, int slotY, int slotIndex, int mouseX, int mouseY);
    }
    
    /**
     * Интерфейс для получения предмета из инвентаря
     */
    public interface InventoryItemProvider {
        ItemStack getStack(int slotIndex);
    }
    
    /**
     * Рендерит интерфейс продажи
     */
    public static void renderSellUI(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int mouseX, int mouseY, SellUIState state,
            InventorySlotRenderer slotRenderer, InventoryItemProvider itemProvider,
            BiFunction<String, Integer, List<String>> wrapTextFunc) {
        
        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70;

        // Панель с улучшенным оформлением
        context.fill(contentX + 2, contentY + 2, contentX + contentWidth + 2, contentY + contentHeight + 2, 0x40000000);
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, COLOR_BG_PANEL);
        context.fill(contentX, contentY, contentX + 2, contentY + contentHeight, COLOR_GREEN); // Зеленая акцентная линия
        RenderUtils.drawBorder(context, contentX, contentY, contentWidth, contentHeight, COLOR_BORDER);

        LocalizationManager lang = LocalizationManager.getInstance();
        
        // Заголовок с подложкой
        String selectTitle = lang.get("select_item");
        int selectTitleWidth = textRenderer.getWidth(selectTitle);
        context.fill(contentX + 8, contentY + 5, contentX + 15 + selectTitleWidth, contentY + 17, 0x30000000);
        context.drawTextWithShadow(textRenderer,
                Text.literal(selectTitle),
                contentX + 10, contentY + 8, COLOR_TEXT_TITLE);

        // Слоты инвентаря
        int slotSize = 18;
        int cols = 9;
        int rows = 3;
        int invStartX = contentX + (contentWidth - cols * slotSize) / 2;
        int invStartY = contentY + 25;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slotIndex = 9 + row * 9 + col;
                int slotX = invStartX + col * slotSize;
                int slotY = invStartY + row * slotSize;
                slotRenderer.renderSlot(context, slotX, slotY, slotIndex, mouseX, mouseY);
            }
        }

        int hotbarY = invStartY + rows * slotSize + 6;
        for (int col = 0; col < 9; col++) {
            int slotX = invStartX + col * slotSize;
            slotRenderer.renderSlot(context, slotX, hotbarY, col, mouseX, mouseY);
        }

        int infoY = hotbarY + slotSize + 8;
        if (state.selectedInventorySlot >= 0) {
            ItemStack selected = itemProvider.getStack(state.selectedInventorySlot);
            if (selected != null && !selected.isEmpty()) {
                context.drawTextWithShadow(textRenderer,
                        Text.literal(lang.get("selected_item") + " ").append(selected.getName()),
                        contentX + 10, infoY, COLOR_GREEN);
            }
        } else {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(lang.get("select_item_first")),
                    contentX + 10, infoY, COLOR_TEXT_MUTED);
        }

        // Поле цены с иконкой изумруда
        int priceFieldY = infoY + 16;
        int halfWidth = (contentWidth - 30) / 2;
        
        // Иконка изумруда
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_EMERALD,
                contentX + 10, priceFieldY - 4, 0, 0, 16, 16, 16, 16);
        context.drawTextWithShadow(textRenderer, Text.literal(lang.get("price_label")),
                contentX + 28, priceFieldY, COLOR_TEXT_TITLE);
        drawInputField(context, textRenderer, contentX + 10, priceFieldY + 12, halfWidth, 16, 
                state.priceText, "500, 5 LE...", state.priceFocused, wrapTextFunc);
        
        // Поле условий сделки
        context.drawTextWithShadow(textRenderer, Text.literal(lang.get("conditions_label")),
                contentX + 20 + halfWidth, priceFieldY, COLOR_TEXT_TITLE);
        drawMultilineInputField(context, textRenderer, contentX + 20 + halfWidth, priceFieldY + 12, halfWidth, 40, 
                state.descriptionText, lang.get("additional_conditions"), state.descriptionFocused, wrapTextFunc);

        int btnX = contentX + contentWidth / 2 - 70;
        int btnY = priceFieldY + 60;
        
        // Проверка заполненности полей
        boolean fieldsValid = !state.priceText.trim().isEmpty() && !state.descriptionText.trim().isEmpty();
        
        boolean canSubmit;
        String buttonText;
        if (state.isEditingListing) {
            canSubmit = fieldsValid;
            buttonText = lang.get("save_changes");
        } else {
            canSubmit = state.selectedInventorySlot >= 0 && state.myListingsCount < MAX_LISTINGS_PER_USER && fieldsValid;
            buttonText = lang.get("list_for_sale");
        }
        
        boolean submitHovered = mouseX >= btnX && mouseX < btnX + 140 &&
                mouseY >= btnY && mouseY < btnY + 20 && canSubmit;
        RenderUtils.drawButton(context, textRenderer, btnX, btnY, 140, 20, buttonText, submitHovered, 
                canSubmit ? COLOR_GOLD : COLOR_TEXT_MUTED);
        
        // Показываем сообщение о лимите
        if (!state.isEditingListing && state.myListingsCount >= MAX_LISTINGS_PER_USER) {
            String limitMsg = lang.get("limit_full", state.myListingsCount, MAX_LISTINGS_PER_USER);
            int limitWidth = textRenderer.getWidth(limitMsg);
            context.drawTextWithShadow(textRenderer, Text.literal(limitMsg),
                    contentX + contentWidth / 2 - limitWidth / 2, btnY + 24, COLOR_RED);
        } else if (!state.isEditingListing) {
            String limitMsg = lang.get("listings_count", state.myListingsCount, MAX_LISTINGS_PER_USER);
            int limitWidth = textRenderer.getWidth(limitMsg);
            context.drawTextWithShadow(textRenderer, Text.literal(limitMsg),
                    contentX + contentWidth / 2 - limitWidth / 2, btnY + 24, COLOR_TEXT_MUTED);
        }
        
        // Кнопка отмены редактирования
        if (state.isEditingListing) {
            int cancelBtnX = contentX + contentWidth / 2 - 40;
            int cancelBtnY = btnY + 24;
            boolean cancelHovered = mouseX >= cancelBtnX && mouseX < cancelBtnX + 80 &&
                    mouseY >= cancelBtnY && mouseY < cancelBtnY + 16;
            RenderUtils.drawButton(context, textRenderer, cancelBtnX, cancelBtnY, 80, 16, 
                    lang.get("cancel"), cancelHovered, COLOR_RED);
        }
    }
    
    /**
     * Рисует поле ввода
     */
    private static void drawInputField(DrawContext context, TextRenderer textRenderer,
            int x, int y, int width, int height, String text, String placeholder, boolean focused,
            BiFunction<String, Integer, List<String>> wrapTextFunc) {
        
        String displayText = text.isEmpty() ? placeholder : text;
        int textColor = text.isEmpty() ? COLOR_TEXT_MUTED : COLOR_TEXT_TITLE;
        
        List<String> lines = wrapTextFunc.apply(displayText, width - 12);
        if (lines.isEmpty()) {
            lines.add("");
        }
        
        int lineHeight = 10;
        int maxLines = 4;
        int actualLines = Math.min(lines.size(), maxLines);
        int dynamicHeight = Math.max(height, 12 + actualLines * lineHeight);
        
        context.fill(x, y, x + width, y + dynamicHeight, COLOR_INPUT_BG);
        
        int borderColor = focused ? COLOR_GOLD : COLOR_BORDER;
        RenderUtils.drawBorder(context, x, y, width, dynamicHeight, borderColor);
        
        if (focused) {
            context.fill(x + 1, y + dynamicHeight - 2, x + width - 1, y + dynamicHeight - 1, COLOR_GOLD);
        }
        
        int textStartX = x + 6;
        int textY = y + 6;
        
        for (int i = 0; i < actualLines; i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) {
                if (textRenderer.getWidth(line) > width - 20) {
                    line = TextUtils.trimTextToWidth(textRenderer, line, width - 25) + "...";
                }
            }
            context.drawTextWithShadow(textRenderer, Text.literal(line),
                    textStartX, textY + (i * lineHeight), textColor);
        }
        
        if (focused && System.currentTimeMillis() % 1000 < 500 && !text.isEmpty()) {
            int cursorLine = Math.min(lines.size() - 1, maxLines - 1);
            String lastLine = lines.get(Math.min(lines.size() - 1, maxLines - 1));
            int cursorX = textStartX + textRenderer.getWidth(lastLine);
            cursorX = Math.min(cursorX, x + width - 4);
            int cursorY = textY + (cursorLine * lineHeight);
            context.fill(cursorX, cursorY, cursorX + 1, cursorY + 8, COLOR_GOLD);
        } else if (focused && System.currentTimeMillis() % 1000 < 500) {
            context.fill(textStartX, textY, textStartX + 1, textY + 8, COLOR_GOLD);
        }
    }
    
    /**
     * Рисует многострочное поле ввода
     */
    private static void drawMultilineInputField(DrawContext context, TextRenderer textRenderer,
            int x, int y, int width, int height, String text, String placeholder, boolean focused,
            BiFunction<String, Integer, List<String>> wrapTextFunc) {
        
        String displayText = text.isEmpty() ? placeholder : text;
        int textColor = text.isEmpty() ? COLOR_TEXT_MUTED : COLOR_TEXT_TITLE;
        
        List<String> lines = wrapTextFunc.apply(displayText, width - 12);
        if (lines.isEmpty()) {
            lines.add("");
        }
        
        int lineHeight = 10;
        int maxLines = height / lineHeight;
        int actualLines = Math.min(lines.size(), maxLines);
        
        context.fill(x, y, x + width, y + height, COLOR_INPUT_BG);
        
        int borderColor = focused ? COLOR_GOLD : COLOR_BORDER;
        RenderUtils.drawBorder(context, x, y, width, height, borderColor);
        
        if (focused) {
            context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, COLOR_GOLD);
        }
        
        int textStartX = x + 6;
        int textY = y + 6;
        
        for (int i = 0; i < actualLines; i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) {
                if (textRenderer.getWidth(line) > width - 20) {
                    line = TextUtils.trimTextToWidth(textRenderer, line, width - 25) + "...";
                }
            }
            context.drawTextWithShadow(textRenderer, Text.literal(line),
                    textStartX, textY + (i * lineHeight), textColor);
        }
        
        if (focused && System.currentTimeMillis() % 1000 < 500 && !text.isEmpty()) {
            int cursorLine = Math.min(lines.size() - 1, maxLines - 1);
            String lastLine = lines.get(Math.min(lines.size() - 1, maxLines - 1));
            int cursorX = textStartX + textRenderer.getWidth(lastLine);
            cursorX = Math.min(cursorX, x + width - 4);
            int cursorY = textY + (cursorLine * lineHeight);
            context.fill(cursorX, cursorY, cursorX + 1, cursorY + 8, COLOR_GOLD);
        } else if (focused && System.currentTimeMillis() % 1000 < 500) {
            context.fill(textStartX, textY, textStartX + 1, textY + 8, COLOR_GOLD);
        }
    }
}
