package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import crossword.Entry.WordDirection;
import crossword.Message.MessageType;

public class MessageTest {
    
    /**
     * Testing strategy for Messages
     * 
     * Partition on the type of the messages sent 
     *  - SET_ID
     *  - PUZZLES
     *  - GAME_STATE
     *  - MATCHES
     *  - PICK_MATCH
     *  - GAME_START
     *  - EXIT, TRY, CHALLENGE
     *  - ERROR
     *  - SCORE
     */  

    @Test
    // covers: A message with type of SET_ID
    public void testEqualsMessagetTypeSET_ID() {
        assertEquals(Message.setId("ID101").type, MessageType.SET_ID, "expected message type to be equal to SET_ID");
    }

    @Test
    // covers: A message with type of PUZZLES
    public void testEqualsMessagetTypePUZZLES() {
        assertEquals(Message.sendPuzzles(new ArrayList<Puzzle>()).type, MessageType.PUZZLES,
                "expected message type to be equal to PUZZLES");
    }

    @Test
    // covers: A message with type of GAME_STATE
    public void testEqualsMessagetTypeGAME_STATE() {
        Entry e1 = new Entry("word", "clue", WordDirection.ACROSS, 0, 1);
        ArrayList<Entry> entries = new ArrayList<Entry>();
        entries.add(e1);
        Puzzle p1 = new Puzzle("name", "description", entries);

        assertEquals(Message.sendMatchState(new Match.MatchState(new Match(p1, "playrOneId"))).type,
                MessageType.GAME_STATE, "expected message type to be equal to GAME_STATE");
    }

    @Test
    // covers: A message with type of MATCHES
    public void testEqualsMessagetTypeMATCHES() {
        assertEquals(Message.sendMatches(new HashMap<String, String>()).type, MessageType.MATCHES,
                "expected message type to be equal to MATCHES");
    }

    @Test
    // covers: A message with type of PICK_MATCH
    public void testEqualsMessagetTypePICK_MATCH() {
        assertEquals(Message.playMatch("ID101").type, MessageType.PICK_MATCH,
                "expected message type to be equal to PICK_MATCH");
    }

    @Test
    // covers: A message with type of NEW_MATCH
    public void testEqualsMessagetTypeNEW_MATCH() {
        assertEquals(Message.newMatch("matchId", "puzzleId", "description").type, MessageType.NEW_MATCH,
                "expected message type to be equal to NEW_MATCH");
    }

    @Test
    // covers: A message with type of GAME_START
    public void testEqualsMessagetTypeGAME_START() {
        assertEquals(Message.gameStart("name").type, MessageType.GAME_START,
                "expected message type to be equal to GAME_START");
    }

    @Test
    // covers: A message with type of EXIT
    public void testEqualsMessagetTypeEXIT() {
        assertEquals(Message.exit().type, MessageType.EXIT, "expected message type to be equal to EXIT");
    }

    @Test
    // covers: A message with type of TRY
    public void testEqualsMessagetTypeTRY() {
        assertEquals(Message.tryWord(101, "word").type, MessageType.TRY, "expected message type to be equal to TRY");
    }

    @Test
    // covers: A message with type of CHALLENGE
    public void testEqualsMessagetTypeCHALLENGE() {
        assertEquals(Message.challengeWord(101, "word").type, MessageType.CHALLENGE,
                "expected message type to be equal to CHALLENGE");
    }

    @Test
    // covers: A message with type of ERROR
    public void testEqualsMessagetTypeERROR() {
        assertEquals(Message.error("error").type, MessageType.ERROR, "expected message type to be equal to ERROR");
    }

    @Test
    // covers: A message with type of SCORE
    public void testEqualsMessagetTypeSCORE() {
        assertEquals(Message.score("100").type, MessageType.SCORE, "expected message type to be equal to SCORE");
    }

}
