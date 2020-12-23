import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClientHandler extends Thread {
    private Socket socket;
    private List<String> validPaths;


    public ClientHandler(Socket socket, List<String> validPaths) {
        this.socket = socket;
        this.validPaths = validPaths;

        startProcess();
    }

    private void startProcess(){
        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());

//            var client = getHttpClient();
            while (socket.isConnected()) {
                var requestLine = in.readLine();

                String[] parts = getParts(out, requestLine);
                if (parts == null) continue;

                var path = parts[1];
                if (!checkPath(out, path))
                    continue;

                doURLEncodedUtils(path);

                var filePath = Path.of(".", "public", path);
                var mimeType = Files.probeContentType(filePath);
                var length = Files.size(filePath);

                writeData(out, filePath, mimeType, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkPath(BufferedOutputStream out, String path) throws IOException {
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found \r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n"
            ).getBytes());
            out.flush();
            return false;
        }
        return true;
    }

    private String[] getParts(BufferedOutputStream out, String requestLine) throws IOException {
        var parts = requestLine.split(" ");
        if (parts.length != 3) {
            out.write((
                    "HTTP/1.1 400 Illegal request. Wrong number of parts \r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n"
            ).getBytes());
            out.flush();
            return null;
        }
        return parts;
    }

    private void doURLEncodedUtils(String path) {
        List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(path, Charset.forName("utf-8"));
    }

    private void writeData(BufferedOutputStream out, Path filePath, String mimeType, long length) throws IOException {
        out.write((
                "HTTP/1.1 200 OK \r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n"
        ).getBytes());

        Files.copy(filePath, out);
        out.flush();
    }

//    private CloseableHttpClient getHttpClient() {
//        return HttpClientBuilder.create()
//                .setDefaultRequestConfig(RequestConfig.custom()
//                        .setConnectTimeout(5000)
//                        .setSocketTimeout(30000)
//                        .setRedirectsEnabled(false)
//                        .build())
//                .build();
//    }
//
//    private byte[] getData(CloseableHttpClient httpClient, String url) throws IOException {
//        HttpGet request = new HttpGet(url);
//        CloseableHttpResponse response = httpClient.execute(request);
//        return response.getEntity().getContent().readAllBytes();
//    }

}
