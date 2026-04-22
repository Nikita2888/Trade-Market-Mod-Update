package com.trademarket.client.screen;

import com.trademarket.data.MarketListing;
import com.trademarket.data.SupabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Состояние экрана TradeMarketScreen
 * Содержит все изменяемые поля состояния, которые используются в экране
 */
public class ScreenState {

    // ==================== ПОЗИЦИЯ GUI ====================
    
    public int guiLeft;
    public int guiTop;

    // ==================== НАВИГАЦИЯ ====================
    
    public int currentPage = 0;
    public int currentTab = 0;
    public int hoveredSidebarTab = -1;

    // ==================== ЛИСТИНГИ ====================
    
    public List<MarketListing> displayedListings = new ArrayList<>();
    public int selectedInventorySlot = -1;
    public MarketListing selectedListing = null;
    public boolean viewingDetails = false;
    public boolean hasExistingDealOnListing = false;

    // ==================== TOOLTIP ====================
    
    public boolean showInstructionTooltip = false;
    public int instructionTooltipX = 0;
    public int instructionTooltipY = 0;

    // ==================== ЧАТ ЛОТА ====================
    
    public List<SupabaseClient.ChatMessage> chatMessages = new ArrayList<>();
    public int chatScrollOffset = 0;
    public long lastChatRefresh = 0;
    public int chatMaxVisible = 5;

    // ==================== ПОЛЯ ВВОДА ====================
    
    public String priceText = "";
    public String descriptionText = "";
    public String messageText = "";
    public boolean priceFocused = false;
    public boolean descriptionFocused = false;
    public boolean messageFocused = false;

    // ==================== ЗАГРУЗКА И СТАТУС ====================
    
    public long lastRefreshTime = 0;
    public long lastSupabaseRefresh = 0;
    public boolean isLoading = false;
    public String statusMessage = null;
    public long statusMessageTime = 0;

    // ==================== ЗАКЛАДКА СОЦСЕТЕЙ ====================
    
    public boolean bookmarkExpanded = false;
    
    // Перетаскивание панели Info
    public int infoPanelOffsetX = 0;
    public int infoPanelOffsetY = 0;
    public boolean isDraggingInfoPanel = false;
    public int infoDragStartX = 0;
    public int infoDragStartY = 0;

    // ==================== СИСТЕМА ПОДДЕРЖКИ ====================
    
    public boolean supportExpanded = false;
    public List<SupportModels.SupportTicket> supportTickets = new ArrayList<>();
    public SupportModels.SupportTicket activeTicket = null;
    public List<SupportModels.TicketMessage> ticketMessages = new ArrayList<>();
    public String supportMessageText = "";
    public boolean supportMessageFocused = false;
    public int supportTicketScroll = 0;
    public int supportChatScroll = 0;
    public String newTicketSubject = "";
    public boolean newTicketSubjectFocused = false;
    public boolean creatingNewTicket = false;
    public boolean isCreatingTicket = false;
    public boolean supportOnline = false;
    public long lastSupportStatusCheck = 0;
    public long lastTicketMessagesRefresh = 0;
    public long lastAdminHeartbeat = 0;
    
    // Перетаскивание панели поддержки
    public int supportPanelOffsetX = 0;
    public int supportPanelOffsetY = 0;
    public boolean isDraggingSupportPanel = false;
    public int dragStartX = 0;
    public int dragStartY = 0;

    // ==================== АДМИН-СИСТЕМА ====================
    
    public SupabaseClient.AdminInfo currentAdminInfo = null;
    public List<SupabaseClient.UserBan> currentUserBans = new ArrayList<>();
    public boolean isCurrentUserBannedListing = false;
    public boolean isCurrentUserBannedBuying = false;
    public boolean isCurrentUserBannedChat = false;
    public boolean isCurrentUserBannedSupport = false;
    public boolean isCurrentUserBannedFull = false;
    
    // Админ панель в деталях лота
    public boolean showAdminPanel = false;
    public String adminBanReason = "";
    public boolean adminBanReasonFocused = false;
    public String adminBanType = "listing";
    public int adminBanDuration = 0;
    
    // Перетаскивание админ-панели
    public int adminPanelOffsetX = 0;
    public int adminPanelOffsetY = 0;
    public boolean isDraggingAdminPanel = false;
    public int adminDragStartX = 0;
    public int adminDragStartY = 0;

    // ==================== РЕДАКТИРОВАНИЕ ЛОТА ====================
    
    public boolean isEditingListing = false;
    public MarketListing editingListing = null;

