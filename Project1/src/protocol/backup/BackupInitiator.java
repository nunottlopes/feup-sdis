package protocol.backup;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;
import protocol.ProtocolInfo;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class BackupInitiator {

    public static final int MAX_NUM_CHUNKS = 1000000;
    public static final int MAX_RETRANSMISSIONS = 5;

    private String path;
    private int repDegree;
    private String fileId;
    private File file;

    public BackupInitiator(String path, int repDegree) {
        this.path = path;
        this.repDegree = repDegree;
    }

    public void run() throws InvalidProtocolExecution {

        byte[] data = getFileData();
        fileId = generateFileId(file);

        ArrayList<Chunk> chunks = splitIntoChunks(data);

        if(!validBackup(repDegree, chunks.size())) return;

        Peer.getInstance().getProtocolInfo().startBackup(fileId);

        ArrayList<Thread> threads = new ArrayList<>(chunks.size());
        for(Chunk c : chunks) {
            Thread t = new Thread(() -> {
                int delay = 1;

                String[] args = {
                        Peer.getInstance().getVersion(),
                        Integer.toString(Peer.getInstance().getId()),
                        fileId,
                        Integer.toString(c.getChunkNo()),
                        Integer.toString(c.getRepDegree())
                };
                Message msg = new Message(Message.MessageType.PUTCHUNK, args, c.getData());


                ProtocolInfo status = Peer.getInstance().getProtocolInfo();

                for(int i = 0; i < MAX_RETRANSMISSIONS; i++) {
                    Peer.getInstance().send(Channel.Type.MDB, msg, false);

                    try {
                        TimeUnit.SECONDS.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    delay *= 2;
                    int currentRepDegree = status.getChunkRepDegree(c.getFileId(), c.getChunkNo());
                    if(currentRepDegree >= c.getRepDegree()) break;
//                    System.out.println("Desired Replication Degree not achieved (" + currentRepDegree + "/" + c.getRepDegree() + ") . Resending PUTCHUNK");
                }
            });
            threads.add(t);
            t.start();
        }

        for(Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Peer.getInstance().getProtocolInfo().endBackup(fileId);

    }

    private byte[] getFileData() throws InvalidProtocolExecution {
        file = new File(path);


        FileInputStream in_stream;
        try {
            in_stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "File not found!");
        }

        byte[] data = new byte[(int) file.length()];

        try {
            in_stream.read(data);
            in_stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;

    }

    private boolean validBackup(int repDegree, int n_chunks) throws InvalidProtocolExecution {
        if(repDegree > 9) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP, "Max number of replication degree is 9");
        }
        if(n_chunks > MAX_NUM_CHUNKS) {
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.BACKUP,"Backup Error: Max file size is 64GBytes");
        }
        return true;
    }

    private ArrayList<Chunk> splitIntoChunks(byte[] data) {
        ArrayList<Chunk> ret = new ArrayList<>();

        int n = data.length / (Chunk.MAX_SIZE) + 1;

        for(int i = 0; i < n; i++) {

            byte[] chunk_data;

            if(i == n-1) {
                if(data.length % Chunk.MAX_SIZE ==0) {
                    chunk_data= new byte[0];
                } else {
                    chunk_data= Arrays.copyOfRange(data, i*Chunk.MAX_SIZE, i*Chunk.MAX_SIZE + (data.length % Chunk.MAX_SIZE));
                }
            } else {
                chunk_data= Arrays.copyOfRange(data, i*Chunk.MAX_SIZE, i*Chunk.MAX_SIZE + Chunk.MAX_SIZE);
            }

            Chunk chunk=new Chunk(fileId, i, repDegree, chunk_data);
            ret.add(chunk);
        }

        return ret;
    }

    private String generateFileId(File f) {
        String file_id = f.getName() + f.lastModified() + Peer.getInstance().getId();
        return sha256(file_id);
    }

    public String sha256(String s) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return new HexBinaryAdapter().marshal(hash);
    }
}
