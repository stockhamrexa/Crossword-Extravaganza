 package crossword;

import java.awt.Dimension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An immutable and thread-safe crossword Puzzle. The Puzzle is filled with valid Entries, but does not have to represent a 
 * valid crossword puzzle as defined by the group project handout. Implements serializable to allow Puzzle objects to be
 * sent over a socket.
 */
public class Puzzle implements Serializable {
    
    // Abstraction Function:
    //  - AF(name, description, entries) = A crossword puzzle with name name and description description. The Puzzle consists of 
    //                                     Entries, each of which is mapped to its own unique ID. ID is the index of each Entry
    //                                     in the entries list such that entry.get(i) is an entry with ID i. The Puzzle is zero 
    //                                     indexed such that the top left corner of the puzzle has location 0, 0. Entries may 
    //                                     overlap with each other, but overlaps can potentially make the Puzzle invalid.
                                                            
    // Representation Invariant:
    //  - entries.size() > 0
    //  - Each Entry in entries has a unique ID
    //  - name contains no \r, \n, \t, or \ characters
    //  - name contains no double quotations 
    //  - description contains no double quotations
    
    // Safety From Rep Exposure:
    //  - All fields are private
    //  - All fields are final
    //  - The name and description fields are immutable
    //  - While entries is mutable, it is filled with immutable data types and a defensive copy is made before being sent to the 
    //    Client.
    //  - Fields are not modified by this class or the Client
    
    // Thread Safety Argument:
    //  - All fields are final
    //  - The name and description fields are immutable
    //  - There are no mutators and as such, fields are not modified 
    //  - While entries is mutable, it is filled with immutable data types and a defensive copy is made before being sent to the 
    //    Client.

    private final String name;
    private final String description;
    private final List<Entry> entries;
    
