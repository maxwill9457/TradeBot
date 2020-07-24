import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;


import java.util.Date;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class TradeUnit extends DefinedData  {
	
	String ProcessName = "TradUnit";
	String SimulationMode ;//ONLINE            		real schedule, real data,  and operation
							//OPERATION_SIMULATION  real schedule, real data, no actual operation execute 
							//OFFLINE_SIMULATUION   test schedule,　real data,  no actual operation execute 
							//TEST_DATA_SIMULATION  test schedule,　test data and no actual operation execute   


	String 	TradeUnitState; // Trade operation enable/disable
	
	String 	target; // Trade target
	String 	target_num; // Trade target
	
	DefinedData TradeData;
	WebAccessUnit 	WebAccess; // Trade operation associated with web access 
	CatchException 	WebAccess_catchException;
	
	DecisionMakingUnit	DecisionMaking;
	CatchException 		DecisionMaking_catchException;
	
	TradeOperatorUnit 	TradeOperator; //交易股票的所有網業開啟 
	CatchException 		TradeOperator_catchException;
	
	BufferedReader FileDataBuffer; 
	
	WriteLog WriteLog; // 定期的にlogを記載するルーチン
	
	LogUnit TradeLog;  // Trade operation log
	LogUnit ErrorLog;  // Error時log
	
	java.util.Timer TradeUnitTimer;
	
	int StartTime 	= 00000;
	int EndTime		= 240000;
	
	int Speed;
	
	//int StartTime 	= 0;
	//int EndTime		= 30000;
	
	TradeUnit(String target,String target_num,String SimulationMode,int Speed, String LogPath,String Infofile){
		
		String SubProcessName = "Initiation";
		TradeUnitState = "PREPARE";
		this.target = target; 
		this.target_num = target_num; 
		this.Speed = Speed;
		this.SimulationMode = SimulationMode;
			
		System.out.println( target+ "	"+target_num+"	"+ProcessName+"_"+SubProcessName+"_"+this.SimulationMode+"_"+"Activating" );
			
		try{
			TradeData = new DefinedData();
			
			FileDataBuffer = new BufferedReader(new FileReader(Infofile)); //9用確認
			TradeData.UserProperty.USER_NAME	=	FileDataBuffer.readLine();
			TradeData.UserProperty.PASSWORD		=	FileDataBuffer.readLine();
		
			if (SimulationMode.equals("TEST_DATA_SIMULATION")||SimulationMode.equals("OFFLINE_SIMULATUION")){ //モードによりログの記録を変更
				//String Path = "D://invest//project//Trade_Bot_20160106//TradeBot//log//";
				ErrorLog = new LogUnit(LogPath, this.target+"Error_Simu",0); // create error log file	 date time errorpoint  content
				TradeLog = new LogUnit(LogPath+"daily//", this.target,1); // create log file		
				WriteLog= new WriteLog(); 
		
				TradeUnitTimer= new java.util.Timer(true);
				TradeUnitTimer.schedule(WriteLog,0,Speed);//シミュレーション速度にあわせてログ取得
			}else{
				//String Path = "D://invest//project//Trade_Bot_20160106//TradeBot//log//";
				ErrorLog = new LogUnit(LogPath, this.target+"Error",0); // create error log file	 date time errorpoint  content
				TradeLog = new LogUnit(LogPath+"daily//", this.target,1); // create log file
				WriteLog= new WriteLog(); 
		
				TradeUnitTimer= new java.util.Timer(true);
				TradeUnitTimer.schedule(WriteLog,0,500); //0.5秒おきにログ取得
			}
		
		
			TradeUnitSchedulor();
			// web系情報取得
			
			WebAccess = new WebAccessUnit(	this.target,
											this.target_num,
											TradeData.BoardInfo,
											TradeData.UserProperty,
											ErrorLog,
											this.SimulationMode,
											LogPath,
											Speed); // create web access
			
			
			WebAccess_catchException = new CatchException();
			WebAccess.setName("Thread-WebAccess-"+target);
			WebAccess.setUncaughtExceptionHandler(WebAccess_catchException);
			WebAccess.start();
			
			//情報による行動算出
			DecisionMaking = new DecisionMakingUnit(	this.target,
														this.target_num,
														TradeData.BoardInfo,
														TradeData.UserProperty,
														TradeData.TradeStatics,
														ErrorLog,
														this.SimulationMode,
														LogPath,
														Speed); //create Decision Making
			DecisionMaking_catchException = new CatchException();
			DecisionMaking.setName("Thread-DecisionMaking-"+target);
			DecisionMaking.setUncaughtExceptionHandler(DecisionMaking_catchException);
			DecisionMaking.start();
			//売買操作用
			
			
			TradeOperator = new TradeOperatorUnit(	this.target,
													this.target_num,
													TradeData.BoardInfo,
													TradeData.UserProperty,
													TradeData.TradeStatics,
													ErrorLog,
													this.SimulationMode,
													LogPath,
													Speed); //create Decision Making
			TradeOperator_catchException = new CatchException();
			TradeOperator.setName("Thread-TradeOperator-"+target);
			TradeOperator.setUncaughtExceptionHandler(TradeOperator_catchException);
			TradeOperator.start();
			
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
		
		TradeUnitState = "READY";
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
	}

	public void run(){ // 交易核心   結合情報截取  交易邏輯  記錄 交易行動
		String SubProcessName = "Main_Loop ";
		String PreState = TradeUnitState;
		while(!TradeUnitState.equals("END")){
			
			//String Pre_ShijyouState = TradeData.ShijyouState;
			TradeUnitSchedulor(); //decide TradeUnit state 
			
			switch(TradeUnitState){
			
			case "READY":
				break;	
			case "START":	
				if (PreState.equals("READY")){
					PreState = TradeUnitState;
					LogTitleInitial();
					WebAccess.WebAccessUnitState = "START";	
					//DecisionMaking.DecisionMakingUnitState = "START";
					TradeOperator.TradeOperatorUnitState = "START";
					
					//System.out.println( target+ "	"+ProcessName+"_"+SimulationMode+"_"+"Start" );
				}
				
				//if(Pre_ShijyouState.equals("STANDBY")&&TradeData.ShijyouState.equals("PREPARE")){ //開始のためファイルを作成する
				//}
				//System.out.println( target+ "	"+ProcessName+"_"+SimulationMode+"_"+"Start"  );
				break;
			case "PAUSE":
				break;
			case "FINISHING":
				try{
					System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Finishing" );
					//---------------気配板プロセスの完了待つ-----------------------------
					WebAccess.WebAccessUnitState = "FINISHING";
					while(!WebAccess.WebAccessUnitState.equals("END")){
						try{
							Thread.sleep(100);
						}catch (InterruptedException e){
						}
					}
					DecisionMaking.DecisionMakingUnitState = "FINISHING";
					while(!DecisionMaking.DecisionMakingUnitState.equals("END")){
						try{
							Thread.sleep(100);
						}catch (InterruptedException e){
						}
					}
					TradeOperator.TradeOperatorUnitState = "FINISHING";
					while(!TradeOperator.TradeOperatorUnitState.equals("END")){
						try{
							Thread.sleep(100);
						}catch (InterruptedException e){
						}
					}
					TradeUnitTimer.cancel();
					TradeUnitTimer = null;
				}catch(Exception e){
					System.out.println( e);
					ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
				}
				TradeUnitState = "END";
				break;
			case "ERROR":	
				break;	
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
			}	
		}
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End"  );	
	}
	
	void TradeUnitSchedulor(){
		Calendar rightNow;
		String SubProcessName = "TradeUnitSchedulor";
		rightNow = Calendar.getInstance();
		int tHour = rightNow.get(rightNow.HOUR_OF_DAY); // get hour
		int tMinute 	= rightNow.get(rightNow.MINUTE);
		int tSecond 	= rightNow.get(rightNow.SECOND);
		
		int indextime = tHour*10000+tMinute*100+tSecond;
		if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){
			if(indextime<StartTime){ //standby　8時前
				//TradeUnitState = "READY";
				TradeData.BoardInfo.MarketStatus ="STANDBY";
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Ready");
			}
			else if(indextime >= StartTime && indextime<=90000){ // 指定時間から開始させるための一時設定
			//else if(indextime >= 80000 && indextime<=90000){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
				//TradeUnitState = "START";	
				TradeData.BoardInfo.MarketStatus ="PREPARE";
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Start");
			}
			else if(indextime >= 90000 && indextime<=113000){ // The first half 9:00 to 11:30 Record-Active Action-Active
				//TradeUnitState = "START";
				TradeData.BoardInfo.MarketStatus ="FIRST_HALF";
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Start");
			}
			else if(indextime >= 113000 && indextime<=120000){ // Break 11:30 to 12:00 Record-Active Action-Standy
				//TradeUnitState = "START";
				TradeData.BoardInfo.MarketStatus ="BREAK";
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Start");
			}
			else if(indextime >= 120000 && indextime<=123000){ // The second half preparing 12:00 to 12:30 Record-Active Action-Standy
				//TradeUnitState = "START";
				TradeData.BoardInfo.MarketStatus ="PREPARE";
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Start");
			}
			else if(indextime >= 123000 && indextime<=150000){ // The second half preparing 12:30 to 15:00 Record-Active Action-Active
				//TradeUnitState = "START";
				TradeData.BoardInfo.MarketStatus ="SECOND_HALF";
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Start");
			}			
			else if (indextime>150000){ // Trade Finished 15:00  Record-Standby Action-Standby
				//TradeUnitState = "FINISHING";
				TradeData.BoardInfo.MarketStatus ="TRADE_FINISHED";
			System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"TradeUnitAlarm_Finish");
			}
			else{
				//ErrorLogWrite(ProcessName,SubProcessName, "スケジュールに該当する時間帯がない" );
			}
		}
		else if(SimulationMode.equals("TEST_DATA_SIMULATION")||SimulationMode.equals("OFFLINE_SIMULATUION")){
			TradeData.BoardInfo.MarketStatus ="FIRST_HALF";//前半に設定し、ログ出力をさせる
		}
		//else if(SimulationMode.equals("OPERATION_SIMULATION")){// 未定
		//	TradeData.BoardInfo.MarketStatus ="FIRST_HALF";//前半に設定し、ログ出力をさせる
		//}
		
	}
	void ErrorLogWrite(String ProccessName, String SubProcessName , String Error){
		Calendar rightNow;
		Date Now = new Date();
    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS	");
    	rightNow = Calendar.getInstance();
		Now = rightNow.getTime();
		
		String temp =  D.format(Now) + "	" + ProccessName +"_"+SubProcessName+ "	" + SubProcessName + "	" +Error +"\r\n";
		ErrorLog.FileWrite(temp);
		
	}
	
	void LogTitleInitial(){
		//Log Label 
		String temp; 
		temp =  "DataNumber	yyyy/MM/dd	HH:mm:ss.SSS	";
		temp = temp + "Market	MarketNetChange	Price	NetChange	NetChangePercent	";
		
		for (int i=0 ; i<23; i++){
			temp = temp + "BoardRow["+i+"][]	";
		}
		temp = temp + "board	BoardTime";
		temp = temp + "Attribute	Dekitaka	VWAP	AttributeTime";
		
		temp = temp + "\r\n";
		TradeLog.FileWrite(temp);
		
	}
	
	class WriteLog extends TimerTask {  //output log per one second
	    public void run() {
	    	String SubProcessName = "TimeStart_Loop ";
	    	Calendar rightNow;
	    	Date Now = new Date();
	    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
	    	rightNow = Calendar.getInstance();
			Now = rightNow.getTime();
			
	    	switch(TradeUnitState){
			
			case "READY":	
				break;
				
			case "START":

				if(!TradeData.BoardInfo.MarketStatus.equals("STANDBY")&&!TradeData.BoardInfo.MarketStatus.equals("TRADE_FINISHED")){
					if(TradeData.BoardInfo.DataNumber != TradeData.BoardInfo.PreDataNumber){
						String temp;
						//System.out.println( "\n"+ target+"	BoardInfo" );
						synchronized (BoardInfo.BoardInfoLock){
							temp = TradeData.BoardInfo.DataNumber+"	"+D.format(Now)+"	";
							temp = temp +TradeData.BoardInfo.Market+"	"+TradeData.BoardInfo.MarketNetChange+"	"
									+TradeData.BoardInfo.Price+"	"+TradeData.BoardInfo.NetChange+"	"+TradeData.BoardInfo.NetChangePercent+"	";
							for (int i=0 ; i<23; i++){
								temp = temp + TradeData.BoardInfo.Board[i][0] + "	" +TradeData.BoardInfo.Board[i][1]+ "	" +TradeData.BoardInfo.Board[i][2]+ "	";
								//System.out.println(TradeData.BoardInfo.Board[i][0] + "	"+TradeData.BoardInfo.Board[i][1]+ "	" +TradeData.BoardInfo.Board[i][2]);
							}
							temp = temp + "	board	"+TradeData.BoardInfo.BoardTime;
							temp = temp +"	Attribute	"+ TradeData.BoardInfo.Dekitaka + "	"+ TradeData.BoardInfo.VWAP+"	"+TradeData.BoardInfo.AttributeTime;
							temp = temp + "\r\n";
						}
						TradeLog.FileWrite(temp);
						TradeData.BoardInfo.PreDataNumber = TradeData.BoardInfo.DataNumber;
						//BoardInfo.Locking = false;
					}
				}
				break;
			case "PAUSE":
				System.out.println( "WriteLog pause");
				break;
			case "FINISHING":	
				System.out.println( "WriteLog finishing");
				break;
			case "ERROR":	
				System.out.println( "WriteLog error");
				break;		
			} 	
	    }
	}
	
	
	//void TradeUnitEnd(){
		
	//}
}
