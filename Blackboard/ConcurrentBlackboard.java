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

class Blackboard {
    private final BlockingQueue<ReviewMessage> messages = new LinkedBlockingQueue<>();

    // Adauga un mesaj in Blackboard
    public void addMessage(ReviewMessage message) throws InterruptedException {
        messages.put(message);
    }

    // Preia un mesaj din Blackboard
    public ReviewMessage getMessage() throws InterruptedException {
        return messages.take(); // Folosește take() pentru blocare
    }
    // Verifica daca Blackboard-ul este gol
    public boolean isEmpty() {
        return messages.isEmpty();
    }
}

class ClientConfig {
    boolean resizeImages;
    boolean checkBuyer;
    boolean checkProfanities;
    boolean checkPoliticalPropaganda;
    boolean detectSentiment;
    boolean detectSentimentPlus;

    public ClientConfig(boolean resizeImages, boolean checkBuyer, boolean checkProfanities, boolean checkPoliticalPropaganda, boolean detectSentiment, boolean detectSentimentPlus) {
        this.resizeImages = resizeImages;
        this.checkBuyer = checkBuyer;
        this.checkProfanities = checkProfanities;
        this.checkPoliticalPropaganda = checkPoliticalPropaganda;
        this.detectSentiment = detectSentiment;
        this.detectSentimentPlus = detectSentimentPlus;
    }
}

class ReviewPipeline {
    static final ReviewMessage END_MESSAGE = new ReviewMessage("END", "", "", "");
}

interface Filter {
    void process(Blackboard blackboard);
}

class CheckProfanitiesFilter implements Filter {
    private final ClientConfig config;
    private long processingTime = 0; 
    private int processedMessages = 0;

    public CheckProfanitiesFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                ReviewMessage message = blackboard.getMessage();
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addMessage(message);
                    break;
                }

                // Verifica daca filtrul este activat si daca mesajul contine profanitati
                if (config.checkProfanities && message.reviewText.contains("@#$%")) {
                    continue; // Ignora mesajul daca contine profanitati
                }
                blackboard.addMessage(message);
                processedMessages++;
                long endTime = System.currentTimeMillis();
                processingTime += (endTime - startTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public long getProcessingTime() {
        return processingTime;
    }

    public int getProcessedMessages() {
        return processedMessages;
    }
}

class CheckBuyerFilter implements Filter {
    private final Map<String, String> buyers;
    private final ClientConfig config;
    private long processingTime = 0; 
    private int processedMessages = 0;

    public CheckBuyerFilter(Map<String, String> buyers, ClientConfig config) {
        this.buyers = buyers;
        this.config = config;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                ReviewMessage message = blackboard.getMessage();
                long startTime = System.currentTimeMillis();
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addMessage(message);
                    break;
                }

                // Verifica daca filtrul este activat si daca utilizatorul a cumparat produsul
                if (config.checkBuyer && !buyers.getOrDefault(message.username, "").equals(message.product)) {
                    continue; // Ignora mesajul daca utilizatorul nu a cumparat produsul
                }
                blackboard.addMessage(message);
                processedMessages++;
                long endTime = System.currentTimeMillis();
                processingTime += (endTime - startTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public long getProcessingTime() {
        return processingTime;
    }

    public int getProcessedMessages() {
        return processedMessages;
    }
}

class ResizeImagesFilter implements Filter {
    private final ClientConfig config;
    private long processingTime = 0; 
    private int processedMessages = 0;

    public ResizeImagesFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                ReviewMessage message = blackboard.getMessage();
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addMessage(message);
                    break;
                }

                // Redimensioneaza atasamentul daca filtrul este activat
                if (config.resizeImages && message.attachment != null) {
                    message.attachment = message.attachment.toLowerCase();
                }
                blackboard.addMessage(message);
                processedMessages++;
                long endTime = System.currentTimeMillis();
                processingTime += (endTime - startTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public long getProcessingTime() {
        return processingTime;
    }

    public int getProcessedMessages() {
        return processedMessages;
    }
}

class SentimentDetectionFilter implements Filter {
    private final ClientConfig config;
    private long processingTime = 0; 
    private int processedMessages = 0; 

