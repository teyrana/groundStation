package cubesat.groundStation;

import java.lang.Math;
import java.nio.*;


// external
import org.apache.log4j.Logger;

class TelemetryField {
    protected static Logger fieldLog = Logger.getLogger(ByteFrame.class);

    // Integer. Required. 
    // GUID (globally unique identifier)  (this must uniquely identify this telemetry point 
    //     against all other telemetry points.
    public int pointID=0;

    // String. Optional.
    // Display name to identify this point to humans.
    public String name=null;

    // Required. String. (default = 'uint8')
    // [u]intNN, an integer.  (little endian)  
    //     u - if present, this value is unsigned.
    //     NN - the size of the max value to be stored. (and the number of bytes to retreive
    // byte_(N)+ - A byte array of length N
    //     NN is an string of at least 1 digits ("byte_6" == "byte_06"
    //     (note: 'byte_1' is equivalent to 'uint8')
    // floatXX - floating point number. (little endian)
    //     not yet supported.
    //     "http://en.wikipedia.org/wiki/Floating_point"
    //     half (float16), float (float32), double (float64)
    // ZuluDate(Tiny|Compact)
    //     interprets a 32 bit integer (littleEndian) as unix time in seconds.
    //     displays the year, day of year, and seconds in the day.
    //     "ZuluDateTiny" displays "YYYYDDDsssss" (suitable for filenames)
    //     "ZuluDateCompact" displays "YYYY:DDD:ss,sss"
    // public enum pointType {uint8,int16, int8,int16,int32,int64}
    public String type=null;
    public int byteWidth=0;

    // Integer(Recomended) or String
    // default="00" (CDH Command and Data Handling)
    // Used to identify which subsystem this telemetry point is categorized under.
    public subsystem majorSubsystem=subsystem.NONE;
    public enum subsystem {
	CDH (0,	"CDH",	"Command and Data Handling"),
	Thermal (1, "Therm","Thermal Management System"),
	EPS (2,"EPS","Electrical Power System"),
	ADCS (3,"ADCS","Attitude Determination and Control"),
	STRUCT (4,"STRUCT","Structural"),
	THRUST (5,"THRUST","Propulsion"),
	PAYLOAD (6,"PL","Payload"),
	NONE (7,"n/a","null");

	public final int id;
	public final String abbrev;
	public final String name;
		
	subsystem(int _id, String _abbrev, String _longName){
	    this.id=_id;
	    this.abbrev = _abbrev;
	    this.name = _longName;
	}
    }

    // Integer(Recomended) or String
    // default="00" (CDH Command and Data Handling)
    // Used to identify which subsystem this telemetry point is categorized under.
    public int componentID=0;
    public String componentName=null;
    
    // boolean.  (default=true)
    // set true if you want this displayed.
    public boolean display=true;

    // boolean. (default=true)
    // set false to prevent storage in telemetry log. 
    public boolean store=false;

    public boolean limitsActive=true;
    // Warning/Yellow limit.
    // Exceeding these indicates operator should check up on these.
    public float warningLimitMin=(float)0.0;
    public float warningLimitMax=(float)0.0;

    // Error Limit. Red.
    // Exceeding these indicates a serious problem
    public float errorLimitMin=(float)0.0;
    public float errorLimitMax=(float)0.0;

    /* ===== ===== ===== Instance Methods ===== ===== ===== */

    public TelemetryField(){ }

    public void setField(String fieldName, String fieldValue){
	fieldName = fieldName.toLowerCase();

	if(fieldName.equals("name")){
	    name=fieldValue;
	}else if(fieldName.equals("id")){
	    pointID = Integer.parseInt(fieldValue);
	}else if(fieldName.equals("subsystem")){
	    majorSubsystem= getSubsystem(fieldValue);
	}else if(fieldName.contains(        "default")){
	    // default value not yet implemented.
	}else if(fieldName.equals("display")){
	    if((fieldValue.startsWith("n"))||(fieldValue.startsWith("f"))||(fieldValue.startsWith("0"))){
		display=false;
	    }else{
		display=true;
	    }
	}else if(fieldName.equals("store")){
	    if((fieldValue.startsWith("n"))||(fieldValue.startsWith("f"))||(fieldValue.startsWith("0"))){
		store=false;
	    }else{
		store=true;
	    }
	}else if(fieldName.equals("limit enable")){
	    if((fieldValue.startsWith("n"))||(fieldValue.startsWith("f"))||(fieldValue.startsWith("0"))){
		limitsActive=false;
	    }else{
		limitsActive=true;
	    }
	}else if(fieldName.equals("warning min")){
	    warningLimitMin = (float)Double.parseDouble(fieldValue);
	}else if(fieldName.equals("warning max")){
	    warningLimitMax = (float)Double.parseDouble(fieldValue);
	}else if(fieldName.equals("error min")){
	    errorLimitMin = (float)Double.parseDouble(fieldValue);
	}else if(fieldName.equals("error max")){
	    errorLimitMax = (float)Double.parseDouble(fieldValue);
	}else if(fieldName.equals("type")){
	    setType(fieldValue);
	}else if(fieldName.contains("width")){
	    setWidth(Integer.parseInt(fieldValue));
	}else{
	    fieldLog.warn("unrecognized option: "+fieldName);
	}
    }

