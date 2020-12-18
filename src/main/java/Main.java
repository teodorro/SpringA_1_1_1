import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final int PORT = 9999;
    public static void main(String[] args) throws IOException {

        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            es.submit(() -> new Server().start(Main.PORT));
            es.submit(() -> new Client().start(Main.PORT));
            es.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }


    }
}
