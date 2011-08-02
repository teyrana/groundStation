package cubesat.groundStation;

// external libraries
import gnu.io.*;

// standard libraries
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

public class SerialNode extends ByteNode{
    // documentation and site for RXTX library: 
    // http://rxtx.qbang.org/wiki/index.php/Main_Page

    // serial port options
    protected int baud=9600;  // default speed
    protected static final String[] defaultPortNames={"COM3", "/dev/tty.usbmodem1d11", "/dev/tty.usbserial-A600ebkT"};
    protected String portName="";
    protected CommPort port=null;
    protected String portTypeName="";

    // // method (a)
    // // these two classes are *probably* deprecated.
    // // needs real-world duplex testing.
    // protected SerialReader telemetryReceive;
    // protected SerialWriter telemetrySend;

    // method (b)
    protected InputStream receiveStream=null;
    protected byte[] receiveBuffer=new byte[1024]; // rename me!!
    protected OutputStream sendStream=null;

    public SerialNode(){
	super();
	setPort("none");
    }

    public SerialNode(String _portName){
	this();
	connect(_portName);
    }
    
    /* ===== ===== Instance Methods ===== ===== */

    public void actionPerformed(ActionEvent ae){
	String cmd = ae.getActionCommand();
        String[] params = cmd.split(":");

	if(cmd.contains("setport")){
	    // parse string ... String.
	    nodeLog.debug("  >> setting port: '"+params[1]+"'");
	    setPort(params[1]);
	    fireAction("update.label");
	}else{
	    super.actionPerformed(ae);
	}
    }

    public boolean connect(String _portName) {
	setPort(_portName);
	return connect();
    }

    public void connectDefault(){
	nodeLog.info("Trying default Port Names");
	for (String portName : defaultPortNames){
	    if(connect(portName)){
		return;
	    }
	}
	nodeLog.error("!! Could not connect to default ports.");
	printPortList();
    }

    public boolean connect(){
	try{
	    CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
	    if ( portIdentifier.isCurrentlyOwned()){
		nodeLog.error("Error: Port is currently in use");
	    }else{
		port = portIdentifier.open("Telemetry Data Port", 2050);
		
		if ( port instanceof SerialPort ) {
		    SerialPort serialPort = (SerialPort)port;
		    // Practical defaults.  
		    serialPort.setSerialPortParams(this.baud,
						   SerialPort.DATABITS_8,
						   SerialPort.STOPBITS_1,
						   SerialPort.PARITY_NONE);

		    // // method (a)
		    // telemetrySend.setStream(serialPort.getInputStream());
		    // telemetryReceive.setStream(serialPort.getOutputStream());
		    // (new Thread(telemetrySend)).start();
		    // (new Thread(telemetryReceive)).start();

		    // method (b)
		    receiveStream = serialPort.getInputStream();
		    readable=true;
		    sendStream = serialPort.getOutputStream();
		    writeable=true;
		    
		    // in either method, we're good.
		    connected=true;
		    nodeLog.debug("  >>Successfully connected to port: " + portName);
		    return true;
		}else{
		    // (Only serial ports are handled by this example)
		}
	    }

	} catch ( PortInUseException piue ) {
	    nodeLog.error("    !! Port already in use: " + portName);
	} catch ( NoSuchPortException nspe ) {
	    nodeLog.error("    !! Invalid port name: " + portName);
	} catch (IOException ioe) {
	    nodeLog.error(" IO Exception while connecting serial port.");
	    nodeLog.error(ioe);
	    ioe.printStackTrace();
	}catch(Exception e){
	    // optional.
	    nodeLog.error("!!General Exception while connecting serial port.");
	    nodeLog.error(e);
	    e.printStackTrace();
	}
	return false;
    }
    
    public void close(){
	try{
	    writeable=false;
	    readable=false;
	    receiveStream.close();
	    sendStream.close();
	    port.close();
	    nodeLog.debug("    ::Closed port: " + portName);

	}catch(IOException ioe){
	    System.err.println("IO Exception while closing down telemetry monitor. Exiting.");
	    System.exit(-1);
	}
	
	avgFrameSizeSent=((float)byteCountSentFrame)/((float)frameCountSent);
	avgGapSize = ((float)byteCountTotalGap)/((float)gapCount);
	nodeLog.info("  ");
	nodeLog.info("  Finished serial port main loop:");
	nodeLog.info("===== ===== ===== = ===== ===== =====");
	nodeLog.info(" Read "+byteCountRead+" bytes and scanned "+byteOffsetScanned+" bytes");
	nodeLog.info("     Scanned "+byteCountSentFrame+" bytes into "+frameCountSent+" frames. avg:" +avgFrameSizeSent);
	nodeLog.info("     Discarded "+byteCountTotalGap+" bytes from "+gapCount+" gaps. avg:" +avgGapSize);
	nodeLog.info(" ");
    }
    
