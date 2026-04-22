package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.client.RemoteLogger;
import com.trademarket.client.SoundManager;
import com.trademarket.client.ToastNotificationManager;
import com.trademarket.data.MarketDataManager;
import com.trademarket.data.MarketListing;
import com.trademarket.data.SupabaseClient;
import com.trademarket.network.NetworkHandler;
import com.trademarket.TradeMarketMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Импорт модульных классов
import static com.trademarket.client.screen.ScreenConstants.*;
import static com.trademarket.client.screen.TextUtils.stripWynnAlignment;
import static com.trademarket.client.screen.TextUtils.isWynnLineEmpty;
import static com.trademarket.client.screen.TextUtils.formatTime;
import static com.trademarket.client.screen.TextUtils.formatDate;
import com.trademarket.client.screen.SupportModels.SupportTicket;
import com.trademarket.client.screen.SupportModels.TicketMessage;
import com.trademarket.client.screen.SupportModels.AdminChatForUser;
import com.trademarket.client.screen.SupportModels.TransactionWithType;
import com.trademarket.client.screen.RenderUtils;

@Environment(EnvType.CLIENT)
public class TradeMarketScreen extends Screen {
    
    // Все константы вынесены в ScreenConstants.java

    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private int currentTab = 0;
    private int hoveredSidebarTab = -1; // Для отображения тултипа

    private List<MarketListing> displayedListings;
    private int selectedInventorySlot = -1;
    private MarketListing selectedListing = null;
    private boolean viewingDetails = false;
    private boolean hasExistingDealOnListing = false; // ��сть ли уже сделка на выбранный лот
    
    // Tooltip инструкции
    private boolean showInstructionTooltip = false;
    private int instructionTooltipX = 0;
    private int instructionTooltipY = 0;
    
    // Чат
    private List<SupabaseClient.ChatMessage> chatMessages = new ArrayList<>();
    private int chatScrollOffset = 0;
    private long lastChatRefresh = 0;
    private int chatMaxVisible = 5; // Будет обновляться при рендере
    
    // Поля ввода
    private String priceText = "";           // Поле для цены
    private String descriptionText = "";      // Поле для условий сделки
    private String messageText = "";
    private boolean priceFocused = false;
    private boolean descriptionFocused = false;
    private boolean messageFocused = false;
    
    // Для автообновления
    private long lastRefreshTime = 0;
    private long lastSupabaseRefresh = 0;
    private boolean isLoading = false;
    private String statusMessage = null;
    
    // Закладка соцсетей
    private boolean bookmarkExpanded = false;
    
    // Закладка поддержки (Support)
    private boolean supportExpanded = false;
    private List<SupportTicket> supportTickets = new ArrayList<>();
    private SupportTicket activeTicket = null;
    private List<TicketMessage> ticketMessages = new ArrayList<>();
    private String supportMessageText = "";
    private boolean supportMessageFocused = false;
    private int supportTicketScroll = 0;
    private int supportChatScroll = 0;
    private String newTicketSubject = "";
    private boolean newTicketSubjectFocused = false;
    private boolean creatingNewTicket = false;
    private boolean supportOnline = false;
    private long lastSupportStatusCheck = 0;
    
    // Автообновление сообщений тикета
    private long lastTicketMessagesRefresh = 0;
    
    // Admin heartbeat для онлайн статуса
    private long lastAdminHeartbeat = 0;
    
    // Перетаскивание панели поддержки
    private int supportPanelOffsetX = 0;
    private int supportPanelOffsetY = 0;
    private boolean isDraggingSupportPanel = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    
    // Перетаскивание панели Info
    private int infoPanelOffsetX = 0;
    private int infoPanelOffsetY = 0;
    private boolean isDraggingInfoPanel = false;
    private int infoDragStartX = 0;
    private int infoDragStartY = 0;
    
    // Перетаскивание админ-панели
    private int adminPanelOffsetX = 0;
    private int adminPanelOffsetY = 0;
    private boolean isDraggingAdminPanel = false;
    private int adminDragStartX = 0;
    private int adminDragStartY = 0;
    
    // Время показа статусного сообщения
    private long statusMessageTime = 0;
    
    // Админ-система
    private SupabaseClient.AdminInfo currentAdminInfo = null;
    private List<SupabaseClient.UserBan> currentUserBans = new ArrayList<>();
    private boolean isCurrentUserBannedListing = false;
    private boolean isCurrentUserBannedBuying = false;
    private boolean isCurrentUserBannedChat = false;
    private boolean isCurrentUserBannedSupport = false;
    private boolean isCurrentUserBannedFull = false;
    
    // Админ панель в деталях лота
    private boolean showAdminPanel = false;
    private String adminBanReason = "";
    private boolean adminBanReasonFocused = false;
    private String adminBanType = "listing"; // По умолчанию
    private int adminBanDuration = 0; // 0 = перманентный
    
    // Режим редактирования
    private boolean isEditingListing = false;
    private MarketListing editingListing = null;
    
    // ===== ВКЛАДКА ПОЛЬЗОВАТЕЛЕЙ (АДМИН) =====
    private List<SupabaseClient.OnlineUser> onlineUsers = new ArrayList<>();
    private long lastOnlineUsersRefresh = 0;
    private int onlineUsersScrollOffset = 0;
    
    // Чат админ-юзер (для админов)
    private boolean showAdminUserChat = false;
    private SupabaseClient.AdminUserChat activeAdminUserChat = null;
    private SupabaseClient.OnlineUser selectedOnlineUser = null;
    private List<SupabaseClient.AdminUserMessage> adminUserMessages = new ArrayList<>();
    private String adminUserChatInput = "";
    private boolean adminUserChatInputFocused = false;
    private long lastAdminUserMessagesRefresh = 0;
    private int adminUserChatScrollOffset = 0;
    
    // Чаты с поддержкой (для обычных пользователей) - во вкладке Support
    private List<AdminChatForUser> userAdminChats = new ArrayList<>();
    private AdminChatForUser activeUserAdminChat = null;
    private List<SupabaseClient.AdminUserMessage> userChatMessages = new ArrayList<>();
    private String userChatInput = "";
    private boolean userChatInputFocused = false;
    private long lastUserChatsRefresh = 0;
    private int unreadAdminMessagesCount = 0;
    private boolean showingAdminChatInSupport = false; // Режим отображения чата с админом в Support
    
    // Непрочитанные сообщения в чатах продавец-покупатель
    private int unreadMarketMessagesCount = 0;
    private long lastUnreadMarketMessagesCheck = 0;
    // Map: listing_id -> unread_count (для отображения индикаторов на главном экране)
    private java.util.Map<UUID, Integer> listingUnreadCounts = new java.util.HashMap<>();
    
    // Поиск и фильтры
    private String searchText = "";
    private boolean searchFocused = false;
    private int currentSortMode = 0; // 0=newest, 1=oldest, 2=price_low, 3=price_high
    
    // Репутация продавца (кэш для текущего просматриваемого лота)
    private SupabaseClient.SellerReputation currentSellerReputation = null;
    private UUID lastLoadedSellerReputationId = null;
    
    // UI для оценки продавца
    private boolean showRatingUI = false;
    private int selectedRating = 0; // 1-5 звезд
    private boolean hasAlreadyRated = false;
    private boolean hasConfirmedTransaction = false; // Есть ли подтвержденная сделка с продавцом
    
    // Ожидающие подтверждения транзакции (для покупателя)
    private List<SupabaseClient.Transaction> pendingTransactions = new ArrayList<>();
    private long lastPendingTransactionsRefresh = 0;
    private boolean showPendingTransactionsPanel = false;
    private int pendingTransactionsScroll = 0;
    
    // Ожидающие подтверждения транзакции (для продавца - запросы на сделку)
    private List<SupabaseClient.Transaction> sellerPendingTransactions = new ArrayList<>();
    private boolean showSellerPendingPanel = false;
    private int sellerPendingScroll = 0;

    public TradeMarketScreen() {
        super(Text.literal("Trade Market"));
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;
        
        if (client.world != null) {
            MarketDataManager.getInstance().setRegistries(client.world.getRegistryManager());
        }
        
        loadFromSupabase();
        refreshListings();
        
        // Загружаем избранное пользователя
        if (client.player != null) {
            MarketDataManager.getInstance().loadFavorites(client.player.getUuid(), null, null);
            // Загружаем ожидающие подтверждения транзакции (как покупатель и как продавец)
            loadPendingTransactions();
            loadSellerPendingTransactions();
            // Загружаем непрочитанные сообщения в чатах
            loadUnreadMarketMessagesCount();
        }
    }
    
