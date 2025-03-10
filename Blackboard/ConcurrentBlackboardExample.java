import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class ReviewMessage {
    String username;
    String product;
    String reviewText;
    String attachment;
    private final int totalFilters; // Numărul total de filtre
    private final AtomicInteger processedFilters = new AtomicInteger(0); // Contor atomic
    Set<String> processedBy = ConcurrentHashMap.newKeySet();

    public ReviewMessage(String username, String product, String reviewText, String attachment, int totalFilters) {
        this.username = username;
        this.product = product;
        this.reviewText = reviewText;
        this.attachment = attachment;
        this.totalFilters = totalFilters; // Inițializare corectă
    }

    public boolean isProcessedBy(String filterName) {
        return processedBy.contains(filterName);
    }

    public void markProcessed(String filterName) {
        processedBy.add(filterName);
        processedFilters.incrementAndGet(); // Actualizează contorul
    }

    public boolean isFullyProcessed() {
        return processedFilters.get() == totalFilters;
    }

    @Override
    public String toString() {
        return username + ", " + product + ", " + reviewText + ", " + attachment;
    }
}

class ConcurrentBlackboard {
    private final BlockingQueue<ReviewMessage> queue = new LinkedBlockingQueue<>();
    private final Set<String> activeFilters = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeMessages = new AtomicInteger(0);

    // Adaugă un mesaj în Blackboard
    public void addMessage(ReviewMessage message) throws InterruptedException {
        queue.put(message);
        activeMessages.incrementAndGet();
    }

    // Preia un mesaj pentru un anumit filtru
    public ReviewMessage getMessageForFilter(String filterName) throws InterruptedException {
        while (true) {
            ReviewMessage message = queue.poll(100, TimeUnit.MILLISECONDS);
            if (message == null) continue;
            
            if (!message.isProcessedBy(filterName)) {
                return message;
            } else {
                queue.put(message); // Returnează mesajul în coadă pentru alte filtre
            }
        }
    }

    public void returnMessage(ReviewMessage message, String filterName) throws InterruptedException {
        message.markProcessed(filterName);
        if (message.isFullyProcessed()) {
            activeMessages.decrementAndGet();
        } else {
            queue.put(message);
        }
    }

    public void registerFilter(String filterName) {
        activeFilters.add(filterName);
    }

    public boolean isDone() {
        return activeMessages.get() == 0;
    }

    public int getTotalFilters() {
        return activeFilters.size();
    }

    public Set<String> getActiveFilters() {
        return activeFilters;
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
    static final ReviewMessage END_MESSAGE = new ReviewMessage("END", "", "", "", 0);
}

interface Filter {
    void process(ConcurrentBlackboard blackboard);
}

class CheckProfanitiesFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final ClientConfig config;
    private final String filterName = "ProfanityFilter";
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicLong processingTime = new AtomicLong(0);

    public CheckProfanitiesFilter(ConcurrentBlackboard blackboard, ClientConfig config) {
        this.blackboard = blackboard;
        this.config = config;
        this.blackboard.registerFilter(filterName);
    }

