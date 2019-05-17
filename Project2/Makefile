JFLAGS = -g
JC = "C:\\Program Files\\Java\\jdk1.8.0_211\\bin\\javac.exe"
CLASSPATH = -classpath src/
BIN = -d bin/
.SUFFIXES: .java .class
.java.class:
	$(JC) $(BIN) $(JFLAGS) $(CLASSPATH) $*.java

CLASSES = \
    src/chord/Chord.java \
    src/chord/ChordChannel.java \
    src/chord/ChordMaintenance.java \
    src/chord/ChordTest.java \
    src/global/Pair.java

 
default: classes \

classes: $(CLASSES:.java=.class)
 
clean:
	$(RM) *.class
