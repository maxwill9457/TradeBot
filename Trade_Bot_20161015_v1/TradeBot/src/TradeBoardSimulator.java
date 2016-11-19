import java.util.*;

import java.text.*;
import java.util.Date;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.File;
import java.io.*;
import java.lang.Object;

import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class TradeBoardSimulator extends DefinedData{// 意思決定   Trade情報により、買、維持、売の行動　また価格を決める

	String ProcessName = "TradeBoardSimulator";
	String SimulationMode;
	int Speed;
	String TradeBoardSimulatorState;
	String target;
	
	File DataFileSet = null; //simulation 用ファイルの集合
    File[] DataFileList; //simulation用ファイルリスト
    String LogPath = "D://invest//project//log//"; 
    BufferedReader FileDataBuffer; 
	BoardInfo BoardInfo;
	BoardInfo TempBoardInfo;
	//Object TempBoardInfoLock = new Object();
	
	LogUnit TradeOperateLog; // create statics log file
	LogUnit ErrorLog;
	
	BoardInfomationSimulator BoardInfomationSimulator;
	java.util.Timer TradeBoardSimulatorTimer;
	
	ShowMeigaraTable ShowMeigaraTable;
	
	TradeBoardSimulator(String target,BoardInfo BoardInfo,LogUnit ErrorLog,String SimulationMode, String LogPath,int Speed){
		
		String SubProcessName = "Initiation";
		TradeBoardSimulatorState = "PREPARE";
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
		
		
		this.target = target;
		this.BoardInfo = BoardInfo;
		this.ErrorLog = ErrorLog;
		this.SimulationMode = SimulationMode;
		this.LogPath = LogPath;
		this.Speed = Speed;
		try{
			TempBoardInfo = new BoardInfo();	
			DataFileLoader(); //ファイルを読み込み
		
			BoardInfomationSimulator= new BoardInfomationSimulator();  //実情報の読み込みを再現するTimer 
			TradeBoardSimulatorTimer= new java.util.Timer(true);
			TradeBoardSimulatorTimer.schedule(BoardInfomationSimulator,0,Speed);
		
			ShowMeigaraTable = new ShowMeigaraTable(target+"気配板",
													BoardInfo.Board,
													BoardInfo.time,
													BoardInfo.Date,
													BoardInfo.Market,
													BoardInfo.MarketNetChange,
													BoardInfo.Price,
													BoardInfo.NetChangePercent,
													BoardInfo.BoardInfoLock ,
													BoardInfo.Dekitaka,
													BoardInfo.VWAP,
													ErrorLog);
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
		
		TradeBoardSimulatorState = "READY";
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
	}
		
	public void run(){ 
		String SubProcessName = "Main_Loop ";
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
		String PreState = TradeBoardSimulatorState;
		while(!TradeBoardSimulatorState.equals("END")){
			switch(TradeBoardSimulatorState){
			
			case "READY":
				
				break;	
			case "START":	
				if (PreState.equals("READY")){
					//初回のプロセスの起動に使う
					PreState = TradeBoardSimulatorState;
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				}
				TempBoardInfo.Board_flag = true;
				while(TempBoardInfo.Board_flag == true ){
					try{
						Thread.sleep(5);
						//System.out.println("情報取得待機"+target);
						
					}catch (InterruptedException e){
					}	
				}
				synchronized (BoardInfo.BoardInfoLock){
					synchronized (TempBoardInfo.BoardInfoLock){
						System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"BoardRenew Start");
						try{
							//System.out.println(target+" trigger over");
							if (!TempBoardInfo.time.equals("null")){
								BoardInfo.Date = TempBoardInfo.Date;
								BoardInfo.time = TempBoardInfo.time;
								BoardInfo.Price = TempBoardInfo.Price;
								BoardInfo.NetChange = TempBoardInfo.NetChange;
								BoardInfo.NetChangePercent = TempBoardInfo.NetChangePercent;
								BoardInfo.Market = TempBoardInfo.Market;
								BoardInfo.MarketNetChange = TempBoardInfo.MarketNetChange;
								BoardInfo.SellIndex = TempBoardInfo.SellIndex;
								BoardInfo.BuyIndex = TempBoardInfo.BuyIndex;
								BoardInfo.BoardTime = TempBoardInfo.BoardTime;
				
								for (int x=0 ; x<23; x++){ // update Trade Board
									for (int y=0 ; y<3; y++){
										BoardInfo.Board[x][y] = TempBoardInfo.Board[x][y].replace(",", "");
									}
								}
						
								ShowMeigaraTable.BoardRenew(	BoardInfo.Board,TempBoardInfo.time,TempBoardInfo.Date,
												BoardInfo.Market,BoardInfo.MarketNetChange,
												BoardInfo.Price,BoardInfo.NetChange,BoardInfo.BoardInfoLock,
												BoardInfo.Dekitaka,BoardInfo.VWAP);
							}
						}catch(Exception e){
							System.out.println(e+ " REnew error");
							ErrorLogWrite(ProcessName,SubProcessName, "SimulationBoardRenew error"+"	"+ e.toString() );
						}
					}
				}
				
				
				break;
			case "PAUSE":
				//System.out.println( "TradeOperatorUnit PAUSE");
				break;
			case "FINISHING":
				try{
					FileDataBuffer.close();
				}
				catch(Exception e){
					System.out.println( "BoardInfomationSimulation error");
					
				}
				TradeBoardSimulatorTimer.cancel();
				TradeBoardSimulatorTimer = null;
				
				ShowMeigaraTable.removeAll();;
				ShowMeigaraTable = null;
				
				
				System.out.println("Taskが停止しました");
				//---------------気配板プロセスの完了待つ-----------------------------	
				/*while(!XXXX.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}*/
				//System.out.println( "TradeOperatorUnit FINISH");
				TradeBoardSimulatorState = "END";
				break;
			case "ERROR":	
				//System.out.println( "TradeOperatorUnit ERROR");
				break;
				
			}	
			try{
				Thread.sleep(Speed);
			}catch (InterruptedException e){
				e.printStackTrace();
				System.out.println("TradeBoardSimulation FileDataBuffer close error" );
			}	
		}		
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End" );
		//start any web access process 
	}
	void  DataFileLoader(){
		String SubProcessName = "DataFileLoader";	
	    try{
	    	DataFileSet = new File(LogPath+"daily//");
	    	DataFileList = DataFileSet.listFiles();
	    	
	    	 for (int i = 0; i < DataFileList.length; i++) {
	    	        File file = DataFileList[i];
	    	        System.out.println((i + 1) + ":    " + file);
	    	 }
	    	 FileDataBuffer = new BufferedReader(new FileReader(DataFileList[1])); //9用確認
	    	
	    }catch(Exception e){
	    	 System.out.println("DataFileLoader error" );
	    	 e.printStackTrace();
	    	 ErrorLogWrite(ProcessName, SubProcessName , e.toString());
	    }
	    System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"DataFileLoading_Finished" );
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

	class BoardInfomationSimulator extends TimerTask {  //output log per one second
	    public void run() {
	    	
	    	String tString;
			String[] tString_1;
			String SubProcessName = "BoardInfomationSimulator";	
			
			while(TempBoardInfo.Board_flag == false){
				try{
					Thread.sleep(1);
				}catch (InterruptedException e){
				}
			}
			synchronized (TempBoardInfo.BoardInfoLock){
				try{
					//System.out.println( "TEST");
					tString = FileDataBuffer.readLine();
					//System.out.println(tString);
					tString_1 = tString.split("	");
					//int temp = Integer.valueOf(tString_1[0]);
					TempBoardInfo.DataNumber++;
					TempBoardInfo.Date = tString_1[1];
					TempBoardInfo.time = tString_1[2];
					TempBoardInfo.Market = tString_1[3];
					TempBoardInfo.MarketNetChange = tString_1[4];
					TempBoardInfo.Price = tString_1[5];
					TempBoardInfo.NetChange = tString_1[6];
					TempBoardInfo.NetChangePercent = tString_1[7];
	    		 
					for (int i=0 ; i<23; i++){
						TempBoardInfo.Board[i][0] = tString_1[8+3*i];
						TempBoardInfo.Board[i][1] = tString_1[9+3*i];
						TempBoardInfo.Board[i][2] = tString_1[10+3*i];
					}
					TempBoardInfo.BoardTime = tString_1[79];
					TempBoardInfo.Dekitaka = tString_1[81];
					TempBoardInfo.VWAP = tString_1[82];
					TempBoardInfo.AttributeTime = tString_1[83];
					
					TempBoardInfo.Board_flag=false;
				
				}catch(Exception e){
					System.out.println( e+"	BoardInfomationSimulation error");
					e.printStackTrace();
					ErrorLogWrite(ProcessName, SubProcessName , e.toString());
				}    
			}
		}
	}
}