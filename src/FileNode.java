package cubesat.groundStation;

import gnu.io.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileNode extends ByteNode{
    protected static Logger nodeLog = Logger.getLogger(FileNode.class);

    protected String writeFileName="";
    protected FileOutputStream writeFile=null;
    protected String readFileName="";
    protected FileInputStream readFile=null;

    public FileNode(){
	super();
    }

    public void open(String _name, char mode){
	if(('r'==mode)||('b'==mode)){
	    openReadFile(_name);
	}else if(('w'==mode)||('b'==mode)){
	    openWriteFile(_name);
	}
    }

    public void openReadFile(String _name){
	try{
	    readFileName=_name;
	    readFile = new FileInputStream(readFileName);
	    readable=true;
	    writeable=false;
	    connected=true;
	}catch(FileNotFoundException fnfe){
	    nodeLog.error("Could not create read file "+ readFileName+" in "+nodeName);
	}
    }

    public void openWriteFile(String _name){
	try{
	    writeFileName=_name;
	    writeFile = new FileOutputStream(writeFileName,false);
	    readable=false;
	    writeable=true;
	    connected=true;
	}catch(FileNotFoundException fnfe){
	    nodeLog.error("Could not create write file "+writeFileName+" in "+nodeName);
	}
    }

    public void close(){
	try{
	    if(true == readable){
		readFile.close();
	    }else{
		writeFile.close();
	    }
	    nodeLog.info("  ");
	    nodeLog.info("  FileNode Finished:");
	    nodeLog.info("===== ===== ===== = ===== ===== =====");
	    if(true == readable){
		avgFrameSizeSent=((float)byteCountSentFrame)/((float)frameCountSent);
		nodeLog.info("     read "+byteCountSentFrame+" bytes into "+frameCountSent+" frames. avg:" +avgFrameSizeSent);
	    }else if(true == writeable){
		avgFrameSizeSent=((float)byteCountRcvdFrame)/((float)frameCountRcvd);
		nodeLog.info("     wrote "+byteCountRcvdFrame+" bytes from "+frameCountRcvd+" frames. avg:" +avgFrameSizeRcvd);
	    }
	    nodeLog.info(" ");
	}catch(IOException ioe){
	    nodeLog.fatal("IO Exception while closing down FileNode. Exiting.");
	    System.exit(-1);
	    
	}
    }

    public void update_read(){
	// NYI
    }
    
    public void update_write(){
	//System.out.println("DisplayNode::updating_write...");
	if(null!=inFrame){
	    inFrame.write((OutputStream)writeFile);
	    byteCountRcvdFrame+=inFrame.size();
	    frameCountRcvd++;
	}
    }

    
}
