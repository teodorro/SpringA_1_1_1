import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Client2 {
    private Scanner scanner = new Scanner(System.in);
    private String name;
    private String path = "tmp";
    private List<String> legalRequestCommands = List.of("GET");
    private String fileRequested;


    public Client2(String name) {
        this.name = name;
    }


    public static void main(String[] args) {
        Random rand = new Random(System.currentTimeMillis());
        (new Client2("CLIENT_" + rand.nextInt(1000))).start("localhost", Main.PORT);
    }

    public void start(String hostname, int port){
        try {
            Socket socket = new Socket(hostname, port);
            try (
//                    BufferedReader in = new BufferedReader(
//                    new InputStreamReader(socket.getInputStream()));
                    BufferedInputStream in = new BufferedInputStream(
                            socket.getInputStream());
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream()), true);
                 Scanner scanner = new Scanner(System.in)) {
                String msg;
                while (true) {
                    System.out.println("Enter request...");
                    msg = scanner.nextLine();
                    out.println(msg);
                    if ("end".equals(msg)) break;
                    while(true) {
                        // TODO: read file info, then read file content
                        String responseLine = "in.readLine()";
                        System.out.println("SERVER: " + responseLine);
                        if (responseLine.contains("Connection: close"))// || responseLine.equals(""))
                            break;
//                        if (responseLine.contains("Connection: close")){
//                            var filePath = Path.of(".", "public", path + "/index.html");
//                            byte[] buffer = new byte[in..available()];
//
//                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
