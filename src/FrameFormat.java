 package cubesat.groundStation;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import au.com.bytecode.opencsv.CSVReader;

public class FrameFormat{
    protected static Logger formatLog = Logger.getLogger(BasicNode.class);

    protected TelemetryField[] telemetryFieldList=null;
    /* ===== ===== Instance Methods ===== ===== */

    public FrameFormat(){
    }

    public void loadTelemetryDefinitions(String fileName){
	CSVReader formatReader = null;
	String[] headers = null;
	int idCol=0;
	int typeCol=0;
        int headerLine=1;

	try{
	    if(null!=fileName){
		formatReader = new CSVReader(new FileReader(fileName),',');
		List<String[]> fileEntries = formatReader.readAll();
		headers = fileEntries.get(headerLine);
		
		// locate the field id column...
		for(int i = 0; i<headers.length; i++){
		    String s = headers[i];
		    if(s.equalsIgnoreCase("ID")){
			idCol=i;
			formatLog.debug("found id column:" + idCol);
		    }else if(s.equalsIgnoreCase("Type")){
			typeCol=i;
			formatLog.debug("found type column:" + typeCol);
		    }
		}


		// then find the largest id value
		int maxID = 0;
		for(int i = headerLine+1; i<fileEntries.size(); i++){
		    int curID = Integer.parseInt(fileEntries.get(i)[idCol]);
		    if(maxID< curID){
			maxID = curID;
		    }
		}

		// and size our new format list appropriately.
		telemetryFieldList=new TelemetryField[maxID];
		    
		// now copy every record in, indexing by ID#
		int lineIndex=0;
		for(lineIndex=headerLine+1; lineIndex<fileEntries.size(); lineIndex++){
		    if(fileEntries.get(lineIndex).length <Math.max(idCol, typeCol)){
			continue;
		    }
		    int curID = Integer.parseInt(fileEntries.get(lineIndex)[idCol]);
		    telemetryFieldList[curID]=new TelemetryField();

		    //String curTypeName = fileEntries.get(lineIndex)[typeCol];
		    //telemetryFieldList[curID].setType(curTypeName);

		    for(int curCol=0;curCol<fileEntries.get(lineIndex).length; curCol++){
			//formatLog.debug("   ..(i) ["+headers[curCol]+"]="+fileEntries.get(lineIndex)[curCol]);
			telemetryFieldList[curID].setField(headers[curCol], fileEntries.get(lineIndex)[curCol]);
		    }
		}
		formatReader.close();
	    }
	}catch(FileNotFoundException fnfe){
	    formatLog.error("Could not find format file "+ fileName);
    	}catch(IOException ioe){
    	    formatLog.error("Generic IOException while reading format file: " +ioe);
	}
    }

    public void loadFrameNames(String fileName){
	CSVReader formatReader = null;
	String[] headers = null;
	int headerLine = 1;

	try{
	    if(null!=fileName){
		formatReader = new CSVReader(new FileReader(fileName),',');
		List<String[]> fileEntries = formatReader.readAll();
		headers = fileEntries.get(headerLine);

		// NYI
		// not yet necessary

		formatReader.close();
	    }
	}catch(FileNotFoundException fnfe){
	    formatLog.error("Could not find format file "+ fileName);
    	}catch(IOException ioe){
    	    formatLog.error("Generic IOException while reading format file: " +ioe);
	}
    }
    
    public void loadFrameFormats(String fileName){
	CSVReader formatReader = null;
	String[] headers = null;
	int headerLine = 1;

	try{
	    if(null!=fileName){
		formatReader = new CSVReader(new FileReader(fileName),',');
		List<String[]> fileEntries = formatReader.readAll();
		headers = fileEntries.get(headerLine);

		// NYI
		// not yet necessary

		formatReader.close();
	    }
	}catch(FileNotFoundException fnfe){
	    formatLog.error("Could not find format file "+ fileName);
    	}catch(IOException ioe){
    	    formatLog.error("Generic IOException while reading format file: " +ioe);
	}
    }
    
    public void loadDefaultFiles(){
	loadTelemetryDefinitions("telemetry_definitions.csv");
	//loadFrameNames("frame_names.csv");
	//loadFrameFormats("frame_formats.csv");
    }

}