    /**
     * Create a new Puzzle from a name, description and a list of entries
     * 
     * @param name the name of the puzzle. Must not contain any '\n', '\r', '\t', or '\\' characters.
     * @param description the description of the puzzle. Any literal "\n", "\r", "\t", or "\\" patterns will be converted to their corresponding characters
     * @param entries the Entries in the puzzle
     */
    public Puzzle(String name, String description, List<Entry> entries) {
        this.name = name.replaceAll("\"", ""); 
        this.entries = entries;
        String build = description.replace("\\n", "\n");
        build = build.replace("\\r", "\r");
        build = build.replace("\\t", "\t");
        build = build.replace("\\\\", "\\");
        this.description = build.replaceAll("\"", ""); 
        checkRep();
    }
    
    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        List<String> disallowed = List.of("\"", "\r", "\n", "\t", "\\");
        for (String disallow : disallowed) {
            assert(!name.contains(disallow));
        }
        assert(!description.contains("\""));
        assert(entries.size() > 0);
    }

    /**
     * Produces a copy of the Puzzle that makes it safe for Client-Server interactions 
     * 
     * @param that A valid Puzzle object
     * @return A defensive copy of Puzzle that where each Entry in entries has no value for word
     */
    public static Puzzle prepareForTransfer(Puzzle that) {
        final String name = that.getName();
        final String description = that.getDescription();
        final List<Entry> entries = that.entries.stream().map((entry) -> Entry.prepareForTransfer(entry)).collect(Collectors.toList());
        return new Puzzle(name, description, entries);
    }
    
    /**
     * Get the Puzzle's name
     * 
     * @return The name of the Puzzle, without leading or trailing quotes
     */
    public String getName() {
        checkRep();
        return this.name;
    }
    
    /**
     * Get the Puzzle's description
     * 
     * @return The Puzzle's description, without the leading or trailing quotes
     */
    public String getDescription() {
        checkRep();
        return description;
    }
    
    /**
     * Gets the Entry with the given id
     * 
     * @param entryId A unique id representing a given Entry. The id must represent an Entry in the Puzzle, otherwise behavior 
     *        is undefined
     * @return The Entry in this Puzzle with the given id
     */
    public Entry getEntry(int entryId) {
        checkRep();
        assert(entryId >= 0 & entryId <= entries.size()); // Fail quickly if the id is not valid
        return entries.get(entryId);
    }
    
    /**
     * Gets the id of a given Entry
     * 
     * @param entry An Entry object. Must be an Entry in this Puzzle, otherwise behavior is undefined
     * @return The unique id of Entry entry such that getId(entry) == entries.indexOf(entry)
     */
    public String getId(Entry entry) {
        checkRep();
        assert(entries.contains(entry)); // Fail quickly if the Entry is not in this Puzzle
        return Integer.toString(entries.indexOf(entry));
    }
    
    /**
     * List all of the Puzzle's entries and their id's
     * 
     * @return all of the Puzzle's entries mapped to their id's
     */
    public List<Entry> getEntries() {
        checkRep();
        return entries;
    }
    
    /**
     * Determines whether the Puzzle's entries are consistent/ valid as defined by the group project handout
     * 
     * @return true if the Puzzle is consistent and valid, else false
     */
    public boolean isValidPuzzle() {
        checkRep();
        if (noDuplicates()) {
            for (Entry entryOne : entries) {
                for (Entry entryTwo : entries) {
                    if (!entryOne.equals(entryTwo)) {
                        if (entryOne.getDirection().equals(entryTwo.getDirection())) {
                            if (checkOverlap(entryOne, entryTwo)) {
                                return false;
                            }
                        }
                        else {
                            if (checkIntersection(entryOne, entryTwo)) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Checks for duplicate words
     * 
     * @return false if the crossword puzzle has Entries with the same word, else true
     */
    private boolean noDuplicates() {
        checkRep();
        List<String> wordList = new ArrayList<String>();
        for (Entry entry : entries) {
            wordList.add(entry.getWord());
        }
        Set<String> wordSet = new HashSet<String>(wordList); // Convert wordList to a Set to remove duplicate words
        return wordList.size() == wordSet.size();
    }
    
    /**
     * Checks that two different Entries with the same direction do not overlap each other. 
     * 
     * @param entryOne An Entry object
     * @param entryTwo An Entry object
     * @return true if the entries overlap with each other, otherwise false
     */
    private boolean checkOverlap(Entry entryOne, Entry entryTwo) {
        checkRep();
        assert(entryOne.getDirection().equals(entryTwo.getDirection())); // Fail quickly if the Entries are going different directions
        if (entryOne.getDirection().equals(Entry.ACROSS) & entryOne.getRow() == entryTwo.getRow()) { // Both Entries are going the same direction on the same row
            int startIndexOne = entryOne.getRow();
            int endIndexOne = startIndexOne + entryOne.getLength() - 1;
            int startIndexTwo = entryTwo.getRow();
            int endIndexTwo = startIndexTwo + entryTwo.getLength() - 1;
            if (startIndexTwo >= startIndexOne || startIndexTwo <= endIndexOne) {
                return true;
            }
            else if (startIndexOne >= startIndexTwo & startIndexOne <= endIndexTwo) {
                return true;
            }
            else if (endIndexTwo >= startIndexOne & endIndexTwo <= endIndexOne) {
                return true;
            }
            else if (endIndexOne >= startIndexTwo & endIndexOne <= endIndexTwo) {
                return true;
            }
            else {
                return false;
            }
        }
        else if (entryOne.getDirection().equals(Entry.DOWN) & entryOne.getColumn() == entryTwo.getColumn()) { // Both Entries are going the same direction on the same column
            int startIndexOne = entryOne.getColumn();
            int endIndexOne = startIndexOne + entryOne.getLength() - 1;
            int startIndexTwo = entryTwo.getColumn();
            int endIndexTwo = startIndexTwo + entryTwo.getLength() - 1;
            if (startIndexTwo >= startIndexOne | startIndexTwo <= endIndexOne) {
                return true;
            }
            else if (startIndexOne >= startIndexTwo & startIndexOne <= endIndexTwo) {
                return true;
            }
            else if (endIndexTwo >= startIndexOne & endIndexTwo <= endIndexOne) {
                return true;
            }
            else if (endIndexOne >= startIndexTwo & endIndexOne <= endIndexTwo) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false; 
        }
    }
    
    /**
     * Checks that two different Entries with opposite directions do not intersect, and of they do, that they intersect at the 
     * same letter.
     * 
     * @param entryOne An Entry object
     * @param entryTwo An Entry object
     * @return true if the entries intersect at a point with different letters, otherwise false
     */
    private boolean checkIntersection(Entry entryOne, Entry entryTwo) {
        checkRep();
        assert(!entryOne.getDirection().equals(entryTwo.getDirection())); // Fail quickly if the Entries are going the same direction
        List<Dimension> entryOnePoints = new ArrayList<Dimension>();
        List<Dimension> entryTwoPoints = new ArrayList<Dimension>();
        if (entryOne.getDirection().equals(Entry.ACROSS)) { // If entryOne is across, then entryTwo is down
            for (int i = entryOne.getColumn(); i < entryOne.getLength() + entryOne.getColumn(); i++) {
                entryOnePoints.add(new Dimension(entryOne.getRow(), i));
            }
            for (int i = entryTwo.getRow(); i < entryTwo.getLength() + entryTwo.getRow(); i++) {
                entryTwoPoints.add(new Dimension(i, entryTwo.getColumn()));
            }
        }
        else {
            for (int i = entryOne.getRow(); i < entryOne.getLength() + entryOne.getRow(); i++) {
                entryOnePoints.add(new Dimension(i, entryOne.getColumn()));
            }
            for (int i = entryTwo.getColumn(); i < entryTwo.getLength() + entryTwo.getColumn(); i++) {
                entryTwoPoints.add(new Dimension(entryTwo.getRow(), i));
            }
        }

        for (Dimension point : entryOnePoints) {
            if (entryTwoPoints.contains(point)) {
                int row = point.width;
                int column = point.height;
                if (entryOne.getDirection().equals(Entry.ACROSS)) {
                    return entryOne.getWord().charAt(column - entryOne.getColumn()) != entryTwo.getWord().charAt(row - entryTwo.getRow());
                }
                else if (entryOne.getDirection().equals(Entry.DOWN)) {
                    return entryOne.getWord().charAt(row - entryOne.getRow()) != entryTwo.getWord().charAt(column - entryTwo.getColumn());
                }
            }
        }
        return false;
    }

    /**
     * @return hash code value consistent with the equals() definition of structural equality, such that for all 
     *         e1,e2:Puzzle, e1.equals(e2) implies e1.hashCode() == e2.hashCode()
     */
    @Override
    public int hashCode() { 
        checkRep();
        return Objects.hash(name, description, entries);
    }
    
    /**
     * @param that any object
     * @return true if and only if this and that are structurally-equal Puzzles
     */
    @Override
    public boolean equals(Object that) {
        checkRep();
        if (that instanceof Puzzle) {
            Puzzle other = (Puzzle) that;
            return name.equals(other.name) && description.equals(other.description) && entries.equals(other.entries);
        }
        else {
            return false;
        }   
    }
    
    /**
     * @return a human readable representation of the Message
     */
    @Override
    public String toString() { 
        checkRep();
        String puzzleString = "<" + name + " - " + description + ":";
        for (Entry entry : entries) {
            puzzleString += " " + getId(entry) + ": " + entry.toString();
        }
        return puzzleString + ">";
    }
    
}