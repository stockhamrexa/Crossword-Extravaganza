package crossword;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import crossword.Message.MessageType;

/**
 * A Client which extends Thread to allow for simultaneous play of a crossword extravaganza match
 */
public class ClientConnection extends Thread {
    
    // Abstraction Function:
    //  - AF(socket, server, os, is, id, match, state) = A connection between the CrosswordServer server, communicating with 
    //    Object input and output streams os and is over Socket socket. Each ClientConnection has a unique ID that they choose
    //    in the START state of Match match. Each Client also has a game state.
    
    // Representation Invariant:
    //  - If state == START, id == null
    
    // Safety From Rep Exposure:
    //  - All fields are private
    //  - The socket, server, os, and is fields are final
    //  - The id and state fields are immutable
    //  - All communication with the Client occurs with immutable objects
    //  - All modifiable fields are modified with this classes methods, maintaining the rep invariant
    
    // Thread Safety Argument:
    //  - All fields are private
    //  - The socket, server, os, and is fields are final
    //  - The id and state fields are immutable
    //  - All communication with the Client occurs with immutable objects
    //  - This class extends Thread, and is treated as a thread

    private final Socket socket;
    private final CrosswordServer server;
    private final ObjectOutputStream os;
    private final ObjectInputStream is;

    private String id;
    private Match match;
    private ClientState state = ClientState.START;