    public SentimentDetectionFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                ReviewMessage message = blackboard.getMessage();
                long startTime = System.currentTimeMillis();
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addMessage(message);
                    break;
                }

                // Detecteaza sentimentul daca filtrul este activat
                if (config.detectSentiment && message.reviewText != null) {
                    int upperCaseCount = 0, lowerCaseCount = 0;
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c)) upperCaseCount++;
                        else if (Character.isLowerCase(c)) lowerCaseCount++;
                    }

                    // Adauga un simbol in functie de numarul de litere mari si mici
                    if (upperCaseCount > lowerCaseCount) message.reviewText += "+";
                    else if (lowerCaseCount > upperCaseCount) message.reviewText += "-";
                    else message.reviewText += "=";
                }
                blackboard.addMessage(message);
                processedMessages++;
                long endTime = System.currentTimeMillis();
                processingTime += (endTime - startTime); 
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public long getProcessingTime() {
        return processingTime;
    }

    public int getProcessedMessages() {
        return processedMessages;
    }
}

class CheckPoliticalPropagandaFilter implements Filter {
    private final ClientConfig config;
    private long processingTime = 0;
    private int processedMessages = 0; 

    public CheckPoliticalPropagandaFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                ReviewMessage message = blackboard.getMessage();
                long startTime = System.currentTimeMillis();
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addMessage(message);
                    break;
                }

          
                if (config.checkPoliticalPropaganda && 
                    (message.reviewText.contains("+++") || message.reviewText.contains("---"))) {
                    continue; 
                }
                blackboard.addMessage(message);
                processedMessages++;
                long endTime = System.currentTimeMillis();
                processingTime += (endTime - startTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public long getProcessingTime() {
        return processingTime;
    }

    public int getProcessedMessages() {
        return processedMessages;
    }
}

class SentimentDetectionPlusFilter implements Filter {
    private final ClientConfig config;
    private long processingTime = 0; 
    private int processedMessages = 0; 

    public SentimentDetectionPlusFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                ReviewMessage message = blackboard.getMessage();
                long startTime = System.currentTimeMillis();
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addMessage(message);
                    break;
                }

                // Detecteaza sentimentul cu etichete suplimentare daca filtrul este activat
                if (config.detectSentimentPlus && message.reviewText != null) {
                    int upperCaseCount = 0, lowerCaseCount = 0;
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c)) 
                            upperCaseCount++;
                        else if (Character.isLowerCase(c))
                             lowerCaseCount++;
                    }

                    // Adauga o eticheta in functie de sentiment
                    if (upperCaseCount > lowerCaseCount)
                         message.reviewText += " (Positive)";
                    else if (lowerCaseCount > upperCaseCount)
                         message.reviewText += " (Negative)";
                    else message.reviewText += " (Neutral)";
                }
                blackboard.addMessage(message);
                processedMessages++;
                long endTime = System.currentTimeMillis();
                processingTime += (endTime - startTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public long getProcessingTime() {
        return processingTime;
    }

    public int getProcessedMessages() {
        return processedMessages;
    }
}

