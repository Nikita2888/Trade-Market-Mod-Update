package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.client.SoundManager;
import com.trademarket.data.SupabaseClient;
import net.minecraft.client.MinecraftClient;

import java.util.List;

import static com.trademarket.client.screen.ScreenConstants.*;
import com.trademarket.client.screen.SupportModels.SupportTicket;
import com.trademarket.client.screen.SupportModels.AdminChatForUser;

/**
 * Вспомогательный класс для обработки кликов в TradeMarketScreen.
 * Содержит статические методы, возвращающие результат обработки клика.
 */
public class ClickHandlers {

    /**
     * Результат обработки клика - содержит информацию о том, что нужно сделать
     */
    public static class ClickResult {
        public final boolean consumed;
        public final ClickAction action;
        public final Object data;
        
        private ClickResult(boolean consumed, ClickAction action, Object data) {
            this.consumed = consumed;
            this.action = action;
            this.data = data;
        }
        
        public static ClickResult notConsumed() {
            return new ClickResult(false, ClickAction.NONE, null);
        }
        
        public static ClickResult consumed() {
            return new ClickResult(true, ClickAction.NONE, null);
        }
        
        public static ClickResult withAction(ClickAction action) {
            return new ClickResult(true, action, null);
        }
        
        public static ClickResult withAction(ClickAction action, Object data) {
            return new ClickResult(true, action, data);
        }
    }
    
    /**
     * Действия, которые могут быть инициированы кликом
     */
    public enum ClickAction {
        NONE,
        TOGGLE_BOOKMARK,
        TOGGLE_SUPPORT,
        START_DRAG_INFO_PANEL,
        START_DRAG_SUPPORT_PANEL,
        COPY_DISCORD,
        COPY_TELEGRAM,
        CREATE_NEW_TICKET,
        OPEN_TICKET,
        OPEN_ADMIN_CHAT,
        BACK_FROM_TICKET,
        BACK_FROM_NEW_TICKET,
        BACK_FROM_ADMIN_CHAT,
        SUBMIT_NEW_TICKET,
        SEND_TICKET_MESSAGE,
        SEND_USER_CHAT_MESSAGE,
        CLOSE_TICKET,
        FOCUS_TICKET_SUBJECT,
        FOCUS_TICKET_MESSAGE,
        FOCUS_USER_CHAT_INPUT,
        UNFOCUS_ALL
    }
    
    /**
     * Данные для действия открытия тикета
     */
    public static class OpenTicketData {
        public final SupportTicket ticket;
        public OpenTicketData(SupportTicket ticket) {
            this.ticket = ticket;
        }
    }
    
    /**
     * Данные для действия открытия чата с админом
     */
    public static class OpenAdminChatData {
        public final AdminChatForUser chat;
        public OpenAdminChatData(AdminChatForUser chat) {
            this.chat = chat;
        }
    }
    
    /**
     * Данные для начала перетаскивания
     */
    public static class DragStartData {
        public final int startX;
        public final int startY;
        public DragStartData(int startX, int startY) {
            this.startX = startX;
            this.startY = startY;
        }
    }

    /**
     * Обрабатывает клик по закладке соцсетей
     */
    public static ClickResult handleSocialBookmarkClick(int mx, int my, 
            int currentGuiRight, int guiTop,
            boolean bookmarkExpanded,
            int infoPanelOffsetX, int infoPanelOffsetY,
            int screenWidth, int screenHeight) {
        
        int tabX = currentGuiRight + 6;
        int tabY = guiTop + 8;
        int tabSize = 28;
        
        // Клик по кнопке для открытия/закрытия
        if (mx >= tabX && mx < tabX + tabSize && my >= tabY && my < tabY + tabSize) {
            return ClickResult.withAction(ClickAction.TOGGLE_BOOKMARK);
        }
        
        // Если закладка раскрыта - проверяем клики по кнопкам
        if (bookmarkExpanded) {
            int basePanelX = tabX + tabSize;
            int basePanelY = tabY;
            int panelWidth = 160;
            int panelHeight = 175;
            
            // Применяем смещение
            int panelX = basePanelX + infoPanelOffsetX;
            int panelY = basePanelY + infoPanelOffsetY;
            
            // Ограничиваем позицию
            panelX = Math.max(10, Math.min(screenWidth - panelWidth - 10, panelX));
            panelY = Math.max(10, Math.min(screenHeight - panelHeight - 10, panelY));
            
            int headerHeight = 20;
            int btnHeight = 22;
            int btnX = panelX + 8;
            int btnWidth = panelWidth - 16;
            
            // Клик по заголовку для перетаскивания
            if (mx >= panelX && mx < panelX + panelWidth &&
                my >= panelY && my < panelY + headerHeight) {
                return ClickResult.withAction(ClickAction.START_DRAG_INFO_PANEL, 
                    new DragStartData(mx - infoPanelOffsetX, my - infoPanelOffsetY));
            }
            
            // Discord клик
            int discordY = panelY + 22;
            if (mx >= btnX && mx < btnX + btnWidth && my >= discordY && my < discordY + btnHeight) {
                return ClickResult.withAction(ClickAction.COPY_DISCORD);
            }
            
            // Telegram клик
            int telegramY = discordY + btnHeight + 6;
            if (mx >= btnX && mx < btnX + btnWidth && my >= telegramY && my < telegramY + btnHeight) {
                return ClickResult.withAction(ClickAction.COPY_TELEGRAM);
            }
        }
        
        return ClickResult.notConsumed();
    }

