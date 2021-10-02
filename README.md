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
