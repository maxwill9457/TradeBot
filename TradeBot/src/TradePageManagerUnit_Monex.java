import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.Keys;

import java.util.*;
import javax.swing.JFrame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.*;
import java.lang.Object;
import java.math.BigDecimal;

public class TradePageManagerUnit_Monex extends DefinedData{
	
	String ProcessName = "TradePageManagerUnit_Monex";
	String SimulationMode;
	String TradePageManagerUnitState;
	

	String ADDRESS = "https://www.monex.co.jp/Login/00000000/login/ipan_web/hyoji";
	String target;
	String target_num;
	
	FirefoxProfile profile_board = new FirefoxProfile(new File("D:\\invest\\project\\firefox_profile"));  
	FirefoxProfile profile_attribute = new FirefoxProfile(new File("D:\\invest\\project\\firefox_profile")); 
	//profile.setPreference("general.useragent.override", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
	//profile.setPreference("intl.accept_languages", "en-us, en");
	WebDriver driver_board 		= new FirefoxDriver(profile_board);
	WebDriver driver_attribute 	= new FirefoxDriver(profile_attribute);
	//WebDriver driver_modify	= new FirefoxDriver();
	//WebDriver driver_check	= new FirefoxDriver();
	//WebDriver driver_sell		= new FirefoxDriver();
		
	BoardInfo BoardInfo;
	BoardInfo TempBoardInfo; 
	
	UserProperty UserProperty;
	
	LogUnit ExtractedLog;  // Trade operation log
	LogUnit ErrorLog;
	
	//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
	Boolean BoardExtractFlag = false;
	Boolean AttributeExtractFlag = false;//20161008追加
	
	SimpleDateFormat D = new SimpleDateFormat("HH:mm:ss.SSS");
	SimpleDateFormat DD = new SimpleDateFormat("yyyy/MM/dd"); 	
	
	int BoardInfoDataNumber = 0;
	int AttributeDataNumber = 0;//20161008追加
	
	String BoardInfoWriteTemp;//20161008追加
	String AttributeWriteTemp;//20161008追加
	
	BoardInformationExtraction BoardInfoExtractor;
	CatchException BoardInfoExtractor_catchException;
	
	BoardAttributeExtraction BoardAttributeExtractor; //20161008追加
	CatchException BoardAttributeExtractor_catchException;//20161008追加
	//------------------------------------------------------------------
	ShowMeigaraTable ShowMeigaraTable;
		
	TradePageManagerUnit_Monex(String target,String target_num,BoardInfo BoardInfo,UserProperty UserProperty,LogUnit ErrorLog,String SimulationMode,String LogPath){  // initialization
		
		String SubProcessName 		= "Initiation";
		TradePageManagerUnitState 	= "PREPARE";
		System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
		
		this.SimulationMode = SimulationMode;
		this.target 	= target;
		this.target_num = target_num;
		this.BoardInfo 	= BoardInfo;
		this.UserProperty = UserProperty;
		this.ErrorLog 	= ErrorLog;
		try{
			TempBoardInfo = new BoardInfo();
			ExtractedLog = new LogUnit(LogPath+"extract//",this.target+"_extracted",1); // create log file
			
			MarketBoardOpen(target_num);
			BoardInfoExtractor = new BoardInformationExtraction();
			BoardInfoExtractor_catchException = new CatchException();
			BoardInfoExtractor.setName("Thread-BoardInfoExtractor-"+target);
			BoardInfoExtractor.setUncaughtExceptionHandler(BoardInfoExtractor_catchException);
		
			//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
			BoardAttributeExtractor = new BoardAttributeExtraction();
			BoardAttributeExtractor_catchException = new CatchException();
			BoardAttributeExtractor.setName("Thread-BoardAttributeExtractor-"+target);
			BoardAttributeExtractor.setUncaughtExceptionHandler(BoardAttributeExtractor_catchException);
			//---------------------------------------------------------------
			
			ShowMeigaraTable = new ShowMeigaraTable(target,BoardInfo.Board,
													BoardInfo.time,
													BoardInfo.Date,
													BoardInfo.Market,
													BoardInfo.MarketNetChange,
													BoardInfo.Price,
													BoardInfo.NetChangePercent,
													BoardInfo.BoardInfoLock,
													BoardInfo.Dekitaka,
													BoardInfo.VWAP,
													this.ErrorLog );
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
		
		TradePageManagerUnitState = "READY";
		BoardInfoExtractor.BoardInformationExtractorState = "READY";
		
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );
	}

