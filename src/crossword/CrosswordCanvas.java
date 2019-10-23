package crossword;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

/**
 * A CrosswordCanvas used to draw a crossword puzzle
 */
class CrosswordCanvas extends JComponent {
    
    private static final int originX = 100; // Horizontal offset from corner for first cell
    private static final int originY = 100; // Vertical offset from corner for first cell
    private static final int delta = 30; // Size of each cell in crossword
    private static final Font mainFont = new Font("Arial", Font.PLAIN, delta * 4 / 5); // Font for letters in the crossword
    private static final Font indexFont = new Font("Arial", Font.PLAIN, delta / 3); // Font for small indices used to indicate an ID in the crossword
    private static final Font textFont = new Font("Arial", Font.PLAIN, 16); // Font for small indices used to indicate an ID in the crossword
    private static final Color RED = new Color(191, 53, 53);
    private static final Color BLUE = new Color(53, 103, 191);
    private static final Color BLACK = new Color(0, 0, 0);
    private int line = 0; // Position where the next line of code will be written
    
    private List<Entry> entries = new ArrayList<>(); // List of entries
    private Map<Integer, String> words = new HashMap<>(); // Map from entry indices to words
    private Map<Integer, String> owners = new HashMap<>(); // Map from entry indices to owner
    private Map<Integer, Boolean> confirmed = new HashMap<>(); // Map from entry indices to whether it has been confirmed
    private String playerId = " "; 
    
    // Abstraction function: 
    //  - AF(line, entries) = A canvas where entries are the puzzle entries to be displayed when the canvas is repainted, 
    //                        and line is the position where the next output line will be written
    
    // Representation Invariant:
    //  - line >= 0
    
    // Safety From Rep Exposure:
    //  - All fields are private, and there are no methods that return any of them. entries is an unmodifiable list.
    