    public ClientConnection(Socket socket, CrosswordServer crosswordServer) throws IOException {
        this.socket = socket;
        this.server = crosswordServer;
        this.os = new ObjectOutputStream(socket.getOutputStream());
        this.is = new ObjectInputStream(socket.getInputStream());
        checkRep();
    }
    
    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        if (state.equals(ClientState.START)) {
            assert(id == null);
        }
        else {
            assert(id != null);
        }
    }

    /**
     * Handle a single client connection. Returns when the client disconnects.
     *
     * @throws ClassNotFoundException If the input stream sends an unrecognizeable input
     */
    @Override
    public void run() {
        checkRep();
        try (socket; os; is;) {
            while (!socket.isClosed()) {
                os.reset(); // Reset the output stream
                Message msg = (Message) is.readObject();
                System.out.println("Received message type " + msg.type.toString());
                if (!state.isValid(msg)) {
                    sendError("Invalid message");
                }
                switch (msg.type) {
                    case SET_ID: {
                        if (msg.inner instanceof String && isValidId((String) msg.inner)) {
                            this.id = (String) msg.inner;
                            this.state = ClientState.CHOOSE;
                            sendMatchesAndPuzzles();
                        } else {
                            sendError("Invalid id");
                        }
                    }
                    break;
                    case NEW_MATCH: { 
                        if (!(msg.inner instanceof List)) {
                            sendError("Invalid format");
                        }
                        List<String> parts = (List<String>) msg.inner;
                        final boolean matchCreated = server.createMatch(id, parts.get(0), parts.get(1)); 
                        server.updateMatches(); // Live update the list of matches                      
                        if (matchCreated) {
                            state = ClientState.WAIT;
                            os.writeObject(new Message(MessageType.WAIT, true));
                        } else {
                            os.writeObject(new Message(MessageType.WAIT, false));
                        }
                    }
                    break;
                    case PICK_MATCH: {
                        if (msg.inner instanceof String & server.getOpenMatches().keySet().contains((String) msg.inner)) { // If it is the ID of a match that exists
                            match = server.joinMatch(id, (String) msg.inner);
                            if (match != null) {
                                state = ClientState.PLAY;                               
                                server.updateMatches(); // Live update the list of matches                                                    
                                os.writeObject(Message.gameStart(match.getPuzzle().getName()));
                            } else {
                                sendError("Match already started");
                            }
                        } else {
                            sendError("Wrong match");
                        }
                    }
                    break;
                    case TRY: {
                        if (msg.inner instanceof Message.Attempt && match != null) {
                            final Message.Attempt attempt = (Message.Attempt) msg.inner;
                            final boolean validGuess = match.tryWord(id, Integer.toString(attempt.wordId), attempt.word);
                            System.out.println("TRY " + Integer.toString(attempt.wordId) + " " + attempt.word + " " + Boolean.toString(validGuess));
                            if (!validGuess) {
                                sendError("You cannot try to guess that word");
                           } else {
                               if (match.isGameOver()) { // Check for the game to be over
                                   server.updateState(match);
                                   server.sendScore(match);
                                   server.bothShowScore(match);
                               }
                               else {
                                   server.updateState(match);
                               }
                            }
                        } else {
                            sendError("Wrong message");
                        }
                    }
                    break;
                    case CHALLENGE: {
                        if (msg.inner instanceof Message.Attempt && match != null) {
                            final Message.Attempt attempt = (Message.Attempt) msg.inner;
                            final boolean validChallenge = match.challengeWord(id, Integer.toString(attempt.wordId), attempt.word);
                            if (!validChallenge) {
                                sendError("You cannot challenge that word");
                            } else {
                               if (match.isGameOver()) { // Check for the game to be over
                                   server.updateState(match);
                                   server.sendScore(match);
                                   server.bothShowScore(match);
                               }
                               else {
                                   server.updateState(match);
                               }
                            }
                        } else {
                            sendError("Wrong message");
                        }
                    }
                    break;
                    case EXIT: {                                       
                        if (state.equals(ClientState.PLAY)) {
                            server.sendScore(match);
                            server.bothShowScore(match);
                        } else {
                            os.writeObject(Message.exit());
                            socket.close();
                            // Quit the match *after* messages have been sent and not in the play state
                            server.quitMatch(this);                        
                            server.updateMatches(); // Live update the list of matches when one is quit
                        }
                    }
                    break;
                    case RESET: {
                        if (state.equals(ClientState.SHOW_SCORE)) {
                            state = ClientState.CHOOSE;
                            server.updateMatches(); // Live update the list of matches   
                        } 
                    }
                    break;
                    default: {
                        throw new IOException("Unimplemented message type " + msg.type.toString());
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Client disconnected");
        }
    }

    /**
     * Initiate a match with a client and transition to play state
     *
     * @param match A Match object
     */
    public void startMatch(Match match) {
        checkRep();
        try {
            state = ClientState.PLAY;
            this.match = match;
            os.reset();
            os.writeObject(Message.gameStart(match.getPuzzle().getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a user id is valid and is ready to be matched
     *
     * @param id Checks with the server to see if the ID is valid and available
     * @return true if id is not null and is available 
     */
    private boolean isValidId(String id) { 
        checkRep();
        return id != null && !id.isBlank() && server.isIdFree(id);
    }

    /**
     * Send the matches and valid puzzles to the client to pick one
     * 
     * @throws IOException If there is an error communicating with the Client
     */
    private void sendMatchesAndPuzzles() throws IOException {
        checkRep();
        os.reset();
        os.writeObject(Message.sendPuzzles(server.getPuzzlesForClient()));
        os.writeObject(Message.sendMatches(server.getOpenMatches()));
    }
    
    /**
     * Updates the list of available matches for a client
     * 
     * @throws IOException If an error occurs communicating with the Client
     */
    public void updateMatches() throws IOException {
        checkRep();
        os.reset();
        os.writeObject(Message.sendMatches(server.getOpenMatches()));
    }

    public void sendStateUpdate(Match.MatchState state) {
        checkRep();
        try {
            os.reset();
           os.writeObject(Message.sendMatchState(state));
        }catch (IOException e) {
           e.printStackTrace();
        }
    }

    public void sendScore() {
        checkRep();
        try {
            os.reset();
            os.writeObject(Message.score(match.getWinner()));
        } catch (IOException e) {
           e.printStackTrace();
       }
    }

    /**
     * Handles sending error messages to the client  
     *
     * @param details string indicating the error message. For example: invalid Id, No such puzzle, etc  
     * @throws IOException If there is an error communicating with the Client
     */
    private void sendError(String details) throws IOException {
        checkRep();
        os.reset();
        os.writeObject(Message.error(details));
    }

    /**
     * Fetch client's id
     * 
     * @return id represents a client's id in the game
     */
    public String getClientId() {
        checkRep();
        return id;
    }
    
    /**
     * Fetch the clients current state
     * 
     * @return The state instance variable
     */
    public ClientState getClientState() {
        checkRep();
        return state;
    }
    
    /**
     * Sets this clients state field to state
     */
    public void setState(ClientState state) {
        checkRep();
        this.state = state;
    }
    
}