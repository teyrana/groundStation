package cubesat.groundStation;

// standard
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;

// external
import org.apache.log4j.Logger;

class FieldFrame extends ByteFrame{
    protected static Logger frameLog = Logger.getLogger(ByteFrame.class);


    public FieldFrame(){
    }


    public String toString(){
	return super.toString()+" (field frame NYI)";
    }

    public boolean write(OutputStream dest){
	// try{
	//     // 
	//     return false;
	// }catch(IOException ioe){
	//     framelog.error("IOException while attempting to write packet. returning failure.");
	//     return false;
	// }
	return false;
    }



}
