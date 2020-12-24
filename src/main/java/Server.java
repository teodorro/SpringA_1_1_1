import org.javatuples.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final static String HOSTNAME = "localhost";
    private static final int PORT = 9999;
    private ServerSocket serverSocket;
    public ExecutorService es = Executors.newFixedThreadPool(64);
    private Map<Pair<String, String>, Handler> handlers = new HashMap<>();
    private List<String> validRequestMethods = List.of("GET", "POST");
    private List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/messages");

    public List<String> getValidPaths() {
        return validPaths;
    }

    public static void main(String[] args) {
        Server server = new Server();
        addHandlers(server);
        server.start(HOSTNAME, PORT);
    }

    private static void addHandlers(Server server) {
        server.addHandler("GET", "/messages", new Handler1());
        server.addHandler("POST", "/messages", new Handler2());
    }


    public void start(String hostname, int port) {
        openChannel(hostname, port);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                es.submit(() -> {
                    ClientHandler clientHandler = new ClientHandler(socket, validPaths, validRequestMethods, handlers);
                    clientHandler.start();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openChannel(String ip, int port)  {
        serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler){
        if (!validRequestMethods.contains(method))
            System.out.println("Error adding handler. Invalid method");
        handlers.put(new Pair<>(method, path), handler);
    }
}
