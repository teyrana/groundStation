package cubesat.groundStation;

// external libraries
import gnu.io.*;

// standard libraries
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

public class ByteNode extends BasicNode{

    protected int byteLimitDebug=-1;
    protected int byteLimitFrameMin=0;     // bytes to allow before losing sync, and resuming search for sync bytes
    protected int byteLimitFrameMax=0;    // max bytes to read into the frame
    protected int recognizedVersion=0;
    //protected int byteLimitTrack=5012;   // bytes to allow before losing tracking
    protected byte[] syncBytes= null;   //{(byte)0x47, (byte)0xFF};
    protected byte[] scanBuffer=new byte[1024]; // default size only
    protected boolean scanForSync=true;
    protected boolean readFrame=false;
    protected boolean formatConfirmed=false;
    //protected boolean synced=false;
    protected final int maxFormatSupported=0;
    protected int syncIndex=0; // index of sync array
    protected int byteCountSearch; // bytes since last header
    protected int byteOffsetScanned=0;  //0-indexed. counts the bytes that make it into frames

    protected float avgGapSize=0;
    protected int byteCountTotalGap=0;
    protected int gapCount=0;
    protected int lastFrameStart;
    protected int lastGapStart;

    public ByteNode(){
	setFormatFrame(new ByteFrame());
    }
    
    /* ===== ===== Instance Methods ===== ===== */

