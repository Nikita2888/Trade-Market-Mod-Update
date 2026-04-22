package com.trademarket.network;

import com.trademarket.TradeMarketMod;
import com.trademarket.client.LocalizationManager;
import com.trademarket.client.RemoteLogger;
import com.trademarket.client.ToastNotificationManager;
import com.trademarket.data.MarketDataManager;
import com.trademarket.data.MarketListing;
import com.trademarket.data.SupabaseClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Обработчик сетевых операций для Trade Market
 * Теперь работает напрямую с Supabase без необходимости серверного мода
 */
public class NetworkHandler {

    /**
     * Регистрация на стороне сервера (оставляем пустым для совместимости)
     * Теперь вся логика работает через Supabase напрямую с клиента
     */
    public static void registerServerPackets() {
        // Серверные пакеты больше не нужны - всё работает через Supabase
        TradeMarketMod.LOGGER.info("Trade Market работает в режиме Supabase - серверные пакеты не требуются");
    }

    /**
     * Регистрация на стороне клиента
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientPackets() {
        // Клиентские пакеты от сервера больше не нужны
        TradeMarketMod.LOGGER.info("Trade Market клиент инициализирован - используется Supabase");
    }

    // ==================== Client Methods ====================

    /**
     * Создать новый лот и отправить в Supabase (без цены - для обратной совместимости)
     */
    @Environment(EnvType.CLIENT)
    public static void sendCreateListing(int slotIndex, String description) {
        sendCreateListing(slotIndex, "", description);
    }
    
    /**
     * Создать новый лот и отправить в Supabase (с ценой)
     */
    @Environment(EnvType.CLIENT)
    public static void sendCreateListing(int slotIndex, String price, String description) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack stack = client.player.getInventory().getStack(slotIndex);
        if (stack.isEmpty()) return;

        // Создаем лот с client для получения tooltip
        MarketListing listing = new MarketListing(
                client.player.getUuid(),
                client.player.getName().getString(),
                stack,
                price,
                description,
                client.world.getRegistryManager(),
                client
        );

        // Отправляем в Supabase
        final String itemName = listing.getItemDisplayName();
        final String priceValue = listing.getPrice();
        final int quantity = listing.getItemCount();
        
