package crossword;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.mit.eecs.parserlib.ParseTree;
import edu.mit.eecs.parserlib.Parser;
import edu.mit.eecs.parserlib.UnableToParseException;

public class PuzzleParser {
    
    /**
     * Main method. Parses an example Puzzle.
     * 
     * @param args unused command line arguments
     * @throws UnableToParseException if the example did not parse correctly
     * @throws IOException if the example could not be opened
     */
    public static void main(final String[] args) throws UnableToParseException, IOException {
        final String input = new String(Files.readAllBytes(Paths.get("puzzles/complex/complex.puzzle")));
        Puzzle puzzle = parse(input);
    }
    
    // Non-terminals of the grammar
    private static enum ExpressionGrammar {
        FILE, NAME, DESCRIPTION, ENTRY, WORDNAME, CLUE, DIRECTION, ROW, COL, STRINGIDENT, 
        STRING, INT, WHITESPACE, ENTRIES, NEWLINES
    }
    
    private static Parser<ExpressionGrammar> parser = makeParser();
    
    /**
     * Compile the grammar into a parser.
     * 
     * @return parser for the grammar
     * @throws RuntimeException if grammar file can't be read or has syntax errors
     */
    private static Parser<ExpressionGrammar> makeParser() {
        // Try to read the grammar file relative to the project root
        try {
            // Read the grammar as a file, relative to the project root.
            final File grammarFile = new File("src/crossword/Puzzle.g");
            return Parser.compile(grammarFile, ExpressionGrammar.FILE);
            
        // Parser.compile() throws two checked exceptions.
        // Translate these checked exceptions into unchecked RuntimeExceptions,
        // because these failures indicate internal bugs rather than client errors
        } 
        catch (IOException e) {
            throw new RuntimeException("can't read the grammar file", e);
        } 
        catch (UnableToParseException e) {
            throw new RuntimeException("the grammar has a syntax error", e);
        }
    }
    
    /**
     * Parse a string into an expression.
     * 
     * @param string string to parse
     * @return Expression parsed from the string
     * @throws UnableToParseException if the string doesn't match the Expression grammar
     */
    public static Puzzle parse(final String string) throws UnableToParseException {
        // Parse the example into a parse tree
        final ParseTree<ExpressionGrammar> parseTree = parser.parse(string);

        // Display the parse tree in various ways, for debugging only
        // System.out.println("parse tree " + parseTree);
        // Visualizer.showInBrowser(parseTree);

        return makeAbstractSyntaxTree(parseTree); // Make an AST from the parse tree
    }

    /**
     * Convert a parse tree into an abstract syntax tree.
     * 
     * @param parseTree constructed according to the grammar in Puzzle.g
     * @return abstract syntax tree corresponding to parseTree
     */
    private static Puzzle makeAbstractSyntaxTree(final ParseTree<ExpressionGrammar> tree) {
        // Because a Puzzle is not a recursive datatype, this matches only the FILE nonterminal
        assert tree.name() == ExpressionGrammar.FILE;
        final List<ParseTree<ExpressionGrammar>> children = tree.children();
        final String name = children.get(0).text();
        final String description = children.get(1).text();
        final List<Entry> entries = new ArrayList<Entry>();
        // For each ENTRY nonterminal, get its children and build the actual Entry object
        final ParseTree<ExpressionGrammar> treeEntries = children.get(2);
        for (ParseTree<ExpressionGrammar> treeEntry : treeEntries.children()) {
            final List<ParseTree<ExpressionGrammar>> entryParts = treeEntry.children();
            final String word = entryParts.get(0).text();
            final String clue = entryParts.get(1).text();
            final String direction = entryParts.get(2).text();
            final int row = Integer.parseInt(entryParts.get(2+1).text());
            final int col = Integer.parseInt(entryParts.get(2+2).text());
            if (direction.equals("ACROSS")) {
                entries.add(new Entry(word, clue, Entry.ACROSS, row, col));
            } else { // direction.equals("DOWN")
                entries.add(new Entry(word, clue, Entry.DOWN, row, col));
            }
        }
        return new Puzzle(name, description, entries);
    }
    
}