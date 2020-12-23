import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClientHandler extends Thread {
    private ByteBuffer inputBuffer;
    private Socket socket;
    private List<String> validPaths;
    private String requestLine;


    public ClientHandler(Socket socket, List<String> validPaths) {
        this.socket = socket;
        this.validPaths = validPaths;

        startProcess();
    }

    private void startProcess(){
        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());

            var requestLine = in.readLine();
            var parts = requestLine.split(" ");
            if (parts.length != 3)
                return;

            var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found \r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n"
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
                            "Connection: close\r\n"
            ).getBytes());

            Files.copy(filePath, out);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
