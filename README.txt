==================================================
TRADE MARKET MOD - Minecraft 1.21.11 (Fabric)
==================================================

Версия: 1.1.4
Автор: Nikita2888


СТРУКТУРА ПРОЕКТА:
------------------
minecraft-mod/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/main/
    ├── java/com/trademarket/
    │   ├── TradeMarketMod.java              (главный класс)
    │   ├── client/
    │   │   ├── TradeMarketClient.java       (клиент, клавиша F8)
    │   │   ├── UpdateChecker.java           (проверка обновлений GitHub)
    │   │   ├── LocalizationManager.java     (локализация EN/RU)
    │   │   ├── SoundManager.java            (звуки)
    │   │   ├── RemoteLogger.java            (логирование)
    │   │   ├── NotificationChecker.java     (уведомления)
    │   │   ├── ToastNotificationManager.java (тосты)
    │   │   └── screen/
    │   │       ├── TradeMarketScreen.java   (главный GUI экран)
    │   │       ├── UpdateScreen.java        (экран обновлений)
    │   │       ├── ScreenState.java         (состояние экрана)
    │   │       ├── ScreenConstants.java     (константы цветов/размеров)
    │   │       ├── RenderUtils.java         (утилиты рендеринга)
    │   │       ├── TextUtils.java           (работа с текстом)
    │   │       ├── DataLoader.java          (загрузка данных)
    │   │       ├── ClickHandlers.java       (обработка кликов)
    │   │       ├── SidebarRenderer.java     (боковая панель)
    │   │       ├── TabsRenderer.java        (вкладки)
    │   │       ├── ListingsRenderer.java    (список лотов)
    │   │       ├── DetailsViewRenderer.java (детали лота)
    │   │       ├── SellUIRenderer.java      (интерфейс продажи)
    │   │       ├── BookmarkRenderer.java    (закладки)
    │   │       ├── AdminPanelRenderer.java  (админ панель)
    │   │       ├── SupportPanelRenderer.java (поддержка)
    │   │       └── SupportModels.java       (модели поддержки)
    │   ├── data/
    │   │   ├── MarketListing.java           (класс лота)
    │   │   ├── MarketDataManager.java       (менеджер данных)
    │   │   └── SupabaseClient.java          (клиент Supabase)
    │   └── network/
    │       └── NetworkHandler.java          (сетевые запросы)
    └── resources/
        ├── fabric.mod.json
        └── assets/trademarket/
            ├── icon.png
            ├── lang/
            │   ├── en_us.json
            │   └── ru_ru.json
            └── textures/gui/
                ├── trade-market-texture.png
                ├── discord.png
                ├── telegram.png
                ├── emerald.png
                └── ...


УСТАНОВКА И СБОРКА:
-------------------
1. Установи JDK 21

2. Собери мод:
   ./gradlew build
   
3. JAR файл будет в: build/libs/trademarket-1.1.4.jar

4. Установи в Minecraft:
   - Скопируй JAR в папку mods/
   - Нужен Fabric Loader и Fabric API


КАК ПОЛЬЗОВАТЬСЯ:
-----------------
1. Нажми F8 чтобы открыть Trade Market

2. При открытии мод автоматически проверит обновления через GitHub
   - Если есть новая версия - покажет changelog и кнопку скачать
   - Если версия актуальна - сразу откроет главное меню

3. Вкладки:
   - "Все" - все лоты других игроков
   - "Мои" - твои активные лоты
   - "Продать" - выставить предмет на продажу
   - "Избранное" - сохраненные лоты
   - "Чаты" - сообщения по лотам
   - "Юзеры" - онлайн пользователи (для админов)

4. Чтобы продать:
   - Перейди на вкладку "Продать"
   - Выбери предмет из инвентаря
   - Укажи цену
   - Нажми "Выставить"

5. Чтобы связаться с продавцом:
   - Открой нужный лот
   - Нажми "Написать"
   - Договоритесь о сделке


СИСТЕМА АВТО-ОБНОВЛЕНИЙ:
------------------------
Мод проверяет обновления через GitHub Releases API.

Настройка (для разработчиков):
1. Создай репозиторий на GitHub
2. В файле UpdateChecker.java измени:
   - GITHUB_OWNER = "твой_ник"
   - GITHUB_REPO = "название_репо"

3. Создавай релизы на GitHub:
   - Tag: v1.2.0 (номер версии с 'v')
   - Загрузи .jar файл
   - Напиши changelog в описании

Для приватных репозиториев:
- Добавь GITHUB_TOKEN в UpdateChecker.java
- Создай токен на github.com/settings/tokens с правами "repo"


ОСОБЕННОСТИ:
------------
- Данные хранятся в Supabase (облако)
- Авто-проверка обновлений при каждом нажатии F8
- Поддержка русского и английского языков
- Система избранного
- Чат между покупателем и продавцом
- Админ-панель для модерации
- Система поддержки (тикеты)
- Уведомления о новых сообщениях


ТРЕБОВАНИЯ:
-----------
- Minecraft 1.21.11
- Fabric Loader 0.18.1+
- Fabric API 0.141.1+1.21.11
- Java 21


КОНТАКТЫ:
---------
- Discord: @querty_5555
- Telegram: @no_need_lmao
- GitHub: github.com/Nikita2888/Trade-Market-Mod-Update


==================================================
