package peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static globals.Globals.getFileData;

public class FileManager implements Serializable {
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

    public void createFolders(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addChunk(Chunk chunk) {
        ConcurrentHashMap<Integer, Chunk> chunks;
        chunks = chunksStored.getOrDefault(chunk.getFileId(), new ConcurrentHashMap<>());
        chunk.clearData();
        chunks.putIfAbsent(chunk.getChunkNo(), chunk);

        chunksStored.putIfAbsent(chunk.getFileId(), chunks);
    }

    public boolean hasChunk(String fileId, int chunkNo) {
        ConcurrentHashMap<Integer, Chunk> chunks = chunksStored.get(fileId);

        return chunks != null && chunks.containsKey(chunkNo);
    }

    public void removeChunk(String fileId, int chunkNo) {
        chunksStored.get(fileId).remove(chunkNo);
    }

    public Chunk getChunkFromFile(String fileId, int chunkNo) {
        Chunk chunk = chunksStored.get(fileId).get(chunkNo);
        File file = new File(Peer.getInstance().getBackupPath(fileId) + chunkNo);

        byte[] data;
        try {
            data = getFileData(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        chunk.setData(data);

        return chunk;
    }

    public Chunk getChunk(String fileId, int chunkNo){
        return chunksStored.get(fileId).get(chunkNo);
    }

    public boolean saveChunkFile(String fileName, String path, byte[] data) throws IOException {
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

    public void removeChunkFile(String path, String fileName){
        File chunkFile = new File(path, fileName);
        free_mem += chunkFile.length();
        used_mem -= chunkFile.length();
        chunkFile.delete();
    }

    public boolean removeFileFolder(String path){
        File file = new File(path);

        if(!file.exists())
            return true;

        for(String fileName : file.list()){
            removeChunkFile(file.getPath(), fileName);
        }

        file.delete();
        return true;
    }

    public void removeFolderIfEmpty(String path){
        File file = new File(path);

        if (file.exists()){
            if(file.list().length == 0){
                file.delete();
            }
        }
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

    public void removeBackedupChunks(String fileId) {
        for(ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Chunk>> entry : backedupFiles.entrySet()){
            if(entry.getValue().entrySet().iterator().next().getValue().getFileId().equals(fileId)){
                backedupFiles.remove(entry.getKey());
                break;
            }
        }
    }

    public void removeStoredChunks(String fileId) {
        chunksStored.remove(fileId);
        backedupFiles.remove(fileId);
    }

    public void updateStoredChunks(String fileId, ConcurrentHashMap<Integer,Set<Integer>> chunkPerceivedRepDegree) {
        ConcurrentHashMap<Integer, Chunk> chunks = chunksStored.get(fileId);
        for(ConcurrentHashMap.Entry<Integer, Chunk> entry : chunks.entrySet()){
            Set<Integer> item = chunkPerceivedRepDegree.get(entry.getKey());
            if(item != null)
                entry.getValue().setPerceivedRepDegree(item);
        }
    }

    public boolean hasStoredChunks(String fileId) {
        return chunksStored.containsKey(fileId);
    }

    public List<Chunk> getAllStoredChunks(){
        List<Chunk> chunks =  new ArrayList<>();;
        for(ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Chunk>> entry_file : chunksStored.entrySet()){
            for(ConcurrentHashMap.Entry<Integer, Chunk> entry_chunk : entry_file.getValue().entrySet()){
                chunks.add(entry_chunk.getValue());
            }
        }
        return chunks;
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

    public long getMaxMemory(){
        return free_mem+used_mem;
    }

    public void updateFreeMem(long spaceReclaim) {
        this.free_mem = spaceReclaim - this.used_mem;
    }

}
