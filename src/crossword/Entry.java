package crossword;

import java.io.Serializable;

/**
 * A single immutable and thread safe Entry in a crossword Puzzle. Implements serializable to allow Entry objects to be
 * sent over a socket.
 */
public class Entry implements Serializable {
    
    // Abstraction Function:
    // - AF(word, clue, direction, row, col, length) = A crossword entry of word, where length is the number of characters in 
    //                                                 word, moving left to right if direction is ACROSS and top to bottom if
    //                                                 it is DOWN, starting at row, col on a zero indexed puzzle grid with
    //                                                 rows increasing downwards and columns increasing to the right. Clue is 
    //                                                 a hint about the value of word.
    
    // Representation Invariant:
    //  - word contains only a-z and '-'
    //  - clue contains no double quotations
    //  - row >= 0
    //  - col >= 0
    
    // Safety From Rep Exposure:
    //  - All fields are private
    //  - All fields are final
    //  - All fields are immutable
    //  - Fields are not modified, and communication with the client occurs only through immutable data types
    
    // Thread Safety Argument:
    //  - All fields are final
    //  - All fields are immutable
    //  - Defensive copies of all fields are made before the Entry is shared over a socket
    //  - There are no mutators and as such, fields are not modified 
    
    public static enum WordDirection {ACROSS, DOWN};
    public static final WordDirection ACROSS = WordDirection.ACROSS;
    public static final WordDirection DOWN = WordDirection.DOWN;
    
    private final String word;
    private final String clue;
    private final Integer row;
    private final Integer col;
    private final Integer length;
    private final WordDirection direction;
    
    /**
     * Creates a new Entry
     * 
     * @param word the answer for the Entry. Must contain only a-z and '-' characters
     * @param clue the clue for the Entry. All "\r", "\n", "\t", and "\\" patterns will be converted to their corresponding characters
     * @param direction the direction of the entry, either ACROSS or DOWN.
     * @param row the row the of the top left character of the entry. Must be >= 0. 
     * @param col the column of the top left character of the entry. Must be >= 0.
     */
    public Entry(String word, String clue, WordDirection direction, int row, int col) {
        this.word = word.replaceAll("\"", "");
        this.row = row;
        this.col = col;
        this.direction = direction;
        this.length = this.word.length();
        String build = clue.replace("\\r", "\r");
        build = build.replace("\\n", "\n");
        build = build.replace("\\t", "\t");
        build = build.replace("\\\\", "\\");
        this.clue = build.replaceAll("\"", "");
        checkRep();
    }
   
    /**
     * Assert the rep invariant
     */
    private void checkRep() {
        assert(word.matches("[-a-z]+"));
        assert(row >= 0);
        assert(col >= 0);
        assert(!clue.contains("\""));
    }
    
    /**
     * Produces a copy of the Entry that makes it safe for Client-Server interactions by not sharing the value of word
     * 
     * @param that A valid Entry object
     * @return A defensive copy of Entry that without a value for word
     */
    public static Entry prepareForTransfer(Entry that) {
        StringBuilder build = new StringBuilder(); // Null is not allowed here, so generate a string of dashes as long as the word
        for (int i=0; i<that.getWord().length();i++) {
            build.append('-');
        }
        final String word = build.toString();
        final String clue = that.getClue();
        final WordDirection direction = that.getDirection();
        final int row = that.getRow();
        final int column = that.getColumn();
        return new Entry(word, clue, direction, row, column);
    }
    
    /**
     * Get the word that this Entry represents
     * 
     * @return The word/answer for the entry
     */
    public String getWord() {
        checkRep();
        return word;
    }
    
    /**
     * Get the clue for an Entry, without the leading or trailing quotes.
     * 
     * @return The Entry's clue
     */
    public String getClue() {
        checkRep();
        return this.clue;
    }
    
    /**
     * Get the length of the word for Entry
     * 
     * @return The length of Entries word
     */
    public int getLength() {
        checkRep();
        return this.length;
    }
    
    /**
     * Get the entry's direction. The direction can be ACROSS or DOWN
     * 
     * @return The direction of the entry.
     */
    public WordDirection getDirection() {
        checkRep();
        return this.direction;
    }
    
    /**
     * Get the Entry's starting row
     * 
     * @return the row that the Entry starts at
     */
    public int getRow() {
        checkRep();
        return this.row;
    }
    
    /**
     * Get the Entry's starting column
     * 
     * @return the column that the entry starts at
     */
    public int getColumn() {
        checkRep();
        return this.col;
    }
    
    /**
     * @return hash code value consistent with the equals() definition of structural equality, such that for all 
     *         e1,e2:Entry, e1.equals(e2) implies e1.hashCode() == e2.hashCode()
     */
    @Override
    public int hashCode() {
        checkRep();
        return this.toString().hashCode();
    }
    
    /**
     * @param that any object
     * @return true if and only if this and that are structurally-equal Entrys
     */
    @Override
    public boolean equals(Object that) {
        checkRep();
        if (that instanceof Entry) {
            return this.toString().equals(that.toString());
        }
        else {
            return false;
        }
    }
    
    /**
     * @return a human readable representation of the Entry
     */
    @Override
    public String toString() {
        checkRep();
        return "(" + word + ", " + clue + ", " + direction.toString() + ", " + Integer.toString(row) + ", " + Integer.toString(col) + ")";
    }
    
}