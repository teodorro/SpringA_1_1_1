import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Handler1 implements Handler{
    @Override
    public void Handle(Request request, BufferedOutputStream responseStream) {
        int first = -1;
        var pairFirst = request.getParameters().stream().filter(x -> x.getName().equals("first")).findFirst();
        if (pairFirst.isPresent()){
            try {
                var v = pairFirst.get().getValue();
                first = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        int last = -1;
        var pairLast = request.getParameters().stream().filter(x -> x.getName().equals("last")).findFirst();
        if (pairLast.isPresent()){
            try {
                var v = pairLast.get().getValue();
                last = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        var filePath = Path.of(".", "public", request.getPath());
        try {
            var mimeType = Files.probeContentType(filePath);

            var responseLine = "HTTP/1.1 200 OK \r\n" +
                    "Content-Type: " + mimeType + "\r\n" ;

            var messagesPart = "";
            try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))){
                int count = 0;
                while (true) {
                    var line = br.readLine();
                    if (line == null)
                        break;
                    if (count >= first && count < last)
                        messagesPart = messagesPart + line + "\r\n";
                    ++count;
                }
            }
            responseLine = responseLine + messagesPart
                    + "Content-Length: " + messagesPart.length() + "\r\n"
                    + "Connection: close\r\n";

            responseStream.write(responseLine.getBytes());

            responseStream.write(messagesPart.getBytes());
            responseStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
