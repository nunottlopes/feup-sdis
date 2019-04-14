### Run using scripts and makefile

On project folder:

Compile:
```
> make
javac -d out/production/Project1 src/*/*.java
```

Run RMI:
```
> make rmi
rmiregistry -J-Djava.class.path=out/production/Project1
```

Run Peer:
```
> sh peer.sh <version> <peer_num> <peer_access_point>
E.g: sh peer.sh 2.0 1 Peer1
java -cp out/production/Project1 peer.Peer 2.0 1 Peer1 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003
```

Run TestApp:

```
> sh backup.sh <peer_ap> <file_path> <replication_degree>
E.g: sh backup.sh Peer1 files/test.txt 3
java -cp out/production/Project1 test.TestApp Peer1 BACKUP files/test.txt 3
```

```
> sh backupenh.sh <peer_ap> <file_path> <replication_degree>
E.g: sh backupenh.sh Peer1 files/test.txt 3
java -cp out/production/Project1 test.TestApp Peer1 BACKUPENH files/test.txt 3
```

```
> sh restore.sh <peer_ap> <file_path>
E.g: sh restore.sh Peer1 files/test.txt
java -cp out/production/Project1 test.TestApp Peer1 RESTORE files/test.txt
```

```
> sh restoreenh.sh <peer_ap> <file_path>
E.g: sh restoreenh.sh Peer1 files/test.txt
java -cp out/production/Project1 test.TestApp Peer1 RESTOREENH files/test.txt
```

```
> sh delete.sh <peer_ap> <file_path>
E.g: sh delete.sh Peer1 files/test.txt
java -cp out/production/Project1 test.TestApp Peer1 DELETE files/test.txt
```

```
> sh reclaim.sh <peer_ap> <disk_space_to_reclaim>
E.g: sh reclaim.sh Peer1 1000
java -cp out/production/Project1 test.TestApp Peer1 RECLAIM 1000
```

```
> sh state.sh <peer_ap>
E.g: sh state.sh Peer1
java -cp out/production/Project1 test.TestApp Peer1 STATE
```

Run Snooper:
```
> make snooper
java -Djava.net.preferIPv4Stack=true -jar McastSnooper.jar 224.0.0.15:8001 224.0.0.16:8002 224.0.0.17:8003
```

Remove all files:
```
> make clean
@rm -rf out/ database/
```

<br>
<br>

### Run without scripts and makefile 

On project folder:

Compile:
```
> javac -d out/production/Project1 src/*/*.java
```

Run RMI:
```
> rmiregistry -J-Djava.class.path=out/production/Project1
```

Run Peer:
```
> java -cp out/production/Project1 peer.Peer <version> <server id> <access_point> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>
```

Run TestApp:

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> BACKUP <file_path> <desired_replication_degree>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> BACKUPENH <file_path> <desired_replication_degree>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> RESTORE <file_path>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> RESTOREENH <file_path>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> DELETE <file_path>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> RECLAIM <max_amount_disk_space>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> STATE
```

Run Snooper:
```
> java -Djava.net.preferIPv4Stack=true -jar McastSnooper.jar <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>
```

Remove all files:
```
> rm -rf out/ database/
```
