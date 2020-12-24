import java.io.BufferedOutputStream;

public interface Handler {
    void Handle(Request request, BufferedOutputStream responseStream);
}
