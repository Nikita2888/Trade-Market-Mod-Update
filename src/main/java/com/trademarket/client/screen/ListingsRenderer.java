package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.data.MarketDataManager;
import com.trademarket.data.MarketListing;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Рендерер для списка лотов
 */
public class ListingsRenderer {
    
    /**
     * Состояние списка лотов
     */
    public static class ListingsState {
        public final int currentTab;
        public final int currentPage;
        public final int currentSortMode;
        public final boolean searchFocused;
        public final String searchText;
        public final List<MarketListing> displayedListings;
        
        public ListingsState(int currentTab, int currentPage, int currentSortMode,
                            boolean searchFocused, String searchText, List<MarketListing> displayedListings) {
            this.currentTab = currentTab;
            this.currentPage = currentPage;
            this.currentSortMode = currentSortMode;
            this.searchFocused = searchFocused;
            this.searchText = searchText;
            this.displayedListings = displayedListings;
        }
    }
    
    /**
     * Рендерит список лотов
     */
    public static void renderListings(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int mouseX, int mouseY, ListingsState state,
            Function<MarketListing, String> getItemDisplayName,
            BiFunction<MarketListing, ItemStack, Boolean> isWynnLineEmpty) {
        
        int contentX = guiLeft + SIDEBAR_WIDTH + 10;
        int contentY = guiTop + 55;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
        int contentHeight = GUI_HEIGHT - 70;

        // Современная панель контента
        context.fill(contentX + 3, contentY + 3, contentX + contentWidth + 3, contentY + contentHeight + 3, 0x20000000);
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, COLOR_BG_ITEM);
        RenderUtils.drawBorder(context, contentX, contentY, contentWidth, contentHeight, COLOR_BORDER);
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        // Панель поиска и сортировки (только для вкладки "Все лоты")
        if (state.currentTab == 0) {
            int searchY = contentY + 5;
            
            // Поле поиска
            int searchFieldX = contentX + 5;
            int searchFieldWidth = contentWidth - 125;
            int searchFieldHeight = 18;
            
            if (state.searchFocused) {
                context.fill(searchFieldX - 1, searchY - 1, searchFieldX + searchFieldWidth + 1, searchY + searchFieldHeight + 1,
                        (COLOR_GOLD & 0x00FFFFFF) | 0x40000000);
            }
            
            context.fill(searchFieldX, searchY, searchFieldX + searchFieldWidth, searchY + searchFieldHeight, COLOR_INPUT_BG);
            context.fill(searchFieldX + 1, searchY + 1, searchFieldX + searchFieldWidth - 1, searchY + 2, 0x15000000);
            RenderUtils.drawBorder(context, searchFieldX, searchY, searchFieldWidth, searchFieldHeight, 
                    state.searchFocused ? COLOR_GOLD : COLOR_BORDER);
            
            context.drawTextWithShadow(textRenderer, Text.literal("\u26B2"),
                    searchFieldX + 4, searchY + 5, COLOR_TEXT_MUTED);
            
            String displaySearchText = state.searchText.isEmpty() && !state.searchFocused ? lang.get("search") : state.searchText;
            int searchTextColor = state.searchText.isEmpty() && !state.searchFocused ? COLOR_TEXT_MUTED : 0xFFFFFFFF;
            context.drawTextWithShadow(textRenderer, Text.literal(displaySearchText),
                    searchFieldX + 14, searchY + 5, searchTextColor);
            
            // Курсор
            if (state.searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = searchFieldX + 14 + textRenderer.getWidth(state.searchText);
                context.fill(cursorX, searchY + 4, cursorX + 1, searchY + 14, COLOR_GOLD);
            }
            
            // Кнопка сортировки
            int sortBtnX = contentX + contentWidth - 115;
            int sortBtnWidth = 110;
            boolean sortHovered = mouseX >= sortBtnX && mouseX < sortBtnX + sortBtnWidth &&
                    mouseY >= searchY && mouseY < searchY + searchFieldHeight;
            
            if (sortHovered) {
                context.fill(sortBtnX - 1, searchY - 1, sortBtnX + sortBtnWidth + 1, searchY + searchFieldHeight + 1,
                        (COLOR_BLUE & 0x00FFFFFF) | 0x30000000);
            }
            
            context.fill(sortBtnX, searchY, sortBtnX + sortBtnWidth, searchY + searchFieldHeight, 
                    sortHovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG);
            RenderUtils.drawBorder(context, sortBtnX, searchY, sortBtnWidth, searchFieldHeight, 
                    sortHovered ? COLOR_BLUE : COLOR_BORDER);
            
            if (sortHovered) {
                context.fill(sortBtnX + 1, searchY, sortBtnX + sortBtnWidth - 1, searchY + 1, COLOR_BLUE);
            }
            
            String sortText = lang.get(SORT_MODES[state.currentSortMode]);
            if (textRenderer.getWidth(sortText) > sortBtnWidth - 14) {
                sortText = sortText.substring(0, Math.min(sortText.length(), 10)) + "...";
            }
            sortText = sortText + " \u25BC";
            context.drawTextWithShadow(textRenderer, Text.literal(sortText),
                    sortBtnX + 6, searchY + 5, sortHovered ? 0xFFFFFFFF : COLOR_TEXT_TITLE);
            
            contentY += 24;
            contentHeight -= 24;
        }

