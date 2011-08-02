package cubesat.groundStation;

// standard
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.CRC32;

// external
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

class ByteFrame {
    protected static Logger framelog = Logger.getLogger(ByteFrame.class);
    protected static int frameCount=0;

    // frame-generic variables
    protected String frameTypeName = null;
    protected int frameID=0;

    // byte-specific variables
    protected final int emptyFrameLength=500;
    protected final float growthFactor=(float)1.1;
    protected byte[] syncBytes=new byte[0];
    protected byte[] data=new byte[0];
    protected int writeIndex=0;
    protected int frameLengthMin=0;
    protected int frameLengthMax=0;
    protected int frameLengthComputed=-1;

    protected int formatVersion=-1;
    protected int timeStamp=-1;
        

    public ByteFrame(){
	frameID = frameCount;
	frameCount++;
	setFrameTypeName();

	// need to set a format.
	setFormatEmpty();
	setFormatFS7Telemetry();

	clear();
    }

    public ByteFrame(ByteFrame other){
	this();
	
	syncBytes = other.getSyncBytes();
	setFrameLimits( other.getFrameLengthMinimum(), other.getFrameLengthMaximum());
	
	clear();
    }

    public int addByte(byte toWrite){
	if(writeIndex>=frameLengthMax){
	    return 0;
	}
	//framelog.trace("   writing frame @"+writeIndex+" of "+frameLengthMax+") ");
	data[writeIndex]=toWrite;
	writeIndex++;
	return 1;
    }
	    
    public int addBytes(byte[] source, int sourceStart, int sourceLength){
	framelog.debug("   frame-write:("+writeIndex+"/"+data.length+") to ");
	framelog.debug("   buf-read:("+sourceStart+"/"+sourceLength+"/"+source.length+")");

	// should be able to get rid of this....
	if((sourceStart<0)||(sourceLength<1)){
	    return 0;
	}
	if(writeIndex >= frameLengthMax){
	    return 0;
	}
	System.arraycopy(source, sourceStart, data, writeIndex, sourceLength);
	writeIndex+=sourceLength;
	return sourceLength;
    }
        
    public void clear(){
	writeIndex=0;
	formatVersion=-1;
	timeStamp=-1;
	Arrays.fill(data, (byte)0);
	System.arraycopy(syncBytes,0,data,0,syncBytes.length);
	writeIndex=syncBytes.length;
    }

    public int computeFrameLength(){
	int newLen = (0xFF&data[2])*0x100+(0xFF&data[3]);
	frameLengthComputed=newLen;
	return newLen;
    }
    
    public int computeFormatVersion(){
	formatVersion = 0xFF & data[4];
	return formatVersion;
    }
    
    public int computeFields(){
	int fieldCount=0;
	fieldCount++;
	timeStamp = (0xFF&data[5])*0x1000000+(0xFF&data[6])*0x10000 +(0xFF&data[7])*256 +(0xFF&data[8]);

	return fieldCount;
    }
    
    public ByteFrame copy(){	return new ByteFrame(this);    }

    public void dump(){ dump(false, 20,5);} // default options
    
    public void dump(boolean showASCII, int lineRadix,int spaceRadix){
	System.out.println("  >> "+toString()+getStatus());
	System.out.println("=============================");
	System.out.print(getDataString(lineRadix, spaceRadix));
	System.out.println("\n=============================");
	if(showASCII){
	    for (int i=0;i<writeIndex;i++){
		if(0==(i%lineRadix)){
		    System.out.println("");}
		System.out.print(String.format(" %1$2c",(int)data[i]&0xFF));
	    }
	    System.out.print("\n=============================");
	}
	System.out.print("\n");
    }

    public int getByteCount(){ return writeIndex;}

    public String getDataString(int lineRadix,int spaceRadix){
	StringBuilder dataString = new StringBuilder(3*writeIndex+10);
	for (int i=0;i<writeIndex;i++){
	    // ghetto formatting... there's probably a faster way to do this...

	    if(0==(i%lineRadix)){
		dataString.append("\n");}
	    if(0==(i%spaceRadix)){
		dataString.append(" ");}
	    dataString.append(String.format(" %1$02X",(int)data[i]&0xFF));
	}
	//dataString.append("\n=============================");
	return dataString.substring(1); // from 1 to clip the leading '\n'
    }


    public int getFormatVersion(){ return formatVersion;}
    public int getFrameLength(){ return writeIndex;}
    public int getFrameLengthMaximum(){	return frameLengthMax; }
    public int getFrameLengthMinimum(){	return frameLengthMin; }
    public int getID(){ return frameID;}
    public String getStatus(){ return new String(" ("+writeIndex+"/"+frameLengthMax+")"); }
    public byte[] getSyncBytes(){ return syncBytes;}
    public int getTimeStamp(){	return timeStamp; }
    protected void growBuffer(){growBuffer(data.length);}
    protected void growBuffer(int newSize){
	setBufferSize((int)(newSize*growthFactor));
    }

    public void rewind(int byteCount){
	if(byteCount > 0){
	    writeIndex-=byteCount;
	}
    }

    protected void setFrameLimits(int newMin,int newMax){
	frameLengthMin=newMin;
	frameLengthMax=newMax;
	if(frameLengthMin>frameLengthMax){
	    frameLengthMax=frameLengthMin;
	}
	if(frameLengthMax>data.length){
	    setBufferSize(frameLengthMax);
	}
    }

    protected final void setFrameTypeName(){
	// uses reflection to find the name of the current class
	// that's calling this function.
	frameTypeName=getClass().getSimpleName();
    }

    protected void setBufferSize(int newSize){
	frameLengthMax=newSize;
	byte[] newArray=new byte[frameLengthMax];
	int copyLen =Math.min( newArray.length, data.length);
	System.arraycopy(data,0,newArray,0,copyLen);
	data=newArray;
    }

    public void setFormatDebug(){
    	byte[] fs7SyncBytes={(byte)0x47,(byte)0xFF};
	syncBytes = fs7SyncBytes;
	int fs7BufferSize=50;
	setFrameLimits(38,42);
	clear();
    }

    public final void setFormatEmpty(){
	syncBytes=new byte[0];
	setFrameLimits(emptyFrameLength,emptyFrameLength);
	clear();
    }
    
    public final void setFormatFS7Telemetry(){
	byte[] fs7SyncBytes={(byte)0x47,(byte)0xFF};
	syncBytes = fs7SyncBytes;
	setFrameLimits(40,120);
	clear();
    }

    public int size(){return getFrameLength();}

    public String toString(){
	String result=null;
	result=String.format("%s_id_%2$03d", frameTypeName, frameID);
	result.concat(" ("+frameLengthMin+"/"+writeIndex+"/"+frameLengthMax+")");
	return result;
    }

    public boolean write(OutputStream dest){
	try{
	    dest.write(data, 0, writeIndex);
	    return true;
	}catch(IOException ioe){
	    framelog.error("IOException while attempting to write packet. returning failure.");
	    return false;
	}

	
	
    }
    
}
