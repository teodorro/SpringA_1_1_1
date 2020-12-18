import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public void start(int port) {
        try {
            Socket socket = new Socket("127.0.0.1", port);
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream()), true);
                 Scanner scanner = new Scanner(System.in)) {
                String msg;
                while (true) {
//                    System.out.println("Enter address...");
//                    msg = scanner.nextLine();
                    msg = "GET /index.html HTTP/1.1";
                    System.out.println(msg);
                    out.println(msg);
                    if ("end".equals(msg)) break;
                    while (true) {
                        var str = in.readLine();
                        if (str == null)
                            break;
                        System.out.println("SERVER: " + str);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
