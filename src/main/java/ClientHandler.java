import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.javatuples.Pair;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ClientHandler extends Thread {
    private Socket socket;
    private List<String> validPaths;
    private Map<Pair<String, String>, Handler> handlers;
    private List<String> validRequestMethods;


    public ClientHandler(Socket socket, List<String> validPaths, List<String> validRequestMethods, Map<Pair<String, String>, Handler> handlers) {
        this.socket = socket;
        this.validPaths = validPaths;
        this.handlers = handlers;
        this.validRequestMethods = validRequestMethods;

        startProcess();
    }

    private void startProcess(){
        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());

            while (socket.isConnected()) {
                var requestLine = in.readLine();

                var request = makeRequest(requestLine, out);
                if (request == null)
                    continue;

                var r = handlers.keySet().stream()
                        .filter(x -> x.getValue0().equals(request.getMethod()) && x.getValue1().equals(request.getPath()))
                        .findFirst();
                if (r.isPresent()) {
                    var handler = handlers.get(r.get());
                    handler.Handle(request, out);
                } else
                    defaultHandler(request, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void defaultHandler(Request request, BufferedOutputStream out) throws IOException {
        var filePath = Path.of(".", "public", request.getPath());
        var mimeType = Files.probeContentType(filePath);
        var length = Files.size(filePath);

        writeData(out, filePath, mimeType, length);
    }

    private Request makeRequest(String requestLine, BufferedOutputStream out) {
        Request request = new Request();
        try {
            String[] parts = getParts(out, requestLine);
            if (parts == null) return null;

            var method = parts[0];
            if (!checkMethod(out, method))
                return null;
            request.setMethod(method);

            var path = parts[1];
            var shortPath = path.contains("?")
                    ? path.substring(0, path.indexOf('?'))
                    : path;
            if (!checkPath(out, shortPath))
                return null;
            request.setPath(shortPath);

            var queryParams = getQueryParams(path);
            request.setParameters(queryParams);

            var filePath = Path.of(".", "public", shortPath);
            var mimeType = Files.probeContentType(filePath);
            request.setHeader(mimeType);

            // TODO content

        } catch (IOException e) {
            e.printStackTrace();
            try {
                out.write((
                        "HTTP/1.1 500 Error processing \r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n"
                ).getBytes());
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return null;
        }
        return request;
    }

    private boolean checkMethod(BufferedOutputStream out, String method) throws IOException {
        if (!validRequestMethods.contains(method)) {
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

    private List<NameValuePair> getQueryParams(String path) {
        var valuePairs = path.substring(path.indexOf('?') + 1);
        return URLEncodedUtils.parse(valuePairs, Charset.forName("utf-8"));
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

}
