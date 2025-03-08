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

//am folosit END_MESSAGE pentru a semnala sfarsitul procesarii recenzilor, si pentru a semnala ca nu mai exista mesaje de procesat, si ca operatiile trebuie sa se opreasca
class ReviewPipeline {
    static final ReviewMessage END_MESSAGE = new ReviewMessage("END", "", "", "");
}

//aici am facut interfata ce defineste metoda ce are ca scop procesarea recenzilor
interface Filter {
    //aici este definita metoda de procesare a recenzilor, dintr o coada de intrare si scrierea rezultatelor intr o coada de iesire
    void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue);
}

//aceasta clasa implementeaza interfata Filter, si este utilizata pentru a filtra recenziile ce contin pranitati, daca un mesaj contine profanitati, este evitat, altfel, un mesaj curat, il pune in coada de iesire
//daca intalneste un mesaj de tipul END_MESSAGE, se opreste
class CheckProfanitiesFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                   // Preia un mesaj din coada de intrare, cu timeout de 100ms
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                
                //daca nu exista mesaje noi, este pus sa astepte
                if (message == null) 
                    continue;
                //daca intalneste mesaj de tip END_MESSAGE termina bucla
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                
                //altfel, daca nu contine profanitati,trimite mesajul la urmatorul filtru
                if (!message.reviewText.contains("@#$%")) {
                    outputQueue.put(message);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


/*clasa CheckBuyerFilter implementeaza si ea interfata Filter si este utilizata pentru a verifica daca utilizatorul care a lasat recenzia chiar a cumparat produsul pentru care a facut recenzia
  daca chiar a cumparat produsul, recenzia e trimisa pentru procesare, daca nu, este ignorata
*/
class CheckBuyerFilter implements Filter {
    //aceasta este harta  care leaga utilizatorul de produse respectiv
    private final Map<String, String> buyers;

    //constructorul primeste o harta cu utilizatorii si produsele cumparate
    public CheckBuyerFilter(Map<String, String> buyers) {
        this.buyers = buyers;
    }

    //metoda process preia mesajele din coada de intrare si  verifica daca utilizatorul care a scris recenzia chiar a cumparat produsul mentionat, daca da, mesajul este trimis in coada de iesire pentru a fi procesat de urmatorul filtru, daca nu, recenzia este ignorata
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                   // Preia un mesaj din coada de intrare, cu timeout de 100 ms
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);

                  // Daca nu exista mesaje, continua sa astepte un alt mesaj
                if (message == null) 
                
                    continue;

                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }
                // Verifica daca utilizatorul a cumparat produsul mentionat in recenzie
                if (buyers.getOrDefault(message.username, "").equals(message.product)) {
                    //recenzia este trimisa mai departe
                    outputQueue.put(message);
                }
            }
        } catch (InterruptedException e) {
            // Daca thread-ul este intrerupt, opreste procesarea
            Thread.currentThread().interrupt();
        }
    }
}

// aceasta clasa este folosita pentru a modifica atasamentele , mai precis, sa transforme numele fisierelor atasamentelor in litere mici (lowercase).
//daca mesajul contine un atasament, acesta va fi actualizat, apoi va fi trimis mai departe pentru procesare.
class ResizeImagesFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);

                if (message == null)
                     continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                if (message.attachment != null) {
                    // Transforma numele fisierului atasamentului in litere mici
                    message.attachment = message.attachment.toLowerCase();// Modifica atasamentul
                }
                 // Trimite mesajul procesat in coada de iesire pentru a fi procesat de urmatorul filtru
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Acest filtru analizeaza textul recenziei si, in functie de numărul de litere mari si litere mici din text, adauga un simbol la finalul recenziei:
/** - Daca exista mai multe litere mari decat litere mici, se adauga un semn "+".
 * - Daca exista mai multe litere mici decat litere mari, se adauga un semn "-".
 * - Daca numarul de litere mari si mici este acelasi, se adauga un semn "=". */
class SentimentDetectionFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);

                if (message == null)
                     continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                if (message.reviewText != null) {
                    int upperCaseCount = 0, lowerCaseCount = 0;
                    
                    // Analizam fiecare caracter din textul recenziei
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c)) {
                             // Daca caracterul este litera mare, incrementam contorul pentru litere mari
                            upperCaseCount++;
                        } else if (Character.isLowerCase(c)) {
                             // Daca caracterul este litera mica, incrementam contorul pentru litere mici
                            lowerCaseCount++;
                        }
                    }

                      // Daca exista mai multe litere mari decat mici, adaugam un "+" la sfarsitul textulu
                    if (upperCaseCount > lowerCaseCount) {
                        message.reviewText += "+";
                    } else if (lowerCaseCount > upperCaseCount) {
                         // Dacă exista mai multe litere mici decat mari, adaugam un "-" la sfarsitul textului
                        message.reviewText += "-";
                    } else {
                        // Dacă numarul de litere mari si mici este egal, adaugam un "=" la sfarsitul textului
                        message.reviewText += "=";
                    }
                }

                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class RemoveCompetitorLinksFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);

                if (message == null) 
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                
                if (message.reviewText != null) {
                    message.reviewText = message.reviewText.replaceAll("http\\S+", "");
                }

                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class CheckPoliticalPropagandaFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);

                if (message == null) 
                    continue;
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

class SentimentDetectionPlusFilter implements Filter {
    @Override
    public void process(BlockingQueue<ReviewMessage> inputQueue, BlockingQueue<ReviewMessage> outputQueue) {
        try {
            while (true) {
                ReviewMessage message = inputQueue.poll(100, TimeUnit.MILLISECONDS);

                if (message == null)
                    continue;
                if (message == ReviewPipeline.END_MESSAGE) {
                    outputQueue.put(message);
                    break;
                }

                if (message.reviewText != null) {
                    int upperCaseCount = 0, lowerCaseCount = 0;

                    // Analyze each character in the review text
                    for (char c : message.reviewText.toCharArray()) {
                        if (Character.isUpperCase(c)) {
                            upperCaseCount++;
                        } else if (Character.isLowerCase(c)) {
                            lowerCaseCount++;
                        }
                    }

                    // Determine sentiment
                    if (upperCaseCount > lowerCaseCount) {
                        message.reviewText += " (Positive)";
                    } else if (lowerCaseCount > upperCaseCount) {
                        message.reviewText += " (Negative)";
                    } else {
                        message.reviewText += " (Neutral)";
                    }
                }

                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


class ConcurrentPipesFilters {
    public static void main(String[] args) throws InterruptedException {
        //  Map pentru a pastra persoanele si produsele lor
        Map<String, String> buyers = new HashMap<>();
        buyers.put("John", "Laptop");
        buyers.put("Mary", "Phone");
        buyers.put("Ann", "Book");

        // Cozi de mesaje pentru fiecare filtru
        BlockingQueue<ReviewMessage> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue2 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue3 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue4 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue5 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> queue6 = new LinkedBlockingQueue<>();
        BlockingQueue<ReviewMessage> outputQueue = new LinkedBlockingQueue<>();

        // ExecutorService cu 6 thread-uri (cate unul pentru fiecare filtru)
        ExecutorService executor = Executors.newFixedThreadPool(6);

        // Lansam thread-uri pentru fiecare filtru
        executor.execute(() -> new CheckProfanitiesFilter().process(queue1, queue2));  // Filtru 1
        executor.execute(() -> new CheckBuyerFilter(buyers).process(queue2, queue3));  // Filtru 2
        executor.execute(() -> new ResizeImagesFilter().process(queue3, queue4));      // Filtru 3
        executor.execute(() -> new CheckPoliticalPropagandaFilter().process(queue4, queue5)); // Filtru 4
        executor.execute(() -> new SentimentDetectionFilter().process(queue5, queue6)); // Filtru 5
        executor.execute(() -> new SentimentDetectionPlusFilter().process(queue6, outputQueue)); // Filtru 6

        // Lista de mesaje de test
        List<ReviewMessage> messages = Arrays.asList(
                new ReviewMessage("John", "Laptop", "ok", "PICTURE"),
                new ReviewMessage("Mary", "Phone", "@#$%", "IMAGE"), // Ar trebui filtrat (profanities)
                new ReviewMessage("Peter", "Phone", "GREAT", "ManyPictures"),
                new ReviewMessage("Ann", "Book", "So GOOD", "Image"),
                new ReviewMessage("Alice", "Tablet", "I love this +++", "TabletImage"), // Ar trebui filtrat (propaganda)
                new ReviewMessage("Bob", "Laptop", "This is amazing ---", "LaptopImage") // Ar trebui filtrat (propaganda)
        );

        // Adaugam mesajele in prima coada
        for (ReviewMessage message : messages) {
            queue1.put(message);
        }

        // Adaugam mesajul de finalizare
        queue1.put(ReviewPipeline.END_MESSAGE);

        // Asteptam sa se proceseze
        Thread.sleep(1000);

        // Afisam mesajele filtrate si procesate
        while (!outputQueue.isEmpty()) {
            System.out.println(outputQueue.poll());
        }

        // Inchidem ExecutorService-ul
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }
}