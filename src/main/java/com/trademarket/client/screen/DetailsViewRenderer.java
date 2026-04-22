package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.data.MarketDataManager;
import com.trademarket.data.MarketListing;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BiFunction;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Рендерер для детального просмотра лота
 */
public class DetailsViewRenderer {
    
    /**
     * Состояние детального просмотра
     */
    public static class DetailsViewState {
        public final MarketListing selectedListing;
        public final ItemStack itemStack;
        public final SupabaseClient.SellerReputation sellerReputation;
        public final boolean showRatingUI;
        public final int selectedRating;
        public final boolean hasAlreadyRated;
        public final boolean hasConfirmedTransaction;
        public final boolean hasExistingDealOnListing;
        public final List<SupabaseClient.ChatMessage> chatMessages;
        public final int chatScrollOffset;
        public final int chatMaxVisible;
        public final String messageText;
        public final boolean messageFocused;
        public final boolean isOwnListing;
        public final boolean isAdmin;
        public final int screenWidth;
        public final int screenHeight;
        
        public DetailsViewState(
                MarketListing selectedListing, ItemStack itemStack,
                SupabaseClient.SellerReputation sellerReputation,
                boolean showRatingUI, int selectedRating,
                boolean hasAlreadyRated, boolean hasConfirmedTransaction,
                boolean hasExistingDealOnListing,
                List<SupabaseClient.ChatMessage> chatMessages,
                int chatScrollOffset, int chatMaxVisible,
                String messageText, boolean messageFocused,
                boolean isOwnListing, boolean isAdmin,
                int screenWidth, int screenHeight) {
            this.selectedListing = selectedListing;
            this.itemStack = itemStack;
            this.sellerReputation = sellerReputation;
            this.showRatingUI = showRatingUI;
            this.selectedRating = selectedRating;
            this.hasAlreadyRated = hasAlreadyRated;
            this.hasConfirmedTransaction = hasConfirmedTransaction;
            this.hasExistingDealOnListing = hasExistingDealOnListing;
            this.chatMessages = chatMessages;
            this.chatScrollOffset = chatScrollOffset;
            this.chatMaxVisible = chatMaxVisible;
            this.messageText = messageText;
            this.messageFocused = messageFocused;
            this.isOwnListing = isOwnListing;
            this.isAdmin = isAdmin;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }
    }
    
    /**
     * Результат рендеринга (для передачи координат tooltip и других данных)
     */
    public static class RenderResult {
        public int instructionTooltipX;
        public int instructionTooltipY;
        public boolean showInstructionTooltip;
        public int calculatedChatMaxVisible;
    }
    
