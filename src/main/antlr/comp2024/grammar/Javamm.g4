grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

MUL : '*' ;
ADD : '+' ;
SUB : '-';
DIV : '/' ;
AND : '&&' ;
LESS : '<' ;
NEG : '!' ;

RETURN : 'return' ;
CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
NEW : 'new' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;
STATIC : 'static' ;
VOID : 'void' ;
LENGTH : 'length' ;
THIS : 'this' ;

BOOLEAN : 'true' | 'false' ;
INTEGER : [0] | ([1-9][0-9]*);
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

SINGLE_COMMENT : '//' .*? '\n' -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;
WS : [\t\n\r\f ]+ -> skip ;

program
    : importDecl* classD=classDecl EOF
    ;

importDecl
    : IMPORT name+=ID ('.' name+=ID)* ';'
    ;

classDecl
    : CLASS name=ID
        (EXTENDS hyper=ID)?
        '{'
            varDecl*
            methodDecl*
        '}'
    ;

varDecl
    : type name=ID ';'
    ;

type locals[boolean isArray=false]
    : name=INT '[' ']' {$isArray=true;} #ArrayType
    | name=INT '...' {$isArray=true;} #VarargType
    | name=INT #IntType
    | name=BOOLEAN #BoolType
    | name=VOID #VoidType
    | name=ID #ObjectType
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        '(' (param (',' param)*)? ')'
        '{'
            varDecl*
            stmt*
            RETURN expr ';'
        '}'
    | (PUBLIC {$isPublic=true;})?
         STATIC type name=ID '(' ID '[' ']' ID ')'
         '{'
            varDecl*
            stmt*
         '}'
    ;

param
    : type name=ID
    ;

stmt
    : '{' stmt* '}' #Stms
    | name=ID '=' expr ';' #AssignStmt
    | RETURN expr ';' #ReturnStmt
    | 'if' '(' expr ')' stmt 'else' stmt #IfStmt
    | 'while' '(' expr ')' stmt #WhileStmt
    | expr ';' #PrintStmt
    | name=ID '[' expr ']' '=' expr ';' #ArrayAssignStmt
    ;

expr
    : '(' expr ')' #ParensExpr
    | expr '.' LENGTH #LenExpr
    | expr '[' expr ']' #ArrayElemExpr
    | '[' (expr (',' expr)*)? ']' #ArrayExpr
    | expr '.' name=ID '(' (expr (',' expr)*)? ')' #MethodExpr
    | NEW INT '[' expr ']' #NewArrayExpr
    | NEW name=ID '(' ')' #NewObjectExpr
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



