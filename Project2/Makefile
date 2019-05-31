JC = javac
OUT_DIR := out/production/Project2

all: mkdir
	$(JC) -d $(OUT_DIR) src/*/*/*.java src/*/*.java

mkdir:
	@mkdir -p $(OUT_DIR)

rmi:
	rmiregistry -J-Djava.class.path=$(OUT_DIR)
 
clean:
	@rm -rf OUT_DIR database/