    /**
     * Рендерит детальный просмотр лота
     */
    public static RenderResult renderDetailsView(DrawContext context, TextRenderer textRenderer,
            int guiTop, int mouseX, int mouseY, DetailsViewState state,
            BiFunction<String, Integer, List<String>> wrapTextFunc,
            BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        RenderResult result = new RenderResult();
        LocalizationManager lang = LocalizationManager.getInstance();
        
        int detailsGuiLeft = (state.screenWidth - DETAILS_WIDTH) / 2;
        int contentX = detailsGuiLeft + 15;
        int contentY = guiTop + 45;
        int mainPanelWidth = DETAILS_WIDTH - ITEM_PANEL_WIDTH - 45;
        int contentHeight = GUI_HEIGHT - 65;
        
        // Главная панель
        RenderUtils.drawPanel(context, detailsGuiLeft, guiTop, DETAILS_WIDTH, GUI_HEIGHT);
        
        // Заголовок
        int titleY = guiTop + 10;
        String title = lang.get("listing_details");
        int titleWidth = textRenderer.getWidth(title);
        int titleX = detailsGuiLeft + (DETAILS_WIDTH - titleWidth) / 2;
        
        context.fill(titleX - 15, titleY - 2, titleX + titleWidth + 15, titleY + 12, 0x30000000);
        context.drawTextWithShadow(textRenderer,
                Text.literal(title).styled(s -> s.withBold(true)),
                titleX, titleY, COLOR_GOLD);
        
        // Градиентная линия под заголовком
        drawGradientLine(context, detailsGuiLeft + 40, detailsGuiLeft + DETAILS_WIDTH - 40, 
                detailsGuiLeft + DETAILS_WIDTH / 2, titleY + 16, COLOR_GOLD);
        
        // Левая панель (чат)
        context.fill(contentX + 2, contentY + 2, contentX + mainPanelWidth + 2, contentY + contentHeight + 2, 0x40000000);
        context.fill(contentX, contentY, contentX + mainPanelWidth, contentY + contentHeight, COLOR_BG_PANEL);
        context.fill(contentX, contentY, contentX + 2, contentY + contentHeight, COLOR_GOLD);
        RenderUtils.drawBorder(context, contentX, contentY, mainPanelWidth, contentHeight, COLOR_BORDER);
        
        // Кнопка назад
        int backBtnX = contentX + 5;
        int backBtnY = contentY + 5;
        boolean backHovered = mouseX >= backBtnX && mouseX < backBtnX + 55 &&
                mouseY >= backBtnY && mouseY < backBtnY + 18;
        String backText = "< " + lang.get("back");
        RenderUtils.drawButton(context, textRenderer, backBtnX, backBtnY, 55, 18, backText, backHovered, COLOR_GOLD);
        
        // Информация о лоте
        int itemInfoY = contentY + 25;
        ItemStack stack = state.itemStack;
        
        context.drawItem(stack, contentX + 10, itemInfoY);
        context.drawStackOverlay(textRenderer, stack, contentX + 10, itemInfoY);
        
        // Название предмета
        String displayName = state.selectedListing.getItemDisplayName();
        if (displayName == null || displayName.isEmpty() || TextUtils.isWynnLineEmpty(displayName)) {
            displayName = TextUtils.getItemDisplayName(stack).getString();
        }
        context.drawTextWithShadow(textRenderer, Text.literal(displayName),
                contentX + 32, itemInfoY, COLOR_TEXT_TITLE);
        
        // Продавец
        context.drawTextWithShadow(textRenderer,
                Text.literal(lang.get("seller", state.selectedListing.getSellerName())),
                contentX + 32, itemInfoY + 12, COLOR_GOLD_DARK);
        
        // Рейтинг продавца
        String ratingText;
        int ratingColor;
        if (state.sellerReputation != null && state.sellerReputation.totalRatings > 0) {
            String stars = state.sellerReputation.getRatingStars();
            ratingText = stars + " (" + state.sellerReputation.totalRatings + ")";
            ratingColor = COLOR_GOLD;
        } else {
            ratingText = lang.get("no_ratings");
            ratingColor = COLOR_TEXT_MUTED;
        }
        context.drawTextWithShadow(textRenderer, Text.literal(ratingText),
                contentX + mainPanelWidth - textRenderer.getWidth(ratingText) - 10, itemInfoY + 4, ratingColor);
        
        // Количество успешных сделок
        int successfulTrades = (state.sellerReputation != null) ? state.sellerReputation.successfulTrades : 0;
        String tradesText = lang.get("successful_trades") + ": " + successfulTrades;
        context.drawTextWithShadow(textRenderer, Text.literal(tradesText),
                contentX + mainPanelWidth - textRenderer.getWidth(tradesText) - 10, itemInfoY + 14, COLOR_TEXT_MUTED);
        
        // Кнопка оценки и UI
        if (!state.isOwnListing) {
            renderRatingUI(context, textRenderer, contentX, mainPanelWidth, itemInfoY, mouseX, mouseY,
                    state.hasConfirmedTransaction, state.hasAlreadyRated, state.showRatingUI, state.selectedRating, lang);
        }
        
        // Цена и условия
        int currentLineY = itemInfoY + 28;
        String price = state.selectedListing.getPrice();
        if (price != null && !price.isEmpty()) {
            String priceLabel = lang.get("price", price);
            context.drawTextWithShadow(textRenderer, Text.literal(priceLabel),
                    contentX + 10, currentLineY, COLOR_GREEN);
            int priceTextWidth = textRenderer.getWidth(priceLabel);
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_EMERALD,
                    contentX + 10 + priceTextWidth, currentLineY - 5, 0, 0, 16, 16, 16, 16);
            currentLineY += 16;
        }
        
