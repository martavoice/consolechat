package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Марта on 31.03.2017.
 */
public class Server {
   private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        int port = ConsoleHelper.readInt();
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("сервер запущен");
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
                continue;
            } catch (Exception e) {
                serverSocket.close();
                e.printStackTrace();
                break;
            }
        }
    }
    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message answer = connection.receive();

                if (answer.getType() == MessageType.USER_NAME) {

                    if (!answer.getData().isEmpty()) {
                        if (!connectionMap.containsKey(answer.getData())) {
                            connectionMap.put(answer.getData(), connection);
                            connection.send(new Message(MessageType.NAME_ACCEPTED));
                            return answer.getData();
                        }
                    }
                }
            }
        }
        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (!entry.getKey().equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
                }
            }
        }

        private  void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true) {
                Message message = connection.receive();
                if (message != null && message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                } else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Connect to " + socket.getRemoteSocketAddress());
            String username = null;
            try {
                Connection connection = new Connection(socket);
                username = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, username));
                sendListOfUsers(connection,username);
                serverMainLoop(connection,username);
            }catch (IOException e){
              ConsoleHelper.writeMessage("Error");
            }catch (ClassNotFoundException e){
                ConsoleHelper.writeMessage("Error");
            }finally {
                if (username!=null){
                    connectionMap.remove(username);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, username));
                }
                ConsoleHelper.writeMessage("Closed connection to a remote socket address: ");
            }

        }
    }

        public static void sendBroadcastMessage(Message message){
        for (Map.Entry<String, Connection> pair : connectionMap.entrySet()){
            try{
                pair.getValue().send(message);
            }catch (IOException e){
                ConsoleHelper.writeMessage("can't send message");
            }
        }
    }
}
