grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

LINECOMMENT : '//';
OPENCOMMENT : '/*';
CLOSECOMMENT : '*/';
LBRACKET : '[' ;
RBRACKET : ']' ;
ELLIPSIS : '...' ;
DOT : '.' ;
COLON : ',' ;

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
ADD : '+' ;

SUB : '-';
DIV : '/' ;
AND : '&&' ;
LESS : '<' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
NEG : '!' ;

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
NEW : 'new' ;

IMPORT : 'import' ;
EXTENDS : 'extends' ;
STATIC : 'static' ;
VOID : 'void' ;
MAIN : 'main' ;
STRING : 'String' ;
LENGTH : 'length' ;
THIS : 'this' ;

BOOLEAN : 'true' | 'false' ;
INTEGER : [0-9]+;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [\t\n\r\f ]+ -> skip ;

program
    : importDecl* |classD=classDecl EOF
    ;


importDecl
    : IMPORT name+=ID (DOT name+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID
        (EXTENDS ID)?
        LCURLY
            varDecl*
            methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name=INT #IntType
    | name=INT LBRACKET RBRACKET #ArrayType
    | name=BOOLEAN #BoolType
    | name=INT ELLIPSIS #VarargType
    | name=STRING #StringType
    | name=ID #ObjectType;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY
            varDecl*
            stmt*
            RETURN expr SEMI
        RCURLY #ClassMethod
    | (PUBLIC {$isPublic=true;})?
         STATIC VOID MAIN LPAREN STRING LBRACKET RBRACKET name=ID RPAREN
         LCURLY
            varDecl*
            stmt*
         RCURLY #MainMethod
    ;

param
    : (type name+=ID (COLON type name+=ID)*)?
    ;

stmt
    : LCURLY stmt* RCURLY #Stms
    | name=ID EQUALS expr SEMI #AssignStmt
    | RETURN expr SEMI #ReturnStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #PrintStmt
    | name=ID LBRACKET expr RBRACKET EQUALS expr SEMI #ArrayAssignStmt
    ;

expr
    : LPAREN expr RPAREN #ParensExpr
    | expr DOT LENGTH #LenExpr
    | expr LBRACKET expr RBRACKET #ArrayElemExpr
    | LBRACKET (expr (COLON expr)*)? RBRACKET #ArrayExpr
    | expr DOT name=ID LPAREN (expr (COLON expr)*)? RPAREN #MethodExpr
    | NEW INT LBRACKET expr RBRACKET #NewArrayExpr
    | NEW name=ID LPAREN RPAREN #NewObjectExpr
    | op=NEG expr #NegExpr
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | expr op=LESS expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | value=INTEGER #IntegerLiteral
    | value=BOOLEAN #BooleanLiteral
    | name=THIS #ThisExpr
    | name=ID #VarRefExpr
    ;



