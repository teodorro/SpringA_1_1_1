import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Client {
    private Scanner scanner = new Scanner(System.in);
    private String name;
    private String tmpDir = "tmp";
    private List<String> validRequestMethods = List.of("GET", "POST");
    private String fileRequested  = "";
    private int contentLength;
    private int responseLineLength;
    private static final int PORT = 9999;


    public Client(String name) {
        this.name = name;
    }


    public static void main(String[] args) {
        Random rand = new Random(System.currentTimeMillis());
        (new Client("CLIENT_" + rand.nextInt(1000))).start("localhost", PORT);
    }

    public void start(String hostname, int port){
        try {
            Socket socket = new Socket(hostname, port);

                try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                     BufferedInputStream in = new BufferedInputStream(socket.getInputStream())
                ){
                    while (true) {
                        boolean res = sendRequest(out);
                        if (!res)
                            continue;
                        res = receiveInfo(in);
                        if (!res)
                            continue;
                        res = transferFile(in);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean transferFile(BufferedInputStream in) {
        if (!Files.isDirectory(Path.of(".", tmpDir))) {
            try {
                Files.createDirectory(Path.of(".", tmpDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        var filePath = Path.of(".", tmpDir, "\\" + name + "." + fileRequested.substring(1));
        try (FileOutputStream fs = new FileOutputStream(filePath.toString())){
            var content = in.readNBytes(contentLength);
            fs.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean receiveInfo(BufferedInputStream in) {
        try {
            final var limit = 4096;
            final var buffer = new byte[limit];
            in.mark(limit);

            var read = in.read(buffer);
            var responseLines = getStructeredResponse(buffer);

            if (responseLines.isEmpty())
                return false;
            if (!responseLines.get(0).contains("200 OK"))
                return false;

            contentLength = getContentLength(responseLines.stream().filter(x -> x.contains("Content-Length")).findFirst().get());
            if (contentLength < 0)
                return false;

            var lastStr = responseLines.stream().filter(x -> x.contains("Connection: close")).findFirst().get();
            int ind = responseLines.indexOf(lastStr);
            responseLineLength = responseLines.stream().limit(ind + 1).map(x -> x.length()).reduce(0, Integer::sum);

            for (int i = 0; i < ind + 1; i++)
                System.out.println(responseLines.get(i).trim());

            in.reset();
            in.skip(responseLineLength);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private List<String> getStructeredResponse(byte[] buffer) {
        var lines = new ArrayList<byte[]>();
        int from = 0;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == '\r' && i < buffer.length - 1 && buffer[i + 1] == '\n') {
                lines.add(Arrays.copyOfRange(buffer, from, i + 2));
                from = i + 2;
                ++i;
            }
        }

        var responseLines = new ArrayList<String>();
        for (var line: lines){
            responseLines.add(new String(line));
        }
        return responseLines;
    }


    private boolean sendRequest(PrintWriter out) {
        try {
            System.out.println("Enter request...");
//            String requestLine = scanner.nextLine();
            String requestLine = "GET /messages?last=10&first=5 HTTP1.1";
            System.out.println(requestLine + " // hardcoded to make it easier to test. Press enter...");
            String _a = scanner.nextLine();
            out.println(requestLine);

            var request = requestLine.split(" ");
            if (validRequestMethods.contains(request[0])){
                fileRequested = request[1];
                if (fileRequested.contains("?"))
                    fileRequested = fileRequested.substring(0, fileRequested.indexOf('?'));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
}
