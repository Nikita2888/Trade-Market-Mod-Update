package com.trademarket.data;

import com.trademarket.TradeMarketMod;
import net.minecraft.registry.RegistryWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.function.Consumer;

/**
 * Управляет всеми лотами на рынке
 * Работает с Supabase для хранения данных в облаке
 */
public class MarketDataManager {
    private static MarketDataManager INSTANCE;
    private final Map<UUID, MarketListing> listings = new ConcurrentHashMap<>();
    private RegistryWrapper.WrapperLookup registries;
    
    // Время последнего обновления
    private long lastFetchTime = 0;
    private static final long FETCH_COOLDOWN = 5000; // 5 секунд между запросами
    
    // Callbacks для уведомления UI об обновлениях
    private final List<Runnable> updateListeners = new ArrayList<>();
    
    // Избранное (локальный кэш)
    private final Set<UUID> favoriteListings = ConcurrentHashMap.newKeySet();
    
    // Фильтры и поиск
    private String searchQuery = "";
    private SortMode sortMode = SortMode.NEWEST;
    
    public enum SortMode {
        NEWEST, OLDEST, PRICE_LOW, PRICE_HIGH, SELLER
    }

    private MarketDataManager() {}

    public static MarketDataManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MarketDataManager();
        }
        return INSTANCE;
    }
    
    public void setRegistries(RegistryWrapper.WrapperLookup registries) {
        this.registries = registries;
    }
    
    public RegistryWrapper.WrapperLookup getRegistries() {
        return registries;
    }

    /**
     * Добавить слушатель обновлений
     */
    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }
    
    public void removeUpdateListener(Runnable listener) {
        updateListeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (Runnable listener : updateListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                TradeMarketMod.LOGGER.error("Error in update listener", e);
            }
        }
    }

    /**
     * Загрузить все лоты из Supabase
     */
    public void fetchAllListings(Runnable onComplete, Consumer<String> onError) {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < FETCH_COOLDOWN) {
            if (onComplete != null) onComplete.run();
            return;
        }
        lastFetchTime = now;
        
        SupabaseClient.getInstance().fetchAllListings(
            fetchedListings -> {
                listings.clear();
                for (MarketListing listing : fetchedListings) {
                    listings.put(listing.getListingId(), listing);
                }
                TradeMarketMod.LOGGER.info("Fetched {} listings from Supabase", listings.size());
                notifyListeners();
                if (onComplete != null) onComplete.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to fetch listings: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }
    
    /**
     * Загрузить лоты конкретного продавца
     */
    public void fetchSellerListings(UUID sellerId, Consumer<List<MarketListing>> onComplete, Consumer<String> onError) {
        SupabaseClient.getInstance().fetchSellerListings(
            sellerId.toString(),
            onComplete,
            error -> {
                TradeMarketMod.LOGGER.error("Failed to fetch seller listings: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }

    /**
     * Создать новый лот и отправить в Supabase
     */
    public void createListing(MarketListing listing, Runnable onSuccess, Consumer<String> onError) {
        SupabaseClient.getInstance().createListing(
            listing,
            () -> {
                listings.put(listing.getListingId(), listing);
                notifyListeners();
                if (onSuccess != null) onSuccess.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to create listing: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }

    /**
     * Удалить лот (деактивировать в Supabase)
     */
    public void removeListing(UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        SupabaseClient.getInstance().removeListing(
            listingId,
            () -> {
                listings.remove(listingId);
                notifyListeners();
                if (onSuccess != null) onSuccess.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to remove listing: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }
    
    /**
     * Обновить лот (цену и условия)
     */
    public void updateListing(UUID listingId, String price, String description, Runnable onSuccess, Consumer<String> onError) {
        SupabaseClient.getInstance().updateListing(
            listingId,
            price,
            description,
            () -> {
                // Обновляем локальный кэш
                MarketListing listing = listings.get(listingId);
                if (listing != null) {
                    listing.setPrice(price);
                    listing.setDescription(description);
                }
                notifyListeners();
                if (onSuccess != null) onSuccess.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to update listing: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }

    /**
     * Получает лот по ID (из локального кэша)
     */
    public MarketListing getListing(UUID listingId) {
        return listings.get(listingId);
    }

    /**
     * Получает все активные лоты (из локального кэша)
     */
    public List<MarketListing> getActiveListings() {
        return listings.values().stream()
                .filter(MarketListing::isActive)
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .toList();
    }

    /**
     * Получает лоты конкретного продавца (из локального кэша)
     */
    public List<MarketListing> getListingsBySeller(UUID sellerId) {
        return listings.values().stream()
                .filter(l -> l.getSellerId().equals(sellerId))
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .toList();
    }
    
    /**
     * Принудительно обновить данные (сбросить cooldown)
     */
    public void forceRefresh(Runnable onComplete, Consumer<String> onError) {
        lastFetchTime = 0;
        fetchAllListings(onComplete, onError);
    }
    
    /**
     * Очистить локальный кэш
     */
    public void clearCache() {
        listings.clear();
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ ИЗБРАННОГО
    // =====================================================
    
    /**
     * Загрузить избранное пользователя
     */
    public void loadFavorites(UUID userId, Runnable onComplete, Consumer<String> onError) {
        SupabaseClient.getInstance().fetchFavorites(userId, 
            listingIds -> {
                favoriteListings.clear();
                favoriteListings.addAll(listingIds);
                notifyListeners();
                if (onComplete != null) onComplete.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to load favorites: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }
    
    /**
     * Добавить в избранное
     */
    public void addToFavorites(UUID userId, UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        SupabaseClient.getInstance().addToFavorites(userId, listingId,
            () -> {
                favoriteListings.add(listingId);
                notifyListeners();
                if (onSuccess != null) onSuccess.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to add to favorites: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }
    
    /**
     * Удалить из избранного
     */
    public void removeFromFavorites(UUID userId, UUID listingId, Runnable onSuccess, Consumer<String> onError) {
        SupabaseClient.getInstance().removeFromFavorites(userId, listingId,
            () -> {
                favoriteListings.remove(listingId);
                notifyListeners();
                if (onSuccess != null) onSuccess.run();
            },
            error -> {
                TradeMarketMod.LOGGER.error("Failed to remove from favorites: " + error);
                if (onError != null) onError.accept(error);
            }
        );
    }
    
    /**
     * Проверить, есть ли лот в избранном
     */
    public boolean isFavorite(UUID listingId) {
        return favoriteListings.contains(listingId);
    }
    
    /**
     * Получить избранные лоты
     */
    public List<MarketListing> getFavoriteListings() {
        return listings.values().stream()
                .filter(l -> l.isActive() && favoriteListings.contains(l.getListingId()))
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .toList();
    }
    
    // =====================================================
    // МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ И ПОИСКА
    // =====================================================
    
    /**
     * Установить поисковый запрос
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
        notifyListeners();
    }
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    /**
     * Установить режим сортировки
     */
    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        notifyListeners();
    }
    
    public SortMode getSortMode() {
        return sortMode;
    }
    
    /**
     * Получить отфильтрованные и отсортированные лоты
     */
    public List<MarketListing> getFilteredListings() {
        return listings.values().stream()
                .filter(MarketListing::isActive)
                .filter(this::matchesSearch)
                .sorted(this::compareBySortMode)
                .collect(Collectors.toList());
    }
    
    /**
     * Проверить, соответствует ли лот поисковому запросу
     */
    private boolean matchesSearch(MarketListing listing) {
        if (searchQuery.isEmpty()) return true;
        
        String displayName = listing.getItemDisplayName().toLowerCase();
        String itemId = listing.getItemId().toLowerCase();
        String sellerName = listing.getSellerName().toLowerCase();
        String price = listing.getPrice().toLowerCase();
        String description = listing.getDescription().toLowerCase();
        
        return displayName.contains(searchQuery) ||
               itemId.contains(searchQuery) ||
               sellerName.contains(searchQuery) ||
               price.contains(searchQuery) ||
               description.contains(searchQuery);
    }
    
    /**
     * Сравнение лотов по режиму сортировки
     */
    private int compareBySortMode(MarketListing a, MarketListing b) {
        switch (sortMode) {
            case OLDEST:
                return Long.compare(a.getCreatedAt(), b.getCreatedAt());
            case PRICE_LOW:
                return comparePrices(a.getPrice(), b.getPrice());
            case PRICE_HIGH:
                return comparePrices(b.getPrice(), a.getPrice());
            case SELLER:
                return a.getSellerName().compareToIgnoreCase(b.getSellerName());
            case NEWEST:
            default:
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        }
    }
    
    /**
     * Сравнение цен (извлекает числа из строк)
     */
    private int comparePrices(String price1, String price2) {
        int num1 = extractNumber(price1);
        int num2 = extractNumber(price2);
        return Integer.compare(num1, num2);
    }
    
    /**
     * Извлечь число из строки цены
     */
    private int extractNumber(String price) {
        if (price == null || price.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        for (char c : price.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) return 0;
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Сбросить фильтры
     */
    public void clearFilters() {
        searchQuery = "";
        sortMode = SortMode.NEWEST;
        notifyListeners();
    }
}
