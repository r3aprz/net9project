import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChatServer {
    // per tenere traccia dei client connessi
    // ogni client è identificato da un nome utente (stringa) e associato a un oggetto PrintWriter che gestisce l'invio di messaggi al client
    private static final Map<String, PrintWriter> clients = new HashMap<>();
    private static final Map<String, String> /* username, channel */ userChannels = new HashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(12347);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // per leggere input dal client
                BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // per inviare output al client
                PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                String username = clientReader.readLine(); // viene inserito il nome letto in una variabile
                // clients.put(username, clientWriter);  nome inviato alla socket
                if (!clients.containsKey(username)) { // controlla che il nome sia univoco all'interno del server
                    clients.put(username, clientWriter);
                } else {
                    clientWriter.println("Username already exists. Please choose a different one.");
                    continue; // torna all'inizio
                }

                new Thread(() -> {
                    try {
                        String clientMessage;

                        while ((clientMessage = clientReader.readLine()) != null) {
                            if (clientMessage.startsWith("/join #") && channelExists(clientMessage.substring(6))) {
                                handleJoinCommand(username, clientMessage.substring(7));
                            } else if (clientMessage.equals("/leave")) {
                                handleLeaveCommand(username);
                            } else if (clientMessage.startsWith("/createchannel #") ) {
                                handleCreateChannel(username, clientMessage.substring(16));
                            } else if (clientMessage.startsWith("/msg ")) {
                                handleSendMessageToChannel(username, clientMessage.substring(5));
                            } else if (clientMessage.equals("/list")) {
                                clientWriter.println(handleShowChannelsList());
                            } else if (clientMessage.equals("/users")) {
                                clientWriter.println(handleShowUsersList(username));
                            } else {
                                clientWriter.println("Insert Valid Command");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        handleLeaveCommand(username);
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    private static void broadcast(String message) {
        for (PrintWriter writer : clients.values()) {
            writer.println(message);
        }
    } */

    private static void handleJoinCommand(String username, String channel) {
        if (userChannels.containsKey(username)) {
            handleLeaveCommand(username);
        }

        userChannels.put(username, "#" + channel);

        // Invia un messaggio di benvenuto all'utente appena entrato nel canale
        PrintWriter writer = clients.get(username);
        writer.println("Benvenuto nel canale " + userChannels.get(username));

        // Invia un messaggio agli utenti già presenti nel canale
        Set<String> channelUsers = getChannelUsers(userChannels.get(username));
        for (String user : channelUsers) {
            if (!user.equals(username)) {
                writer = clients.get(user);
                writer.println(username + " has joined to channel.");
            }
        }
    }

    private static void handleLeaveCommand(String username) {
        if (userChannels.containsKey(username)) {
            String channel = userChannels.get(username);
            userChannels.remove(username);

            PrintWriter writer;

            Set<String> channelUsers = getChannelUsers(channel);
            for (String user : channelUsers) {
                if (!user.equals(username)) {
                    writer = clients.get(user);
                    writer.println(username + " has left channel.");
                }
            }
        } else {
            System.out.println("use this command only if you're in a channel");
        }
    }

    private static Set<String> getChannelUsers(String channel) {
        return userChannels.entrySet().stream() // mette su uno stream tutte le coppia chiave valore presenti in user channels
                .filter(entry -> entry.getValue().equals(channel)) // mantiene solo le tuple il cui  valore è i canale passato
                .map(Map.Entry::getKey) // delle tuple conserva solo la chiave ossia username
                .collect(java.util.stream.Collectors.toSet()); // unisci tutti gli username rimasti
    }

    private static void handleCreateChannel(String username, String channel) {
        PrintWriter writer = clients.get(username);

        if (userChannels.containsKey(username)) {
            writer.println("Uscita dal canale ... ");
            handleLeaveCommand(username);
        }

        if (!channelExists("#" + channel)) {
            handleJoinCommand(username, channel);
        } else {
            // il canale esiste già, informa l'utente
            writer.println("Il canale di nome " + channel + " già esiste. Scegli un nome diverso.");
        }
    }

    private static boolean channelExists(String channel) {
        return userChannels.containsValue(channel);
    }

    private static void handleSendMessageToChannel(String username, String message) {
        if (userChannels.containsKey(username)) {
            String channel = userChannels.get(username);
            Set<String> channelUsers = getChannelUsers(channel);

            for (String user : channelUsers) {
                PrintWriter writer = clients.get(user);
                writer.println(username + ": " + message);
            }
        }
    }

    private static Set<String> handleShowChannelsList () {
        return userChannels.entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Set<String> handleShowUsersList (String username) {
        String c = userChannels.get(username);
        return userChannels.entrySet().stream()
                .filter(entry -> entry.getValue().equals(c))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }
}