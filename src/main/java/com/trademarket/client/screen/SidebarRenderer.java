package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Рендерер боковой панели навигации
 */
public class SidebarRenderer {
    
    /**
     * Состояние боковой панели
     */
    public static class SidebarState {
        public final int currentTab;
        public final boolean isAdmin;
        public final Map<UUID, Integer> listingUnreadCounts;
        
        public SidebarState(int currentTab, boolean isAdmin, Map<UUID, Integer> listingUnreadCounts) {
            this.currentTab = currentTab;
            this.isAdmin = isAdmin;
            this.listingUnreadCounts = listingUnreadCounts;
        }
    }
    
    /**
     * Результат hover проверки - возвращает индекс hovered вкладки
     */
    public static int drawSidebar(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int mouseX, int mouseY, SidebarState state) {
        
        LocalizationManager lang = LocalizationManager.getInstance();
        
        // Боковая панель слева
        int sidebarX = guiLeft;
        int sidebarY = guiTop + 45; // Ниже заголовка
        int sidebarHeight = GUI_HEIGHT - 55;
        
        // Фон боковой панели
        context.fill(sidebarX, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarHeight, COLOR_BG_DARK);
        // Правая граница
        context.fill(sidebarX + SIDEBAR_WIDTH - 1, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarHeight, COLOR_BORDER);
        
        // Определяем количество вкладок
        int tabCount = state.isAdmin ? 6 : 5;
        
        // Иконки для вкладок (Unicode символы)
        String[] tabIcons = {
            "\u2302",  // 0: All Listings - домик/магазин
            "\u2605",  // 1: My Listings - звезда
            "\u002B",  // 2: Sell - плюс
            "\u2665",  // 3: Favorites - сердце
            "\u2709",  // 4: Chats - конверт
            "\u263A"   // 5: Users - смайлик (админ)
        };
        
        int tabHeight = 44;
        int tabGap = 4;
        int startY = sidebarY + 10;
        
        int hoveredTab = -1;
        
        for (int i = 0; i < tabCount; i++) {
            int tabY = startY + i * (tabHeight + tabGap);
            int tabX = sidebarX + 4;
            int tabW = SIDEBAR_WIDTH - 8;
            
            boolean isActive = state.currentTab == i;
            boolean isHovered = mouseX >= tabX && mouseX < tabX + tabW &&
                    mouseY >= tabY && mouseY < tabY + tabHeight;
            
            if (isHovered) {
                hoveredTab = i;
            }
            
            // Фон вкладки
            if (isActive) {
                // Активная вкладка - с акцентным фоном
                context.fill(tabX, tabY, tabX + tabW, tabY + tabHeight, COLOR_BG_ITEM_HOVER);
                // Левая акцентная полоска
                context.fill(tabX, tabY + 4, tabX + 3, tabY + tabHeight - 4, COLOR_GOLD);
            } else if (isHovered) {
                // Hover эффект
                context.fill(tabX, tabY, tabX + tabW, tabY + tabHeight, COLOR_BG_ITEM);
            }
            
            // Иконка (центрируем)
            String icon = tabIcons[i];
            int iconW = textRenderer.getWidth(icon);
            int iconX = tabX + (tabW - iconW) / 2;
            int iconY = tabY + 10;
            int iconColor = isActive ? COLOR_GOLD : (isHovered ? COLOR_TEXT_TITLE : COLOR_TEXT_MUTED);
            
            // Рисуем иконку крупнее (с bold)
            context.drawTextWithShadow(textRenderer, 
                    Text.literal(icon).styled(s -> s.withBold(true)),
                    iconX, iconY, iconColor);
            
            // Короткое название под иконкой
            String shortName = getShortTabName(i, lang);
            int nameW = textRenderer.getWidth(shortName);
            int nameX = tabX + (tabW - nameW) / 2;
            int nameColor = isActive ? COLOR_TEXT_TITLE : (isHovered ? COLOR_TEXT_NORMAL : COLOR_TEXT_MUTED);
            context.drawTextWithShadow(textRenderer, Text.literal(shortName),
                    nameX, tabY + 26, nameColor);
            
            // Badge для непрочитанных сообщений (вкладка Chats - индекс 4)
            if (i == 4 && !state.listingUnreadCounts.isEmpty()) {
                int totalUnread = state.listingUnreadCounts.values().stream().mapToInt(Integer::intValue).sum();
                if (totalUnread > 0) {
                    int badgeSize = 12;
                    int badgeX = tabX + tabW - badgeSize - 2;
                    int badgeY = tabY + 4;
                    
                    context.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, COLOR_RED);
                    // Скругление
                    context.fill(badgeX, badgeY, badgeX + 1, badgeY + 1, isActive ? COLOR_BG_ITEM_HOVER : COLOR_BG_DARK);
                    context.fill(badgeX + badgeSize - 1, badgeY, badgeX + badgeSize, badgeY + 1, isActive ? COLOR_BG_ITEM_HOVER : COLOR_BG_DARK);
                    
                    String countStr = totalUnread > 9 ? "9+" : String.valueOf(totalUnread);
                    int countW = textRenderer.getWidth(countStr);
                    context.drawTextWithShadow(textRenderer, Text.literal(countStr),
                            badgeX + (badgeSize - countW) / 2, badgeY + 2, 0xFFFFFFFF);
                }
            }
        }
        
