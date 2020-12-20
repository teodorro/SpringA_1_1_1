import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final int PORT = 9999;
    public static void main(String[] args) throws IOException {

        ExecutorService es = Executors.newFixedThreadPool(64);
        try {
            es.submit(() -> new Server().start("localhost", Main.PORT));
            es.submit(() -> new Client("CLIENT_1").start("localhost", Main.PORT));
            es.submit(() -> new Client("CLIENT_2").start("localhost", Main.PORT));
            es.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }


    }
}
