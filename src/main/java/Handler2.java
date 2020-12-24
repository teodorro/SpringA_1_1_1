import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Handler2 implements Handler {
    @Override
    public void Handle(Request request, BufferedOutputStream responseStream) {
        var filePath = Path.of(".", "public", request.getPath());
        try {
            var mimeType = "empty response!";
            var length = 0;

            responseStream.write((
                    "HTTP/1.1 200 OK \r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n"
            ).getBytes());

            responseStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
