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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Рендерер для вкладок Messages и Users
 */
public class TabsRenderer {
    
    /**
     * Состояние вкладки Messages
     */
    public static class MessagesTabState {
        public final Map<UUID, Integer> listingUnreadCounts;
        public final List<MarketListing> unreadListings;
        
        public MessagesTabState(Map<UUID, Integer> listingUnreadCounts, List<MarketListing> unreadListings) {
            this.listingUnreadCounts = listingUnreadCounts;
            this.unreadListings = unreadListings;
        }
    }
    
    /**
     * Рендерит вкладку сообщений
     */
    public static void renderMessagesTab(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int mouseX, int mouseY,
            MessagesTabState state, Function<ItemStack, String> getDisplayName) {
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70;
        
        // Фон панели с улучшенным оформлением
        context.fill(contentX + 2, contentY + 2, contentX + contentWidth + 2, contentY + contentHeight + 2, 0x40000000);
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, COLOR_BG_PANEL);
        context.fill(contentX, contentY, contentX + 2, contentY + contentHeight, COLOR_BLUE);
        RenderUtils.drawBorder(context, contentX, contentY, contentWidth, contentHeight, COLOR_BORDER);
        
        // Заголовок с подложкой
        String title = lang.get("messages_title");
        int titleWidth = textRenderer.getWidth(title);
        context.fill(contentX + 8, contentY + 7, contentX + 15 + titleWidth, contentY + 19, 0x30000000);
        context.drawTextWithShadow(textRenderer, Text.literal(title),
                contentX + 10, contentY + 10, COLOR_GOLD);
        
        // Если нет непрочитанных сообщений
        if (state.listingUnreadCounts.isEmpty()) {
            int emptyBoxWidth = Math.min(200, contentWidth - 40);
            int emptyBoxX = contentX + (contentWidth - emptyBoxWidth) / 2;
            int emptyBoxY = contentY + contentHeight / 2 - 40;
            int emptyBoxHeight = 70;
            
            context.fill(emptyBoxX, emptyBoxY, emptyBoxX + emptyBoxWidth, emptyBoxY + emptyBoxHeight, 0x20000000);
            RenderUtils.drawBorder(context, emptyBoxX, emptyBoxY, emptyBoxWidth, emptyBoxHeight, 0x30FFFFFF);
            
            String msgIcon = "\u2709";
            int iconWidth = textRenderer.getWidth(msgIcon);
            context.drawTextWithShadow(textRenderer, Text.literal(msgIcon).styled(s -> s.withBold(true)),
                    emptyBoxX + (emptyBoxWidth - iconWidth) / 2, emptyBoxY + 15, COLOR_TEXT_MUTED);
            
            String noMessages = lang.get("no_unread_messages");
            int textW = textRenderer.getWidth(noMessages);
            context.drawTextWithShadow(textRenderer, Text.literal(noMessages),
                    emptyBoxX + (emptyBoxWidth - textW) / 2, emptyBoxY + 40, COLOR_TEXT_MUTED);
            return;
        }
        
        // Список лотов с непрочитанными сообщениями
        int listY = contentY + 30;
        int itemHeight = 45;
        int maxVisible = (contentHeight - 40) / itemHeight;
        
        if (state.unreadListings.isEmpty()) {
            return;
        }
        
