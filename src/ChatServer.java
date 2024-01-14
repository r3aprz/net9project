import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


public class ChatServer {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    // per tenere traccia dei client connessi
    // ogni client è identificato da un nome utente (stringa) e associato a un oggetto PrintWriter che gestisce l'invio di messaggi al client
    private static final Map<String, PrintWriter> clients = new HashMap<>();
    private static final Map<String, String> /* username, channel */ userChannels = new HashMap<>();
    private static final Set<String> administrators = new HashSet<>(); // contiene tutti gli username di coloro che sono admin
    private static final Map<String, String> bannedUsers = new HashMap<>(); // tiene traccia delle coppie utente-canale bannati


    public static void main(String[] args) {
        try {
            administrators.add("kekka");
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
                            if (isAdmin(username)) {
                                if (clientMessage.startsWith("/admin ")) {
                                    handleAdminCommand(username, clientMessage.substring(7));
                                } else {
                                    handleGeneralCommands(username, clientMessage);
                                }
                            } else {
                                handleGeneralCommands(username, clientMessage);
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
    private static void handleGeneralCommands (String username, String clientMessage) {
        if (clientMessage.startsWith("/join #") && channelExists(clientMessage.substring(6))) {
            handleJoinCommand(username, clientMessage.substring(7));
        } else if (clientMessage.equals("/leave")) {
            handleLeaveCommand(username);
        } else if (clientMessage.startsWith("/msg ")) {
            handleSendMessageToChannel(username, clientMessage.substring(5));
        } else if (clientMessage.equals("/list")) {
            clients.get(username).println(handleShowChannelsList());
        } else if (clientMessage.equals("/users")) {
            clients.get(username).println(handleShowUsersList(username));
        } else if (clientMessage.startsWith("/privmsg ")) {
            handlePrivateMessage(username, clientMessage.substring(9));
        } else {
            clients.get(username).println("Insert Valid Command");
        }
    }

    private static void handleAdminCommand(String adminUsername, String clientMessage) {
        if (clientMessage.startsWith("/createchannel #")) {
            handleCreateChannel(adminUsername, clientMessage.substring(16));
        } else if (clientMessage.startsWith("/kick ")) {
            handleKickCommand(adminUsername, clientMessage.substring(6));
        } else if (clientMessage.startsWith("/ban ")) {
            handleBanCommand(adminUsername, clientMessage.substring(5));
        } else if (clientMessage.startsWith("/unban ")) {
            handleUnbanCommand(adminUsername, clientMessage.substring(7));
        } else if (clientMessage.startsWith("/promote ")) {
            handlePromoteCommand(adminUsername, clientMessage.substring(9));
        } else {
            clients.get(adminUsername).println("Insert Valid Command");
        }
    }

    private static void handleJoinCommand(String username, String channel) {
        PrintWriter writer = clients.get(username);

        if (userChannels.containsKey(username)) {
            handleLeaveCommand(username);
        }
        if (bannedUsers.containsKey(username)) {
            writer.println("you have been banned from this channel.");
        } else {
            userChannels.put(username, "#" + channel);

            // Invia un messaggio di benvenuto all'utente appena entrato nel canale

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

    private static void handlePrivateMessage (String sender, String message) {
        // la divisione tra il destinatario ed il messaggio è uno spazio
        String[] parts = message.split(" ", 2); // separa il messaggio in un array di due parti

        if (parts.length == 2) { // se le due parti ci sono
            String recipient = parts[0]; // nome destinatario
            String privateMessage = parts[1]; // messaggio

            // controlla se il destinatario esiste nella map
            if (clients.containsKey(recipient)) {
                PrintWriter recipientWriter = clients.get(recipient);

                // stampa sul destinatario il messaggio
                recipientWriter.println("[Private from " + sender + "]: " + privateMessage);
            } else { // se l'utente non esiste
                clients.get(sender).println("User " + recipient + " not found.");
            }
        } else {
            clients.get(sender).println("Invalid /privmsg command. Usage: /privmsg username message");
        }
    }

    private static boolean isAdmin (String username) {
        // verifica se l'username corrisponde a quello di un admin
        return administrators.contains(username);
    }

    /*
    private static void handleAdminCommand(String adminUsername, String command) { // assegnare admin
        String[] parts = command.split(" ", 2);

        if (parts.length == 2) {
            String action = parts[0].toLowerCase();
            String targetUser = parts[1];

            switch (action) {
                case "add":
                    administrators.add(targetUser);
                    clients.get(adminUsername).println(targetUser + " is now an administrator.");
                    break;
                case "remove":
                    administrators.remove(targetUser);
                    clients.get(adminUsername).println(targetUser + " is no longer an administrator.");
                    break;
                default:
                    clients.get(adminUsername).println("Invalid /admin command. Usage: /admin add/remove username");
                    break;
            }
        } else {
            clients.get(adminUsername).println("Invalid /admin command. Usage: /admin add/remove username");
        }
    } */

    private static void handleKickCommand (String adminUsername, String kickuser) {
        if (kickuser == adminUsername) {
            clients.get(adminUsername).println(RED + "non puoi bannarti da solo" + RESET);
        } else {
            if (userChannels.containsKey(kickuser)) {
                String channel = userChannels.get(kickuser);
                userChannels.remove(kickuser);

                Set<String> channelUsers = getChannelUsers(channel);
                for (String user : channelUsers) {
                    PrintWriter writer = clients.get(user);
                    writer.println(RED + kickuser + " è stato espulso dal canale da " + adminUsername + "." + RESET);
                }

                PrintWriter targetWriter = clients.get(kickuser);
                if (targetWriter != null) {
                    targetWriter.println(RED + "sei stato espulso dal canale da " + adminUsername + "." + RESET);
                }
            } else {
                clients.get(adminUsername).println(kickuser + " non è presente nel canale.");
            }
        }
    }

    private static void handleBanCommand (String adminUsername, String banuser) {
        if (banuser == adminUsername) {
            clients.get(adminUsername).println("non puoi bannarti da solo");
        } else {
            if (userChannels.containsKey(banuser)) {
                String channel = userChannels.get(banuser);
                userChannels.remove(banuser);

                bannedUsers.put(banuser, channel);

                Set<String> channelUsers = getChannelUsers(channel);
                for (String user : channelUsers) {
                    PrintWriter writer = clients.get(user);
                    writer.println(RED + banuser + " è stato bannato dal canale da " + adminUsername + "." + RESET);
                }

                PrintWriter targetWriter = clients.get(banuser);
                if (targetWriter != null) {
                    targetWriter.println(RED + "sei stato bannato dal canale da " + adminUsername + "." + RESET);
                }
            } else {
                clients.get(adminUsername).println(banuser + " non è presente nel canale.");
            }
        }
    }

    private static void handleUnbanCommand (String adminUsername, String unbanuser) {
        if (bannedUsers.containsKey(unbanuser)) {
            String channel = bannedUsers.get(unbanuser);
            bannedUsers.remove(unbanuser);

            Set<String> channelUsers = getChannelUsers(channel);
            for (String user : channelUsers) {
                PrintWriter writer = clients.get(user);
                writer.println(RED + unbanuser + " è stato unbannato dal canale da " + adminUsername + "." + RESET);
            }

            clients.get(unbanuser).println(RED + "non sei piu bannato dal canale " + channel + RESET);
        } else {
            clients.get(adminUsername).println(unbanuser + " non è attualmente bannato.");
        }
    }

    private static void handlePromoteCommand (String adminsUsername, String username) {
        if (isAdmin(username)) {
            clients.get(adminsUsername).println("l'utente è già admin");
        } else {
            administrators.add(username);
            clients.get(username).println("l'admin " + adminsUsername + " ti ha promosso ad amministratore");
        }
    }
}