    public void setType(String typeName){
	//fieldLog.warn(" .. setType: "+typeName);
	if(null!=typeName){
	    this.name = typeName;
	}
    }

    public void setWidth(int _width){
    	//fieldLog.warn(" .. setType: "+typeName);
	byteWidth=_width;
    }
    
    /* ===== ===== ===== Static Methods ===== ===== ===== */

    public static subsystem getSubsystem(String abbr){
        for( subsystem ss : subsystem.values()){
	    if(ss.abbrev.equals(abbr)){
		return ss;
	    }
	}
	return subsystem.NONE;
    }
    
    public static byte[] parseAsByte(byte[] data, int len){
	// redundant? why bother?
	return data;
    }

    public static float parseAsFloat(byte[] data, int len){
	float result = 0;
	ByteBuffer bb = null;
	if(32==len){
	    bb = ByteBuffer.allocate(4);	
	    bb.put(data);
	    result = bb.getFloat();

	    // // works, but deprecated.
	    // // IEEE float32 format
	    // // special cases: denormalized.... ? 
	    // //                zero : all zeros
	    // //                infty: all ones
	    // //                NaN: exp == 255, mantissa mixed. 

	    // // bit 31: sign  (MSB = 31)
	    // int sign = ((data[0])>>7)&0x01; 
	    // // bit 30-23: exponent
	    // double exponent = (double)(((0x7F& data[0])<<1) +((0x80&data[1])>>7) -127);
	    // // bit 22-0: fraction (LSB = 0)
	    // double significand = (double)( ((data[1]&0x7F)<<16) +((data[2]&0xFF)<<8) +(data[3]&0xFF) +(1<<23));
	    // significand = significand /8388608.0;  // implicit leading 1
	    // result = (float)(Math.pow(-1,sign)*Math.pow(2,exponent)*significand);
	}else if(64==len){
	    // Arduino does not support float64 / double
	    // 1, sign; 
	    // 11, exponent
	    // 52, mantissa
	    bb = ByteBuffer.allocate(8);	
	    bb.put(data);
	    result = bb.getFloat();
	}else{
	    result =0;
	}
	//System.out.println(String.format("result:%g",result));
	return result;
    }

    public static int parseAsInt(byte[] data, int len){ return parseAsSignedInteger(data, len);}
    
    public static int parseAsSignedInteger(byte[] data, int len){
	int result=0;
	switch(len){
	case 8:
	    result=data[0]&0x7F;
	case 16:
	    result=(data[0]&0x7F)*0x100 + data[1]&0xFF;
	case 32:
	    result=(data[0]&0x7F)*0x1000000 + (data[1]&0xFF)*0x10000 + (data[2]&0xFF)*0x100 + (data[3]*0xFF);
	}
	if(0<(data[0]&0x80)){
	    result=result*-1;
	}
	return result;
    }

    public static int parseAsTime(byte[] data, int len){
	int seconds = parseAsUnsignedInteger(data, len);
	return seconds;
    }

    public static int parseAsUInt(byte[] data, int len){ return parseAsUnsignedInteger(data, len);}
    
    public static int parseAsUnsignedInteger(byte[] data, int len){
	int result=0;
	switch(len){
	case 8:
	    result=data[0]*0xFF;
	case 16:
	    result=data[0]*0xFF*0x100 + data[1]*0xFF;
	case 32:
	    result=(data[0]&0xFF)*0x1000000 + (data[1]&0xFF)*0x10000 + (data[2]&0xFF)*0x100 + (data[3]*0xFF);
	}
	return result;
    }
    

    
}

