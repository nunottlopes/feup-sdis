OUT_DIR := out/production/Project1
TEST_FILE := random.pdf
TEST_APP_CLASS := test.TestApp
PEER_CLASS := peer.Peer
PEER_ACCESS_POINT := Peer1

all: mkdir
	javac -d $(OUT_DIR) src/*/*.java

mkdir:
	@mkdir -p $(OUT_DIR)

peer:
	java -cp $(OUT_DIR) $(PEER_CLASS) 2.0 1 $(PEER_ACCESS_POINT) 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003

rmi:
	rmiregistry -J-Djava.class.path=$(OUT_DIR)

backup:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) $(PEER_ACCESS_POINT) BACKUP $(TEST_FILE) 3

restore:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) $(PEER_ACCESS_POINT) RESTORE $(TEST_FILE)

delete:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) $(PEER_ACCESS_POINT) DELETE $(TEST_FILE)

reclaim:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) $(PEER_ACCESS_POINT) RECLAIM 12345

state:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) $(PEER_ACCESS_POINT) STATE