    public void init(){
     	nodeLog.warn( ">>init: Debug Byte Limit is: "+byteLimitDebug+" bytes");
    }

    
    // Frame Format:
    // [sync bytes(x2)][version(x1)][frame length(x2)][timestamp(x4)] data   [checksum]
    // [        0x47FF][          0][            0x32][    0x123456 ]
    // [0x55]* ["\n\n"]
    //
    protected void scan(byte[] scanBuffer, int length){
	int bufferIndex=0;
	for(bufferIndex=0; bufferIndex<length; bufferIndex++){
	    //if(10 > byteOffsetScanned){
	    //    nodeLog.trace(String.format("  .. buf[%2$d] =%1$02X @file[%3$d]",
	    //                  scanBuffer[bufferIndex]&0xFF,bufferIndex,byteOffsetScanned));
	    //}
	    
	    /*
	    if(tracking){
		if(byteCountSearch >= byteLimitTrack){ // indexing-vs-count off-by-1
		    tracking=false;
		    nodeLog.warn("serial port has lost tracking at:scan:"+byteOffsetScanned);
		    nodeLog.warn("       ("+byteLimitFrameMin+"/"+byteLimitFrameMax+"/"+byteLimitSync+")");
		}
	    }
	    */

	    if((!scanForSync)&&(byteCountSearch >= byteLimitFrameMin)){ // indexing-vs-count off-by-1
		scanForSync=true;
		syncIndex=0;
		nodeLog.trace(">>resumed sync scanning at: @ "+byteCountSearch+" /"+byteCountSearch+" /"+byteLimitFrameMax);
	    }

	    if(scanForSync){
		int syncLen=syncBytes.length;  // just an aka to save typing.
		if(syncIndex < syncBytes.length){
		    if(scanBuffer[bufferIndex]==syncBytes[syncIndex]){

			syncIndex++;
			byteCountSearch=syncIndex;
		    }else{
			// partial sync byte match.
			// Ignore and continue scanning.
			syncIndex=0;
		    }
		}else if(syncBytes.length==syncIndex){
		    tracking=true;
		    formatConfirmed=false;
		    scanForSync=false;
		    if(readFrame){
			nodeLog.trace("  >>Frame Header detected before maxlen # "+(frameCountSent+1)+" @ "+byteOffsetScanned);
			((ByteFrame)accumulatorFrame).rewind(syncLen);
			sendFrame();
		    }else{
			int byteCountThisGap=byteOffsetScanned-lastGapStart-syncLen;
			byteCountTotalGap-=syncLen;
			nodeLog.trace("  << gap ended: "+byteCountThisGap+" @"+(lastFrameStart-1));
		    }
		    readFrame=true;

		    byteCountSearch = syncLen;
		    int thisFrameStart=byteOffsetScanned-syncLen;
		    nodeLog.debug(String.format(">>Frame Detected: # %1$ 4d @ %2$ 6d", frameCountSent+1, thisFrameStart));
		    
		    int frameDistance=(byteOffsetScanned-lastFrameStart-syncLen);
		    lastFrameStart=thisFrameStart;

		    /*
		    //go back and and add this in after we
		    //can write raw bytes to the log file.
		    if(bufferIndex+3>length){
		    // to see if we can read the version
		    // #, and frame length
		    if(recognizedVersion==scanBuffer[bufferIndex+1]){
		    // this format is supported.
		    expectedFrameLength=(int)(scanBuffer[bufferIndex+2]&0xFF+scanBuffer[bufferIndex+3]&0xFF);
		    nodeLog.debug(" found framelength of: "+expectedFrameLength);
		    }
		    byteCountSearch+=3;
		    bufferIndex+=3;
		    byteCountSearch=syncIndex;
		    byteOffsetScanned+=3;
		    }else{
		    nodeLog.fatal("attempted to read non-existent frame header for version, frame length.");
		    nodeLog.fatal("       bufferIndex:"+bufferIndex+", scan length:"+length);
		    }
		    */
		}
	    }

	    if(readFrame){
		//nodeLog.trace(" .. reading frame data: "+String.format("buf[%2$d] =%1$02X @offset[%3$d]",
		//              scanBuffer[bufferIndex]&0xFF,bufferIndex,byteOffsetScanned));
		if(byteCountSearch < byteLimitFrameMax){
		    int writeLen=0;
		    byteCountSearch++;

		    if(!formatConfirmed){
			if(byteCountSearch > 6){
			    ((ByteFrame)accumulatorFrame).computeFrameLength();
			    // set frame length as min or max frame length?
			    if( maxFormatSupported < ((ByteFrame)accumulatorFrame).computeFormatVersion()){
				nodeLog.error("  >> detected a frame with newer protocal than code supports.  ");
				nodeLog.debug("     Telemetry Rev: "+((ByteFrame)accumulatorFrame).getFormatVersion()+
					      "     timestamp: "+((ByteFrame)accumulatorFrame).getTimeStamp());
			    }
			    formatConfirmed=true;
			}
		    }
		    
		    /*
		      // this would be faster, but at the moment it's an unneccessary optimization.
		    if(byteCountSearch < byteLimitSkip){
			// write chunks of array to frame
			int bytesLeftBuffer= length-bufferIndex+1;
			int bytesLeftSkip = byteLimitSkip-byteCountSearch;
			if(bytesLeftBuffer > bytesLeftSkip){
			    writeLen = bytesLeftSkip;
			}else{
			    writeLen = bytesLeftBuffer;
			}
			nodeLog.debug("   . writelen:"+writeLen+"   buf:"+bufferIndex+"   frame:"+byteCountSearch+"   scan:"+byteOffsetScanned);
			nodeLog.debug("     length:"+length+"    search:"+byteCountSearch);

			// SerialNodes can only send ByteFrames
			writeLen = ((ByteFrame)accumulatorFrame).addBytes(scanBuffer,bufferIndex,writeLen);
			writeLen--;
			// these are automatically incremented each loop
			byteOffsetScanned+=writeLen;
			bufferIndex+=writeLen;
			byteCountSearch+=writeLen;
		    }else{
			*/
		    // single write to frame
		    //nodeLog.trace("   >> write 1:   @"+byteCountSearch+"/"+byteLimitFrameMax+"     scan:"+byteOffsetScanned);
		    //nodeLog.trace("              buf:"+bufferIndex+"   scan:"+byteOffsetScanned);
		    ((ByteFrame)accumulatorFrame).addByte(scanBuffer[bufferIndex]);
			//}

		    if(byteCountSearch >= byteLimitFrameMax){  
			//nodeLog.trace(" > Frame Size exhausted #"+(frameCountSent+1)+" @"+byteOffsetScanned);
			nodeLog.trace(" > Frame Size exhausted");

			//read into outframe
			syncIndex=0;
			scanForSync=true;
			readFrame=false;
			lastGapStart=byteOffsetScanned+1;
			gapCount++;

			sendFrame();
		    }
		    if(byteCountSearch > byteLimitFrameMax){
			nodeLog.error("Over-wrote bytes into out frame: past byteLimitFrame.  Error in SerialNode.java:scan(...)");
		    }
		}
	    }else{ //if(!readFrame)
		byteCountTotalGap++;
	    }

	    byteOffsetScanned++;
      	} // bufferIndex loop
    }

    protected void sendFrame(){
	byteCountSentFrame+=((ByteFrame)accumulatorFrame).getFrameLength();
	super.sendFrame(accumulatorFrame);
	accumulatorFrame=formatFrame.copy();
    }

    public void setDebugByteLimit(int newLim){
	byteLimitDebug=newLim;
    }

    public void setFormatFrame(ByteFrame newFormat){
	formatFrame=newFormat;
	syncBytes=newFormat.getSyncBytes();
	byteLimitFrameMin=newFormat.getFrameLengthMinimum();
	byteLimitFrameMax=newFormat.getFrameLengthMaximum();
	if(0==syncBytes.length){
	    // if we're not syncing to specific bytes, -
	    // and merely passing everything through 
	    byteLimitFrameMin=byteLimitFrameMax;
	}
	accumulatorFrame = formatFrame.copy();
	nodeLog.debug("    ::loaded format:\""+formatFrame+"\"");
	nodeLog.debug("            Supported Format Version:"+maxFormatSupported);
    }

}