    /**
     * Обрабатывает клик в списке тикетов
     */
    public static ClickResult handleTicketListClick(int mx, int my, int btnX, int btnWidth, int listY,
            List<SupportTicket> supportTickets, int supportTicketScroll,
            List<AdminChatForUser> userAdminChats) {
        
        int btnHeight = 26; // Высота кнопки "Новый тикет"
        int newTicketBtnY = listY;
        
        // Клик по кнопке "Новый тикет"
        if (mx >= btnX && mx < btnX + btnWidth && my >= newTicketBtnY && my < newTicketBtnY + btnHeight) {
            return ClickResult.withAction(ClickAction.CREATE_NEW_TICKET);
        }
        
        // Клик по тикету в списке
        int separatorY = newTicketBtnY + btnHeight + 10;
        int ticketListY = separatorY + 8;
        int ticketHeight = 36;
        int maxVisibleTickets = 4;
        
        for (int i = 0; i < Math.min(maxVisibleTickets, supportTickets.size() - supportTicketScroll); i++) {
            int ticketIndex = i + supportTicketScroll;
            if (ticketIndex >= supportTickets.size()) break;
            
            int ticketY = ticketListY + i * (ticketHeight + 4);
            if (mx >= btnX && mx < btnX + btnWidth && my >= ticketY && my < ticketY + ticketHeight) {
                return ClickResult.withAction(ClickAction.OPEN_TICKET, 
                    new OpenTicketData(supportTickets.get(ticketIndex)));
            }
        }
        
        // Клик по чатам от администрации
        if (!userAdminChats.isEmpty()) {
            int adminChatsY;
            if (supportTickets.isEmpty()) {
                adminChatsY = ticketListY + 70;
            } else {
                adminChatsY = ticketListY + (Math.min(maxVisibleTickets, supportTickets.size()) * (ticketHeight + 4)) + 15;
            }
            int chatItemY = adminChatsY + 14;
            int chatItemHeight = 32;
            
            for (int i = 0; i < Math.min(3, userAdminChats.size()); i++) {
                if (mx >= btnX && mx < btnX + btnWidth && my >= chatItemY && my < chatItemY + chatItemHeight) {
                    return ClickResult.withAction(ClickAction.OPEN_ADMIN_CHAT,
                        new OpenAdminChatData(userAdminChats.get(i)));
                }
                chatItemY += chatItemHeight + 4;
            }
        }
        
        return ClickResult.consumed(); // Поглощаем клик внутри панели
    }

    /**
     * Обрабатывает клик в форме нового тикета
     */
    public static ClickResult handleNewTicketFormClick(int mx, int my, int btnX, int btnWidth, int formY,
            String newTicketSubject) {
        
        int btnHeight = 20;
        int backBtnY = formY;
        
        // Кнопка назад
        if (mx >= btnX && mx < btnX + 60 && my >= backBtnY && my < backBtnY + btnHeight) {
            return ClickResult.withAction(ClickAction.BACK_FROM_NEW_TICKET);
        }
        
        // Поле ввода темы
        int subjectY = formY + 30;
        int inputY = subjectY + 14;
        int inputHeight = 28;
        if (mx >= btnX && mx < btnX + btnWidth && my >= inputY && my < inputY + inputHeight) {
            return ClickResult.withAction(ClickAction.FOCUS_TICKET_SUBJECT);
        }
        
        // Кнопка создания тикета
        int instructionY = inputY + inputHeight + 16;
        int sendBtnY = instructionY + 60;
        int sendBtnHeight = 28;
        if (!newTicketSubject.trim().isEmpty() && 
            mx >= btnX && mx < btnX + btnWidth && my >= sendBtnY && my < sendBtnY + sendBtnHeight) {
            return ClickResult.withAction(ClickAction.SUBMIT_NEW_TICKET);
        }
        
        // Снимаем фокус если клик вне поля ввода
        return ClickResult.withAction(ClickAction.UNFOCUS_ALL);
    }

