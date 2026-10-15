# GitHub Actions

В репозитории два workflow.

## 1. `ci.yml` — Непрерывная интеграция

Триггеры:

- push в ветки `main` и `develop`;
- любой pull request в `main` / `develop`;
- вручную (`workflow_dispatch`).

Раннер: `ubuntu-latest`.

Шаги:

1. Установка JDK 17 (Temurin).
2. Чтение версии Gradle из `gradle/wrapper/gradle-wrapper.properties` и установка её
   через `gradle/actions/setup-gradle` (с кэшем Gradle).
3. Генерация `gradlew` командой `gradle wrapper` — в репозитории хранится только
   `gradle-wrapper.properties`.
4. Генерация `debug.keystore` через `keytool` (стандартный Android debug keystore:
   алиас `androiddebugkey`, пароль `android`).
5. `./gradlew testDebugUnitTest` — JVM-юнит-тесты для debug-варианта.
6. `./gradlew lintDebug` — Android Lint для debug-варианта (неблокирующий; ошибки
   видны в артефакте `lint-reports` и как warning в summary).
7. `./gradlew assembleDebug` — сборка debug APK.
8. Публикация артефактов: `app-debug-apk`, `unit-test-reports`, `lint-reports`.

### Соответствие задач Gradle и build variants

У Android Gradle Plugin для каждого build type (`debug`, `release`) создаётся свой
набор задач:

| Задача                | Что делает                                                        |
| --------------------- | ----------------------------------------------------------------- |
| `assembleDebug`       | APK с debug-подписью, `debuggable=true`, без R8/минификации.      |
| `assembleRelease`     | Прод-APK: релизная подпись, оптимизации, минификация.             |
| `bundleRelease`       | AAB для Google Play.                                              |
| `testDebugUnitTest`   | JVM-юнит-тесты (`src/test/`) против debug-варианта.               |
| `testReleaseUnitTest` | То же для release-варианта.                                       |
| `lintDebug`           | Android Lint для debug-варианта.                                  |
| `lintRelease`         | То же для release-варианта.                                       |

## 2. `release.yml` — Релиз по тегу

Триггеры:

- push тега вида `v*` (например, `v1.0.0`, `v2.1.0-rc1`);
- вручную (`workflow_dispatch`) — только собирает артефакты без создания GitHub Release.

Job `build-release` собирает:

- `KeyMasterNFC-<version>-universal.apk` — универсальный APK (все ABI в одном файле);
- `KeyMasterNFC-<version>-<abi>.apk` — APK для `armeabi-v7a`, `arm64-v8a`, `x86`,
  `x86_64` (через `-Pandroid.injected.build.abi=<abi>`);
- `KeyMasterNFC-<version>.aab` — Android App Bundle для публикации в Google Play;
- `SHA256SUMS.txt` — контрольные суммы.

`versionName` берётся из имени тега (`v1.2.3` → `1.2.3`), `versionCode` — из
`github.run_number`. Теги с суффиксом (`v1.2.3-rc1`) публикуются как pre-release.

Job `publish-release` создаёт GitHub Release c автоматически сгенерированными
release notes (`softprops/action-gh-release@v2`) и прикрепляет все артефакты.

## Необходимые секреты репозитория

`Settings → Secrets and variables → Actions`:

| Секрет            | Описание                                                        |
| ----------------- | --------------------------------------------------------------- |
| `KEYSTORE_BASE64` | Содержимое `.jks` keystore, закодированное в base64.            |
| `STORE_PASSWORD`  | Пароль хранилища ключей.                                        |
| `KEY_PASSWORD`    | Пароль ключа с алиасом `upload`.                                |
| `GEMINI_API_KEY`  | Ключ Gemini API (используется приложением во время выполнения). |

Подготовка keystore:

```bash
base64 -w 0 my-upload-key.jks > keystore.b64
# содержимое keystore.b64 скопируйте в секрет KEYSTORE_BASE64
```

Алиас ключа должен быть `upload` (см. `app/build.gradle.kts`).

## Как выпустить релиз

```bash
git tag v1.0.0
git push origin v1.0.0
```
