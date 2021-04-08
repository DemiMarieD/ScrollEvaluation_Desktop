package HelperClasses;

import javafx.application.Platform;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Communicator {
    private final int SERVER_PORT;
    private final String SERVER_IP;

    private PrintWriter output;
    private BufferedReader input;

    private String status;

    ServerSocket serverSocket;
    Thread WaitingForConnection_Thread = null;

    //HelperClasses.Controller
    Controller controller;

    public Communicator(Controller controller) {
        this.SERVER_PORT = 8080;
        this.SERVER_IP = findServerIP();
        this.controller = controller;
        status = "Not Connected";
    }

    public void startConnecting(){
        WaitingForConnection_Thread = new Thread(new WaitingForConnection_Thread());
        WaitingForConnection_Thread.start();
    }

    public String findServerIP(){
        //Get IP - Address
        try {
            String address = InetAddress.getLocalHost().toString();
            // hostname = ip.getHostName();
            // System.out.println(address);
            return address.split("/")[1];

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "Not found";
    }


    public void sendMessage(String message){
        if (!message.isEmpty()) {
            new Thread(new SendMessage_Thread(message)).start();
        }
    }

    public void changeController(Controller controller){
        this.controller = controller;
    }

    public String getIP() {
        return SERVER_IP;
    }

    public String getPort() {
        return String.valueOf(SERVER_PORT);
    }

    public String getStatus(){
        return status;
    }



    //* Thread 1 *//
    class WaitingForConnection_Thread implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                //Waiting for connection
                //Run in Main Thread (change UI)
                Runnable updater = () -> {
                    status = "server: _Info:Not Connected";
                    controller.incomingMessage(status);
                };

                // UI update is run on the Application thread
                Platform.runLater(updater);

                //Connecting Socket
                try {
                    socket = serverSocket.accept();
                    output = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //Run in Main Thread (change UI)
                    Runnable updater2 = () -> {
                        status = "server: _Info:Connected";
                        controller.incomingMessage(status);
                    };
                    // UI update is run on the Application thread
                    Platform.runLater(updater2);
                    new Thread(new GetMessage_Thread()).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //* Thread 2 *//
    class GetMessage_Thread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        //Run in Main Thread (change UI)
                        Runnable updater = () -> {
                            status = message; //INCOMING MESSAGE !
                            controller.incomingMessage(status);
                        };
                        // UI update is run on the Application thread
                        Platform.runLater(updater);
                    } else {
                        System.out.println("reload");
                        WaitingForConnection_Thread = new Thread(new WaitingForConnection_Thread());
                        WaitingForConnection_Thread.start();
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("Error");
                    e.printStackTrace();
                }
            }
        }

    }

    //* Thread 3 *//
    class SendMessage_Thread implements Runnable {
        private final String message;
        SendMessage_Thread(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.println(message);
            //output.flush(); //auto flush
            //Run in Main Thread (change UI)
           /* Runnable updater = () -> {
                status = status + "server: " + message + "\n";
                controller.incomingMessage(status);
            };
            // UI update is run on the Application thread
            Platform.runLater(updater); */
            System.out.println("Server - message send: '"+ message+ "'.");
        }
    }

}

