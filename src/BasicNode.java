package cubesat.groundStation;

import gnu.io.*;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BasicNode extends Thread implements ActionListener {
    protected static Logger nodeLog = Logger.getLogger(BasicNode.class);
    protected static int nodeCount=0;
    protected static Vector<BasicNode> masterList=new Vector<BasicNode>();

    protected String nodeName="";
    protected String nodeTypeName="";
    protected int nodeID=-1;
    protected boolean running=true;
    protected boolean connected=false;
    protected boolean tracking=false;

    // status / display listeners
    protected boolean guiExists=false;
    protected Vector<ActionListener> updateListeners = new Vector<ActionListener>();

    // receive into node:
    protected ConcurrentLinkedQueue<ByteFrame> receiveQueue=new ConcurrentLinkedQueue<ByteFrame>();
    protected ByteFrame inFrame=null;
    protected boolean writeable=false;
    protected int frameCountRcvd=0;
    protected int byteCountRcvdFrame=0;
    protected float avgFrameSizeRcvd=0;

    // send from node:
    protected Vector<BasicNode> outNodes=new Vector<BasicNode>();
    protected ByteFrame formatFrame=new ByteFrame();
    protected FrameFormat format=null;
    protected ByteFrame accumulatorFrame=null;
    protected boolean readable=false;
    protected int frameCountSent=0;
    protected int byteCountSentFrame=0;
    protected float avgFrameSizeSent=0;

    /* ===== ===== Instance Methods ===== ===== */

    public void actionPerformed(ActionEvent ae){
	if(ae.getActionCommand().equalsIgnoreCase("connect")){
	    //nodeLog.debug("  >> request connect");
	    connect();
	    fireAction("update.label.?");
	}else if(ae.getActionCommand().equalsIgnoreCase("disconnect")){
	    //nodeLog.debug("  >> request disconnect");
	    disconnect();
	    fireAction("update.label.?");
	}else{
	    nodeLog.warn("!! unrecognized action requested='"+ae.getActionCommand()+"'");
	}
    }

    public void addActionListener(ActionListener al){
	if (null==al){
	    nodeLog.fatal("attempt to set listener to null");
	}else{
	    guiExists=true;
	    updateListeners.add(al);
	}
    }
    
    public void addOutputNode(BasicNode _output){
	if(null==_output){
	    outNodes.clear();
	}else{
	    if(-1==outNodes.indexOf(_output)){
		outNodes.add(_output);
	    }
	}
    }

    public void close(){}
    public boolean connect(){ return connected=true;}
    
    public boolean enqueueFrame(ByteFrame newFrame){
	return receiveQueue.offer(newFrame);
    }

    public void fireAction(String command){
	ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, nodeTypeName+"."+command);
	//nodeLog.warn("    >> size:"ud);
	if(0<updateListeners.size()){
	    for( ActionListener al : updateListeners ){
		al.actionPerformed(ae);
	    }
	}
    }

    public ByteFrame getActiveFrame(){ return inFrame; }
    public int getNodeID(){ return nodeID;}
    public String getNodeName(){ return nodeName;}
    public String getNodeTypeName(){ return nodeTypeName;}
    public boolean getReadable(){ return readable; }
    public String getStatus(){
	return new String("rd:"+readable+" wrt:"+writeable+" ctd:"+connected+" rng:"+running);}
    
    public String getTraffic(){
    	return new String("  >>"+frameCountRcvd+"  <<"+frameCountSent);}
    
    public boolean getWriteable(){ return writeable; }

    public static void stopAll(){
	nodeLog.warn("::Halting System.");
	for (BasicNode n : masterList){
	    n.setRunning(false);
	    n.disconnect();
	}
    }
    
    public boolean isConnected(){ return connected;}
    public boolean isTracking(){ return tracking;}

    public void init(){}
    public static void joinAll(){
	for (BasicNode n : masterList){
	    try{
		//nodeLog.debug("Awaiting shutdown signals. ");
		n.join();
	    }catch(InterruptedException ie){
		nodeLog.fatal("Interrupted while waiting for node threads to join. Ignoring.");
	    }
	}
    }
    
    public BasicNode(){
	super();
	// register node
	masterList.add(this);

	// configure node
	nodeID=nodeCount;
	nodeCount++;
	setDaemon(true);

	// label node
	setNodeTypeName();
	nodeLog.debug("  ::created node: \""+nodeName+"\"");
    }

    protected final void setNodeName(String newName){
	nodeName=newName;
	setThreadName(nodeName);
    }
    protected final void setNodeTypeName(){
	nodeTypeName=getClass().getSimpleName();
	setNodeName(nodeTypeName+"_#"+nodeID);
    }

    public void setReadable(boolean _write){readable=true;}
    public void setRunning(boolean _run){ running=_run;}
    protected void setThreadName(String _tName){ super.setName(_tName);}
    public void setWriteable(boolean _read){writeable=true;}

    public static void startAll(){
	for (BasicNode n : masterList){
	    n.start();
	}
    }
    
    public void disconnect(){connected=false;}

    public static void disconnectAll(){
	for (BasicNode n : masterList){
	    n.disconnect();
	}
    }
    
    protected boolean receiveFrame(){
	if (receiveQueue.isEmpty()){
	    inFrame = null;
	    return false;
	}else{
	    inFrame = receiveQueue.poll();
	    //nodeLog.info("     # frame:"+ inFrame.getID()+"   >> node:"+nodeID);
	    fireAction("update.disp");
	    return true;
	}
    }

    public void run(){
	//nodeLog.trace("..Starting "+nodeName);
	//nodeLog.trace("         "+getStatus());
	
	if(!connected){
	    nodeLog.error( "!!Attempt to start node #"+nodeID+" without connection");
	    BasicNode.stopAll();
	}
	
	while(running){
	    if(true == connected){
		init();
		while(connected){
		    if(readable){
			update_read();
		    }
		    if(writeable){
			receiveFrame();
			update_write();
			inFrame=null;
		    }
		    yield();
		    //nodeLog.debug("  ..Node "+nodeID+" updating.");
				
		}
		close();
		//nodeLog.trace("  ..Node "+nodeName+" closed.");

	    }

	    try{
		sleep(100);
	    }catch(InterruptedException ie){
	        nodeLog.error("Node interrupted while sleeping: "+this.toString());
		// and continue to idle.
	    }
	}
	postRun();
	//nodeLog.trace("..Node "+nodeName+" shut down.");
    }


    protected void sendFrame(ByteFrame _frameToSend){
	frameCountSent++;

	//nodeLog.debug("     # frame:"+ _frameToSend.getID()+"    << node:"+nodeID);
	for (BasicNode n : outNodes){
	    if(false==n.enqueueFrame(_frameToSend)){
		nodeLog.fatal("  error while enqueueing frame traffic.  Exiting.");
		System.exit(-1);
	    }
	}
    }

    public void preRun(){}
    
    public void postRun(){}

    public String toString(){
	return new String(nodeName);
    }

    public void update_read(){ }

    // default behavior is to relay this frame on again.
    public void update_write(){
	if(null!=inFrame){
	    sendFrame(inFrame);
	}
    }
}