    // Thread Safety Argument:
    //  - Not guaranteed to be threadsafe because of how Swing handles threads. Use only from one thread.
    
    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        assert line >= 0;
    }
    
    /**
     * Resets the line counter
     */
    private void resetLine() {
        line = 0;
        checkRep();
    }
    
    /**
     * Draw a cell at position (row, col) in a crossword.
     * 
     * @param row Row where the cell is to be placed.
     * @param col Column where the cell is to be placed.
     * @param g Graphics environment used to draw the cell.
     */
    private void drawCell(int row, int col, Graphics g) {
        g.setColor(new Color(0, 0, 0));
        g.drawRect(originX + col * delta, originY + row * delta, delta, delta);
        checkRep();
    }

    /**
     * Place a letter inside the cell at position (row, col) in a crossword.
     * 
     * @param letter Letter to add to the cell.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param c the color to write in
     * @param g Graphics environment to use.
     */
    private void letterInCell(String letter, int row, int col, Color c, Graphics g) {
        g.setFont(mainFont);
        FontMetrics fm = g.getFontMetrics();
        final Color oldColor = g.getColor();
        g.setColor(c);
        g.drawString(letter, originX + col * delta + delta / 6, originY + row * delta + fm.getAscent() + delta / 10);
        g.setColor(oldColor);
        checkRep();
    }

    /**
     * Add a vertical ID for the cell at position (row, col).
     * 
     * @param id ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param c The color to write in
     * @param g Graphics environment to use.
     */
    private void verticalId(String id, int row, int col, Color c, Graphics g) {
        g.setFont(indexFont);
        final Color oldColor = g.getColor();
        g.setColor(c);
        g.drawString(id, originX + col * delta + delta / 8, originY + row * delta - delta / 15);
        g.setColor(oldColor);
        checkRep();
    }

    /**
     * Add a horizontal ID for the cell at position (row, col).
     * 
     * @param id ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param c the color to write in
     * @param g Graphics environment to use.
     */
    private void horizontalId(String id, int row, int col, Color c, Graphics g) {
        g.setFont(indexFont);
        FontMetrics fm = g.getFontMetrics();
        int maxwidth = fm.charWidth('0') * id.length();
        final Color oldColor = g.getColor();
        g.setColor(c);
        g.drawString(id, originX + col * delta - maxwidth - delta / 8, originY + row * delta + fm.getAscent() + delta / 15);
        g.setColor(oldColor);
        checkRep();
    }

    /**
     * Writes a single line of text with a particular color
     * 
     * @param s The line of text to be written
     * @param g Graphics environment to use
     */
    private void println(String s, Graphics g) {
        g.setFont(textFont);
        FontMetrics fm = g.getFontMetrics();
        Color oldColor = g.getColor(); // Save the old color before using a new one
        g.setColor(new Color(100, 0, 0));
        g.drawString(s, originX + 500, originY + line * fm.getAscent() * 6 / 5);
        g.setColor(oldColor); // Reset the color
        ++line;
        checkRep();
    }

    /**
     * Writes a single line of text with a particular color and sets the background color
     * 
     * @param s The line of text to be written
     * @param g Graphics environment to use
     */
    private void printlnFancy(String s, Graphics g) {
        g.setFont(textFont);
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getAscent() * 6 / 5;
        int xpos = originX + 500;
        int ypos = originY + line * lineHeight;
        Color oldColor = g.getColor();

        g.setColor(new Color(0, 0, 0)); // Save the old color before using a new one
        g.fillRect(xpos, ypos - fm.getAscent(), fm.stringWidth(s), lineHeight);
        g.setColor(new Color(200, 200, 0));
        g.drawString(s, xpos, ypos);
        g.setColor(oldColor); // Reset the color
        ++line;
        checkRep();
    }
    
    /**
     * Paints the crossword puzzle and all of the guesses that players have made onto the CrosswordCanvas. Called every time the
     * canvas is repainted.
     * 
     * @param g Graphics environment to use
     */
    @Override
    public void paint(Graphics g) { // Repaint the words in the puzzle
        checkRep();
        for (int i=0; i<entries.size();i++) {
            final String word = words.get(i);
            final Entry entry = entries.get(i);
            final String owner = owners.get(i);
            final boolean isConfirmed = confirmed.getOrDefault(i, false);
            final Color idColor; // Used for coloring ids
            final Color wordColor; // Used for coloring words
            if (owner == null) { // No owner
                wordColor = BLACK;
                idColor = BLACK;
            } else if (owner.equals(playerId)) {
                wordColor = BLUE;
                idColor = isConfirmed ? BLUE : BLACK;
            } else {
                wordColor = RED;
                idColor = isConfirmed ? RED : BLACK;
            }
            if (entry.getDirection() == Entry.ACROSS) {
                horizontalId(Integer.toString(i), entry.getRow(), entry.getColumn(), idColor, g);
                for (int j=0; j<word.length(); j++) {
                    drawCell(entry.getRow(), entry.getColumn()+j, g);
                    final String letter = Character.toString(word.charAt(j));
                    if (!letter.equals(" ")) { // If the letter is not a space, draw it
                        letterInCell(letter, entry.getRow(), entry.getColumn()+j, wordColor, g);
                    }
                }
            } else {
                verticalId(Integer.toString(i), entry.getRow(), entry.getColumn(), idColor, g);
                for (int j=0; j<word.length(); j++) {
                    drawCell(entry.getRow()+j, entry.getColumn(), g);
                    final String letter = Character.toString(word.charAt(j));
                    if (!letter.equals(" ")) { // If the letter is not a space, draw it
                        letterInCell(letter, entry.getRow()+j, entry.getColumn(), wordColor, g);
                    }
                }
            }
        }
    }
    
    /**
     * Show a (blank) puzzle on the canvas. Loads the Canvas for the first time
     * 
     * @param puzzle the puzzle to show
     */
    public void showPuzzle(final Puzzle puzzle) {
        entries = puzzle.getEntries();
        words.clear();
        for (int i=0; i<entries.size();i++) {
            StringBuilder build = new StringBuilder(); // Build a string of spaces as long as the entry's word
            for (int j=0;j<entries.get(i).getWord().length();j++) {
                build.append(' ');
            }
            words.put(i, build.toString());
        }
        repaint();
        checkRep();
    }
    
    /**
     * Show an ongoing match on the canvas
     * 
     * @param match the match to show
     * @param playerId the id of this canvas/player
     */
    public void showMatch(final Match.MatchState match, final String playerId) { 
        this.playerId = playerId;
        words.clear();
        owners.clear();
        confirmed.clear();
        for (int i = 0; i < entries.size(); i++) {
            if (match.guesses.containsKey(i)) {
                words.put(i, match.guesses.get(i));
                owners.put(i, match.owner.get(i));
                confirmed.put(i, match.confirmed.get(i));
            }
            else {
                String word = "";
                for (int j = 0; j < entries.get(i).getWord().length(); j++) {
                    word += " ";
                }
                words.put(i, word);
                owners.put(i, null);
                confirmed.put(i, false);
            }
        }

        repaint();
        checkRep();
    }
    
    /**
     * Clears the canvas. Called after a match end and a player requests NEW MATCH
     */
    public void clear() {
        entries.clear();
        owners.clear();
        confirmed.clear();
        repaint();
        checkRep();
    }
    
}