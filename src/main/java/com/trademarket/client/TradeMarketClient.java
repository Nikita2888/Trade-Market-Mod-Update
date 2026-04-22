package com.trademarket.client;

import com.trademarket.TradeMarketMod;
import com.trademarket.client.screen.TradeMarketScreen;
import com.trademarket.client.screen.UpdateScreen;
import com.trademarket.data.SupabaseClient;
import com.trademarket.network.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class TradeMarketClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(TradeMarketMod.MOD_ID + "_client");
    private static KeyBinding openMarketKey;
    
    // Кастомная категория для клавиш мода (1.21.11 API)
    private static final KeyBinding.Category TRADEMARKET_CATEGORY = 
            new KeyBinding.Category(Identifier.of(TradeMarketMod.MOD_ID, "keys"));
    
    // Флаг для отслеживания показа приветственного уведомления
    private static boolean welcomeNotificationShown = false;
    private static int spawnCheckTicks = 0;
    private static final int SPAWN_DELAY_TICKS = 60; // 3 секунды задержки после спавна
    
    // Heartbeat для отслеживания онлайн статуса
    private static long lastUserHeartbeat = 0;
    private static final long USER_HEARTBEAT_INTERVAL = 60000; // 1 минута

    @Override
    public void onInitializeClient() {
        LOGGER.info("[TradeMarket] Инициализация клиентской части мода...");

        // Регистрируем клавишу F8 для открытия рынка (1.21.11 API)
        openMarketKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.trademarket.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                TRADEMARKET_CATEGORY
        ));
        
        LOGGER.info("[TradeMarket] Клавиша F8 зарегистрирована для открытия рынка");

        // Регистрируем клиентские пакеты
        NetworkHandler.registerClientPackets();
        LOGGER.info("[TradeMarket] Сетевые пакеты клиента зарегистрированы");

        // Обработка нажатия клавиш
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // F8 - Открыть рынок (сначала экран обновлений)
            if (openMarketKey.wasPressed()) {
                LOGGER.info("[TradeMarket] Нажата клавиша F8!");
                
                if (client.player != null && client.currentScreen == null) {
                    try {
                        // Устанавливаем данные игрока перед каждым запросом
                        SupabaseClient.getInstance().setCurrentPlayer(
                            client.player.getUuidAsString(),
                            client.player.getName().getString()
                        );
                        
                        // Открываем экран проверки обновлений
                        // Он сам откроет основной экран после проверки
                        client.setScreen(new UpdateScreen());
                        
                        // Логируем открытие меню
                        RemoteLogger.getInstance().logMenuOpen();
                        
                        LOGGER.info("[TradeMarket] Экран обновлений открыт!");
                    } catch (Exception e) {
                        LOGGER.error("[TradeMarket] Ошибка при открытии экрана: ", e);
                        RemoteLogger.getInstance().error(
                            RemoteLogger.Category.ERROR, 
                            "MENU_OPEN_ERROR", 
                            "Ошибка при открытии меню", 
                            e
                        );
                    }
                }
            }
            
            // Обновляем анимацию тостов каждый тик
            ToastNotificationManager.getInstance().tick();
            
            // Фоновая проверка уведомлений (когда GUI закрыт)
            if (client.currentScreen == null) {
                NotificationChecker.getInstance().tick();
            }
            
            // Проверка показа приветственного уведомления при входе на сервер
            checkWelcomeNotification(client);
            
            // Периодический heartbeat для онлайн статуса (каждую минуту)
            if (welcomeNotificationShown && client.player != null) {
                String serverIP = "singleplayer";
                if (client.getCurrentServerEntry() != null) {
                    serverIP = client.getCurrentServerEntry().address;
                }
                sendUserHeartbeat(
                    client.player.getUuidAsString(),
                    client.player.getName().getString(),
                    serverIP
                );
            }
        });
        
        // Регистрируем рендеринг тостов поверх HUD
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                int screenWidth = mc.getWindow().getScaledWidth();
                int screenHeight = mc.getWindow().getScaledHeight();
                ToastNotificationManager.getInstance().render(drawContext, screenWidth, screenHeight);
            }
        });

        LOGGER.info("[TradeMarket] Клиентская инициализация завершена!");
    }
    
    /**
     * Проверяет и показывает приветственное уведомление при входе на сервер
     */
    private void checkWelcomeNotification(net.minecraft.client.MinecraftClient client) {
        // Если игрок не в игре - сбрасываем флаги и данные
        if (client.player == null || client.world == null) {
            if (welcomeNotificationShown) {
                // Игрок вышел из игры - сбрасываем данные
                SupabaseClient.getInstance().clearCurrentPlayer();
                NotificationChecker.getInstance().fullReset();
            }
            welcomeNotificationShown = false;
            spawnCheckTicks = 0;
            return;
        }
        
        // Если уведомление уже показано - выходим
        if (welcomeNotificationShown) {
            return;
        }
        
        // Проверяем что игрок полностью загрузился:
        // - Игрок существует
        // - Мир загружен
        // - Игрок не в экране загрузки
        // - Здоровье игрока > 0 (игрок жив и заспавнился)
        // - Позиция игрока валидная (не 0,0,0)
        boolean isFullySpawned = client.player.getHealth() > 0 
                && client.player.getY() != 0
                && client.currentScreen == null
                && !client.player.isDead();
        
        if (isFullySpawned) {
            spawnCheckTicks++;
            
            // Ждем несколько секунд после спавна для стабильности
            if (spawnCheckTicks >= SPAWN_DELAY_TICKS) {
                // Устанавливаем данные игрока для API запросов
                String playerUUID = client.player.getUuidAsString();
                String playerName = client.player.getName().getString();
                
                SupabaseClient.getInstance().setCurrentPlayer(playerUUID, playerName);
                
                // Инициализируем удалённое логирование
                RemoteLogger.getInstance().setPlayerInfo(playerUUID, playerName);
                
                // Получаем IP сервера
                String serverIP = "singleplayer";
                if (client.getCurrentServerEntry() != null) {
                    serverIP = client.getCurrentServerEntry().address;
                }
                RemoteLogger.getInstance().setServerIP(serverIP);
                RemoteLogger.getInstance().logServerJoin(serverIP);
                
                // Отправляем первый heartbeat для регистрации в списке онлайн пользователей
                sendUserHeartbeat(playerUUID, playerName, serverIP);
                
                showWelcomeNotification();
                welcomeNotificationShown = true;
            }
        } else {
            // Сбрасываем счетчик если условия не выполнены
            spawnCheckTicks = 0;
        }
    }
    
    /**
     * Показывает приветственное уведомление о том как открыть меню
     */
    private void showWelcomeNotification() {
        LocalizationManager lang = LocalizationManager.getInstance();
        ToastNotificationManager.getInstance().showWelcome(
            lang.get("toast_welcome_title"),
            lang.get("toast_welcome_key")
        );
        LOGGER.info("[TradeMarket] Показано приветственное уведомление");
    }
    
    /**
     * Отправляет heartbeat для регистрации онлайн статуса
     */
    private void sendUserHeartbeat(String playerUUID, String playerName, String serverIP) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUserHeartbeat < USER_HEARTBEAT_INTERVAL) {
            return; // Слишком рано для следующего heartbeat
        }
        
        lastUserHeartbeat = currentTime;
        SupabaseClient.getInstance().sendUserHeartbeat(
            playerUUID,
            playerName,
            serverIP,
            TradeMarketMod.MOD_VERSION,
            () -> LOGGER.debug("[TradeMarket] User heartbeat sent successfully"),
            error -> LOGGER.debug("[TradeMarket] Failed to send user heartbeat: " + error)
        );
    }
    
    /**
     * Сбрасывает флаг приветственного уведомления (вызывать при выходе с сервера)
     */
    public static void resetWelcomeNotification() {
        welcomeNotificationShown = false;
        spawnCheckTicks = 0;
        lastUserHeartbeat = 0;
        
        // Логируем выход с сервера
        RemoteLogger.getInstance().logServerLeave();
        
        // Сбрасываем данные игрока и уведомлений
        SupabaseClient.getInstance().clearCurrentPlayer();
        NotificationChecker.getInstance().fullReset();
        RemoteLogger.getInstance().reset();
    }
}
