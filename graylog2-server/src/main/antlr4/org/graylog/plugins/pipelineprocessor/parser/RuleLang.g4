/*
 Parts of the grammar are derived from the Java.g4 grammar at https://github.com/antlr/grammars-v4/blob/master/java/Java.g4
 Those parts are under the following license:

 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 All rights reserved.
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.
 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

grammar RuleLang;

@header {
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
}

file
    :   (   ruleDeclaration
        |   pipelineDeclaration
        )+
        EOF
    ;

pipelineDecls
    :   pipelineDeclaration+ EOF
    ;

pipeline
    :   pipelineDeclaration EOF
    ;

pipelineDeclaration
    :   Pipeline name=String
        stageDeclaration+
        End
    ;

stageDeclaration
    :   Stage stage=Integer Match modifier=(All|Either)
        ruleRef*
    ;

ruleRef
    :   Rule name=String ';'?
    ;

ruleDecls
    :   ruleDeclaration+ EOF
    ;

ruleDeclaration
    :   Rule name=String
        When condition=expression
        (Then actions=statement*)?
        End
    ;

expression
    :   '(' expression ')'                                              # ParenExpr
    |   literal                                                         # LiteralPrimary
    |   functionCall                                                    # Func
    |   Identifier                                                      # Identifier
    |   '[' (expression (',' expression)*)* ']'                         # ArrayLiteralExpr
    |   '{' (propAssignment (',' propAssignment)*)* '}'                 # MapLiteralExpr
    |   MessageRef '.' field=expression                                 # MessageRef
    |   fieldSet=expression '.' field=expression                        # Nested
    |   array=expression '[' index=expression ']'                       # IndexedAccess
    |   sign=('+'|'-') expr=expression                                  # SignedExpression
    |   Not expression                                                  # Not
    |   left=expression mult=('*'|'/'|'%') right=expression             # Multiplication
    |   left=expression add=('+'|'-') right=expression                  # Addition
    |   left=expression comparison=('<=' | '>=' | '>' | '<') right=expression # Comparison
    |   left=expression equality=('==' | '!=') right=expression         # Equality
    |   left=expression and=And right=expression                        # And
    |   left=expression or=Or right=expression                          # Or
    ;

propAssignment
    :   Identifier ':' expression
    ;

statement
    :   functionCall ';'                          # FuncStmt
    |   Let varName=Identifier '=' expression ';' # VarAssignStmt
    |   ';'                                       # EmptyStmt
    ;

functionCall
    :   funcName=Identifier '(' arguments? ')'
    ;

arguments
    :   propAssignment (',' propAssignment)*    # NamedArgs
    |   expression (',' expression)*            # PositionalArgs
    ;

literal
    :   Integer     # Integer
    |   Float       # Float
    |   Char        # Char
    |   String      # String
    |   Boolean     # Boolean
    ;

// Lexer

All : A L L;
Either: E I T H E R;
And : A N D | '&&';
Or: O R | '||';
Not: N O T | '!';
Pipeline: P I P E L I N E;
Rule: R U L E;
During: D U R I N G;
Stage: S T A G E;
When: W H E N;
Then: T H E N;
End: E N D;
Let: L E T;
Match: M A T C H;
MessageRef: '$message';

Boolean
    :   'true'|'false'
    ;

// Integer literals

Integer
    :   DecimalIntegerLiteral
    |   HexIntegerLiteral
    |   OctalIntegerLiteral
    |   BinaryIntegerLiteral
    ;

fragment
DecimalIntegerLiteral
    :   Sign? DecimalNumeral IntegerTypeSuffix?
    ;

fragment
HexIntegerLiteral
    :   Sign? HexNumeral IntegerTypeSuffix?
    ;

fragment
OctalIntegerLiteral
    :   Sign? OctalNumeral IntegerTypeSuffix?
    ;

fragment
BinaryIntegerLiteral
    :   Sign? BinaryNumeral IntegerTypeSuffix?
    ;

fragment
IntegerTypeSuffix
    :   [lL]
    ;

fragment
DecimalNumeral
    :   '0'
    |   NonZeroDigit (Digits? | Underscores Digits)
    ;

fragment
Digits
    :   Digit (DigitOrUnderscore* Digit)?
    ;

fragment
Digit
    :   '0'
    |   NonZeroDigit
    ;

fragment
NonZeroDigit
    :   [1-9]
    ;

fragment
DigitOrUnderscore
    :   Digit
    |   '_'
    ;

fragment
Underscores
    :   '_'+
    ;

fragment
HexNumeral
    :   '0' [xX] HexDigits
    ;

fragment
HexDigits
    :   HexDigit (HexDigitOrUnderscore* HexDigit)?
    ;

fragment
HexDigit
    :   [0-9a-fA-F]
    ;

fragment
HexDigitOrUnderscore
    :   HexDigit
    |   '_'
    ;

fragment
OctalNumeral
    :   '0' Underscores? OctalDigits
    ;

fragment
OctalDigits
    :   OctalDigit (OctalDigitOrUnderscore* OctalDigit)?
    ;

fragment
OctalDigit
    :   [0-7]
    ;

fragment
OctalDigitOrUnderscore
    :   OctalDigit
    |   '_'
    ;

fragment
BinaryNumeral
    :   '0' [bB] BinaryDigits
    ;

fragment
BinaryDigits
    :   BinaryDigit (BinaryDigitOrUnderscore* BinaryDigit)?
    ;

fragment
BinaryDigit
    :   [01]
    ;

fragment
BinaryDigitOrUnderscore
    :   BinaryDigit
    |   '_'
    ;

// Floats
Float
    :   Sign? DecimalFloatingPointLiteral
    |   Sign? HexadecimalFloatingPointLiteral
    ;

fragment
DecimalFloatingPointLiteral
    :   Digits '.' Digits? ExponentPart? FloatTypeSuffix?
    |   '.' Digits ExponentPart? FloatTypeSuffix?
    |   Digits ExponentPart FloatTypeSuffix?
    |   Digits FloatTypeSuffix
    ;

fragment
ExponentPart
    :   ExponentIndicator SignedInteger
    ;

fragment
ExponentIndicator
    :   [eE]
    ;

fragment
SignedInteger
    :   Sign? Digits
    ;

fragment
Sign
    :   [+-]
    ;

fragment
FloatTypeSuffix
    :   [fFdD]
    ;

fragment
HexadecimalFloatingPointLiteral
    :   HexSignificand BinaryExponent FloatTypeSuffix?
    ;

fragment
HexSignificand
    :   HexNumeral '.'?
    |   '0' [xX] HexDigits? '.' HexDigits
    ;

fragment
BinaryExponent
    :   BinaryExponentIndicator SignedInteger
    ;

fragment
BinaryExponentIndicator
    :   [pP]
    ;

// Char

Char
    :   '\'' SingleCharacter '\''
    |   '\'' EscapeSequence '\''
    ;

fragment
SingleCharacter
    :   ~['\\]
    ;

// String literals
String
    :   '"' StringCharacters? '"'
    ;
fragment
StringCharacters
    :   StringCharacter+
    ;
fragment
StringCharacter
    :   ~["\\]
    |   EscapeSequence
    ;
// §3.10.6 Escape Sequences for Character and String Literals
fragment
EscapeSequence
    :   '\\' [btnfr"'\\]
    |   OctalEscape
    |   UnicodeEscape
    ;

fragment
OctalEscape
    :   '\\' OctalDigit
    |   '\\' OctalDigit OctalDigit
    |   '\\' ZeroToThree OctalDigit OctalDigit
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
ZeroToThree
    :   [0-3]
    ;

Identifier
    :   [a-zA-Z_] [a-zA-Z_0-9]*
    |   '`' ~['`']+ '`'
    ;


//
// Whitespace and comments
//

WS  :  [ \t\r\n\u000C]+ -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> skip
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> skip
    ;


// to support case insensitive keywords

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];