    /**
     * Загрузить ожидающие подтверждения транзакции для текущего покупателя
     */
    private void loadPendingTransactions() {
        if (client.player == null) return;
        
        lastPendingTransactionsRefresh = System.currentTimeMillis();
        SupabaseClient.getInstance().fetchPendingTransactions(
            client.player.getUuid(),
            transactions -> {
                pendingTransactions = transactions;
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load pending transactions: " + error)
        );
    }
    
    /**
     * Загрузить запросы на сделку для текущего ПРОДАВЦА
     */
    private void loadSellerPendingTransactions() {
        if (client.player == null) return;
        
        // Уведомления о новых запросах на сделку ��������брабатываются в NotificationChecker
        SupabaseClient.getInstance().fetchSellerPendingTransactions(
            client.player.getUuid(),
            transactions -> {
                sellerPendingTransactions = transactions;
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load seller pending transactions: " + error)
        );
    }
    
    /**
     * Начать сделку с продавцом (создает транзакцию со статусом pending)
     */
    private void startDealWithSeller() {
        if (selectedListing == null || client.player == null) return;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        // Проверяем, что покупатель не продавец
        if (selectedListing.getSellerId().equals(client.player.getUuid())) {
            return;
        }
        
        // Проверяем бан на покупки
        if (isCurrentUserBannedBuying || isCurrentUserBannedFull) {
            setStatusMessage(lang.get("buying_banned"));
            SoundManager.getInstance().playErrorSound();
            return;
        }
        
        // Сначала проверяем, нет ли уже pending транзакции для этого лота от этого покупателя
        SupabaseClient.getInstance().checkPendingTransactionExists(
            selectedListing.getListingId(),
            client.player.getUuid(),
            exists -> {
                if (exists) {
                    // Транзакция уже существует - показываем сообщение
                    setStatusMessage(lang.get("deal_already_exists"));
                    SoundManager.getInstance().playErrorSound();
                } else {
                    // Создаем новую транзакцию
                    createPendingTransaction();
                }
            },
            error -> {
                // При ошибке проверки все равно пытаемся создать (сервер отклонит дубликат)
                createPendingTransaction();
            }
        );
    }
    
    /**
     * Прове��ить, есть ли уже сделка (pending или completed) на выбранный лот от текущего покупателя
     */
    private void checkExistingDealOnListing() {
        if (selectedListing == null || client.player == null) return;
        
        // Если это свой лот - не проверяем
        if (selectedListing.getSellerId().equals(client.player.getUuid())) return;
        
        SupabaseClient.getInstance().checkPendingTransactionExists(
            selectedListing.getListingId(),
            client.player.getUuid(),
            exists -> {
                hasExistingDealOnListing = exists;
            },
            error -> {
                hasExistingDealOnListing = false;
            }
        );
    }
    
    /**
     * Создать pending транзакцию (вызывается после проверки на дубликат)
     */
    private void createPendingTransaction() {
        if (selectedListing == null || client.player == null) return;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        SupabaseClient.getInstance().recordTransaction(
            selectedListing.getListingId(),
            selectedListing.getSellerId(),
            selectedListing.getSellerName(),
            client.player.getUuid(),
            client.player.getName().getString(),
            selectedListing.getItemId(),
            selectedListing.getItemDisplayName(),
            selectedListing.getItemCount(),
            selectedListing.getPrice(),
            () -> {
                setStatusMessage(lang.get("deal_started"));
                SoundManager.getInstance().playSuccessSound();
                // Помечаем что сделка уже существует - предотвращает повторные клики
                hasExistingDealOnListing = true;
                // Обновляем pending transactions
                loadPendingTransactions();
                loadSellerPendingTransactions();
            },
            error -> {
                setStatusMessage(lang.get("error_generic", error));
                SoundManager.getInstance().playErrorSound();
            }
        );
    }
    
    private void loadFromSupabase() {
        if (isLoading) return;
        
        isLoading = true;
        // Убрано сообщение Loading - оно мешает и часто появляется
        // setStatusMessage(LocalizationManager.getInstance().get("loading"));
        lastSupabaseRefresh = System.currentTimeMillis();
        
        // Загружаем админ-информацию
        loadAdminInfo();
        
        // Загружаем блокировки текущего пользователя
        loadUserBans();
        
        // Загружаем тикеты
        loadUserTickets();
        
        // Проверяем статус поддержки
        checkSupportStatus();
        
        MarketDataManager.getInstance().forceRefresh(
            () -> {
                isLoading = false;
                setStatusMessage(null);
                refreshListings();
            },
            error -> {
                isLoading = false;
                    setStatusMessage(LocalizationManager.getInstance().get("error_connection"));
                TradeMarketMod.LOGGER.error("Supabase error: " + error);
            }
        );
    }
    
    private void loadAdminInfo() {
        if (client.player == null) return;
        String playerName = client.player.getName().getString();
        
        SupabaseClient.getInstance().checkIsAdmin(playerName,
            adminInfo -> {
                currentAdminInfo = adminInfo;
                if (adminInfo.isAdmin) {
                    TradeMarketMod.LOGGER.info("Admin logged in: " + playerName + " (" + adminInfo.role + ")");
                    // Обновляем last_active при входе админа и сбрасываем таймер heartbeat
                    SupabaseClient.getInstance().updateAdminLastActive(playerName);
                    lastAdminHeartbeat = System.currentTimeMillis();
                }
            },
            error -> TradeMarketMod.LOGGER.error("Error checking admin status: " + error)
        );
    }
    
    private void loadUserBans() {
        if (client.player == null) return;
        String playerName = client.player.getName().getString();
        
        SupabaseClient.getInstance().checkUserBans(playerName,
            bans -> {
                currentUserBans = bans;
                isCurrentUserBannedListing = false;
                isCurrentUserBannedBuying = false;
                isCurrentUserBannedChat = false;
                isCurrentUserBannedSupport = false;
                isCurrentUserBannedFull = false;
                
                for (SupabaseClient.UserBan ban : bans) {
                    if (ban.isActive && !ban.isExpired()) {
                        switch (ban.banType) {
                            case "listing": isCurrentUserBannedListing = true; break;
                            case "buying": isCurrentUserBannedBuying = true; break;
                            case "chat": isCurrentUserBannedChat = true; break;
                            case "support": isCurrentUserBannedSupport = true; break;
                            case "full": isCurrentUserBannedFull = true; break;
                        }
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.error("Error loading user bans: " + error)
        );
    }
    
    private void loadUserTickets() {
        if (client.player == null) return;
        String playerName = client.player.getName().getString();
        
        // Если админ - загружаем ��се открытые тикеты
        if (currentAdminInfo != null && currentAdminInfo.isAdmin) {
            SupabaseClient.getInstance().getAllOpenTickets(
                tickets -> {
                    supportTickets.clear();
                    for (SupabaseClient.SupportTicket t : tickets) {
                        supportTickets.add(new SupportTicket(t.id, t.subject, t.status, t.createdBy, t.createdAt));
                    }
                },
                error -> TradeMarketMod.LOGGER.error("Error loading tickets: " + error)
            );
        } else {
            // Обычный пользователь - только свои тикеты
            SupabaseClient.getInstance().getUserTickets(playerName,
                tickets -> {
                    supportTickets.clear();
                    for (SupabaseClient.SupportTicket t : tickets) {
                        supportTickets.add(new SupportTicket(t.id, t.subject, t.status, t.createdBy, t.createdAt));
                    }
                },
                error -> TradeMarketMod.LOGGER.error("Error loading user tickets: " + error)
            );
        }
    }
    
    private void checkSupportStatus() {
        lastSupportStatusCheck = System.currentTimeMillis();
        SupabaseClient.getInstance().checkSupportOnline(
            online -> supportOnline = online
        );
    }
    
    private void loadChatMessages() {
        loadChatMessages(true); // По умолчанию скроллим вниз
    }
    
    private void loadChatMessages(boolean scrollToBottom) {
        if (selectedListing == null) return;
        
        lastChatRefresh = System.currentTimeMillis();
        final int prevMessageCount = chatMessages.size();
        // Проверяем, бы�� ли пользователь внизу ���������ата (с у��етом небольшого запаса)
        final int maxScrollPrev = Math.max(0, chatMessages.size() - chatMaxVisible);
        final boolean wasAtBottom = chatMessages.isEmpty() || chatScrollOffset >= maxScrollPrev - 1;
        
        SupabaseClient.getInstance().fetchMessages(
            selectedListing.getListingId(),
            messages -> {
                // Уведомления о новых сообщениях обрабатываются в NotificationChecker
                // Здесь только обновляем список сообщений
                chatMessages = messages;
                
                // Всегда скроллим вниз если:
                // - явно запрошено scrollToBottom
                // - появились новые сообщения и пользователь был внизу чата
                if (scrollToBottom || (messages.size() > prevMessageCount && wasAtBottom)) {
                    // Скроллим в самый низ - offset указывает на первое видимое сообщение
                    // Чтобы показать последние сообщения, offset = total - maxVisible
                    int maxScroll = Math.max(0, messages.size() - chatMaxVisible);
                    chatScrollOffset = maxScroll;
                }
                // Иначе сохраняем текущую позицию (пользователь читает историю)
                
                // Помечаем сообщения как прочитанные
                markMarketChatMessagesAsRead();
            },
            error -> TradeMarketMod.LOGGER.error("Chat load error: " + error)
        );
    }
    
    /**
     * Помечает сообщения в чате продавец-покупатель как прочитанные
     */
    private void markMarketChatMessagesAsRead() {
        if (selectedListing == null || client.player == null) return;
        
        UUID listingId = selectedListing.getListingId();
        SupabaseClient.getInstance().markMarketMessagesAsRead(
            listingId,
            client.player.getUuid(),
            () -> {
                // Сразу удаляем из Map (для мгновенного обновления UI)
                listingUnreadCounts.remove(listingId);
                // Обновим счетчик непрочитанных сообщений
                loadUnreadMarketMessagesCount();
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to mark market messages as read: " + error)
        );
    }
    
    /**
     * Загружает количество непрочитанных сообщений в чатах продавец-покупатель
     */
    private void loadUnreadMarketMessagesCount() {
        if (client.player == null) return;
        
        SupabaseClient.getInstance().getUnreadMarketMessagesCount(
            client.player.getUuid(),
            count -> unreadMarketMessagesCount = count,
            error -> TradeMarketMod.LOGGER.debug("Failed to load unread market messages count: " + error)
        );
        
        // Также загружаем детальную информацию по листингам для отображения индикаторов
        SupabaseClient.getInstance().getUserActiveChats(
            client.player.getUuid(),
            chats -> {
                listingUnreadCounts.clear();
                for (SupabaseClient.ActiveChat chat : chats) {
                    if (chat.unreadCount > 0) {
                        listingUnreadCounts.put(chat.listingId, chat.unreadCount);
                    }
                }
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load listing unread counts: " + error)
        );
    }
    
    private void sendChatMessage() {
        if (selectedListing == null || messageText.isEmpty() || client.player == null) return;
        
        // Проверяем бан на чат
        if (isCurrentUserBannedChat || isCurrentUserBannedFull) {
            setStatusMessage(LocalizationManager.getInstance().get("chat_banned"));
            return;
        }
        
        String msg = messageText;
        messageText = "";
        
        // Используе�� sendChatMessage который проверяет бан на сервере
        SupabaseClient.getInstance().sendChatMessage(
            selectedListing.getListingId(),
            client.player.getUuid(),
            client.player.getName().getString(),
            msg,
            () -> loadChatMessages(true), // Скроллим вниз после отправки
            error -> {
                setStatusMessage(LocalizationManager.getInstance().get("error_generic", error));
                TradeMarketMod.LOGGER.error("Send message error: " + error);
            }
        );
    }

    private void refreshListings() {
        lastRefreshTime = System.currentTimeMillis();
        if (currentTab == 0) {
            // Используем фильтрацию если есть поисковый запрос
            if (!searchText.isEmpty() || currentSortMode != 0) {
                displayedListings = MarketDataManager.getInstance().getFilteredListings();
            } else {
                displayedListings = MarketDataManager.getInstance().getActiveListings();
            }
        } else if (currentTab == 1 && client.player != null) {
            displayedListings = MarketDataManager.getInstance()
                    .getListingsBySeller(client.player.getUuid());
        } else if (currentTab == 3) {
            // Вкладка "Избранное"
            displayedListings = MarketDataManager.getInstance().getFavoriteListings();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (System.currentTimeMillis() - lastRefreshTime > 500) {
            refreshListings();
        }
        
        if (System.currentTimeMillis() - lastSupabaseRefresh > SUPABASE_REFRESH_INTERVAL) {
            loadFromSupabase();
        }
        
        if (viewingDetails && System.currentTimeMillis() - lastChatRefresh > CHAT_REFRESH_INTERVAL) {
            loadChatMessages(false); // При автообновлении не сбрасываем скролл
        }
        
        // Периодическое обновление pending transactions (для покупателя и продавца)
        if (System.currentTimeMillis() - lastPendingTransactionsRefresh > PENDING_TRANSACTIONS_REFRESH_INTERVAL) {
            loadPendingTransactions();
            loadSellerPendingTransactions();
        }
        
        // Периодическое обновление индикатора непрочитанных сообщений (каждые 10 секунд)
        if (System.currentTimeMillis() - lastUnreadMarketMessagesCheck > UNREAD_MARKET_CHECK_INTERVAL) {
            lastUnreadMarketMessagesCheck = System.currentTimeMillis();
            loadUnreadMarketMessagesCount();
        }

        // Затемненный фон
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        // Главная панель
        drawPanel(context, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        drawTitle(context);
        
        if (viewingDetails && selectedListing != null) {
            renderDetailsView(context, mouseX, mouseY);
        } else {
            drawSidebar(context, mouseX, mouseY);
            
            if (currentTab == 0 || currentTab == 1 || currentTab == 3) {
                renderListings(context, mouseX, mouseY);
            } else if (currentTab == 2) {
                renderSellUI(context, mouseX, mouseY);
            } else if (currentTab == 4) {
                renderMessagesTab(context, mouseX, mouseY);
            } else if (currentTab == 5) {
                renderUsersTab(context, mouseX, mouseY);
            }
        }
        
        // Закладки рисуются ПОСЛЕ основного контента и sidebar, чтобы быть поверх
        drawSocialBookmark(context, mouseX, mouseY);
        drawSupportBookmark(context, mouseX, mouseY);
        
        // Панель ожидающих подтверждения транзакций
        drawPendingTransactionsPanel(context, mouseX, mouseY);
        
        // Tooltip инструкции (рисуем поверх всего)
        if (showInstructionTooltip && viewingDetails) {
            drawInstructionTooltip(context, instructionTooltipX, instructionTooltipY);
        }
        
        // Статус - теперь отображается в отдельном окне по центру экрана
        if (statusMessage != null) {
            // Проверяем время жизни сообщения
            if (System.currentTimeMillis() - statusMessageTime > STATUS_MESSAGE_DURATION) {
                setStatusMessage(null);
            } else {
                drawStatusMessage(context);
            }
        }
        
        // Тултип боковой панели рисуется в самом конце, чтобы быть поверх всего контента
        if (!viewingDetails) {
            drawSidebarTooltip(context);
        }
    }
    
    /**
     * Рисует статусное сообщение в центре экрана в красивом окне
     */
    private void drawStatusMessage(DrawContext context) {
        // Сохраняем в локальную переменную для избежания race condition
        String message = statusMessage;
        if (message == null || message.isEmpty()) return;
        
        int padding = 12;
        int msgWidth = this.textRenderer.getWidth(message);
        int boxWidth = msgWidth + padding * 2 + 20; // +20 для иконки
        int boxHeight = 26;
        
        int boxX = (this.width - boxWidth) / 2;
        int boxY = guiTop - boxHeight - 10;
        
        if (boxY < 10) {
            boxY = guiTop + GUI_HEIGHT + 10;
        }
        
        // Определяем стиль в зависимости от типа сообщения
        int bgColor = COLOR_BG_PANEL;
        int accentColor = COLOR_GOLD;
        int textColor = COLOR_TEXT_TITLE;
        String icon = "\u2139"; // Info icon
        
        LocalizationManager lang = LocalizationManager.getInstance();
        if (lang.isErrorMessage(message)) {
            accentColor = COLOR_RED;
            bgColor = 0xFF1C1517;
            icon = "\u2717"; // X mark
        } else if (lang.isSuccessMessage(message)) {
            accentColor = COLOR_GREEN;
            bgColor = 0xFF141C15;
            icon = "\u2713"; // Check mark
        }
        
        // Тень
        context.fill(boxX + 2, boxY + 2, boxX + boxWidth + 2, boxY + boxHeight + 2, 0x40000000);
        
        // Фон
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, bgColor);
        
        // Левая акцентная полоска
        context.fill(boxX, boxY, boxX + 3, boxY + boxHeight, accentColor);
        
        // Тонкая граница
        drawBorder(context, boxX, boxY, boxWidth, boxHeight, COLOR_BORDER);
        
        // Иконка
        context.drawTextWithShadow(this.textRenderer, Text.literal(icon),
                boxX + 8, boxY + (boxHeight - 8) / 2, accentColor);
        
        // Текст
        context.drawTextWithShadow(this.textRenderer, Text.literal(message),
                boxX + 22, boxY + (boxHeight - 8) / 2, textColor);
    }
    
    /**
     * Ус��анавливает статусное сообщение с временной меткой
     */
    private void setStatusMessage(String message) {
        this.statusMessage = message;
        if (message != null) {
            this.statusMessageTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Рисует tooltip с инструкцией по пр��в����дению сделки
     */
    private void drawInstructionTooltip(DrawContext context, int x, int y) {
        LocalizationManager lang = LocalizationManager.getInstance();
        
        String[] steps = {
            lang.get("trade_instruction_title"),
            "",
            lang.get("trade_step_1"),
            lang.get("trade_step_2"),
            lang.get("trade_step_3"),
            lang.get("trade_step_4"),
            lang.get("trade_step_5"),
            lang.get("trade_step_6")
        };
        
        // Вычисляем размеры tooltip
        int padding = 8;
        int lineHeight = 10;
        int maxWidth = 0;
        for (String step : steps) {
            int w = this.textRenderer.getWidth(step);
            if (w > maxWidth) maxWidth = w;
        }
        
        int tooltipWidth = maxWidth + padding * 2;
        int tooltipHeight = steps.length * lineHeight + padding * 2 - 2;
        
        // Корректируем позицию чтобы tooltip не выходил за экран
        if (x + tooltipWidth > this.width) {
            x = this.width - tooltipWidth - 5;
        }
        if (y + tooltipHeight > this.height) {
            y = this.height - tooltipHeight - 5;
        }
        
        // Фон с тенью
        context.fill(x + 2, y + 2, x + tooltipWidth + 2, y + tooltipHeight + 2, 0x80000000);
        
        // Основной фон
        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, COLOR_BG_DARK);
        drawBorder(context, x, y, tooltipWidth, tooltipHeight, COLOR_GOLD);
        
        // Текст
        int textY = y + padding;
        for (int i = 0; i < steps.length; i++) {
            String step = steps[i];
            if (step.isEmpty()) {
                textY += 4; // Пустая строка - меньший отступ
                continue;
            }
            
            int color = (i == 0) ? COLOR_GOLD : COLOR_TEXT; // Заголовок золотой
            boolean bold = (i == 0);
            
            context.drawTextWithShadow(this.textRenderer, 
                    Text.literal(step).styled(s -> s.withBold(bold)),
                    x + padding, textY, color);
            textY += lineHeight;
        }
    }

    private void renderDetailsView(DrawContext context, int mouseX, int mouseY) {
        // Делегируем в DetailsViewRenderer, но из-за сложности чата и tooltip
        // оставляем полную логику здесь, а используем renderer для основной части
        
        LocalizationManager lang = LocalizationManager.getInstance();
        int detailsGuiLeft = (this.width - DETAILS_WIDTH) / 2;
        int contentX = detailsGuiLeft + 15;
        int contentY = guiTop + 45;
        int mainPanelWidth = DETAILS_WIDTH - ITEM_PANEL_WIDTH - 45;
        int contentHeight = GUI_HEIGHT - 65;

        // Главная панель
        drawPanel(context, detailsGuiLeft, guiTop, DETAILS_WIDTH, GUI_HEIGHT);
        
        // Заголовок
        int titleY = guiTop + 10;
        String title = lang.get("listing_details");
        int titleWidth = this.textRenderer.getWidth(title);
        int titleX = detailsGuiLeft + (DETAILS_WIDTH - titleWidth) / 2;
        context.fill(titleX - 15, titleY - 2, titleX + titleWidth + 15, titleY + 12, 0x30000000);
        context.drawTextWithShadow(this.textRenderer, Text.literal(title).styled(s -> s.withBold(true)),
                titleX, titleY, COLOR_GOLD);
        
        // Градиентная линия
        int lineY = titleY + 16;
        int lineCenterX = detailsGuiLeft + DETAILS_WIDTH / 2;
        for (int i = 0; i < (DETAILS_WIDTH - 80) / 2; i++) {
            int alpha = Math.min(255, 60 + (i * 195 / ((DETAILS_WIDTH - 80) / 2)));
            int color = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);
            context.fill(lineCenterX - i, lineY, lineCenterX - i + 1, lineY + 1, color);
            context.fill(lineCenterX + i, lineY, lineCenterX + i + 1, lineY + 1, color);
        }

        // Левая панель (чат)
        context.fill(contentX + 2, contentY + 2, contentX + mainPanelWidth + 2, contentY + contentHeight + 2, 0x40000000);
        context.fill(contentX, contentY, contentX + mainPanelWidth, contentY + contentHeight, COLOR_BG_PANEL);
        context.fill(contentX, contentY, contentX + 2, contentY + contentHeight, COLOR_GOLD);
        drawBorder(context, contentX, contentY, mainPanelWidth, contentHeight, COLOR_BORDER);

        // Кнопка назад
        int backBtnX = contentX + 5;
        int backBtnY = contentY + 5;
        boolean backHovered = mouseX >= backBtnX && mouseX < backBtnX + 55 &&
                mouseY >= backBtnY && mouseY < backBtnY + 18;
        String backText = "< " + lang.get("back");
        drawButton(context, backBtnX, backBtnY, 55, 18, backText, backHovered, COLOR_GOLD);

        // Информация о лоте - используем registries для правильного отображения NBT
        ItemStack stack = selectedListing.getItemStack(MarketDataManager.getInstance().getRegistries());
        int itemInfoY = contentY + 25;
        
        // Предмет
        context.drawItem(stack, contentX + 10, itemInfoY);
        context.drawStackOverlay(this.textRenderer, stack, contentX + 10, itemInfoY);
        
        // Название предмета - используем сохраненно�� itemDisplayName
        String displayName = selectedListing.getItemDisplayName();
        if (displayName == null || displayName.isEmpty() || isWynnLineEmpty(displayName)) {
            displayName = getItemDisplayName(stack).getString();
        }
        context.drawTextWithShadow(this.textRenderer, Text.literal(displayName),
                contentX + 32, itemInfoY, COLOR_TEXT_TITLE);
        
        // Про����авец
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(lang.get("seller", selectedListing.getSellerName())),
                contentX + 32, itemInfoY + 12, COLOR_GOLD_DARK);
        
        // Рейтинг продавца (загружаем если еще не загружен)
        UUID sellerId = selectedListing.getSellerId();
        if (lastLoadedSellerReputationId == null || !lastLoadedSellerReputationId.equals(sellerId)) {
            lastLoadedSellerReputationId = sellerId;
            currentSellerReputation = null;
            showRatingUI = false;
            selectedRating = 0;
            hasAlreadyRated = false;
            hasConfirmedTransaction = false;
            
            // Загружаем репутацию
            SupabaseClient.getInstance().fetchSellerReputation(sellerId,
                rep -> currentSellerReputation = rep,
                error -> TradeMarketMod.LOGGER.debug("Failed to load seller reputation: " + error));
            
            // ��роверяем, оценивал ли текущий игрок этого продавца и есть ли подтвержденная сделка
            if (client.player != null) {
                UUID playerId = client.player.getUuid();
                SupabaseClient.getInstance().checkIfAlreadyRated(sellerId, playerId,
                    rated -> hasAlreadyRated = rated,
                    error -> TradeMarketMod.LOGGER.debug("Failed to check if already rated: " + error));
                
                // Проверяем наличие подтвержденной сделки на ЭТОТ КОНКРЕТНЫЙ ЛОТ для возможности оценки
                SupabaseClient.getInstance().hasConfirmedTransaction(playerId, sellerId, selectedListing.getListingId(),
                    hasTransaction -> hasConfirmedTransaction = hasTransaction,
                    error -> TradeMarketMod.LOGGER.debug("Failed to check confirmed transaction: " + error));
            }
        }
        
        // Отображаем рейтинг продавца (всегда показываем)
        String ratingText;
        int ratingColor;
        if (currentSellerReputation != null && currentSellerReputation.totalRatings > 0) {
            String stars = currentSellerReputation.getRatingStars();
            ratingText = stars + " (" + currentSellerReputation.totalRatings + ")";
            ratingColor = COLOR_GOLD;
        } else {
            ratingText = lang.get("no_ratings");
            ratingColor = COLOR_TEXT_MUTED;
        }
        context.drawTextWithShadow(this.textRenderer, Text.literal(ratingText),
                contentX + mainPanelWidth - this.textRenderer.getWidth(ratingText) - 10, itemInfoY + 4, ratingColor);
        
        // Количество успешных сделок
        int successfulTrades = (currentSellerReputation != null) ? currentSellerReputation.successfulTrades : 0;
        String tradesText = lang.get("successful_trades") + ": " + successfulTrades;
        context.drawTextWithShadow(this.textRenderer, Text.literal(tradesText),
                contentX + mainPanelWidth - this.textRenderer.getWidth(tradesText) - 10, itemInfoY + 14, COLOR_TEXT_MUTED);
        
        // Кнопка "Оценить продавца" (если это не свой лот и есть подтвержденная сделка)
        if (client.player != null && !selectedListing.getSellerId().equals(client.player.getUuid())) {
            String rateBtnText;
            boolean canRate = hasConfirmedTransaction && !hasAlreadyRated;
            
            if (hasAlreadyRated) {
                rateBtnText = lang.get("already_rated");
            } else if (!hasConfirmedTransaction) {
                rateBtnText = lang.get("need_confirmed_transaction");
            } else {
                rateBtnText = lang.get("rate_seller");
            }
            
            int rateBtnWidth = this.textRenderer.getWidth(rateBtnText) + 12;
            int rateBtnX = contentX + mainPanelWidth - rateBtnWidth - 10;
            int rateBtnY = itemInfoY + 26;
            int rateBtnH = 14;
            
            boolean rateHover = canRate && mouseX >= rateBtnX && mouseX < rateBtnX + rateBtnWidth && 
                               mouseY >= rateBtnY && mouseY < rateBtnY + rateBtnH;
            
            int btnBgColor = canRate ? (rateHover ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG) : COLOR_BG_PANEL;
            int btnBorderColor = canRate ? COLOR_GOLD_DARK : COLOR_TEXT_MUTED;
            int btnTextColor = canRate ? COLOR_GOLD : COLOR_TEXT_MUTED;
            
            context.fill(rateBtnX, rateBtnY, rateBtnX + rateBtnWidth, rateBtnY + rateBtnH, btnBgColor);
            drawBorder(context, rateBtnX, rateBtnY, rateBtnWidth, rateBtnH, btnBorderColor);
            context.drawTextWithShadow(this.textRenderer, Text.literal(rateBtnText),
                    rateBtnX + 6, rateBtnY + 3, btnTextColor);
            
            // Показываем UI выбора оценки только если можно оценить
            if (showRatingUI && canRate) {
                int ratingUIX = rateBtnX - 50;
                int ratingUIY = rateBtnY + rateBtnH + 2;
                int ratingUIW = rateBtnWidth + 50;
                int ratingUIH = 30;
                
                // Фон ��анели оценки
                context.fill(ratingUIX, ratingUIY, ratingUIX + ratingUIW, ratingUIY + ratingUIH, COLOR_BG_PANEL);
                drawBorder(context, ratingUIX, ratingUIY, ratingUIW, ratingUIH, COLOR_GOLD_DARK);
                
                // 5 звезд для выбора
                for (int i = 1; i <= 5; i++) {
                    int starX = ratingUIX + 10 + (i - 1) * 18;
                    int starY = ratingUIY + 6;
                    boolean starHover = mouseX >= starX && mouseX < starX + 16 && 
                                       mouseY >= starY && mouseY < starY + 16;
                    String starChar = (i <= selectedRating || starHover) ? "\u2605" : "\u2606";
                    int starColor = (i <= selectedRating || starHover) ? COLOR_GOLD : COLOR_TEXT_MUTED;
                    context.drawTextWithShadow(this.textRenderer, Text.literal(starChar),
                            starX, starY, starColor);
                }
                
                // Кнопка подтверждения
                if (selectedRating > 0) {
                    String confirmText = lang.get("send");
                    int confirmX = ratingUIX + ratingUIW - this.textRenderer.getWidth(confirmText) - 10;
                    int confirmY = ratingUIY + 8;
                    boolean confirmHover = mouseX >= confirmX - 4 && mouseX < confirmX + this.textRenderer.getWidth(confirmText) + 4 &&
                                          mouseY >= confirmY - 2 && mouseY < confirmY + 12;
                    context.drawTextWithShadow(this.textRenderer, Text.literal(confirmText),
                            confirmX, confirmY, confirmHover ? COLOR_GREEN : COLOR_GOLD);
                }
            }
            
        }
        
        // Цена и условия (�� отступом для чата)
        int currentLineY = itemInfoY + 28;
        String price = selectedListing.getPrice();
        if (price != null && !price.isEmpty()) {
            // Текст "Цена/Price: " + число + " " + иконка
            String priceLabel = lang.get("price", price);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(priceLabel),
                    contentX + 10, currentLineY, COLOR_GREEN);
            // Иконка изумруда после текста (16x16)
            int priceTextWidth = this.textRenderer.getWidth(priceLabel);
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_EMERALD,
                    contentX + 10 + priceTextWidth, currentLineY - 5, 0, 0, 16, 16, 16, 16);
            currentLineY += 16;
        }
        
        // Условия - показываем в несколько строк если текст длинный
        String description = selectedListing.getDescription();
        if (description != null && !description.isEmpty()) {
            int maxDescWidth = mainPanelWidth - 30;
            String descLabel = lang.get("conditions");
            int descLabelWidth = this.textRenderer.getWidth(descLabel);
            
            // Рендерим метку "Условия/Conditions:"
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(descLabel),
                    contentX + 10, currentLineY, COLOR_GOLD);
            
            // Разбиваем текст условий на с��рок��
            List<String> descLines = wrapText(description, maxDescWidth - descLabelWidth);
            if (descLines.isEmpty()) {
                descLines.add(description);
            }
            
            // Пе��в��ю строку рендерим рядом с меткой
            if (!descLines.isEmpty()) {
                String firstLine = descLines.get(0);
                if (this.textRenderer.getWidth(firstLine) > maxDescWidth - descLabelWidth) {
                    firstLine = trimTextToWidth(firstLine, maxDescWidth - descLabelWidth - 10) + "...";
                }
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal(firstLine),
                        contentX + 10 + descLabelWidth, currentLineY, COLOR_TEXT_NORMAL);
                currentLineY += 12;
            }
            
            // Остальные строки с отс��упом
            for (int i = 1; i < Math.min(descLines.size(), 3); i++) { // Макс 3 строки
                String line = descLines.get(i);
                if (this.textRenderer.getWidth(line) > maxDescWidth) {
                    line = trimTextToWidth(line, maxDescWidth - 10) + "...";
                }
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal(line),
                        contentX + 10, currentLineY, COLOR_TEXT_NORMAL);
                currentLineY += 12;
            }
            
            // Если есть еще текст, показываем "..."
            if (descLines.size() > 3) {
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal("..."),
                        contentX + 10, currentLineY, COLOR_TEXT_MUTED);
                currentLineY += 12;
            }
        }
        
        // Кнопка "Начать сделку" (если это не свой лот и нет существующей сделки) - после цены и условий
        if (client.player != null && !selectedListing.getSellerId().equals(client.player.getUuid())) {
            int startDealX = contentX + 10;
            int startDealY = currentLineY + 4; // С небольшим ����������т��тупом под условиями
            int startDealH = 14;
            
            if (hasExistingDealOnListing) {
                // Показываем что сделка уже начата
                String dealExistsText = lang.get("deal_already_exists");
                int dealExistsWidth = this.textRenderer.getWidth(dealExistsText) + 12;
                
                context.fill(startDealX, startDealY, startDealX + dealExistsWidth, startDealY + startDealH, 
                        COLOR_BUTTON_BG);
                drawBorder(context, startDealX, startDealY, dealExistsWidth, startDealH, COLOR_TEXT_MUTED);
                context.drawTextWithShadow(this.textRenderer, Text.literal(dealExistsText),
                        startDealX + 6, startDealY + 3, COLOR_TEXT_MUTED);
                
                currentLineY = startDealY + startDealH;
            } else {
                // Показываем активную кнопку "Начать сделку" с современным оформлением
                String startDealText = lang.get("start_deal");
                int startDealWidth = this.textRenderer.getWidth(startDealText) + 16;
                
                boolean startDealHover = mouseX >= startDealX && mouseX < startDealX + startDealWidth && 
                                        mouseY >= startDealY && mouseY < startDealY + startDealH;
                
                // Эффект свечения при наведении
                if (startDealHover) {
                    context.fill(startDealX - 2, startDealY - 2, startDealX + startDealWidth + 2, startDealY + startDealH + 2,
                            (COLOR_GREEN & 0x00FFFFFF) | 0x40000000);
                }
                
                context.fill(startDealX, startDealY, startDealX + startDealWidth, startDealY + startDealH, 
                        startDealHover ? COLOR_GREEN : COLOR_BUTTON_BG);
                // Акцентная линия слева
                context.fill(startDealX, startDealY, startDealX + 2, startDealY + startDealH, COLOR_GREEN);
                drawBorder(context, startDealX, startDealY, startDealWidth, startDealH, COLOR_GREEN);
                context.drawTextWithShadow(this.textRenderer, Text.literal(startDealText),
                        startDealX + 8, startDealY + 3, startDealHover ? 0xFFFFFFFF : COLOR_GREEN);
                
                // Обновляем currentLineY для правильного позициониров��ния чата
                currentLineY = startDealY + startDealH;
            }
            
            // ===== КНОПКА ИНСТРУКЦИИ (?) =====
            currentLineY += 6;
            
            // Кнопка "?" для инструкции - размещаем правее от кнопки "Начать сделку"
            int helpBtnSize = 14;
            int helpTextWidth = this.textRenderer.getWidth(lang.get("trade_instruction_hint"));
            int helpBtnX = contentX + mainPanelWidth - helpTextWidth - helpBtnSize - 25; // Справа от текста
            int helpBtnY = currentLineY;
            
            boolean helpHover = mouseX >= helpBtnX && mouseX < helpBtnX + helpBtnSize && 
                               mouseY >= helpBtnY && mouseY < helpBtnY + helpBtnSize;
            
            // Рисуем кнопку
            context.fill(helpBtnX, helpBtnY, helpBtnX + helpBtnSize, helpBtnY + helpBtnSize, 
                    helpHover ? COLOR_GOLD : COLOR_BUTTON_BG);
            drawBorder(context, helpBtnX, helpBtnY, helpBtnSize, helpBtnSize, COLOR_GOLD);
            context.drawTextWithShadow(this.textRenderer, Text.literal("?"),
                    helpBtnX + 4, helpBtnY + 3, helpHover ? 0xFF000000 : COLOR_GOLD);
            
            // Текст рядом с кнопкой
            context.drawTextWithShadow(this.textRenderer, 
                    Text.literal(lang.get("trade_instruction_hint")),
                    helpBtnX + helpBtnSize + 5, helpBtnY + 3, COLOR_TEXT_MUTED);
            
            // Сохраняем позицию для tooltip (рисуем в конце)
            instructionTooltipX = helpBtnX;
            instructionTooltipY = helpBtnY + helpBtnSize + 2;
            showInstructionTooltip = helpHover;
            
            currentLineY += helpBtnSize + 4;
        }

        // ===== ПАНЕЛЬ ХАРАКТЕРИСТИК ПРЕДМЕТА (справа) =====
        int itemPanelX = contentX + mainPanelWidth + 10;
        int itemPanelY = contentY;
        int itemPanelHeight = contentHeight;
        
        // Тень для панели
        context.fill(itemPanelX + 2, itemPanelY + 2, itemPanelX + ITEM_PANEL_WIDTH + 2, itemPanelY + itemPanelHeight + 2, 0x40000000);
        // Фон панели
        context.fill(itemPanelX, itemPanelY, itemPanelX + ITEM_PANEL_WIDTH, itemPanelY + itemPanelHeight, COLOR_BG_PANEL);
        // Акцентная линия сверху
        context.fill(itemPanelX, itemPanelY, itemPanelX + ITEM_PANEL_WIDTH, itemPanelY + 2, COLOR_GOLD);
        // Граница
        drawBorder(context, itemPanelX, itemPanelY, ITEM_PANEL_WIDTH, itemPanelHeight, COLOR_BORDER);
        
        // Заголовок панели с подложкой
        String charTitle = lang.get("characteristics");
        int charTitleWidth = this.textRenderer.getWidth(charTitle);
        int charTitleX = itemPanelX + (ITEM_PANEL_WIDTH - charTitleWidth) / 2;
        context.fill(charTitleX - 8, itemPanelY + 6, charTitleX + charTitleWidth + 8, itemPanelY + 18, 0x30000000);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(charTitle).styled(s -> s.withBold(true)),
                charTitleX, itemPanelY + 8, COLOR_GOLD);
        
        // Градиентная линия под за����оловком
        int charLineY = itemPanelY + 22;
        int charLineStartX = itemPanelX + 10;
        int charLineEndX = itemPanelX + ITEM_PANEL_WIDTH - 10;
        int charLineCenterX = itemPanelX + ITEM_PANEL_WIDTH / 2;
        for (int i = 0; i < (charLineEndX - charLineStartX) / 2; i++) {
            int alpha = Math.min(255, 40 + (i * 215 / ((charLineEndX - charLineStartX) / 2)));
            int color = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);
            context.fill(charLineCenterX - i, charLineY, charLineCenterX - i + 1, charLineY + 1, color);
            context.fill(charLineCenterX + i, charLineY, charLineCenterX + i + 1, charLineY + 1, color);
        }
        
