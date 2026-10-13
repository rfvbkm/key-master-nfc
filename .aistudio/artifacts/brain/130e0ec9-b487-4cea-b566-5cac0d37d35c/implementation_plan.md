# KeyMaster NFC — Программатор и менеджер беспроводных ключей

Полнофункциональное Android-приложение для взаимодействия с бесконтактными картами и RFID/NFC-ключами доступа через NFC: чтение секторов и страниц памяти, запись и клонирование дампов, аппаратное AES-GCM шифрование сохраненных ключей в Android KeyStore, а также экспертный криптографический аудит через Gemini 3.1 Pro Preview в режиме глубокого анализа.

## User Review & Critical Decisions

> [!IMPORTANT]
> Ключевые решения согласованы на этапе уточнения требований:

- **Поддерживаемые типы карт и протоколы**: MIFARE Classic (1K/4K, сектора, Key A / Key B), MIFARE Ultralight / NTAG213/215/216 (страницы памяти, блокировка, NDEF), ISO-DEP / NfcA и стандартные NDEF-теги.
- **Основной сценарий**: Инструмент дубликатора и программатора ключей, библиотека сохраненных дампов, поблочный редактор памяти и запись на поддерживаемые болванки/карты (UID change / Magic cards, NTAG rewritable).
- **Криптографическая безопасность**: Аппаратное AES-GCM 256-битное шифрование локальной базы ключей и дампов с использованием ключа, сгенерированного и защищенного в аппаратном модуле `Android KeyStore`.
- **Интеллектуальный аудит (Gemini 3.1 Pro Preview)**: Встроенный AI-анализатор дампов и протоколов (расшифровка секторов, аудит прав доступа Access Bits, обнаружение стандартных заводских ключей, анализ форматов домофонов и СКУД) с параметром `thinkingLevel = HIGH`.

---

## 1. Overview & Core Concept

### What It Does
**KeyMaster NFC** превращает смартфон с NFC-модулем в портативный программатор беспроводных ключей и карт доступа:
1. **Считыватель (Scanner / Reader)**: Мгновенное определение типа чипа (ATQA, SAK, UID/NUID), перебор ключей из словаря (default keys dictionary) для MIFARE Classic, чтение всех секторов и блоков памяти, чтение страниц Ultralight/NTAG.
2. **Менеджер дампов (Key Vault)**: Защищенное хранилище виртуальных карт и дампов ключей, зашифрованное аппаратно через AES-256-GCM. Возможность экспорта/импорта в стандартных форматах (`.bin`, `.mct`, `.json`).
3. **Программатор / Запись (Writer & Cloner)**: Запись данных в сектора, обновление ключей доступа A/B и битов условий доступа (Access Bits), форматирование NDEF, клонирование дампа на перезаписываемые заготовки (Magic Cards Gen1a/Gen2 CUID/FUID).
4. **HEX-Редактор памяти**: Интерактивная таблица секторов/блоков с цветовой подсветкой заголовков секторов, ключей доступа Key A/B, системных блоков и данных.
5. **AI Крипто-Аналитик (Gemini 3.1 Pro)**: Глубокий анализ дампа ключа: определение производителя системы контроля доступа (СКУД, домофоны Vizit, Cyfral, Metakom, парковочные системы), расшифровка структур данных, проверка уязвимостей конфигурации секторов.

### Target Audience
Мастера по изготовлению ключей, специалисты СКУД и систем безопасности, инженеры умных домов, исследователи RFID/NFC технологий и пользователи, желающие иметь безопасную цифровую копию своих пропусков и брелоков.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Сканирование ключа (Tap & Read)**:
   - Пользователь подносит брелок или карту к NFC-антенне смартфона.
   - Экран реагирует визуальным радаром и тактильным виброоткликом (Haptic Feedback).
   - Отображается карточка обнаруженного чипа: UID, тип карты (MIFARE Classic 1K, NTAG213 и т.д.), производитель, технологии (`NfcA`, `MifareClassic`, `Ndef`).
   - Запускается автоматическое чтение секторов с визуализацией прогресса (прочитано 16/16 секторов).

2. **Просмотр и редактирование дампа (HEX Editor)**:
   - Отображение дампа с группировкой по секторам.
   - Секторные трейлеры подсвечиваются: Key A (зеленый), Access Bits (синий), Key B (оранжевый).
   - Возможность тапнуть на любой блок и отредактировать HEX или ASCII значение.
   - Кнопка "AI Анализ безопасности" отправляет снимок дампа в Gemini с выводом детального отчета по правам доступа и формату карты.

3. **Запись и клонирование (Write / Clone)**:
   - Выбор сохраненного профиля из защищенного хранилища.
   - Выбор режима записи: полная запись (включая сектор 0 на Magic-карты), выборочная запись блоков данных или обновление ключей аутентификации.
   - Приложение ожидает поднесения чипа-реципиента и проводит верификацию после записи.

4. **Защищенное хранилище (Vault)**:
   - Список сохраненных карт с тегами (Домофон, Офис, Парковка, Квартира).
   - Индикатор шифрования "AES-GCM KeyStore Protected".
   - Управление словарем ключей (Keys Dictionary): стандартные ключи (`FFFFFFFFFFFF`, `A0A1A2A3A4A5`, транспортные ключи систем доступа) + возможность добавления пользовательских ключей.

