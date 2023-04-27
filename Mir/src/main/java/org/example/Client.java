package org.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Client {
    private final String serverAddress;

    private final int serverPort;

    private Socket socket;
    //Чтение данных через сокет
    private BufferedReader reader;
    //Запись данных через сокет
    private PrintWriter writer;
    //Логин клиента
    private String login;
    //Инициализация Client с считаным портом и адресом сервера
    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
    //Подключение к серверу
    private void connectToServer() throws Exception {
        socket = new Socket(serverAddress, serverPort);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Вы подключены");
    }

    private void sendMessage(String message) throws Exception {
        // Отправка сообщения на сервер
        writer.println(message);

        //Ждем ответ от сервера
        String response = reader.readLine();

        // Парсим json и проверяем на null
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if (!json.isJsonNull()) {
            System.out.println("Сообщение доставлено");
        } else {
            throw new Exception("Произошла ошибка, ваш сеанс будет завершен");
        }
    }
    private JsonObject createJsonMessage(String body) {
        // Создаем json объект для сервера(логин(статик) и боди(изменяемый))
        JsonObject user = new JsonObject();
        user.addProperty("Login", login);
        JsonObject message = new JsonObject();
        message.addProperty("Body", body);
        message.addProperty("Timestamp", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

        JsonObject messageReturn = new JsonObject();

        messageReturn.add("User", user);

        messageReturn.add("Message", message);

        JsonObject request = new JsonObject();

        request.add("Request", messageReturn);

        return request;
    }

    private void startChat() throws Exception {
        //Задаем логин
        System.out.print("Введите имя: ");
        login = (new BufferedReader(new InputStreamReader(System.in))).readLine();
        //Цикл с отправкой сообщения
        while (true) {
            System.out.print("Введите сообщение: ");
            String body = (new BufferedReader(new InputStreamReader(System.in))).readLine();

            if ("\\exit".equalsIgnoreCase(body)) {
                break;
            }
            //Создание сообщение для сервера(json body)
            JsonObject jsonMessage = createJsonMessage(body);
            sendMessage(jsonMessage.toString());
        }
    }

    public static void main(String[] args) throws Exception {

        Properties properties = new Properties();
        // Считывание Хост:Порт с файла и подключение по этим данным клиента
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);

            String serverAddress = properties.getProperty("server.host");
            int serverPort = Integer.parseInt(properties.getProperty("server.port"));

            Client client = new Client(serverAddress, serverPort);
            try {
                client.connectToServer();
                client.startChat();
            } catch (Exception e) {
                System.err.println("Произошла ошибка, ваш сеанс будет завершен");
            } finally {
                client.disconnectFromServer();
            }
        }
    }
    //Дисконект с сервера
    private void disconnectFromServer() throws Exception {
        socket.close();
        reader.close();
        writer.close();
    }
}