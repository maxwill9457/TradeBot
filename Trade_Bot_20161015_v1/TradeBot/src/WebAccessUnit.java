
import java.util.*;

import java.text.*;


public class WebAccessUnit extends DefinedData {
	
	
	String ProcessName = "WebAccessUnit";
	String SimulationMode;
	int Speed;
	
	String WebAccessUnitState;
	String target;
	
	BoardInfo BoardInfo;
	UserProperty UserProperty;
	
	TradePageManagerUnit_Monex TradePageManager; //交易股票的所有網業開啟 
	CatchException TradePageManager_catchException;
	
	TradeBoardSimulator TradeBoardSimulator;
	CatchException TradeBoardSimulator_catchException;
	
	TimeStar TradePageUpdateTask;
	java.util.Timer TradePageUpdateTimer;

	LogUnit  ErrorLog;
	
	
	WebAccessUnit(String target, BoardInfo BoardInfo,UserProperty UserProperty, LogUnit ErrorLog,String SimulationMode, String LogPath,int Speed){ //initialization
		//1秒截取數據一次
		String SubProcessName = "Initiation";
		WebAccessUnitState = "PREPARE";
		
		this.SimulationMode = SimulationMode;
		this.Speed = Speed;
		this.target = target;
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
		
		this.ErrorLog = ErrorLog;
		//ActionDecisionLog = new LogUnit(this.target); // create log file to record ActionDecision result
		try{
			//TradePageManager = new TradePageManagerUnit(target);
			if(SimulationMode.equals("TEST_DATA_SIMULATION")){
				TradeBoardSimulator = new TradeBoardSimulator(	target,BoardInfo,
																this.ErrorLog,
																SimulationMode,
																LogPath,
																Speed);
				TradeBoardSimulator_catchException = new CatchException();
				TradeBoardSimulator.setName("Thread-TradeBoardSimulator-"+target);
				TradeBoardSimulator.setUncaughtExceptionHandler(TradeBoardSimulator_catchException);
				TradeBoardSimulator.start();
				
			}else{
				TradePageManager = new TradePageManagerUnit_Monex(	target, 
																	BoardInfo,
																	UserProperty,
																	this.ErrorLog,
																	SimulationMode,
																	LogPath);
				TradePageManager_catchException = new CatchException();
				TradePageManager.setName("Thread-TradePageManager-"+target);
				TradePageManager.setUncaughtExceptionHandler(TradePageManager_catchException);
				
				TradePageUpdateTask= new TimeStar();
				TradePageUpdateTimer= new java.util.Timer(true);
				TradePageUpdateTimer.schedule(TradePageUpdateTask,0,1000);
				
				while(!TradePageManager.TradePageManagerUnitState.equals("READY")){ //Wait for being triggered 
					try{
						Thread.sleep(100);
					}catch (InterruptedException e){
					}
				}
				TradePageManager.start();  // start tradepage operation 
			}
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
		
		WebAccessUnitState = "READY";
		System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );
	}
	
	public void run(){
		String SubProcessName = "Main_Loop ";
		System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
		while(!WebAccessUnitState.equals("END")){
			switch(WebAccessUnitState){
			
			case "READY":
				//System.out.println( "WebAccessUnit READY");
				break;	
			case "START":
				if(SimulationMode.equals("TEST_DATA_SIMULATION")){
					TradeBoardSimulator.TradeBoardSimulatorState = "START";
				}else{
					TradePageManager.TradePageManagerUnitState = "START";	
				}
				System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				
				break;
			case "PAUSE":
				//System.out.println( "WebAccessUnit PAUSE");
				break;
			case "FINISHING":	
				//System.out.println( "WebAccessUnit FINISH");
				try{
				if(SimulationMode.equals("TEST_DATA_SIMULATION")){
					TradeBoardSimulator.TradeBoardSimulatorState = "FINISHING";	
					System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Finishing" );
					while(!TradeBoardSimulator.TradeBoardSimulatorState.equals("END")){
						try{
							Thread.sleep(10);
						}catch (InterruptedException e){
						}
					}
				}else{
					TradePageManager.TradePageManagerUnitState = "FINISHING";	
					System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Finishing" );
					TradePageUpdateTimer.cancel();
					TradePageUpdateTimer = null;
					while(!TradePageManager.TradePageManagerUnitState.equals("END")){
						try{
							Thread.sleep(10);
						}catch (InterruptedException e){
						}
					}
				}
				
				}catch(Exception e){
					System.out.println( e);
				}
				
				WebAccessUnitState="END";
				break;
				
			case "ERROR":	
				//System.out.println( "WebAccessUnit ERROR");
				break;
				
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
			}	
		}	
		System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End" );
		//start any web access process  
	}
	
	class TimeStar extends TimerTask {  //data update per second
	    public void run() {
	    	String SubProcessName = "TimeStar_Loop ";
			//Date Now = new Date();
			//SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss");
	    	//rightNow = Calendar.getInstance(); 
	    	//Now = rightNow.getTime(); 
	    	
	    	//TradePageManager.BoardInfo.time = D.format(Now);
	    
			switch(WebAccessUnitState){
			case "READY":
				break;	
			case "START":		
				System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Timestar_start");
				break;
			case "PAUSE":
				break;
			case "FINISHING":	
				break;
			case "ERROR":	
				break;		
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
			}
	    	//System.out.println(TradePageManager1.BoardInfo.time);
			//TradePageManager1.BoardInfo.Price = TradePageManager1.BoardInfo.TempPrice;
			//for (int i=0 ; i<19; i++){	
					//TradeLog.FileWrite(TradePageManager1.BoardInfo.Board[i][0] + " " +TradePageManager1.BoardInfo.Board[i][1]+ "	" +TradePageManager1.BoardInfo.Board[i][2]);
					//System.out.println(TradePageManager1.BoardInfo.Board[i][0] + "	"+TradePageManager1.BoardInfo.Board[i][1]+ "	" +TradePageManager1.BoardInfo.Board[i][2]);
			//}
	    }
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
