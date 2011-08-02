package cubesat.groundStation;

// external
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// standard
import java.util.Date;
public class Time{
    protected int epoch_seconds;
    protected final int reference_time_2011=1293840000;

    // ZULU time
    protected int day_z=0;
    protected int sec_z=0;

    protected int day_utc=0;
    protected int sec_utc=0;

    public Time(int unix_time_seconds){

	epoch_seconds = unix_time_seconds;

    }




    
    
    

}