        int displayed = 0;
        for (int i = 0; i < state.unreadListings.size() && displayed < maxVisible; i++) {
            MarketListing listing = state.unreadListings.get(i);
            int itemY = listY + displayed * itemHeight;
            
            boolean isHovered = mouseX >= contentX + 5 && mouseX < contentX + contentWidth - 10 &&
                    mouseY >= itemY && mouseY < itemY + itemHeight - 5;
            
            if (isHovered) {
                context.fill(contentX + 4, itemY - 1, contentX + contentWidth - 9, itemY + itemHeight - 4,
                        (COLOR_BLUE & 0x00FFFFFF) | 0x25000000);
            }
            
            int bgColor = isHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_ITEM;
            context.fill(contentX + 5, itemY, contentX + contentWidth - 10, itemY + itemHeight - 5, bgColor);
            context.fill(contentX + 5, itemY, contentX + 8, itemY + itemHeight - 5, 
                    isHovered ? COLOR_BLUE : COLOR_GOLD_DARK);
            RenderUtils.drawBorder(context, contentX + 5, itemY, contentWidth - 15, itemHeight - 5, 
                    isHovered ? COLOR_BLUE : COLOR_BORDER);
            
            // Иконка предмета
            ItemStack stack = listing.getItemStack(MarketDataManager.getInstance().getRegistries());
            context.drawItem(stack, contentX + 12, itemY + 7);
            context.drawStackOverlay(textRenderer, stack, contentX + 12, itemY + 7);
            
            // Название предмета
            String displayName = listing.getItemDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = getDisplayName.apply(stack);
            }
            context.drawTextWithShadow(textRenderer, Text.literal(displayName),
                    contentX + 35, itemY + 5, COLOR_TEXT_TITLE);
            
            // Продавец
            String seller = lang.get("seller", listing.getSellerName());
            context.drawTextWithShadow(textRenderer, Text.literal(seller),
                    contentX + 35, itemY + 17, COLOR_TEXT_MUTED);
            
            // Количество непрочитанных
            Integer unreadCount = state.listingUnreadCounts.get(listing.getListingId());
            if (unreadCount != null && unreadCount > 0) {
                String unreadText = lang.get("unread_count", unreadCount);
                int unreadX = contentX + contentWidth - 20 - textRenderer.getWidth(unreadText);
                
                int badgeW = textRenderer.getWidth(unreadText) + 8;
                context.fill(unreadX - 4, itemY + 12, unreadX + badgeW - 4, itemY + 26, 0xFFE53935);
                context.drawTextWithShadow(textRenderer, Text.literal(unreadText),
                        unreadX, itemY + 14, 0xFFFFFFFF);
            }
            
