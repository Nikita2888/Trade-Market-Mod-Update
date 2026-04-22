package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

import static com.trademarket.client.screen.ScreenConstants.*;
import static com.trademarket.client.screen.TextUtils.formatDate;
import static com.trademarket.client.screen.TextUtils.formatTime;
import com.trademarket.client.screen.SupportModels.SupportTicket;
import com.trademarket.client.screen.SupportModels.TicketMessage;
import com.trademarket.client.screen.SupportModels.AdminChatForUser;

/**
 * Рендерер для панели поддержки (Support Bookmark)
 * Содержит методы отрисовки тикетов, чатов и форм поддержки
 */
public final class SupportPanelRenderer {

    private SupportPanelRenderer() {
        // Приватный конструктор для предотвращения создания экземпляров
    }

    /**
     * Рисует список тикетов
     */
    public static void drawTicketList(DrawContext context, TextRenderer textRenderer, 
            int panelX, int contentY, int panelWidth, int panelHeight, 
            int mouseX, int mouseY,
            List<SupportTicket> supportTickets, int supportTicketScroll,
            List<AdminChatForUser> userAdminChats, int unreadAdminMessagesCount) {
        
        LocalizationManager lang = LocalizationManager.getInstance();
        int listY = contentY + 24;
        int btnX = panelX + 8;
        int btnWidth = panelWidth - 16;
        
        // Кнопка создания нового тикета с современным оформлением
        int newTicketBtnY = listY;
        int btnHeight = 26;
        boolean newBtnHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                mouseY >= newTicketBtnY && mouseY < newTicketBtnY + btnHeight;
        
        // Эффект свечения при наведении
        if (newBtnHovered) {
            context.fill(btnX - 1, newTicketBtnY - 1, btnX + btnWidth + 1, newTicketBtnY + btnHeight + 1,
                    (COLOR_GREEN & 0x00FFFFFF) | 0x40000000);
        }
        
        int newBtnBg = newBtnHovered ? COLOR_GREEN : COLOR_BUTTON_BG;
        context.fill(btnX, newTicketBtnY, btnX + btnWidth, newTicketBtnY + btnHeight, newBtnBg);
        // Акцент слева
        context.fill(btnX, newTicketBtnY, btnX + 3, newTicketBtnY + btnHeight, COLOR_GREEN);
        RenderUtils.drawBorder(context, btnX, newTicketBtnY, btnWidth, btnHeight, COLOR_GREEN);
        
        String newTicketText = "+ " + lang.get("new_ticket");
        int newTicketTextWidth = textRenderer.getWidth(newTicketText);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(newTicketText),
                btnX + (btnWidth - newTicketTextWidth) / 2, newTicketBtnY + 9, 
                newBtnHovered ? 0xFF000000 : 0xFFFFFFFF);
        
        // Градиентный разделитель
        int separatorY = newTicketBtnY + btnHeight + 10;
        int sepCenterX = panelX + panelWidth / 2;
        for (int i = 0; i < (panelWidth - 20) / 2; i++) {
            int alpha = Math.max(20, 100 - (i * 80 / ((panelWidth - 20) / 2)));
            int color = (alpha << 24) | 0x00FFFFFF;
            context.fill(sepCenterX - i, separatorY, sepCenterX - i + 1, separatorY + 1, color);
            context.fill(sepCenterX + i, separatorY, sepCenterX + i + 1, separatorY + 1, color);
        }
        
        // Список тикетов
        int ticketListY = separatorY + 8;
        int ticketHeight = 36;
        int maxVisible = 4;
        
