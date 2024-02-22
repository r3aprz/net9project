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
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";


    private static final int PORT = 12347;
    // per tenere traccia dei client connessi
    // ogni client è identificato da un nome utente (stringa) e associato a un oggetto PrintWriter che gestisce l'invio di messaggi al client
    private static final Map<String, PrintWriter> clients = new HashMap<>();
    private static final Set<String> channels = new HashSet<>();
    private static final Map<String, String> /* username, channel */ userChannels = new HashMap<>();
    private static final Set<String> administrators = new HashSet<>(); // contiene tutti gli username di coloro che sono admin
    private static final Map<String, String> bannedUsers = new HashMap<>(); // tiene traccia delle coppie utente-canale bannati


    public static void main(String[] args) {
        try {
            administrators.add("kekka");
            ServerSocket serverSocket = new ServerSocket(PORT);

            System.out.println(GREEN + "Server started on port " + PORT + " . . ." + RESET);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // accetta connessioni in arrivo

                // per leggere input dal client
                BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // per inviare output al client
                PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                String username = "";
                boolean userAlreadyExists = false;

                do {
                    username = clientReader.readLine(); // viene inserito il nome letto in una variabile
                    userAlreadyExists = clients.containsKey(username); // controlla che il nome sia univoco all'interno del server

                    if (userAlreadyExists) {
                        clientWriter.println(RED + "Username already exists. Please choose a different one." + RESET);
                    } else {
                        clients.put(username, clientWriter); // aggiunge il nome utente e il writer del client all'elenco dei client connessi
                        clientWriter.println(GREEN + "Hi " + username + ", welcome to the server!" + RESET);
                    }
                } while (userAlreadyExists);

                String _username = username;
                new Thread(() -> {
                    try {
                        String clientMessage;

                        while ((clientMessage = clientReader.readLine()) != null) {
                            if (isAdmin(_username)) {
                                handleAdminCommand(_username, clientMessage);
                            } else {
                                handleGeneralCommands(_username, clientMessage);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        handleLeaveCommand(_username);
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleGeneralCommands (String username, String clientMessage) {
        if (clientMessage.startsWith("/join #")) {
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
            handleGeneralCommands(adminUsername, clientMessage);
        }
    }

    private static void handleJoinCommand(String username, String channel) { // controlla se il canale esiste
        if (!channels.contains("#" + channel)) {
            clients.get(username).println(RED + "this channel doesn't exists" + RESET);
        } else {
            PrintWriter writer = clients.get(username);

            if (userChannels.containsKey(username)) {
                handleLeaveCommand(username);
            }
            if (bannedUsers.containsKey(username)) {
                writer.println(RED + "you have been banned from this channel." + RESET);
            } else {
                userChannels.put(username, channel);

                // Invia un messaggio di benvenuto all'utente appena entrato nel canale

                writer.println("Benvenuto nel canale #" + userChannels.get(username));

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
    }

    private static void handleLeaveCommand(String username) {
        PrintWriter userwriter = clients.get(username);

        if (userChannels.containsKey(username)) {
            String channel = userChannels.get(username);
            userChannels.remove(username);

            PrintWriter writer;

            Set<String> channelUsers = getChannelUsers(channel);
            for (String user : channelUsers) {
                if (!user.equals(username)) {
                    writer = clients.get(user);
                    writer.println(RED + username + " has left channel." + RESET);
                }
            }
        } else {
            userwriter.println(RED + "use this command only if you're in a channel" + RESET);
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

        if (!channelExists(channel)) {
            channels.add("#" + channel);
            writer.println(GREEN + "Canale #" + channel + " è stato creato" + RESET);
        } else {
            // il canale esiste già, informa l'utente
            writer.println(RED + "Il canale di nome '" + channel + "' già esiste. Scegli un nome diverso." + RESET);
        }
    }

    private static boolean channelExists(String channel) {
        return channels.contains(channel);
    }

    private static void handleSendMessageToChannel(String username, String message) {
        if (userChannels.containsKey(username)) {
            String channel = userChannels.get(username);
            Set<String> channelUsers = getChannelUsers(channel);

            for (String user : channelUsers) {
                if (user != username) {
                    PrintWriter writer = clients.get(user);
                    writer.println(BLUE + username + ": "+ RESET + message);
                } else {
                    PrintWriter writer = clients.get(user);
                    writer.println(BLUE + "me: "+ RESET + message);
                }
            }
        }
    }

    private static Set<String> handleShowChannelsList () {
        return channels;
    }

    private static Set<String> handleShowUsersList (String username) {
        String c = userChannels.get(username);
        if (c != null) {
            return userChannels.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(c))
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toSet());
        } else {
            String errorString = "you're not in a channel !";
            Set<String> charSet = new HashSet<>();
            charSet.add(errorString);

            return charSet;
        }
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
                recipientWriter.println(BLUE + "[Private from " + sender + "]: " + RESET + privateMessage);
            } else { // se l'utente non esiste
                clients.get(sender).println(RED + "User " + recipient + " not found." + RESET);
            }
        } else {
            clients.get(sender).println(RED + "Invalid /privmsg command. Usage: /privmsg username message" + RESET);
        }
    }

    private static boolean isAdmin (String username) {
        // verifica se l'username corrisponde a quello di un admin
        return administrators.contains(username);
    }

    private static void handleKickCommand (String adminUsername, String kickuser) {
        if (kickuser == adminUsername) {
            clients.get(adminUsername).println(RED + "non puoi bannarti da solo" + RESET);
        } else {
            if (userChannels.containsKey(kickuser)) {
                String channel = userChannels.get(kickuser);
                handleLeaveCommand(kickuser);

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
                clients.get(adminUsername).println(RED + kickuser + " non è presente nel canale." + RESET);
            }
        }
    }

    private static void handleBanCommand (String adminUsername, String banuser) {
        if (banuser == adminUsername) {
            clients.get(adminUsername).println(RED + "non puoi bannarti da solo" + RESET);
        } else {
            if (userChannels.containsKey(banuser)) {
                String channel = userChannels.get(banuser);
                handleLeaveCommand(banuser);

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
                clients.get(adminUsername).println(RED + banuser + " non è presente nel canale." + RESET);
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
            clients.get(adminUsername).println(RED + unbanuser + " non è attualmente bannato." + RESET);
        }
    }

    private static void handlePromoteCommand (String adminsUsername, String username) {
        if (isAdmin(username)) {
            clients.get(adminsUsername).println(RED + "l'utente è già admin" + RESET);
        } else {
            administrators.add(username);
            clients.get(username).println(GREEN + "l'admin " + adminsUsername + " ti ha promosso ad amministratore" + RESET);
        }
    }
}