    // ==================== ВКЛАДКА ПОЛЬЗОВАТЕЛЕЙ (АДМИН) ====================
    
    public List<SupabaseClient.OnlineUser> onlineUsers = new ArrayList<>();
    public long lastOnlineUsersRefresh = 0;
    public int onlineUsersScrollOffset = 0;
    
    // Чат админ-юзер (для админов)
    public boolean showAdminUserChat = false;
    public SupabaseClient.AdminUserChat activeAdminUserChat = null;
    public SupabaseClient.OnlineUser selectedOnlineUser = null;
    public List<SupabaseClient.AdminUserMessage> adminUserMessages = new ArrayList<>();
    public String adminUserChatInput = "";
    public boolean adminUserChatInputFocused = false;
    public long lastAdminUserMessagesRefresh = 0;
    public int adminUserChatScrollOffset = 0;

    // ==================== ЧАТЫ С ПОДДЕРЖКОЙ (ДЛЯ ПОЛЬЗОВАТЕЛЕЙ) ====================
    
    public List<SupportModels.AdminChatForUser> userAdminChats = new ArrayList<>();
    public SupportModels.AdminChatForUser activeUserAdminChat = null;
    public List<SupabaseClient.AdminUserMessage> userChatMessages = new ArrayList<>();
    public String userChatInput = "";
    public boolean userChatInputFocused = false;
    public long lastUserChatsRefresh = 0;
    public int unreadAdminMessagesCount = 0;
    public boolean showingAdminChatInSupport = false;

    // ==================== НЕПРОЧИТАННЫЕ СООБЩЕНИЯ В ЧАТАХ ====================
    
    public int unreadMarketMessagesCount = 0;
    public long lastUnreadMarketMessagesCheck = 0;
    public Map<UUID, Integer> listingUnreadCounts = new HashMap<>();

    // ==================== ПОИСК И ФИЛЬТРЫ ====================
    
    public String searchText = "";
    public boolean searchFocused = false;
    public int currentSortMode = 0;

    // ==================== РЕПУТАЦИЯ ПРОДАВЦА ====================
    
    public SupabaseClient.SellerReputation currentSellerReputation = null;
    public UUID lastLoadedSellerReputationId = null;
    
    // UI для оценки продавца
    public boolean showRatingUI = false;
    public int selectedRating = 0;
    public boolean hasAlreadyRated = false;
    public boolean hasConfirmedTransaction = false;

    // ==================== PENDING TRANSACTIONS ====================
    
    public List<SupabaseClient.Transaction> pendingTransactions = new ArrayList<>();
    public long lastPendingTransactionsRefresh = 0;
    public boolean showPendingTransactionsPanel = false;
    public int pendingTransactionsScroll = 0;
    
    public List<SupabaseClient.Transaction> sellerPendingTransactions = new ArrayList<>();
    public boolean showSellerPendingPanel = false;
    public int sellerPendingScroll = 0;

    /**
     * Сбрасывает состояние деталей лота при выходе из просмотра
     */
    public void resetDetailsState() {
        selectedListing = null;
        viewingDetails = false;
        hasExistingDealOnListing = false;
        showInstructionTooltip = false;
        chatMessages.clear();
        chatScrollOffset = 0;
        messageText = "";
        messageFocused = false;
        showAdminPanel = false;
        adminPanelOffsetX = 0;
        adminPanelOffsetY = 0;
        currentSellerReputation = null;
        lastLoadedSellerReputationId = null;
        showRatingUI = false;
        selectedRating = 0;
        hasAlreadyRated = false;
        hasConfirmedTransaction = false;
    }

    /**
     * Сбрасывает состояние формы создания лота
     */
    public void resetSellFormState() {
        selectedInventorySlot = -1;
        priceText = "";
        descriptionText = "";
        priceFocused = false;
        descriptionFocused = false;
        isEditingListing = false;
        editingListing = null;
    }

    /**
     * Сбрасывает фокус со всех полей ввода
     */
    public void clearAllFocus() {
        priceFocused = false;
        descriptionFocused = false;
        messageFocused = false;
        supportMessageFocused = false;
        newTicketSubjectFocused = false;
        adminBanReasonFocused = false;
        adminUserChatInputFocused = false;
        userChatInputFocused = false;
        searchFocused = false;
    }

    /**
     * Устанавливает статусное сообщение с временной меткой
     */
    public void setStatusMessage(String message) {
        this.statusMessage = message;
        if (message != null) {
            this.statusMessageTime = System.currentTimeMillis();
        }
    }
}
