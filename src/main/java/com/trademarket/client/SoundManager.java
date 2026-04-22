package com.trademarket.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/**
 * Менеджер звуковых эффектов для TradeMarket
 */
public class SoundManager {
    private static SoundManager instance;
    private boolean soundEnabled = true;
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    private SoundManager() {}
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Звук открытия GUI
     */
    public void playOpenSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.8f));
        }
    }
    
    /**
     * Звук закрытия GUI
     */
    public void playCloseSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.6f));
        }
    }
    
    /**
     * Звук нажатия кнопки
     */
    public void playClickSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f));
        }
    }
    
    /**
     * Звук успешной операции (покупка/продажа)
     */
    public void playSuccessSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f));
        }
    }
    
    /**
     * Звук ошибки
     */
    public void playErrorSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f));
        }
    }
    
    /**
     * Звук нового сообщения
     */
    public void playMessageSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.5f));
        }
    }
    
    /**
     * Звук уведомления
     */
    public void playNotificationSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.8f, 1.2f));
        }
    }
    
    /**
     * Звук добавления в избранное
     */
    public void playFavoriteSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.ENTITY_PLAYER_LEVELUP, 0.3f, 1.5f));
        }
    }
    
    /**
     * Звук переключения вкладок
     */
    public void playTabSwitchSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.2f));
        }
    }
    
    /**
     * Звук скролла
     */
    public void playScrollSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.5f));
        }
    }
    
    /**
     * Звук оценки продавца
     */
    public void playRatingSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f));
        }
    }
    
    /**
     * Звук тост-уведомления (как достижение)
     */
    public void playToastSound() {
        if (!soundEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f));
        }
    }
}
