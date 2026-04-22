package com.trademarket.client.screen;

import com.trademarket.TradeMarketMod;
import com.trademarket.client.LocalizationManager;
import com.trademarket.client.UpdateChecker;
import com.trademarket.data.SupabaseClient;
import com.trademarket.network.NetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

import static com.trademarket.client.screen.ScreenConstants.*;

/**
 * Экран проверки обновлений с анимациями
 */
public class UpdateScreen extends Screen {
    
    // Размеры окна
    private static final int WINDOW_WIDTH = 380;
    private static final int WINDOW_HEIGHT = 280;
    
    // Состояния экрана
    private enum State {
        CHECKING,       // Проверка обновлений
        UPDATE_FOUND,   // Найдено обновление
        UP_TO_DATE,     // Версия актуальна
        ERROR           // Ошибка проверки
    }
    
    private State currentState = State.CHECKING;
    private int guiLeft, guiTop;
    
    // Анимация
    private long startTime;
    private float spinnerAngle = 0;
    private int dotCount = 0;
    private long lastDotTime = 0;
    
    // Минимальное время показа анимации (чтобы пользователь успел увидеть)
    private static final long MIN_CHECKING_TIME = 1500; // 1.5 секунды
    private boolean canProceed = false;
    
    // Changelog scroll
    private int changelogScrollOffset = 0;
    private List<String> changelogLines = new ArrayList<>();
    private boolean changelogParsed = false;
    
    public UpdateScreen() {
        super(Text.literal("Update Check"));
        this.startTime = System.currentTimeMillis();
        
        // Запускаем проверку обновлений
        UpdateChecker.getInstance().reset();
        UpdateChecker.getInstance().checkForUpdates(
            updateInfo -> {
                // Выполняем в главном потоке Minecraft
                MinecraftClient.getInstance().execute(() -> {
                    if (updateInfo.isNewer) {
                        currentState = State.UPDATE_FOUND;
                        // parseChangelog вызовется в init() когда textRenderer будет готов
                    } else {
                        currentState = State.UP_TO_DATE;
                    }
                });
            },
            error -> {
                // Выполняем в главном потоке Minecraft
                MinecraftClient.getInstance().execute(() -> {
                    currentState = State.ERROR;
                });
            }
        );
    }
    
    private void parseChangelog(String changelog) {
        changelogLines.clear();
        if (changelog == null || changelog.isEmpty()) {
            return;
        }
        
        // Разбиваем на строки и оборачиваем длинные
        String[] lines = changelog.split("\n");
        int maxWidth = WINDOW_WIDTH - 40;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                changelogLines.add("");
                continue;
            }
            