	public void run() {// TradePageManager 
			
		String SubProcessName = "Main_Loop ";
		System.out.println(  target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
		String PreState = TradePageManagerUnitState;
		
		while(!TradePageManagerUnitState.equals("END")){ //trigger of Data collection
			
			switch(TradePageManagerUnitState){
			case "READY":
				try{
					Thread.sleep(10);
				}catch (InterruptedException e){
				}
				PreState = "READY";
				break;
				
			case "START":
				if (PreState.equals("READY")){
					
					BoardInfoExtractor.start();
					BoardAttributeExtractor.start();
					LogTitleInitial();
					PreState = TradePageManagerUnitState;
					System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				}				
				
				TempBoardInfo.Board_flag = true;
				BoardExtractFlag = true; //スレッド分割のため追加
				AttributeExtractFlag = true; //スレッド分割のため追加
				
				//while(TempBoardInfo.Board_flag == true ){ //BoardInfoExtractor　BoardAttributeExtractor情報更新中は待機
				while(BoardExtractFlag == true || AttributeExtractFlag == true){
				try{
						Thread.sleep(5);
						//System.out.println("情報取得待機"+target);
					}catch (InterruptedException e){
					}	
				}
				synchronized (BoardInfo.BoardInfoLock){
					synchronized (TempBoardInfo.BoardInfoLock){
						System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"BoardRenew Start");
						Date Now = new Date();
						System.out.println("ボード更新開始"+D.format(Now));
						
						ExtractedLog.FileWrite( BoardInfoWriteTemp +"	"+AttributeWriteTemp+"\r\n"); //スレッド分割のため追加　スレッド中の書き込みを削除
						
						BoardInfo.DataNumber =TempBoardInfo.DataNumber;
						BoardInfo.Price = TempBoardInfo.Price.replaceAll(",", "");
						BoardInfo.Market = TempBoardInfo.Market.replaceAll(",", "");
						
						try{
							if(BoardInfo.DataNumber == 1){
								PriceRangeReference();
								BoardInfo.MarketOpen = TempBoardInfo.Market.replaceAll(",", "");
								BoardInfo.PriceOpen = TempBoardInfo.Price.replaceAll(",", "");
							}

							BigDecimal tempMarket 		= new BigDecimal( TempBoardInfo.Market.replaceAll(",", ""));
							BigDecimal tempMarketOpen 	= new BigDecimal( BoardInfo.MarketOpen.replaceAll(",", ""));
							BoardInfo.MarketNetChange 	= tempMarket.subtract(tempMarketOpen).toString();
		
							BigDecimal tempPrice 		= new BigDecimal(TempBoardInfo.Price.replaceAll(",", ""));
							BigDecimal tempPriceOpen 	= new BigDecimal(BoardInfo.PriceOpen.replaceAll(",", ""));
							BigDecimal tNetChange 		= tempPrice.subtract(tempPriceOpen);
							BoardInfo.NetChange 		= tNetChange.toString();
							BoardInfo.NetChangePercent 	= tNetChange.divide(tempPriceOpen,2, BigDecimal.ROUND_HALF_UP).toString();
						
							BoardInfo.SellIndex = TempBoardInfo.SellIndex;
							BoardInfo.BuyIndex 	= TempBoardInfo.BuyIndex;
							BoardInfo.Date 		= TempBoardInfo.Date;
							BoardInfo.BoardTime = TempBoardInfo.BoardTime;
							BoardInfo.AttributeTime = TempBoardInfo.AttributeTime;//スレッド分割のため追加 
							BoardInfo.Dekitaka	= TempBoardInfo.Dekitaka.replaceAll(",", "");
							BoardInfo.VWAP		= TempBoardInfo.VWAP.replaceAll(",", "");
				
							for (int x=0 ; x<23; x++){ // update Trade Board
								for (int y=0 ; y<3; y++){
									BoardInfo.Board[x][y] = TempBoardInfo.Board[x][y].replace(",", "");
								}
							}
						}catch (Exception e){
							System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"BoardIndoの更新に失敗"+"_"+ e.toString());
							ErrorLogWrite(ProcessName,SubProcessName, "InfoRenew error"+"	"+ e.toString() );
						}
						try{
						
							ShowMeigaraTable.BoardRenew(	BoardInfo.Board,BoardInfo.BoardTime,TempBoardInfo.Date,
															BoardInfo.Market,BoardInfo.MarketNetChange,
															BoardInfo.Price,BoardInfo.NetChange,BoardInfo.BoardInfoLock,
															BoardInfo.Dekitaka,BoardInfo.VWAP);
						//System.out.println("情報取得待機"+target);
						
						}catch (Exception e){
							System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"	"+"BoardRenew Start");
							ErrorLogWrite(ProcessName,SubProcessName, "BoardRenew error"+"	"+ e.toString() );
						}	
						Date Now1 = new Date();
						System.out.println("ボード更新終了"+D.format(Now1));
					}
				}
				break;
			case "PAUSE":
				break;
			case "FINISHING":
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Finishing" );
				
				while(!BoardInfoExtractor.BoardInformationExtractorState.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}
				driver_board.quit();
				driver_attribute.quit();
				ShowMeigaraTable.dispose();
				ShowMeigaraTable = null;
				TradePageManagerUnitState="END";
				break;
			
			case "ERROR":
				break;		
			}	
			try{
				Thread.sleep(1);
			}catch (InterruptedException e){
				e.printStackTrace();
				System.out.println("TradeBoardSimulation FileDataBuffer close error" );
			}	
		}
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+"End");
	}
		
	
	void MarketBoardOpen(String target_num){
	//---------------------信用買画面------------------------------------------------------
		driver_board.get(ADDRESS);
		Login(driver_board, UserProperty.USER_NAME,UserProperty.PASSWORD);
		driver_attribute.get(ADDRESS);//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
		Login(driver_attribute, UserProperty.USER_NAME, UserProperty.PASSWORD);//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
		//---------------------turn to stock page----------------------
		driver_board.findElement(By.linkText("マーケットボード")).click();
		driver_attribute.findElement(By.linkText("マーケットボード")).click();//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
		
		try{
			Thread.sleep(3000);
		}catch (InterruptedException e){
		}	
		
		driver_board.findElement(By.xpath("//div[@id='xb-matrix']/div/div/div[4]/input")).sendKeys(target_num);
		driver_attribute.findElement(By.xpath("//div[@id='xb-matrix']/div/div/div[4]/input")).sendKeys(target_num);//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
		
		try{
			Thread.sleep(1000);
		}catch (InterruptedException e){
		}	
		
		driver_board.findElement(By.cssSelector("li.ng-scope.selected")).click();
		driver_attribute.findElement(By.cssSelector("li.ng-scope.selected")).click();//20161008追加　出来高取得とスレッド二本化による情報取得高速化---------------------
		
	
		//----------------------------------------------------------------------------------
	}	
	
	void Login(WebDriver driver,String user_name, String password ) {	
		//---------------------Login ------------------------------------------------------
			driver.findElement(By.name("loginid")).sendKeys(user_name);
			driver.findElement(By.name("passwd")).sendKeys(password);
			driver.findElement(By.className("text-button")).click();
		//---------------------------------------------------------------------------------
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
	public class BoardInformationExtraction extends Thread{ // 定期截取情報
		
		String BoardInformationExtractorState; //1.standby 2.run 3.Finish
		String lable_head = "//div[@id='xb-matrix']/div[2]/div";
		
		String SubProcessName = "BoardInformationExtraction";
		
	    SimpleDateFormat D = new SimpleDateFormat("HH:mm:ss.SSS");
	    SimpleDateFormat DD = new SimpleDateFormat("yyyy/MM/dd");
	    
		public void run(){//BoardInformationExtraction
					
			BoardInformationExtractorState = "START";
			System.out.println(target+" basicタイム");
		
			int j = 0;
			String tString;
			String[] tString_1;
			String color;
			try{
			//------------------------FINISHまでループする----------------------------------------
				while(!TradePageManagerUnitState.equals("FINISHING")){
					while(BoardExtractFlag == false){
						try{
							Thread.sleep(1);
						}catch (InterruptedException e){
						}
					}
					Date Now = new Date();
					
					TempBoardInfo.BoardTime = D.format(Now);
					TempBoardInfo.Date = DD.format(Now);
					
					//ExtractedLog.FileWrite(TempBoardInfo.BoardTime+"\r\n");
					//System.out.println( target+ "	BoardInfoExtracted	start	"+(BoardInfoDataNumber+1)+"	" + TempBoardInfo.BoardTime );
					
					List<WebElement> childs = driver_board.findElements(By.xpath("//div[@id='xb-matrix']/div[2]/div"));
					//String a = driver_board.findElement(By.xpath("//div[@id='xb-matrix']/div[2]")).getText();; //全情報抽出
					//System.out.println(a);
									
					TempBoardInfo.BuyIndex=0;
					TempBoardInfo.SellIndex=0;
					j = 0;
					BoardInfoWriteTemp = "";
					//------------------------気配板から情報更新----------------------------------------
					synchronized (TempBoardInfo.BoardInfoLock){
						for (WebElement e  : childs)
						{
					
							if(j ==0){  //成行注文
								tString =e.getText();
								String temp = tString.replace("\n", "\\n");
								TempBoardInfo.DataNumber++;
								BoardInfoDataNumber++;
								BoardInfoWriteTemp = temp;
							
								tString_1 = tString.split("\n");
								if(tString_1.length ==1){// 時になし
									TempBoardInfo.Board[0][0] = "0";
									TempBoardInfo.Board[0][1] = tString_1[0];
									TempBoardInfo.Board[0][2] = "0";
								}
								else if(tString_1.length ==2){//買側注文あり
									if(tString_1[0].equals("成行注文")){
										TempBoardInfo.Board[0][0] = "0";
										TempBoardInfo.Board[0][1] = tString_1[0];
										TempBoardInfo.Board[0][2] = tString_1[1];
									}
									else if (tString_1[1].equals("成行注文")){//売側注文あり
										TempBoardInfo.Board[0][0] = tString_1[0];
										TempBoardInfo.Board[0][1] = tString_1[1];
										TempBoardInfo.Board[0][2] = "0";
									}
								}
								else if(tString_1.length ==3){//両方注文あり
									TempBoardInfo.Board[0][0] = tString_1[0];
									TempBoardInfo.Board[0][1] = tString_1[1];
									TempBoardInfo.Board[0][2] = tString_1[2];
								}
							}
							else if (j==2){
								tString =e.getText();
								String temp = tString.replace("\n", "\\n");
								BoardInfoWriteTemp = BoardInfoWriteTemp+"	"+temp;
								tString_1 = tString.split("\n");
								if(tString_1.length ==1){// 時になし OVERのみ
									TempBoardInfo.Board[1][0] = "0";
									TempBoardInfo.Board[1][1] = tString_1[0];
									TempBoardInfo.Board[1][2] = "0";
								}
								else if(tString_1.length ==2){//OVER買あり
									if(tString_1[0].equals("OVER")){
										TempBoardInfo.Board[1][0] = "0";
										TempBoardInfo.Board[1][1] = tString_1[0];
										TempBoardInfo.Board[1][2] = tString_1[1];
									}
									else if (tString_1[1].equals("OVER")){//OVER売あり
										TempBoardInfo.Board[1][0] = tString_1[0];
										TempBoardInfo.Board[1][1] = tString_1[1];
										TempBoardInfo.Board[1][2] = "0";
									}
								}
								else if(tString_1.length ==3){//両方注文あり？？　たぶん発生しない
									TempBoardInfo.Board[1][0] = tString_1[0];
									TempBoardInfo.Board[1][1] = tString_1[1];
									TempBoardInfo.Board[1][2] = tString_1[2];
								}
							}
							else if (j>2){	
								tString =e.getText();
								String temp = tString.replace("\n", "\\n");
								BoardInfoWriteTemp = BoardInfoWriteTemp+"	"+temp;
								tString_1 = tString.split("\n");
						
								if(TempBoardInfo.BuyIndex==0 && j<13 ){
									if(tString_1.length ==3){ //売あり　点が付いている
										if(tString_1[1].equals("・")||tString_1[1].equals("前")||tString_1[1].equals("特")){
											TempBoardInfo.Board[j-1][0] = tString_1[0];
											TempBoardInfo.Board[j-1][1] = tString_1[2];
											TempBoardInfo.Board[j-1][2] = "0";
										}
										else if(tString_1[2].equals("・")||tString_1[2].equals("前")||tString_1[2].equals("特")){
											TempBoardInfo.Board[j-1][0] = tString_1[0];
											TempBoardInfo.Board[j-1][1] = tString_1[1];
											TempBoardInfo.Board[j-1][2] = "0";
										}
									
										TempBoardInfo.BuyIndex = j-1;
									}
									else if(tString_1.length ==5) {//寄せ
										TempBoardInfo.Board[j-1][0] = tString_1[0];
										TempBoardInfo.Board[j-1][1] = tString_1[2];
										TempBoardInfo.Board[j-1][2] = tString_1[3];
										TempBoardInfo.BuyIndex = j-1;
										TempBoardInfo.SellIndex = j-1;
									}
									else if(tString_1.length ==4) {//開始前　売買あり
										if(tString_1[1].equals("前")||tString_1[1].equals("特")){
											TempBoardInfo.Board[j-1][0] = tString_1[0];
											TempBoardInfo.Board[j-1][1] = tString_1[2];
											TempBoardInfo.Board[j-1][2] = tString_1[3];
										}
										else if (tString_1[2].equals("前")||tString_1[2].equals("特")){
											TempBoardInfo.Board[j-1][0] = tString_1[0];
											TempBoardInfo.Board[j-1][1] = tString_1[1];
											TempBoardInfo.Board[j-1][2] = tString_1[3];
										}
										TempBoardInfo.BuyIndex = j-1;
										
									}
									else if(tString_1.length ==2){//売あり　
										TempBoardInfo.Board[j-1][0] = tString_1[0];
										TempBoardInfo.Board[j-1][1] = tString_1[1];
										TempBoardInfo.Board[j-1][2] = "0";
									}
									else{ //漲停
										TempBoardInfo.Board[j-1][0] = "0";
										TempBoardInfo.Board[j-1][1] = "0";
										TempBoardInfo.Board[j-1][2] = "0";
									}		
								}
								else{
									if(tString_1.length ==3){//買あり　点が付いている
										TempBoardInfo.SellIndex = j-1;
										TempBoardInfo.Board[j-1][0] = "0";
										TempBoardInfo.Board[j-1][1] = tString_1[0];
										TempBoardInfo.Board[j-1][2] = tString_1[1];
									//}	
									}
									else if (tString_1.length == 4){//開始前　売買あり
										TempBoardInfo.SellIndex = j-1;
										TempBoardInfo.Board[j-1][0] = "0";
										TempBoardInfo.Board[j-1][1] = tString_1[1];
										TempBoardInfo.Board[j-1][2] = tString_1[3];
									//}	
									}
									//if(j==19){
									//	TempBoardInfo.Board[j-2][0] = "0";
									//	TempBoardInfo.Board[j-2][1] = tString_1[1];
									//	TempBoardInfo.Board[j-2][2] = tString_1[2];
									//}
									else if(tString_1.length ==2){//買あり　
										TempBoardInfo.Board[j-1][0] = "0";
										TempBoardInfo.Board[j-1][1] = tString_1[0];
										TempBoardInfo.Board[j-1][2] = tString_1[1];
									}	
									else {//跌停
										TempBoardInfo.Board[j-1][0] = "0";
										TempBoardInfo.Board[j-1][1] = "0";
										TempBoardInfo.Board[j-1][2] = "0";
									}	
								}
							//System.out.println(BoardInfo.TempBoard[j-2][0] + "	"+BoardInfo.TempBoard[j-2][1]+ "	" +BoardInfo.TempBoard[j-2][2]);
							}
							j++;
						}
						//20161008 出来高取得+スレッド分割
						//------------------------時価情報更新----------------------------------------
						//TempBoardInfo.Price = driver_board.findElement(By.xpath("//div[@id='xb-matrix']/div/div[3]/div/span[1]")).getText();//20161008 処理をBoardAttributeextrationに移行
						//WriteTemp = WriteTemp+"	"+TempBoardInfo.Price;//20161008 処理をBoardAttributeextrationに移行

						//------------------------日経平均情報更新----------------------------------------
						//TempBoardInfo.Market = driver_board.findElement(By.xpath("//div[@id='index-ticker']/div[2]/div/ul/li/span[1]")).getText();//20161008 処理をBoardAttributeextrationに移行
						//WriteTemp = WriteTemp+"	"+TempBoardInfo.Market;//20161008 処理をBoardAttributeextrationに移行

						//------------------------更新完了時の----------------------------------------
						//Date Now = new Date();
						//SimpleDateFormat DD = new SimpleDateFormat("yyyy/MM/dd");
						TempBoardInfo.BoardTime = D.format(Now);
						TempBoardInfo.Date = DD.format(Now);
						BoardExtractFlag=false;
					
						BoardInfoWriteTemp = TempBoardInfo.DataNumber+"	"+TempBoardInfo.Date+"	"+ TempBoardInfo.BoardTime+"	"+BoardInfoWriteTemp;
						//ExtractedLog.FileWrite(BoardInfoWriteTemp+"\r\n"); //スレッド分割のため　ここで書き込まない
						//ExtractedLog.FileWrite(TempBoardInfo.BoardTime+"\r\n");
						System.out.println( target+ "	BoardInfoExtracted	"+BoardInfoDataNumber+"	" + TempBoardInfo.BoardTime );
						Now = null; 
						childs = null;
					}
				}
				//}catch (VirtualMachineError e){
				}catch (NoSuchElementException  e){
					System.out.println(e+"tradeboard child");
					ErrorLogWrite(ProcessName,SubProcessName, "extraction error"+"	"+ e.toString() );
				}
			
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"BoardInformationExtractor End ");
			BoardInformationExtractorState = "END";	
		}	
	}
	
