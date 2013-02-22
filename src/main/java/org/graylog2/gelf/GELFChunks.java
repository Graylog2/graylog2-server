/*
 * @(#) GELFChunks.java
 * Created 20.02.2013 by oleg
 * (C) ONE, SIA
 */
package org.graylog2.gelf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This stores chunks received and used to resemble the full messages from its chunks
 * 
 * @author Oleg Anastasyev<oa@hq.one.lv>
 *
 */
public class GELFChunks
{
    private static final Logger LOG = LoggerFactory.getLogger(GELFChunks.class);

    private final int count;
    private final BitSet idsArrived;
    
    private byte[] body;
    private int chunkSize;
    
    private int bodySize;
    
    private  final int arrival;
    
    private byte[] lastChunkData;
    
    /**
     * @param the first chunk received
     */
    public GELFChunks(GELFMessageChunk chunk)
    {
        this.count = chunk.getSequenceCount();
        this.arrival = chunk.getArrival();
        this.idsArrived = new BitSet(count);
        
        this.idsArrived.set(chunk.getSequenceNumber());
        
        if (!chunk.isLastChunk()) {
            chunkSize = chunk.getBodyLength();
            body=new byte[chunkSize*count];
            chunk.writeBody(body, chunkSize*chunk.getSequenceNumber());
            bodySize+=chunkSize;
        } else {
            // this is the last chunk. we cannot use it as source of body size, so we just save
            // it to separate array for later append
            lastChunkData=new byte[chunk.getBodyLength()];
            chunk.writeBody(lastChunkData, 0);
        }

    }


    /**
     * @param chunk
     * 
     * @return true if chunk is complete
     */
    public synchronized boolean add(GELFMessageChunk chunk)
    {
        int num = chunk.getSequenceNumber();
        
        int bodyLength = chunk.getBodyLength();
        if ( bodyLength!=chunkSize && !chunk.isLastChunk() && body!=null ) {
            // invalid chunk
            LOG.warn("Invalid chunk size. Chunk ignored. Expected {}, but got {}",chunkSize,bodyLength);
            return false;
        }
        
        if (idsArrived.get(num))
            return false; // this is a dup
        
        idsArrived.set(num);
        
        if (body==null) {
            // constructor was called with last chunk, so init of body was delayed.
            // initializing it now
            chunkSize = bodyLength;
            body=new byte[chunkSize*count];
            // appending last chunk data
            System.arraycopy(lastChunkData, 0, body, chunkSize*(count-1), lastChunkData.length);
            bodySize+=lastChunkData.length;
        } 

        // appending chunk data
        chunk.writeBody(body, chunkSize*num);
        bodySize+=bodyLength;
        
        return idsArrived.nextClearBit(0) >= count;
    }

    /**
     * @return the arrival seconds since 1.1.1970 of 1st chunk arrival
     */
    public int getArrival()
    {
        return arrival;
    }

    /**
     * @return message assembled from chunks
     */
    public GELFMessage assembleGELFMessage() {
        return new GELFMessage(body, 0, bodySize);
    }

    /**
     * @return
     */
    public boolean isComplete()
    {
        return idsArrived.nextClearBit(0)>=count;
    }
}
