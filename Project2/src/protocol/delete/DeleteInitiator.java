package protocol.delete;


import globals.Globals;
import peer.Chunk;
import peer.Peer;
import protocol.InvalidProtocolExecution;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static message.SendMessage.sendDELETE;
import static message.SendMessage.sendGETCHUNK;

/**
 * DeleteInitiator class
 */
public class DeleteInitiator {

    /**
     * Max number of times a DELETE message should be sent
     */
    public static final int MAX_DELETE_MESSAGES = 1;

    /**
     * Time interval between each DELETE message
     */
    private static final int TIME_INTERVAL = 500;

    /**
     * File path to be deleted
     */
    private String path;

    /**
     * File id
     */
    private String fileId;

    /**
     * DeleteInitiator constructor
     * @param path
     */
    public DeleteInitiator(String path){
        this.path = path;
    }

    /**
     * Runs Delete protocol for initiator-peer
     * @throws InvalidProtocolExecution
     */
    public void run() throws InvalidProtocolExecution {
        File file = new File(path);
        if(!file.exists())
            throw new InvalidProtocolExecution(InvalidProtocolExecution.Protocol.DELETE, "File not found!");

        fileId = Globals.generateFileId(file);

        int number_of_chunks = (int) file.length() / (Chunk.MAX_SIZE) + 1;

        CountDownLatch latch = new CountDownLatch(MAX_DELETE_MESSAGES);

        for (int i = 0 ; i < MAX_DELETE_MESSAGES; i++) {
            Peer.getInstance().getExecutor().schedule(()->{
                for(int n = 0; n < number_of_chunks; n++){
                    String name = fileId + n;
                    int hash = Peer.getInstance().getChord().hash(name);
                    
                    String[] message = Peer.getInstance().getChord().sendLookup(hash, true);

                    if(message != null){
                        Chunk chunk = null;
						int id = Integer.parseInt(message[2]);
                        InetAddress address = null;

                        try {
							address = InetAddress.getByName(message[3]);
						} catch (UnknownHostException e1) {
						}
                     
						if (!message[3].equals(Peer.getInstance().getChord().getChordAddress()))
						{
							sendGETCHUNK(fileId, n, address);
							
							int delay = 10, maxTime = 2000;
							
							for (int j = 0; j < maxTime/delay; j++)
							{
								try {
							        TimeUnit.MILLISECONDS.sleep(delay);
							    } catch (InterruptedException e) {
							        e.printStackTrace();
							    }
							                                    
							    if (Peer.getInstance().getProtocolInfo().hasReceivedChunk(fileId, n))
							    {
							    	chunk = Peer.getInstance().getFileManager().getChunk(fileId, n);
							        break;
							    }
							}
						}
						else
						{
							chunk = Peer.getInstance().getFileManager().getChunk(fileId, n);
						}
									
						
						if (chunk == null) // Error
							System.err.println("Error getting chunk for delete");
						
						Peer.getInstance().getFileManager().removeChunk(fileId, n);
						
						int repDegree = chunk.getRepDegree();
						
						for (int j = 0; j < repDegree; j++)
						{
							
							if (!message[3].equals(Peer.getInstance().getChord().getChordAddress()))
							{
							    sendDELETE(fileId, address);
							}
							else
							{
								new Delete(fileId);
							}
							
						    
						    id = Math.floorMod(id+1, Peer.getInstance().getChord().getMaxPeers());
						    
						    message = Peer.getInstance().getChord().sendLookup(id+1, true);
							
							id = Integer.parseInt(message[2]);
							
							try {
								address = InetAddress.getByName(message[3]);
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
                }

                latch.countDown();
                }, TIME_INTERVAL*i, TimeUnit.MILLISECONDS);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
