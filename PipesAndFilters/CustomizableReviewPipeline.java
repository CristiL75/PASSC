import java.util.*;
import java.util.concurrent.*;

class ReviewMessage {
    String username;
    String product;
    String reviewText;
    String attachment;

    public ReviewMessage(String username, String product, String reviewText, String attachment) {
        this.username = username;
        this.product = product;
        this.reviewText = reviewText;
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return username + ", " + product + ", " + reviewText + ", " + attachment;
    }
}

class ReviewPipeline {
    static final ReviewMessage END_MESSAGE = new ReviewMessage("END", "", "", "");
}

interface Filter {
    void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue);
}

// Filtrare profanități
class CheckProfanitiesFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                if (!message.reviewText.contains("@#$%")) {
                    outputQueue.put(message);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Verificare cumpărător certificat
class CheckBuyerFilter implements Filter {
    private final Map<String, String> buyers;

    public CheckBuyerFilter(Map<String, String> buyers) {
        this.buyers = buyers;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                if (buyers.getOrDefault(message.username, "").equals(message.product)) {
                    outputQueue.put(message);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Redimensionare imagini (transformă numele fișierului în litere mici)
class ResizeImagesFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                if (message.attachment != null) {
                    message.attachment = message.attachment.toLowerCase();
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Filtrare propagandă politică
class CheckPoliticalPropagandaFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                if (message.reviewText != null && (message.reviewText.contains("+++") || message.reviewText.contains("---"))) {
                    continue;
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Detectare sentiment
class SentimentDetectionFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                int upperCaseCount = 0, lowerCaseCount = 0;
                for (char c : message.reviewText.toCharArray()) {
                    if (Character.isUpperCase(c)) upperCaseCount++;
                    else if (Character.isLowerCase(c)) lowerCaseCount++;
                }
                if (upperCaseCount > lowerCaseCount) {
                    message.reviewText += "+";
                } else if (lowerCaseCount > upperCaseCount) {
                    message.reviewText += "-";
                } else {
                    message.reviewText += "=";
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Configurația clientului
class ClientConfig {
    boolean filterProfanities;
    boolean filterCertifiedBuyers;
    boolean resizeImages;
    boolean filterPoliticalPropaganda;
    boolean detectSentiment;

    public ClientConfig(boolean filterProfanities, boolean filterCertifiedBuyers, boolean resizeImages, 
                        boolean filterPoliticalPropaganda, boolean detectSentiment) {
        this.filterProfanities = filterProfanities;
        this.filterCertifiedBuyers = filterCertifiedBuyers;
        this.resizeImages = resizeImages;
        this.filterPoliticalPropaganda = filterPoliticalPropaganda;
        this.detectSentiment = detectSentiment;
    }
}

// Construcția pipeline-ului de filtrare în funcție de configurația clientului
class ReviewPipelineBuilder {
    public static BlockingQueue<ReviewMessage> buildPipeline(ClientConfig config, Map<String, String> buyers, ExecutorService executor) {
        final BlockingQueue<ReviewMessage> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> currentQueue = inputQueue;
        
        if (config.filterProfanities) {
            final BlockingQueue<ReviewMessage> nextQueue = new LinkedBlockingQueue<>();
            executor.execute(() -> new CheckProfanitiesFilter().process(currentQueue, nextQueue));
            currentQueue = nextQueue;
        }
        if (config.filterCertifiedBuyers) {
            final BlockingQueue<ReviewMessage> nextQueue = new LinkedBlockingQueue<>();
            executor.execute(() -> new CheckBuyerFilter(buyers).process(currentQueue, nextQueue));
            currentQueue = nextQueue;
        }
        if (config.resizeImages) {
            final BlockingQueue<ReviewMessage> nextQueue = new LinkedBlockingQueue<>();
            executor.execute(() -> new ResizeImagesFilter().process(currentQueue, nextQueue));
            currentQueue = nextQueue;
        }
        if (config.filterPoliticalPropaganda) {
            final BlockingQueue<ReviewMessage> nextQueue = new LinkedBlockingQueue<>();
            executor.execute(() -> new CheckPoliticalPropagandaFilter().process(currentQueue, nextQueue));
            currentQueue = nextQueue;
        }
        if (config.detectSentiment) {
            final BlockingQueue<ReviewMessage> nextQueue = new LinkedBlockingQueue<>();
            executor.execute(() -> new SentimentDetectionFilter().process(currentQueue, nextQueue));
            currentQueue = nextQueue;
        }
        return inputQueue;
    }
}

// Aplicația principală
class ConcurrentPipesFilters {
    public static void main(String[] args) throws InterruptedException {
        Map<String, String> buyers = new HashMap<>();
        buyers.put("John", "Laptop");
        buyers.put("Mary", "Phone");
        buyers.put("Ann", "Book");

        Map<String, ClientConfig> clientConfigs = new HashMap<>();
        clientConfigs.put("ShopA", new ClientConfig(true, false, true, true, true));
        clientConfigs.put("ShopB", new ClientConfig(false, true, false, false, true));

        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<ReviewMessage> messages = Arrays.asList(
            new ReviewMessage("John", "Laptop", "ok", "PICTURE"),
            new ReviewMessage("Mary", "Phone", "@#$%", "IMAGE"),
            new ReviewMessage("Ann", "Book", "So GOOD", "Image")
        );

        for (String client : clientConfigs.keySet()) {
            System.out.println("Processing reviews for " + client + "...");
            BlockingQueue<ReviewMessage> pipeline = ReviewPipelineBuilder.buildPipeline(clientConfigs.get(client), buyers, executor);
            for (ReviewMessage message : messages) {
                pipeline.put(message);
            }
            pipeline.put(ReviewPipeline.END_MESSAGE);
            Thread.sleep(1000);
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }
}