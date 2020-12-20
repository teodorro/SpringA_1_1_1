import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;

public class ClientHandler extends Thread {
    private ByteBuffer inputBuffer;
    private SocketChannel socketChannel;
    private List<String> validPaths;
    private String requestLine;


    public ClientHandler(SocketChannel socketChannel, List<String> validPaths) {
        this.socketChannel = socketChannel;
        this.validPaths = validPaths;

        inputBuffer = ByteBuffer.allocate(2 << 10);
        initStream();
    }

    private void initStream() {
        while (socketChannel.isConnected()) {
            RequestAnswer res = readRequest();
            if (res == RequestAnswer.EMPTY)
                continue;
            if (res != RequestAnswer.OK)
                break;
            sendRespond();
        }
    }


    private RequestAnswer readRequest() {
        int bytesCount = 0;
        if (!socketChannel.isOpen())
            return RequestAnswer.SOCKET_CHANNEL_NOT_OPENED;
        try {
            bytesCount = socketChannel.read(inputBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            inputBuffer.clear();
            return RequestAnswer.ERROR_READING;
        }
        if (bytesCount == 0) {
            requestLine = "";
            inputBuffer.clear();
            return RequestAnswer.EMPTY;
        }

        requestLine = new String(inputBuffer.array(), 0, bytesCount, StandardCharsets.UTF_8).trim();
        System.out.println("SERVER: " + requestLine);

        inputBuffer.clear();
        return RequestAnswer.OK;
    }

    private RequestAnswer sendRespond() {
        var parts = requestLine.split(" ");
        if (parts.length != 3)
            return RequestAnswer.WRONG_NUMBER_PARTS;

        var path = parts[1];
            RequestAnswer notFound = checkNotFound(path);
        if (notFound != null)
            return notFound;

        Path filePath = writeFileInfo(path);

        writeFile(filePath);

        return RequestAnswer.OK;
    }

    private RequestAnswer checkNotFound(String path) {
        try {
            if (!validPaths.contains(path)) {
                socketChannel.write(ByteBuffer.wrap((
                        "HTTP/1.1 404 Not Found \r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes()));
                return RequestAnswer.NOT_FOUND;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Path writeFileInfo(String path) {
        var filePath = Path.of(".", "public", path);
        try {
            var mimeType = Files.probeContentType(filePath);
            var length = Files.size(filePath);
            socketChannel.write(ByteBuffer.wrap((
                    "HTTP/1.1 200 OK \r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    private void writeFile(Path filePath) {
        try {
            FileChannel fileChannel = FileChannel.open(filePath, EnumSet.of(StandardOpenOption.READ));
            int res = 0;
            do {
                inputBuffer.clear();
                res = fileChannel.read(inputBuffer);
                inputBuffer.flip();
                if (res > 0)
                    socketChannel.write(inputBuffer);
            } while (res > 0);
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