        // Большая иконка предмета (центрирована) с улучшенным оформлением
        int bigItemX = itemPanelX + (ITEM_PANEL_WIDTH - 16) / 2;
        int bigItemY = itemPanelY + 36; // Увеличен отступ от заголовка
        
        // Эффект свечения вокруг слота
        context.fill(bigItemX - 7, bigItemY - 7, bigItemX + 23, bigItemY + 23, (COLOR_GOLD & 0x00FFFFFF) | 0x25000000);
        // Фон слота
        context.fill(bigItemX - 4, bigItemY - 4, bigItemX + 20, bigItemY + 20, COLOR_SLOT_BG);
        // Акцентная граница
        drawBorder(context, bigItemX - 4, bigItemY - 4, 24, 24, COLOR_GOLD_DARK);
        // Внутренняя граница
        context.fill(bigItemX - 3, bigItemY - 3, bigItemX + 19, bigItemY - 2, 0x20FFFFFF);
        
        context.drawItem(stack, bigItemX, bigItemY);
        context.drawStackOverlay(this.textRenderer, stack, bigItemX, bigItemY);
        
        // Название предмета (с сохранен��ем цветов) - используем getItemDisplayName для WynnCraft
        Text itemNameForPanel = getItemDisplayName(stack);
        int nameWidth = this.textRenderer.getWidth(itemNameForPanel);
        int nameX = itemPanelX + (ITEM_PANEL_WIDTH - nameWidth) / 2;
        if (nameWidth > ITEM_PANEL_WIDTH - 20) {
            nameX = itemPanelX + 10;
        }
        int nameY = bigItemY + 24; // Под иконкой 16px + отступ 8px
        context.drawTextWithShadow(this.textRenderer, itemNameForPanel, nameX, nameY, 0xFFFFFFFF);
        
