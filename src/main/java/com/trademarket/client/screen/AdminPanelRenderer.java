package com.trademarket.client.screen;

import com.trademarket.client.LocalizationManager;
import com.trademarket.data.MarketListing;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Рендерер для админ-панели в деталях лота
 */
public class AdminPanelRenderer {
    
    /**
     * Информация об администраторе
     */
    public static class AdminInfo {
        public final boolean isAdmin;
        public final String role;
        private final boolean canDeleteListings;
        
        public AdminInfo(boolean isAdmin, String role, boolean canDeleteListings) {
            this.isAdmin = isAdmin;
            this.role = role;
            this.canDeleteListings = canDeleteListings;
        }
        
        public boolean canDeleteListings() {
            return canDeleteListings;
        }
    }
    
    /**
     * Состояние админ-панели
     */
    public static class AdminPanelState {
        public final boolean showAdminPanel;
        public final int adminPanelOffsetX;
        public final int adminPanelOffsetY;
        public final boolean isDraggingAdminPanel;
        public final int screenWidth;
        public final int screenHeight;
        
        public AdminPanelState(boolean showAdminPanel, int adminPanelOffsetX, int adminPanelOffsetY,
                              boolean isDraggingAdminPanel, int screenWidth, int screenHeight) {
            this.showAdminPanel = showAdminPanel;
            this.adminPanelOffsetX = adminPanelOffsetX;
            this.adminPanelOffsetY = adminPanelOffsetY;
            this.isDraggingAdminPanel = isDraggingAdminPanel;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }
    }
    
    /**
     * Рисует админ-панель в деталях лота
     */
    public static void drawAdminPanel(DrawContext context, TextRenderer textRenderer,
            int detailsGuiLeft, int guiTop, int width, int height, int mouseX, int mouseY,
            AdminPanelState state, AdminInfo adminInfo, MarketListing selectedListing) {
        
        // Кнопка админ-панели (справа внизу)
        int adminBtnX = detailsGuiLeft + width - 100;
        int adminBtnY = guiTop + height - 30;
        int adminBtnWidth = 85;
        int adminBtnHeight = 20;
        
        boolean adminBtnHovered = mouseX >= adminBtnX && mouseX < adminBtnX + adminBtnWidth &&
                mouseY >= adminBtnY && mouseY < adminBtnY + adminBtnHeight;
        
        int adminBtnColor = state.showAdminPanel ? COLOR_RED : 0xFF8B0000;
        RenderUtils.drawButton(context, textRenderer, adminBtnX, adminBtnY, adminBtnWidth, adminBtnHeight, 
                state.showAdminPanel ? LocalizationManager.getInstance().get("close") : LocalizationManager.getInstance().get("admin"), 
                adminBtnHovered, adminBtnColor);
        
        // Индикатор роли админа
        String roleText = "[" + adminInfo.role.toUpperCase() + "]";
        context.drawTextWithShadow(textRenderer, Text.literal(roleText),
                adminBtnX - textRenderer.getWidth(roleText) - 5, adminBtnY + 6, COLOR_RED);
        
        // Если панель открыта - показываем опции
        if (state.showAdminPanel && selectedListing != null) {
            drawAdminPanelContent(context, textRenderer, detailsGuiLeft, guiTop, width, mouseX, mouseY,
                    state, adminInfo, selectedListing);
        }
    }
    
    /**
     * Рисует содержимое админ-панели
     */
    private static void drawAdminPanelContent(DrawContext context, TextRenderer textRenderer,
            int detailsGuiLeft, int guiTop, int width, int mouseX, int mouseY,
            AdminPanelState state, AdminInfo adminInfo, MarketListing selectedListing) {
        
        int basePanelX = detailsGuiLeft + width - 180;
        int basePanelY = guiTop + 50;
        int panelWidth = 170;
        int panelHeight = 200;
        
        // Применяем смещение от перетаскивания
        int panelX = basePanelX + state.adminPanelOffsetX;
        int panelY = basePanelY + state.adminPanelOffsetY;
        
        // Ограничиваем позицию панели в пределах экрана
        panelX = Math.max(10, Math.min(state.screenWidth - panelWidth - 10, panelX));
        panelY = Math.max(10, Math.min(state.screenHeight - panelHeight - 10, panelY));
        
        // Фон панели
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEE1A1010);
        RenderUtils.drawBorder(context, panelX, panelY, panelWidth, panelHeight, COLOR_RED);
        
