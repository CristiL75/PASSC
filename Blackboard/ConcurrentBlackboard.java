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
    private final List<BlockingQueue<ReviewMessage>> queues;  // Cozi pentru fiecare etapă
    
    public Blackboard(int numStages) {
        queues = new ArrayList<>();
        for (int i = 0; i < numStages; i++) {
            queues.add(new LinkedBlockingQueue<>());
        }
    }

    // Adaugă mesaj la o anumită etapă (coadă)
    public void addToStage(int stage, ReviewMessage message) throws InterruptedException {
        queues.get(stage).put(message);
    }

    // Preia mesaj de la o anumită etapă (coadă)
    public ReviewMessage getFromStage(int stage) throws InterruptedException {
        return queues.get(stage).poll(100, TimeUnit.MILLISECONDS); 
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
    private final int inputStage;   // Etapa (coada) de intrare
    private final int outputStage;  // Etapa (coada) de ieșire
    private long processingTime = 0; 
    private int processedMessages = 0;

    public CheckProfanitiesFilter(ClientConfig config, int inputStage, int outputStage) {
        this.config = config;
        this.inputStage = inputStage;
        this.outputStage = outputStage;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                // Preia mesaj din coada de intrare
                ReviewMessage message = blackboard.getFromStage(inputStage);
                if (message == null) continue;

                // Propagă mesajul de END către următoarea etapă
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addToStage(outputStage, message);
                    break;
                }

                // Procesare: elimină mesajele cu profanități
                if (config.checkProfanities && message.reviewText.contains("@#$%")) {
                    continue; // Nu adăuga mesajul în următoarea coadă
                }

                // Trimite mesajul la următoarea etapă
                blackboard.addToStage(outputStage, message);
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
    private final int inputStage;   // Etapa de intrare
    private final int outputStage;  // Etapa de ieșire
    private long processingTime = 0;
    private int processedMessages = 0; 

    public CheckBuyerFilter(Map<String, String> buyers, ClientConfig config, int inputStage, int outputStage) {
        this.buyers = buyers;
        this.config = config;
        this.inputStage = inputStage;
        this.outputStage = outputStage;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                // Preia mesaj din coada corespunzătoare etapei de intrare
                ReviewMessage message = blackboard.getFromStage(inputStage);
                if (message == null) continue;

                // Propagă mesajul de END către următoarea etapă
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addToStage(outputStage, message);
                    break;
                }

                // Verifică dacă utilizatorul a cumpărat produsul (doar dacă este activat în config)
                if (config.checkBuyer) {
                    String purchasedProduct = buyers.getOrDefault(message.username, "");
                    if (!purchasedProduct.equals(message.product)) {
                        continue; // Sarim peste mesaj dacă nu este valid
                    }
                }

        
                blackboard.addToStage(outputStage, message);
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
    private final int inputStage;   // Etapa de intrare
    private final int outputStage;  // Etapa de ieșire
    private long processingTime = 0; 
    private int processedMessages = 0;

    public CheckPoliticalPropagandaFilter(ClientConfig config, int inputStage, int outputStage) {
        this.config = config;
        this.inputStage = inputStage;
        this.outputStage = outputStage;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                // Preia mesaj din coada corespunzătoare etapei de intrare
                ReviewMessage message = blackboard.getFromStage(inputStage);
                if (message == null) continue;

                // Propagă mesajul de END către următoarea etapă
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addToStage(outputStage, message);
                    break;
                }

                // Verifică propagandă politică (doar dacă este activat în config)
                if (config.checkPoliticalPropaganda) {
                    String text = message.reviewText;
                    if (text.contains("+++") || text.contains("---")) {
                        continue; // Sarim peste mesaj dacă conține șabloane suspecte
                    }
                }

                // Trimite mesajul la următoarea etapă
                blackboard.addToStage(outputStage, message);
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
    private final int inputStage;   // Etapa de intrare
    private final int outputStage;  // Etapa de ieșire
    private long processingTime = 0; 
    private int processedMessages = 0; 

    public ResizeImagesFilter(ClientConfig config, int inputStage, int outputStage) {
        this.config = config;
        this.inputStage = inputStage;
        this.outputStage = outputStage;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                // Preia mesaj din coada corespunzătoare etapei de intrare
                ReviewMessage message = blackboard.getFromStage(inputStage);
                if (message == null) continue;

                // Propagă mesajul de END către următoarea etapă
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addToStage(outputStage, message);
                    break;
                }

                // Redimensionează imaginea (doar dacă este activat în config)
                if (config.resizeImages && message.attachment != null) {
                    message.attachment = message.attachment.toLowerCase();
                }

                // Trimite mesajul la următoarea etapă
                blackboard.addToStage(outputStage, message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
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
    private final int inputStage;
    private final int outputStage;
    private long processingTime = 0; 
    private int processedMessages = 0;

    public SentimentDetectionFilter(ClientConfig config, int inputStage, int outputStage) {
        this.config = config;
        this.inputStage = inputStage;
        this.outputStage = outputStage;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                ReviewMessage message = blackboard.getFromStage(inputStage);
                if (message == null) continue;

                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addToStage(outputStage, message);
                    break;
                }

                if (config.detectSentiment && message.reviewText != null) {
                    int upper = 0, lower = 0;
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c)) upper++;
                        else if (Character.isLowerCase(c)) lower++;
                    }
                    
                    // Adăugăm sufixul corespunzător
                    if (upper > lower) message.reviewText += " +";
                    else if (lower > upper) message.reviewText += " -";
                    else message.reviewText += " =";
                }

                blackboard.addToStage(outputStage, message);
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
    private final int inputStage;
    private final int outputStage;
    private long processingTime = 0; 
    private int processedMessages = 0;

    public SentimentDetectionPlusFilter(ClientConfig config, int inputStage, int outputStage) {
        this.config = config;
        this.inputStage = inputStage;
        this.outputStage = outputStage;
    }

    @Override
    public void process(Blackboard blackboard) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                ReviewMessage message = blackboard.getFromStage(inputStage);
                if (message == null) continue;

                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.addToStage(outputStage, message);
                    break;
                }

                if (config.detectSentimentPlus && message.reviewText != null) {
                    int upper = 0, lower = 0;
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c)) upper++;
                        else if (Character.isLowerCase(c)) lower++;
                    }
                    
                    // Adăugăm eticheta detaliată
                    if (upper > lower) message.reviewText += " (Positive)";
                    else if (lower > upper) message.reviewText += " (Negative)";
                    else message.reviewText += " (Neutral)";
                }

                blackboard.addToStage(outputStage, message);
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

        // Blackboard cu 7 cozi (0-6) pentru cele 6 filtre + o coadă finală
        Blackboard blackboard = new Blackboard(7);

        List<Filter> filters = Arrays.asList(
            new CheckProfanitiesFilter(client1Config, 0, 1),      // Stage 0 → 1
            new CheckBuyerFilter(buyers, client1Config, 1, 2),     // Stage 1 → 2
            new CheckPoliticalPropagandaFilter(client1Config, 2, 3), // Stage 2 → 3
            new ResizeImagesFilter(client1Config, 3, 4),           // Stage 3 → 4
            new SentimentDetectionFilter(client1Config, 4, 5),     // Stage 4 → 5
            new SentimentDetectionPlusFilter(client1Config, 5, 6)  // Stage 5 → 6
        );

        ExecutorService executor = Executors.newFixedThreadPool(filters.size());

        // Pornește toate filtrele
        for (Filter filter : filters) {
            executor.execute(() -> filter.process(blackboard));
        }

        // Mesaje de intrare
        List<ReviewMessage> messages = Arrays.asList(
            new ReviewMessage("John", "Laptop", "ok", "PICTURE"),
            new ReviewMessage("Mary", "Phone", "@#$%", "IMAGE"),
            new ReviewMessage("Peter", "Phone", "GREAT", "ManyPictures"),
            new ReviewMessage("Ann", "Book", "So GOOD", "Image"),
            new ReviewMessage("Alice", "Tablet", "I love this +++", "TabletImage"),
            new ReviewMessage("Bob", "Laptop", "This is amazing ---", "LaptopImage")
        );

        // Adaugă mesaje în prima etapă (coada 0)
        for (ReviewMessage message : messages) {
            blackboard.addToStage(0, message);
        }
        blackboard.addToStage(0, ReviewPipeline.END_MESSAGE);

        // Așteaptă terminarea procesării
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        // Colectează rezultatele din ultima etapă (coada 6)
        List<ReviewMessage> processedMessages = new ArrayList<>();
        ReviewMessage message;
        while ((message = blackboard.getFromStage(6)) != null) {
            if (message == ReviewPipeline.END_MESSAGE) {
                break;
            }
            processedMessages.add(message);
        }

        // Afișează rezultatele
        for (ReviewMessage msg : processedMessages) {
            System.out.println(msg);
        }
    }
}
