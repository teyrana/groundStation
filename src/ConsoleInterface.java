package cubesat.groundStation;

// standard libraries
import java.util.Vector;
import java.awt.event.*;

// external
import org.apache.log4j.Logger;

public class ConsoleInterface implements ActionListener {
    static Logger clilog = Logger.getLogger(RootWindow.class);
    
    protected static final int consoleWidth=80;

    // this is to enforce the singleton...ness.
    // this means that there should never be more than one of these frames. Enforce it.
    protected static ConsoleInterface onlyInterface=null;

    // back-end handles
    protected SerialNode sourceNode=null;
    protected DisplayNode dispNode=null;
    protected FileNode logNode=null;  // not used...

    private ConsoleInterface(){    }

    public void actionPerformed(ActionEvent ae){
	String cmd = ae.getActionCommand();
	String[] params = ae.getActionCommand().split("[.]");
	for(int i =0; i<params.length; i++){
	    params[i]=params[i].toLowerCase();
	}
	
	//clilog.trace("  .. Event receive@ConsoleInterface= "+ae.getActionCommand());

	if(params[0].equalsIgnoreCase(sourceNode.getNodeTypeName())){
	    // ignore messages from SerialNodes
	}else if(params[0].equalsIgnoreCase(dispNode.getNodeTypeName())){
	    if(params[1].equalsIgnoreCase("update")){
		if(params[2].equals("disp")){
		    updateFrameDisplay(dispNode.getActiveFrame());
		}
	    }
	}else if(params[0].equalsIgnoreCase(logNode.getNodeTypeName())){
	    // ignore all messages from fileNodes
	}else{
	    clilog.warn("!! unrecognized action requested='"+cmd+"'");
	}
    }

    // enforce singleton design pattern
    public static ConsoleInterface getInterface(){
	if(null==onlyInterface){
	    onlyInterface = new ConsoleInterface();
	}
	return onlyInterface;
    }

    public void initialize(SerialNode _sourceNode, DisplayNode _dispNode, FileNode _logNode){
	if((null==_sourceNode)||(null==_dispNode)||(null==_logNode)){
	    clilog.error("Attempt to populate the gui without populating the model");
	    return;
	}
	sourceNode=_sourceNode;
	dispNode=_dispNode;
	logNode=_logNode;

	// do not do this until the interface construction is finished.
	sourceNode.addActionListener(this);
	dispNode.addActionListener(this);
	logNode.addActionListener(this);

    }

    public void setSerialNode(SerialNode _node){ sourceNode=_node;}
    public void setDisplayNode(DisplayNode _node){ dispNode=_node;}
    public void setOutputNode(FileNode _node){ logNode=_node;}

    public void updateFrameDisplay(ByteFrame updateSource){
	//System.out.println(" >> updating frame display elements");
	if(null==updateSource){
	    return;
	}else{
	    // dump the current frame to the console
	    // for dos/windows prompts, this is about the max width
	    updateSource.dump(false,20,4);

	}
    }
    

}
