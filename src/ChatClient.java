import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12347);

            // legge da socket -> socket.getInputStream()
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // scrive su socket -> socket.getOutputStream()
            PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);

            /* section INSERIMENTO NOME UTENTE */

            // crea oggetto per leggere da input
            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter your username: ");
            // inserisce input letto in variabile username
            String username = userInputReader.readLine();
            // invia nome inserito al socket
            serverWriter.println(username);

            // creo thread che rimane in attesa di messaggi
            new Thread(() -> {
                try { // attesa
                    String serverMessage; // variabile che conterrà il messaggio in arrivo
                    while ((serverMessage = serverReader.readLine()) != null) { // attente mex
                        System.out.println(serverMessage); // quando lo riceve lo stampa
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start(); // esegue in thread, lo fa partire

            new Thread(() -> {
                try {
                    String userInput;
                    while ((userInput = userInputReader.readLine()) != null) {
                        if(userInput.startsWith("/")) {
                            serverWriter.println(userInput);
                        } else {
                            System.out.println("insert command");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}