        // Пустое состояние
        if (state.displayedListings == null || state.displayedListings.isEmpty()) {
            renderEmptyState(context, textRenderer, contentX, contentY, contentWidth, state.currentTab, lang);
            return;
        }

        // Список лотов
        int startIndex = state.currentPage * LISTINGS_PER_PAGE;
        int endIndex = Math.min(startIndex + LISTINGS_PER_PAGE, state.displayedListings.size());

        for (int i = startIndex; i < endIndex; i++) {
            MarketListing listing = state.displayedListings.get(i);
            int itemY = contentY + 6 + (i - startIndex) * ITEM_HEIGHT;
            int itemWidth = contentWidth - 12;
            int itemX = contentX + 6;

            boolean isHovered = mouseX >= itemX && mouseX < itemX + itemWidth &&
                    mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT - 4;

            renderListingItem(context, textRenderer, itemX, itemY, itemWidth, listing, isHovered, 
                    mouseX, mouseY, state.currentTab, getItemDisplayName, isWynnLineEmpty);
        }

        // Навигация
        int totalPages = (int) Math.ceil(state.displayedListings.size() / (double) LISTINGS_PER_PAGE);
        if (totalPages > 1) {
            renderPagination(context, textRenderer, contentX, contentY, contentWidth, contentHeight,
                    mouseX, mouseY, state.currentPage, totalPages);
        }
    }
    
    /**
     * Рендерит пустое состояние списка
     */
    private static void renderEmptyState(DrawContext context, TextRenderer textRenderer,
            int contentX, int contentY, int contentWidth, int currentTab, LocalizationManager lang) {
        String emptyText;
        String emptyIcon;
        if (currentTab == 0) {
            emptyText = lang.get("no_listings_available");
            emptyIcon = "\u2610";
        } else if (currentTab == 3) {
            emptyText = lang.get("no_favorites");
            emptyIcon = "\u2606";
        } else {
            emptyText = lang.get("no_listings");
            emptyIcon = "\u2610";
        }
        
        int emptyBoxWidth = Math.min(180, contentWidth - 40);
        int emptyBoxX = contentX + (contentWidth - emptyBoxWidth) / 2;
        int emptyBoxY = contentY + 30;
        int emptyBoxHeight = 60;
        
        context.fill(emptyBoxX, emptyBoxY, emptyBoxX + emptyBoxWidth, emptyBoxY + emptyBoxHeight, 0x20000000);
        RenderUtils.drawBorder(context, emptyBoxX, emptyBoxY, emptyBoxWidth, emptyBoxHeight, 0x30FFFFFF);
        
        int iconWidth = textRenderer.getWidth(emptyIcon);
        context.drawTextWithShadow(textRenderer, Text.literal(emptyIcon).styled(s -> s.withBold(true)),
                emptyBoxX + (emptyBoxWidth - iconWidth) / 2, emptyBoxY + 12, COLOR_TEXT_MUTED);
        
        int textWidth = textRenderer.getWidth(emptyText);
        context.drawTextWithShadow(textRenderer, Text.literal(emptyText),
                emptyBoxX + (emptyBoxWidth - textWidth) / 2, emptyBoxY + 35, COLOR_TEXT_MUTED);
    }
    
    /**
     * Рендерит один элемент списка
     */
    private static void renderListingItem(DrawContext context, TextRenderer textRenderer,
            int itemX, int itemY, int itemWidth, MarketListing listing, boolean isHovered,
            int mouseX, int mouseY, int currentTab,
            Function<MarketListing, String> getItemDisplayName,
            BiFunction<MarketListing, ItemStack, Boolean> isWynnLineEmpty) {
        
        int bgColor = isHovered ? COLOR_BG_ITEM_HOVER : COLOR_BG_PANEL;
        context.fill(itemX, itemY, itemX + itemWidth, itemY + ITEM_HEIGHT - 4, bgColor);
        
        context.fill(itemX, itemY + ITEM_HEIGHT - 5, itemX + itemWidth, itemY + ITEM_HEIGHT - 4, COLOR_BORDER);
        
        if (isHovered) {
            context.fill(itemX, itemY, itemX + 2, itemY + ITEM_HEIGHT - 4, COLOR_GOLD);
        }

        ItemStack stack = listing.getItemStack(MarketDataManager.getInstance().getRegistries());
        context.drawItem(stack, itemX + 8, itemY + 8);
        context.drawStackOverlay(textRenderer, stack, itemX + 8, itemY + 8);

        String displayName = listing.getItemDisplayName();
        if (displayName == null || displayName.isEmpty() || isWynnLineEmpty.apply(listing, stack)) {
            displayName = getItemDisplayName.apply(listing);
        }
        context.drawTextWithShadow(textRenderer, Text.literal(displayName),
                itemX + 30, itemY + 4, COLOR_TEXT_TITLE);
        
        String price = listing.getPrice();
        LocalizationManager lang = LocalizationManager.getInstance();
        if (price != null && !price.isEmpty()) {
            String priceLabel = lang.get("price", price);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(priceLabel),
                    itemX + 30, itemY + 14, COLOR_GREEN);
            int priceTextWidth = textRenderer.getWidth(priceLabel);
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE_EMERALD,
                    itemX + 30 + priceTextWidth, itemY + 10, 0, 0, 16, 16, 16, 16);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(lang.get("from_seller", listing.getSellerName())),
                    itemX + 30, itemY + 24, COLOR_TEXT_MUTED);
        } else {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(lang.get("from_seller", listing.getSellerName())),
                    itemX + 30, itemY + 17, COLOR_TEXT_MUTED);
        }

        // Кнопки справа
        renderListingButtons(context, textRenderer, itemX, itemY, itemWidth, listing, mouseX, mouseY, currentTab);
    }
    
    /**
     * Рендерит кнопки справа от лота
     */
    private static void renderListingButtons(DrawContext context, TextRenderer textRenderer,
            int itemX, int itemY, int itemWidth, MarketListing listing,
            int mouseX, int mouseY, int currentTab) {
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        if (currentTab == 0 || currentTab == 3) {
            // Кнопка избранного
            int favBtnX = itemX + itemWidth - 85;
            int favBtnY = itemY + 10;
            boolean isFavorite = MarketDataManager.getInstance().isFavorite(listing.getListingId());
            boolean favHovered = mouseX >= favBtnX && mouseX < favBtnX + 20 &&
                    mouseY >= favBtnY && mouseY < favBtnY + 16;
            
            int heartColor = isFavorite ? COLOR_RED : (favHovered ? 0xFFFF8888 : COLOR_TEXT_MUTED);
            String heartSymbol = isFavorite ? "\u2665" : "\u2661";
            context.drawTextWithShadow(textRenderer, Text.literal(heartSymbol),
                    favBtnX + 4, favBtnY + 2, heartColor);
            
            // Кнопка "Открыть"
            int btnX = itemX + itemWidth - 60;
            int btnY = itemY + 10;
            boolean btnHovered = mouseX >= btnX && mouseX < btnX + 55 &&
                    mouseY >= btnY && mouseY < btnY + 16;
            RenderUtils.drawButton(context, textRenderer, btnX, btnY, 55, 16, lang.get("open"), btnHovered, COLOR_BLUE);
        } else if (currentTab == 1) {
            // Кнопка "Ред."
            int editBtnX = itemX + itemWidth - 100;
            int editBtnY = itemY + 10;
            boolean editBtnHovered = mouseX >= editBtnX && mouseX < editBtnX + 40 &&
                    mouseY >= editBtnY && mouseY < editBtnY + 16;
            RenderUtils.drawButton(context, textRenderer, editBtnX, editBtnY, 40, 16, lang.get("edit"), editBtnHovered, COLOR_BLUE);
            
            // Кнопка "Снять"
            int btnX = itemX + itemWidth - 55;
            int btnY = itemY + 10;
            boolean btnHovered = mouseX >= btnX && mouseX < btnX + 50 &&
                    mouseY >= btnY && mouseY < btnY + 16;
            RenderUtils.drawButton(context, textRenderer, btnX, btnY, 50, 16, lang.get("delete"), btnHovered, COLOR_RED);
        }
    }
    
    /**
     * Рендерит пагинацию
     */
    private static void renderPagination(DrawContext context, TextRenderer textRenderer,
            int contentX, int contentY, int contentWidth, int contentHeight,
            int mouseX, int mouseY, int currentPage, int totalPages) {
        
        int navY = contentY + contentHeight - 22;
        
        int navBgWidth = 120;
        int navBgX = contentX + contentWidth / 2 - navBgWidth / 2;
        context.fill(navBgX, navY - 2, navBgX + navBgWidth, navY + 18, 0x30000000);
        
        if (currentPage > 0) {
            boolean leftHovered = mouseX >= contentX + 10 && mouseX < contentX + 32 &&
                    mouseY >= navY && mouseY < navY + 18;
            RenderUtils.drawButton(context, textRenderer, contentX + 10, navY, 22, 18, "\u25C0", leftHovered, COLOR_GOLD);
        }
        
        String pageInfo = (currentPage + 1) + " / " + totalPages;
        int pageWidth = textRenderer.getWidth(pageInfo);
        context.drawTextWithShadow(textRenderer, Text.literal(pageInfo),
                contentX + contentWidth / 2 - pageWidth / 2, navY + 5, COLOR_TEXT_TITLE);
        
        if (currentPage < totalPages - 1) {
            boolean rightHovered = mouseX >= contentX + contentWidth - 32 && mouseX < contentX + contentWidth - 10 &&
                    mouseY >= navY && mouseY < navY + 18;
            RenderUtils.drawButton(context, textRenderer, contentX + contentWidth - 32, navY, 22, 18, "\u25B6", rightHovered, COLOR_GOLD);
        }
    }
}