        // Условия
        String description = state.selectedListing.getDescription();
        if (description != null && !description.isEmpty()) {
            currentLineY = renderConditions(context, textRenderer, contentX, mainPanelWidth, currentLineY,
                    description, lang, wrapTextFunc, trimTextToWidthFunc);
        }
        
        // Кнопка "Начать сделку"
        if (!state.isOwnListing) {
            result.showInstructionTooltip = false;
            int[] coords = renderStartDealButton(context, textRenderer, contentX, mainPanelWidth, currentLineY,
                    mouseX, mouseY, state.hasExistingDealOnListing, lang);
            currentLineY = coords[0];
            
            // Кнопка инструкции
            currentLineY += 6;
            int helpBtnSize = 14;
            int helpTextWidth = textRenderer.getWidth(lang.get("trade_instruction_hint"));
            int helpBtnX = contentX + mainPanelWidth - helpTextWidth - helpBtnSize - 25;
            int helpBtnY = currentLineY;
            
            boolean helpHover = mouseX >= helpBtnX && mouseX < helpBtnX + helpBtnSize && 
                               mouseY >= helpBtnY && mouseY < helpBtnY + helpBtnSize;
            
            context.fill(helpBtnX, helpBtnY, helpBtnX + helpBtnSize, helpBtnY + helpBtnSize, 
                    helpHover ? COLOR_GOLD : COLOR_BUTTON_BG);
            RenderUtils.drawBorder(context, helpBtnX, helpBtnY, helpBtnSize, helpBtnSize, COLOR_GOLD);
            context.drawTextWithShadow(textRenderer, Text.literal("?"),
                    helpBtnX + 4, helpBtnY + 3, helpHover ? 0xFF000000 : COLOR_GOLD);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(lang.get("trade_instruction_hint")),
                    helpBtnX + helpBtnSize + 5, helpBtnY + 3, COLOR_TEXT_MUTED);
            
            result.instructionTooltipX = helpBtnX;
            result.instructionTooltipY = helpBtnY + helpBtnSize + 2;
            result.showInstructionTooltip = helpHover;
            
