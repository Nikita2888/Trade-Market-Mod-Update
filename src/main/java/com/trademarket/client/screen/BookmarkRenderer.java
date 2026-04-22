package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BiFunction;

import static com.trademarket.client.screen.ScreenConstants.*;
import com.trademarket.client.screen.SupportModels.SupportTicket;
import com.trademarket.client.screen.SupportModels.TicketMessage;
import com.trademarket.client.screen.SupportModels.AdminChatForUser;

/**
 * Рендерер для закладок (Info/Social и Support)
 */
public class BookmarkRenderer {
    
    /**
     * Интерфейс для периодических действий в Support панели
     */
    public interface SupportPanelCallbacks {
        void checkSupportStatus();
        void updateAdminHeartbeat();
        void loadUserAdminChats();
        void refreshTicketMessages();
        void loadUserChatMessages();
    }
    
    /**
     * Состояние Support панели
     */
    public static class SupportPanelState {
        public final boolean supportExpanded;
        public final boolean supportOnline;
        public final int unreadAdminMessagesCount;
        public final int supportPanelOffsetX;
        public final int supportPanelOffsetY;
        public final boolean isDraggingSupportPanel;
        public final boolean creatingNewTicket;
        public final SupportTicket activeTicket;
        public final List<TicketMessage> ticketMessages;
        public final int supportChatScroll;
        public final String supportMessageText;
        public final boolean supportMessageFocused;
        public final String newTicketSubject;
        public final boolean newTicketSubjectFocused;
        public final boolean showingAdminChatInSupport;
        public final AdminChatForUser activeUserAdminChat;
        public final List<SupabaseClient.AdminUserMessage> userChatMessages;
        public final String userChatInput;
        public final boolean userChatInputFocused;
        public final List<SupportTicket> supportTickets;
        public final int supportTicketScroll;
        public final List<AdminChatForUser> userAdminChats;
        public final boolean isAdmin;
        public final String currentPlayerName;
        public final long lastSupportStatusCheck;
        public final long lastAdminHeartbeat;
        public final long lastUserChatsRefresh;
        public final long lastTicketMessagesRefresh;
        public final long lastAdminUserMessagesRefresh;
        
        public SupportPanelState(
                boolean supportExpanded, boolean supportOnline, int unreadAdminMessagesCount,
                int supportPanelOffsetX, int supportPanelOffsetY, boolean isDraggingSupportPanel,
                boolean creatingNewTicket, SupportTicket activeTicket, List<TicketMessage> ticketMessages,
                int supportChatScroll, String supportMessageText, boolean supportMessageFocused,
                String newTicketSubject, boolean newTicketSubjectFocused,
                boolean showingAdminChatInSupport, AdminChatForUser activeUserAdminChat,
                List<SupabaseClient.AdminUserMessage> userChatMessages, String userChatInput, boolean userChatInputFocused,
                List<SupportTicket> supportTickets, int supportTicketScroll, List<AdminChatForUser> userAdminChats,
                boolean isAdmin, String currentPlayerName,
                long lastSupportStatusCheck, long lastAdminHeartbeat, long lastUserChatsRefresh,
                long lastTicketMessagesRefresh, long lastAdminUserMessagesRefresh) {
            this.supportExpanded = supportExpanded;
            this.supportOnline = supportOnline;
            this.unreadAdminMessagesCount = unreadAdminMessagesCount;
            this.supportPanelOffsetX = supportPanelOffsetX;
            this.supportPanelOffsetY = supportPanelOffsetY;
            this.isDraggingSupportPanel = isDraggingSupportPanel;
            this.creatingNewTicket = creatingNewTicket;
            this.activeTicket = activeTicket;
            this.ticketMessages = ticketMessages;
            this.supportChatScroll = supportChatScroll;
            this.supportMessageText = supportMessageText;
            this.supportMessageFocused = supportMessageFocused;
            this.newTicketSubject = newTicketSubject;
            this.newTicketSubjectFocused = newTicketSubjectFocused;
            this.showingAdminChatInSupport = showingAdminChatInSupport;
            this.activeUserAdminChat = activeUserAdminChat;
            this.userChatMessages = userChatMessages;
            this.userChatInput = userChatInput;
            this.userChatInputFocused = userChatInputFocused;
            this.supportTickets = supportTickets;
            this.supportTicketScroll = supportTicketScroll;
            this.userAdminChats = userAdminChats;
            this.isAdmin = isAdmin;
            this.currentPlayerName = currentPlayerName;
            this.lastSupportStatusCheck = lastSupportStatusCheck;
            this.lastAdminHeartbeat = lastAdminHeartbeat;
            this.lastUserChatsRefresh = lastUserChatsRefresh;
            this.lastTicketMessagesRefresh = lastTicketMessagesRefresh;
            this.lastAdminUserMessagesRefresh = lastAdminUserMessagesRefresh;
        }
    }

