OUT_DIR := out/production/Project1
TEST_FILE := files/test
TEST_APP_CLASS := test.TestApp
PEER_CLASS := peer.Peer

all: mkdir
	javac -d $(OUT_DIR) src/*/*/*.java src/*/*.java

mkdir:
	@mkdir -p $(OUT_DIR)

peer1:
	java -cp $(OUT_DIR) $(PEER_CLASS) 1.0 1 Peer1 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003

peer2:
	java -cp $(OUT_DIR) $(PEER_CLASS) 1.0 2 Peer2 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003

peer3:
	java -cp $(OUT_DIR) $(PEER_CLASS) 1.0 3 Peer3 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003

peer4:
	java -cp $(OUT_DIR) $(PEER_CLASS) 1.0 4 Peer4 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003

rmi:
	rmiregistry -J-Djava.class.path=$(OUT_DIR)

backup:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer1 BACKUP $(TEST_FILE) 2

restore:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer1 RESTORE $(TEST_FILE)

delete:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer1 DELETE $(TEST_FILE)

reclaim:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer1 RECLAIM 12345

state1:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer1 STATE

state2:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer2 STATE

state3:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer3 STATE

state4:
	java -cp $(OUT_DIR) $(TEST_APP_CLASS) Peer4 STATE

snooper:
	java -Djava.net.preferIPv4Stack=true -jar McastSnooper.jar 224.0.0.15:8001 224.0.0.16:8002 224.0.0.17:8003

clean:
	@rm -rf out/ database/