        // Количество - под названием с увеличенным отступом
        int infoY = nameY + 14;
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(lang.get("quantity", stack.getCount())),
                itemPanelX + 10, infoY, COLOR_TEXT_NORMAL);
        infoY += 14;
        
        // Разделительная ��иния перед характеристиками
        context.fill(itemPanelX + 5, infoY, itemPanelX + ITEM_PANEL_WIDTH - 5, infoY + 1, COLOR_BORDER);
        infoY += 8; // Отступ после линии
        
        // Tooltip/Lore п��едмета - рендерим оригинальные Text объекты с сохранением форматирования
        List<Text> tooltip = stack.getTooltip(
                net.minecraft.item.Item.TooltipContext.create(client.world),
                client.player, 
                net.minecraft.item.tooltip.TooltipType.ADVANCED);
        
        if (tooltip.size() > 1) {
            
            // Показываем ВС���� строки tooltip с сохранением форматирован��я
            int lineCount = 0;
            for (int i = 1; i < tooltip.size(); i++) {
                Text line = tooltip.get(i);
                
                // Пропускаем пустые строки (только alignment символы)
                if (isWynnLineEmpty(line.getString())) continue;
                
                lineCount++;
                
                // Первая строка содержит иконку предмета к��торая выступает вверх - добавляе�� отступ
                if (lineCount == 1) {
                    infoY += 6; // Дополнит��льный отступ перед строкой с иконкой
                }
                
                // Рен��ерим оригинальный Text с форма��и��ов��нием
                // Проверяем ширину и обрезаем если нужно
                int lineWidth = this.textRenderer.getWidth(line);
                if (lineWidth > ITEM_PANEL_WIDTH - 20) {
                    // Обрезаем текст, сохраняя стиль
                    String trimmedStr = trimTextToWidth(line.getString(), ITEM_PANEL_WIDTH - 25);
                    Text trimmedLine = Text.literal(trimmedStr + "...").setStyle(line.getStyle());
                    context.drawTextWithShadow(this.textRenderer, trimmedLine, 
                            itemPanelX + 10, infoY, 0xFFFFFFFF);
                } else {
                    // Рендерим как есть с оригинальным форматированием
                    context.drawTextWithShadow(this.textRenderer, line, 
                            itemPanelX + 10, infoY, 0xFFFFFFFF);
                }
                
                // Первая строка содержит иконку предмета (16px высота) - нужно больше места
                if (lineCount == 1) {
                    infoY += 18; // Увеличенный интервал после иконки предмета
                } else {
                    infoY += 12; // Стандартный межстрочный интервал
                }
            }
        } else {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(lang.get("no_effects")),
                    itemPanelX + 10, infoY, COLOR_TEXT_MUTED);
        }

        // ===== ЧАТ =====
        // Фиксированная позиция чата - одинаковая дл�� продавца и ����купателя
        // Резервир��������������ем место для кнопок даже если их нет (чтоб�� чат был одинакового размера)
        int fixedChatOffset = 95; // Фиксированный отступ от начала контента (место для кнопок)
        int chatY = itemInfoY + fixedChatOffset;
        // Учитываем высоту ��оля ввода (20px) + отступы (5px + 10px снизу) = 35px
        int chatHeight = contentHeight - fixedChatOffset - 45;
        chatHeight = Math.max(60, chatHeight); // Минимальная высота
        int chatWidth = mainPanelWidth - 20;
        
        // Заголовок чата с улучшенным оформлением
        String chatTitle = lang.get("chat_with_seller");
        int chatTitleWidth = this.textRenderer.getWidth(chatTitle);
        // Подложка под заголовок
        context.fill(contentX + 8, chatY - 16, contentX + 14 + chatTitleWidth, chatY - 4, 0x30000000);
        // Акцентная точка перед заголовком
        context.fill(contentX + 10, chatY - 12, contentX + 13, chatY - 9, COLOR_BLUE);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(chatTitle),
                contentX + 16, chatY - 14, COLOR_BLUE);
        
        // Область чата с улучшенным оформлением
        // Внутре��ня�� тень
        context.fill(contentX + 10, chatY, contentX + 10 + chatWidth, chatY + chatHeight, COLOR_CHAT_BG);
        // Акцент сверху
        context.fill(contentX + 10, chatY, contentX + 10 + chatWidth, chatY + 1, COLOR_BLUE);
        // Внутренняя подсветка
        context.fill(contentX + 11, chatY + 1, contentX + 9 + chatWidth, chatY + 2, 0x15FFFFFF);
        drawBorder(context, contentX + 10, chatY, chatWidth, chatHeight, COLOR_INPUT_BORDER);
        
        // Сообщения - ограничивае�������� количество видимых сообщений по высоте чата
        int msgY = chatY + 5;
        int msgHeight = CHAT_MSG_HEIGHT;
        chatMaxVisible = (chatHeight - 10) / msgHeight; // Вычисляем сколько помещается
        chatMaxVisible = Math.max(1, chatMaxVisible); // Минимум 1
        
        // Корректируем offset если ��н стал некорректным после изменения размера или новых сообщений
        int maxScroll = Math.max(0, chatMessages.size() - chatMaxVisible);
        if (chatScrollOffset > maxScroll) {
            chatScrollOffset = maxScroll;
        }
        
        int startIdx = chatScrollOffset;
        int endIdx = Math.min(startIdx + chatMaxVisible, chatMessages.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            // П��оверяем что сообщение помещается в область чата
            if (msgY + msgHeight > chatY + chatHeight - 5) {
                break;
            }
            
            SupabaseClient.ChatMessage msg = chatMessages.get(i);
            boolean isMyMessage = client.player != null && msg.senderId.equals(client.player.getUuid());
            boolean isSeller = selectedListing != null && msg.senderName.equals(selectedListing.getSellerName());
            
            int msgBgColor = isMyMessage ? COLOR_CHAT_MY_MSG : COLOR_CHAT_OTHER_MSG;
            int msgX = contentX + 15;
            int msgWidth = chatWidth - 15;
            
            // Фон сообщения с улучшенным оформлением
            context.fill(msgX, msgY, msgX + msgWidth, msgY + 18, msgBgColor);
            // Акцентная полоска слева (цвет зависит от отправ��теля)
            int accentColor = isMyMessage ? COLOR_GREEN : COLOR_BLUE;
            context.fill(msgX, msgY, msgX + 2, msgY + 18, accentColor);
            // Легкая подсветка сверху
            context.fill(msgX + 2, msgY, msgX + msgWidth, msgY + 1, 0x10FFFFFF);
            
            // Имя отправителя: "Вы/You" для своих сообщений, "Продавец/Seller" для продавца, иначе имя
            String senderPrefix;
            if (isMyMessage) {
                senderPrefix = lang.get("you");
            } else if (isSeller) {
                senderPrefix = lang.get("seller_prefix");
            } else {
                senderPrefix = msg.senderName + ": ";
            }
            int senderColor = isMyMessage ? COLOR_GREEN : COLOR_BLUE;
            context.drawTextWithShadow(this.textRenderer, Text.literal(senderPrefix),
                    msgX + 5, msgY + 5, senderColor);
            
            // Текст сообщения
            int textOffset = this.textRenderer.getWidth(senderPrefix);
            String msgText = msg.message;
            if (this.textRenderer.getWidth(msgText) > msgWidth - textOffset - 15) {
                while (this.textRenderer.getWidth(msgText + "...") > msgWidth - textOffset - 15 && msgText.length() > 0) {
                    msgText = msgText.substring(0, msgText.length() - 1);
                }
                msgText += "...";
            }
            context.drawTextWithShadow(this.textRenderer, Text.literal(msgText),
                    msgX + 5 + textOffset, msgY + 5, 0xFFFFFFFF);
            
            msgY += msgHeight;
        }
        
        // Пустой чат
        if (chatMessages.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(lang.get("no_messages")),
                    contentX + 20, chatY + chatHeight / 2 - 4, COLOR_TEXT_MUTED);
        }
        
        // Скролл чата
        if (chatMessages.size() > chatMaxVisible) {
            int scrollBarHeight = chatHeight - 10;
            int thumbHeight = Math.max(20, scrollBarHeight * chatMaxVisible / chatMessages.size());
            int scrollMaxScroll = Math.max(1, chatMessages.size() - chatMaxVisible);
            int thumbY = chatY + 5 + (scrollBarHeight - thumbHeight) * chatScrollOffset / scrollMaxScroll;
            
            context.fill(contentX + chatWidth + 2, chatY + 5, contentX + chatWidth + 5, chatY + chatHeight - 5, COLOR_BORDER);
            context.fill(contentX + chatWidth + 2, thumbY, contentX + chatWidth + 5, thumbY + thumbHeight, COLOR_GOLD);
        }

        // Поле ввода сообщения (динамическая высота)
        int inputY = chatY + chatHeight + 5;
        int inputWidth = chatWidth - 75;
        int inputHeight = getInputFieldHeight(messageText, inputWidth, 20);
        drawInputField(context, contentX + 10, inputY, inputWidth, 20, messageText,
                lang.get("enter_message"), messageFocused, mouseX, mouseY);
        
        // Кнопка отправить (выравнена по нижнему краю поля ввода)
        int sendBtnX = contentX + chatWidth - 60;
        int sendBtnY = inputY + inputHeight - 20; // Кнопка внизу поля
        boolean sendHovered = mouseX >= sendBtnX && mouseX < sendBtnX + 65 &&
                mouseY >= sendBtnY && mouseY < sendBtnY + 20;
        drawButton(context, sendBtnX, sendBtnY, 65, 20, lang.get("send"), sendHovered, COLOR_GREEN);
        
        // ===== АДМИН-ПАНЕЛЬ =====
        if (currentAdminInfo != null && currentAdminInfo.isAdmin) {
            drawAdminPanel(context, detailsGuiLeft, guiTop, DETAILS_WIDTH, GUI_HEIGHT, mouseX, mouseY);
        }
    }
    
    /**
     * Обрабатывает клики в админ-панели
     */
    private boolean handleAdminPanelClick(int mx, int my) {
        int detailsGuiLeft = (this.width - DETAILS_WIDTH) / 2;
        int width = DETAILS_WIDTH;
        int height = GUI_HEIGHT;
        
        // Кнопка админ-панели
        int adminBtnX = detailsGuiLeft + width - 100;
        int adminBtnY = guiTop + height - 30;
        int adminBtnWidth = 85;
        int adminBtnHeight = 20;
        
        if (mx >= adminBtnX && mx < adminBtnX + adminBtnWidth &&
                my >= adminBtnY && my < adminBtnY + adminBtnHeight) {
            showAdminPanel = !showAdminPanel;
            if (!showAdminPanel) {
                // Сбрасываем позицию при закрытии
                adminPanelOffsetX = 0;
                adminPanelOffsetY = 0;
            }
            return true;
        }
        
        // Если панель открыта - проверяем клики внутри
        if (showAdminPanel && selectedListing != null) {
            int basePanelX = detailsGuiLeft + width - 180;
            int basePanelY = guiTop + 50; // Сдвинуто ниже (было 40)
            int panelWidth = 170;
            int panelHeight = 200;
            
            // Применяем с��ещение
            int panelX = basePanelX + adminPanelOffsetX;
            int panelY = basePanelY + adminPanelOffsetY;
            
            // ��гр��ни������и��аем позицию
            panelX = Math.max(10, Math.min(this.width - panelWidth - 10, panelX));
            panelY = Math.max(10, Math.min(this.height - panelHeight - 10, panelY));
            
            int headerHeight = 22;
            
            // Кли���� по заголовку ��ля перетаскивания
            if (mx >= panelX && mx < panelX + panelWidth &&
                my >= panelY && my < panelY + headerHeight) {
                isDraggingAdminPanel = true;
                adminDragStartX = mx - adminPanelOffsetX;
                adminDragStartY = my - adminPanelOffsetY;
                return true;
            }
            
            int btnX = panelX + 10;
            int btnWidth = panelWidth - 20;
            int btnY = panelY + 40;
            int btnH = 18;
            
            String sellerName = selectedListing.getSellerName();
            String adminName = client.player != null ? client.player.getName().getString() : "Admin";
            
            // Кнопка удалить лот
            if (currentAdminInfo.canDeleteListings()) {
                if (mx >= btnX && mx < btnX + btnWidth && my >= btnY && my < btnY + btnH) {
                    SupabaseClient.getInstance().deleteListingAdmin(
                        selectedListing.getListingId().toString(),
                        adminName,
                        LocalizationManager.getInstance().get("deleted_by_admin"),
                        () -> {
                            setStatusMessage(LocalizationManager.getInstance().get("listing_deleted"));
                            showAdminPanel = false;
                            viewingDetails = false;
                            selectedListing = null;
                            loadFromSupabase();
                        },
                        error -> setStatusMessage(LocalizationManager.getInstance().get("error_generic", error))
                    );
                    return true;
                }
                btnY += btnH + 5;
            }
            
            // Пропускаем раз��елитель и заголовок
            btnY += 8 + 14;
            
            // Кнопки блокировок
            String[] banTypes = {"listing", "buying", "chat", "support", "full"};
            
            for (int i = 0; i < banTypes.length; i++) {
                if (mx >= btnX && mx < btnX + btnWidth && my >= btnY && my < btnY + btnH) {
                    final String banType = banTypes[i];
                    SupabaseClient.getInstance().banUser(
                        sellerName,
                        banType,
                        LocalizationManager.getInstance().get("ban_issued_by_admin", adminName),
                        null,
                        null,
                        () -> {
                            setStatusMessage(LocalizationManager.getInstance().get("ban_issued", banType));
                            // Перезагружаем баны текущего пользователя (важно если админ банит сам себя)
                            loadUserBans();
                        },
                        error -> setStatusMessage(LocalizationManager.getInstance().get("error_generic", error))
                    );
                    return true;
                }
                btnY += btnH + 3;
            }
            
            // К��опка разблокировки
            btnY += 5;
            if (mx >= btnX && mx < btnX + btnWidth && my >= btnY && my < btnY + btnH) {
                // Снимаем все баны последовательно
                final int[] completed = {0};
                final int total = banTypes.length;
                
                for (String banType : banTypes) {
                    SupabaseClient.getInstance().unbanUser(
                        sellerName,
                        banType,
                        adminName,
                        () -> {
                            completed[0]++;
                            if (completed[0] >= total) {
                                // Все баны сняты - перезагружаем баны текущего пользователя
                                // (важно если ��дмин снимает баны с самого себя)
                                loadUserBans();
                                setStatusMessage(LocalizationManager.getInstance().get("all_bans_removed"));
                            }
                        },
                        error -> {
                            completed[0]++;
                            TradeMarketMod.LOGGER.warn("[TradeMarket] Failed to remove ban " + banType + " for " + sellerName + ": " + error);
                        }
                    );
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Рисует админ-панель в деталях лота - делегирует в AdminPanelRenderer
     */
    private void drawAdminPanel(DrawContext context, int detailsGuiLeft, int guiTop, int width, int height, int mouseX, int mouseY) {
        AdminPanelRenderer.AdminPanelState state = new AdminPanelRenderer.AdminPanelState(
            showAdminPanel, adminPanelOffsetX, adminPanelOffsetY,
            isDraggingAdminPanel, this.width, this.height
        );
        
        AdminPanelRenderer.AdminInfo adminInfo = new AdminPanelRenderer.AdminInfo(
            currentAdminInfo.isAdmin, currentAdminInfo.role, currentAdminInfo.canDeleteListings()
        );
        
        AdminPanelRenderer.drawAdminPanel(context, this.textRenderer,
            detailsGuiLeft, guiTop, width, height, mouseX, mouseY,
            state, adminInfo, selectedListing);
    }

    // drawPanel, drawBorder, drawCorner, drawModernCorner - делегируем в RenderUtils
    
    private void drawPanel(DrawContext context, int x, int y, int width, int height) {
        RenderUtils.drawPanel(context, x, y, width, height);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        RenderUtils.drawBorder(context, x, y, width, height, color);
    }

    private void drawModernCorner(DrawContext context, int x, int y, boolean flipX, boolean flipY) {
        RenderUtils.drawModernCorner(context, x, y, flipX, flipY);
    }
    
    /**
     * Очищает текст от WynnCraft alignment символов (surrogate pairs для выравнивания)
     * Сохраняет все остальное форматирование и специальные симво��ы
     * Основано на WynnTils StyledText.stripAlignment()
     */
    /**
     * Обрабатывает клик по закладке соцсетей
     */
    private boolean handleSocialBookmarkClick(int mx, int my) {
        int currentGuiRight = viewingDetails ? (this.width + DETAILS_WIDTH) / 2 : guiLeft + GUI_WIDTH;
        
        ClickHandlers.ClickResult result = ClickHandlers.handleSocialBookmarkClick(
            mx, my, currentGuiRight, guiTop, bookmarkExpanded,
            infoPanelOffsetX, infoPanelOffsetY, this.width, this.height);
        
        if (!result.consumed) return false;
        
        switch (result.action) {
            case TOGGLE_BOOKMARK:
                bookmarkExpanded = !bookmarkExpanded;
                if (!bookmarkExpanded) {
                    infoPanelOffsetX = 0;
                    infoPanelOffsetY = 0;
                }
                return true;
            case START_DRAG_INFO_PANEL:
                ClickHandlers.DragStartData dragData = (ClickHandlers.DragStartData) result.data;
                isDraggingInfoPanel = true;
                infoDragStartX = dragData.startX;
                infoDragStartY = dragData.startY;
                return true;
            case COPY_DISCORD:
                client.keyboard.setClipboard(DISCORD_USERNAME);
                setStatusMessage("Discord скопирован!");
                return true;
            case COPY_TELEGRAM:
                client.keyboard.setClipboard(TELEGRAM_USERNAME);
                setStatusMessage("Telegram скопирован!");
                return true;
            default:
                return true;
        }
    }
    
    /**
     * Обрабатывает клик по закладке Support
     */
    private boolean handleSupportBookmarkClick(int mx, int my) {
        int currentGuiRight = viewingDetails ? (this.width + DETAILS_WIDTH) / 2 : guiLeft + GUI_WIDTH;
        int tabX = currentGuiRight + 6;
        int infoTabY = guiTop + 8;
        int infoTabSize = 28;
        int tabY = infoTabY + infoTabSize + 6;
        int tabSize = 28;
        
        // Клик по кнопке для открытия/закрытия
        if (mx >= tabX && mx < tabX + tabSize && my >= tabY && my < tabY + tabSize) {
            supportExpanded = !supportExpanded;
            // Закрываем Info если открываем Support
            if (supportExpanded) {
                bookmarkExpanded = false;
                // Загружаем чаты от админов при открытии
                loadUserAdminChats();
            } else {
                // Сбрасываем позицию при закрытии
                supportPanelOffsetX = 0;
                supportPanelOffsetY = 0;
            }
            return true;
        }
        
        // Если закладка раскрыта - проверяем клики внутри панели
        if (supportExpanded) {
            int basePanelX = tabX + tabSize;
            int basePanelY = tabY - 30;
            int panelWidth = 260;
            int panelHeight = 280;
            
            // Применяем смещение от перетаскивания
            int panelX = basePanelX + supportPanelOffsetX;
            int panelY = basePanelY + supportPanelOffsetY;
            
            // Ограни��иваем позицию панели в пределах экрана
            panelX = Math.max(10, Math.min(this.width - panelWidth - 10, panelX));
            panelY = Math.max(10, Math.min(this.height - panelHeight - 10, panelY));
            
            int contentY = panelY;
            int btnX = panelX + 8;
            int btnWidth = panelWidth - 16;
            int headerHeight = 22;
            
            // Проверяем клик по заголовку для перетаскивания
            if (mx >= panelX && mx < panelX + panelWidth &&
                my >= panelY && my < panelY + headerHeight) {
                isDraggingSupportPanel = true;
                dragStartX = mx - supportPanelOffsetX;
                dragStartY = my - supportPanelOffsetY;
                return true;
            }
            
            // Проверяем, что клик внутри п��нели (ниже заголовка)
            if (mx >= panelX && mx < panelX + panelWidth && 
                my >= panelY + headerHeight && my < panelY + panelHeight) {
                
  if (creatingNewTicket) {
  return handleNewTicketFormClick(mx, my, btnX, btnWidth, contentY + 24);
  } else if (activeTicket != null) {
  return handleTicketChatClick(mx, my, btnX, btnWidth, contentY + 24);
  } else if (showingAdminChatInSupport && activeUserAdminChat != null) {
  return handleAdminChatInSupportClick(mx, my, btnX, btnWidth, contentY + 24);
  } else {
  return handleTicketListClick(mx, my, btnX, btnWidth, contentY + 24);
  }
            }
        }
        
        return false;
    }
    
    /**
     * Обработка кликов в списке тикетов
     */
    private boolean handleTicketListClick(int mx, int my, int btnX, int btnWidth, int listY) {
        ClickHandlers.ClickResult result = ClickHandlers.handleTicketListClick(
            mx, my, btnX, btnWidth, listY, supportTickets, supportTicketScroll, userAdminChats);
        
        if (!result.consumed) return false;
        
        switch (result.action) {
            case CREATE_NEW_TICKET:
                creatingNewTicket = true;
                newTicketSubject = "";
                return true;
            case OPEN_TICKET:
                ClickHandlers.OpenTicketData ticketData = (ClickHandlers.OpenTicketData) result.data;
                activeTicket = ticketData.ticket;
                loadTicketMessages(activeTicket.id);
                return true;
            case OPEN_ADMIN_CHAT:
                ClickHandlers.OpenAdminChatData chatData = (ClickHandlers.OpenAdminChatData) result.data;
                activeUserAdminChat = chatData.chat;
                showingAdminChatInSupport = true;
                userChatMessages.clear();
                userChatInput = "";
                loadUserChatMessages();
                SoundManager.getInstance().playClickSound();
                return true;
            default:
                return true;
        }
    }
    
    /**
     * Обработка кликов в форме нового тикета
     */
    private boolean handleNewTicketFormClick(int mx, int my, int btnX, int btnWidth, int formY) {
        ClickHandlers.ClickResult result = ClickHandlers.handleNewTicketFormClick(
            mx, my, btnX, btnWidth, formY, newTicketSubject);
        
        if (!result.consumed) return false;
        
        switch (result.action) {
            case BACK_FROM_NEW_TICKET:
                creatingNewTicket = false;
                newTicketSubject = "";
                newTicketSubjectFocused = false;
                return true;
            case FOCUS_TICKET_SUBJECT:
                newTicketSubjectFocused = true;
                supportMessageFocused = false;
                priceFocused = false;
                descriptionFocused = false;
                messageFocused = false;
                return true;
            case SUBMIT_NEW_TICKET:
                createNewTicket();
                return true;
            case UNFOCUS_ALL:
                newTicketSubjectFocused = false;
                return true;
            default:
                return true;
        }
    }
    
    /**
     * Обработка кликов в чате пользователя с админом (в панели Support)
     */
    private boolean handleAdminChatInSupportClick(int mx, int my, int btnX, int btnWidth, int formY) {
        int panelHeight = 280;
        
        ClickHandlers.ClickResult result = ClickHandlers.handleAdminChatInSupportClick(
            mx, my, btnX, btnWidth, formY, userChatInput, panelHeight);
        
        if (!result.consumed) return false;
        
        switch (result.action) {
            case BACK_FROM_ADMIN_CHAT:
                showingAdminChatInSupport = false;
                activeUserAdminChat = null;
                userChatMessages.clear();
                userChatInput = "";
                userChatInputFocused = false;
                loadUserAdminChats();
                return true;
            case FOCUS_USER_CHAT_INPUT:
                userChatInputFocused = true;
                supportMessageFocused = false;
                newTicketSubjectFocused = false;
                return true;
            case SEND_USER_CHAT_MESSAGE:
                sendUserChatMessage();
                SoundManager.getInstance().playMessageSound();
                return true;
            default:
                userChatInputFocused = false;
                return true;
        }
    }
    
    /**
     * Обработка кликов в чате тикета
     */
    private boolean handleTicketChatClick(int mx, int my, int btnX, int btnWidth, int chatY) {
        boolean isAdmin = currentAdminInfo != null && currentAdminInfo.isAdmin;
        
        ClickHandlers.ClickResult result = ClickHandlers.handleTicketChatClick(
            mx, my, btnX, btnWidth, chatY, activeTicket, supportMessageText, isAdmin);
        
        if (!result.consumed) return false;
        
        switch (result.action) {
            case BACK_FROM_TICKET:
                activeTicket = null;
                ticketMessages.clear();
                supportMessageText = "";
                supportMessageFocused = false;
                loadUserTickets();
                return true;
            case CLOSE_TICKET:
                if (activeTicket != null) {
                    String adminName = client.player != null ? client.player.getName().getString() : "Admin";
                    SupabaseClient.getInstance().closeTicket(
                        activeTicket.id,
                        adminName,
                        () -> {
                            setStatusMessage(LocalizationManager.getInstance().get("ticket_closed"));
                            activeTicket.status = "closed";
                            loadUserTickets();
                        },
                        error -> setStatusMessage(LocalizationManager.getInstance().get("error_generic", error))
                    );
                }
                return true;
            case FOCUS_TICKET_MESSAGE:
                supportMessageFocused = true;
                newTicketSubjectFocused = false;
                priceFocused = false;
                descriptionFocused = false;
                messageFocused = false;
                return true;
            case SEND_TICKET_MESSAGE:
                sendTicketMessage();
                return true;
            default:
                supportMessageFocused = false;
                return true;
        }
    }
    
    // Защита от двойного создания тикета
    private boolean isCreatingTicket = false;
    
    /**
     * Создаёт новый тикет
     */
    private void createNewTicket() {
        if (newTicketSubject.trim().isEmpty()) return;
        
        // Защита от двойного клика
        if (isCreatingTicket) return;
        isCreatingTicket = true;
        
        // Проверка бана на поддержку
        if (isCurrentUserBannedSupport || isCurrentUserBannedFull) {
            setStatusMessage(LocalizationManager.getInstance().get("tickets_banned"));
            isCreatingTicket = false;
            return;
        }
        
        String playerName = client.player != null ? client.player.getName().getString() : "Unknown";
        final String subject = newTicketSubject.trim();
        
        setStatusMessage(LocalizationManager.getInstance().get("creating_ticket"));
        
        SupabaseClient.getInstance().createSupportTicket(
            subject,
            playerName,
            ticket -> {
                SupportTicket newTicket = new SupportTicket(
                    ticket.id,
                    ticket.subject,
                    ticket.status,
                    ticket.createdBy,
                    ticket.createdAt
                );
                supportTickets.add(0, newTicket);
                activeTicket = newTicket;
                ticketMessages.clear();
                creatingNewTicket = false;
                newTicketSubject = "";
                newTicketSubjectFocused = false;
                isCreatingTicket = false;
                setStatusMessage(LocalizationManager.getInstance().get("ticket_created"));
                // Логируем создание тикета
                RemoteLogger.getInstance().logTicketCreate(ticket.id, ticket.subject);
            },
            error -> {
                isCreatingTicket = false;
                statusMessage = LocalizationManager.getInstance().get("error_generic", error);
                TradeMarketMod.LOGGER.error("Error creating ticket: " + error);
                // Логируем ошибку
                RemoteLogger.getInstance().error(RemoteLogger.Category.TICKET, "TICKET_CREATE_ERROR", error);
            }
        );
    }
    
    /**
     * Отправляет сообщение в тикет
     */
    private void sendTicketMessage() {
        if (supportMessageText.trim().isEmpty() || activeTicket == null) return;
        
        // Проверка бана
        if (isCurrentUserBannedSupport || isCurrentUserBannedFull) {
            setStatusMessage(LocalizationManager.getInstance().get("messages_banned"));
            return;
        }
        
        String playerName = client.player != null ? client.player.getName().getString() : "Unknown";
        final String message = supportMessageText.trim();
        // Проверяем, является ли отправитель админом (для флага is_support)
        boolean isSupport = currentAdminInfo != null && currentAdminInfo.isAdmin;
        
        // Оптимисти��ное добавле��ие
        TicketMessage newMessage = new TicketMessage(
            UUID.randomUUID().toString(),
            activeTicket.id,
            playerName,
            message,
            System.currentTimeMillis(),
            isSupport
        );
        ticketMessages.add(newMessage);
        supportMessageText = "";
        
        // Автопрокрутка вниз
        supportChatScroll = Integer.MAX_VALUE;
        
        // Если это админ - обновляем last_active
        if (isSupport) {
            SupabaseClient.getInstance().updateAdminLastActive(playerName);
        }
        
        // Отправляем в Supabase
        final String ticketId = activeTicket.id;
        SupabaseClient.getInstance().sendTicketMessage(
            ticketId,
            playerName,
            message,
            isSupport,
            () -> {
                // Успешно отправлено - логируем
                RemoteLogger.getInstance().logTicketMessage(ticketId);
            },
            error -> {
                setStatusMessage(LocalizationManager.getInstance().get("send_error", error));
                // Удаляем оптимистично добавленное сообщение
                ticketMessages.remove(newMessage);
                // Логируем ошибку
                RemoteLogger.getInstance().error(RemoteLogger.Category.TICKET, "TICKET_MESSAGE_ERROR", error);
            }
        );
    }
    
    /**
     * Загружает сообщения тикета
     */
    private void loadTicketMessages(String ticketId) {
        ticketMessages.clear();
        supportChatScroll = 0;
        lastTicketMessagesRefresh = System.currentTimeMillis();
        
        SupabaseClient.getInstance().getTicketMessages(
            ticketId,
            messages -> {
                ticketMessages.clear();
                for (SupabaseClient.TicketMessage msg : messages) {
                    ticketMessages.add(new TicketMessage(
                        msg.id,
                        msg.ticketId,
                        msg.sender,
                        msg.message,
                        msg.timestamp,
                        msg.isSupport
                    ));
                }
                // Автопрокрутка к последним сообщениям (в новом дизайне скролл в пикселях)
                // Устанавливаем большое значение чтобы прокрутить до конца, оно скорректируется при рен��ере
                supportChatScroll = Integer.MAX_VALUE;
            },
            error -> TradeMarketMod.LOGGER.error("Error loading ticket messages: " + error)
        );
    }
    
    /**
     * Обновляет сообщения тикета без сброса скролла (для автообновления)
     */
    private void refreshTicketMessages() {
        if (activeTicket == null) return;
        
        lastTicketMessagesRefresh = System.currentTimeMillis();
        final int prevMessageCount = ticketMessages.size();
        final int currentScroll = supportChatScroll;
        
        SupabaseClient.getInstance().getTicketMessages(
            activeTicket.id,
            messages -> {
                // Если количество сообщений изменилос�� - обновляе��
                if (messages.size() != prevMessageCount) {
                    ticketMessages.clear();
                    for (SupabaseClient.TicketMessage msg : messages) {
                        ticketMessages.add(new TicketMessage(
                            msg.id,
                            msg.ticketId,
                            msg.sender,
                            msg.message,
                            msg.timestamp,
                            msg.isSupport
                        ));
                    }
                    // Если были новые сообщения - прокручиваем вниз
                    // Уведомления обрабатываются в NotificationChecker
                    if (messages.size() > prevMessageCount) {
                        supportChatScroll = Integer.MAX_VALUE;
                    }
                }
            },
            error -> {} // Тихо игнорируем ошибки при автообновлении
        );
    }
    
    /**
     * Рисует закладку с соцсетями справа от окна (современный минималистичный стиль)
     * Делегируется в BookmarkRenderer
     */
    private void drawSocialBookmark(DrawContext context, int mouseX, int mouseY) {
        int currentGuiRight = viewingDetails ? (this.width + DETAILS_WIDTH) / 2 : guiLeft + GUI_WIDTH;
        BookmarkRenderer.drawSocialBookmark(context, this.textRenderer,
            mouseX, mouseY, currentGuiRight, guiTop,
            bookmarkExpanded, infoPanelOffsetX, infoPanelOffsetY,
            isDraggingInfoPanel, this.width, this.height);
    }
    
    /**
     * Рисует закладку Support (ниже Info - современный минималистичный стиль)
     */
    private void drawSupportBookmark(DrawContext context, int mouseX, int mouseY) {
        int currentGuiRight = viewingDetails ? (this.width + DETAILS_WIDTH) / 2 : guiLeft + GUI_WIDTH;
        
        // Создаем состояние для передачи в BookmarkRenderer
        boolean isAdmin = currentAdminInfo != null && currentAdminInfo.isAdmin;
        String currentPlayer = client != null && client.player != null ? 
                client.player.getName().getString() : "";
        
        BookmarkRenderer.SupportPanelState state = new BookmarkRenderer.SupportPanelState(
            supportExpanded, supportOnline, unreadAdminMessagesCount,
            supportPanelOffsetX, supportPanelOffsetY, isDraggingSupportPanel,
            creatingNewTicket, activeTicket, ticketMessages,
            supportChatScroll, supportMessageText, supportMessageFocused,
            newTicketSubject, newTicketSubjectFocused,
            showingAdminChatInSupport, activeUserAdminChat,
            userChatMessages, userChatInput, userChatInputFocused,
            supportTickets, supportTicketScroll, userAdminChats,
            isAdmin, currentPlayer,
            lastSupportStatusCheck, lastAdminHeartbeat, lastUserChatsRefresh,
            lastTicketMessagesRefresh, lastAdminUserMessagesRefresh
        );
        
        // Создаем callbacks для периодических действий
        BookmarkRenderer.SupportPanelCallbacks callbacks = new BookmarkRenderer.SupportPanelCallbacks() {
            @Override
            public void checkSupportStatus() {
                TradeMarketScreen.this.checkSupportStatus();
            }
            
            @Override
            public void updateAdminHeartbeat() {
                lastAdminHeartbeat = System.currentTimeMillis();
                String playerName = client.player != null ? client.player.getName().getString() : null;
                if (playerName != null) {
                    SupabaseClient.getInstance().updateAdminLastActive(playerName);
                }
            }
            
            @Override
            public void loadUserAdminChats() {
                TradeMarketScreen.this.loadUserAdminChats();
            }
            
            @Override
            public void refreshTicketMessages() {
                TradeMarketScreen.this.refreshTicketMessages();
            }
            
            @Override
            public void loadUserChatMessages() {
                lastAdminUserMessagesRefresh = System.currentTimeMillis();
                TradeMarketScreen.this.loadUserChatMessages();
            }
        };
        
        // Делегируем отрисовку в BookmarkRenderer
        BookmarkRenderer.drawSupportBookmark(context, this.textRenderer,
            mouseX, mouseY, currentGuiRight, guiTop, this.width, this.height,
            state, callbacks, this::wrapText, this::trimTextToWidth);
    }
    
    /**
     * Загружает чаты пользователя с админами
     */
    private void loadUserAdminChats() {
        if (client.player == null) return;
        
        lastUserChatsRefresh = System.currentTimeMillis();
        String userUuid = client.player.getUuidAsString();
        
        SupabaseClient.getInstance().getUserAdminChats(userUuid, 
            chatsJson -> {
                userAdminChats.clear();
                unreadAdminMessagesCount = 0;
                
                for (int i = 0; i < chatsJson.size(); i++) {
                    com.google.gson.JsonObject chatObj = chatsJson.get(i).getAsJsonObject();
                    String id = chatObj.has("id") ? chatObj.get("id").getAsString() : "";
                    String adminName = chatObj.has("admin_name") ? chatObj.get("admin_name").getAsString() : "";
                    String userUuidVal = chatObj.has("user_uuid") ? chatObj.get("user_uuid").getAsString() : "";
                    String userName = chatObj.has("user_name") ? chatObj.get("user_name").getAsString() : "";
                    
                    String lastMessage = "";
                    if (chatObj.has("last_message") && !chatObj.get("last_message").isJsonNull()) {
                        com.google.gson.JsonObject lastMsgObj = chatObj.get("last_message").getAsJsonObject();
                        lastMessage = lastMsgObj.has("content") ? lastMsgObj.get("content").getAsString() : "";
                    }
                    
                    int unread = chatObj.has("unread_count") ? chatObj.get("unread_count").getAsInt() : 0;
                    unreadAdminMessagesCount += unread;
                    
                    long updatedAt = 0;
                    if (chatObj.has("updated_at") && !chatObj.get("updated_at").isJsonNull()) {
                        try {
                            String dateStr = chatObj.get("updated_at").getAsString();
                            updatedAt = java.time.Instant.parse(dateStr).toEpochMilli();
                        } catch (Exception e) {
                            updatedAt = System.currentTimeMillis();
                        }
                    }
                    
                    userAdminChats.add(new AdminChatForUser(id, adminName, userUuidVal, userName, lastMessage, unread, updatedAt));
                }
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load user admin chats: " + error)
        );
    }
    
    /**
     * Загружает сообщения чата пользователя с админом
     */
    private void loadUserChatMessages() {
        if (activeUserAdminChat == null) return;
        
        SupabaseClient.getInstance().getAdminChatMessages(activeUserAdminChat.id,
            messages -> {
                userChatMessages = messages;
                // Помечаем сообщения от админа как прочитанные
                markAdminMessagesAsReadInChat();
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load user chat messages: " + error)
        );
    }
    
    /**
     * Помечает сообщения от админа как прочитанные в текущем чате
     */
    private void markAdminMessagesAsReadInChat() {
        if (activeUserAdminChat == null) return;
        
        // Если есть непрочитанные сообщения - помечаем их
        if (activeUserAdminChat.unreadCount > 0) {
            SupabaseClient.getInstance().markAdminMessagesAsRead(
                activeUserAdminChat.id,
                () -> {
                    // Обновляем счетчик непрочитанных
                    activeUserAdminChat.unreadCount = 0;
                    // Обновляем общий счетчик
                    loadUserAdminChats();
                },
                error -> TradeMarketMod.LOGGER.debug("Failed to mark messages as read: " + error)
            );
        }
    }
    
    /**
     * Отправляет сообщение от пользователя в чат с админом
     */
    private void sendUserChatMessage() {
        if (activeUserAdminChat == null || userChatInput.trim().isEmpty() || client.player == null) return;
        
        String content = userChatInput.trim();
        String senderName = client.player.getName().getString();
        
        SupabaseClient.getInstance().sendAdminChatMessage(
            activeUserAdminChat.id,
            senderName,
            "user", // sender_type = user
            content,
            () -> {
                userChatInput = "";
                loadUserChatMessages();
            },
            error -> {
                setStatusMessage(LocalizationManager.getInstance().get("send_error", error));
                SoundManager.getInstance().playErrorSound();
            }
        );
    }
    
    /**
     * Разбивает текст на строки п�� ширине
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");
        
        for (String word : words) {
            if (currentLine.length() == 0) {
                // Если слово сли��ком длинное, разбиваем его
                while (this.textRenderer.getWidth(word) > maxWidth) {
                    int cutIndex = 1;
                    while (cutIndex < word.length() && this.textRenderer.getWidth(word.substring(0, cutIndex + 1)) <= maxWidth) {
                        cutIndex++;
                    }
                    lines.add(word.substring(0, cutIndex));
                    word = word.substring(cutIndex);
                }
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (this.textRenderer.getWidth(testLine) <= maxWidth) {
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
     * Рисует иконку закладки (открытая/закрытая) используя текстуры
     */
    private void drawBookmarkIcon(DrawContext context, int x, int y, boolean expanded) {
        Identifier texture = expanded ? TEXTURE_BOOK_OPEN : TEXTURE_BOOK_CLOSED;
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture,
                x, y, 0, 0, 12, 12, 16, 16);
    }
    
    /**
     * Обрезает текст до указанной ширины в пикселях
     */
    private String trimTextToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        
        String stripped = stripWynnAlignment(text);
        if (this.textRenderer.getWidth(stripped) <= maxWidth) {
            return stripped;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : stripped.toCharArray()) {
            String test = result.toString() + c + "...";
            if (this.textRenderer.getWidth(test) > maxWidth) {
                break;
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Получает отображаемое название предмета.
     * Для WynnCraft предметов берет первую строку из tooltip, которая сод��ржит реальное название.
     */
    private Text getItemDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Text.literal("Unknown");
        }
        
        // Пробуем получить tooltip - там обычно реальное название для WynnCraft предметов
        try {
            List<Text> tooltip = stack.getTooltip(
                    net.minecraft.item.Item.TooltipContext.create(client.world),
                    client.player, 
                    net.minecraft.item.tooltip.TooltipType.ADVANCED);
            
            if (tooltip != null && !tooltip.isEmpty()) {
                Text firstName = tooltip.get(0);
                // Проверяем, что на��вание не пустое посл�� удаления alignment символов
                if (!isWynnLineEmpty(firstName.getString())) {
                    return firstName;
                }
            }
        } catch (Exception e) {
            // Fallback to getName()
        }
        
        // Fallback - стандартн��е название
        Text name = stack.getName();
        if (!isWynnLineEmpty(name.getString())) {
            return name;
        }
        
        // Последний fallback - название предмета из регистра
        return Text.literal(stack.getItem().getName().getString());
    }
    
    private void drawTitle(DrawContext context) {
        LocalizationManager lang = LocalizationManager.getInstance();
        int titleY = guiTop + 12;
        
        // Область контента (справа от sidebar)
        int contentAreaX = guiLeft + SIDEBAR_WIDTH;
        int contentAreaWidth = GUI_WIDTH - SIDEBAR_WIDTH;
        
        // Современный текстовый заголовок в стиле app
        String mainTitle;
        if (viewingDetails) {
            mainTitle = lang.get("listing_details");
        } else {
            mainTitle = "Trade Market";
        }
        
        int titleWidth = this.textRenderer.getWidth(mainTitle);
        int titleX = contentAreaX + (contentAreaWidth - titleWidth) / 2;
        
        // Иконка корзины/магазина слева от заголовка
        String shopIcon = "\u2302"; // Домик
        int iconWidth = this.textRenderer.getWidth(shopIcon);
        int iconX = titleX - iconWidth - 6;
        context.drawTextWithShadow(this.textRenderer, Text.literal(shopIcon),
                iconX, titleY + 4, COLOR_GOLD);
        
        // Основной загол��вок
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(mainTitle).styled(s -> s.withBold(true)),
                titleX, titleY + 4,
                COLOR_TEXT_TITLE);
        
        // Подзаголовок (только для главного меню)
        if (!viewingDetails) {
            String subtitle = lang.isRussian() ? "Торговая площадка" : "Marketplace";
            int subtitleWidth = this.textRenderer.getWidth(subtitle);
            int subtitleX = contentAreaX + (contentAreaWidth - subtitleWidth) / 2;
            context.drawTextWithShadow(this.textRenderer, Text.literal(subtitle),
                    subtitleX, titleY + 16, COLOR_TEXT_MUTED);
        }
        
        // Тонкая разделительная линия под заголовком
        int lineY = titleY + (viewingDetails ? 18 : 30);
        int lineStartX = contentAreaX + 10;
        int lineEndX = guiLeft + GUI_WIDTH - 40;
        
        // Градиентная линия
        int lineCenterX = contentAreaX + contentAreaWidth / 2;
        int halfWidth = (lineEndX - lineStartX) / 2;
        for (int i = 0; i < halfWidth; i++) {
            int alpha = Math.min(50, 10 + (i * 40 / halfWidth));
            int color = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);
            context.fill(lineCenterX - i, lineY, lineCenterX - i + 1, lineY + 1, color);
            context.fill(lineCenterX + i, lineY, lineCenterX + i + 1, lineY + 1, color);
        }
        
        // Кнопка переключения язы��а
        int langBtnX = guiLeft + GUI_WIDTH - 30;
        int langBtnY = guiTop + 10;
        int langBtnSize = 22;
        
        context.fill(langBtnX, langBtnY, langBtnX + langBtnSize, langBtnY + langBtnSize, COLOR_BG_ITEM);
        drawBorder(context, langBtnX, langBtnY, langBtnSize, langBtnSize, COLOR_BORDER);
        
        // Текстура флага
        Identifier langTexture = lang.isRussian() ? TEXTURE_LANG_RU : TEXTURE_LANG_EN;
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, langTexture,
                langBtnX + 3, langBtnY + 3, 0, 0, 16, 16, 16, 16);
    }
    
    /**
     * Проверяет клик по кнопке языка
     */
    private boolean isLanguageButtonClicked(int mouseX, int mouseY) {
        int langBtnX = guiLeft + GUI_WIDTH - 30;
        int langBtnY = guiTop + 10;
        int langBtnSize = 22;
        return mouseX >= langBtnX && mouseX < langBtnX + langBtnSize &&
               mouseY >= langBtnY && mouseY < langBtnY + langBtnSize;
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY) {
        boolean isAdmin = currentAdminInfo != null && currentAdminInfo.isAdmin;
        SidebarRenderer.SidebarState state = new SidebarRenderer.SidebarState(
            currentTab, isAdmin, listingUnreadCounts
        );
        hoveredSidebarTab = SidebarRenderer.drawSidebar(context, this.textRenderer,
            guiLeft, guiTop, mouseX, mouseY, state);
    }
    
    // Тултип для боковой панели - вызывается в самом конце render() чтобы быть поверх всего
    private void drawSidebarTooltip(DrawContext context) {
        boolean isAdmin = currentAdminInfo != null && currentAdminInfo.isAdmin;
        SidebarRenderer.drawSidebarTooltip(context, this.textRenderer,
            guiLeft, guiTop, hoveredSidebarTab, isAdmin);
    }

    private void renderListings(DrawContext context, int mouseX, int mouseY) {
        ListingsRenderer.ListingsState state = new ListingsRenderer.ListingsState(
            currentTab, currentPage, currentSortMode,
            searchFocused, searchText, displayedListings
        );
        
        ListingsRenderer.renderListings(context, this.textRenderer,
            guiLeft, guiTop, mouseX, mouseY, state,
            listing -> {
                ItemStack stack = listing.getItemStack(MarketDataManager.getInstance().getRegistries());
                return getItemDisplayName(stack).getString();
            },
            (listing, stack) -> isWynnLineEmpty(listing.getItemDisplayName())
        );
    }

    private void renderSellUI(DrawContext context, int mouseX, int mouseY) {
        if (client.player == null) return;
        
        // Считаем количество моих лотов
        int myListingsCount = 0;
        String playerName = client.player.getName().getString();
        for (MarketListing listing : displayedListings) {
            if (listing.getSellerName().equals(playerName)) {
                myListingsCount++;
            }
        }
        
        SellUIRenderer.SellUIState state = new SellUIRenderer.SellUIState(
            selectedInventorySlot, priceText, priceFocused,
            descriptionText, descriptionFocused,
            isEditingListing, myListingsCount
        );
        
        SellUIRenderer.renderSellUI(context, this.textRenderer,
            guiLeft, guiTop, mouseX, mouseY, state,
            this::renderInventorySlot,
            slotIndex -> client.player.getInventory().getStack(slotIndex),
            this::wrapText);
    }

    private void drawInputField(DrawContext context, int x, int y, int width, int height,
            String text, String placeholder, boolean focused, int mouseX, int mouseY) {
        // Минималистичное поле ввода с переносом строк
        
        String displayText = text.isEmpty() ? placeholder : text;
        int textColor = text.isEmpty() ? COLOR_TEXT_MUTED : COLOR_TEXT_TITLE;
        
        // ��азбиваем текст на строки для определения высоты
        List<String> lines = wrapText(displayText, width - 12);
        if (lines.isEmpty()) {
            lines.add("");
        }
        
        int lineHeight = 10;
        int maxLines = 4; // Максимум 4 строки
        int actualLines = Math.min(lines.size(), maxLines);
        int dynamicHeight = Math.max(height, 12 + actualLines * lineHeight);
        
        // Фон поля ввода
        context.fill(x, y, x + width, y + dynamicHeight, COLOR_INPUT_BG);
        
        // Граница
        int borderColor = focused ? COLOR_GOLD : COLOR_BORDER;
        drawBorder(context, x, y, width, dynamicHeight, borderColor);
        
        // Нижняя акцентная линия при фокусе
        if (focused) {
            context.fill(x + 1, y + dynamicHeight - 2, x + width - 1, y + dynamicHeight - 1, COLOR_GOLD);
        }
        
        // Рисуем строки
        int textStartX = x + 6;
        int textY = y + 6;
        
        for (int i = 0; i < actualLines; i++) {
            String line = lines.get(i);
            // Если последняя видимая строка и есть еще текст, добавляем ...
            if (i == maxLines - 1 && lines.size() > maxLines) {
                if (this.textRenderer.getWidth(line) > width - 20) {
                    line = trimTextToWidth(line, width - 25) + "...";
                }
            }
            context.drawTextWithShadow(this.textRenderer, Text.literal(line),
                    textStartX, textY + (i * lineHeight), textColor);
        }
        
        // Мигающий курсор
        if (focused && System.currentTimeMillis() % 1000 < 500 && !text.isEmpty()) {
            int cursorLine = Math.min(lines.size() - 1, maxLines - 1);
            String lastLine = lines.get(Math.min(lines.size() - 1, maxLines - 1));
            int cursorX = textStartX + this.textRenderer.getWidth(lastLine);
            cursorX = Math.min(cursorX, x + width - 4);
            int cursorY = textY + (cursorLine * lineHeight);
            context.fill(cursorX, cursorY, cursorX + 1, cursorY + 8, COLOR_GOLD);
        } else if (focused && System.currentTimeMillis() % 1000 < 500) {
            // Пустой текст - курсор в начале
            context.fill(textStartX, textY, textStartX + 1, textY + 8, COLOR_GOLD);
        }
    }
    
    // Вычисляет динамическую высоту поля ввода
    private int getInputFieldHeight(String text, int width, int minHeight) {
        if (text.isEmpty()) return minHeight;
        List<String> lines = wrapText(text, width - 12);
        int lineHeight = 10;
        int maxLines = 4;
        int actualLines = Math.min(lines.size(), maxLines);
        return Math.max(minHeight, 12 + actualLines * lineHeight);
    }
    
    /**
     * Многострочное поле ввод�� с переносом текста
     */
    private void drawMultilineInputField(DrawContext context, int x, int y, int width, int height,
            String text, String placeholder, boolean focused, int mouseX, int mouseY) {
        // Минималистичное многострочное поле ввода
        
        // Фон поля ввода
        context.fill(x, y, x + width, y + height, COLOR_INPUT_BG);
        
        // Граница
        int borderColor = focused ? COLOR_GOLD : COLOR_BORDER;
        drawBorder(context, x, y, width, height, borderColor);
        
        // Нижняя акцентная линия при фокусе
        if (focused) {
            context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, COLOR_GOLD);
        }
        
        String displayText = text.isEmpty() ? placeholder : text;
        int textColor = text.isEmpty() ? COLOR_TEXT_MUTED : COLOR_TEXT_TITLE;
        
        // Разбиваем текст на строки
        List<String> lines = wrapText(displayText, width - 8);
        if (lines.isEmpty()) {
            lines.add(displayText);
        }
        
        int lineHeight = 10;
        int maxLines = (height - 4) / lineHeight;
        int textY = y + 4;
        
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            String line = lines.get(i);
            // Если последняя видимая строка и есть еще текст, добавляем ...
            if (i == maxLines - 1 && lines.size() > maxLines) {
                if (this.textRenderer.getWidth(line) > width - 20) {
                    line = trimTextToWidth(line, width - 25) + "...";
                } else {
                    line = line + "...";
                }
            }
            context.drawTextWithShadow(this.textRenderer, Text.literal(line),
                    x + 4, textY + (i * lineHeight), textColor);
        }
        
        // Курсор
        if (focused && System.currentTimeMillis() % 1000 < 500) {
            int cursorLine = 0;
            int cursorPos = 0;
            if (!text.isEmpty() && !lines.isEmpty()) {
                // Кур��ор в конце последней строки
                cursorLine = Math.min(lines.size() - 1, maxLines - 1);
                String lastLine = lines.get(Math.min(lines.size() - 1, maxLines - 1));
                cursorPos = this.textRenderer.getWidth(lastLine);
            }
            int cursorX = x + 4 + cursorPos;
            int cursorY = textY + (cursorLine * lineHeight);
            context.fill(cursorX, cursorY, cursorX + 1, cursorY + 8, COLOR_TEXT_NORMAL);
        }
    }

    private void drawButton(DrawContext context, int x, int y, int width, int height,
            String text, boolean hovered, int accentColor) {
        // Современный плоский дизайн кнопки в стиле app
        
        // Определяем цвет фона - для цветных кнопок используем accentColor
        boolean isColoredButton = (accentColor == COLOR_GREEN || accentColor == COLOR_RED || 
                                   accentColor == COLOR_BLUE || accentColor == COLOR_GOLD);
        
        int bgColor;
        int textColor;
        
        if (isColoredButton) {
            // Цветная кнопка - используем акцентный цвет как фон
            if (hovered) {
                // Светлее п��и hover
                int r = Math.min(255, ((accentColor >> 16) & 0xFF) + 20);
                int g = Math.min(255, ((accentColor >> 8) & 0xFF) + 20);
                int b = Math.min(255, (accentColor & 0xFF) + 20);
                bgColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            } else {
                bgColor = accentColor;
            }
            textColor = 0xFFFFFFFF;
        } else {
            // Обычная кнопка - серый фон
            bgColor = hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
            textColor = hovered ? COLOR_TEXT_TITLE : COLOR_TEXT_NORMAL;
        }
        
        // Фон кнопки
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Эмуляция скругленных углов
        int cornerColor = COLOR_BG_PANEL;
        context.fill(x, y, x + 1, y + 1, cornerColor);
        context.fill(x + width - 1, y, x + width, y + 1, cornerColor);
        context.fill(x, y + height - 1, x + 1, y + height, cornerColor);
        context.fill(x + width - 1, y + height - 1, x + width, y + height, cornerColor);
        
        // Тонкая граница только для неактивных обычных кнопок
        if (!isColoredButton) {
            drawBorder(context, x, y, width, height, hovered ? COLOR_BUTTON_BORDER : COLOR_BORDER);
        }
        
        int textWidth = this.textRenderer.getWidth(text);
        context.drawTextWithShadow(this.textRenderer, Text.literal(text),
                x + (width - textWidth) / 2, y + (height - 8) / 2, textColor);
    }

    private void renderInventorySlot(DrawContext context, int x, int y, int slotIndex, int mouseX, int mouseY) {
        int slotSize = 18;
        ItemStack stack = client.player.getInventory().getStack(slotIndex);

        boolean isSelected = slotIndex == selectedInventorySlot;
        boolean isHovered = mouseX >= x && mouseX < x + slotSize &&
                mouseY >= y && mouseY < y + slotSize;
        boolean hasItem = !stack.isEmpty();
        // Используем canSellItemBySlot для проверки с учётом номера слота
        boolean canSell = hasItem && canSellItemBySlot(slotIndex, stack);

        // Эффект свечения для выбранного слота
        if (isSelected && canSell) {
            context.fill(x - 1, y - 1, x + slotSize + 1, y + slotSize + 1, 
                    (COLOR_GREEN & 0x00FFFFFF) | 0x50000000);
        } else if (isHovered && hasItem && canSell) {
            context.fill(x - 1, y - 1, x + slotSize + 1, y + slotSize + 1, 
                    (COLOR_GOLD & 0x00FFFFFF) | 0x30000000);
        }

        // Фон слота
        context.fill(x, y, x + slotSize, y + slotSize, COLOR_SLOT_BG);
        
        // Граница
        int borderColor = isSelected ? COLOR_GREEN : (isHovered && hasItem && canSell ? COLOR_GOLD : COLOR_SLOT_BORDER);
        drawBorder(context, x, y, slotSize, slotSize, borderColor);
        
        // Подсветка внутри слота
        if (isSelected && canSell) {
            context.fill(x + 1, y + 1, x + slotSize - 1, y + slotSize - 1, 0x303FB950);
        } else if (isHovered && hasItem && canSell) {
            context.fill(x + 1, y + 1, x + slotSize - 1, y + slotSize - 1, 0x2500D9FF);
        }

        if (hasItem) {
            context.drawItem(stack, x + 1, y + 1);
            context.drawStackOverlay(this.textRenderer, stack, x + 1, y + 1);
            
            // Рисуем красный крестик для непродаваемых предметов
            if (!canSell) {
                drawRedCross(context, x, y, slotSize);
            }
        }
    }
    
    /**
     * Проверяет, можно ли продать предмет
     * @param stack предмет для проверки
     * @return true если предм��т можно продать
     */
    private boolean canSellItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Проверяем название предмета на н����продаваемые предметы Wynncraft
        String itemName = stack.getName().getString().toLowerCase();
        if (itemName.contains("emerald pouch") || 
            itemName.contains("content book") || 
            itemName.contains("character info") || 
            itemName.contains("ingredient pouch")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет, можно ли продать предмет по индексу слота
     * Проверка специальных слотов убрана - полагае��ся на проверку содержимого предмета
     */
    private boolean canSellItemBySlot(int slotIndex, ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Проверяем только сод��ржимое предмета, а не номер слота
        // Специальные предметы Wynncraft определяются ��о их названию и тегам
        return canSellItem(stack);
    }
    
    /**
     * Рисует красный крестик поверх слота
     */
    private void drawRedCross(DrawContext context, int x, int y, int slotSize) {
        int crossColor = 0xFFFF3333; // Красный цвет
        int padding = 2;
        int thickness = 2;
        
        // Рисуем диагональ слева-сверху в��раво-вниз
        for (int i = 0; i < slotSize - padding * 2; i++) {
            for (int t = 0; t < thickness; t++) {
                int px = x + padding + i;
                int py = y + padding + i + t;
                if (py < y + slotSize - padding) {
                    context.fill(px, py, px + 1, py + 1, crossColor);
                }
            }
        }
        
        // Рисуем диагональ справа-сверху влево-вниз
        for (int i = 0; i < slotSize - padding * 2; i++) {
            for (int t = 0; t < thickness; t++) {
                int px = x + slotSize - padding - i - 1;
                int py = y + padding + i + t;
                if (py < y + slotSize - padding) {
                    context.fill(px, py, px + 1, py + 1, crossColor);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x();
        int my = (int) click.y();
        
        // Проверяем клик по кнопке переключения языка
        if (isLanguageButtonClicked(mx, my)) {
            SoundManager.getInstance().playTabSwitchSound();
            LocalizationManager.getInstance().toggleLanguage();
            return true;
        }
        
        // Проверяем клик по закладке Support (ПЕРВЫ��, так как она перекрывает Info)
        if (handleSupportBookmarkClick(mx, my)) {
            SoundManager.getInstance().playClickSound();
            return true;
        }
        
        // Проверяем клик по закладке с соцсетями (Info)
        if (handleSocialBookmarkClick(mx, my)) {
            SoundManager.getInstance().playClickSound();
            return true;
        }
        
        // Обработка кликов по панели pending transactions
        if (handlePendingTransactionsClick(mx, my)) {
            SoundManager.getInstance().playClickSound();
            return true;
        }

        // Режим просмотра деталей
        if (viewingDetails && selectedListing != null) {
            int detailsGuiLeft = (this.width - DETAILS_WIDTH) / 2;
            int contentX = detailsGuiLeft + 15;
            int contentY = guiTop + 45; // Сдвинуто ниже (было 35)
            int mainPanelWidth = DETAILS_WIDTH - ITEM_PANEL_WIDTH - 45;
            int contentHeight = GUI_HEIGHT - 65; // Скорректировано (было 55)
            
            // Кнопка назад
            if (mx >= contentX + 5 && mx < contentX + 55 && my >= contentY + 5 && my < contentY + 21) {
                SoundManager.getInstance().playClickSound();
                viewingDetails = false;
                selectedListing = null;
                chatMessages.clear();
                messageText = "";
                // Сбрасываем кэш репутации и UI оценки
                currentSellerReputation = null;
                lastLoadedSellerReputationId = null;
                showRatingUI = false;
                selectedRating = 0;
                hasAlreadyRated = false;
                return true;
            }
            
            // Поле ввода сообщения - координаты синхронизированы с отрисо��ко��
            int itemInfoY = contentY + 25;
            // Вычисляем descriptionOffset так же как при рендеринге
            LocalizationManager langChat = LocalizationManager.getInstance();
            int chatCalcLineY = itemInfoY + 28;
            String priceValChat = selectedListing.getPrice();
            if (priceValChat != null && !priceValChat.isEmpty()) {
                chatCalcLineY += 16; // После цены
            }
            String descValChat = selectedListing.getDescription();
            if (descValChat != null && !descValChat.isEmpty()) {
                int maxDescWChat = mainPanelWidth - 30;
                String descLblChat = langChat.get("conditions");
                int descLblWChat = this.textRenderer.getWidth(descLblChat);
                List<String> dLinesChat = wrapText(descValChat, maxDescWChat - descLblWChat);
                if (dLinesChat.isEmpty()) dLinesChat.add(descValChat);
                chatCalcLineY += 12; // Первая строка
                chatCalcLineY += Math.min(dLinesChat.size() - 1, 2) * 12; // Остальные строки (макс 2)
                if (dLinesChat.size() > 3) chatCalcLineY += 12; // "..."
            }
            // Если это чужой лот - добавляем место для кнопки "Начать сделку"
            if (client.player != null && !selectedListing.getSellerId().equals(client.player.getUuid())) {
                chatCalcLineY += 18; // Высота кнопки + отступ
            }
            // Фиксированная позиция чата - синхронизируем с отрисовкой
            int fixedChatOffset = 95;
            int chatY = itemInfoY + fixedChatOffset;
            int chatHeight = contentHeight - fixedChatOffset - 45;
            chatHeight = Math.max(60, chatHeight);
            int chatWidth = mainPanelWidth - 20;
            int inputY = chatY + chatHeight + 5;
            int inputWidth = chatWidth - 75;
            int inputHeight = getInputFieldHeight(messageText, inputWidth, 20);
            
            // Клик по полю ввода сообщения (учитываем динамическую высоту)
            if (mx >= contentX + 10 && mx < contentX + 10 + inputWidth &&
                    my >= inputY && my < inputY + inputHeight) {
                messageFocused = true;
                descriptionFocused = false;
                priceFocused = false;
                return true;
            }
            
            // Кнопка отправить (выравнена по низу поля ввода)
            int sendBtnX = contentX + chatWidth - 60;
            int sendBtnY = inputY + inputHeight - 20;
            if (mx >= sendBtnX && mx < sendBtnX + 65 && my >= sendBtnY && my < sendBtnY + 20) {
                SoundManager.getInstance().playMessageSound();
                sendChatMessage();
                return true;
            }
            
            // Обработка кликов по кнопке оценки продавца
            if (client.player != null && !selectedListing.getSellerId().equals(client.player.getUuid())) {
                LocalizationManager lang = LocalizationManager.getInstance();
                boolean canRate = hasConfirmedTransaction && !hasAlreadyRated;
                
                String rateBtnText;
                if (hasAlreadyRated) {
                    rateBtnText = lang.get("already_rated");
                } else if (!hasConfirmedTransaction) {
                    rateBtnText = lang.get("need_confirmed_transaction");
                } else {
                    rateBtnText = lang.get("rate_seller");
                }
                
                int rateBtnWidth = this.textRenderer.getWidth(rateBtnText) + 12;
                int rateBtnX = contentX + mainPanelWidth - rateBtnWidth - 10;
                int rateBtnY = itemInfoY + 26;
                int rateBtnH = 14;
                
                // Клик по кнопке "Оценить продавца" (только если мо��но оценить)
                if (canRate && mx >= rateBtnX && mx < rateBtnX + rateBtnWidth && 
                    my >= rateBtnY && my < rateBtnY + rateBtnH) {
                    SoundManager.getInstance().playClickSound();
                    showRatingUI = !showRatingUI;
                    selectedRating = 0;
                    return true;
                }
                
                // Клик по зве��дам в UI оценки
                if (showRatingUI && canRate) {
                    int ratingUIX = rateBtnX - 50;
                    int ratingUIY = rateBtnY + rateBtnH + 2;
                    int ratingUIW = rateBtnWidth + 50;
                    
                    // Проверяем клик по каждой звезде
                    for (int i = 1; i <= 5; i++) {
                        int starX = ratingUIX + 10 + (i - 1) * 18;
                        int starY = ratingUIY + 6;
                        if (mx >= starX && mx < starX + 16 && my >= starY && my < starY + 16) {
                            SoundManager.getInstance().playClickSound();
                            selectedRating = i;
                            return true;
                        }
                    }
                    
                    // Клик по кнопке подтверждения
                    if (selectedRating > 0) {
                        String confirmText = lang.get("send");
                        int confirmX = ratingUIX + ratingUIW - this.textRenderer.getWidth(confirmText) - 10;
                        int confirmY = ratingUIY + 8;
                        if (mx >= confirmX - 4 && mx < confirmX + this.textRenderer.getWidth(confirmText) + 4 &&
                            my >= confirmY - 2 && my < confirmY + 12) {
                            // От��равляем оценку
                            SoundManager.getInstance().playRatingSound();
                            UUID sellerId = selectedListing.getSellerId();
                            String sellerName = selectedListing.getSellerName();
                            UUID raterId = client.player.getUuid();
                            String raterName = client.player.getName().getString();
                            
                            SupabaseClient.getInstance().rateSeller(sellerId, sellerName, raterId, raterName, selectedListing.getListingId(),
                                selectedRating, "",
                                () -> {
                                    hasAlreadyRated = true;
                                    showRatingUI = false;
                                    selectedRating = 0;
                                    // Перезагружаем репутацию
                                    lastLoadedSellerReputationId = null;
                                    setStatusMessage(lang.get("thanks_for_rating"));
                                },
                                error -> {
                                    setStatusMessage(error);
                                    SoundManager.getInstance().playErrorSound();
                                });
                            return true;
                        }
                    }
                }
            }
            
            // Клик по кнопке "Начать сделку" (только для чужих лотов)
            // Вычисляем позицию кнопки так же как при рендеринге
            if (client.player != null && !selectedListing.getSellerId().equals(client.player.getUuid())) {
                LocalizationManager langLocal = LocalizationManager.getInstance();
                
                // Вычисляем currentLineY к��к при рендеринге
                int calcLineY = itemInfoY + 28;
                String priceVal = selectedListing.getPrice();
                if (priceVal != null && !priceVal.isEmpty()) {
                    calcLineY += 16; // После цены
                }
                String descVal = selectedListing.getDescription();
                if (descVal != null && !descVal.isEmpty()) {
                    int maxDescW = mainPanelWidth - 30;
                    String descLbl = langLocal.get("conditions");
                    int descLblW = this.textRenderer.getWidth(descLbl);
                    List<String> dLines = wrapText(descVal, maxDescW - descLblW);
                    if (dLines.isEmpty()) dLines.add(descVal);
                    calcLineY += 12; // Первая строка
                    calcLineY += Math.min(dLines.size() - 1, 2) * 12; // Остальные строки (макс 2)
                    if (dLines.size() > 3) calcLineY += 12; // "..."
                }
                
                // Проверяем клик только если нет существу��щей сделки
                if (!hasExistingDealOnListing) {
                    String startDealText = langLocal.get("start_deal");
                    int startDealWidth = this.textRenderer.getWidth(startDealText) + 12;
                    int startDealX = contentX + 10;
                    int startDealY = calcLineY + 4;
                    int startDealH = 14;
                    
                    if (mx >= startDealX && mx < startDealX + startDealWidth && 
                        my >= startDealY && my < startDealY + startDealH) {
                        SoundManager.getInstance().playClickSound();
                        startDealWithSeller();
                        return true;
                    }
                }
            }
            
            // Обработка кликов админ-панели
            if (currentAdminInfo != null && currentAdminInfo.isAdmin) {
                if (handleAdminPanelClick(mx, my)) {
                    return true;
                }
            }
            
            messageFocused = false;
            return true;
        }

        // Боковая панель (параметры синхронизированы с drawSidebar)
        int sidebarX = guiLeft;
        int sidebarY = guiTop + 45;
        boolean isAdmin = currentAdminInfo != null && currentAdminInfo.isAdmin;
        int tabCount = isAdmin ? 6 : 5;
        int tabHeight = 44;
        int tabGap = 4;
        int startY = sidebarY + 10;
        
        for (int i = 0; i < tabCount; i++) {
            int tabY = startY + i * (tabHeight + tabGap);
            int tabX = sidebarX + 4;
            int tabW = SIDEBAR_WIDTH - 8;
            
            if (mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + tabHeight) {
                if (currentTab != i) {
                    SoundManager.getInstance().playTabSwitchSound();
                    currentTab = i;
                    currentPage = 0;
                    selectedListing = null;
                    selectedInventorySlot = -1;
                    descriptionText = "";
                    // При переключении на вкладку Messages загружаем непрочитанные
                    if (i == 4) {
                        loadUnreadMarketMessagesCount();
                        cachedUnreadListings.clear();
                        loadListingsWithUnreadMessages();
                    }
                    // При переключении на вкла��ку Users загружаем онлайн пользователей
                    if (i == 5 && isAdmin) {
                        loadOnlineUsers();
                        showAdminUserChat = false;
                        activeAdminUserChat = null;
                        selectedOnlineUser = null;
                    }
                    refreshListings();
                }
                return true;
            }
        }
        
        // Обработка кликов для вкладки Users (только для а����мино��)
        if (currentTab == 5 && isAdmin) {
            if (handleUsersTabClick(mx, my)) {
                return true;
            }
        }

        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55; // Под заголовком (без горизонтальных вкладок)
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70; // С��орректировано под новые отступы
        
        // ===== ОБРАБОТКА КЛИКОВ ПО ПОИСКУ И СОРТИРОВКЕ (только для вкладки "Все лоты") =====
        if (currentTab == 0) {
            int searchY = contentY + 5;
            int searchFieldX = contentX + 5;
            int searchFieldWidth = contentWidth - 125; // Синхронизировано с render
            int searchFieldHeight = 18; // Синхронизировано с render
            
            // Клик по полю поиска
            if (mx >= searchFieldX && mx < searchFieldX + searchFieldWidth &&
                    my >= searchY && my < searchY + searchFieldHeight) {
                searchFocused = true;
                priceFocused = false;
                descriptionFocused = false;
                messageFocused = false;
                return true;
            }
            
            // Клик по кнопке сортировки
            int sortBtnX = contentX + contentWidth - 115; // Синхронизировано с render
            int sortBtnWidth = 110; // Синхронизировано с render
            if (mx >= sortBtnX && mx < sortBtnX + sortBtnWidth &&
                    my >= searchY && my < searchY + searchFieldHeight) {
                SoundManager.getInstance().playClickSound();
                currentSortMode = (currentSortMode + 1) % SORT_MODES.length;
                // Применяем сортировку
                MarketDataManager dm = MarketDataManager.getInstance();
                switch (currentSortMode) {
                    case 0 -> dm.setSortMode(MarketDataManager.SortMode.NEWEST);
                    case 1 -> dm.setSortMode(MarketDataManager.SortMode.OLDEST);
                    case 2 -> dm.setSortMode(MarketDataManager.SortMode.PRICE_LOW);
                    case 3 -> dm.setSortMode(MarketDataManager.SortMode.PRICE_HIGH);
                }
                refreshListings();
                return true;
            }
            
            searchFocused = false;
            
            // Смещаем contentY для кликов по лотам (синхронизировано с render)
            contentY += 24;
            contentHeight -= 24;
        }

        // Вкладка продажи
        if (currentTab == 2 && client.player != null) {
            int slotSize = 18;
            int cols = 9;
            int rows = 3;
            int invStartX = contentX + (contentWidth - cols * slotSize) / 2;
            int invStartY = contentY + 25;
            int hotbarY = invStartY + rows * slotSize + 6;
            int infoY = hotbarY + slotSize + 8;
            int priceFieldY = infoY + 16;
            int halfWidth = (contentWidth - 30) / 2;
            
            // Поле цены
            if (mx >= contentX + 10 && mx < contentX + 10 + halfWidth &&
                    my >= priceFieldY + 10 && my < priceFieldY + 26) {
                priceFocused = true;
                descriptionFocused = false;
                messageFocused = false;
                return true;
            }
            
            // Поле условий
            if (mx >= contentX + 20 + halfWidth && mx < contentX + 20 + halfWidth * 2 &&
                    my >= priceFieldY + 10 && my < priceFieldY + 26) {
                priceFocused = false;
                descriptionFocused = true;
                messageFocused = false;
                return true;
            }
            
            // Кнопка выставить/сохранить
            int btnX = contentX + contentWidth / 2 - 70;
            int btnY = priceFieldY + 60;
            if (mx >= btnX && mx < btnX + 140 && my >= btnY && my < btnY + 20) {
                LocalizationManager lang = LocalizationManager.getInstance();
                
                // Валидация полей - проверяем что оба поля заполнены
                if (priceText.trim().isEmpty() && descriptionText.trim().isEmpty()) {
                    SoundManager.getInstance().playErrorSound();
                    setStatusMessage(lang.get("fill_all_fields"));
                    return true;
                }
                if (priceText.trim().isEmpty()) {
                    SoundManager.getInstance().playErrorSound();
                    setStatusMessage(lang.get("fill_price_field"));
                    return true;
                }
                if (descriptionText.trim().isEmpty()) {
                    SoundManager.getInstance().playErrorSound();
                    setStatusMessage(lang.get("fill_conditions_field"));
                    return true;
                }
                
                if (isEditingListing && editingListing != null) {
                    // Режим редактирования - обновляем лот
                    SoundManager.getInstance().playSuccessSound();
                    NetworkHandler.sendUpdateListing(editingListing.getListingId(), priceText, descriptionText);
                    isEditingListing = false;
                    editingListing = null;
                    priceText = "";
                    descriptionText = "";
                    currentTab = 1;
                    currentPage = 0;
                    refreshListings();
                } else if (selectedInventorySlot >= 0 && client.player != null) {
                    // Проверяем бан на выкладку
                    if (isCurrentUserBannedListing || isCurrentUserBannedFull) {
                        SoundManager.getInstance().playErrorSound();
                        setStatusMessage(lang.get("listing_banned"));
                        return true;
                    }
                    
                    // Проверяем лимит
                    int myListingsCount = 0;
                    String playerName = client.player.getName().getString();
                    for (MarketListing listing : displayedListings) {
                        if (listing.getSellerName().equals(playerName)) {
                            myListingsCount++;
                        }
                    }
                    
                    if (myListingsCount < MAX_LISTINGS_PER_USER) {
                        ItemStack stack = client.player.getInventory().getStack(selectedInventorySlot);
                        if (!stack.isEmpty()) {
                            SoundManager.getInstance().playSuccessSound();
                            NetworkHandler.sendCreateListing(selectedInventorySlot, priceText, descriptionText);
                            selectedInventorySlot = -1;
                            priceText = "";
                            descriptionText = "";
                            currentTab = 1;
                            currentPage = 0;
                            refreshListings();
                        }
                    }
                }
                return true;
            }
            
            // Кнопка отмены редактирования
            if (isEditingListing) {
                int cancelBtnX = contentX + contentWidth / 2 - 40;
                int cancelBtnY = btnY + 24;
                if (mx >= cancelBtnX && mx < cancelBtnX + 80 && my >= cancelBtnY && my < cancelBtnY + 16) {
                    SoundManager.getInstance().playClickSound();
                    isEditingListing = false;
                    editingListing = null;
                    priceText = "";
                    descriptionText = "";
                    currentTab = 1;
                    return true;
                }
            }

            // Слоты инвентаря
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int slotX = invStartX + col * slotSize;
                    int slotY = invStartY + row * slotSize;
                    if (mx >= slotX && mx < slotX + slotSize && my >= slotY && my < slotY + slotSize) {
                        int slotIndex = 9 + row * 9 + col;
                        ItemStack stack = client.player.getInventory().getStack(slotIndex);
                        // Используем canSellItemBySlot для проверки с учётом номера слота
                        if (!stack.isEmpty() && canSellItemBySlot(slotIndex, stack)) {
                            SoundManager.getInstance().playClickSound();
                            selectedInventorySlot = slotIndex;
                        }
                        return true;
                    }
                }
            }

            // Хотбар
            for (int col = 0; col < 9; col++) {
                int slotX = invStartX + col * slotSize;
                if (mx >= slotX && mx < slotX + slotSize && my >= hotbarY && my < hotbarY + slotSize) {
                    ItemStack stack = client.player.getInventory().getStack(col);
                    // Используем canSellItemBySlot для провер��и с учётом номера слота
                    if (!stack.isEmpty() && canSellItemBySlot(col, stack)) {
                        SoundManager.getInstance().playClickSound();
                        selectedInventorySlot = col;
                    }
                    return true;
                }
            }
        }

        // Обзор, мои лоты и избранное
        if ((currentTab == 0 || currentTab == 1 || currentTab == 3) && displayedListings != null && !displayedListings.isEmpty()) {
            int startIndex = currentPage * LISTINGS_PER_PAGE;
            int endIndex = Math.min(startIndex + LISTINGS_PER_PAGE, displayedListings.size());

            for (int i = startIndex; i < endIndex; i++) {
                int itemY = contentY + 6 + (i - startIndex) * ITEM_HEIGHT;
                int itemWidth = contentWidth - 12;
                int itemX = contentX + 6;
                
                // Кнопки действия (синхронизировано с renderListings)
                int btnX = (currentTab == 0 || currentTab == 3) ? itemX + itemWidth - 60 : itemX + itemWidth - 55;
                int btnW = (currentTab == 0 || currentTab == 3) ? 55 : 50;
                int btnY = itemY + 10;
                int btnH = 16;
                
                MarketListing listing = displayedListings.get(i);
                
                if (currentTab == 1) {
                    // Мои лоты - проверяем кнопку "Ред."
                    int editBtnX = itemX + itemWidth - 100;
                    int editBtnW = 40;
                    if (mx >= editBtnX && mx < editBtnX + editBtnW && my >= btnY && my < btnY + btnH) {
                        // Редактировать лот
                        isEditingListing = true;
                        editingListing = listing;
                        priceText = listing.getPrice();
                        descriptionText = listing.getDescription();
                        currentTab = 2; // Переключаем��я н�� вкладку "П��одать"
                        return true;
                    }
                }
                
                // Клик по кнопке избранного (для вкладок "Все лоты" и "Избранное")
                if (currentTab == 0 || currentTab == 3) {
                    int favBtnX = itemX + itemWidth - 85;
                    int favBtnY = itemY + 10;
                    if (mx >= favBtnX && mx < favBtnX + 20 && my >= favBtnY && my < favBtnY + 16) {
                        SoundManager.getInstance().playFavoriteSound();
                        UUID listingId = listing.getListingId();
                        UUID playerId = client.player != null ? client.player.getUuid() : null;
                        if (playerId != null) {
                            if (MarketDataManager.getInstance().isFavorite(listingId)) {
                                MarketDataManager.getInstance().removeFromFavorites(playerId, listingId,
                                    () -> setStatusMessage(LocalizationManager.getInstance().get("removed_from_favorites")),
                                    error -> setStatusMessage(LocalizationManager.getInstance().get("error") + ": " + error));
                            } else {
                                MarketDataManager.getInstance().addToFavorites(playerId, listingId,
                                    () -> setStatusMessage(LocalizationManager.getInstance().get("added_to_favorites")),
                                    error -> setStatusMessage(LocalizationManager.getInstance().get("error") + ": " + error));
                            }
                        }
                        return true;
                    }
                }
                
                if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                    if (currentTab == 0 || currentTab == 3) {
                        // Открыть детали лота
                        SoundManager.getInstance().playClickSound();
                        selectedListing = listing;
                        viewingDetails = true;
                        hasExistingDealOnListing = false; // Сб��асываем флаг
                        chatMessages.clear();
                        loadChatMessages();
                        // Проверяем есть ли уже сделка на этот лот
                        checkExistingDealOnListing();
                    } else if (currentTab == 1) {
                        // С��ять лот
                        SoundManager.getInstance().playClickSound();
                        NetworkHandler.sendRemoveListing(listing.getListingId());
                        refreshListings();
                    }
                    return true;
                }
            }

            // Навигация
            int totalPages = (int) Math.ceil(displayedListings.size() / (double) LISTINGS_PER_PAGE);
            if (totalPages > 1) {
                int navY = contentY + contentHeight - 20;
                
                if (currentPage > 0 && mx >= contentX + 10 && mx < contentX + 30 &&
                        my >= navY && my < navY + 16) {
                    SoundManager.getInstance().playClickSound();
                    currentPage--;
                    return true;
                }
                
                if (currentPage < totalPages - 1 && mx >= contentX + contentWidth - 30 &&
                        mx < contentX + contentWidth - 10 && my >= navY && my < navY + 16) {
                    SoundManager.getInstance().playClickSound();
                    currentPage++;
                    return true;
                }
            }
        }
        
        // Клик на вкладке Чаты (Messages) - открыть лот с непрочитанными сообщениями
        if (currentTab == 4 && !listingUnreadCounts.isEmpty()) {
            int listY = contentY + 30;
            int itemHeight = 45;
            int maxVisible = (contentHeight - 40) / itemHeight;
            
            // Собираем лоты с непрочитанными
            java.util.List<MarketListing> unreadListings = new java.util.ArrayList<>();
            for (UUID listingId : listingUnreadCounts.keySet()) {
                MarketListing listing = findListingById(listingId);
                if (listing != null) {
                    unreadListings.add(listing);
                }
            }
            
            for (int i = 0; i < Math.min(unreadListings.size(), maxVisible); i++) {
                int itemY = listY + i * itemHeight;
                
                if (mx >= contentX + 5 && mx < contentX + contentWidth - 10 &&
                        my >= itemY && my < itemY + itemHeight - 5) {
                    // Открыть детали этого лота
                    SoundManager.getInstance().playClickSound();
                    selectedListing = unreadListings.get(i);
                    viewingDetails = true;
                    hasExistingDealOnListing = false;
                    chatMessages.clear();
                    loadChatMessages();
                    checkExistingDealOnListing();
                    return true;
                }
            }
        }

        descriptionFocused = false;
        messageFocused = false;
        return super.mouseClicked(click, bl);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Скролл панели pending transactions (объеди��енный спи��ок)
        int totalPending = pendingTransactions.size() + sellerPendingTransactions.size();
        if (showPendingTransactionsPanel && totalPending > 3) {
            int maxScroll = Math.max(0, totalPending - 3);
            int newOffset = pendingTransactionsScroll - (int) verticalAmount;
            pendingTransactionsScroll = Math.max(0, Math.min(maxScroll, newOffset));
            return true;
        }
        
        // Скролл чата с продавцом
        if (viewingDetails && !chatMessages.isEmpty()) {
            int maxScroll = Math.max(0, chatMessages.size() - chatMaxVisible);
            int newOffset = chatScrollOffset - (int) verticalAmount;
            chatScrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
            return true;
        }
        
        // Скро��л чата поддержки (тикеты)
        if (supportExpanded && activeTicket != null && !ticketMessages.isEmpty()) {
            // Скролл в пикселях (примерно 20 пиксе��ей за один скролл)
            int scrollAmount = (int)(-verticalAmount * 20);
            supportChatScroll += scrollAmount;
            if (supportChatScroll < 0) supportChatScroll = 0;
            // Максимум скорректируется при рендере
            return true;
        }
        
        // Скролл для вкладки Users (список онлайн пользователей)
        if (currentTab == 5 && !showAdminUserChat && !onlineUsers.isEmpty()) {
            int contentHeight = GUI_HEIGHT - 70;
            int listHeight = contentHeight - 60;
            int itemHeight = 28;
            int maxVisible = listHeight / itemHeight;
            int maxScroll = Math.max(0, onlineUsers.size() - maxVisible);
            int newOffset = onlineUsersScrollOffset - (int) verticalAmount;
            onlineUsersScrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
            return true;
        }
        
        // Скролл для чата админ-юзер
        if (currentTab == 5 && showAdminUserChat && !adminUserMessages.isEmpty()) {
            int maxScroll = Math.max(0, adminUserMessages.size() - 5); // примерно 5 сообщений видно
            int newOffset = adminUserChatScrollOffset - (int) verticalAmount;
            adminUserChatScrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        // Перетаскив��ние панели поддержки
        if (isDraggingSupportPanel) {
            supportPanelOffsetX = (int) click.x() - dragStartX;
            supportPanelOffsetY = (int) click.y() - dragStartY;
            return true;
        }
        // Перетаскивание панели Info
        if (isDraggingInfoPanel) {
            infoPanelOffsetX = (int) click.x() - infoDragStartX;
            infoPanelOffsetY = (int) click.y() - infoDragStartY;
            return true;
        }
        // Перетаскивание админ-панели
        if (isDraggingAdminPanel) {
            adminPanelOffsetX = (int) click.x() - adminDragStartX;
            adminPanelOffsetY = (int) click.y() - adminDragStartY;
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(Click click) {
        if (isDraggingSupportPanel) {
            isDraggingSupportPanel = false;
            return true;
        }
        if (isDraggingInfoPanel) {
            isDraggingInfoPanel = false;
            return true;
        }
        if (isDraggingAdminPanel) {
            isDraggingAdminPanel = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        // Проверяем нажатие Ctrl через GLFW
        long windowHandle = client.getWindow().getHandle();
        boolean hasControlDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                                 GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        
        // Обработка Ctrl+V (вставка из буфера обмена) для всех полей ввода
        if (hasControlDown && keyCode == 86) { // 86 = V
            String clipboard = client.keyboard.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                // Удаляем переносы строк для однострочных полей
                String cleanClipboard = clipboard.replace("\n", " ").replace("\r", "");
                
                if (searchFocused) {
                    int maxLen = 30 - searchText.length();
                    if (maxLen > 0) {
                        searchText += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                        MarketDataManager.getInstance().setSearchQuery(searchText);
                        refreshListings();
                    }
                    return true;
                }
                if (newTicketSubjectFocused) {
                    int maxLen = 50 - newTicketSubject.length();
                    if (maxLen > 0) {
                        newTicketSubject += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
                if (supportMessageFocused) {
                    int maxLen = 200 - supportMessageText.length();
                    if (maxLen > 0) {
                        supportMessageText += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
                if (adminUserChatInputFocused) {
                    int maxLen = 200 - adminUserChatInput.length();
                    if (maxLen > 0) {
                        adminUserChatInput += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
                if (userChatInputFocused) {
                    int maxLen = 200 - userChatInput.length();
                    if (maxLen > 0) {
                        userChatInput += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
                if (priceFocused) {
                    int maxLen = 30 - priceText.length();
                    if (maxLen > 0) {
                        priceText += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
                if (descriptionFocused) {
                    int maxLen = 100 - descriptionText.length();
                    if (maxLen > 0) {
                        descriptionText += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
                if (messageFocused) {
                    int maxLen = 200 - messageText.length();
                    if (maxLen > 0) {
                        messageText += cleanClipboard.substring(0, Math.min(cleanClipboard.length(), maxLen));
                    }
                    return true;
                }
            }
        }
        
        // Обработка Ctrl+C (копирование) - копирует весь текст из активного поля
        if (hasControlDown && keyCode == 67) { // 67 = C
            String textToCopy = null;
            if (searchFocused && !searchText.isEmpty()) textToCopy = searchText;
            else if (newTicketSubjectFocused && !newTicketSubject.isEmpty()) textToCopy = newTicketSubject;
            else if (supportMessageFocused && !supportMessageText.isEmpty()) textToCopy = supportMessageText;
            else if (adminUserChatInputFocused && !adminUserChatInput.isEmpty()) textToCopy = adminUserChatInput;
            else if (userChatInputFocused && !userChatInput.isEmpty()) textToCopy = userChatInput;
            else if (priceFocused && !priceText.isEmpty()) textToCopy = priceText;
            else if (descriptionFocused && !descriptionText.isEmpty()) textToCopy = descriptionText;
            else if (messageFocused && !messageText.isEmpty()) textToCopy = messageText;
            
            if (textToCopy != null) {
                client.keyboard.setClipboard(textToCopy);
                return true;
            }
        }
        
        // Обработка Ctrl+A (выделить всё) - очищает поле для быстрого ввода нового текста
        if (hasControlDown && keyCode == 65) { // 65 = A
            // Пока просто возвращаем true чтобы предотвратить действие по умолчанию
            return true;
        }
        
        // Обраб��тка поля поиска
        if (searchFocused) {
            if (keyCode == 259) { // Backspace
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    MarketDataManager.getInstance().setSearchQuery(searchText);
                    refreshListings();
                }
                return true;
            }
            if (keyCode == 256) { // Escape
                searchFocused = false;
                return true;
            }
        }
        
        // Обработк�� полей Support
        if (newTicketSubjectFocused || supportMessageFocused) {
            if (keyCode == 259) { // Backspace
                if (newTicketSubjectFocused && !newTicketSubject.isEmpty()) {
                    newTicketSubject = newTicketSubject.substring(0, newTicketSubject.length() - 1);
                } else if (supportMessageFocused && !supportMessageText.isEmpty()) {
                    supportMessageText = supportMessageText.substring(0, supportMessageText.length() - 1);
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                if (supportMessageFocused && activeTicket != null) {
                    sendTicketMessage();
                } else if (newTicketSubjectFocused && !newTicketSubject.trim().isEmpty()) {
                    createNewTicket();
                }
                return true;
            }
            if (keyCode == 256) { // Escape
                newTicketSubjectFocused = false;
                supportMessageFocused = false;
                return true;
            }
        }
        
        // Обработка поля чата админ-юзер (вкладка Users для админов)
        if (adminUserChatInputFocused) {
            if (keyCode == 259) { // Backspace
                if (!adminUserChatInput.isEmpty()) {
                    adminUserChatInput = adminUserChatInput.substring(0, adminUserChatInput.length() - 1);
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                if (!adminUserChatInput.trim().isEmpty()) {
                    sendAdminUserChatMessage();
                }
                return true;
            }
            if (keyCode == 256) { // Escape
                adminUserChatInputFocused = false;
                return true;
            }
        }
        
        // Обработка поля чата пользователя с админом (в Support)
        if (userChatInputFocused) {
            if (keyCode == 259) { // Backspace
                if (!userChatInput.isEmpty()) {
                    userChatInput = userChatInput.substring(0, userChatInput.length() - 1);
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                if (!userChatInput.trim().isEmpty()) {
                    sendUserChatMessage();
                }
                return true;
            }
            if (keyCode == 256) { // Escape
                userChatInputFocused = false;
                return true;
            }
        }
        
        if (priceFocused || descriptionFocused || messageFocused) {
            if (keyCode == 259) { // Backspace
                if (priceFocused && !priceText.isEmpty()) {
                    priceText = priceText.substring(0, priceText.length() - 1);
                } else if (descriptionFocused && !descriptionText.isEmpty()) {
                    descriptionText = descriptionText.substring(0, descriptionText.length() - 1);
                } else if (messageFocused && !messageText.isEmpty()) {
                    messageText = messageText.substring(0, messageText.length() - 1);
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                if (messageFocused && viewingDetails) {
                    sendChatMessage();
                }
                priceFocused = false;
                descriptionFocused = false;
                messageFocused = false;
                return true;
            }
            if (keyCode == 256) { // Escape
                descriptionFocused = false;
                messageFocused = false;
                return true;
            }
        }
        
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        char chr = (char) charInput.codepoint();
        
        // Ввод в поле поиска
        if (searchFocused && searchText.length() < 30) {
            searchText += chr;
            // Применяем поиск
            MarketDataManager.getInstance().setSearchQuery(searchText);
            refreshListings();
            return true;
        }
        
        // Ввод для полей Support
        if (newTicketSubjectFocused && newTicketSubject.length() < 50) {
            newTicketSubject += chr;
            return true;
        }
        if (supportMessageFocused && supportMessageText.length() < 200) {
            supportMessageText += chr;
            return true;
        }
        
        // Ввод для чата админ-юзер (вкладка Users для админов)
        if (adminUserChatInputFocused && adminUserChatInput.length() < 200) {
            adminUserChatInput += chr;
            return true;
        }
        
        // Ввод для чата пользователя с админом (в Support)
        if (userChatInputFocused && userChatInput.length() < 200) {
            userChatInput += chr;
            return true;
        }
        
        if (priceFocused && priceText.length() < 30) {
            priceText += chr;
            return true;
        }
        if (descriptionFocused && descriptionText.length() < 100) {
            descriptionText += chr;
            return true;
        }
        if (messageFocused && messageText.length() < 200) {
            messageText += chr;
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
    
    // ==================== PENDING TRANSACTIONS ====================
    
    /**
     * Обработка кликов по панели pending transactions
     */
    private boolean handlePendingTransactionsClick(int mx, int my) {
        // Обрабатываем только в режиме просмотр�� деталей лота
        if (!viewingDetails) return false;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        int buyerPendingCount = pendingTransactions.size();
        int sellerPendingCount = sellerPendingTransactions.size();
        int pendingCount = buyerPendingCount + sellerPendingCount;
        
        // Кнопка внутри ле��ой панели деталей (спр��ва от "< Назад")
        int contentX = guiLeft + 10;
        int btnWidth = 40;
        int btnHeight = 18;
        int btnX = contentX + 192; // Синхронизировано с drawPendingTransactionsPanel
        int btnY = guiTop + 58; // Сдвину��о ниже (было 38)
        
        if (mx >= btnX && mx < btnX + btnWidth && my >= btnY && my < btnY + btnHeight) {
            showPendingTransactionsPanel = !showPendingTransactionsPanel;
            pendingTransactionsScroll = 0;
            return true;
        }
        
        // Если панель о��крыта
        if (showPendingTransactionsPanel) {
            int panelWidth = 280;
            int panelHeight = pendingCount > 0 ? Math.min(200, 60 + pendingCount * 50) : 80;
            int panelX = this.width / 2 - panelWidth / 2;
            int panelY = this.height / 2 - panelHeight / 2;
            
            // Клик внутри панели
            if (mx >= panelX && mx < panelX + panelWidth && my >= panelY && my < panelY + panelHeight) {
                
                // Кнопка закрытия
                int closeX = panelX + panelWidth - 18;
                int closeY = panelY + 5;
                if (mx >= closeX && mx < closeX + 14 && my >= closeY && my < closeY + 14) {
                    showPendingTransactionsPanel = false;
                    return true;
                }
                
                // Клики ��о транзакциям - создаем объединенный список
                List<TransactionWithType> allTransactions = new ArrayList<>();
                for (SupabaseClient.Transaction t : pendingTransactions) {
                    allTransactions.add(new TransactionWithType(t, false)); // buyer
                }
                for (SupabaseClient.Transaction t : sellerPendingTransactions) {
                    allTransactions.add(new TransactionWithType(t, true)); // seller
                }
                
                int listY = panelY + 25;
                int itemHeight = 45;
                int visibleCount = Math.min(3, allTransactions.size());
                
                for (int i = 0; i < visibleCount && (i + pendingTransactionsScroll) < allTransactions.size(); i++) {
                    TransactionWithType twt = allTransactions.get(i + pendingTransactionsScroll);
                    SupabaseClient.Transaction trans = twt.transaction;
                    boolean isSeller = twt.isSellerView;
                    int itemY = listY + i * itemHeight;
                    
                    // Кнопка подтверждения - только для покупателя
                    if (!isSeller) {
                        String confirmText = lang.get("confirm_transaction");
                        int confirmWidth = this.textRenderer.getWidth(confirmText) + 10;
                        int confirmX = panelX + panelWidth - confirmWidth - 15;
                        int confirmY = itemY + 26;
                        
                        if (mx >= confirmX && mx < confirmX + confirmWidth &&
                            my >= confirmY && my < confirmY + 12) {
                            confirmPendingTransaction(trans);
                            return true;
                        }
                    }
                }
                
                return true; // Поглощаем кли�� внут��и панели
            } else {
                // Клик вне панели - закрываем её
                showPendingTransactionsPanel = false;
                return false;
            }
        }
        
        return false;
    }
    
    // ==================== USERS TAB (ADMIN) ====================
    
    /**
     * Загружает список онлайн пользователей
     */
    private void loadOnlineUsers() {
        lastOnlineUsersRefresh = System.currentTimeMillis();
        SupabaseClient.getInstance().getOnlineUsers(
            users -> {
                onlineUsers = users;
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load online users: " + error)
        );
    }
    
    /**
     * Рендерит вкладку "Чаты" с непрочитанными сообщениями - делегирует в TabsRenderer
     */
    private void renderMessagesTab(DrawContext context, int mouseX, int mouseY) {
        // Собираем лоты с непрочитанными сообщениями
        java.util.List<MarketListing> unreadListings = new java.util.ArrayList<>();
        for (UUID listingId : listingUnreadCounts.keySet()) {
            MarketListing listing = findListingById(listingId);
            if (listing != null) {
                unreadListings.add(listing);
            }
        }
        
        // Если не нашли лоты - загрузим их
        if (unreadListings.isEmpty() && !listingUnreadCounts.isEmpty()) {
            loadListingsWithUnreadMessages();
            return;
        }
        
        TabsRenderer.MessagesTabState state = new TabsRenderer.MessagesTabState(
            listingUnreadCounts, unreadListings
        );
        
        TabsRenderer.renderMessagesTab(context, this.textRenderer,
            guiLeft, guiTop, mouseX, mouseY, state,
            stack -> getItemDisplayName(stack).getString());
    }
    
    /**
     * Ищет лот по ID среди загруженных
     */
    private MarketListing findListingById(UUID listingId) {
        if (displayedListings != null) {
            for (MarketListing listing : displayedListings) {
                if (listing.getListingId().equals(listingId)) {
                    return listing;
                }
            }
        }
        // Проверяем в кэше лотов с непрочитанными
        if (cachedUnreadListings != null) {
            for (MarketListing listing : cachedUnreadListings) {
                if (listing.getListingId().equals(listingId)) {
                    return listing;
                }
            }
        }
        return null;
    }
    
    // Кэш лотов с непрочитанными сообщениями
    private java.util.List<MarketListing> cachedUnreadListings = new java.util.ArrayList<>();
    private long lastUnreadListingsLoad = 0;
    
    /**
     * Загружает лоты с непрочитанными сообщениями
     */
    private void loadListingsWithUnreadMessages() {
        if (System.currentTimeMillis() - lastUnreadListingsLoad < 5000) return; // Не чаще раз в 5 сек
        lastUnreadListingsLoad = System.currentTimeMillis();
        
        // Загружаем лоты по ID из listingUnreadCounts
        for (UUID listingId : listingUnreadCounts.keySet()) {
            SupabaseClient.getInstance().getListingById(listingId,
                listing -> {
                    if (listing != null && !cachedUnreadListings.contains(listing)) {
                        cachedUnreadListings.add(listing);
                    }
                },
                error -> TradeMarketMod.LOGGER.debug("Failed to load listing: " + error)
            );
        }
    }
    
    /**
     * Рендерит вкладку "Пользователи" (только для админов) - делегирует в TabsRenderer
     */
    private void renderUsersTab(DrawContext context, int mouseX, int mouseY) {
        // Автообновление списка онлайн пользователей
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOnlineUsersRefresh > ONLINE_USERS_REFRESH_INTERVAL) {
            loadOnlineUsers();
        }
        
        // Если открыт чат с пользователем - рендерим чат
        if (showAdminUserChat && activeAdminUserChat != null) {
            renderAdminUserChat(context, mouseX, mouseY);
            return;
        }
        
        TabsRenderer.UsersTabState state = new TabsRenderer.UsersTabState(
            onlineUsers, onlineUsersScrollOffset,
            showAdminUserChat, selectedOnlineUser != null ? selectedOnlineUser.playerName : null
        );
        
        TabsRenderer.renderUsersTab(context, this.textRenderer,
            guiLeft, guiTop, mouseX, mouseY, state);
    }
    
    /**
     * Рендерит чат админ-юзер
     */
    private void renderAdminUserChat(DrawContext context, int mouseX, int mouseY) {
        if (activeAdminUserChat == null || selectedOnlineUser == null) return;
        
        // Автообновление сообщений
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdminUserMessagesRefresh > ADMIN_USER_MESSAGES_REFRESH_INTERVAL) {
            loadAdminUserChatMessages();
        }
        
        TabsRenderer.AdminUserChatState state = new TabsRenderer.AdminUserChatState(
            selectedOnlineUser.playerName, adminUserMessages,
            adminUserChatInput, adminUserChatInputFocused, adminUserChatScrollOffset
        );
        
        TabsRenderer.renderAdminUserChat(context, this.textRenderer,
            guiLeft, guiTop, mouseX, mouseY, state, this::trimTextToWidth);
    }
    
    /**
     * Загружает сообщения чата админ-юзер
     */
    private void loadAdminUserChatMessages() {
        if (activeAdminUserChat == null) return;
        
        lastAdminUserMessagesRefresh = System.currentTimeMillis();
        SupabaseClient.getInstance().getAdminChatMessages(
            activeAdminUserChat.id,
            messages -> {
                adminUserMessages = messages;
                // Помечаем сообщения от пользователя ка�� прочитанные для админа
                markUserMessagesAsReadForAdmin();
            },
            error -> TradeMarketMod.LOGGER.debug("Failed to load admin-user chat messages: " + error)
        );
    }
    
    /**
     * Помечает соо��щения от пользователя как прочитанные (для админа)
     */
    private void markUserMessagesAsReadForAdmin() {
        if (activeAdminUserChat == null) return;
        
        // Если есть непрочитанные сообщения от пользователя
        if (activeAdminUserChat.unreadCount > 0) {
            SupabaseClient.getInstance().markUserMessagesAsReadForAdmin(
                activeAdminUserChat.id,
                () -> {
                    activeAdminUserChat.unreadCount = 0;
                },
                error -> TradeMarketMod.LOGGER.debug("Failed to mark user messages as read: " + error)
            );
        }
    }
    
    /**
     * Обрабатывает клики на вкладке Users
     */
    private boolean handleUsersTabClick(int mx, int my) {
        LocalizationManager lang = LocalizationManager.getInstance();
        
        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70;
        
        // Если открыт чат с пользователем
        if (showAdminUserChat && activeAdminUserChat != null) {
            // К��опка назад
            int backBtnX = contentX + 5;
            int backBtnY = contentY + 5;
            if (mx >= backBtnX && mx < backBtnX + 55 && my >= backBtnY && my < backBtnY + 18) {
                SoundManager.getInstance().playClickSound();
                showAdminUserChat = false;
                activeAdminUserChat = null;
                selectedOnlineUser = null;
                adminUserMessages.clear();
                adminUserChatInput = "";
                return true;
            }
            
            // Поле ввода сообщения
            int inputY = contentY + contentHeight - 30;
            int inputWidth = contentWidth - 80;
            if (mx >= contentX + 5 && mx < contentX + 5 + inputWidth && my >= inputY && my < inputY + 20) {
                adminUserChatInputFocused = true;
                return true;
            }
            
            // Кнопка отправить
            int sendBtnX = contentX + contentWidth - 70;
            int sendBtnY = inputY;
            if (mx >= sendBtnX && mx < sendBtnX + 65 && my >= sendBtnY && my < sendBtnY + 20) {
                if (!adminUserChatInput.trim().isEmpty()) {
                    SoundManager.getInstance().playMessageSound();
                    sendAdminUserChatMessage();
                }
                return true;
            }
            
            adminUserChatInputFocused = false;
            return true;
        }
        
        // Список онлайн пользователей
        int listY = contentY + 22;
        int listHeight = contentHeight - 60;
        int itemHeight = 28;
        int maxVisible = listHeight / itemHeight;
        
        for (int i = 0; i < maxVisible && (i + onlineUsersScrollOffset) < onlineUsers.size(); i++) {
            SupabaseClient.OnlineUser user = onlineUsers.get(i + onlineUsersScrollOffset);
            int itemY = listY + 5 + i * itemHeight;
            
            // Кнопка "Написать"
            int writeBtnWidth = this.textRenderer.getWidth(lang.get("write_message")) + 10;
            int writeBtnX = contentX + contentWidth - writeBtnWidth - 25;
            int writeBtnY = itemY + 5;
            int writeBtnH = itemHeight - 13;
            
            if (mx >= writeBtnX && mx < writeBtnX + writeBtnWidth && my >= writeBtnY && my < writeBtnY + writeBtnH) {
                SoundManager.getInstance().playClickSound();
                openChatWithUser(user);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Открывает чат с пользователем
     */
    private void openChatWithUser(SupabaseClient.OnlineUser user) {
        if (client.player == null || currentAdminInfo == null || !currentAdminInfo.isAdmin) return;
        
        selectedOnlineUser = user;
        String adminName = client.player.getName().getString();
        
        // Создаем или получаем существующ��й чат
        SupabaseClient.getInstance().createOrGetAdminChat(
            adminName,
            user.playerUUID,
            user.playerName,
            chat -> {
                activeAdminUserChat = chat;
                showAdminUserChat = true;
                adminUserMessages.clear();
                adminUserChatInput = "";
                loadAdminUserChatMessages();
            },
            error -> {
                setStatusMessage(LocalizationManager.getInstance().get("error_generic", error));
                SoundManager.getInstance().playErrorSound();
            }
        );
    }
    
    /**
     * Отправляет сообщение в чат админ-юзер
     */
    private void sendAdminUserChatMessage() {
        if (activeAdminUserChat == null || adminUserChatInput.trim().isEmpty() || client.player == null) return;
        
        String content = adminUserChatInput.trim();
        String senderName = client.player.getName().getString();
        
        SupabaseClient.getInstance().sendAdminChatMessage(
            activeAdminUserChat.id,
            senderName,
            "admin",
            content,
            () -> {
                adminUserChatInput = "";
                loadAdminUserChatMessages();
            },
            error -> {
                setStatusMessage(LocalizationManager.getInstance().get("send_error", error));
                SoundManager.getInstance().playErrorSound();
            }
        );
    }
    
    /**
     * Рисует панель ожидающих подтверж��ения транзакций
     */
    private void drawPendingTransactionsPanel(DrawContext context, int mouseX, int mouseY) {
        // Показыва��м только в режиме просмотра деталей лота
        if (!viewingDetails) return;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        // Подсчет pending транзакций - для покупателя и для продавца
        int buyerPendingCount = pendingTransactions.size();
        int sellerPendingCount = sellerPendingTransactions.size();
        int pendingCount = buyerPendingCount + sellerPendingCount;
        
        // ��нопка внутри левой панели деталей (справа от "< Назад")
        int contentX = guiLeft + 10;
        int btnWidth = 40;
        int btnHeight = 18;
        int btnX = contentX + 192; // Справа от кнопки "< Назад"
        int btnY = guiTop + 48; // Выше, на уровне кнопки "< Назад"
        
        // Рисуем кнопку с современным оформлением
        boolean btnHover = mouseX >= btnX && mouseX < btnX + btnWidth && 
                          mouseY >= btnY && mouseY < btnY + btnHeight;
        
        // Эффект свечения если есть pending транзакции
        if (pendingCount > 0) {
            context.fill(btnX - 2, btnY - 2, btnX + btnWidth + 2, btnY + btnHeight + 2,
                    (COLOR_GOLD & 0x00FFFFFF) | (btnHover ? 0x60000000 : 0x30000000));
        }
        
        // Фон кнопки
        int btnBgColor = pendingCount > 0 ? (btnHover ? COLOR_GOLD : COLOR_BUTTON_BG) : (btnHover ? COLOR_BUTTON_HOVER : COLOR_BG_PANEL);
        context.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnBgColor);
        drawBorder(context, btnX, btnY, btnWidth, btnHeight, pendingCount > 0 ? COLOR_GOLD : COLOR_BORDER);
        
        // Акцент сверху для активной кнопки
        if (pendingCount > 0) {
            context.fill(btnX + 1, btnY, btnX + btnWidth - 1, btnY + 1, COLOR_GOLD);
        }
        
        // Иконка галочка + количество
        String btnContent = pendingCount > 0 ? "\u2714 " + pendingCount : "\u2714";
        int iconColor = pendingCount > 0 ? (btnHover ? 0xFF000000 : COLOR_GOLD) : COLOR_TEXT_MUTED;
        int textWidth = this.textRenderer.getWidth(btnContent);
        context.drawTextWithShadow(this.textRenderer, Text.literal(btnContent),
                btnX + (btnWidth - textWidth) / 2, btnY + 5, iconColor);
        
        // Тултип при наведении
        if (btnHover && !showPendingTransactionsPanel) {
            String tooltip = lang.get("pending_transactions") + " (" + pendingCount + ")";
            int tooltipWidth = this.textRenderer.getWidth(tooltip) + 8;
            int tooltipX = mouseX + 10;
            int tooltipY = mouseY - 15;
            context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 14, COLOR_BG_DARK);
            drawBorder(context, tooltipX, tooltipY, tooltipWidth, 14, COLOR_BORDER);
            context.drawTextWithShadow(this.textRenderer, Text.literal(tooltip),
                    tooltipX + 4, tooltipY + 3, COLOR_TEXT_NORMAL);
        }
        
        // Панель со списком транзакций с современным оформлением
        if (showPendingTransactionsPanel) {
            int panelWidth = 300;
            int panelHeight = pendingCount > 0 ? Math.min(220, 70 + pendingCount * 50) : 90;
            int panelX = this.width / 2 - panelWidth / 2;
            int panelY = this.height / 2 - panelHeight / 2;
            
            // Тень панели
            context.fill(panelX + 4, panelY + 4, panelX + panelWidth + 4, panelY + panelHeight + 4, 0x60000000);
            
            // Фон панели
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BG_DARK);
            // Акцентная линия сверху
            context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, COLOR_GOLD);
            drawBorder(context, panelX, panelY, panelWidth, panelHeight, COLOR_BORDER);
            
            // Заголовок с подложкой
            String title = lang.get("pending_transactions");
            int titleWidth = this.textRenderer.getWidth(title);
            int titleX = panelX + panelWidth / 2 - titleWidth / 2;
            context.fill(titleX - 10, panelY + 6, titleX + titleWidth + 10, panelY + 18, 0x30000000);
            context.drawTextWithShadow(this.textRenderer, Text.literal(title),
                    titleX, panelY + 8, COLOR_GOLD);
            
            // Кнопка закрытия с улучшенным оформлением
            int closeX = panelX + panelWidth - 20;
            int closeY = panelY + 5;
            boolean closeHover = mouseX >= closeX && mouseX < closeX + 16 && 
                                mouseY >= closeY && mouseY < closeY + 16;
            if (closeHover) {
                context.fill(closeX - 1, closeY - 1, closeX + 15, closeY + 15, (COLOR_RED & 0x00FFFFFF) | 0x40000000);
            }
            context.fill(closeX, closeY, closeX + 14, closeY + 14, closeHover ? COLOR_RED : COLOR_BUTTON_BG);
            drawBorder(context, closeX, closeY, 14, 14, closeHover ? COLOR_RED : COLOR_BORDER);
            context.drawTextWithShadow(this.textRenderer, Text.literal("X"),
                    closeX + 4, closeY + 3, closeHover ? 0xFFFFFFFF : COLOR_TEXT_MUTED);
            
            // Список транзакций
            int listY = panelY + 25;
            if (pendingCount == 0) {
                String noTrans = lang.get("no_pending_transactions");
                context.drawTextWithShadow(this.textRenderer, Text.literal(noTrans),
                        panelX + panelWidth / 2 - this.textRenderer.getWidth(noTrans) / 2,
                        listY + 20, COLOR_TEXT_MUTED);
            } else {
                int itemHeight = 45;
                int visibleCount = Math.min(3, pendingCount);
                
                // Создаем о��ъединенный список транзакций
                List<TransactionWithType> allTransactions = new ArrayList<>();
                for (SupabaseClient.Transaction t : pendingTransactions) {
                    allTransactions.add(new TransactionWithType(t, false)); // buyer
                }
                for (SupabaseClient.Transaction t : sellerPendingTransactions) {
                    allTransactions.add(new TransactionWithType(t, true)); // seller
                }
                
                for (int i = 0; i < visibleCount && (i + pendingTransactionsScroll) < allTransactions.size(); i++) {
                    TransactionWithType twt = allTransactions.get(i + pendingTransactionsScroll);
                    SupabaseClient.Transaction trans = twt.transaction;
                    boolean isSeller = twt.isSellerView;
                    int itemY = listY + i * itemHeight;
                    
                    // Фон элемента - разный цвет для покупателя и продавца
                    boolean itemHover = mouseX >= panelX + 10 && mouseX < panelX + panelWidth - 10 &&
                                       mouseY >= itemY && mouseY < itemY + itemHeight - 5;
                    int itemBg = isSeller ? (itemHover ? 0xFF2A3828 : 0xFF1E2A1E) : (itemHover ? COLOR_BG_ITEM_HOVER : COLOR_BG_ITEM);
                    context.fill(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight - 5, itemBg);
                    drawBorder(context, panelX + 10, itemY, panelWidth - 20, itemHeight - 5, 
                            isSeller ? COLOR_GREEN : COLOR_BORDER);
                    
                    // Информация о транзакции
                    String itemName = trans.itemDisplayName;
                    if (itemName.length() > 25) itemName = itemName.substring(0, 22) + "...";
                    context.drawTextWithShadow(this.textRenderer, Text.literal(itemName),
                            panelX + 15, itemY + 5, COLOR_TEXT_TITLE);
                    
                    // Для покупателя показываем продавца, для продавца - покупателя
                    String infoText;
                    if (isSeller) {
                        infoText = lang.get("buyer") + ": " + trans.buyerName;
                    } else {
                        infoText = lang.get("seller", trans.sellerName);
                    }
                    context.drawTextWithShadow(this.textRenderer, Text.literal(infoText),
                            panelX + 15, itemY + 16, COLOR_TEXT_MUTED);
                    
                    // Кнопка подтверждения - только для покупателя (он подтверждает получение)
                    if (!isSeller) {
                        String confirmText = lang.get("confirm_transaction");
                        int confirmWidth = this.textRenderer.getWidth(confirmText) + 10;
                        int confirmX = panelX + panelWidth - confirmWidth - 15;
                        int confirmY = itemY + 26;
                        
                        boolean confirmHover = mouseX >= confirmX && mouseX < confirmX + confirmWidth &&
                                              mouseY >= confirmY && mouseY < confirmY + 12;
                        context.fill(confirmX, confirmY, confirmX + confirmWidth, confirmY + 12,
                                confirmHover ? COLOR_GREEN : COLOR_BUTTON_BG);
                        drawBorder(context, confirmX, confirmY, confirmWidth, 12, COLOR_GREEN);
                        context.drawTextWithShadow(this.textRenderer, Text.literal(confirmText),
                                confirmX + 5, confirmY + 2, confirmHover ? 0xFFFFFFFF : COLOR_GREEN);
                    } else {
                        // Для продавца показываем статус "Ожидает"
                        String waitingText = lang.get("pending_confirmation");
                        context.drawTextWithShadow(this.textRenderer, Text.literal(waitingText),
                                panelX + 15, itemY + 27, COLOR_GOLD);
                    }
                }
                
                // Скроллбар если нужен
                if (pendingCount > 3) {
                    int scrollBarHeight = panelHeight - 35;
                    int scrollThumbHeight = Math.max(20, scrollBarHeight * 3 / pendingCount);
                    int scrollThumbY = listY + (scrollBarHeight - scrollThumbHeight) * pendingTransactionsScroll / Math.max(1, pendingCount - 3);
                    
                    context.fill(panelX + panelWidth - 8, listY, panelX + panelWidth - 4, listY + scrollBarHeight, COLOR_BG_PANEL);
                    context.fill(panelX + panelWidth - 8, scrollThumbY, panelX + panelWidth - 4, scrollThumbY + scrollThumbHeight, COLOR_GOLD_DARK);
                }
            }
        }
    }
    
    // TransactionWithType вынесен в SupportModels.java
    
    /**
     * Подтвердить транзакцию
     */
    private void confirmPendingTransaction(SupabaseClient.Transaction transaction) {
        if (client.player == null) return;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        SupabaseClient.getInstance().confirmTransaction(
            transaction.id,
            client.player.getUuid(),
            () -> {
                setStatusMessage(lang.get("transaction_confirmed"));
                SoundManager.getInstance().playSuccessSound();
                
                // Показываем уведомление о завершении сделки
                ToastNotificationManager.getInstance().showDealCompleted(transaction.sellerName);
                
                // Удаляем лот из базы после успешного подтверждения ��делки
                SupabaseClient.getInstance().deleteListing(
                    transaction.listingId,
                    () -> {
                        TradeMarketMod.LOGGER.info("[TradeMarket] Listing " + transaction.listingId + " removed after successful transaction");
                        // Перезагружаем список лотов чтобы убрат�� проданный предмет
                        refreshListings();
                    },
                    error -> {
                        TradeMarketMod.LOGGER.warn("[TradeMarket] Failed to remove listing after transaction: " + error);
                    }
                );
                
                // Перезагружаем оба сп��ска
                loadPendingTransactions();
                loadSellerPendingTransactions();
                // Сбрасываем кэш репутации чтобы обновить счетчик успешных сделок
                lastLoadedSellerReputationId = null;
            },
            error -> {
                setStatusMessage(lang.get("error_generic", error));
                SoundManager.getInstance().playErrorSound();
            }
        );
    }
    
    // Внутренние классы SupportTicket, TicketMessage, AdminChatForUser вынесены в SupportModels.java
}