            currentLineY += helpBtnSize + 4;
        }
        
        // Панель характеристик предмета
        renderItemPanel(context, textRenderer, contentX + mainPanelWidth + 10, contentY, contentHeight,
                stack, lang, trimTextToWidthFunc);
        
        // Чат
        int fixedChatOffset = 95;
        int chatY = itemInfoY + fixedChatOffset;
        int chatHeight = contentHeight - fixedChatOffset - 45;
        chatHeight = Math.max(60, chatHeight);
        int chatWidth = mainPanelWidth - 20;
        
        result.calculatedChatMaxVisible = renderChat(context, textRenderer, contentX, chatY, chatWidth, chatHeight,
                state.chatMessages, state.chatScrollOffset, state.messageText, state.messageFocused,
                state.selectedListing, mouseX, mouseY, lang);
        
        return result;
    }
    
    private static void drawGradientLine(DrawContext context, int startX, int endX, int centerX, int y, int color) {
        for (int i = 0; i < (endX - startX) / 2; i++) {
            int alpha = Math.min(255, 60 + (i * 195 / ((endX - startX) / 2)));
            int c = (alpha << 24) | (color & 0x00FFFFFF);
            context.fill(centerX - i, y, centerX - i + 1, y + 1, c);
            context.fill(centerX + i, y, centerX + i + 1, y + 1, c);
        }
    }
    
    private static void renderRatingUI(DrawContext context, TextRenderer textRenderer,
            int contentX, int mainPanelWidth, int itemInfoY, int mouseX, int mouseY,
            boolean hasConfirmedTransaction, boolean hasAlreadyRated, boolean showRatingUI, int selectedRating,
            LocalizationManager lang) {
        
        String rateBtnText;
        boolean canRate = hasConfirmedTransaction && !hasAlreadyRated;
        
        if (hasAlreadyRated) {
            rateBtnText = lang.get("already_rated");
        } else if (!hasConfirmedTransaction) {
            rateBtnText = lang.get("need_confirmed_transaction");
        } else {
            rateBtnText = lang.get("rate_seller");
        }
        
        int rateBtnWidth = textRenderer.getWidth(rateBtnText) + 12;
        int rateBtnX = contentX + mainPanelWidth - rateBtnWidth - 10;
        int rateBtnY = itemInfoY + 26;
        int rateBtnH = 14;
        
        boolean rateHover = canRate && mouseX >= rateBtnX && mouseX < rateBtnX + rateBtnWidth && 
                           mouseY >= rateBtnY && mouseY < rateBtnY + rateBtnH;
        
        int btnBgColor = canRate ? (rateHover ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG) : COLOR_BG_PANEL;
        int btnBorderColor = canRate ? COLOR_GOLD_DARK : COLOR_TEXT_MUTED;
        int btnTextColor = canRate ? COLOR_GOLD : COLOR_TEXT_MUTED;
        
        context.fill(rateBtnX, rateBtnY, rateBtnX + rateBtnWidth, rateBtnY + rateBtnH, btnBgColor);
        RenderUtils.drawBorder(context, rateBtnX, rateBtnY, rateBtnWidth, rateBtnH, btnBorderColor);
        context.drawTextWithShadow(textRenderer, Text.literal(rateBtnText),
                rateBtnX + 6, rateBtnY + 3, btnTextColor);
        
        // UI выбора оценки
        if (showRatingUI && canRate) {
            int ratingUIX = rateBtnX - 50;
            int ratingUIY = rateBtnY + rateBtnH + 2;
            int ratingUIW = rateBtnWidth + 50;
            int ratingUIH = 30;
            
            context.fill(ratingUIX, ratingUIY, ratingUIX + ratingUIW, ratingUIY + ratingUIH, COLOR_BG_PANEL);
            RenderUtils.drawBorder(context, ratingUIX, ratingUIY, ratingUIW, ratingUIH, COLOR_GOLD_DARK);
            
            for (int i = 1; i <= 5; i++) {
                int starX = ratingUIX + 10 + (i - 1) * 18;
                int starY = ratingUIY + 6;
                boolean starHover = mouseX >= starX && mouseX < starX + 16 && 
                                   mouseY >= starY && mouseY < starY + 16;
                String starChar = (i <= selectedRating || starHover) ? "\u2605" : "\u2606";
                int starColor = (i <= selectedRating || starHover) ? COLOR_GOLD : COLOR_TEXT_MUTED;
                context.drawTextWithShadow(textRenderer, Text.literal(starChar), starX, starY, starColor);
            }
            
            if (selectedRating > 0) {
                String confirmText = lang.get("send");
                int confirmX = ratingUIX + ratingUIW - textRenderer.getWidth(confirmText) - 10;
                int confirmY = ratingUIY + 8;
                boolean confirmHover = mouseX >= confirmX - 4 && mouseX < confirmX + textRenderer.getWidth(confirmText) + 4 &&
                                      mouseY >= confirmY - 2 && mouseY < confirmY + 12;
                context.drawTextWithShadow(textRenderer, Text.literal(confirmText),
                        confirmX, confirmY, confirmHover ? COLOR_GREEN : COLOR_GOLD);
            }
        }
    }
    
    private static int renderConditions(DrawContext context, TextRenderer textRenderer,
            int contentX, int mainPanelWidth, int currentLineY, String description,
            LocalizationManager lang,
            BiFunction<String, Integer, List<String>> wrapTextFunc,
            BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        int maxDescWidth = mainPanelWidth - 30;
        String descLabel = lang.get("conditions");
        int descLabelWidth = textRenderer.getWidth(descLabel);
        
        context.drawTextWithShadow(textRenderer, Text.literal(descLabel),
                contentX + 10, currentLineY, COLOR_GOLD);
        
        List<String> descLines = wrapTextFunc.apply(description, maxDescWidth - descLabelWidth);
        if (descLines.isEmpty()) {
            descLines.add(description);
        }
        
        if (!descLines.isEmpty()) {
            String firstLine = descLines.get(0);
            if (textRenderer.getWidth(firstLine) > maxDescWidth - descLabelWidth) {
                firstLine = trimTextToWidthFunc.apply(firstLine, maxDescWidth - descLabelWidth - 10) + "...";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(firstLine),
                    contentX + 10 + descLabelWidth, currentLineY, COLOR_TEXT_NORMAL);
            currentLineY += 12;
        }
        
        for (int i = 1; i < Math.min(descLines.size(), 3); i++) {
            String line = descLines.get(i);
            if (textRenderer.getWidth(line) > maxDescWidth) {
                line = trimTextToWidthFunc.apply(line, maxDescWidth - 10) + "...";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(line),
                    contentX + 10, currentLineY, COLOR_TEXT_NORMAL);
            currentLineY += 12;
        }
        
        if (descLines.size() > 3) {
            context.drawTextWithShadow(textRenderer, Text.literal("..."),
                    contentX + 10, currentLineY, COLOR_TEXT_MUTED);
            currentLineY += 12;
        }
        
        return currentLineY;
    }
    
    private static int[] renderStartDealButton(DrawContext context, TextRenderer textRenderer,
            int contentX, int mainPanelWidth, int currentLineY, int mouseX, int mouseY,
            boolean hasExistingDealOnListing, LocalizationManager lang) {
        
        int startDealX = contentX + 10;
        int startDealY = currentLineY + 4;
        int startDealH = 14;
        
        if (hasExistingDealOnListing) {
            String dealExistsText = lang.get("deal_already_exists");
            int dealExistsWidth = textRenderer.getWidth(dealExistsText) + 12;
            
            context.fill(startDealX, startDealY, startDealX + dealExistsWidth, startDealY + startDealH, COLOR_BUTTON_BG);
            RenderUtils.drawBorder(context, startDealX, startDealY, dealExistsWidth, startDealH, COLOR_TEXT_MUTED);
            context.drawTextWithShadow(textRenderer, Text.literal(dealExistsText),
                    startDealX + 6, startDealY + 3, COLOR_TEXT_MUTED);
            
            return new int[]{startDealY + startDealH};
        } else {
            String startDealText = lang.get("start_deal");
            int startDealWidth = textRenderer.getWidth(startDealText) + 16;
            
            boolean startDealHover = mouseX >= startDealX && mouseX < startDealX + startDealWidth && 
                                    mouseY >= startDealY && mouseY < startDealY + startDealH;
            
            if (startDealHover) {
                context.fill(startDealX - 2, startDealY - 2, startDealX + startDealWidth + 2, startDealY + startDealH + 2,
                        (COLOR_GREEN & 0x00FFFFFF) | 0x40000000);
            }
            
            context.fill(startDealX, startDealY, startDealX + startDealWidth, startDealY + startDealH, 
                    startDealHover ? COLOR_GREEN : COLOR_BUTTON_BG);
            context.fill(startDealX, startDealY, startDealX + 2, startDealY + startDealH, COLOR_GREEN);
            RenderUtils.drawBorder(context, startDealX, startDealY, startDealWidth, startDealH, COLOR_GREEN);
            context.drawTextWithShadow(textRenderer, Text.literal(startDealText),
                    startDealX + 8, startDealY + 3, startDealHover ? 0xFFFFFFFF : COLOR_GREEN);
            
            return new int[]{startDealY + startDealH};
        }
    }
    
    private static void renderItemPanel(DrawContext context, TextRenderer textRenderer,
            int itemPanelX, int itemPanelY, int itemPanelHeight, ItemStack stack,
            LocalizationManager lang, BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        // Тень
        context.fill(itemPanelX + 2, itemPanelY + 2, itemPanelX + ITEM_PANEL_WIDTH + 2, itemPanelY + itemPanelHeight + 2, 0x40000000);
        // Фон
        context.fill(itemPanelX, itemPanelY, itemPanelX + ITEM_PANEL_WIDTH, itemPanelY + itemPanelHeight, COLOR_BG_PANEL);
        // Акцент сверху
        context.fill(itemPanelX, itemPanelY, itemPanelX + ITEM_PANEL_WIDTH, itemPanelY + 2, COLOR_GOLD);
        // Граница
        RenderUtils.drawBorder(context, itemPanelX, itemPanelY, ITEM_PANEL_WIDTH, itemPanelHeight, COLOR_BORDER);
        
        // Заголовок
        String charTitle = lang.get("characteristics");
        int charTitleWidth = textRenderer.getWidth(charTitle);
        int charTitleX = itemPanelX + (ITEM_PANEL_WIDTH - charTitleWidth) / 2;
        context.fill(charTitleX - 8, itemPanelY + 6, charTitleX + charTitleWidth + 8, itemPanelY + 18, 0x30000000);
        context.drawTextWithShadow(textRenderer,
                Text.literal(charTitle).styled(s -> s.withBold(true)),
                charTitleX, itemPanelY + 8, COLOR_GOLD);
        
        // Градиентная линия
        drawGradientLine(context, itemPanelX + 10, itemPanelX + ITEM_PANEL_WIDTH - 10,
                itemPanelX + ITEM_PANEL_WIDTH / 2, itemPanelY + 22, COLOR_GOLD);
        
        // Большая иконка предмета
        int bigItemX = itemPanelX + (ITEM_PANEL_WIDTH - 16) / 2;
        int bigItemY = itemPanelY + 36;
        
        context.fill(bigItemX - 7, bigItemY - 7, bigItemX + 23, bigItemY + 23, (COLOR_GOLD & 0x00FFFFFF) | 0x25000000);
        context.fill(bigItemX - 4, bigItemY - 4, bigItemX + 20, bigItemY + 20, COLOR_SLOT_BG);
        RenderUtils.drawBorder(context, bigItemX - 4, bigItemY - 4, 24, 24, COLOR_GOLD_DARK);
        context.fill(bigItemX - 3, bigItemY - 3, bigItemX + 19, bigItemY - 2, 0x20FFFFFF);
        
        context.drawItem(stack, bigItemX, bigItemY);
        context.drawStackOverlay(textRenderer, stack, bigItemX, bigItemY);
        
        // Название предмета
        Text itemNameForPanel = TextUtils.getItemDisplayName(stack);
        int nameWidth = textRenderer.getWidth(itemNameForPanel);
        int nameX = itemPanelX + (ITEM_PANEL_WIDTH - nameWidth) / 2;
        if (nameWidth > ITEM_PANEL_WIDTH - 20) {
            nameX = itemPanelX + 10;
        }
        int nameY = bigItemY + 24;
        context.drawTextWithShadow(textRenderer, itemNameForPanel, nameX, nameY, 0xFFFFFFFF);
        
        // Количество
        int infoY = nameY + 14;
        context.drawTextWithShadow(textRenderer,
                Text.literal(lang.get("quantity", stack.getCount())),
                itemPanelX + 10, infoY, COLOR_TEXT_NORMAL);
        infoY += 14;
        
        // Разделитель
        context.fill(itemPanelX + 5, infoY, itemPanelX + ITEM_PANEL_WIDTH - 5, infoY + 1, COLOR_BORDER);
        infoY += 8;
        
        // Tooltip предмета - упрощенная версия, полная логика остается в TradeMarketScreen
    }
    
    private static int renderChat(DrawContext context, TextRenderer textRenderer,
            int contentX, int chatY, int chatWidth, int chatHeight,
            List<SupabaseClient.ChatMessage> chatMessages, int chatScrollOffset,
            String messageText, boolean messageFocused, MarketListing selectedListing,
            int mouseX, int mouseY, LocalizationManager lang) {
        
        // Заголовок чата
        String chatTitle = lang.get("chat_with_seller");
        int chatTitleWidth = textRenderer.getWidth(chatTitle);
        context.fill(contentX + 8, chatY - 16, contentX + 14 + chatTitleWidth, chatY - 4, 0x30000000);
        context.fill(contentX + 10, chatY - 12, contentX + 13, chatY - 9, COLOR_BLUE);
        context.drawTextWithShadow(textRenderer, Text.literal(chatTitle),
                contentX + 16, chatY - 14, COLOR_BLUE);
        
        // Область чата
        context.fill(contentX + 10, chatY, contentX + 10 + chatWidth, chatY + chatHeight, COLOR_CHAT_BG);
        context.fill(contentX + 10, chatY, contentX + 10 + chatWidth, chatY + 1, COLOR_BLUE);
        context.fill(contentX + 11, chatY + 1, contentX + 9 + chatWidth, chatY + 2, 0x15FFFFFF);
        RenderUtils.drawBorder(context, contentX + 10, chatY, chatWidth, chatHeight, COLOR_INPUT_BORDER);
        
        // Вычисляем сколько сообщений помещается
        int msgHeight = CHAT_MSG_HEIGHT;
        int chatMaxVisible = (chatHeight - 10) / msgHeight;
        chatMaxVisible = Math.max(1, chatMaxVisible);
        
        // Сообщения
        int msgY = chatY + 5;
        int startIdx = chatScrollOffset;
        int endIdx = Math.min(startIdx + chatMaxVisible, chatMessages.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            if (msgY + msgHeight > chatY + chatHeight - 5) break;
            
            SupabaseClient.ChatMessage msg = chatMessages.get(i);
            // Simplified rendering - full logic stays in TradeMarketScreen
            int msgBgColor = COLOR_CHAT_OTHER_MSG;
            int msgX = contentX + 15;
            int msgWidth = chatWidth - 15;
            
            context.fill(msgX, msgY, msgX + msgWidth, msgY + 18, msgBgColor);
            context.fill(msgX, msgY, msgX + 2, msgY + 18, COLOR_BLUE);
            context.fill(msgX + 2, msgY, msgX + msgWidth, msgY + 1, 0x10FFFFFF);
            
            String senderPrefix = msg.senderName + ": ";
            context.drawTextWithShadow(textRenderer, Text.literal(senderPrefix),
                    msgX + 5, msgY + 5, COLOR_BLUE);
            
            int textOffset = textRenderer.getWidth(senderPrefix);
            String msgText = msg.message;
            if (textRenderer.getWidth(msgText) > msgWidth - textOffset - 15) {
                while (textRenderer.getWidth(msgText + "...") > msgWidth - textOffset - 15 && msgText.length() > 0) {
                    msgText = msgText.substring(0, msgText.length() - 1);
                }
                msgText += "...";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(msgText),
                    msgX + 5 + textOffset, msgY + 5, 0xFFFFFFFF);
            
            msgY += msgHeight;
        }
        
        // Пустой чат
        if (chatMessages.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(lang.get("no_messages")),
                    contentX + 20, chatY + chatHeight / 2 - 4, COLOR_TEXT_MUTED);
        }
        
        // Скролл
        if (chatMessages.size() > chatMaxVisible) {
            int scrollBarHeight = chatHeight - 10;
            int thumbHeight = Math.max(20, scrollBarHeight * chatMaxVisible / chatMessages.size());
            int scrollMaxScroll = Math.max(1, chatMessages.size() - chatMaxVisible);
            int thumbY = chatY + 5 + (scrollBarHeight - thumbHeight) * chatScrollOffset / scrollMaxScroll;
            
            context.fill(contentX + chatWidth + 2, chatY + 5, contentX + chatWidth + 5, chatY + chatHeight - 5, COLOR_BORDER);
            context.fill(contentX + chatWidth + 2, thumbY, contentX + chatWidth + 5, thumbY + thumbHeight, COLOR_GOLD);
        }
        
        // Поле ввода
        int inputY = chatY + chatHeight + 5;
        int inputWidth = chatWidth - 75;
        RenderUtils.drawInputField(context, textRenderer, contentX + 10, inputY, inputWidth, 20, 
                messageText, lang.get("enter_message"), messageFocused, mouseX, mouseY);
        
        // Кнопка отправить
        int sendBtnX = contentX + chatWidth - 60;
        int sendBtnY = inputY;
        boolean sendHovered = mouseX >= sendBtnX && mouseX < sendBtnX + 65 &&
                mouseY >= sendBtnY && mouseY < sendBtnY + 20;
        RenderUtils.drawButton(context, textRenderer, sendBtnX, sendBtnY, 65, 20, lang.get("send"), sendHovered, COLOR_GREEN);
        
        return chatMaxVisible;
    }
}
