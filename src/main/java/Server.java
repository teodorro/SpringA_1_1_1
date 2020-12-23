import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    public ExecutorService es = Executors.newFixedThreadPool(64);
    private static final int PORT = 9999;

    private List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png");

    public List<String> getValidPaths() {
        return validPaths;
    }

    public static void main(String[] args) {
        (new Server()).start("localhost", PORT);
    }


    public void start(String hostname, int port) {
        openChannel(hostname, port);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                es.submit(() -> {
                    ClientHandler clientHandler = new ClientHandler(socket, validPaths);
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
}