public class BoardAttributeExtraction extends Thread{ // 定期截取情報
		
		String BoardAttributeExtractorState; //1.standby 2.run 3.Finish
		String lable_head = "//div[@id='xb-matrix']/div[2]/div";
		
		String SubProcessName = "BoardAttributeExtraction";
		
	    SimpleDateFormat D = new SimpleDateFormat("HH:mm:ss.SSS");
	    SimpleDateFormat DD = new SimpleDateFormat("yyyy/MM/dd"); 		
		public void run(){//BoardInformationExtraction
					
			BoardAttributeExtractorState = "START";
	
			System.out.println(target+" basicタイム");
		
			int j = 0;
			String tString;
			String[] tString_1;
			String color;
			try{
			//------------------------FINISHまでループする----------------------------------------
				while(!TradePageManagerUnitState.equals("FINISHING")){
					while(AttributeExtractFlag == false){
						try{
							Thread.sleep(1);
						}catch (InterruptedException e){
						}
					}
					TempBoardInfo.BuyIndex=0;
					TempBoardInfo.SellIndex=0;
					j = 0;
					AttributeWriteTemp = "";
					//------------------------気配板から情報更新----------------------------------------
					
					AttributeDataNumber++;
					//------------------------時価情報更新----------------------------------------
					TempBoardInfo.Price = driver_attribute.findElement(By.xpath("//div[@id='xb-matrix']/div/div[3]/div/span[1]")).getText();
					AttributeWriteTemp = AttributeWriteTemp+"	"+TempBoardInfo.Price;
						
					//------------------------日経平均情報更新----------------------------------------
					TempBoardInfo.Market = driver_attribute.findElement(By.xpath("//*[@id='index-ticker']/div[2]/div/ul/li[1]/p/span[1]")).getText();
					AttributeWriteTemp = AttributeWriteTemp+"	"+TempBoardInfo.Market;
					
					//------------------------2016出来高情報更新----------------------------------------
					TempBoardInfo.Dekitaka = driver_attribute.findElement(By.xpath("//div[@id='xb-matrix']/div/div[3]/div[3]/span[1]")).getText();
					AttributeWriteTemp = AttributeWriteTemp+"	"+TempBoardInfo.Dekitaka;
					
					//------------------------2016出来高情報更新----------------------------------------
					TempBoardInfo.VWAP = driver_attribute.findElement(By.xpath("//div[@id='xb-matrix']/div/div[3]/div[5]/div/span[1]")).getText();
					AttributeWriteTemp = AttributeWriteTemp+"	"+TempBoardInfo.VWAP;
						
					System.out.println( target+ "	VWAP	"+ TempBoardInfo.VWAP);
						
						//------------------------更新完了時の----------------------------------------
					Date Now = new Date();
					TempBoardInfo.AttributeTime = D.format(Now);
					TempBoardInfo.Date = DD.format(Now);
					AttributeExtractFlag=false;
					
					AttributeWriteTemp = TempBoardInfo.DataNumber+"	"+TempBoardInfo.Date+"	"+ TempBoardInfo.AttributeTime+"	"+AttributeWriteTemp;
					//ExtractedLog.FileWrite(AttributeWriteTemp+"\r\n");//スレッド分割のため　ここで書き込まない
					//ExtractedLog.FileWrite(TempBoardInfo.BoardTime+"\r\n");
					System.out.println( target+ "	AttributeExtracted	"+ AttributeDataNumber+"	" + TempBoardInfo.AttributeTime );
					Now = null; 
				}
				//}catch (VirtualMachineError e){
				}catch (NoSuchElementException  e){
					System.out.println(e+"tradeboard child");
					ErrorLogWrite(ProcessName,SubProcessName, "extraction error"+"	"+ e.toString() );
				}
			
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"BoardInformationExtractor End ");
			BoardAttributeExtractorState = "END";		
		}	
	}
	
	void LogTitleInitial(){
		//Log Label 
		String temp; 
		temp =  "BoardInfoDataNumber	yyyy/MM/dd	HH:mm:ss.SSS	";
		for (int i=0 ; i<23; i++){
			temp = temp + "BoardRow["+i+"][]	";
		}
		temp = temp + "AttributeDataNumber	Date	AttributeTime	Price	Market	Dekitaka	VWAP	";
		temp = temp + "\r\n";
		ExtractedLog.FileWrite(temp);
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
