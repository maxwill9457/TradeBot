import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;



public class MindModuleUnit extends DefinedData{
	
	String ProcessName = "MindModuleUnit";
	String SimulationMode;
	String MindModuleUnitState;
	String target;
	String target_num;
	
	String TradeAction;
	
	BoardInfo BoardInfo;
	UserProperty UserProperty;
	TradeStatics TradeStatics;
	
	LogUnit MindMoudleLog;
	LogUnit ErrorLog;
	
	Random rnd ;
	
	MindModuleUnit(String target,String target_num,BoardInfo BoardInfo, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String LogPath,String SimulationMode){
		
		String SubProcessName = "Initiation";
		MindModuleUnitState = "PREPARE";
		this.target = target;
		
		MindMoudleLog = new LogUnit(LogPath+"MindModuleUnit//statics//", this.target+"MindModule",1); // create statics log file	
		this.ErrorLog = ErrorLog;
		
		//---------------------------初期設定----------------------------------------
		this.SimulationMode = SimulationMode;
		this.target = target;
		this.BoardInfo = BoardInfo;
		this.UserProperty = UserProperty;
		this.TradeStatics = TradeStatics;
		
		MindModuleUnitState = "READY";
		try{
		
			rnd = new Random();
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}	
	}
		
	public void run(){ 
		String SubProcessName = "Main_Loop ";
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
		String PreState = MindModuleUnitState;
		while(!MindModuleUnitState.equals("END")){
			switch(MindModuleUnitState){
			
			case "READY":
				//System.out.println( "DecisionMakingUnit READY");
				break;	
			case "START":	
				if (PreState.equals("READY")){
					//初回のプロセスの起動に使う
					PreState = MindModuleUnitState;
					LogTitleInitial();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start"+"Action Score"+ UserProperty.UserAction.ActionScore);
				}	
				try{
					UserProperty.UserAction.ActionScore = rnd.nextInt(10000);	
					MindLogWrite();
				}catch (Exception e){
					ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
				}	
				
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				break;
			case "PAUSE":
				//System.out.println( "DecisionMakingUnit PAUSE");
				break;
			case "FINISHING":
				//---------------気配板プロセスの完了待つ-----------------------------	
				/*while(!XXXX.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}*/
				//System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+"Finishing");
				MindModuleUnitState = "END";
				break;
			case "ERROR":	
				//System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+"Error");
				break;
				
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
				
			}	
		}		
		//start any web access process 
	}
	
	public void MindModule(){
		
	}
	
	void LogTitleInitial(){
		//Log Label 
		String temp; 
		temp =  "StaticsNumber	DataNumber	yyyy/MM/dd	HH:mm:ss.SSS	";
		temp = temp + "Score	";
		temp  = temp + "\r\n";
		MindMoudleLog.FileWrite(temp);	
	}
	
	public void MindLogWrite(){
		String SubProcessName = "MindLogWrite";
    	Calendar rightNow;
    	Date Now = new Date();
    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
    	rightNow = Calendar.getInstance();
		Now = rightNow.getTime();
		
		String temp = TradeStatics.StaticsNumber +"	"+ BoardInfo.DataNumber+"	"+D.format(Now)+"	"; 
		
		temp =temp + UserProperty.UserAction.ActionScore+"	";	
		temp  = temp + "\r\n";
		MindMoudleLog.FileWrite(temp);	
		
	}	
	
	void ErrorLogWrite(String ProccessName, String SubProcessName , String Error){
		Calendar rightNow;
		Date Now = new Date();
    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
    	rightNow = Calendar.getInstance();
		Now = rightNow.getTime();
		
		String temp =  D.format(Now) + "	" + ProccessName + "	" + SubProcessName + "	" +Error +"\r\n";
		ErrorLog.FileWrite(temp);
		
	}
	
	//public void EnvironmentValidation(){ }// 市場判断
	//public void 
	

}