    /**
     * Рисует закладку с соцсетями (Info)
     */
    public static void drawSocialBookmark(DrawContext context, TextRenderer textRenderer,
            int mouseX, int mouseY, int currentGuiRight, int guiTop,
            boolean bookmarkExpanded, int infoPanelOffsetX, int infoPanelOffsetY,
            boolean isDraggingInfoPanel, int screenWidth, int screenHeight) {
        
        int tabX = currentGuiRight + 6;
        int tabY = guiTop + 8;
        int tabSize = 28;
        
        // Современная круглая кнопка-иконка
        boolean tabHovered = mouseX >= tabX && mouseX < tabX + tabSize && 
                mouseY >= tabY && mouseY < tabY + tabSize;
        
        // Фон кнопки
        int tabBg = tabHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_PANEL;
        context.fill(tabX, tabY, tabX + tabSize, tabY + tabSize, tabBg);
        
        // Эмуляция скругления углов
        int cornerBg = 0xFF0F0F14;
        context.fill(tabX, tabY, tabX + 2, tabY + 1, cornerBg);
        context.fill(tabX, tabY, tabX + 1, tabY + 2, cornerBg);
        context.fill(tabX + tabSize - 2, tabY, tabX + tabSize, tabY + 1, cornerBg);
        context.fill(tabX + tabSize - 1, tabY, tabX + tabSize, tabY + 2, cornerBg);
        context.fill(tabX, tabY + tabSize - 1, tabX + 2, tabY + tabSize, cornerBg);
        context.fill(tabX, tabY + tabSize - 2, tabX + 1, tabY + tabSize, cornerBg);
        context.fill(tabX + tabSize - 2, tabY + tabSize - 1, tabX + tabSize, tabY + tabSize, cornerBg);
        context.fill(tabX + tabSize - 1, tabY + tabSize - 2, tabX + tabSize, tabY + tabSize, cornerBg);
        
        // Граница при hover
        if (tabHovered) {
            RenderUtils.drawBorder(context, tabX, tabY, tabSize, tabSize, COLOR_GOLD);
        }
        
        // Иконка информации
        String icon = "\u2139";
        int iconColor = tabHovered ? COLOR_GOLD : COLOR_TEXT_MUTED;
        int iconW = textRenderer.getWidth(icon);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(icon).styled(s -> s.withBold(true)),
                tabX + (tabSize - iconW) / 2, tabY + 10, iconColor);
        
        // Tooltip при наведении
        int supportBtnY = tabY + tabSize + 6;
        int supportBtnSize = 28;
        boolean overSupportBtn = mouseX >= tabX && mouseX < tabX + supportBtnSize && 
                mouseY >= supportBtnY && mouseY < supportBtnY + supportBtnSize;
        if (tabHovered && !bookmarkExpanded && !overSupportBtn) {
            context.drawTooltip(textRenderer, 
                    Text.literal(LocalizationManager.getInstance().get("info")), mouseX, mouseY);
        }
        
