JFLAGS = -g
JC = javac
CLASSPATH = -classpath src/
BIN = -d bin/
.SUFFIXES: .java .class
.java.class:
	$(JC) $(BIN) $(JFLAGS) $(CLASSPATH) $*.java

CLASSES = \
    src/chord/Chord.java \

 
default: classes \

classes: $(CLASSES:.java=.class)
 
clean:
	$(RM) *.class