        MarketDataManager.getInstance().createListing(
                listing,
                () -> {
                    // Успех - показываем сообщение в главном потоке
                    client.execute(() -> {
                        client.player.sendMessage(
                                Text.literal("§a[Trade Market] Предмет выставлен на продажу!"),
                                false
                        );
                        // Показываем тост-уведомление
                        LocalizationManager lang = LocalizationManager.getInstance();
                        ToastNotificationManager.getInstance().showInfo(
                            lang.get("listing_created"),
                            itemName
                        );
                        // Логируем создание листинга
                        RemoteLogger.getInstance().logListingCreate(itemName, priceValue, quantity);
                        // Обновляем список
                        MarketDataManager.getInstance().forceRefresh(null, null);
                    });
                },
                error -> {
                    // Ошибка
                    client.execute(() -> {
                        client.player.sendMessage(
                                Text.literal("§c[Trade Market] Ошибка: " + error),
                                false
                        );
                        // Логируем ошибку
                        RemoteLogger.getInstance().error(RemoteLogger.Category.LISTING, "LISTING_CREATE_ERROR", error);
                    });
                }
        );
    }

    /**
     * Удалить лот из Supabase
     */
    @Environment(EnvType.CLIENT)
    public static void sendRemoveListing(UUID listingId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Получаем информацию о листинге до удаления для логирования
        MarketListing listing = MarketDataManager.getInstance().getListing(listingId);
        final String itemName = listing != null ? listing.getItemDisplayName() : "Unknown";
        
        MarketDataManager.getInstance().removeListing(
                listingId,
                () -> {
                    client.execute(() -> {
                        client.player.sendMessage(
                                Text.literal("§c[Trade Market] Лот удален!"),
                                false
                        );
                        // Логируем удаление
                        RemoteLogger.getInstance().logListingDelete(listingId.toString(), itemName);
                        MarketDataManager.getInstance().forceRefresh(null, null);
                    });
                },
                error -> {
                    client.execute(() -> {
                        client.player.sendMessage(
                                Text.literal("§c[Trade Market] Ошибка удаления: " + error),
                                false
                        );
                        RemoteLogger.getInstance().error(RemoteLogger.Category.LISTING, "LISTING_DELETE_ERROR", error);
                    });
                }
        );
    }
    
    /**
     * Обновить существующий лот в Supabase
     */
    @Environment(EnvType.CLIENT)
    public static void sendUpdateListing(UUID listingId, String price, String description) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        MarketDataManager.getInstance().updateListing(
                listingId,
                price,
                description,
                () -> {
                    client.execute(() -> {
                        client.player.sendMessage(
                                Text.literal("§a[Trade Market] Лот обновлен!"),
                                false
                        );
                        MarketDataManager.getInstance().forceRefresh(null, null);
                    });
                },
                error -> {
                    client.execute(() -> {
                        client.player.sendMessage(
                                Text.literal("§c[Trade Market] Ошибка обновления: " + error),
                                false
                        );
                    });
                }
        );
    }

    /**
     * Отправить сообщение продавцу
     * Поскольку у нас нет прямого доступа к другим клиентам,
     * копируем ник продавца в чат для связи через Discord/внешние средства
     */
    @Environment(EnvType.CLIENT)
    public static void sendContactSeller(UUID listingId, String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        MarketListing listing = MarketDataManager.getInstance().getListing(listingId);
        if (listing == null) {
            client.player.sendMessage(
                    Text.literal("§c[Trade Market] Лот не найден"),
                    false
            );
            return;
        }

        // Показываем информацию о продавце
        client.player.sendMessage(Text.literal("§6═══════════════════════════════════"), false);
        client.player.sendMessage(Text.literal("§e[Trade Market] §fИнформация о продавце:"), false);
        client.player.sendMessage(Text.literal("§7Ник: §f" + listing.getSellerName()), false);
        client.player.sendMessage(Text.literal("§7Предмет: §f" + listing.getItemId()), false);
        if (!listing.getDescription().isEmpty()) {
            client.player.sendMessage(Text.literal("§7Условия: §f" + listing.getDescription()), false);
        }
        client.player.sendMessage(Text.literal("§7Ваше сообщение: §f" + message), false);
        client.player.sendMessage(Text.literal(""), false);
        client.player.sendMessage(Text.literal("§aСвяжитесь с продавцом в игровом чате:"), false);
        client.player.sendMessage(Text.literal("§f/msg " + listing.getSellerName() + " " + message), false);
        client.player.sendMessage(Text.literal("§6═══════════════════════════════════"), false);
    }

    /**
     * Загрузить все лоты из Supabase
     */
    @Environment(EnvType.CLIENT)
    public static void fetchAllListings() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Устанавливаем registries для десериализации предметов
        if (client.world != null) {
            MarketDataManager.getInstance().setRegistries(client.world.getRegistryManager());
        }

        MarketDataManager.getInstance().fetchAllListings(
                () -> {
                    TradeMarketMod.LOGGER.info("Лоты успешно загружены из Supabase");
                },
                error -> {
                    TradeMarketMod.LOGGER.error("Ошибка загрузки лотов: " + error);
                    // Логируем ошибку загрузки
                    RemoteLogger.getInstance().logConnectionError("/listings", error);
                    if (client.player != null) {
                        client.execute(() -> {
                            client.player.sendMessage(
                                    Text.literal("§c[Trade Market] Не удалось загрузить лоты. Проверьте подключение к интернету."),
                                    false
                            );
                        });
                    }
                }
        );
    }
}
