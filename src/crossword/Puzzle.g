@skip newlines {
    ENTRIES ::= ENTRY*;  
}

@skip whitespace {
    FILE ::= ">>" NAME DESCRIPTION "\n" ENTRIES;
    ENTRY ::= "("  WORDNAME ","  CLUE "," DIRECTION "," ROW "," COL ")";
}

NAME ::= stringident;
DESCRIPTION ::= string;
WORDNAME ::= [a-z\-]+;
CLUE ::= string;
DIRECTION ::= "DOWN" | "ACROSS";
ROW ::= int;
COL ::= int;
string ::= '"' ([^"\r\n\\] | '\\' [\\nrt] )* '"';
stringident ::= '"' [^"\r\n\t\\]* '"';
int ::= [0-9]+;
whitespace ::= [ \t\r]+;
newlines ::= [ \t\r\n]+;