package cubesat.groundStation;

// external
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// standard
import java.util.Date;
public class main{

    // defaults
    private static boolean enableCLI=true;
    private static boolean enableDebug=false;
    private static boolean enableGUI=false;


    /* TRACE,
       DEBUG,
       INFO,
       WARN,
       ERROR,
       FATAL
    */
    static Logger mlog = Logger.getLogger(main.class);
    //http://logging.apache.org/log4j/1.2/manual.html

    public static void scan_args(String[] args){
	for(String s : args){
	    //System.out.println(" .. arg:"+ s);
	    if(s.startsWith("-")){
		if(s.contains("d")){
		    enableDebug = true;
		}
		if(s.contains("g")){
		    enableGUI=true;
		    enableCLI=false;
		}else if(s.contains("c")){
		    enableCLI=true;
		    enableGUI=false;
		}

	    }
	}
    }
    
    public static void main(String[] args){
	PropertyConfigurator.configure("log4j.cfg");
	mlog.warn(" ");
	mlog.warn("=========================================================");
	mlog.warn("Starting logs. " + (new Date()).toString());
	mlog.warn("=========================================================");
	mlog.warn(" ");

	SerialNode sourceNode = new SerialNode();
	DisplayNode dispNode = new DisplayNode(); // change this type
	FileNode logNode = new FileNode();
	RootWindow guInterface = null;
	ConsoleInterface clInterface= null;
	
	scan_args(args);
	//mlog.debug("  .. arg: enableCLI = "+enableCLI);
	//mlog.debug("  .. arg: enableDebug = "+enableDebug);
	//mlog.debug("  .. arg: enableGUI = "+enableGUI);
	
	// source Node
	mlog.info("Building Telemetry Monitor Backend");
	sourceNode.connectDefault();
	sourceNode.setDebugByteLimit(500);

	// display Node
	sourceNode.addOutputNode( dispNode);

	// output Node 
	sourceNode.addOutputNode(logNode);
	logNode.open("telem_log.bin",'w');

	if(enableDebug){
	    //Time t = new time();
	    FrameFormat dbf = new FrameFormat();
	    dbf.loadDefaultFiles();
	    System.exit(0);
	}else if(enableGUI){
	    mlog.debug("Building Telemetry Monitor GUI");
	    guInterface = RootWindow.getInterface();
	    guInterface.initialize(sourceNode,dispNode,logNode);
	    sourceNode.setDebugByteLimit(800);
	}else if(enableCLI){
	    mlog.info("Building CLI");
	    clInterface = ConsoleInterface.getInterface();
	    clInterface.initialize(sourceNode,dispNode,logNode);
	    sourceNode.setDebugByteLimit(320);
	}else{
	    System.err.println("  No Interface specified.  Exiting.");
	    System.exit(-1);
	}
	
	mlog.info("Setup is finished, starting threads.");
	mlog.info(" ");

	BasicNode.startAll();

	try{
	    Thread.sleep(1000);
	}catch(InterruptedException ie){
	    mlog.error("Main method interrupted while sleeping");
	}
	BasicNode.joinAll();

	//System.exit(0);

	return;
    }


}
