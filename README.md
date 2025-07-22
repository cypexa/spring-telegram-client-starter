# Spring Telegram Client Starter

Компонент-фасад для авторизации в Telegram через REST API.

## Конфигурация

Настройте следующие параметры в `application.properties`:

```properties
# Обязательные параметры
spring.telegram.client.api-id=YOUR_API_ID
spring.telegram.client.api-hash=YOUR_API_HASH

# Дополнительные параметры
spring.telegram.client.database-directory=./tdlib
spring.telegram.client.files-directory=./tdlib
spring.telegram.client.system-language-code=en
spring.telegram.client.device-model=Desktop
spring.telegram.client.application-version=1.0
spring.telegram.client.enable-storage-optimizer=true
spring.telegram.client.ignore-file-names=false
spring.telegram.client.use-test-dc=false
spring.telegram.client.log-verbosity-level=1
```

## REST API Endpoints

### 1. Отправка номера телефона

```
POST /api/telegram/auth/phone
Content-Type: application/json

{
  "phoneNumber": "+79991234567"
}
```

**Ответ:**
```json
{
  "state": "AuthorizationStateWaitCode",
  "message": "Phone number sent successfully",
  "success": true,
  "error": null
}
```

### 2. Подтверждение кода

```
POST /api/telegram/auth/code
Content-Type: application/json

{
  "code": "12345"
}
```

**Ответ:**
```json
{
  "state": "AuthorizationStateReady",
  "message": "Authentication code verified successfully",
  "success": true,
  "error": null
}
```

### 3. Статус авторизации

```
GET /api/telegram/auth/status
```

**Ответ:**
```json
{
  "state": "AuthorizationStateReady",
  "message": "User is authorized",
  "success": true,
  "error": null
}
```

## Состояния авторизации

- `NOT_INITIALIZED` - Клиент не инициализирован
- `AuthorizationStateWaitTdlibParameters` - Ожидание параметров TDLib
- `AuthorizationStateWaitPhoneNumber` - Ожидание номера телефона
- `AuthorizationStateWaitCode` - Ожидание кода подтверждения
- `AuthorizationStateWaitRegistration` - Ожидание регистрации
- `AuthorizationStateWaitPassword` - Ожидание пароля
- `AuthorizationStateReady` - Авторизация завершена
- `AuthorizationStateLoggingOut` - Выход из системы
- `AuthorizationStateClosing` - Закрытие соединения
- `AuthorizationStateClosed` - Соединение закрыто

## Пример использования

1. Сначала отправьте номер телефона:
```bash
curl -X POST http://localhost:8080/api/telegram/auth/phone \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+79991234567"}'
```

2. Введите код подтверждения, полученный в Telegram:
```bash
curl -X POST http://localhost:8080/api/telegram/auth/code \
  -H "Content-Type: application/json" \
  -d '{"code": "12345"}'
```

3. Проверьте статус авторизации:
```bash
curl -X GET http://localhost:8080/api/telegram/auth/status
```

## Получение API ID и Hash

1. Зайдите на https://my.telegram.org/auth
2. Войдите в аккаунт
3. Перейдите в "API development tools"
4. Создайте новое приложение
5. Скопируйте API ID и API Hash в конфигурацию

## Настройка логирования

По умолчанию TDLib выводит много внутренних логов. Чтобы их отключить:

1. Установите минимальный уровень логирования TDLib:
```properties
spring.telegram.client.log-verbosity-level=0
```

Уровни логирования:
- `0` - NEVER (отключить полностью)
- `1` - ERROR (только ошибки) - по умолчанию
- `2` - WARNING (предупреждения)
- `3` - INFO (информационные сообщения)
- `4` - DEBUG (отладочные сообщения)
- `5` - VERBOSE (подробные логи)

2. Дополнительно можно отключить логирование Spring Boot для TDLib:
```properties
logging.level.org.drinkless.tdlib=ERROR
```

## Зависимости

- Spring Boot 3.5.3
- TDLib (через JNI)
- Lombok
- Jackson

## Запуск

```bash
mvn spring-boot:run
```

Сервер будет доступен по адресу `http://localhost:8080` 