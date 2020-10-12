package ch5_building_blocks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class PreLoader {
    private final FutureTask<ProductInfo> futureTask = new FutureTask<>(() -> loadProducts());
    private final Thread thread = new Thread(futureTask);

    public void start(){
        thread.start();
    }

    public ProductInfo get() throws InterruptedException {
        try {
            return futureTask.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ProductInfo loadProducts() {
        //This will be the long running/time consuming task.
        return null;
    }
}

class ProductInfo {
}
