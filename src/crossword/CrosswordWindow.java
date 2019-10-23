package crossword;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableModel;

/**
 * A mutable CrosswordWindow that displays the crossword extravaganza match for a player. Each CrosswordWindow has a title, 
 * description, textbox to take user input, and a footer to display feedback on user input. Depending on the state of the match, 
 * the CrosswordWindow will display the available Matches, the Puzzle, and the Puzzles hints.
 */
public class CrosswordWindow extends JFrame {
    
    // Abstraction Function:
    //  - AF(canvas, title, description, textbox, hints, hintModel, puzzles, puzzlesModel, hintScroll, puzzleScroll, info, 
    //    statusBar) = A CrosswordWindow with a title and a description displaying the state of the Match. The CrosswordCanvas 
    //    displays the Puzzle and guesses that both players have made, passed through the textbox. In the CHOOSE state of a match, 
    //    puzzles, puzzlesModel, and puzzleScroll display available matches to join. In the PLAY state of a Match, CrosswordCanvas
    //    is visible and hints, hintModel, and hintScroll display the Puzzle's hints.
    
    // Representation Invariant:
    //  - True
    
    // Safety From Rep Exposure:
    //  - All fields are private
    //  - All fields are final
    //  - The title and description fields are immutable
    //  - All other fields are mutable but are not shared with the Client
    
    // Thread Safety Argument:
    //  - All fields are final
    //  - The CrosswordWindow contains mutable fields, but is never shared with the Client and is only modified by methods in the 
    //    CrosswordCanvas class
    
    public final CrosswordCanvas canvas;
    private final JLabel title = new JLabel("Welcome,", JLabel.CENTER);
    private final JLabel description = new JLabel("Please enter an ID for yourself. It can't contain spaces or tabs", JLabel.CENTER);
    private final JTextField textbox = new JTextField(30);
    private final JTable hints;
    private final DefaultTableModel hintModel;
    private final JTable puzzles;
    private final  DefaultTableModel puzzlesModel;
    private final JScrollPane hintScroll;
    private final JScrollPane puzzleScroll;
    private final JLabel info = new JLabel();
    private final JLabel statusBar = new JLabel("START state");
    
    /**
     * Create a new CrosswordWindow
     * 
     * @param canvasWidth The width of the windows. width >= 0
     * @param canvasHeight The height of the window. height >= 0
     * @param onEnter An ActionListener that returns when the enter key is pressed
     */
    public CrosswordWindow(int canvasWidth, int canvasHeight, ActionListener onEnter) {
        super("Crossword Extravaganza Client");
        canvas = new CrosswordCanvas();
        final String[] hintHeaders = {"ID", "Hint"}; // https://stackoverflow.com/questions/2316016/how-to-instantiate-an-empty-jtable
        hintModel = new DefaultTableModel(0, hintHeaders.length);
        hintModel.setColumnIdentifiers(hintHeaders);
        hints = new JTable(hintModel);
        hints.setEnabled(false);
        
        final String[] puzzleHeaders = {"ID", "Description"};
        puzzlesModel = new DefaultTableModel(0, puzzleHeaders.length);
        puzzlesModel.setColumnIdentifiers(puzzleHeaders);
        puzzles = new JTable(puzzlesModel);
        puzzles.setEnabled(false);
        
        canvas.setSize(canvasWidth, canvasHeight);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        description.setFont(new Font("Arial", Font.ITALIC, 20));

        final JButton enterButton = new JButton("Enter");
        enterButton.setSize(15, 15);
        enterButton.addActionListener(onEnter);

        textbox.setFont(new Font("Arial", Font.BOLD, 20));
        
        info.setFont(new Font("Arial", Font.PLAIN, 15));
        info.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        
        statusBar.setFont(new Font("Arial", Font.BOLD, 15));
        
        JPanel headerPane = new JPanel();
        headerPane.setLayout(new BorderLayout());
        headerPane.add(title, BorderLayout.NORTH);
        headerPane.add(description);
        JPanel commandPane = new JPanel();
        commandPane.add(textbox);
        commandPane.add(enterButton);
        headerPane.add(commandPane, BorderLayout.SOUTH);
        
        hintScroll = new JScrollPane(hints);
        hintScroll.setBorder(BorderFactory.createTitledBorder("Hints"));
        
        puzzleScroll = new JScrollPane(puzzles);
        puzzleScroll.setBorder(BorderFactory.createTitledBorder("Puzzles"));
        
        JPanel centerPane = new JPanel();
        final BoxLayout box = new BoxLayout(centerPane, BoxLayout.Y_AXIS);
        centerPane.setLayout(box);
        centerPane.add(canvas);
        centerPane.add(info);
        
        setLayout(new BorderLayout());
        add(headerPane, BorderLayout.NORTH);
        add(puzzleScroll, BorderLayout.WEST);
        add(centerPane, BorderLayout.CENTER);
        add(hintScroll, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
        setSize(canvasWidth + 50, canvasHeight + 50);
        setVisible(true);
    }
    
    /**
     * Get the text contained in the textbox
     * 
     * @return the text
     */
    public String getText() {
        return textbox.getText();
    }
    
    /**
     * Set the title of the window
     * 
     * @param newTitle the new title to set
     */
    public void setTitle(final String newTitle) {
        title.setText(newTitle);
    }
    
    /**
     * Set the description of the window
     * 
     * @param newDesc the new description
     */
    public void setDescription(final String newDesc) {
        description.setText(newDesc);
    }
    
    /**
     * Set the rows of the displayed table
     * 
     * @param rows id, clue array to set
     */
    public void setRows(final List<String[]> rows, final String borderTitle) {
        hintModel.setRowCount(0);
        for (String[] row: rows) {
            hintModel.addRow(row);
        }
        hintScroll.setBorder(BorderFactory.createTitledBorder(borderTitle));
    }
    
    /**
     * Display a list of puzzles in the left pane 
     * 
     * @param puzzles the list of puzzles to display
     */
    public void displayPuzzles(final List<Puzzle> puzzles) {
        puzzlesModel.setRowCount(0);
        for (Puzzle puzzle: puzzles) {
            final String[] row = {puzzle.getName(), puzzle.getDescription()};
            puzzlesModel.addRow(row);
        }
        puzzleScroll.setVisible(true);
    }
    
    /**
     * Hide the puzzles list
     */
    public void hidePuzzles() {
        puzzleScroll.setVisible(false);
    }
    
    /**
     * Displays a message in the status bar
     * 
     * @param msg The String to display
     */
    public void setStatus(final String msg) {
        statusBar.setText(msg);
    }
    
    /**
     * Set the name in the title bar
     * 
     * @param name The String to display
     */
    public void setName(final String name) {
        super.setTitle(name);
    }
    
    /**
     * Clear the text in the textbox
     */
    public void clearText() {
        textbox.setText("");
    }
    
    /**
     * Set the text in the infobox
     * 
     * @param msg A Message object
     */
    public void setInfo(final String msg) {
        info.setText("<html>" + msg.replaceAll("\\n", "<br>") + "</html>");
    }
    
}