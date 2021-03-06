
import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Calendar;

// develope version 20161015 開発中

public class Main {
	
	static int StartTime = 0;
	static int EndTime   = 240000;
	static String MeigaraName 	= "ピクセラ";
	static String MeigaraNum 	= "6731";
	static String Infofile =  "D://invest//project//info.txt";
	static String LogPath = "D://invest//project//002.dev_Env//log";
	static String SimulationMode = "OPERATION_SIMULATION";  		
																//ONLINE            	real schedule, real data,  and operation
																//OPERATION_SIMULATION  real schedule, real data, no actual operation execute 
																//OFFLINE_SIMULATUION   test schedule,　real data,  no actual operation execute 
																//TEST_DATA_SIMULATION  test schedule,　test data and no actual operation execute   
	static int Speed = 10; //millisecond シミュレーション再生倍速
	
	static String TradeBotState ="STANDBY" ;
	
	public static void main(String[] args) {
		
		 Schedulor TradeBotSchedulor = new Schedulor();
		
		class CatchException implements UncaughtExceptionHandler {
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println(t.getName());
			}
		}
		TradeUnit TradeUnit1;
		if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){
			TradeUnit1 = new TradeUnit(MeigaraName,MeigaraNum,SimulationMode,Speed, LogPath+"dev//",Infofile) ; //啟動新的股票交易單元
		}
		else{
			//TradeUnit1 = new TradeUnit(MeigaraName,MeigaraNum,SimulationMode,Speed, LogPath+"test//",Infofile) ; //啟動新的股票交易單元
			TradeUnit1 = new TradeUnit(MeigaraName,MeigaraNum,SimulationMode,Speed, LogPath+"dev//",Infofile) ; //啟動新的股票交易單元
			
		}
		CatchException TradeUnit1_catchException = new CatchException();
		TradeUnit1.setName("Thread-TradeUnit1");
		TradeUnit1.setUncaughtExceptionHandler(TradeUnit1_catchException);
		
		while(!TradeUnit1.TradeUnitState.equals("READY")){
			try{
				Thread.sleep(1);
			}catch (InterruptedException e){
			}	
		}
		
		while(!TradeBotState.equals("END") ){ //Wait for being triggered 
			
			TradeBotSchedulor.TradeBotSchedulor();
			//System.out.println(TradeBotState);
			
			switch(TradeBotState){
			
			case "STANDBY":
				System.out.println("Tradebot_Standby");
				break;	
			case "START":
				if (TradeUnit1== null){

					if(SimulationMode.equals("ONLINE")){
						TradeUnit1 = new TradeUnit(MeigaraName,MeigaraNum,SimulationMode,Speed, LogPath,Infofile) ; //啟動新的股票交易單元
					}
					else{
						TradeUnit1 = new TradeUnit(MeigaraName,MeigaraNum,SimulationMode,Speed, LogPath+"Test//",Infofile) ; //啟動新的股票交易單元
					}
					TradeUnit1_catchException = new CatchException();
					TradeUnit1.setName("Thread-TradeUnit1");
					TradeUnit1.setUncaughtExceptionHandler(TradeUnit1_catchException);
					
					while(!TradeUnit1.TradeUnitState.equals("READY")){
						try{
							Thread.sleep(1);
						}catch (InterruptedException e){
						}	
					}
					System.out.println("Tradebot_New object");
				}
				else{
					System.out.println("Tradebot_exist object");
				}
				//System.out.println( "TradeUnit1 start" );
				TradeUnit1.start();	
				try{
					Thread.sleep(10);
				}catch (InterruptedException e){
				}	
				TradeUnit1.TradeUnitState = "START";
				TradeBotState = "RUN"; 
			
				break;
			case "RUN":
				//System.out.println("Tradebot_run");
				break;
			case "FINISH":
				TradeUnit1.TradeUnitState = "FINISHING";
				while(!TradeUnit1.TradeUnitState.equals("END")){
					System.out.println("Tradebot_finishing");
					try{
						Thread.sleep(100);
					}catch (InterruptedException e){
					}
				}
				TradeUnit1=null;
				TradeBotState = "STANDBY";
			
				System.out.println("Tradebot_finished");
			
				break;
			case "ERROR":	
				break;
				
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
			}	
		}
		System.out.println("Main END");
		
	}	
	static class Schedulor{
		void TradeBotSchedulor( ){
			Calendar rightNow;

			rightNow = Calendar.getInstance();
			int tHour 	= rightNow.get(rightNow.HOUR_OF_DAY); // get hour
			int tMinute = rightNow.get(rightNow.MINUTE);
			int tSecond = rightNow.get(rightNow.SECOND);
			int tDayofWeek = rightNow.get(rightNow.DAY_OF_WEEK);//2016/10/1更新  土日起動しないようにする
			
			int indextime = tHour*10000+tMinute*100+tSecond;
			if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){
				if(tDayofWeek != 1 && tDayofWeek != 7){ //2016/10/1更新  土日起動しないようにする
					if(indextime<StartTime){ //standby　8時前
						TradeBotState = "STANDBY";
					}
					else if(indextime >= StartTime && indextime<=EndTime && TradeBotState.equals("STANDBY")){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
						TradeBotState = "START";	
					}
					else if(indextime >= StartTime && indextime<=EndTime && TradeBotState.equals("RUN")){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
						TradeBotState = "RUN";	
					}
					else if (indextime>EndTime && TradeBotState.equals("RUN")){ // Trade Finished 15:00  Record-Standby Action-Standby
						TradeBotState = "FINISH";
					}
				}else{
					TradeBotState = "STANDBY";
				} 
			}
			else{
				if(indextime >= StartTime && indextime<=EndTime && TradeBotState.equals("STANDBY")){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
					TradeBotState = "START";	
				}
				else if(indextime >= StartTime && indextime<=EndTime && TradeBotState.equals("RUN")){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
					TradeBotState = "RUN";	
				}
				else if (indextime>EndTime && TradeBotState.equals("RUN")){ // Trade Finished 15:00  Record-Standby Action-Standby
					TradeBotState = "FINISH";
				}
				else{
					TradeBotState = "STANDBY";
				} 
			}
			
			//2016/10/1更新
			
			//else if(indextime >= StartTime+30 && indextime<=EndTime+30 && TradeBotState.equals("STANDBY")){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
			//	TradeBotState = "START";	
			//}
			//else if(indextime >= StartTime+30 && indextime<=EndTime+30 && TradeBotState.equals("RUN")){ // Trade preparing 8:00 to 9:00   Record-Active Action-Standy
			//	TradeBotState = "RUN";	
			//}
			//else if (indextime>EndTime+30 && TradeBotState.equals("RUN")){ // Trade Finished 15:00  Record-Standby Action-Standby
			//	TradeBotState = "FINISH";
			//}

		 }
		
	}
}



		

	