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
    String LogPath; 
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
						//System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"BoardRenew Start");
						
						BoardInfo.DataNumber =TempBoardInfo.DataNumber;
						BoardInfo.Price = TempBoardInfo.Price.replaceAll(",", "");
						BoardInfo.Market = TempBoardInfo.Market.replaceAll(",", "");					

						
						if(BoardInfo.DataNumber ==1){
							PriceRangeReference(); //現在株価の変動範囲
							BoardInfo.MarketOpen = TempBoardInfo.Market.replaceAll(",", "");
							BoardInfo.PriceOpen = TempBoardInfo.Price.replaceAll(",", "");
						}

						
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
								BoardInfo.Dekitaka	= TempBoardInfo.Dekitaka.replaceAll(",", "");
								BoardInfo.VWAP		= TempBoardInfo.VWAP.replaceAll(",", "");
								
								for (int x=0 ; x<23; x++){ // update Trade Board
									for (int y=0 ; y<3; y++){
										BoardInfo.Board[x][y] = TempBoardInfo.Board[x][y].replace(",", "");
									}
								}
								try{
									ShowMeigaraTable.BoardRenew(	BoardInfo.Board,TempBoardInfo.time,TempBoardInfo.Date,
												BoardInfo.Market,BoardInfo.MarketNetChange,
												BoardInfo.Price,BoardInfo.NetChange,BoardInfo.BoardInfoLock,
												BoardInfo.Dekitaka,BoardInfo.VWAP);
								}catch(NullPointerException e){
									System.out.println(e+ " REnew error");
									ErrorLogWrite(ProcessName,SubProcessName, "SimulationBoardRenew error"+"	"+ e.toString() );
								}
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
	void PriceRangeReference(){
		String tempPrice = BoardInfo.Price.replaceAll(",", "");
		double RefPrice =Double.parseDouble(tempPrice);
		if(1<=RefPrice && RefPrice<100){ BoardInfo.PriceRange = 30;}
		else if(100<=RefPrice && RefPrice<200){BoardInfo.PriceRange = 50.0;}
		else if(200<=RefPrice && RefPrice<500){BoardInfo.PriceRange = 80.0;}
		else if(500<=RefPrice && RefPrice<700){BoardInfo.PriceRange = 100.0;}
		else if(700<=RefPrice && RefPrice<1000){BoardInfo.PriceRange = 150.0;}
		else if(1000<=RefPrice && RefPrice<1500){BoardInfo.PriceRange = 300.0;}
		else if(1500<=RefPrice && RefPrice<2000){BoardInfo.PriceRange = 400.0;}
		else if(2000<=RefPrice && RefPrice<3000){BoardInfo.PriceRange = 500.0;}
		else if(3000<=RefPrice && RefPrice<5000){BoardInfo.PriceRange = 700.0;}
		else if(5000<=RefPrice && RefPrice<7000){BoardInfo.PriceRange = 1000.0;}
		else if(7000<=RefPrice && RefPrice<10000){BoardInfo.PriceRange = 1500.0;}
		else if(10000<=RefPrice && RefPrice<15000){BoardInfo.PriceRange = 3000.0;}
		else if(15000<=RefPrice && RefPrice<20000){BoardInfo.PriceRange = 4000.0;}
		else if(20000<=RefPrice && RefPrice<30000){BoardInfo.PriceRange = 5000.0;}
		else if(30000<=RefPrice && RefPrice<50000){BoardInfo.PriceRange = 7000.0;}
		else if(50000<=RefPrice && RefPrice<70000){BoardInfo.PriceRange = 10000.0;}
		else if(70000<=RefPrice && RefPrice<100000){BoardInfo.PriceRange = 15000.0;}
		else if(100000<=RefPrice && RefPrice<150000){BoardInfo.PriceRange = 30000.0;}	
		
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
	    	 FileDataBuffer = new BufferedReader(new FileReader(DataFileList[0])); //9用確認
	    	
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
					if (tString != null){
						//System.out.println(tString);
						tString_1 = tString.split("	");
					
						if (tString_1[0].equals("DataNumber")){
							tString = FileDataBuffer.readLine();
							tString_1 = tString.split("	");
						}
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
							if ( i>2 && tString_1[8+3*i].equals("0")&&!TempBoardInfo.Board[i-1][0].equals("0") ){
								TempBoardInfo.SellIndex = i-1;
							}
							if ( i>2 && i < 22 && !tString_1[10+3*i].equals("0")&&TempBoardInfo.Board[i-1][2].equals("0") ){
								TempBoardInfo.BuyIndex = i;
							}
							TempBoardInfo.Board[i][0] = tString_1[8+3*i];
							TempBoardInfo.Board[i][1] = tString_1[9+3*i];
							TempBoardInfo.Board[i][2] = tString_1[10+3*i];
						
						}
						TempBoardInfo.BoardTime = tString_1[79];
						TempBoardInfo.Dekitaka = tString_1[81];
						TempBoardInfo.VWAP = tString_1[82];
						TempBoardInfo.AttributeTime = tString_1[83];
					
						TempBoardInfo.Board_flag=false;
					}
				}catch(Exception e){
					System.out.println( e+"	BoardInfomationSimulation error");
					e.printStackTrace();
					ErrorLogWrite(ProcessName, SubProcessName , e.toString());
				}    
			}
			
		}
	}
}