import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Client2 {
    private Scanner scanner = new Scanner(System.in);
    private String name;
    private String path = "tmp";
    private List<String> legalRequestCommands = List.of("GET");
    private String fileRequested  = "/asd.txt";
    private int contentLength;
    private int responseLineLength;


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
            while (true) {

                try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                     BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                     BufferedReader br = new BufferedReader(new InputStreamReader(in))
                ){
                    boolean res = sendRequest(out);
                    if (!res)
                        continue;
//                    res = receiveInfo(br);
//                    if (!res)
//                        continue;
                    res = transferFile(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }


//                try (
//                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
//                        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
//                        Scanner scanner = new Scanner(System.in)) {
//
//                    String msg;
//                    System.out.println("Enter request...");
//                    msg = scanner.nextLine();
//                    out.println(msg);
//                    if ("end".equals(msg)) break;
//                    while (true) {
//                        String responseLine = "in.readLine()";
//                        System.out.println("SERVER: " + responseLine);
//                        if (responseLine.contains("Connection: close"))// || responseLine.equals(""))
//                            break;
////                        if (responseLine.contains("Connection: close")){
////                            var filePath = Path.of(".", "public", path + "/index.html");
////                            byte[] buffer = new byte[in..available()];
////
////                        }
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private boolean transferFile(Socket socket) {
//        var filePath = Path.of(".", "tmp", "\\" + name + "." + fileRequested.substring(1));
//        try (BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
//        FileOutputStream fs = new FileOutputStream(filePath.toString())){
//            int res = 0;
//            int counter = 0;
//            byte[] buffer = new byte[4096];
//            do{
//                res = in.read(buffer);
//                if (res > 0) {
//                    fs.write(buffer);
//                    counter += res;
//                }
//            } while (res > 0);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }

    private boolean transferFile(BufferedInputStream in) {
        var filePath = Path.of(".", "tmp", "\\" + name + "." + fileRequested.substring(1));
        try (FileOutputStream fs = new FileOutputStream(filePath.toString())){
            int res = 0;
            int counter = 0;
            byte[] buffer = new byte[9];
            do{
                res = in.read(buffer);
                if (res > 0) {
                    fs.write(buffer);
                    counter += res;
                }
            } while (res > 0);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

//    private boolean receiveInfo(Socket socket) {
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
//            String responseLine = "";
//            do {
//                responseLine += in.readLine();
//                responseLine += "\r\n";
//            } while (responseLine.contains("Connection: close"));
//
//            if (responseLine == null)
//                return false;
//            System.out.println(responseLine);
//
//            if (!responseLine.contains("200 OK"))
//                return false;
//
//            contentLength = getContentLength(responseLine);
//            if (contentLength < 0)
//                return false;
//            responseLineLength = responseLine.length();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }

    private boolean receiveInfo(BufferedReader in) {
        try {
            String responseLine = "";
            do {
                responseLine += in.readLine();
                responseLine += "\r\n";
            } while (!responseLine.contains("Connection: close"));

            if (responseLine == null)
                return false;
            System.out.println(responseLine);

            if (!responseLine.contains("200 OK"))
                return false;

            contentLength = getContentLength(responseLine);
            if (contentLength < 0)
                return false;
            responseLineLength = responseLine.length();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

//    private boolean sendRequest(Socket socket) {
//        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)){
//            System.out.println("Enter request...");
//            String requestLine = scanner.nextLine();
//            out.println(requestLine);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }

    private boolean sendRequest(PrintWriter out) {
        try {
            System.out.println("Enter request...");
            String requestLine = scanner.nextLine();
            out.println(requestLine);
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
