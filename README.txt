==================================================
TRADE MARKET MOD - Minecraft 1.21.11 (Fabric)
==================================================

СТРУКТУРА ПРОЕКТА:
------------------
minecraft-mod/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/main/
    ├── java/com/trademarket/
    │   ├── TradeMarketMod.java          (главный класс)
    │   ├── client/
    │   │   ├── TradeMarketClient.java   (клиент, клавиша F8)
    │   │   └── screen/
    │   │       └── TradeMarketScreen.java (GUI экран)
    │   ├── data/
    │   │   ├── MarketListing.java       (класс лота)
    │   │   └── MarketDataManager.java   (менеджер данных)
    │   └── network/
    │       └── NetworkHandler.java      (сетевые пакеты)
    └── resources/
        ├── fabric.mod.json
        └── assets/trademarket/lang/
            ├── en_us.json
            └── ru_ru.json


УСТАНОВКА И СБОРКА:
-------------------
1. Установи JDK 21
2. Скачай Gradle Wrapper:
   - gradle wrapper

3. Собери мод:
   - ./gradlew build
   
4. JAR файл будет в: build/libs/trademarket-1.0.0.jar

5. Установи в Minecraft:
   - Скопируй JAR в папку mods/
   - Нужен Fabric Loader и Fabric API


КАК ПОЛЬЗОВАТЬСЯ:
-----------------
1. Нажми F8 чтобы открыть Trade Market

2. Вкладки:
   - "Обзор" - смотри все лоты других игроков
   - "Мои лоты" - управляй своими лотами
   - "Продать" - выстави предмет на продажу

3. Чтобы продать:
   - Перейди на вкладку "Продать"
   - Кликни на предмет в инвентаре
   - Напиши описание/цену
   - Нажми "Выставить на продажу"

4. Чтобы купить:
   - Найди нужный лот на вкладке "Обзор"
   - Нажми "Связаться"
   - Напиши сообщение продавцу
   - Договоритесь о встрече в игре


ОСОБЕННОСТИ:
------------
- Данные сохраняются в папке мира: trademarket/market_data.dat
- Предмет НЕ забирается у игрока при выставлении (это объявление)
- Сделка происходит лично между игроками
- Работает на серверах с несколькими игроками
- Сообщения приходят в чат с пометкой [Trade Market]


ТРЕБОВАНИЯ:
-----------
- Minecraft 1.21.11
- Fabric Loader 0.18.1+
- Fabric API 0.141.1+1.21.11
- Java 21
