# RDT (Reliable Data Transefer)

## Description

Проект содержит реализацию протокола RDT и пример клиента и сервера для демонстрации корректной работы протокола.

**ВАЖНО**: Т.к. RDT не реализует window size, пользователь сам должен позаботиться, чтобы не возникло ситуации, когда два процесса одновременно отправляют друг другу сообщения c данными, чтобы не возникло deadlock'a.

### Определения
* __Клиент__ - процесс, который инициирует сооединение.
* __Сервер__ - процесс, который закрывает соединение.

### RDT реализует:
* __Открытие и закрытие соединения__. Соединение открывается клиентом через __handshake__  и закрывается сервером через __Close the connection__.
    * __Handshake__.
        1. Клиент отправляет SYN(NCK = 0) серверу.
        2. Сервер отправляет SYN ACK(SEQ = 1) клиенту.
        3. Клиент отправляет ACK(NCK = 1) серверу.
    * __Close the connection__.
        1. Сервер отправляет FIN.
        2. Клиент отправляет FIN ACK.
        3. Сервер отправляет ACK.
* __Гарантированная доставка__.
    * Все пакеты будут доставлены или будет выброшено исключение.
    * Предполагается, что пакет не может быть поврежден: только утерян.
    * Для демонстрации механизма гарантированной доставки, процесс с вероятностью 20% не принимает пакет.

# Exec
1. Для запуска сервера, введите
```
gradle run -PchooseMain=ru.nsu.fit.g19202.karpov.rdt.server.Server
```
в терминале.
2. Для запуска клиента, введите
```
gradle run -PchooseMain=ru.nsu.fit.g19202.karpov.rdt.client.Client
```
в терминале