        return hoveredTab;
    }
    
    /**
     * Рисует tooltip для боковой панели
     */
    public static void drawSidebarTooltip(DrawContext context, TextRenderer textRenderer,
            int guiLeft, int guiTop, int hoveredSidebarTab, boolean isAdmin) {
        
        if (hoveredSidebarTab < 0) return;
        
        LocalizationManager lang = LocalizationManager.getInstance();
        int tabCount = isAdmin ? 6 : 5;
        
        if (hoveredSidebarTab >= tabCount) return;
        
        // Названия и описания вкладок
        String[] tabNames = {
            lang.get("tab_all_listings"),
            lang.get("tab_my_listings"),
            lang.get("tab_sell"),
            lang.get("tab_favorites"),
            lang.get("tab_messages"),
            lang.get("tab_users")
        };
        
        String[] tabDescriptions = {
            lang.isRussian() ? "Все доступные лоты" : "Browse all listings",
            lang.isRussian() ? "Ваши активные лоты" : "Your active listings",
            lang.isRussian() ? "Выставить на продажу" : "Create new listing",
            lang.isRussian() ? "Избранные лоты" : "Your saved items",
            lang.isRussian() ? "Сообщения и чаты" : "Messages and chats",
            lang.isRussian() ? "Онлайн пользователи" : "Online users"
        };
        
        String tooltipTitle = tabNames[hoveredSidebarTab];
        String tooltipDesc = tabDescriptions[hoveredSidebarTab];
        
        int sidebarY = guiTop + 45;
        int tabHeight = 44;
        int tabGap = 4;
        int startY = sidebarY + 10;
        
        int tooltipX = guiLeft + SIDEBAR_WIDTH + 5;
        int tooltipY = startY + hoveredSidebarTab * (tabHeight + tabGap) + 10;
        
        int titleW = textRenderer.getWidth(tooltipTitle);
        int descW = textRenderer.getWidth(tooltipDesc);
        int tooltipW = Math.max(titleW, descW) + 16;
        int tooltipH = 28;
        
        // Фон тултипа (с тенью для выделения)
        context.fill(tooltipX + 2, tooltipY + 2, tooltipX + tooltipW + 2, tooltipY + tooltipH + 2, 0x80000000);
        context.fill(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH, COLOR_BG_PANEL);
        context.fill(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + 1, COLOR_GOLD);
        RenderUtils.drawBorder(context, tooltipX, tooltipY, tooltipW, tooltipH, COLOR_BORDER);
        
        // Текст
        context.drawTextWithShadow(textRenderer, 
                Text.literal(tooltipTitle).styled(s -> s.withBold(true)),
                tooltipX + 8, tooltipY + 4, COLOR_TEXT_TITLE);
        context.drawTextWithShadow(textRenderer, Text.literal(tooltipDesc),
                tooltipX + 8, tooltipY + 15, COLOR_TEXT_MUTED);
    }
    
    /**
     * Короткие названия вкладок для боковой панели
     */
    private static String getShortTabName(int tabIndex, LocalizationManager lang) {
        switch (tabIndex) {
            case 0: return lang.isRussian() ? "Все" : "All";
            case 1: return lang.isRussian() ? "Мои" : "Mine";
            case 2: return lang.isRussian() ? "Продать" : "Sell";
            case 3: return lang.isRussian() ? "Избр." : "Favs";
            case 4: return lang.isRussian() ? "Чаты" : "Chats";
            case 5: return lang.isRussian() ? "Юзеры" : "Users";
            default: return "";
        }
    }
}
