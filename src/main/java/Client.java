import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Client {
    private SocketChannel socketChannel;
    private ByteBuffer inputBuffer;
    private Scanner scanner = new Scanner(System.in);
    private String name;
    private String path = "tmp";
    private List<String> legalRequestCommands = List.of("GET");
    private String fileRequested;


    public Client(String name) {
        this.name = name;
    }


    public static void main(String[] args) {
        Random rand = new Random(System.currentTimeMillis());
        (new Client("CLIENT_" + rand.nextInt(1000))).start("localhost", Main.PORT);
    }

    public void start(String hostname, int port) {
        openChannel(hostname, port);
        startTransfer();
    }

    private void openChannel(String ip, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(socketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputBuffer = ByteBuffer.allocate(2 << 10);
    }

    private void startTransfer() {
        while (socketChannel.isConnected()) {
            boolean res = sendRequest();
            if (!res)
                continue;
            getResponse();
            System.out.println("");
        }
        stopConnection();
        System.out.println(name + " off");
    }

    private boolean sendRequest() {
        System.out.println(name + ": Enter path...");

        String requestLine = scanner.nextLine();
//        String requestLine = "GET /index.html HTTP/1.1" + name;

        if (requestLine.isBlank())
            return false;

        switch (validateRequest(requestLine)){
            case ILLEGAL_REQUEST_COMMAND:
                System.out.println("Error: illegal request command");
                return false;
            case WRONG_NUMBER_PARTS:
                System.out.println("Error: wrong command syntax");
                return false;
        }

        fileRequested = requestLine.split(" ")[1];

        if (!socketChannel.isOpen())
            return false;
        try {
            socketChannel.write(ByteBuffer.wrap(requestLine.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private RequestAnswer validateRequest(String requestLine) {
        var parts = requestLine.split(" ");
        if (parts.length != 3)
            return RequestAnswer.WRONG_NUMBER_PARTS;

        if (!legalRequestCommands.contains(parts[0]))
            return RequestAnswer.ILLEGAL_REQUEST_COMMAND;

        return RequestAnswer.OK;
    }

    private boolean getResponse() {
        String responseLine = getContentInfo();

        if (responseLine == null)
            return false;
        System.out.println(responseLine);

        if (!responseLine.contains("200 OK"))
            return false;

        int length = getContentLength(responseLine);
        if (length < 0)
            return false;

        writeFile(length);

        return true;
    }

    private void writeFile(int length) {
        try {
            var filePath = Path.of(".", "tmp", "\\" + name + "." + fileRequested.substring(1));
            FileChannel fileChannel = FileChannel.open(filePath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
            int res = 0;
            int counter = 0;
            do{
                inputBuffer.clear();
                res = socketChannel.read(inputBuffer);
                inputBuffer.flip();
                if (res > 0) {
                    fileChannel.write(inputBuffer);
                    counter += res;
                }
            } while (res > 0  && counter < length);
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getContentInfo() {
        int bytesCount = 0;
        try {
            do {
                bytesCount = socketChannel.read(inputBuffer);
                if (bytesCount == 0)
                    inputBuffer.clear();
            } while (bytesCount == 0);
        } catch (IOException e) {
            e.printStackTrace();
            inputBuffer.clear();
            return null;
        }
        String responseLine = new String(inputBuffer.array(), 0, bytesCount, StandardCharsets.UTF_8).trim();
        inputBuffer.clear();
        inputBuffer.flip();
        return responseLine;
    }

    private int getContentLength(String responseLine) {
        try {
            if (!responseLine.contains("\r\n") || !responseLine.contains(" ") || !responseLine.contains("Content-Length")){
                System.out.println("Illegal content info structure");
                return -1;
            }
            String[] lines = responseLine.split("\r\n");
            var lengthLineOptional = Arrays.stream(lines).filter(x -> x.contains("Content-Length")).findFirst();
            String lengthLine = "";
            if (!lengthLineOptional.isEmpty())
                lengthLine = lengthLineOptional.get();
            int length = Integer.parseInt(lengthLine.split(" ")[1]);
            return length;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void stopConnection() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

}