        // Заголовок (область для перетаскивания)
        int headerHeight = 22;
        boolean headerHovered = mouseX >= panelX && mouseX < panelX + panelWidth &&
                mouseY >= panelY && mouseY < panelY + headerHeight;
        int headerBg = headerHovered || state.isDraggingAdminPanel ? 0xFF2A1515 : 0x00000000;
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + headerHeight, headerBg);
        
        // Иконка перетаскивания
        int dragIconX = panelX + panelWidth - 20;
        int dragIconY = panelY + 7;
        for (int i = 0; i < 3; i++) {
            context.fill(dragIconX, dragIconY + i * 3, dragIconX + 12, dragIconY + i * 3 + 1, 
                    headerHovered ? COLOR_RED : COLOR_TEXT_MUTED);
        }
        
        context.drawTextWithShadow(textRenderer, 
                Text.literal(LocalizationManager.getInstance().get("admin_panel")).styled(s -> s.withBold(true)),
                panelX + 10, panelY + 8, COLOR_RED);
        
        String sellerName = selectedListing.getSellerName();
        context.drawTextWithShadow(textRenderer, 
                Text.literal(LocalizationManager.getInstance().get("player", sellerName)),
                panelX + 10, panelY + 22, COLOR_TEXT_NORMAL);
        
        int btnX = panelX + 10;
        int btnWidth = panelWidth - 20;
        int btnY = panelY + 40;
        int btnH = 18;
        
        // Кнопка удалить лот
        if (adminInfo.canDeleteListings()) {
            boolean delHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                    mouseY >= btnY && mouseY < btnY + btnH;
            RenderUtils.drawButton(context, textRenderer, btnX, btnY, btnWidth, btnH, 
                    LocalizationManager.getInstance().get("delete_listing"), delHovered, COLOR_RED);
            btnY += btnH + 5;
        }
        
        // Разделитель
        context.fill(panelX + 5, btnY, panelX + panelWidth - 5, btnY + 1, COLOR_BORDER);
        btnY += 8;
        
        // Заголовок блокировок
        context.drawTextWithShadow(textRenderer, 
                Text.literal(LocalizationManager.getInstance().get("blocks")),
                panelX + 10, btnY, COLOR_GOLD);
        btnY += 14;
        
        // Кнопки блокировок
        LocalizationManager l = LocalizationManager.getInstance();
        String[] banLabels = {l.get("ban_listing"), l.get("ban_buying"), l.get("ban_chat"), l.get("ban_tickets"), l.get("ban_full")};
        
        for (int i = 0; i < banLabels.length; i++) {
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                    mouseY >= btnY && mouseY < btnY + btnH;
            int btnColor = i == 4 ? 0xFF8B0000 : 0xFF5A3030;
            RenderUtils.drawButton(context, textRenderer, btnX, btnY, btnWidth, btnH, banLabels[i], hovered, btnColor);
            btnY += btnH + 3;
        }
        
        // Кнопка разблокировки
        btnY += 5;
        boolean unbanHovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                mouseY >= btnY && mouseY < btnY + btnH;
        RenderUtils.drawButton(context, textRenderer, btnX, btnY, btnWidth, btnH, 
                LocalizationManager.getInstance().get("remove_all_bans"), unbanHovered, COLOR_GREEN);
    }
    
    /**
     * Вычисляет позицию админ-панели с учетом смещения и ограничений экрана
     */
    public static int[] calculatePanelPosition(int detailsGuiLeft, int guiTop, int width,
            int adminPanelOffsetX, int adminPanelOffsetY, int screenWidth, int screenHeight) {
        int basePanelX = detailsGuiLeft + width - 180;
        int basePanelY = guiTop + 50;
        int panelWidth = 170;
        int panelHeight = 200;
        
        int panelX = basePanelX + adminPanelOffsetX;
        int panelY = basePanelY + adminPanelOffsetY;
        
        panelX = Math.max(10, Math.min(screenWidth - panelWidth - 10, panelX));
        panelY = Math.max(10, Math.min(screenHeight - panelHeight - 10, panelY));
        
        return new int[]{panelX, panelY, panelWidth, panelHeight};
    }
    
    /**
     * Вычисляет позицию кнопки админ-панели
     */
    public static int[] getAdminButtonBounds(int detailsGuiLeft, int guiTop, int width, int height) {
        int adminBtnX = detailsGuiLeft + width - 100;
        int adminBtnY = guiTop + height - 30;
        int adminBtnWidth = 85;
        int adminBtnHeight = 20;
        return new int[]{adminBtnX, adminBtnY, adminBtnWidth, adminBtnHeight};
    }
}