    @Override
    public void run() {
        try {
            while (!blackboard.isDone()) {
                ReviewMessage message = blackboard.getMessageForFilter(filterName);
                if (message == null) 
                    continue;
                
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.returnMessage(message, filterName);
                    break;
                }
    
                // Dacă mesajul conține profanități, îl eliminăm
                if (config.checkProfanities && message.reviewText.contains("@#$%")) {
                    System.out.println("CheckProfanitiesFilter: Mesaj eliminat: " + message);
                    continue; // Nu retrimitem mesajul
                }
                
                blackboard.returnMessage(message, filterName);
                System.out.println("CheckProfanitiesFilter: Mesaj procesat: " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long getProcessingTime() {
        return processingTime.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }
}

class CheckBuyerFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final Map<String, String> buyers;
    private final ClientConfig config;
    private final String filterName = "BuyerFilter";
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicLong processingTime = new AtomicLong(0);

    public CheckBuyerFilter(ConcurrentBlackboard blackboard, Map<String, String> buyers, ClientConfig config) {
        this.blackboard = blackboard;
        this.buyers = new ConcurrentHashMap<>(buyers); // Copie thread-safe
        this.config = config;
        this.blackboard.registerFilter(filterName);
    }

    @Override
    public void run() {
        try {
            while (!blackboard.isDone()) {
                ReviewMessage message = blackboard.getMessageForFilter(filterName);
                if (message == null) continue;

                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.returnMessage(message, filterName);
                    break;
                }

                boolean isValid = true;
                if (config.checkBuyer) {
                    String purchasedProduct = buyers.get(message.username);
                    if (purchasedProduct == null || !purchasedProduct.equals(message.product)) {
                        System.out.println("CheckBuyerFilter: Mesaj eliminat (utilizator nu a cumpărat produsul): " + message);
                        continue; // Ieșim complet din metodă, fără a retrimite mesajul înapoi în coadă
                    }
                    
                }

                processedMessages.incrementAndGet();
                blackboard.returnMessage(message, filterName);
                System.out.println("CheckBuyerFilter: Mesaj procesat: " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long getProcessingTime() {
        return processingTime.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }
}

class ResizeImagesFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final ClientConfig config;
    private final String filterName = "ResizeImagesFilter";
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicLong processingTime = new AtomicLong(0);

    public ResizeImagesFilter(ConcurrentBlackboard blackboard, ClientConfig config) {
        this.blackboard = blackboard;
        this.config = config;
        this.blackboard.registerFilter(filterName);
    }

    @Override
        public void run() {
            try {
                while (!blackboard.isDone()) {
                    ReviewMessage message = blackboard.getMessageForFilter(filterName);
                    if (message == null) continue;

                    if (message == ReviewPipeline.END_MESSAGE) {
                        blackboard.returnMessage(message, filterName);
                        break;
                    }

                    if (config.resizeImages && message.attachment != null) {
                        message.attachment = "Resized: " + message.attachment.toLowerCase();
                    }

                    message.markProcessed(filterName);
                    blackboard.returnMessage(message, filterName);
                    System.out.println("ResizeImagesFilter: Mesaj procesat: " + message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    public long getProcessingTime() {
        return processingTime.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }
}

class SentimentDetectionFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final ClientConfig config;
    private final String filterName = "SentimentFilter";
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicLong processingTime = new AtomicLong(0);

    public SentimentDetectionFilter(ConcurrentBlackboard blackboard, ClientConfig config) {
        this.blackboard = blackboard;
        this.config = config;
        this.blackboard.registerFilter(filterName);
    }

    @Override
    public void run() {
        try {
            while (!blackboard.isDone()) {
                ReviewMessage message = blackboard.getMessageForFilter(filterName);
                if (message == null) continue;
                
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.returnMessage(message, filterName);
                    break;
                }

                if (config.detectSentiment && message.reviewText != null) {
                    int upper = 0, lower = 0;
                    String text = message.reviewText; // Lucrăm pe o copie locală
                    
                    for (char c : text.toCharArray()) {
                        if (Character.isUpperCase(c)) upper++;
                        else if (Character.isLowerCase(c)) lower++;
                    }

                    // Creăm un nou string în loc să modificăm pe cel existent
                    String newText = text;
                    if (upper > lower) newText += "+";
                    else if (lower > upper) newText += "-";
                    else newText += "=";

                    // Actualizăm mesajul atomic
                    message.reviewText = newText;
                }

                processedMessages.incrementAndGet();
                blackboard.returnMessage(message, filterName);
                System.out.println("SentimentDetectionFilter: Mesaj procesat: " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long getProcessingTime() {
        return processingTime.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }
}

class CheckPoliticalPropagandaFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final ClientConfig config;
    private final String filterName = "PoliticalPropagandaFilter";
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicLong processingTime = new AtomicLong(0);

    public CheckPoliticalPropagandaFilter(ConcurrentBlackboard blackboard, ClientConfig config) {
        this.blackboard = blackboard;
        this.config = config;
        this.blackboard.registerFilter(filterName);
    }

 
    @Override
    public void run() {
        try {
            while (!blackboard.isDone()) {
                ReviewMessage message = blackboard.getMessageForFilter(filterName);
                if (message == null) continue;
    
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.returnMessage(message, filterName);
                    break;
                }
    
                if (config.checkPoliticalPropaganda &&
                    (message.reviewText.contains("+++") || message.reviewText.contains("---"))) {
                    System.out.println("CheckPoliticalPropagandaFilter: Mesaj eliminat: " + message);
                    continue; // Nu retrimitem mesajul înapoi
                }

    
                message.markProcessed(filterName);
                blackboard.returnMessage(message, filterName);
                System.out.println("CheckPoliticalPropagandaFilter: Mesaj procesat: " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    

    public long getProcessingTime() {
        return processingTime.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }
}

class SentimentDetectionPlusFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final ClientConfig config;
    private final String filterName = "SentimentPlusFilter";
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicLong processingTime = new AtomicLong(0);

    public SentimentDetectionPlusFilter(ConcurrentBlackboard blackboard, ClientConfig config) {
        this.blackboard = blackboard;
        this.config = config;
        this.blackboard.registerFilter(filterName);
    }

    @Override
    public void run() {
        try {
            while (!blackboard.isDone()) {
                ReviewMessage message = blackboard.getMessageForFilter(filterName);
                if (message == null) continue;

                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.returnMessage(message, filterName);
                    break;
                }

                if (config.detectSentimentPlus && message.reviewText != null) {
                    int upper = 0, lower = 0;
                    String currentText = message.reviewText;
                    
                    for (char c : currentText.toCharArray()) {
                        if (Character.isUpperCase(c)) upper++;
                        else if (Character.isLowerCase(c)) lower++;
                    }

                    String sentiment;
                    if (upper > lower) sentiment = " (Positive)";
                    else if (lower > upper) sentiment = " (Negative)";
                    else sentiment = " (Neutral)";

                    message.reviewText = currentText + sentiment;
                    
                }

                processedMessages.incrementAndGet();
                blackboard.returnMessage(message, filterName);
                System.out.println("SentimentDetectionPlusFilter: Mesaj procesat: " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long getProcessingTime() {
        return processingTime.get();
    }

    public int getProcessedMessages() {
        return processedMessages.get();
    }
}

class FinalFilter implements Runnable {
    private final ConcurrentBlackboard blackboard;
    private final String filterName = "FinalFilter";
    private final BlockingQueue<ReviewMessage> resultsQueue = new LinkedBlockingQueue<>();

    public FinalFilter(ConcurrentBlackboard blackboard) {
        this.blackboard = blackboard;
        this.blackboard.registerFilter(filterName);
    }

    @Override
    public void run() {
        try {
            while (!blackboard.isDone()) {
                ReviewMessage message = blackboard.getMessageForFilter(filterName);
                if (message == null) continue;
    
                if (message == ReviewPipeline.END_MESSAGE) {
                    blackboard.returnMessage(message, filterName);
                    break;
                }
    
                // Verificăm dacă mesajul a fost eliminat de un filtru anterior
                if (message.reviewText.contains("+++") || message.reviewText.contains("---")) {
                    System.out.println("FinalFilter: Mesaj eliminat definitiv " + message);
                    continue; // Nu adăugăm mesajul în rezultate
                }
    
                if (message.isFullyProcessed()) {
                    resultsQueue.put(message);
                    System.out.println("FinalFilter: Mesaj acceptat " + message);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    
    public List<ReviewMessage> getResults() {
        return new ArrayList<>(resultsQueue);
    }
}

public class ConcurrentBlackboardExample {
    public static void main(String[] args) throws InterruptedException {
        Map<String, String> buyers = new HashMap<>();
        buyers.put("John", "Laptop");
        buyers.put("Mary", "Phone");
        buyers.put("Ann", "Book");

        ClientConfig config = new ClientConfig(true, true, true, true, true, false);
        ConcurrentBlackboard blackboard = new ConcurrentBlackboard();

        // Register filters
        List<Runnable> filters = Arrays.asList(
            new CheckProfanitiesFilter(blackboard, config),
            new CheckBuyerFilter(blackboard, buyers, config),
            new ResizeImagesFilter(blackboard, config),
            new CheckPoliticalPropagandaFilter(blackboard, config),
            new SentimentDetectionFilter(blackboard, config),
            new SentimentDetectionPlusFilter(blackboard, config),
            new FinalFilter(blackboard)
        );

        ExecutorService executor = Executors.newFixedThreadPool(filters.size());
        filters.forEach(executor::execute);

        // Add messages with correct totalFilters
        int totalFilters = blackboard.getTotalFilters();
        List<ReviewMessage> messages = Arrays.asList(
            new ReviewMessage("John", "Laptop", "ok", "PICTURE", totalFilters),
            new ReviewMessage("Mary", "Phone", "@#$%", "IMAGE", totalFilters),
            new ReviewMessage("Alice", "Tablet", "I love this +++", "TabletImage", totalFilters),
            new ReviewMessage("Peter", "Phone", "GREAT", "ManyPictures", totalFilters),
            new ReviewMessage("Ann", "Book", "So GOOD", "Image", totalFilters),
            new ReviewMessage("Bob", "Laptop", "This is amazing ---", "LaptopImage", totalFilters),
            new ReviewMessage("END", "", "", "", totalFilters) // END_MESSAGE
        );

        for (ReviewMessage message : messages) {
            blackboard.addMessage(message);
        }

        // Proper shutdown
        executor.shutdown();
        while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("Waiting for completion...");
        }

        // Get results
        System.out.println("\nFinal Results:");
        ((FinalFilter) filters.get(filters.size()-1)).getResults().forEach(System.out::println);
    }
}