        if (supportTickets.isEmpty()) {
            // Улучшенное пустое состояние
            int emptyBoxWidth = panelWidth - 20;
            int emptyBoxX = panelX + 10;
            int emptyBoxY = ticketListY + 5;
            int emptyBoxHeight = 60;
            
            context.fill(emptyBoxX, emptyBoxY, emptyBoxX + emptyBoxWidth, emptyBoxY + emptyBoxHeight, 0x20000000);
            
            // Иконка
            String helpIcon = "\u2753"; // Знак вопроса
            int iconWidth = textRenderer.getWidth(helpIcon);
            context.drawTextWithShadow(textRenderer, Text.literal(helpIcon),
                    emptyBoxX + (emptyBoxWidth - iconWidth) / 2, emptyBoxY + 8, COLOR_TEXT_MUTED);
            
            // Текст
            String noTickets = lang.get("no_active_tickets");
            int textW = textRenderer.getWidth(noTickets);
            context.drawTextWithShadow(textRenderer, Text.literal(noTickets),
                    emptyBoxX + (emptyBoxWidth - textW) / 2, emptyBoxY + 25, COLOR_TEXT_MUTED);
            
            String createHint = lang.get("create_new_ticket");
            int hintW = textRenderer.getWidth(createHint);
            context.drawTextWithShadow(textRenderer, Text.literal(createHint),
                    emptyBoxX + (emptyBoxWidth - hintW) / 2, emptyBoxY + 42, 0xFF888888);
        } else {
            // Отображаем тикеты
            for (int i = 0; i < Math.min(maxVisible, supportTickets.size() - supportTicketScroll); i++) {
                int ticketIndex = i + supportTicketScroll;
                if (ticketIndex >= supportTickets.size()) break;
                
                SupportTicket ticket = supportTickets.get(ticketIndex);
                int ticketY = ticketListY + i * (ticketHeight + 4);
                
                boolean ticketHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                        mouseY >= ticketY && mouseY < ticketY + ticketHeight;
                
                // Эффект свечения при наведении
                if (ticketHovered) {
                    context.fill(btnX - 1, ticketY - 1, btnX + btnWidth + 1, ticketY + ticketHeight + 1,
                            (COLOR_GOLD & 0x00FFFFFF) | 0x20000000);
                }
                
                int ticketBg = ticketHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_ITEM;
                context.fill(btnX, ticketY, btnX + btnWidth, ticketY + ticketHeight, ticketBg);
                
                // Акцентная полоска слева в зависимости от статуса
                int statusColor = ticket.status.equals("open") ? COLOR_GREEN : 
                                  ticket.status.equals("pending") ? COLOR_GOLD : COLOR_TEXT_MUTED;
                context.fill(btnX, ticketY, btnX + 3, ticketY + ticketHeight, statusColor);
                
                RenderUtils.drawBorder(context, btnX, ticketY, btnWidth, ticketHeight, ticketHovered ? statusColor : COLOR_BORDER);
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(ticket.status.equals("open") ? lang.get("ticket_open") : 
                                    ticket.status.equals("pending") ? lang.get("ticket_pending") : lang.get("ticket_closed_status")),
                        btnX + 4, ticketY + 4, statusColor);
                
                // Тема тикета
                String subject = ticket.subject.length() > 40 ? 
                        ticket.subject.substring(0, 37) + "..." : ticket.subject;
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(subject),
                        btnX + 4, ticketY + 16, COLOR_TEXT_NORMAL);
                
