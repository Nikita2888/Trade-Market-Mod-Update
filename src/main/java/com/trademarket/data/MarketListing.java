package com.trademarket.data;

import com.mojang.serialization.DataResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Представляет один лот на рынке
 * Поддерживает сериализацию для Supabase
 * Обновлено для Minecraft 1.21.11 API
 */
public class MarketListing {
    private final UUID listingId;
    private final UUID sellerId;
    private final String sellerName;
    private final String itemId;       // minecraft:diamond_sword
    private final int itemCount;       // количество
    private final String itemNbt;      // NBT в строковом формате
    private final String itemDisplayName; // Читаемое название для отображения
    private String price;           // Цена (например: "5 LE" или "100 EB") - изменяемое поле
    private String description;     // Условия сделки - изменяемое поле
    private final long createdAt;
    private boolean isActive;
    
    // Кэшированный ItemStack
    private transient ItemStack cachedItemStack;

    /**
     * Конструктор для создания нового лота из ItemStack
     */
    public MarketListing(UUID sellerId, String sellerName, ItemStack itemStack, 
                         String price, String description, RegistryWrapper.WrapperLookup registries,
                         net.minecraft.client.MinecraftClient client) {
        this.listingId = UUID.randomUUID();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemId = Registries.ITEM.getId(itemStack.getItem()).toString();
        this.itemCount = itemStack.getCount();
        
        // Сохраняем читаемое название из tooltip (для WynnCraft предметов)
        String displayName = "";
        try {
            var tooltip = itemStack.getTooltip(
                    net.minecraft.item.Item.TooltipContext.create(client.world),
                    client.player,
                    net.minecraft.item.tooltip.TooltipType.ADVANCED);
            if (tooltip != null && !tooltip.isEmpty()) {
                displayName = tooltip.get(0).getString();
            }
        } catch (Exception e) {
            displayName = itemStack.getName().getString();
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = itemStack.getName().getString();
        }
        // Очищаем от непечатаемых символов (квадратики)
        this.itemDisplayName = cleanDisplayName(displayName);
        
        // Сериализуем ItemStack через CODEC (1.21.11 API)
        String nbtString = "";
        try {
            RegistryOps<net.minecraft.nbt.NbtElement> ops = registries.getOps(NbtOps.INSTANCE);
            DataResult<net.minecraft.nbt.NbtElement> result = ItemStack.CODEC.encodeStart(ops, itemStack);
            if (result.result().isPresent()) {
                nbtString = result.result().get().toString();
            }
        } catch (Exception e) {
            // Fallback - пустая строка
        }
        this.itemNbt = nbtString;
        
        this.price = price != null ? price : "";
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.cachedItemStack = itemStack.copy();
    }

