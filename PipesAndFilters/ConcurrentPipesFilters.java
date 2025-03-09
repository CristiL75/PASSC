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

class ClientConfig {
    boolean resizeImages; // Daca trebuie redimensionate imaginile
    boolean checkBuyer;   // Daca trebuie verificat daca utilizatorul a cumparat produsul
    boolean checkProfanities; // Daca trebuie verificat daca exista profanitati
    boolean checkPoliticalPropaganda; // Daca trebuie verificata propaganda politica
    boolean detectSentiment; // Daca trebuie detectat sentimentul
    boolean detectSentimentPlus; // Daca trebuie detectat sentimentul cu etichete suplimentare

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
    static final ReviewMessage END_MESSAGE = new ReviewMessage("END", "", "", ""); // Mesajul de final pentru a semnala sfarsitul procesarii
}

interface Filter {
    void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue);
}

class CheckProfanitiesFilter implements Filter {
    private final ClientConfig config;
    private long processingTime = 0; // Timpul de procesare pentru acest filtru
    private int processedMessages = 0; // Numarul de mesaje procesate

    public CheckProfanitiesFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                // Verifica daca filtrul este activat si daca mesajul contine cuvinte obscene
                if (config.checkProfanities && message.reviewText.contains("@#$%")) {
                    continue; // Ignora mesajul daca contine 
                }
                outputQueue.put(message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
                processingTime += (endTime - startTime); // Actualizarea timpului total de procesare
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
    private long processingTime = 0; // Timpul de procesare pentru acest filtru
    private int processedMessages = 0; // Numarul de mesaje procesate

    public CheckBuyerFilter(Map<String, String> buyers, ClientConfig config) {
        this.buyers = buyers;
        this.config = config;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                // Verifica daca filtrul este activat si daca utilizatorul a cumparat produsul
                if (config.checkBuyer && !buyers.getOrDefault(message.username, "").equals(message.product)) {
                    continue; // Ignora mesajul daca utilizatorul nu a cumparat produsul
                }
                outputQueue.put(message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
                processingTime += (endTime - startTime); // Actualizarea timpului total de procesare
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
    private long processingTime = 0; // Timpul de procesare pentru acest filtru
    private int processedMessages = 0; // Numarul de mesaje procesate

    public ResizeImagesFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                // Redimensioneaza atasamentul daca filtrul este activat
                if (config.resizeImages && message.attachment != null) {
                    message.attachment = message.attachment.toLowerCase();
                }
                outputQueue.put(message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
                processingTime += (endTime - startTime); // Actualizarea timpului total de procesare
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
    private long processingTime = 0; // Timpul de procesare pentru acest filtru
    private int processedMessages = 0; // Numarul de mesaje procesate

    public SentimentDetectionFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                // Detecteaza sentimentul daca filtrul este activat
                if (config.detectSentiment && message.reviewText != null) {
                    int upperCaseCount = 0, lowerCaseCount = 0;
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c))
                             upperCaseCount++;
                        else if (Character.isLowerCase(c)) 
                            lowerCaseCount++;
                    }

                    // Adauga un simbol in functie de numarul de litere mari si mici
                    if (upperCaseCount > lowerCaseCount)
                         message.reviewText += "+";
                    else if (lowerCaseCount > upperCaseCount) 
                        message.reviewText += "-";
                    else
                         message.reviewText += "=";
                }
                outputQueue.put(message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
                processingTime += (endTime - startTime); // Actualizarea timpului total de procesare
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
    private long processingTime = 0; // Timpul de procesare pentru acest filtru
    private int processedMessages = 0; // Numarul de mesaje procesate

    public CheckPoliticalPropagandaFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null)
                     continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                // Verifica daca filtrul este activat si daca mesajul contine propaganda politica
                if (config.checkPoliticalPropaganda && (message.reviewText.contains("+++") || message.reviewText.contains("---"))) {
                    continue; // Ignora mesajul daca contine propaganda politica
                }
                outputQueue.put(message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
                processingTime += (endTime - startTime); // Actualizarea timpului total de procesare
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
    private long processingTime = 0; // Timpul de procesare pentru acest filtru
    private int processedMessages = 0; // Numarul de mesaje procesate

    public SentimentDetectionPlusFilter(ClientConfig config) {
        this.config = config;
    }

    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                long startTime = System.currentTimeMillis(); // Masurarea timpului de inceput
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
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
                    else 
                        message.reviewText += " (Neutral)";
                }
                outputQueue.put(message);
                processedMessages++; // Incrementarea numarului de mesaje procesate
                long endTime = System.currentTimeMillis(); // Masurarea timpului de sfarsit
                processingTime += (endTime - startTime); // Actualizarea timpului total de procesare
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

class ConcurrentPipesFilters {
    public static void main(String[] args) throws InterruptedException {
        // Mapa pentru a stoca utilizatorii si produsele cumparate
        Map<String, String> buyers = new HashMap<>();
        buyers.put("John", "Laptop");
        buyers.put("Mary", "Phone");
        buyers.put("Ann", "Book");

        // Configuratii pentru clienti
        ClientConfig client1Config = new ClientConfig(true, true, true, true, true, false); // Client 1
        ClientConfig client2Config = new ClientConfig(false, false, true, false, true, true); // Client 2

        // Coada pentru fiecare filtru
        BlockingQueue<ReviewMessage> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue2 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue3 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue4 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue5 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue6 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> outputQueue = new LinkedBlockingQueue<>();

        // Crearea filtrelor
        CheckProfanitiesFilter profanityFilter = new CheckProfanitiesFilter(client1Config);
        CheckBuyerFilter buyerFilter = new CheckBuyerFilter(buyers, client1Config);
        ResizeImagesFilter resizeFilter = new ResizeImagesFilter(client1Config);
        SentimentDetectionFilter sentimentFilter = new SentimentDetectionFilter(client1Config);
        CheckPoliticalPropagandaFilter propagandaFilter = new CheckPoliticalPropagandaFilter(client1Config);
        SentimentDetectionPlusFilter sentimentPlusFilter = new SentimentDetectionPlusFilter(client1Config);

        // ExecutorService cu 6 thread-uri (cate unul pentru fiecare filtru)
        ExecutorService executor = Executors.newFixedThreadPool(6);

        // Porneste thread-urile pentru fiecare filtru
        executor.execute(() -> profanityFilter.process(queue1, queue2));
        executor.execute(() -> buyerFilter.process(queue2, queue3));
        executor.execute(() -> resizeFilter.process(queue3, queue4));
        executor.execute(() -> propagandaFilter.process(queue4, queue5));
        executor.execute(() -> sentimentFilter.process(queue5, queue6));
        executor.execute(() -> sentimentPlusFilter.process(queue6, outputQueue));

        // Lista de mesaje de test
        List<ReviewMessage> messages = Arrays.asList(
                new ReviewMessage("John", "Laptop", "ok", "PICTURE"),
                new ReviewMessage("Mary", "Phone", "@#$%", "IMAGE"), // Va fi filtrat (profanitati)
                new ReviewMessage("Peter", "Phone", "GREAT", "ManyPictures"),
                new ReviewMessage("Ann", "Book", "So GOOD", "Image"),
                new ReviewMessage("Alice", "Tablet", "I love this +++", "TabletImage"), // Va fi filtrat (propaganda)
                new ReviewMessage("Bob", "Laptop", "This is amazing ---", "LaptopImage") // Va fi filtrat (propaganda)
        );

        // Adauga mesajele in prima coada
        for (ReviewMessage message : messages) {
            queue1.put(message);
        }

        // Adauga mesajul de final
        queue1.put(ReviewPipeline.END_MESSAGE);

        // Masurarea timpului de inceput pentru intregul sistem
        long startTime = System.currentTimeMillis();

        // Asteapta ca procesarea sa se termine
        Thread.sleep(1000);

        // Masurarea timpului de sfarsit pentru intregul sistem
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Inchide ExecutorService
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        
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

        System.out.println("\nMesaje Procesate:");
        while (!outputQueue.isEmpty()) {
            System.out.println(outputQueue.poll());
        }
    }
}