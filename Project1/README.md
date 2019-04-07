### Run using predefined Peer and TestApp

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
> make peer
java -cp out/production/Project1 peer.Peer 2.0 1 Peer1 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003
```

Run TestApp:

```
> make backup
java -cp out/production/Project1 test.TestApp Peer1 BACKUP random.pdf 3
```

```
> make restore
java -cp out/production/Project1 test.TestApp Peer1 RESTORE random.pdf
```

```
> make delete
java -cp out/production/Project1 test.TestApp Peer1 DELETE random.pdf
```

```
> make reclaim
java -cp out/production/Project1 test.TestApp Peer1 RECLAIM 12345
```

```
> make state
java -cp out/production/Project1 test.TestApp Peer1 STATE
```


### Run

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
> java -cp out/production/Project1 peer.Peer <version> <server id> <access_point> <MC_IP_address> <MC_port> <MDB_IP_address> <MDB_port> <MDR_IP_address> <MDR_port>
```

Run TestApp:

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> BACKUP <file_path> <desired_replication_degree>
```

```
> java -cp out/production/Project1 test.TestApp <peer_access_point> RESTORE <file_path>
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