### Visual Identity & Theme
- **Стиль**: Tech / Cyberpunk Minimalist — темная палитра с неоновыми статусными акцентами (Cyber Cyan, Signal Amber, Emerald Green).
- **Цветовая палитра**:
  - `Background`: `#0D1117` (глубокий графитово-черный)
  - `Surface`: `#161B22` и `#21262D` (контрастные панели)
  - `Primary / Accent`: `#00E5FF` (электрический циан для NFC-радара и активных состояний)
  - `Secondary`: `#7C4DFF` (неоновый фиолетовый для криптографии и AI)
  - `Success`: `#00E676` (успешное чтение/аутентификация)
  - `Warning / Key B`: `#FF9100`
- **Типографика**: Моноширинный шрифт для HEX-данных и UID, четкие M3 заголовки с чистым визуальным ритмом.
- **Интерактивные анимации**: Пульсирующий NFC-радар при ожидании метки, плавная анимация прогресса чтения секторов, тактильная отдача при успехе/ошибке.

---

## 3. Key Product Decisions & Trade-Offs

- **Android NFC Tech vs NdefOnly**: Используем низкоуровневые интерфейсы `MifareClassic`, `MifareUltralight`, `NfcA` через `NfcAdapter.enableReaderMode` с флагами `FLAG_READER_NFC_A | FLAG_READER_SKIP_NDEF_CHECK`, что обеспечивает максимальную скорость и стабильность считывания без задержек системного диалога Android.
- **Аппаратное шифрование Android KeyStore**: Мастер-ключ AES-256 генерируется внутри TEE/StrongBox устройства. Данные дампов и ключи доступа шифруются в режиме AES/GCM/NoPadding с уникальным 12-байтным IV на каждую запись. База данных Room хранит только зашифрованные шифротексты, гарантируя безопасность даже при руте или резервном копировании.
- **Поддержка стандартных и Magic-карт**: Предусмотрена запись как стандартных блоков данных, так и специальная логика для заготовок с перезаписываемым сектором 0 (UID change commands).
- **Gemini 3.1 Pro Thinking (High)**: Для анализа протоколов и безопасности используем `gemini-3.1-pro-preview` с `thinkingConfig: { thinkingLevel: "high" }`. Модель глубоко анализирует структуру Access Bits секторов MIFARE, выявляет уязвимости конфигурации (например, открытые права на запись трейлера сектора) и идентифицирует форматы пропусков.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        KeyMaster NFC App (M3)                          │
├────────────────────────────────────────────────────────────────────────┤
│ UI Layer:                                                              │
│  ├── ScannerScreen (NFC Radar, Tag Detection, Real-time Sector Reader) │
│  ├── DumpViewerScreen (Hex/ASCII Editor, Sector Trailers, Permissions) │
│  ├── VaultScreen (Encrypted Key Cards List, Tags, Import/Export)       │
│  ├── WriterScreen (Cloning, Write to Tag, Format, UID Writer)          │
│  ├── KeyDictionaryScreen (Default & Custom Keys for Authentication)    │
│  └── AiSecurityScreen (Gemini 3.1 Pro Deep Thinking Analysis)         │
├────────────────────────────────────────────────────────────────────────┤
│ ViewModel & Domain:                                                    │
│  ├── NfcViewModel (Reader Mode Lifecycle, Tag Dispatcher, Read/Write)  │
│  ├── VaultViewModel (Encrypted CRUD, Export/Import MCT/JSON)           │
│  └── AiAnalysisViewModel (Gemini REST Service with Thinking Level HIGH)│
├────────────────────────────────────────────────────────────────────────┤
│ Services & Cryptography:                                               │
│  ├── NfcManager (MifareClassic, MifareUltralight, NfcA, IsoDep, Ndef)  │
│  ├── CryptoVaultManager (Android KeyStore AES-256-GCM Encrypt/Decrypt) │
│  ├── MifareClassicHelper (Sector Auth, Access Bits Decoder, Hex Utils) │
│  └── GeminiApiService (Retrofit + Kotlin Serialization REST Client)    │
├────────────────────────────────────────────────────────────────────────┤
│ Persistence:                                                           │
│  └── Room Database (CardsDao, EncryptedDumpEntity, KeyDictionaryEntity)│
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models & Entities
1. **SavedCardEntity**: `id`, `name`, `tagType`, `uidHex`, `encryptedPayloadBase64`, `ivBase64`, `createdAt`, `notes`, `categoryTag`.
2. **CardDump**: `uid`, `atqa`, `sak`, `type`, `sectors: List<SectorDump>`, `ultralightPages: List<PageDump>`.
3. **SectorDump**: `sectorIndex`, `keyAHex`, `keyBHex`, `accessBitsHex`, `blocks: List<ByteArray>`.
4. **DictionaryKey**: `keyHex`, `label` (стандартный словарь включает FFFFFFFFFFFF, A0A1A2A3A4A5, D3F7D3F7D3F7, 000000000000, B0B1B2B3B4B5, 4D3A99C351DD, 1A982C7E459A и др.).