                // Дата
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(formatDate(ticket.createdAt)),
                        btnX + 4, ticketY + 26, COLOR_TEXT_MUTED);
            }
        }
        
        // Секция "Сообщения от поддержки" (чаты от админов)
        if (!userAdminChats.isEmpty()) {
            // Правильный расчет позиции - учитываем пустой список тикетов
            int adminChatsY;
            if (supportTickets.isEmpty()) {
                // Если тикетов нет - размещаем после текста "to contact support"
                adminChatsY = ticketListY + 70;
            } else {
                // Если есть тикеты - после списка тикетов
                adminChatsY = ticketListY + (Math.min(maxVisible, supportTickets.size()) * (ticketHeight + 4)) + 15;
            }
            
            // Разделитель и заголовок
            context.fill(panelX + 5, adminChatsY - 8, panelX + panelWidth - 5, adminChatsY - 7, 0xFF5A4A4A);
            
            String adminChatsTitle = lang.get("messages_from_support");
            if (unreadAdminMessagesCount > 0) {
                adminChatsTitle += " (" + unreadAdminMessagesCount + ")";
            }
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(adminChatsTitle),
                    panelX + 10, adminChatsY, unreadAdminMessagesCount > 0 ? COLOR_GOLD : COLOR_TEXT_MUTED);
            
            int chatItemY = adminChatsY + 14;
            int chatItemHeight = 32;
            
            for (int i = 0; i < Math.min(3, userAdminChats.size()); i++) {
                AdminChatForUser chat = userAdminChats.get(i);
                
                boolean chatHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                        mouseY >= chatItemY && mouseY < chatItemY + chatItemHeight;
                
                int chatBg = chatHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_ITEM;
                context.fill(btnX, chatItemY, btnX + btnWidth, chatItemY + chatItemHeight, chatBg);
                RenderUtils.drawBorder(context, btnX, chatItemY, btnWidth, chatItemHeight, 
                        chat.unreadCount > 0 ? COLOR_GOLD : COLOR_BORDER);
                
                // От кого (админ)
                String fromText = lang.get("from_support_admin", chat.adminName);
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(fromText),
                        btnX + 6, chatItemY + 4, COLOR_TEXT_TITLE);
                
                // Последнее сообщение
                String lastMsg = chat.lastMessage != null ? chat.lastMessage : "";
                if (lastMsg.length() > 35) {
                    lastMsg = lastMsg.substring(0, 32) + "...";
                }
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(lastMsg),
                        btnX + 6, chatItemY + 16, COLOR_TEXT_MUTED);
                
                // Индикатор непрочитанных (красный бейдж с числом)
                if (chat.unreadCount > 0) {
                    int badgeX = btnX + btnWidth - 22;
                    int badgeY = chatItemY + 8;
                    int badgeSize = 16;
                    
                    // Красный круглый бейдж
                    context.fill(badgeX - 1, badgeY - 1, badgeX + badgeSize + 1, badgeY + badgeSize + 1, 0xFF1A1A1A);
                    context.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, 0xFFE53935);
                    
                    String countStr = chat.unreadCount > 9 ? "9+" : String.valueOf(chat.unreadCount);
                    int textWidth = textRenderer.getWidth(countStr);
                    context.drawTextWithShadow(textRenderer, Text.literal(countStr),
                            badgeX + (badgeSize - textWidth) / 2, badgeY + 4, 0xFFFFFFFF);
                }
                
                chatItemY += chatItemHeight + 4;
            }
        }
    }

    /**
     * Рисует форму создания нового тикета
     */
    public static void drawNewTicketForm(DrawContext context, TextRenderer textRenderer,
            int panelX, int contentY, int panelWidth, int panelHeight, 
            int mouseX, int mouseY,
            String newTicketSubject, boolean newTicketSubjectFocused) {
        
        int formY = contentY + 24;
        int btnX = panelX + 8;
        int btnWidth = panelWidth - 16;
        
        // Кнопка назад
        int backBtnY = formY;
        int btnHeight = 20;
        boolean backHovered = mouseX >= btnX && mouseX < btnX + 60 &&
                mouseY >= backBtnY && mouseY < backBtnY + btnHeight;
        
        int backBg = backHovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        context.fill(btnX, backBtnY, btnX + 60, backBtnY + btnHeight, backBg);
        RenderUtils.drawBorder(context, btnX, backBtnY, 60, btnHeight, COLOR_BORDER);
        LocalizationManager ticketLang = LocalizationManager.getInstance();
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("back")),
                btnX + 8, backBtnY + 6, COLOR_TEXT_NORMAL);
        
        // Заголовок формы
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("new_ticket")).styled(s -> s.withBold(true)),
                panelX + 70, formY + 6, COLOR_GOLD);
        
        // Поле темы
        int subjectY = formY + 30;
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("ticket_subject")),
                btnX, subjectY, COLOR_TEXT_NORMAL);
        
        int inputY = subjectY + 14;
        int inputHeight = 28;
        int inputBg = newTicketSubjectFocused ? 0xFF252015 : COLOR_INPUT_BG;
        int inputBorder = newTicketSubjectFocused ? COLOR_GOLD : COLOR_INPUT_BORDER;
        context.fill(btnX, inputY, btnX + btnWidth, inputY + inputHeight, inputBg);
        RenderUtils.drawBorder(context, btnX, inputY, btnWidth, inputHeight, inputBorder);
        
        String displaySubject = newTicketSubject.isEmpty() && !newTicketSubjectFocused ? 
                ticketLang.get("describe_problem") : newTicketSubject;
        int textColor = newTicketSubject.isEmpty() && !newTicketSubjectFocused ? 
                COLOR_TEXT_MUTED : COLOR_TEXT_NORMAL;
        String displayText = displaySubject + (newTicketSubjectFocused ? "_" : "");
        // Обрезаем текст чтобы он не выходил за границы поля
        int maxTextWidth = btnWidth - 8;
        while (textRenderer.getWidth(displayText) > maxTextWidth && displayText.length() > 1) {
            displayText = displayText.substring(displayText.length() > 2 ? 1 : 0, displayText.length());
        }
        // Показываем конец текста при вводе
        if (newTicketSubjectFocused && textRenderer.getWidth(newTicketSubject + "_") > maxTextWidth) {
            String cursor = "_";
            String visibleText = newTicketSubject;
            while (textRenderer.getWidth(visibleText + cursor) > maxTextWidth && visibleText.length() > 0) {
                visibleText = visibleText.substring(1);
            }
            displayText = visibleText + cursor;
        }
        context.drawTextWithShadow(textRenderer, Text.literal(displayText), 
                btnX + 4, inputY + 10, textColor);
        
        // Инструкции
        int instructionY = inputY + inputHeight + 16;
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("describe_your_problem")),
                btnX, instructionY, COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("as_detailed")),
                btnX, instructionY + 12, COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("we_will_reply")),
                btnX, instructionY + 28, COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, 
                Text.literal("Скорее!"),
                btnX, instructionY + 40, COLOR_TEXT_MUTED);
        
        // Кнопка отправки
        int sendBtnY = instructionY + 60;
        int sendBtnHeight = 28;
        boolean canSend = !newTicketSubject.trim().isEmpty();
        boolean sendHovered = canSend && mouseX >= btnX && mouseX < btnX + btnWidth &&
                mouseY >= sendBtnY && mouseY < sendBtnY + sendBtnHeight;
        
        int sendBg = !canSend ? 0xFF2A2A2A : (sendHovered ? 0xFF4A6040 : 0xFF3A5030);
        context.fill(btnX, sendBtnY, btnX + btnWidth, sendBtnY + sendBtnHeight, sendBg);
        RenderUtils.drawBorder(context, btnX, sendBtnY, btnWidth, sendBtnHeight, canSend ? COLOR_GREEN : COLOR_TEXT_MUTED);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketLang.get("create_ticket")),
                btnX + btnWidth / 2 - 40, sendBtnY + 10, canSend ? 0xFFFFFFFF : COLOR_TEXT_MUTED);
    }

    /**
     * Рисует чат тикета
     * 
     * @param context контекст отрисовки
     * @param textRenderer рендерер текста
     * @param panelX X координата панели
     * @param contentY Y координата контента
     * @param panelWidth ширина панели
     * @param panelHeight высота панели
     * @param mouseX X координата мыши
     * @param mouseY Y координата мыши
     * @param activeTicket активный тикет
     * @param ticketMessages список сообщений тикета
     * @param supportChatScroll текущий скролл чата
     * @param supportMessageText текст в поле ввода
     * @param supportMessageFocused фокус на поле ввода
     * @param isAdmin является ли текущий пользователь админом
     * @param currentPlayerName имя текущего игрока
     * @param wrapTextFunc функция для переноса текста
     */
    public static void drawTicketChat(DrawContext context, TextRenderer textRenderer, 
            int panelX, int contentY, int panelWidth, int panelHeight, 
            int mouseX, int mouseY,
            SupportTicket activeTicket, List<TicketMessage> ticketMessages, int supportChatScroll,
            String supportMessageText, boolean supportMessageFocused,
            boolean isAdmin, String currentPlayerName,
            java.util.function.BiFunction<String, Integer, List<String>> wrapTextFunc) {
        
        int chatY = contentY + 24;
        int btnX = panelX + 8;
        int btnWidth = panelWidth - 16;
        int btnHeight = 20;
        
        // Заголовок с информацией об авторе (для админов показываем автора на отдельной строке)
        int headerHeight = isAdmin ? 40 : 24;
        
        // Кнопка назад
        int backBtnY = chatY;
        boolean backHovered = mouseX >= btnX && mouseX < btnX + 60 &&
                mouseY >= backBtnY && mouseY < backBtnY + btnHeight;
        
        int backBg = backHovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        context.fill(btnX, backBtnY, btnX + 60, backBtnY + btnHeight, backBg);
        RenderUtils.drawBorder(context, btnX, backBtnY, 60, btnHeight, COLOR_BORDER);
        context.drawTextWithShadow(textRenderer, 
                Text.literal("< Назад"),
                btnX + 8, backBtnY + 6, COLOR_TEXT_NORMAL);
        
        // Название тикета
        int maxTitleWidth = isAdmin ? panelWidth - 80 : panelWidth - 150;
        String ticketTitle = activeTicket.subject;
        if (textRenderer.getWidth(ticketTitle) > maxTitleWidth) {
            while (textRenderer.getWidth(ticketTitle + "...") > maxTitleWidth && ticketTitle.length() > 1) {
                ticketTitle = ticketTitle.substring(0, ticketTitle.length() - 1);
            }
            ticketTitle = ticketTitle + "...";
        }
        context.drawTextWithShadow(textRenderer, 
                Text.literal(ticketTitle).styled(s -> s.withBold(true)),
                btnX + 65, chatY + 6, COLOR_GOLD);
        
        // Статус тикета
        if (activeTicket.status.equals("closed")) {
            context.drawTextWithShadow(textRenderer, 
                    Text.literal("[ЗАКРЫТ]").styled(s -> s.withBold(true)),
                    panelX + panelWidth - 65, chatY + 6, COLOR_TEXT_MUTED);
        }
        
        // От кого тикет (для админов)
        LocalizationManager ticketChatLang = LocalizationManager.getInstance();
        if (isAdmin) {
            String fromLabel = ticketChatLang.get("from");
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(fromLabel).styled(s -> s.withBold(true)),
                    btnX, chatY + 22, COLOR_TEXT_MUTED);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(activeTicket.createdBy),
                    btnX + textRenderer.getWidth(fromLabel), chatY + 22, COLOR_GOLD_LIGHT);
        }
        
        // Кнопка закрытия тикета (для админов)
        if (isAdmin && !activeTicket.status.equals("closed")) {
            int closeBtnX = panelX + panelWidth - 75;
            int closeBtnY = chatY + 18;
            int closeBtnWidth = 67;
            int closeBtnHeight = 18;
            boolean closeHovered = mouseX >= closeBtnX && mouseX < closeBtnX + closeBtnWidth &&
                    mouseY >= closeBtnY && mouseY < closeBtnY + closeBtnHeight;
            
            int closeBg = closeHovered ? 0xFF8B2020 : 0xFF5A1010;
            context.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnWidth, closeBtnY + closeBtnHeight, closeBg);
            RenderUtils.drawBorder(context, closeBtnX, closeBtnY, closeBtnWidth, closeBtnHeight, COLOR_RED);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(LocalizationManager.getInstance().get("close_ticket")),
                    closeBtnX + 10, closeBtnY + 5, 0xFFFFFFFF);
        }
        
        // Область чата
        int chatAreaY = chatY + headerHeight + 4;
        int chatAreaHeight = isAdmin ? 155 : 170;
        context.fill(btnX, chatAreaY, btnX + btnWidth, chatAreaY + chatAreaHeight, 0xFF1A1A1A);
        RenderUtils.drawBorder(context, btnX, chatAreaY, btnWidth, chatAreaHeight, COLOR_BORDER);
        
        // Отрисовка сообщений в стиле чата
        int msgPadding = 6;
        int bubblePadding = 8;
        int maxBubbleWidth = (int)(btnWidth * 0.75); // Пузырек занимает макс 75% ширины
        int lineHeight = 11;
        
        if (ticketMessages.isEmpty()) {
            String emptyText1 = LocalizationManager.getInstance().get("write_message_to_start");
            String emptyText2 = LocalizationManager.getInstance().get("to_start_dialog");
            int emptyWidth1 = textRenderer.getWidth(emptyText1);
            int emptyWidth2 = textRenderer.getWidth(emptyText2);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(emptyText1),
                    btnX + (btnWidth - emptyWidth1) / 2, chatAreaY + chatAreaHeight / 2 - 10, COLOR_TEXT_MUTED);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(emptyText2),
                    btnX + (btnWidth - emptyWidth2) / 2, chatAreaY + chatAreaHeight / 2 + 4, COLOR_TEXT_MUTED);
        } else {
            // Рассчитываем высоту каждого сообщения и общую высоту
            int totalHeight = 0;
            int[] msgHeights = new int[ticketMessages.size()];
            int[] bubbleWidths = new int[ticketMessages.size()];
            
            for (int i = 0; i < ticketMessages.size(); i++) {
                TicketMessage msg = ticketMessages.get(i);
                List<String> lines = wrapTextFunc.apply(msg.message, maxBubbleWidth - bubblePadding * 2);
                
                // Вычисляем ширину текста
                int textWidth = 0;
                for (String line : lines) {
                    textWidth = Math.max(textWidth, textRenderer.getWidth(line));
                }
                
                // Минимальная ширина для имени и времени
                String sender;
                if (msg.isSupport) {
                    sender = LocalizationManager.getInstance().get("support_sender");
                } else if (msg.sender.equals(currentPlayerName)) {
                    sender = LocalizationManager.getInstance().get("you").replace(": ", "");
                } else {
                    sender = msg.sender;
                }
                String time = formatTime(msg.timestamp);
                int headerWidth = textRenderer.getWidth(sender) + textRenderer.getWidth(time) + 20;
                
                int bubbleWidth = Math.min(maxBubbleWidth, Math.max(textWidth, headerWidth) + bubblePadding * 2);
                bubbleWidths[i] = bubbleWidth;
                
                int bubbleHeight = bubblePadding * 2 + lines.size() * lineHeight + 14;
                msgHeights[i] = bubbleHeight;
                totalHeight += bubbleHeight + msgPadding;
            }
            
            // Вычисляем максимальный скролл
            int visibleHeight = chatAreaHeight - 8;
            int maxScroll = Math.max(0, totalHeight - visibleHeight);
            
            // Корректируем скролл
            int correctedScroll = supportChatScroll;
            if (correctedScroll > maxScroll) correctedScroll = maxScroll;
            if (correctedScroll < 0) correctedScroll = 0;
            
            // Отрисовка сверху вниз
            int currentY = chatAreaY + 4 - correctedScroll;
            
            // Включаем обрезку по области чата
            context.enableScissor(btnX + 1, chatAreaY + 1, btnX + btnWidth - 1, chatAreaY + chatAreaHeight - 1);
            
            for (int i = 0; i < ticketMessages.size(); i++) {
                TicketMessage msg = ticketMessages.get(i);
                int bubbleHeight = msgHeights[i];
                int bubbleWidth = bubbleWidths[i];
                
                // Пропускаем невидимые сообщения
                if (currentY + bubbleHeight < chatAreaY || currentY > chatAreaY + chatAreaHeight) {
                    currentY += bubbleHeight + msgPadding;
                    continue;
                }
                
                // Определяем чьё это сообщение
                boolean isMyMessage = !msg.isSupport && msg.sender.equals(currentPlayerName);
                if (!msg.isSupport && activeTicket.createdBy.equals(currentPlayerName)) {
                    isMyMessage = true;
                }
                
                // Позиция пузырька
                int bubbleX;
                if (msg.isSupport) {
                    bubbleX = btnX + 4;
                } else {
                    bubbleX = btnX + btnWidth - bubbleWidth - 8;
                }
                
                // Цвета пузырька
                int bubbleBg = msg.isSupport ? 0xFF2D3748 : 0xFF1E3A1E;
                int bubbleBorder = msg.isSupport ? 0xFF4A5568 : 0xFF2D5A2D;
                int senderColor = msg.isSupport ? 0xFF63B3ED : 0xFF68D391;
                
                // Рисуем пузырек
                context.fill(bubbleX, currentY, bubbleX + bubbleWidth, currentY + bubbleHeight, bubbleBg);
                RenderUtils.drawBorder(context, bubbleX, currentY, bubbleWidth, bubbleHeight, bubbleBorder);
                
                // Имя отправителя
                String senderName;
                if (msg.isSupport) {
                    senderName = LocalizationManager.getInstance().get("support_sender");
                } else if (msg.sender.equals(currentPlayerName)) {
                    senderName = LocalizationManager.getInstance().get("you").replace(": ", "");
                } else {
                    senderName = msg.sender;
                }
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(senderName).styled(s -> s.withBold(true)),
                        bubbleX + bubblePadding, currentY + bubblePadding, senderColor);
                
                // Время
                String time = formatTime(msg.timestamp);
                int senderWidth = textRenderer.getWidth(senderName);
                context.drawTextWithShadow(textRenderer, 
                        Text.literal(time),
                        bubbleX + bubblePadding + senderWidth + 8, currentY + bubblePadding, 0xFF888888);
                
                // Текст сообщения
                List<String> lines = wrapTextFunc.apply(msg.message, maxBubbleWidth - bubblePadding * 2);
                int textY = currentY + bubblePadding + 14;
                for (String line : lines) {
                    context.drawTextWithShadow(textRenderer, 
                            Text.literal(line),
                            bubbleX + bubblePadding, textY, COLOR_TEXT_NORMAL);
                    textY += lineHeight;
                }
                
                currentY += bubbleHeight + msgPadding;
            }
            
            context.disableScissor();
            
            // Полоса прокрутки (если нужна)
            if (totalHeight > visibleHeight) {
                int scrollBarHeight = chatAreaHeight - 4;
                int thumbHeight = Math.max(20, (int)((float)visibleHeight / totalHeight * scrollBarHeight));
                int thumbY = chatAreaY + 2 + (int)((float)correctedScroll / maxScroll * (scrollBarHeight - thumbHeight));
                
                context.fill(btnX + btnWidth - 6, chatAreaY + 2, btnX + btnWidth - 2, chatAreaY + chatAreaHeight - 2, 0xFF333333);
                context.fill(btnX + btnWidth - 5, thumbY, btnX + btnWidth - 3, thumbY + thumbHeight, 0xFF666666);
            }
        }
        
        // Поле ввода сообщения (если тикет не закрыт)
        if (!activeTicket.status.equals("closed")) {
            int inputY = chatAreaY + chatAreaHeight + 6;
            int inputHeight = 24;
            int inputBg = supportMessageFocused ? 0xFF252525 : 0xFF1E1E1E;
            int inputBorder = supportMessageFocused ? COLOR_GOLD : 0xFF444444;
            context.fill(btnX, inputY, btnX + btnWidth - 40, inputY + inputHeight, inputBg);
            RenderUtils.drawBorder(context, btnX, inputY, btnWidth - 40, inputHeight, inputBorder);
            
            String displayMsg = supportMessageText.isEmpty() && !supportMessageFocused ? 
                    LocalizationManager.getInstance().get("write_message_placeholder") : supportMessageText;
            int textColor = supportMessageText.isEmpty() && !supportMessageFocused ? 
                    COLOR_TEXT_MUTED : COLOR_TEXT_NORMAL;
            String displayText = displayMsg + (supportMessageFocused ? "_" : "");
            
            int maxInputWidth = btnWidth - 40 - 12;
            if (supportMessageFocused && textRenderer.getWidth(displayText) > maxInputWidth) {
                String visibleText = supportMessageText;
                while (textRenderer.getWidth(visibleText + "_") > maxInputWidth && visibleText.length() > 0) {
                    visibleText = visibleText.substring(1);
                }
                displayText = visibleText + "_";
            } else if (textRenderer.getWidth(displayText) > maxInputWidth) {
                while (textRenderer.getWidth(displayText) > maxInputWidth && displayText.length() > 1) {
                    displayText = displayText.substring(1);
                }
            }
            context.drawTextWithShadow(textRenderer, Text.literal(displayText), 
                    btnX + 6, inputY + 8, textColor);
            
            // Кнопка отправки
            int sendBtnX = btnX + btnWidth - 36;
            boolean canSend = !supportMessageText.trim().isEmpty();
            boolean sendHovered = canSend && mouseX >= sendBtnX && mouseX < sendBtnX + 36 &&
                    mouseY >= inputY && mouseY < inputY + inputHeight;
            
            int sendBg = !canSend ? 0xFF2A2A2A : (sendHovered ? 0xFF3D6B3D : 0xFF2D5A2D);
            context.fill(sendBtnX, inputY, sendBtnX + 36, inputY + inputHeight, sendBg);
            RenderUtils.drawBorder(context, sendBtnX, inputY, 36, inputHeight, canSend ? 0xFF4CAF50 : 0xFF555555);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(">"),
                    sendBtnX + 14, inputY + 8, canSend ? 0xFFFFFFFF : 0xFF666666);
        } else {
            // Тикет закрыт
            int closedY = chatAreaY + chatAreaHeight + 10;
            String closedText = LocalizationManager.getInstance().get("ticket_closed").replace("!", "");
            int closedWidth = textRenderer.getWidth(closedText);
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(closedText),
                    btnX + (btnWidth - closedWidth) / 2, closedY, COLOR_TEXT_MUTED);
        }
    }

    /**
     * Рисует чат пользователя с администрацией (в панели Support)
     */
    public static void drawAdminChatInSupport(DrawContext context, TextRenderer textRenderer,
            int panelX, int contentY, int panelWidth, int panelHeight, 
            int mouseX, int mouseY,
            List<SupabaseClient.AdminUserMessage> userChatMessages,
            String userChatInput, boolean userChatInputFocused,
            java.util.function.BiFunction<String, Integer, String> trimTextToWidthFunc) {
        
        LocalizationManager lang = LocalizationManager.getInstance();
        int btnX = panelX + 8;
        int btnWidth = panelWidth - 16;
        int formY = contentY + 24;
        
        // Кнопка назад
        int backBtnY = formY;
        int btnHeight = 20;
        boolean backHovered = mouseX >= btnX && mouseX < btnX + 60 &&
                mouseY >= backBtnY && mouseY < backBtnY + btnHeight;
        
        int backBg = backHovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        context.fill(btnX, backBtnY, btnX + 60, backBtnY + btnHeight, backBg);
        RenderUtils.drawBorder(context, btnX, backBtnY, 60, btnHeight, COLOR_BORDER);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(lang.get("back")),
                btnX + 8, backBtnY + 6, COLOR_TEXT_NORMAL);
        
        // Заголовок чата
        String chatTitle = lang.get("chat_with_support");
        context.drawTextWithShadow(textRenderer, 
                Text.literal(chatTitle),
                btnX + 70, backBtnY + 6, COLOR_GOLD);
        
        // Область сообщений
        int chatAreaY = backBtnY + btnHeight + 8;
        int chatAreaHeight = panelHeight - 100;
        context.fill(btnX, chatAreaY, btnX + btnWidth, chatAreaY + chatAreaHeight, 0xFF1A1A1A);
        RenderUtils.drawBorder(context, btnX, chatAreaY, btnWidth, chatAreaHeight, COLOR_BORDER);
        
        // Отображение сообщений
        if (userChatMessages.isEmpty()) {
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(lang.get("no_messages")),
                    btnX + 10, chatAreaY + 20, COLOR_TEXT_MUTED);
        } else {
            int msgY = chatAreaY + 5;
            int msgHeight = 28;
            int maxVisible = (chatAreaHeight - 10) / msgHeight;
            
            int startIdx = Math.max(0, userChatMessages.size() - maxVisible);
            for (int i = startIdx; i < userChatMessages.size(); i++) {
                SupabaseClient.AdminUserMessage msg = userChatMessages.get(i);
                boolean isFromAdmin = "admin".equals(msg.senderType);
                
                // Сообщения от админа слева, от пользователя справа
                int msgBgColor = isFromAdmin ? COLOR_CHAT_OTHER_MSG : COLOR_CHAT_MY_MSG;
                int msgTextWidth = Math.min(textRenderer.getWidth(msg.content), btnWidth - 40);
                int msgBoxWidth = msgTextWidth + 16;
                int msgBoxHeight = 16;
                
                int msgX;
                if (isFromAdmin) {
                    msgX = btnX + 5;
                } else {
                    msgX = btnX + btnWidth - msgBoxWidth - 5;
                }
                
                // Фон сообщения
                context.fill(msgX, msgY, msgX + msgBoxWidth, msgY + msgBoxHeight, msgBgColor);
                RenderUtils.drawBorder(context, msgX, msgY, msgBoxWidth, msgBoxHeight, COLOR_BORDER);
                
                // Текст
                String displayText = msg.content;
                if (textRenderer.getWidth(displayText) > btnWidth - 50) {
                    displayText = trimTextToWidthFunc.apply(displayText, btnWidth - 60) + "...";
                }
                context.drawTextWithShadow(textRenderer, Text.literal(displayText),
                        msgX + 8, msgY + 4, COLOR_TEXT_NORMAL);
                
                // Подпись под сообщением
                String senderLabel = isFromAdmin ? lang.get("support_label") : lang.get("you").replace(": ", "");
                int labelColor = isFromAdmin ? COLOR_GOLD : COLOR_TEXT_MUTED;
                int labelX = isFromAdmin ? msgX : (msgX + msgBoxWidth - textRenderer.getWidth(senderLabel));
                context.drawTextWithShadow(textRenderer, Text.literal(senderLabel),
                        labelX, msgY + msgBoxHeight + 2, labelColor);
                
                msgY += msgHeight;
            }
        }
        
        // Поле ввода
        int inputY = chatAreaY + chatAreaHeight + 6;
        int inputHeight = 24;
        int inputBg = userChatInputFocused ? 0xFF252525 : 0xFF1E1E1E;
        int inputBorder = userChatInputFocused ? COLOR_GOLD : 0xFF444444;
        context.fill(btnX, inputY, btnX + btnWidth - 40, inputY + inputHeight, inputBg);
        RenderUtils.drawBorder(context, btnX, inputY, btnWidth - 40, inputHeight, inputBorder);
        
        String displayInput = userChatInput.isEmpty() && !userChatInputFocused ? 
                lang.get("write_message_placeholder") : userChatInput;
        int inputTextColor = userChatInput.isEmpty() && !userChatInputFocused ? 
                COLOR_TEXT_MUTED : COLOR_TEXT_NORMAL;
        String inputText = displayInput + (userChatInputFocused ? "_" : "");
        context.drawTextWithShadow(textRenderer, Text.literal(inputText), 
                btnX + 6, inputY + 8, inputTextColor);
        
        // Кнопка отправки
        int sendBtnX = btnX + btnWidth - 36;
        boolean canSend = !userChatInput.trim().isEmpty();
        boolean sendHovered = canSend && mouseX >= sendBtnX && mouseX < sendBtnX + 36 &&
                mouseY >= inputY && mouseY < inputY + inputHeight;
        
        int sendBg = !canSend ? 0xFF2A2A2A : (sendHovered ? 0xFF3D6B3D : 0xFF2D5A2D);
        context.fill(sendBtnX, inputY, sendBtnX + 36, inputY + inputHeight, sendBg);
        RenderUtils.drawBorder(context, sendBtnX, inputY, 36, inputHeight, canSend ? 0xFF4CAF50 : 0xFF555555);
        context.drawTextWithShadow(textRenderer, 
                Text.literal(">"),
                sendBtnX + 14, inputY + 8, canSend ? 0xFFFFFFFF : 0xFF666666);
    }
}