            displayed++;
        }
    }
    
    /**
     * Состояние вкладки Users
     */
    public static class UsersTabState {
        public final List<SupabaseClient.OnlineUser> onlineUsers;
        public final int scrollOffset;
        public final boolean showChat;
        public final String activeUserName;
        
        public UsersTabState(List<SupabaseClient.OnlineUser> onlineUsers, int scrollOffset,
                boolean showChat, String activeUserName) {
            this.onlineUsers = onlineUsers;
            this.scrollOffset = scrollOffset;
            this.showChat = showChat;
            this.activeUserName = activeUserName;
        }
    }
    
    /**
     * Рендерит вкладку пользователей (только для админов)
     */
    public static void renderUsersTab(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int mouseX, int mouseY, UsersTabState state) {
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70;
        
        // Заголовок с подложкой
        String title = lang.get("online_users");
        int titleWidth = textRenderer.getWidth(title);
        int titleX = contentX + (contentWidth - titleWidth) / 2;
        context.fill(titleX - 10, contentY + 3, titleX + titleWidth + 40, contentY + 15, 0x30000000);
        context.drawTextWithShadow(textRenderer, Text.literal(title),
                titleX, contentY + 5, COLOR_GOLD);
        
        // Количество онлайн пользователей
        String countText = "(" + state.onlineUsers.size() + ")";
        context.drawTextWithShadow(textRenderer, Text.literal(countText),
                titleX + titleWidth + 5, contentY + 5, COLOR_TEXT_MUTED);
        
        // Список пользователей
        int listY = contentY + 22;
        int listHeight = contentHeight - 60;
        int itemHeight = 28;
        int maxVisible = listHeight / itemHeight;
        
        // Фон списка
        context.fill(contentX + 6, listY + 1, contentX + contentWidth - 4, listY + listHeight + 1, 0x40000000);
        context.fill(contentX + 5, listY, contentX + contentWidth - 5, listY + listHeight, COLOR_BG_PANEL);
        context.fill(contentX + 5, listY, contentX + 8, listY + listHeight, COLOR_BLUE);
        RenderUtils.drawBorder(context, contentX + 5, listY, contentWidth - 10, listHeight, COLOR_BORDER);
        
        if (state.onlineUsers.isEmpty()) {
            String noUsersText = lang.get("no_online_users");
            int noUsersWidth = textRenderer.getWidth(noUsersText);
            context.drawTextWithShadow(textRenderer, Text.literal(noUsersText),
                    contentX + (contentWidth - noUsersWidth) / 2, listY + listHeight / 2 - 4, COLOR_TEXT_MUTED);
        } else {
            for (int i = 0; i < maxVisible && (i + state.scrollOffset) < state.onlineUsers.size(); i++) {
                SupabaseClient.OnlineUser user = state.onlineUsers.get(i + state.scrollOffset);
                int itemY = listY + 5 + i * itemHeight;
                
                boolean isHovered = mouseX >= contentX + 10 && mouseX < contentX + contentWidth - 15 &&
                                   mouseY >= itemY && mouseY < itemY + itemHeight - 3;
                
                if (isHovered) {
                    context.fill(contentX + 9, itemY - 1, contentX + contentWidth - 14, itemY + itemHeight - 2,
                            (COLOR_BLUE & 0x00FFFFFF) | 0x25000000);
                }
                
                context.fill(contentX + 10, itemY, contentX + contentWidth - 15, itemY + itemHeight - 3,
                        isHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_ITEM);
                context.fill(contentX + 10, itemY, contentX + 13, itemY + itemHeight - 3, COLOR_GREEN);
                RenderUtils.drawBorder(context, contentX + 10, itemY, contentWidth - 25, itemHeight - 3, 
                        isHovered ? COLOR_BLUE : COLOR_BORDER);
                
                // Иконка онлайн статуса
                context.fill(contentX + 16, itemY + 10, contentX + 22, itemY + 16, COLOR_GREEN);
                
                // Имя пользователя
                context.drawTextWithShadow(textRenderer, Text.literal(user.playerName),
                        contentX + 28, itemY + 6, COLOR_TEXT_TITLE);
                
                // Информация о сервере и версии
                String infoText = "";
                if (user.serverIP != null && !user.serverIP.isEmpty()) {
                    infoText = user.serverIP;
                }
                if (user.modVersion != null && !user.modVersion.isEmpty()) {
                    infoText += (infoText.isEmpty() ? "" : " | ") + "v" + user.modVersion;
                }
                if (!infoText.isEmpty()) {
                    context.drawTextWithShadow(textRenderer, Text.literal(infoText),
                            contentX + 28, itemY + 16, COLOR_TEXT_MUTED);
                }
                
                // Кнопка "Написать"
                int writeBtnWidth = textRenderer.getWidth(lang.get("write_message")) + 10;
                int writeBtnX = contentX + contentWidth - writeBtnWidth - 25;
                int writeBtnY = itemY + 5;
                int writeBtnH = itemHeight - 13;
                
                boolean writeBtnHover = mouseX >= writeBtnX && mouseX < writeBtnX + writeBtnWidth &&
                                       mouseY >= writeBtnY && mouseY < writeBtnY + writeBtnH;
                context.fill(writeBtnX, writeBtnY, writeBtnX + writeBtnWidth, writeBtnY + writeBtnH,
                        writeBtnHover ? COLOR_GOLD : COLOR_BUTTON_BG);
                RenderUtils.drawBorder(context, writeBtnX, writeBtnY, writeBtnWidth, writeBtnH, COLOR_GOLD_DARK);
                context.drawTextWithShadow(textRenderer, Text.literal(lang.get("write_message")),
                        writeBtnX + 5, writeBtnY + 2, writeBtnHover ? 0xFF000000 : COLOR_GOLD);
            }
            
            // Скроллбар
            if (state.onlineUsers.size() > maxVisible) {
                int scrollBarX = contentX + contentWidth - 12;
                int scrollBarY = listY + 2;
                int scrollBarHeight = listHeight - 4;
                int scrollThumbHeight = Math.max(20, scrollBarHeight * maxVisible / state.onlineUsers.size());
                int scrollThumbY = scrollBarY + (scrollBarHeight - scrollThumbHeight) * state.scrollOffset / 
                        Math.max(1, state.onlineUsers.size() - maxVisible);
                
                context.fill(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarHeight, COLOR_BG_PANEL);
                context.fill(scrollBarX, scrollThumbY, scrollBarX + 6, scrollThumbY + scrollThumbHeight, COLOR_GOLD_DARK);
            }
        }
        
        // Благодарность за использование мода
        String thanksText = lang.get("thanks_for_using");
        int thanksWidth = textRenderer.getWidth(thanksText);
        int thanksY = contentY + contentHeight - 20;
        context.drawTextWithShadow(textRenderer, Text.literal(thanksText),
                contentX + (contentWidth - thanksWidth) / 2, thanksY, COLOR_GOLD);
    }
    
    /**
     * Состояние чата админ-юзер
     */
    public static class AdminUserChatState {
        public final String userName;
        public final List<SupabaseClient.AdminUserMessage> messages;
        public final String inputText;
        public final boolean inputFocused;
        public final int scrollOffset;
        
        public AdminUserChatState(String userName, List<SupabaseClient.AdminUserMessage> messages,
                String inputText, boolean inputFocused, int scrollOffset) {
            this.userName = userName;
            this.messages = messages;
            this.inputText = inputText;
            this.inputFocused = inputFocused;
            this.scrollOffset = scrollOffset;
        }
    }
    
    /**
     * Рендерит чат админ-юзер
     */
    public static void renderAdminUserChat(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int mouseX, int mouseY, AdminUserChatState state,
            java.util.function.BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        if (state.userName == null) return;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70;
        
        // Фон чата
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, COLOR_BG_PANEL);
        RenderUtils.drawBorder(context, contentX, contentY, contentWidth, contentHeight, COLOR_BORDER);
        
        // Заголовок
        String chatTitle = lang.get("chat_with_user", state.userName);
        context.drawTextWithShadow(textRenderer, Text.literal(chatTitle),
                contentX + 50, contentY + 8, COLOR_GOLD);
        
        // Кнопка назад
        int backBtnX = contentX + 5;
        int backBtnY = contentY + 5;
        boolean backHovered = mouseX >= backBtnX && mouseX < backBtnX + 55 &&
                             mouseY >= backBtnY && mouseY < backBtnY + 18;
        String backText = "< " + lang.get("back");
        RenderUtils.drawButton(context, textRenderer, backBtnX, backBtnY, 55, 18, backText, backHovered, COLOR_GOLD);
        
        // Область сообщений
        int chatAreaY = contentY + 28;
        int chatAreaHeight = contentHeight - 65;
        context.fill(contentX + 7, chatAreaY + 2, contentX + contentWidth - 3, chatAreaY + chatAreaHeight + 2, 0x30000000);
        context.fill(contentX + 5, chatAreaY, contentX + contentWidth - 5, chatAreaY + chatAreaHeight, COLOR_CHAT_BG);
        context.fill(contentX + 5, chatAreaY, contentX + contentWidth - 5, chatAreaY + 1, COLOR_BLUE);
        RenderUtils.drawBorder(context, contentX + 5, chatAreaY, contentWidth - 10, chatAreaHeight, COLOR_BORDER);
        
        // Сообщения
        if (state.messages.isEmpty()) {
            String noMsgText = lang.get("no_messages");
            int noMsgWidth = textRenderer.getWidth(noMsgText);
            context.drawTextWithShadow(textRenderer, Text.literal(noMsgText),
                    contentX + (contentWidth - noMsgWidth) / 2, chatAreaY + chatAreaHeight / 2 - 4, COLOR_TEXT_MUTED);
        } else {
            int msgY = chatAreaY + 5;
            int maxMsgWidth = contentWidth - 40;
            int msgBoxHeight = 16;
            int msgTotalHeight = 28;
            int maxVisible = (chatAreaHeight - 10) / msgTotalHeight;
            
            int startIdx = Math.max(0, state.messages.size() - maxVisible - state.scrollOffset);
            int endIdx = Math.min(state.messages.size(), startIdx + maxVisible);
            
            for (int i = startIdx; i < endIdx; i++) {
                SupabaseClient.AdminUserMessage msg = state.messages.get(i);
                boolean isAdmin = "admin".equals(msg.senderType);
                
                int msgBgColor = isAdmin ? COLOR_CHAT_MY_MSG : COLOR_CHAT_OTHER_MSG;
                int msgTextWidth = Math.min(textRenderer.getWidth(msg.content), maxMsgWidth - 20);
                int msgBoxWidth = msgTextWidth + 16;
                
                int msgX;
                if (isAdmin) {
                    msgX = contentX + contentWidth - 15 - msgBoxWidth;
                } else {
                    msgX = contentX + 15;
                }
                
                context.fill(msgX, msgY, msgX + msgBoxWidth, msgY + msgBoxHeight, msgBgColor);
                RenderUtils.drawBorder(context, msgX, msgY, msgBoxWidth, msgBoxHeight, COLOR_BORDER);
                
                String displayText = msg.content;
                if (textRenderer.getWidth(displayText) > maxMsgWidth - 20) {
                    displayText = trimTextToWidthFunc.apply(displayText, maxMsgWidth - 30) + "...";
                }
                context.drawTextWithShadow(textRenderer, Text.literal(displayText),
                        msgX + 8, msgY + 4, COLOR_TEXT_NORMAL);
                
                String senderLabel = isAdmin ? lang.get("support_label") : state.userName;
                int labelColor = isAdmin ? COLOR_GOLD : COLOR_TEXT_MUTED;
                int senderLabelWidth = textRenderer.getWidth(senderLabel);
                int labelX = isAdmin ? msgX + msgBoxWidth - senderLabelWidth : msgX;
                context.drawTextWithShadow(textRenderer, Text.literal(senderLabel),
                        labelX, msgY + msgBoxHeight + 2, labelColor);
                
                msgY += msgTotalHeight;
            }
        }
        
        // Поле ввода
        int inputY = contentY + contentHeight - 30;
        int inputWidth = contentWidth - 80;
        
        context.fill(contentX + 5, inputY, contentX + 5 + inputWidth, inputY + 20, COLOR_INPUT_BG);
        RenderUtils.drawBorder(context, contentX + 5, inputY, inputWidth, 20, 
                state.inputFocused ? COLOR_GOLD : COLOR_INPUT_BORDER);
        
        String displayInput = state.inputText.isEmpty() && !state.inputFocused ? 
                lang.get("enter_message") : state.inputText;
        int inputTextColor = state.inputText.isEmpty() && !state.inputFocused ? 
                COLOR_TEXT_MUTED : COLOR_TEXT_NORMAL;
        context.drawTextWithShadow(textRenderer, Text.literal(displayInput),
                contentX + 10, inputY + 6, inputTextColor);
        
        // Курсор
        if (state.inputFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = contentX + 10 + textRenderer.getWidth(state.inputText);
            context.fill(cursorX, inputY + 4, cursorX + 1, inputY + 16, COLOR_TEXT_NORMAL);
        }
        
        // Кнопка отправить
        int sendBtnX = contentX + contentWidth - 70;
        int sendBtnY = inputY;
        boolean sendHovered = mouseX >= sendBtnX && mouseX < sendBtnX + 65 &&
                             mouseY >= sendBtnY && mouseY < sendBtnY + 20;
        RenderUtils.drawButton(context, textRenderer, sendBtnX, sendBtnY, 65, 20, lang.get("send"), sendHovered, COLOR_GREEN);
    }
}
