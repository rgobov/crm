# Android-приложение 999 CRM

Модуль запускает production PWA через Trusted Web Activity. Если на устройстве нет подходящего Custom Tabs-провайдера, используется встроенный WebView fallback.

## Сборка

```bash
gradle :app:bundleRelease
```

Если Gradle не установлен, откройте каталог `android` в Android Studio, синхронизируйте проект и установите Android SDK 35.

## Подготовка TWA

1. Создать release keystore и не добавлять его в Git.
2. Подписать AAB ключом разработчика RuStore.
3. Получить SHA-256 отпечаток release-сертификата.
4. Разместить на `https://crm.999crm.ru/.well-known/assetlinks.json` файл с package name `ru.tryneuro.crm` и этим отпечатком.
5. Проверить TWA на чистом устройстве и устройстве без Chrome.

## Ограничения

В проекте намеренно запрашивается только `INTERNET`. Доступ к файлам предоставляется системным выбором файла без постоянного Android-разрешения.