class ConcurrentBlackboard {
    public static void main(String[] args) throws InterruptedException {
        Map<String, String> buyers = new HashMap<>();
        buyers.put("John", "Laptop");
        buyers.put("Mary", "Phone");
        buyers.put("Ann", "Book");

        ClientConfig client1Config = new ClientConfig(true, true, true, true, true, false);

        Blackboard blackboard = new Blackboard();

        ExecutorService executor = Executors.newFixedThreadPool(6);

        CheckProfanitiesFilter profanityFilter = new CheckProfanitiesFilter(client1Config);
        CheckBuyerFilter buyerFilter = new CheckBuyerFilter(buyers, client1Config);
        ResizeImagesFilter resizeFilter = new ResizeImagesFilter(client1Config);
        SentimentDetectionFilter sentimentFilter = new SentimentDetectionFilter(client1Config);
        CheckPoliticalPropagandaFilter propagandaFilter = new CheckPoliticalPropagandaFilter(client1Config);
        SentimentDetectionPlusFilter sentimentPlusFilter = new SentimentDetectionPlusFilter(client1Config);

        executor.execute(() -> profanityFilter.process(blackboard));
        executor.execute(() -> buyerFilter.process(blackboard));
        executor.execute(() -> resizeFilter.process(blackboard));
        executor.execute(() -> propagandaFilter.process(blackboard));
        executor.execute(() -> sentimentFilter.process(blackboard));
        executor.execute(() -> sentimentPlusFilter.process(blackboard));

        List<ReviewMessage> messages = Arrays.asList(
                new ReviewMessage("John", "Laptop", "ok", "PICTURE"),
                new ReviewMessage("Mary", "Phone", "@#$%", "IMAGE"),
                new ReviewMessage("Peter", "Phone", "GREAT", "ManyPictures"),
                new ReviewMessage("Ann", "Book", "So GOOD", "Image"),
                new ReviewMessage("Alice", "Tablet", "I love this +++", "TabletImage"),
                new ReviewMessage("Bob", "Laptop", "This is amazing ---", "LaptopImage")
        );

        long startTime = System.currentTimeMillis();

        for (ReviewMessage message : messages) {
            blackboard.addMessage(message);
        }

        blackboard.addMessage(ReviewPipeline.END_MESSAGE);


        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime; 

        List<ReviewMessage> processedMessages = new ArrayList<>();
        ReviewMessage message;
        while ((message = blackboard.getMessage()) != null) {
            if (message == ReviewPipeline.END_MESSAGE) break;
            processedMessages.add(message);
        }
        System.out.println(" Rezultate");
        System.out.println("Timpul total de procesare: " + totalTime + " ms");
        System.out.println("Throughput: " + (double) messages.size() / (totalTime / 1000.0) + " mesaje/secunda");

        System.out.println("\n Detalii Filtre ");
        System.out.println("1. CheckProfanitiesFilter:");
        System.out.println("   - Timp de procesare: " + profanityFilter.getProcessingTime() + " ms");
        System.out.println("   - Mesaje procesate: " + profanityFilter.getProcessedMessages());

        System.out.println("2. CheckBuyerFilter:");
        System.out.println("   - Timp de procesare: " + buyerFilter.getProcessingTime() + " ms");
        System.out.println("   - Mesaje procesate: " + buyerFilter.getProcessedMessages());

        System.out.println("3. ResizeImagesFilter:");
        System.out.println("   - Timp de procesare: " + resizeFilter.getProcessingTime() + " ms");
        System.out.println("   - Mesaje procesate: " + resizeFilter.getProcessedMessages());

        System.out.println("4. SentimentDetectionFilter:");
        System.out.println("   - Timp de procesare: " + sentimentFilter.getProcessingTime() + " ms");
        System.out.println("   - Mesaje procesate: " + sentimentFilter.getProcessedMessages());

        System.out.println("5. CheckPoliticalPropagandaFilter:");
        System.out.println("   - Timp de procesare: " + propagandaFilter.getProcessingTime() + " ms");
        System.out.println("   - Mesaje procesate: " + propagandaFilter.getProcessedMessages());

        System.out.println("6. SentimentDetectionPlusFilter:");
        System.out.println("   - Timp de procesare: " + sentimentPlusFilter.getProcessingTime() + " ms");
        System.out.println("   - Mesaje procesate: " + sentimentPlusFilter.getProcessedMessages());

        System.out.println("\n Mesaje procesate:");

        for (ReviewMessage msg : processedMessages) {
            System.out.println(msg);
        }
    }
} 