package peer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    public static final long MAX_CAPACITY = 8*1000000;
    private long free_mem;
    private long used_mem;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk> > chunksStored;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk> > backedupFiles;

    public FileManager() {
        free_mem = MAX_CAPACITY;
        used_mem = 0;
        chunksStored = new ConcurrentHashMap<>();
        backedupFiles = new ConcurrentHashMap<>();
    }

    public void createFolder(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addChunk(Chunk chunk) {
        ConcurrentHashMap<Integer, Chunk> chunks;
        chunks = chunksStored.getOrDefault(chunk.getFileId(), new ConcurrentHashMap<>());
        chunks.putIfAbsent(chunk.getChunkNo(), chunk);

        chunksStored.putIfAbsent(chunk.getFileId(), chunks);
    }

    public boolean hasChunk(String fileId, int chunkNo) {
        ConcurrentHashMap<Integer, Chunk> chunks = chunksStored.get(fileId);

        return chunks != null && chunks.containsKey(chunkNo);
    }

    public Chunk getChunk(String fileId, int chunkNo) {
        return chunksStored.get(fileId).get(chunkNo);
    }

    public boolean saveFile(String fileName, String path, byte[] data) throws IOException {
        long file_size = data.length;

        if (free_mem < file_size) {
            return false;
        }
        String filePath = path + "/" + fileName;

        if (Files.exists(Paths.get(filePath))) {
            return true;
        }

        OutputStream out = Files.newOutputStream(Paths.get(filePath));
        out.write(data);
        out.close();

        free_mem -= file_size;
        used_mem += file_size;

        return true;
    }

    public void updateChunkPerceivedRepDegree(String fileId, int chunkNo, int peerId){
        Chunk chunk = chunksStored.get(fileId).get(chunkNo);
        if (chunk != null)
            chunk.addPerceivedRepDegreePeerId(peerId);
    }

    public void addBackedupChunk(String fileId, ConcurrentHashMap<Integer,Set<Integer>> perceivedRepDegreeList, int repDegree, String path) {
        int chunkNo;
        Chunk chunk;
        ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, Set<Integer>> item : perceivedRepDegreeList.entrySet()){
            chunkNo = item.getKey();
            chunk = new Chunk(fileId, chunkNo, repDegree, item.getValue());
            chunks.putIfAbsent(chunkNo, chunk);
        }
        backedupFiles.putIfAbsent(path, chunks);
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> getBackedupFiles() {
        return backedupFiles;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> getChunksStored() {
        return chunksStored;
    }

    public long getUsed_mem() {
        return used_mem;
    }
}
