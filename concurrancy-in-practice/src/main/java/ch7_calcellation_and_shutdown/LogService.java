package ch7_calcellation_and_shutdown;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Time;
import java.util.concurrent.*;

public class LogService {
        private final BlockingQueue<String> queue;
        private final LoggerThread logger;

        private boolean isShutDown;
        private int reservation;

        public LogService(BlockingQueue<String> queue, LoggerThread logger) throws IOException {
            this.queue = new LinkedBlockingDeque<>(10);
            this.logger = new LoggerThread(new PrintWriter(File.createTempFile("logFile","log")));
        }

        public void start(){
            logger.start();
        }

        public void stop(){
            synchronized (this){
                isShutDown = true;
            }
            logger.interrupt();
        }

        public void log(String message) throws InterruptedException {
            synchronized (this){
                if (isShutDown){
                    throw new IllegalStateException();
                }
                reservation++;
            }
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
                        synchronized (LogService.this){
                            if (isShutDown && reservation == 0){
                                break;
                            }
                        }
                        String msg = queue.take();
                        synchronized (LogService.this){
                            --reservation;
                        }
                        writer.println(msg);
                    } catch (InterruptedException e) {
                        //Ignore the exception
                    }finally {
                        writer.close();
                    }
                }
            }
        }
    }




