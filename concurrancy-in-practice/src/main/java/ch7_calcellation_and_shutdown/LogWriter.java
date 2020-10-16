package ch7_calcellation_and_shutdown;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class LogWriter {
    private final BlockingQueue<String> queue;
    private final LoggerThread logger;

    public LogWriter(BlockingQueue<String> queue, LoggerThread logger) throws IOException {
        this.queue = new LinkedBlockingDeque<>(10);
        this.logger = new LoggerThread(new PrintWriter(File.createTempFile("logFile","log")));
    }

    public void start(){
        logger.start();
    }

    public void log(String message) throws InterruptedException {
        queue.put(message);
    }

    private class LoggerThread  extends Thread{
        private final PrintWriter writer;

        public LoggerThread(PrintWriter writer) {
            this.writer = writer;
        }
        @Override
        public void run() {
            while (true){
                try {
                    writer.println(queue.take());
                } catch (InterruptedException e) {
                    //Ignore the exception
                }finally {
                    writer.close();
                }
            }
        }
    }
}
