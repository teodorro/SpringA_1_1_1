import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocketChannel serverChannel;
    private List<ClientHandler> clients = new ArrayList<>();
    public ExecutorService es = Executors.newFixedThreadPool(64);

    private List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png");

    public List<String> getValidPaths() {
        return validPaths;
    }

    public static void main(String[] args) {
        (new Server()).start("localhost", Main.PORT);
    }


    public void start(String hostname, int port) {
        openChannel(hostname, port);
        while (true) {
            try {
                SocketChannel socketChannel = serverChannel.accept();
                es.submit(() -> {
                    ClientHandler clientHandler = new ClientHandler(socketChannel, validPaths);
                    clientHandler.start();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openChannel(String ip, int port)  {
        serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(ip, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
