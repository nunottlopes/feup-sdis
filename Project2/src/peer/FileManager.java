package peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk FileManager class
 */
public class FileManager implements Serializable {

    /**
     * Peer max disk space reserved for backup service
     */
    public static final long MAX_CAPACITY = 8*100000000;

    /**
     * Peer available disk space
     */
    private long free_mem;

    /**
     * Peer used disk space
     */
    private long used_mem;

    /**
     * Chunks information stored from backup protocol
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk> > chunksStored;

    /**
     * Information from chunks that have been used to initiate backup protocol
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk> > backedupFiles;

    /**
     * FileManager constructor
     */
    public FileManager() {
        free_mem = MAX_CAPACITY;
        used_mem = 0;
        chunksStored = new ConcurrentHashMap<>();
        backedupFiles = new ConcurrentHashMap<>();
    }

    /**
     * Creates folders if they don't exist
     * @param path
     */
    public void createFolders(String path) {
        try {
            if(!Files.exists(Paths.get(path))){
                Files.createDirectories(Paths.get(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds chunk to chunksStored
     * @param chunk
     */
    public void addChunk(Chunk chunk) {
        ConcurrentHashMap<Integer, Chunk> chunks;
        chunks = chunksStored.getOrDefault(chunk.getFileId(), new ConcurrentHashMap<>());
        chunk.clearData();
        chunks.putIfAbsent(chunk.getChunkNo(), chunk);

        chunksStored.putIfAbsent(chunk.getFileId(), chunks);
    }

    /**
     * Checks if chunksStored has a chunk
     * @param fileId
     * @param chunkNo
     * @return true if chunk exists
     */
    public boolean hasChunk(String fileId, int chunkNo) {
        ConcurrentHashMap<Integer, Chunk> chunks = chunksStored.get(fileId);

        return chunks != null && chunks.containsKey(chunkNo);
    }

    /**
     * Removes a chunk from chunksStored
     * @param fileId
     * @param chunkNo
     */
    public void removeChunk(String fileId, int chunkNo) {
        chunksStored.get(fileId).remove(chunkNo);
        if(chunksStored.get(fileId).size() == 0)
            chunksStored.remove(fileId);
    }

    /**
     * Returns chunk from chunksStored
     * @param fileId
     * @param chunkNo
     * @return chunk
     */
    public Chunk getChunk(String fileId, int chunkNo){
        return chunksStored.get(fileId).get(chunkNo);
    }

    /**
     * Saves chunk to a file
     * @param fileName
     * @param path
     * @param data
     * @return true if chunk was saved successfully, false otherwise
     * @throws IOException
     */
    public boolean saveChunkFile(String fileName, String path, byte[] data) throws IOException {
        long file_size = data.length;

        if (free_mem < file_size) {
            System.out.println("No more available memory!");
            return false;
        }
        String filePath = path + "/" + fileName;

        createFolders(path);

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

    /**
     * Removes chunk file
     * @param path
     * @param fileName
     * @param updateFreeMem
     */
    public void removeChunkFile(String path, String fileName, boolean updateFreeMem){
        File chunkFile = new File(path, fileName);
        if(updateFreeMem)
            free_mem += chunkFile.length();
        used_mem -= chunkFile.length();
        chunkFile.delete();
    }

    /**
     * Removes File Backup Folder
     * @param path
     * @param updateFreeMem
     * @return true if success, false otherwise
     */
    public boolean removeFileFolder(String path, boolean updateFreeMem){
        File file = new File(path);

        if(!file.exists())
            return true;

        for(String fileName : file.list()){
            removeChunkFile(file.getPath(), fileName, updateFreeMem);
        }

        file.delete();
        if(file.exists())
            return false;
        return true;
    }

    /**
     * Removes folder if it's empty
     * @param path
     */
    public void removeFolderIfEmpty(String path){
        File file = new File(path);

        if (file.exists()){
            if(file.list().length == 0){
                file.delete();
            }
        }
    }

    /**
     * Adds chunk to backedupFiles
     * @param fileId
     * @param perceivedRepDegreeList
     * @param repDegree
     * @param path
     */
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

    /**
     * Removes file from chunksStored and backedupFiles
     * @param fileId
     */
    public void removeStoredChunks(String fileId) {
        chunksStored.remove(fileId);
        backedupFiles.remove(fileId);
    }

    /**
     * Checks if file exists in chunksStored
     * @param fileId
     * @return true if exist, false otherwise
     */
    public boolean hasStoredChunks(String fileId) {
        return chunksStored.containsKey(fileId);
    }

    /**
     * Returns all stored chunks from chunksStored
     * @return all stored chunks
     */
    public List<Chunk> getAllStoredChunks(){
        List<Chunk> chunks =  new ArrayList<>();;
        for(ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Chunk>> entry_file : chunksStored.entrySet()){
            for(ConcurrentHashMap.Entry<Integer, Chunk> entry_chunk : entry_file.getValue().entrySet()){
                chunks.add(entry_chunk.getValue());
            }
        }
        return chunks;
    }

    /**
     * Returns backedupFiles
     * @return backedupFiles
     */
    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> getBackedupFiles() {
        return backedupFiles;
    }

    /**
     * Returns chunksStored
     * @return chunksStored
     */
    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Chunk>> getChunksStored() {
        return chunksStored;
    }

    /**
     * Returns chunk used memory
     * @return used memory
     */
    public long getUsed_mem() {
        return used_mem;
    }

    /**
     * Returns chunk max memory
     * @return max memory
     */
    public long getMaxMemory(){
        return free_mem+used_mem;
    }

    /**
     * Updates free memory
     * @param spaceReclaim
     */
    public void updateFreeMem(long spaceReclaim) {
        this.free_mem = spaceReclaim - this.used_mem;
    }
}
