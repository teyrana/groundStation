package cubesat.groundStation;

import gnu.io.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class DisplayNode extends BasicNode{

    public void close(){
	
    }
    
    public boolean connect(){
	connected=true;
	return connected;
    }
    
    public DisplayNode(){
	super();
	connected=true;
	writeable=true;
    }

    public void update_read(){
	//System.out.println("DisplayNode::updating_read...");
    }
    
    public void update_write(){
	
	//System.out.println("DisplayNode::updating_write...");
    }

}