        // Если закладка раскрыта - показываем панель
        if (bookmarkExpanded) {
            drawSocialPanel(context, textRenderer, mouseX, mouseY,
                tabX + tabSize, tabY, infoPanelOffsetX, infoPanelOffsetY,
                isDraggingInfoPanel, screenWidth, screenHeight);
        }
    }
    
    /**
     * Рисует панель соцсетей
     */
    private static void drawSocialPanel(DrawContext context, TextRenderer textRenderer,
            int mouseX, int mouseY, int basePanelX, int basePanelY,
            int infoPanelOffsetX, int infoPanelOffsetY,
            boolean isDraggingInfoPanel, int screenWidth, int screenHeight) {
        
        int panelWidth = 160;
        int panelHeight = 175;
        
        // Применяем смещение от перетаскивания
        int panelX = basePanelX + infoPanelOffsetX;
        int panelY = basePanelY + infoPanelOffsetY;
        
        // Ограничиваем позицию панели в пределах экрана
        panelX = Math.max(10, Math.min(screenWidth - panelWidth - 10, panelX));
        panelY = Math.max(10, Math.min(screenHeight - panelHeight - 10, panelY));
        
        // Фон панели с улучшенным оформлением
        context.fill(panelX + 3, panelY + 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0x50000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BG_DARK);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, COLOR_GOLD);
        RenderUtils.drawBorder(context, panelX, panelY, panelWidth, panelHeight, COLOR_BORDER);
        
        // Заголовок
        int headerHeight = 22;
        boolean headerHovered = mouseX >= panelX && mouseX < panelX + panelWidth &&
                mouseY >= panelY && mouseY < panelY + headerHeight;
        int headerBg = headerHovered || isDraggingInfoPanel ? COLOR_BUTTON_HOVER : 0x00000000;
        context.fill(panelX + 1, panelY + 2, panelX + panelWidth - 1, panelY + headerHeight, headerBg);
        
        // Иконка перетаскивания
        int dragIconX = panelX + panelWidth - 20;
        int dragIconY = panelY + 8;
        for (int i = 0; i < 3; i++) {
            context.fill(dragIconX, dragIconY + i * 3, dragIconX + 12, dragIconY + i * 3 + 1, 
                    headerHovered ? COLOR_GOLD : COLOR_TEXT_MUTED);
        }
        
        context.drawTextWithShadow(textRenderer, 
                Text.literal(LocalizationManager.getInstance().get("contacts")).styled(s -> s.withBold(true)),
                panelX + 10, panelY + 6, COLOR_GOLD);
        
        int btnHeight = 22;
        int btnX = panelX + 8;
        int btnWidth = panelWidth - 16;
        
        // Discord кнопка
        int discordY = panelY + 22;
        boolean discordHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                mouseY >= discordY && mouseY < discordY + btnHeight;
        
        int discordBg = discordHovered ? 0xFF6B73F5 : COLOR_DISCORD;
        context.fill(btnX, discordY, btnX + btnWidth, discordY + btnHeight, discordBg);
        RenderUtils.drawBorder(context, btnX, discordY, btnWidth, btnHeight, 0xFF4752C4);
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_DISCORD,
                btnX + 3, discordY + 3, 0, 0, 16, 16, 16, 16);
        context.drawTextWithShadow(textRenderer, Text.literal(DISCORD_USERNAME), 
                btnX + 22, discordY + 7, 0xFFFFFFFF);
        
        // Telegram кнопка
        int telegramY = discordY + btnHeight + 6;
        boolean telegramHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                mouseY >= telegramY && mouseY < telegramY + btnHeight;
        
        int telegramBg = telegramHovered ? 0xFF3DAEE8 : COLOR_TELEGRAM;
        context.fill(btnX, telegramY, btnX + btnWidth, telegramY + btnHeight, telegramBg);
        RenderUtils.drawBorder(context, btnX, telegramY, btnWidth, btnHeight, 0xFF1A7EB8);
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_TELEGRAM,
                btnX + 3, telegramY + 3, 0, 0, 16, 16, 16, 16);
        context.drawTextWithShadow(textRenderer, Text.literal(TELEGRAM_USERNAME), 
                btnX + 22, telegramY + 7, 0xFFFFFFFF);
        
        // Разделительная линия
        int separatorY = telegramY + btnHeight + 8;
        context.fill(panelX + 5, separatorY, panelX + panelWidth - 5, separatorY + 1, COLOR_GOLD_DARK);
        
        // Предупреждение
        int warningY = separatorY + 6;
        int warningTextX = panelX + 6;
        
        LocalizationManager warnLang = LocalizationManager.getInstance();
        String warning1 = warnLang.get("warning_line1");
        String warning2 = warnLang.get("warning_line2");
        String warning3 = warnLang.get("warning_line3");
        String warning4 = warnLang.get("warning_line4");
        
        context.drawTextWithShadow(textRenderer, Text.literal(warning1),
                warningTextX, warningY, COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal(warning2),
                warningTextX, warningY + 10, COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal(warning3),
                warningTextX, warningY + 20, COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal(warning4),
                warningTextX, warningY + 30, COLOR_TEXT_MUTED);
        
        // Красный текст с призывом
        int redTextY = warningY + 48;
        String redLine1 = warnLang.get("respect_line1");
        String redLine2 = warnLang.get("respect_line2");
        String redLine3 = warnLang.get("respect_line3");
        
        context.drawTextWithShadow(textRenderer, 
                Text.literal(redLine1).styled(s -> s.withBold(true)),
                warningTextX, redTextY, COLOR_RED);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(redLine2).styled(s -> s.withBold(true)),
                warningTextX, redTextY + 10, COLOR_RED);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(redLine3).styled(s -> s.withBold(true)),
                warningTextX, redTextY + 20, COLOR_RED);
        
        // Tooltip
        if (discordHovered) {
            context.drawTooltip(textRenderer, 
                    Text.literal(LocalizationManager.getInstance().get("click_to_copy")), mouseX, mouseY);
        } else if (telegramHovered) {
            context.drawTooltip(textRenderer, 
                    Text.literal(LocalizationManager.getInstance().get("click_to_copy")), mouseX, mouseY);
        }
    }

    /**
     * Рисует кнопку закладки Support
     */
    public static void drawSupportBookmarkButton(DrawContext context, TextRenderer textRenderer,
            int mouseX, int mouseY, int currentGuiRight, int guiTop,
            boolean supportExpanded, boolean supportOnline, int unreadAdminMessagesCount) {
        
        int tabX = currentGuiRight + 6;
        int infoTabY = guiTop + 8;
        int infoTabSize = 28;
        int tabY = infoTabY + infoTabSize + 6;
        int tabSize = 28;
        
        int accentColor = COLOR_RED;
        
        boolean tabHovered = mouseX >= tabX && mouseX < tabX + tabSize && 
                mouseY >= tabY && mouseY < tabY + tabSize;
        
        // Фон кнопки
        int tabBg = tabHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_PANEL;
        context.fill(tabX, tabY, tabX + tabSize, tabY + tabSize, tabBg);
        
        // Эмуляция скругления углов
        int cornerBg = 0xFF0F0F14;
        context.fill(tabX, tabY, tabX + 2, tabY + 1, cornerBg);
        context.fill(tabX, tabY, tabX + 1, tabY + 2, cornerBg);
        context.fill(tabX + tabSize - 2, tabY, tabX + tabSize, tabY + 1, cornerBg);
        context.fill(tabX + tabSize - 1, tabY, tabX + tabSize, tabY + 2, cornerBg);
        context.fill(tabX, tabY + tabSize - 1, tabX + 2, tabY + tabSize, cornerBg);
        context.fill(tabX, tabY + tabSize - 2, tabX + 1, tabY + tabSize, cornerBg);
        context.fill(tabX + tabSize - 2, tabY + tabSize - 1, tabX + tabSize, tabY + tabSize, cornerBg);
        context.fill(tabX + tabSize - 1, tabY + tabSize - 2, tabX + tabSize, tabY + tabSize, cornerBg);
        
        // Граница при hover
        if (tabHovered) {
            RenderUtils.drawBorder(context, tabX, tabY, tabSize, tabSize, accentColor);
        }
        
        // Иконка Support (текстура)
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_SUPPORT,
                tabX + 6, tabY + 6, 0, 0, 16, 16, 16, 16);
        
        // Индикатор статуса поддержки
        int statusX = tabX + tabSize - 8;
        int statusY = tabY + tabSize - 8;
        int statusColor = supportOnline ? COLOR_GREEN : COLOR_TEXT_MUTED;
        context.fill(statusX - 1, statusY - 1, statusX + 6, statusY + 6, 0xFF000000);
        context.fill(statusX, statusY, statusX + 5, statusY + 5, statusColor);
        
        // Индикатор непрочитанных сообщений
        if (unreadAdminMessagesCount > 0) {
            int badgeX = tabX + tabSize - 6;
            int badgeY = tabY - 2;
            int badgeSize = 12;
            
            context.fill(badgeX - 1, badgeY - 1, badgeX + badgeSize + 1, badgeY + badgeSize + 1, 0xFF000000);
            context.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, 0xFFE53935);
            
            String countStr = unreadAdminMessagesCount > 9 ? "9+" : String.valueOf(unreadAdminMessagesCount);
            int textWidth = textRenderer.getWidth(countStr);
            context.drawText(textRenderer, countStr,
                    badgeX + (badgeSize - textWidth) / 2, badgeY + 2, 0xFFFFFFFF, false);
        }
        
            // Tooltip при наведении
        if (tabHovered && !supportExpanded) {
            String tooltipText = supportOnline ? 
                LocalizationManager.getInstance().get("support_online") : 
                LocalizationManager.getInstance().get("support_offline");
            context.drawTooltip(textRenderer, Text.literal(tooltipText), mouseX, mouseY);
        }
    }
    
    /**
     * Рисует полную закладку Support с кнопкой и панелью
     */
    public static void drawSupportBookmark(DrawContext context, TextRenderer textRenderer,
            int mouseX, int mouseY, int currentGuiRight, int guiTop, int screenWidth, int screenHeight,
            SupportPanelState state, SupportPanelCallbacks callbacks,
            BiFunction<String, Integer, List<String>> wrapTextFunc,
            BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        int tabX = currentGuiRight + 6;
        int infoTabY = guiTop + 8;
        int infoTabSize = 28;
        // Кнопка Support ниже Info с отступом
        int tabY = infoTabY + infoTabSize + 6;
        int tabSize = 28; // Квадратная кнопка
        
        // Цвет акцента - розово-красный для Support
        int accentColor = COLOR_RED;
        
        // Современная круглая кнопка-иконка
        boolean tabHovered = mouseX >= tabX && mouseX < tabX + tabSize && 
                mouseY >= tabY && mouseY < tabY + tabSize;
        
        // Фон кнопки
        int tabBg = tabHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_PANEL;
        context.fill(tabX, tabY, tabX + tabSize, tabY + tabSize, tabBg);
        
        // Эмуляция скругления углов
        int cornerBg = 0xFF0F0F14;
        context.fill(tabX, tabY, tabX + 2, tabY + 1, cornerBg);
        context.fill(tabX, tabY, tabX + 1, tabY + 2, cornerBg);
        context.fill(tabX + tabSize - 2, tabY, tabX + tabSize, tabY + 1, cornerBg);
        context.fill(tabX + tabSize - 1, tabY, tabX + tabSize, tabY + 2, cornerBg);
        context.fill(tabX, tabY + tabSize - 1, tabX + 2, tabY + tabSize, cornerBg);
        context.fill(tabX, tabY + tabSize - 2, tabX + 1, tabY + tabSize, cornerBg);
        context.fill(tabX + tabSize - 2, tabY + tabSize - 1, tabX + tabSize, tabY + tabSize, cornerBg);
        context.fill(tabX + tabSize - 1, tabY + tabSize - 2, tabX + tabSize, tabY + tabSize, cornerBg);
        
        // Граница при hover
        if (tabHovered) {
            RenderUtils.drawBorder(context, tabX, tabY, tabSize, tabSize, accentColor);
        }
        
        // Иконка помощи (? знак вопроса)
        String icon = "?";
        int iconColor = tabHovered ? accentColor : COLOR_TEXT_MUTED;
        int iconW = textRenderer.getWidth(icon);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(icon).styled(s -> s.withBold(true)),
                tabX + (tabSize - iconW) / 2, tabY + 10, iconColor);
        
        // Индикатор непрочитанных сообщений от админов
        if (state.unreadAdminMessagesCount > 0) {
            int badgeSize = 14;
            int badgeX = tabX + tabSize - badgeSize + 4;
            int badgeY = tabY - 4;
            
            // Красный badge
            context.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, COLOR_RED);
            // Скругление
            context.fill(badgeX, badgeY, badgeX + 1, badgeY + 1, cornerBg);
            context.fill(badgeX + badgeSize - 1, badgeY, badgeX + badgeSize, badgeY + 1, cornerBg);
            
            String countStr = state.unreadAdminMessagesCount > 9 ? "9+" : String.valueOf(state.unreadAdminMessagesCount);
            int textWidth = textRenderer.getWidth(countStr);
            context.drawTextWithShadow(textRenderer, Text.literal(countStr),
                    badgeX + (badgeSize - textWidth) / 2, badgeY + 3, 0xFFFFFFFF);
        }
        
        // Tooltip при наведении на закрытую вкладку
        if (tabHovered && !state.supportExpanded) {
            context.drawTooltip(textRenderer, 
                    Text.literal(LocalizationManager.getInstance().get("support")), mouseX, mouseY);
        }
        
        // Если закладка раскрыта - показываем панель поддержки
        if (state.supportExpanded) {
            drawSupportPanel(context, textRenderer, mouseX, mouseY,
                    tabX, tabY, tabSize, screenWidth, screenHeight,
                    state, callbacks, wrapTextFunc, trimTextToWidthFunc);
        }
    }
    
    /**
     * Рисует панель поддержки
     */
    private static void drawSupportPanel(DrawContext context, TextRenderer textRenderer,
            int mouseX, int mouseY, int tabX, int tabY, int tabSize, int screenWidth, int screenHeight,
            SupportPanelState state, SupportPanelCallbacks callbacks,
            BiFunction<String, Integer, List<String>> wrapTextFunc,
            BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        int accentColor = COLOR_RED;
        int basePanelX = tabX + tabSize;
        int basePanelY = tabY - 30;
        int panelWidth = 260;
        int panelHeight = 280;
        
        // Применяем смещение от перетаскивания
        int panelX = basePanelX + state.supportPanelOffsetX;
        int panelY = basePanelY + state.supportPanelOffsetY;
        
        // Ограничиваем позицию панели в пределах экрана
        panelX = Math.max(10, Math.min(screenWidth - panelWidth - 10, panelX));
        panelY = Math.max(10, Math.min(screenHeight - panelHeight - 10, panelY));
        
        // Фон панели с улучшенным оформлением
        // Тень
        context.fill(panelX + 3, panelY + 3, panelX + panelWidth + 3, panelY + panelHeight + 3, 0x50000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BG_DARK);
        // Акцентная линия сверху
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, accentColor);
        RenderUtils.drawBorder(context, panelX, panelY, panelWidth, panelHeight, COLOR_BORDER);
        
        int contentY = panelY;
        
        // Заголовок с индикатором перетаскивания
        int headerHeight = 22;
        boolean headerHovered = mouseX >= panelX && mouseX < panelX + panelWidth &&
                mouseY >= panelY && mouseY < panelY + headerHeight;
        int headerBg = headerHovered || state.isDraggingSupportPanel ? 0xFF3A2830 : COLOR_BG_DARK;
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + headerHeight, headerBg);
        
        // Иконка перетаскивания (три горизонтальные линии)
        int dragIconX = panelX + panelWidth - 20;
        int dragIconY = panelY + 7;
        for (int i = 0; i < 3; i++) {
            context.fill(dragIconX, dragIconY + i * 3, dragIconX + 12, dragIconY + i * 3 + 1, 
                    headerHovered ? COLOR_GOLD : COLOR_TEXT_MUTED);
        }
        
        context.drawTextWithShadow(textRenderer, 
                Text.literal(LocalizationManager.getInstance().get("support_title")).styled(s -> s.withBold(true)),
                panelX + 10, contentY + 6, 0xFFE57373);
        
        // Статус поддержки (онлайн/офлайн)
        LocalizationManager langSupport = LocalizationManager.getInstance();
        String statusText = state.supportOnline ? langSupport.get("online") : langSupport.get("offline");
        int statusColor = state.supportOnline ? COLOR_GREEN : COLOR_TEXT_MUTED;
        int statusX = panelX + 10 + textRenderer.getWidth(langSupport.get("support_title") + " ") + 15;
        context.drawTextWithShadow(textRenderer, 
                Text.literal(statusText),
                statusX, contentY + 6, statusColor);
        
        // Периодическая проверка статуса поддержки
        if (System.currentTimeMillis() - state.lastSupportStatusCheck > SUPPORT_STATUS_CHECK_INTERVAL) {
            callbacks.checkSupportStatus();
        }
        
        // Периодический heartbeat для админа (для поддержания онлайн статуса)
        if (state.isAdmin) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - state.lastAdminHeartbeat > ADMIN_HEARTBEAT_INTERVAL) {
                callbacks.updateAdminHeartbeat();
            }
        }
        
        // Периодическое обновление чатов от админов
        long currentTimeChats = System.currentTimeMillis();
        if (currentTimeChats - state.lastUserChatsRefresh > USER_CHATS_REFRESH_INTERVAL) {
            callbacks.loadUserAdminChats();
        }
        
        if (state.creatingNewTicket) {
            // Режим создания нового тикета - делегируем в SupportPanelRenderer
            SupportPanelRenderer.drawNewTicketForm(context, textRenderer,
                panelX, contentY, panelWidth, panelHeight, mouseX, mouseY,
                state.newTicketSubject, state.newTicketSubjectFocused);
        } else if (state.activeTicket != null) {
            // Режим просмотра чата тикета - автообновление
            if (System.currentTimeMillis() - state.lastTicketMessagesRefresh > TICKET_MESSAGES_REFRESH_INTERVAL) {
                callbacks.refreshTicketMessages();
            }
            // Делегируем рендеринг в SupportPanelRenderer
            SupportPanelRenderer.drawTicketChat(context, textRenderer,
                panelX, contentY, panelWidth, panelHeight, mouseX, mouseY,
                state.activeTicket, state.ticketMessages, state.supportChatScroll,
                state.supportMessageText, state.supportMessageFocused,
                state.isAdmin, state.currentPlayerName, wrapTextFunc);
        } else if (state.showingAdminChatInSupport && state.activeUserAdminChat != null) {
            // Режим чата с администрацией - автообновление
            long currentTime = System.currentTimeMillis();
            if (currentTime - state.lastAdminUserMessagesRefresh > ADMIN_USER_MESSAGES_REFRESH_INTERVAL) {
                callbacks.loadUserChatMessages();
            }
            // Делегируем рендеринг в SupportPanelRenderer
            SupportPanelRenderer.drawAdminChatInSupport(context, textRenderer,
                panelX, contentY, panelWidth, panelHeight, mouseX, mouseY,
                state.userChatMessages, state.userChatInput, state.userChatInputFocused,
                trimTextToWidthFunc);
        } else {
            // Список тикетов + чаты от администрации - делегируем в SupportPanelRenderer
            SupportPanelRenderer.drawTicketList(context, textRenderer, 
                panelX, contentY, panelWidth, panelHeight, mouseX, mouseY,
                state.supportTickets, state.supportTicketScroll, state.userAdminChats, state.unreadAdminMessagesCount);
        }
    }
}
