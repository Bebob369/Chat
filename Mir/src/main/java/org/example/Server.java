package org.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Server {

    private static final String CONFIG_FILE = "config.properties";
    private static Properties config;

    // загрузка настроек из файла
    public static void main(String[] args) throws IOException {
        loadConfig();
        // получение порта сервера из настроек
        int port = Integer.parseInt(config.getProperty("server.port"));


        System.out.println("Запуск сервера...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(String.format("Сервер запущен на порту %d", port));
            // бесконечный цикл для прослушивания клиентских подключений
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новый клиент подключился: " + clientSocket.getInetAddress().getHostAddress());

                // Обработка клиентского подключения в новом потоке(создаем и запускаем поток)
                Thread thread = new Thread(() -> handleConnection(clientSocket));
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка запуска сервера: " + e.getMessage());
        }
    }
    // метод для обработки подключения клиента
    private static void handleConnection(Socket clientSocket) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));// получение входного потока от клиента
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)// создание выходного потока для отправки данных клиенту
        ) {
            String inputLine;
            // цикл для получения данных от клиента
            while ((inputLine = in.readLine()) != null) {
                // получение объекта JSON от клиента
                JsonObject jsonObject = new Gson().fromJson(inputLine, JsonObject.class).get("Request").getAsJsonObject();
                //получаем логин и текст сообщения
                String login = jsonObject.getAsJsonObject("User").get("Login").getAsString();
                String body = jsonObject.getAsJsonObject("Message").get("Body").getAsString();
                String responseMessage = String.format(
                        "{\"UserLogin\":\"%s\",\"Result\":\"success\",\"Timestamp\":\"%s\"}",
                        login,
                        dateFormat.format(new Date())
                );
                // формирование объекта JSON с ответом на запрос клиента
                String response = String.format(
                        "{\"Response\":{\"Message\":%s}}",
                        responseMessage
                );

                System.out.println("Принято сообщение от клиента: " + login + " отправлено: " + jsonObject.getAsJsonObject("Message").get("Timestamp").getAsString() + ", текст: " + body);
                System.out.println(String.format("Отправка ответа клиенту: %s", response));

                out.println(response);
            }

        }
        //Если возникнет ошибка IOException то программа выведет сообщение и перейдет к блоку finaly
        catch (IOException e) {
            System.out.println("Ошибка обработки подключения клиента: " + e.getMessage());
        }
        //Закрытие клиент сокета
        finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии соединения с клиентом: " + e.getMessage());
            }
        }
    }
    //Загрузка настроек из файла CONFIG_FILE в объект config
    private static void loadConfig() throws IOException {
        config = new Properties();
        FileInputStream input = new FileInputStream(CONFIG_FILE);
        config.load(input);
        input.close();
    }
}
