import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png");

    public List<String> getValidPaths() {
        return validPaths;
    }

    public void start(int port) {

        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final var out = new BufferedOutputStream(socket.getOutputStream());

                    doSmth(in, out);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void doSmth( BufferedReader in, BufferedOutputStream out) throws IOException {
        System.out.println("-- do smth");
        var requestLine = in.readLine();
        var parts = requestLine.split(" ");
        if (parts.length != 3)
            return;

        var path = parts[1];
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found \r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return;
        }

        var filePath = Path.of(".", "public", path);
        var mimeType = Files.probeContentType(filePath);
        var length = Files.size(filePath);

        out.write((
                "HTTP/1.1 200 OK \r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();

    }
}
