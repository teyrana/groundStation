package cubesat.groundStation;

import gnu.io.*;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
//import java.util.*;

public class CommandNode extends FileNode {
    protected static Logger nodelog = Logger.getLogger(FileNode.class);

    public void actionPerformed(ActionEvent ae){
	if(ae.getActionCommand().equalsIgnoreCase("connect")){
	    //nodeLog.debug("  >> request connect");

	}
    }
    public void open(String _name, char mode){
    }

    public void update_read(){
	// NYI
    }
    
    public void update_write(){
	// NYI
    }
    
}