    /**
     * Обрабатывает клик в чате тикета
     */
    public static ClickResult handleTicketChatClick(int mx, int my, int btnX, int btnWidth, int chatY,
            SupportTicket activeTicket, String supportMessageText, boolean isAdmin) {
        
        int btnHeight = 20;
        int backBtnY = chatY;
        int panelWidth = btnWidth + 16;
        int panelX = btnX - 8;
        int headerHeight = isAdmin ? 40 : 24;
        
        // Кнопка назад
        if (mx >= btnX && mx < btnX + 60 && my >= backBtnY && my < backBtnY + btnHeight) {
            return ClickResult.withAction(ClickAction.BACK_FROM_TICKET);
        }
        
        // Кнопка закрытия тикета (для админов)
        if (isAdmin && activeTicket != null && !activeTicket.status.equals("closed")) {
            int closeBtnX = panelX + panelWidth - 75;
            int closeBtnY = chatY + 18;
            int closeBtnWidth = 67;
            int closeBtnHeight = 18;
            if (mx >= closeBtnX && mx < closeBtnX + closeBtnWidth && 
                my >= closeBtnY && my < closeBtnY + closeBtnHeight) {
                return ClickResult.withAction(ClickAction.CLOSE_TICKET);
            }
        }
        
        // Область чата и поле ввода
        int chatAreaY = chatY + headerHeight + 4;
        int chatAreaHeight = isAdmin ? 155 : 170;
        
        if (activeTicket != null && !activeTicket.status.equals("closed")) {
            int inputY = chatAreaY + chatAreaHeight + 6;
            int inputHeight = 24;
            
            // Поле ввода
            if (mx >= btnX && mx < btnX + btnWidth - 40 && my >= inputY && my < inputY + inputHeight) {
                return ClickResult.withAction(ClickAction.FOCUS_TICKET_MESSAGE);
            }
            
            // Кнопка отправки
            int sendBtnX = btnX + btnWidth - 36;
            if (!supportMessageText.trim().isEmpty() && 
                mx >= sendBtnX && mx < sendBtnX + 36 && my >= inputY && my < inputY + inputHeight) {
                return ClickResult.withAction(ClickAction.SEND_TICKET_MESSAGE);
            }
        }
        
        return ClickResult.consumed();
    }

    /**
     * Обрабатывает клик в чате пользователя с админом
     */
    public static ClickResult handleAdminChatInSupportClick(int mx, int my, int btnX, int btnWidth, int formY,
            String userChatInput, int panelHeight) {
        
        int btnHeight = 20;
        int backBtnY = formY;
        
        // Кнопка назад
        if (mx >= btnX && mx < btnX + 60 && my >= backBtnY && my < backBtnY + btnHeight) {
            return ClickResult.withAction(ClickAction.BACK_FROM_ADMIN_CHAT);
        }
        
        // Поле ввода сообщения
        int chatAreaY = backBtnY + btnHeight + 8;
        int chatAreaHeight = panelHeight - 100;
        int inputY = chatAreaY + chatAreaHeight + 6;
        int inputHeight = 24;
        
        if (mx >= btnX && mx < btnX + btnWidth - 40 && my >= inputY && my < inputY + inputHeight) {
            return ClickResult.withAction(ClickAction.FOCUS_USER_CHAT_INPUT);
        }
        
        // Кнопка отправки
        int sendBtnX = btnX + btnWidth - 36;
        if (!userChatInput.trim().isEmpty() && 
            mx >= sendBtnX && mx < sendBtnX + 36 && my >= inputY && my < inputY + inputHeight) {
            return ClickResult.withAction(ClickAction.SEND_USER_CHAT_MESSAGE);
        }
        
        return ClickResult.consumed();
    }
}