            // Простой перенос строк
            while (textRenderer != null && textRenderer.getWidth(line) > maxWidth && line.length() > 10) {
                int splitIndex = line.length() / 2;
                // Ищем пробел для переноса
                int spaceIndex = line.lastIndexOf(' ', splitIndex);
                if (spaceIndex > 10) {
                    splitIndex = spaceIndex;
                }
                changelogLines.add(line.substring(0, splitIndex));
                line = line.substring(splitIndex).trim();
            }
            changelogLines.add(line);
        }
    }
    
    @Override
    protected void init() {
        this.guiLeft = (this.width - WINDOW_WIDTH) / 2;
        this.guiTop = (this.height - WINDOW_HEIGHT) / 2;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнение фона
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // Обновляем анимации
        updateAnimations();
        
        // Проверяем минимальное время показа
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= MIN_CHECKING_TIME && UpdateChecker.getInstance().isCheckComplete()) {
            canProceed = true;
            
            // Парсим changelog только один раз и только в render thread
            if (!changelogParsed && currentState == State.UPDATE_FOUND) {
                UpdateChecker.UpdateInfo update = UpdateChecker.getInstance().getLatestUpdate();
                if (update != null) {
                    parseChangelog(update.changelog);
                    changelogParsed = true;
                }
            }
        }
        
        // Основная панель
        drawMainPanel(context, mouseX, mouseY);
        
        // Контент в зависимости от состояния
        switch (currentState) {
            case CHECKING -> drawCheckingState(context, mouseX, mouseY);
            case UPDATE_FOUND -> drawUpdateFoundState(context, mouseX, mouseY);
            case UP_TO_DATE -> drawUpToDateState(context, mouseX, mouseY);
            case ERROR -> drawErrorState(context, mouseX, mouseY);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void updateAnimations() {
        // Спиннер
        spinnerAngle += 5f;
        if (spinnerAngle >= 360) spinnerAngle -= 360;
        
        // Анимация точек
        long now = System.currentTimeMillis();
        if (now - lastDotTime > 400) {
            dotCount = (dotCount + 1) % 4;
            lastDotTime = now;
        }
    }
    
    private void drawMainPanel(DrawContext context, int mouseX, int mouseY) {
        // Тень
        context.fill(guiLeft + 4, guiTop + 4, guiLeft + WINDOW_WIDTH + 4, guiTop + WINDOW_HEIGHT + 4, 0x60000000);
        
        // Основной фон
        context.fill(guiLeft, guiTop, guiLeft + WINDOW_WIDTH, guiTop + WINDOW_HEIGHT, COLOR_BG_DARK);
        
        // Верхняя акцентная линия
        int accentColor = switch (currentState) {
            case CHECKING -> COLOR_BLUE;
            case UPDATE_FOUND -> COLOR_GOLD;
            case UP_TO_DATE -> COLOR_GREEN;
            case ERROR -> COLOR_RED;
        };
        context.fill(guiLeft, guiTop, guiLeft + WINDOW_WIDTH, guiTop + 3, accentColor);
        
        // Рамка
        RenderUtils.drawBorder(context, guiLeft, guiTop, WINDOW_WIDTH, WINDOW_HEIGHT, COLOR_BORDER);
        
        // Заголовок
        String title = "Trade Market";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawTextWithShadow(this.textRenderer, Text.literal(title).styled(s -> s.withBold(true)),
                guiLeft + (WINDOW_WIDTH - titleWidth) / 2, guiTop + 12, COLOR_GOLD);
        
        // Версия
        String version = "v" + TradeMarketMod.MOD_VERSION;
        int versionWidth = this.textRenderer.getWidth(version);
        context.drawTextWithShadow(this.textRenderer, Text.literal(version),
                guiLeft + (WINDOW_WIDTH - versionWidth) / 2, guiTop + 24, COLOR_TEXT_MUTED);
    }
    
    private void drawCheckingState(DrawContext context, int mouseX, int mouseY) {
        int centerX = guiLeft + WINDOW_WIDTH / 2;
        int centerY = guiTop + 100;
        
        // Анимированный спиннер
        drawSpinner(context, centerX, centerY, 20);
        
        // Текст с анимированными точками
        String dots = ".".repeat(dotCount);
        String checkingText = LocalizationManager.getInstance().get("update_checking") + dots;
        int textWidth = this.textRenderer.getWidth(checkingText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(checkingText),
                centerX - textWidth / 2, centerY + 35, COLOR_TEXT_NORMAL);
        
        // Подсказка
        String hint = LocalizationManager.getInstance().get("update_please_wait");
        int hintWidth = this.textRenderer.getWidth(hint);
        context.drawTextWithShadow(this.textRenderer, Text.literal(hint),
                centerX - hintWidth / 2, centerY + 50, COLOR_TEXT_MUTED);
    }
    
    private void drawSpinner(DrawContext context, int x, int y, int radius) {
        // Рисуем несколько точек по кругу с разной прозрачностью
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            double angle = Math.toRadians(spinnerAngle + i * (360.0 / segments));
            int dotX = x + (int)(Math.cos(angle) * radius);
            int dotY = y + (int)(Math.sin(angle) * radius);
            
            // Прозрачность уменьшается от головы к хвосту
            int alpha = 255 - (i * 30);
            if (alpha < 50) alpha = 50;
            int color = (alpha << 24) | (COLOR_GOLD & 0x00FFFFFF);
            
            int dotSize = 3 - (i / 3);
            if (dotSize < 1) dotSize = 1;
            
            context.fill(dotX - dotSize, dotY - dotSize, dotX + dotSize, dotY + dotSize, color);
        }
    }
    
    private void drawUpdateFoundState(DrawContext context, int mouseX, int mouseY) {
        UpdateChecker.UpdateInfo update = UpdateChecker.getInstance().getLatestUpdate();
        if (update == null) return;
        
        int centerX = guiLeft + WINDOW_WIDTH / 2;
        
        // Иконка обновления (стрелка вверх)
        String icon = "\u2B06"; // Unicode стрелка вверх
        int iconWidth = this.textRenderer.getWidth(icon);
        context.drawTextWithShadow(this.textRenderer, Text.literal(icon).styled(s -> s.withBold(true)),
                centerX - iconWidth / 2, guiTop + 50, COLOR_GOLD);
        
        // Заголовок
        String foundText = LocalizationManager.getInstance().get("update_available");
        int foundWidth = this.textRenderer.getWidth(foundText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(foundText).styled(s -> s.withBold(true)),
                centerX - foundWidth / 2, guiTop + 65, COLOR_GOLD);
        
        // Версии
        String versionText = TradeMarketMod.MOD_VERSION + " \u2192 " + update.version;
        int versionWidth = this.textRenderer.getWidth(versionText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(versionText),
                centerX - versionWidth / 2, guiTop + 80, COLOR_TEXT_NORMAL);
        
        // Changelog область
        int changelogY = guiTop + 100;
        int changelogHeight = 110;
        
        // Фон для changelog
        context.fill(guiLeft + 15, changelogY, guiLeft + WINDOW_WIDTH - 15, changelogY + changelogHeight, COLOR_BG_PANEL);
        RenderUtils.drawBorder(context, guiLeft + 15, changelogY, WINDOW_WIDTH - 30, changelogHeight, COLOR_BORDER);
        
        // Заголовок changelog
        String changelogTitle = LocalizationManager.getInstance().get("update_changelog");
        context.drawTextWithShadow(this.textRenderer, Text.literal(changelogTitle).styled(s -> s.withBold(true)),
                guiLeft + 20, changelogY + 5, COLOR_TEXT_TITLE);
        
        // Скроллируемый changelog
        int lineY = changelogY + 18;
        int maxLines = (changelogHeight - 25) / 10;
        
        for (int i = changelogScrollOffset; i < Math.min(changelogLines.size(), changelogScrollOffset + maxLines); i++) {
            String line = changelogLines.get(i);
            int lineColor = line.startsWith("-") || line.startsWith("*") ? COLOR_TEXT_NORMAL : COLOR_TEXT_MUTED;
            if (line.startsWith("#")) {
                line = line.replaceFirst("#+\\s*", "");
                lineColor = COLOR_GOLD;
            }
            context.drawTextWithShadow(this.textRenderer, Text.literal(line),
                    guiLeft + 20, lineY, lineColor);
            lineY += 10;
        }
        
        // Скроллбар если нужен
        if (changelogLines.size() > maxLines) {
            int scrollBarX = guiLeft + WINDOW_WIDTH - 22;
            int scrollBarHeight = changelogHeight - 20;
            int thumbHeight = Math.max(15, scrollBarHeight * maxLines / changelogLines.size());
            int thumbY = changelogY + 15 + (scrollBarHeight - thumbHeight) * changelogScrollOffset / Math.max(1, changelogLines.size() - maxLines);
            
            context.fill(scrollBarX, changelogY + 15, scrollBarX + 4, changelogY + changelogHeight - 5, COLOR_BG_DARK);
            context.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, COLOR_GOLD_DARK);
        }
        
        // Кнопки
        drawButtons(context, mouseX, mouseY, true);
    }
    
    private void drawUpToDateState(DrawContext context, int mouseX, int mouseY) {
        int centerX = guiLeft + WINDOW_WIDTH / 2;
        int centerY = guiTop + 100;
        
        // Галочка
        String checkmark = "\u2714"; // Unicode галочка
        int checkWidth = this.textRenderer.getWidth(checkmark);
        context.drawTextWithShadow(this.textRenderer, Text.literal(checkmark).styled(s -> s.withBold(true)),
                centerX - checkWidth / 2, centerY - 10, COLOR_GREEN);
        
        // Текст
        String upToDateText = LocalizationManager.getInstance().get("update_up_to_date");
        int textWidth = this.textRenderer.getWidth(upToDateText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(upToDateText).styled(s -> s.withBold(true)),
                centerX - textWidth / 2, centerY + 15, COLOR_GREEN);
        
        // Версия
        String versionText = LocalizationManager.getInstance().get("update_current_version", TradeMarketMod.MOD_VERSION);
        int versionWidth = this.textRenderer.getWidth(versionText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(versionText),
                centerX - versionWidth / 2, centerY + 35, COLOR_TEXT_MUTED);
        
        // Кнопка продолжить
        drawButtons(context, mouseX, mouseY, false);
    }
    
    private void drawErrorState(DrawContext context, int mouseX, int mouseY) {
        int centerX = guiLeft + WINDOW_WIDTH / 2;
        int centerY = guiTop + 100;
        
        // Иконка ошибки
        String errorIcon = "\u26A0"; // Unicode warning
        int iconWidth = this.textRenderer.getWidth(errorIcon);
        context.drawTextWithShadow(this.textRenderer, Text.literal(errorIcon).styled(s -> s.withBold(true)),
                centerX - iconWidth / 2, centerY - 10, COLOR_RED);
        
        // Текст
        String errorText = LocalizationManager.getInstance().get("update_check_failed");
        int textWidth = this.textRenderer.getWidth(errorText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(errorText),
                centerX - textWidth / 2, centerY + 15, COLOR_RED);
        
        // Подсказка
        String hint = LocalizationManager.getInstance().get("update_continue_anyway");
        int hintWidth = this.textRenderer.getWidth(hint);
        context.drawTextWithShadow(this.textRenderer, Text.literal(hint),
                centerX - hintWidth / 2, centerY + 35, COLOR_TEXT_MUTED);
        
        // Кнопка продолжить
        drawButtons(context, mouseX, mouseY, false);
    }
    
    private void drawButtons(DrawContext context, int mouseX, int mouseY, boolean showDownload) {
        int btnY = guiTop + WINDOW_HEIGHT - 40;
        int btnHeight = 24;
        
        if (showDownload) {
            // Две кнопки: Скачать и Продолжить
            int btnWidth = 140;
            int gap = 20;
            int totalWidth = btnWidth * 2 + gap;
            int startX = guiLeft + (WINDOW_WIDTH - totalWidth) / 2;
            
            // Кнопка "Скачать"
            boolean downloadHovered = mouseX >= startX && mouseX < startX + btnWidth &&
                    mouseY >= btnY && mouseY < btnY + btnHeight;
            
            int downloadBg = downloadHovered ? 0xFF2E7D32 : COLOR_GREEN;
            context.fill(startX, btnY, startX + btnWidth, btnY + btnHeight, downloadBg);
            RenderUtils.drawBorder(context, startX, btnY, btnWidth, btnHeight, 0xFF1B5E20);
            
            String downloadText = LocalizationManager.getInstance().get("update_download");
            int downloadTextW = this.textRenderer.getWidth(downloadText);
            context.drawTextWithShadow(this.textRenderer, Text.literal(downloadText),
                    startX + (btnWidth - downloadTextW) / 2, btnY + 8, 0xFFFFFFFF);
            
            // Кнопка "Продолжить"
            int continueX = startX + btnWidth + gap;
            boolean continueHovered = mouseX >= continueX && mouseX < continueX + btnWidth &&
                    mouseY >= btnY && mouseY < btnY + btnHeight;
            
            int continueBg = continueHovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
            context.fill(continueX, btnY, continueX + btnWidth, btnY + btnHeight, continueBg);
            RenderUtils.drawBorder(context, continueX, btnY, btnWidth, btnHeight, COLOR_BORDER);
            
            String continueText = LocalizationManager.getInstance().get("update_continue");
            int continueTextW = this.textRenderer.getWidth(continueText);
            context.drawTextWithShadow(this.textRenderer, Text.literal(continueText),
                    continueX + (btnWidth - continueTextW) / 2, btnY + 8, COLOR_TEXT_NORMAL);
            
        } else {
            // Одна кнопка по центру
            int btnWidth = 160;
            int btnX = guiLeft + (WINDOW_WIDTH - btnWidth) / 2;
            
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnWidth &&
                    mouseY >= btnY && mouseY < btnY + btnHeight;
            
            int btnBg = hovered ? 0xFF2E7D32 : COLOR_GREEN;
            context.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnBg);
            RenderUtils.drawBorder(context, btnX, btnY, btnWidth, btnHeight, 0xFF1B5E20);
            
            String text = LocalizationManager.getInstance().get("update_continue");
            int textW = this.textRenderer.getWidth(text);
            context.drawTextWithShadow(this.textRenderer, Text.literal(text),
                    btnX + (btnWidth - textW) / 2, btnY + 8, 0xFFFFFFFF);
        }
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x();
        int my = (int) click.y();
        
        int btnY = guiTop + WINDOW_HEIGHT - 40;
        int btnHeight = 24;
        
        if (currentState == State.UPDATE_FOUND) {
            // Две кнопки
            int btnWidth = 140;
            int gap = 20;
            int totalWidth = btnWidth * 2 + gap;
            int startX = guiLeft + (WINDOW_WIDTH - totalWidth) / 2;
            
            // Скачать
            if (mx >= startX && mx < startX + btnWidth &&
                    my >= btnY && my < btnY + btnHeight) {
                
                UpdateChecker.UpdateInfo update = UpdateChecker.getInstance().getLatestUpdate();
                if (update != null && !update.downloadUrl.isEmpty()) {
                    Util.getOperatingSystem().open(update.downloadUrl);
                }
                return true;
            }
            
            // Продолжить
            int continueX = startX + btnWidth + gap;
            if (mx >= continueX && mx < continueX + btnWidth &&
                    my >= btnY && my < btnY + btnHeight) {
                openMainMenu();
                return true;
            }
            
        } else if (currentState == State.UP_TO_DATE || currentState == State.ERROR) {
            // Одна кнопка
            int btnWidth = 160;
            int btnX = guiLeft + (WINDOW_WIDTH - btnWidth) / 2;
            
            if (mx >= btnX && mx < btnX + btnWidth &&
                    my >= btnY && my < btnY + btnHeight) {
                openMainMenu();
                return true;
            }
        }
        
        return super.mouseClicked(click, bl);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentState == State.UPDATE_FOUND && !changelogLines.isEmpty()) {
            int changelogY = guiTop + 100;
            int changelogHeight = 110;
            
            if (mouseX >= guiLeft + 15 && mouseX < guiLeft + WINDOW_WIDTH - 15 &&
                    mouseY >= changelogY && mouseY < changelogY + changelogHeight) {
                
                int maxLines = (changelogHeight - 25) / 10;
                int maxScroll = Math.max(0, changelogLines.size() - maxLines);
                
                changelogScrollOffset -= (int) verticalAmount;
                changelogScrollOffset = Math.max(0, Math.min(maxScroll, changelogScrollOffset));
                
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        // ESC или Enter - продолжить (если проверка завершена)
        if ((keyCode == 256 || keyCode == 257) && canProceed && 
                (currentState == State.UP_TO_DATE || currentState == State.ERROR || currentState == State.UPDATE_FOUND)) {
            openMainMenu();
            return true;
        }
        
        return super.keyPressed(keyInput);
    }
    
    private void openMainMenu() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Устанавливаем данные игрока
            SupabaseClient.getInstance().setCurrentPlayer(
                    client.player.getUuidAsString(),
                    client.player.getName().getString()
            );
            
            NetworkHandler.fetchAllListings();
            client.setScreen(new TradeMarketScreen());
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
