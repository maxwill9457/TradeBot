import java.util.*;



import java.text.*;


public class Temp extends DefinedData{// 意思決定   Trade情報により、買、維持、売の行動　また価格を決める
	String ProcessName = "temp";
	String TradeOperatorUnitState;
	String target;
	
	BoardInfo BoardInfo;
	UserProperty UserProperty;
	TradeStatics TradeStatics;
	
	LogUnit TradeOperateLog; // create statics log file
	LogUnit ErrorLog;
	
	
	
	Temp(String target,BoardInfo BoardInfo, UserProperty UserProperty,TradeStatics TradeStatics, LogUnit ErrorLog){

		String SubProcessName = "Initiation";
		TradeOperatorUnitState = "PREPARE";
		System.out.println( target+ "	"+ProcessName+"_"+"Activating" );
		
		this.target = target;
		
		
		TradeOperatorUnitState = "READY";
		System.out.println(target+ "	"+ProcessName+"_"+"Ready" );	
	}
		
	public void run(){ 
		String SubProcessName = "Main_Loop ";
		System.out.println(target+ "	"+ProcessName+"_"+"Standby" );
		String PreState = TradeOperatorUnitState;
		while(!TradeOperatorUnitState.equals("END")){
			switch(TradeOperatorUnitState){
			
			case "READY":
				
				break;	
			case "START":	
				if (PreState.equals("READY")){
					//初回のプロセスの起動に使う
					PreState = TradeOperatorUnitState;
					System.out.println(target+ "	"+ProcessName+"_"+"Start");
				}				
				System.out.println( target+ "	"+ProcessName+"_"+"Start");
				break;
			case "PAUSE":
				//System.out.println( "TradeOperatorUnit PAUSE");
				break;
			case "FINISHING":
				//---------------気配板プロセスの完了待つ-----------------------------	
				/*while(!XXXX.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}*/
				//System.out.println( "TradeOperatorUnit FINISH");
				TradeOperatorUnitState = "END";
				break;
			case "ERROR":	
				//System.out.println( "TradeOperatorUnit ERROR");
				break;
				
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
			}	
		}		
		System.out.println(target+ "	"+ProcessName+"_"+"End" );
		//start any web access process 
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
}