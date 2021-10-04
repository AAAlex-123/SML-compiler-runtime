# SML-compiler-runtime
A compiler for a very simple, BASIC-like language along with a Runtime Environment for executing the machine code produced by the compiler.

## Runtime Environment (SML_Executor)

The Executor uses an accumulator, meaning all of the opeartions happen in a single place. 
Compared to an implementation using multiple registers or operating directly on memory, a 
single accumulator greatly simplifies the design of the compiler while somewhat reducing 
the complexity of the Runtime as well as the number of different instructions. The machine 
code is loaded into the Executor's memory and then each instruction is executed one-by-one 
until the HALT instruction is read.

## Compiler (SML_Compiler)

The Compiler parses a high-level-language file (or stdin) to produce machine code 
interpretable from the Executor. The language is deriberately BASIC-like to simplify the 
compilation process, since each high-level-language statement is contained within a single 
line and can be parsed independently from the rest of the code. Notable exceptions are the 
`if` and `while` statements which inherently require a stack-like structure to function 
since they can be nested. They are rather poorly implemented on top of the rest of the 
Compiler because they were the last feature to be added to a compiler that didn't use 
stacks or treat code as separate blocks from the beginning.

## Commandline Tool (SML_Simulator)

The Simulator provides a commandline interface that simplifies the process of compiling 
high-level-language code and executing machine code. The user may issue many successive 
compilation and execution commands without the need for running a Java class each time.

## Machine Code

The machine code consists of 16-bit instructions whose high bits are the instruction's 
Operation Code (its identifier) and the low bits its Operand (usually the address in 
memory). The instructions are read from a newline-separated file and are executed 
sequentially one-by-one. They are adequately documented in the `Instruction` enum.

## High Level Language

The high-level-language consists of statements which span exactly line.

Notes on the statements and the lines of a high-level-language program:
* Each line begins with a unique number which will be used to identify it to provide 
better compilation error messages.
* After the number, any number of spaces follow and then a statement.
* Each statement spans exactly one line.
* Statements start with the statement identifier and then follow the whitespace-separated 
tokens for that statement
* The statements can either be "constructors", meaning that they define new symbols 
(variables or labels), or "non constructors", meaning that they operate on existing 
symbols. 

Referencing the following example statement,

_IntDeclarationStatement_:<br/>
&nbsp;&nbsp;`int` { _Variable_}

Note that:
* Terminal symbols are shown in fixed width font. These are to appear in a program exactly 
as written. (e.g. `int`)
* Nonterminal symbols are shown in italic type. The definition of a nonterminal is 
introduced by the name of the nonterminal being defined, followed by a colon. One or more 
alternative definitions for the nonterminal then follow on succeeding lines. (e.g.
_IntDeclarationStatement_, _Variable_)
* The syntax {x} on the right-hand side of a production denotes zero or more occurrences 
of x separated by whatever comes before it. (e.g. { _Variable_})
* Few nonterminals are defined by a narrative phrase in quotes where it would be 
impractical to list all the alternatives.
* The phrase _(one of)_ on the right-hand side of a production signifies that each of the 
symbols on the following line is an alternative definition.

The grammar of the language is as follows:

_Statement_: <br/>
&nbsp;&nbsp;_ConstructorStatement_ <br/>
&nbsp;&nbsp;_NonConstructorStatement_

_ConstructorStatement_: <br/>
&nbsp;&nbsp;_IntDeclarationStatement_ <br/>
&nbsp;&nbsp;_LabelDeclarationStatement_

_NonConstructorStatement_: <br/>
&nbsp;&nbsp;_CommentStatement_ <br/>
&nbsp;&nbsp;_InputStatement_ <br/>
&nbsp;&nbsp;_LetStatement_ <br/>
&nbsp;&nbsp;_PrintStatement_ <br/>
&nbsp;&nbsp;_GotoStatement_ <br/>
&nbsp;&nbsp;_IFGotoStatement_ <br/>
&nbsp;&nbsp;_IfStatement_ <br/>
&nbsp;&nbsp;_ElseStatement_ <br/>
&nbsp;&nbsp;_EndifStatement_ <br/>
&nbsp;&nbsp;_WhileStatement_ <br/>
&nbsp;&nbsp;_EndwhileStatement_ <br/>
&nbsp;&nbsp;_EndStatement_ <br/>
&nbsp;&nbsp;_NoopStatement_ <br/>
&nbsp;&nbsp;_DumpStatement_ <br/>

_IntDeclarationStatement_:<br/>
&nbsp;&nbsp;`int` { _Variable_}

_LelDeclarationStatement_:<br/>
&nbsp;&nbsp;`label` _Label_<br/>

_CommentStatement_:<br/>
&nbsp;&nbsp;`//` "anything apart from newline character"<br/>

_InputStatement_:<br/>
&nbsp;&nbsp;`input`&nbsp;&nbsp;{ _Variable_}<br/>

_LetStatement_:<br/>
&nbsp;&nbsp;`let` _Variable_ `=` _InfixExpression_<br/>

_PrintStatement_:<br/>
&nbsp;&nbsp;`print` { _Variable_}<br/>

_GotoStatement_:<br/>
&nbsp;&nbsp;`goto` _Label_<br/>

_IFGotoStatement_:<br/>
&nbsp;&nbsp;`ifg` _Condition_ `jumpto` _Label_<br/>

_IfStatement_:<br/>
&nbsp;&nbsp;`if` _Condition_<br/>

_ElseStatement_:<br/>
&nbsp;&nbsp;`else`<br/>

_EndifStatement_:<br/>
&nbsp;&nbsp;`endif`<br/>

_WhileStatement_:<br/>
&nbsp;&nbsp;`while` _Condition_<br/>

_EndwhileStatement_:<br/>
&nbsp;&nbsp;`endwhile`<br/>

_EndStatement_:<br/>
&nbsp;&nbsp;`end`<br/>

_NoopStatement_:<br/>
&nbsp;&nbsp;`noop`<br/>

_DumpStatement_:<br/>
&nbsp;&nbsp;`dump`<br/>

_Variable_:<br/>
&nbsp;&nbsp;"matches the regex '^[a-zA-Z]\\w*$'" and is not a reserved word<br/>

_Label_:<br/>
&nbsp;&nbsp;"matches the regex '^:\\w*$' and is not a reserved word<br/>

_InfixExpression_:<br/>
&nbsp;&nbsp;"a regular infix expression with symbols for _Variable_ and _MathematicalOperator_<br/>

_Condition_:<br/>
&nbsp;&nbsp;_Variable_ _ComparisonSymbol_ _Variable_<br/>

_MathematicalOperator_:<br/>
&nbsp;&nbsp;_(one of)_<br/>
&nbsp;&nbsp;`<+` `-` `*` `/` `^` `%` `(` `)`<br/>

_ComparisonSymbol_:<br/>
&nbsp;&nbsp;_(one of)_<br/>
&nbsp;&nbsp;`<` `>` `<=` `>=` `==` `!=`<br/>