    /**
     * Конструктор для загрузки из Supabase
     */
    public MarketListing(UUID listingId, UUID sellerId, String sellerName,
                         String itemId, int itemCount, String itemNbt,
                         String itemDisplayName, String price, String description, 
                         long createdAt, boolean isActive) {
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemId = itemId;
        this.itemCount = itemCount;
        this.itemNbt = itemNbt != null ? itemNbt : "";
        this.itemDisplayName = itemDisplayName != null ? itemDisplayName : "";
        this.price = price != null ? price : "";
        this.description = description != null ? description : "";
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    /**
     * Получить ItemStack из данных лота (1.21.11 API)
     */
    public ItemStack getItemStack(RegistryWrapper.WrapperLookup registries) {
        if (cachedItemStack != null) {
            return cachedItemStack.copy();
        }
        
        try {
            // Если есть NBT данные, восстанавливаем через CODEC
            if (itemNbt != null && !itemNbt.isEmpty()) {
                // 1.21.11 API: StringNbtReader.fromOps().read() вместо parse()
                StringNbtReader<net.minecraft.nbt.NbtElement> reader = StringNbtReader.fromOps(NbtOps.INSTANCE);
                net.minecraft.nbt.NbtElement nbt = reader.read(itemNbt);
                RegistryOps<net.minecraft.nbt.NbtElement> ops = registries.getOps(NbtOps.INSTANCE);
                DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, nbt);
                if (result.result().isPresent()) {
                    cachedItemStack = result.result().get();
                    return cachedItemStack.copy();
                }
            }
            
            // Иначе создаем простой ItemStack по ID
            Identifier id = Identifier.tryParse(itemId);
            if (id != null) {
                var item = Registries.ITEM.get(id);
                cachedItemStack = new ItemStack(item, itemCount);
                return cachedItemStack.copy();
            }
        } catch (Exception e) {
            // Ошибка парсинга NBT
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Получить ItemStack (без registries - использует кэш или создает базовый)
     */
    public ItemStack getItemStack() {
        if (cachedItemStack != null) {
            return cachedItemStack.copy();
        }
        
        try {
            Identifier id = Identifier.tryParse(itemId);
            if (id != null) {
                var item = Registries.ITEM.get(id);
                cachedItemStack = new ItemStack(item, itemCount);
                return cachedItemStack.copy();
            }
        } catch (Exception e) {
            // ignore
        }
        
        return ItemStack.EMPTY;
    }

    // Геттеры
    public UUID getListingId() { return listingId; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getItemId() { return itemId; }
    public int getItemCount() { return itemCount; }
    public String getItemNbt() { return itemNbt; }
    public String getItemDisplayName() { return itemDisplayName; }
    public String getPrice() { return price; }
    public String getDescription() { return description; }
    public long getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }

    public void setActive(boolean active) { this.isActive = active; }
    public void setPrice(String price) { this.price = price != null ? price : ""; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }
    
    /**
     * Получить отформатированное время создания
     */
    public String getFormattedTime() {
        long diff = System.currentTimeMillis() - createdAt;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        com.trademarket.client.LocalizationManager lang = com.trademarket.client.LocalizationManager.getInstance();
        if (days > 0) {
            return lang.get("time_days_ago", days);
        } else if (hours > 0) {
            return lang.get("time_hours_ago", hours);
        } else if (minutes > 0) {
            return lang.get("time_minutes_ago", minutes);
        } else {
            return lang.get("time_just_now");
        }
    }

    /**
     * Сохранение в NBT (1.21.11 API - используем fallback версии методов)
     */
    public NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        // UUID сохраняем как строки (putUuid удален в 1.21.11)
        nbt.putString("listingId", listingId.toString());
        nbt.putString("sellerId", sellerId.toString());
        nbt.putString("sellerName", sellerName);
        nbt.putString("itemId", itemId);
        nbt.putInt("itemCount", itemCount);
        nbt.putString("itemNbt", itemNbt);
        nbt.putString("itemDisplayName", itemDisplayName);
        nbt.putString("price", price);
        nbt.putString("description", description);
        nbt.putLong("createdAt", createdAt);
        nbt.putBoolean("isActive", isActive);
        return nbt;
    }

    /**
     * Загрузка из NBT (1.21.11 API - используем fallback версии с двумя параметрами)
     */
    public static MarketListing fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        // UUID читаем из строк
        UUID listingId = UUID.fromString(nbt.getString("listingId", "00000000-0000-0000-0000-000000000000"));
        UUID sellerId = UUID.fromString(nbt.getString("sellerId", "00000000-0000-0000-0000-000000000000"));
        String sellerName = nbt.getString("sellerName", "Unknown");
        String itemId = nbt.getString("itemId", "minecraft:air");
        int itemCount = nbt.getInt("itemCount", 1);
        String itemNbt = nbt.getString("itemNbt", "");
        String itemDisplayName = nbt.getString("itemDisplayName", "");
        String price = nbt.getString("price", "");
        String description = nbt.getString("description", "");
        long createdAt = nbt.getLong("createdAt", System.currentTimeMillis());
        boolean isActive = nbt.getBoolean("isActive", true);

        return new MarketListing(listingId, sellerId, sellerName, itemId, itemCount,
                itemNbt, itemDisplayName, price, description, createdAt, isActive);
    }
    
    /**
     * Очищает название от непечатаемых WynnCraft символов
     * На основе WynnTils: https://github.com/Wynntils/Wynntils
     */
    private static String cleanDisplayName(String input) {
        if (input == null || input.isEmpty()) return "";
        
        // WynnTils: ITEM_NAME_MARKER = "\uDAFC\uDC00" - маркер границы названия
        String cleaned = input.replace("\uDAFC\uDC00", "");
        
        // WynnTils normalizeBadString: удаляем À и ֎, заменяем ' на '
        cleaned = cleaned.replace("ÀÀÀ", " ");
        cleaned = cleaned.replace("À", "");
        cleaned = cleaned.replace("\u00C0", ""); // À
        cleaned = cleaned.replace("֎", "");
        cleaned = cleaned.replace("\u058E", ""); // ֎
        cleaned = cleaned.replace("'", "'"); // RIGHT SINGLE QUOTATION MARK -> apostrophe
        cleaned = cleaned.replace("\u2019", "'");
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            
            // Пропускаем surrogate pairs (специальные WynnCraft символы)
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < cleaned.length() && Character.isLowSurrogate(cleaned.charAt(i + 1))) {
                    i++; // Пропускаем и low surrogate
                }
                continue;
            }
            if (Character.isLowSurrogate(c)) continue;
            
            // Пропускаем Private Use Area символы (иконки WynnCraft)
            if (c >= 0xE000 && c <= 0xF8FF) continue;
            
            // Пропускаем непечатаемые символы (кроме пробелов и переносов)
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') continue;
            
            // Пропускаем Minecraft форматирующий символ §
            if (c == '\u00A7') {
                // Пропускаем и следующий символ (код цвета/стиля)
                if (i + 1 < cleaned.length()) i++;
                continue;
            }
            
            result.append(c);
        }
        
        return result.toString().trim();
    }
}