    public void disconnect(){
	connected=false;
    }

    // to ensure that the serial port is actually closed, and released.
    protected void finalize() throws Throwable{
	try{
	    close();
	}finally{
	    super.finalize();
	}
    }

    public static Vector<CommPortIdentifier> getPortList(){
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
	Vector<CommPortIdentifier> portList= new Vector<CommPortIdentifier>();

	while ( portEnum.hasMoreElements() ) {
	    Object thisElem= portEnum.nextElement();
	    //CommPortIdentifier portIdentifier =
	    //portList.nextElement();
	    if (thisElem instanceof CommPortIdentifier){
		CommPortIdentifier portIdentifier = (CommPortIdentifier)thisElem;
		portList.add(portIdentifier);
	    }else{
		nodeLog.error(" cast error when querying ports: could not cast: ");
		nodeLog.error(thisElem);
	    }
	}

	return portList;
    }

    public int getBaud(){ return baud;}
    public String getPortName(){ return portName; }

    protected static String getPortTypeName ( int portType ){
        switch ( portType){
            case CommPortIdentifier.PORT_I2C:
                return "I2C";
            case CommPortIdentifier.PORT_PARALLEL:
                return "Parallel";
            case CommPortIdentifier.PORT_RAW:
                return "Raw";
            case CommPortIdentifier.PORT_RS485:
                return "RS485";
            case CommPortIdentifier.PORT_SERIAL:
                return "Serial";
            default:
                return "unknown type";
        }
    }
    protected static Vector<String> getPortNameList(){
	Vector<CommPortIdentifier> portList= getPortList();
	Vector<String> portNameList=new Vector<String>();
	for(int i = 0; i<portList.size(); i++){
	    CommPortIdentifier curPort = portList.get(i);
	    portNameList.add(curPort.getName());
	}
	return portNameList;
    }

    public static void printPortList(){
	Vector<CommPortIdentifier> portList= getPortList();
	System.out.println("    Available Ports:");
	for(int i = 0; i<portList.size(); i++){
	    CommPortIdentifier curPort = portList.get(i);
	    System.out.println("        "+curPort.getName()+": "+getPortTypeName(curPort.getPortType()));
	}	
    }
    
    public void setBaud(int _baud){
	if(isConnected()){
	    nodeLog.warn("attempt to change the port on an active serial connection");
	}else{
	    switch(_baud){
	    case 2400:
	    case 4800:
	    case 9600:
	    case 19200:
	    case 38400:
	    case 57600:
	    case 1152000:
		baud = _baud;
	    default:
		baud = 9600;
		nodeLog.warn("  !! Unrecognized baud rate requested: "+_baud);
		nodeLog.warn("  !! Setting to default baud rate: "+baud);
	    }
	    fireAction("update.label");
	}
    }

    public void setMinimumBufferSize(int size){
	if(size > receiveBuffer.length){
	    receiveBuffer=new byte[(int)(size*1.1)];
	}
    }

    public void setPort(String _portName){
	if(isConnected()){
	    nodeLog.warn("attempt to change the port on an active serial connection");
	}else{
	    this.portName=_portName;
	    setNodeName(nodeTypeName+"_#"+nodeID+"_on_"+this.portName);
	}
    }

    public void shutdown(){
	return;
    }

    private int byteCountRead=0;
    public void update_read(){

	int len = 0;

	if((byteLimitDebug > 0 ) && (byteCountRead > byteLimitDebug)){
	    nodeLog.warn("!!Byte Limit hit: "+byteOffsetScanned+"/"+byteCountRead+"/"+byteLimitDebug);
	    stopAll();
	}
	
	try {
	    if(0<receiveStream.available()){
		len = this.receiveStream.read(receiveBuffer);		
		byteCountRead +=len;
		scan(receiveBuffer, len);
		
		if(-1 == len){
		    nodeLog.fatal("received -1 length== EOF while reading serial port.");
		    System.exit(-1);
		}		    
	    }
	    yield();
	}catch(IOException ioe){
	    nodeLog.error("Error while reading serial port: "+ioe);
	    ioe.printStackTrace();
	}
	
    }

    public void update_write(){
	// no-op. (NYI)
    }
}
