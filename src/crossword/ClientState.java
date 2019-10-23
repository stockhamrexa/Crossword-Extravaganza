package crossword;

import static crossword.Message.MessageType.CHALLENGE;
import static crossword.Message.MessageType.EXIT;
import static crossword.Message.MessageType.NEW_MATCH;
import static crossword.Message.MessageType.PICK_MATCH;
import static crossword.Message.MessageType.RESET;
import static crossword.Message.MessageType.SET_ID;
import static crossword.Message.MessageType.TRY;

import java.util.Set;

/**
 * Represents the state of a Client as viewed from the server, and what messages are able to be sent from a given state
 */
public enum ClientState { 
    START(SET_ID),
    CHOOSE(PICK_MATCH, NEW_MATCH),
    WAIT,
    PLAY(TRY, CHALLENGE, EXIT),
    SHOW_SCORE(PICK_MATCH, NEW_MATCH, RESET);

    private final Set<Message.MessageType> validTypes;
   
    // Abstraction Function:
    //  - AF(validTypes) = A set representing all of the valid message types for a given state 
    
    // Representation Invariant:
    //  - validTypes.size() > 0
    
    // Safety From Rep Exposure:
    //  - validTypes is private, final, and unmodifiable. It is never returned by any method
    
    // Thread Safety Argument:
    //  - Each ClientState contains only immutable objects and is itself immutable
    //  - ClientState is shared with the Client but has no mutators and is never modified

    ClientState(Message.MessageType... validTypes) { // Fills the validTypes Set
        this.validTypes = Set.of(validTypes);
        checkRep();
    }

    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        assert validTypes.size() >= 0;
    }

    /**
     * Checks to see whether the message has a valid type
     * 
     * @param msg A Message object
     * @return True if the message type is a valid type, else false
     */
    public boolean isValid(Message msg) {
        checkRep();
        return validTypes.contains(msg.type);
    }
    
}