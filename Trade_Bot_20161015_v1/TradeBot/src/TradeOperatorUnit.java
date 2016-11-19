import java.util.*;
import java.io.File;
import java.text.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public class TradeOperatorUnit extends DefinedData{// 意思決定   Trade情報により、買、維持、売の行動　また価格を決める

	String ProcessName = "TradeOperatorUnit";
	String SimulationMode;
	String TradeOperatorUnitState;
	
	String TradeOrderAgentState;
	String TradeSellAgentState;
	String TradeMonitorAgentState;
	
	String target;
	

	String ADDRESS = "https://www.monex.co.jp/Login/00000000/login/ipan_web/hyoji";
	
	BoardInfo BoardInfo;
	UserProperty UserProperty;
	TradeStatics TradeStatics;
	
	FirefoxProfile profile_order 	= new FirefoxProfile(new File("D:\\temp"));  
	FirefoxProfile profile_sell 	= new FirefoxProfile(new File("D:\\temp")); 
	FirefoxProfile profile_monitor 	= new FirefoxProfile(new File("D:\\temp")); 
	
	WebDriver driver_order 			= new FirefoxDriver(profile_order);
	//WebDriver driver_sell 			= new FirefoxDriver(profile_sell);
	WebDriver driver_monitor 		= new FirefoxDriver(profile_monitor);
	
	OrderAgentUnit OrderAgentUnit;
	CatchException OrderAgentUnit_catchException;
	
	TradeMonitorUnit TradeMonitorUnit;
	CatchException TradeMonitorUnit_catchException;
	
	LogUnit TradeOperateLog; // create statics log file
	LogUnit  ErrorLog;
	
	Date Now = new Date();
	
	
	TradeOperatorUnit(String target,BoardInfo BoardInfo, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){

		String SubProcessName = "Initiation";
		TradeOperatorUnitState = "PREPARE";
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
		
		this.SimulationMode = SimulationMode;
		this.target = target;
		this.ErrorLog = ErrorLog;
		
		this.BoardInfo = BoardInfo;
		this.UserProperty = UserProperty;
		
		try{
			TradeOperateLog = new LogUnit(LogPath,this.target+"Trade",0); // create log file
			Now = new Date();
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
		//---------------モニターユニットを生成---------------------
		this.TradeMonitorUnit = new TradeMonitorUnit(target,driver_monitor,UserProperty,TradeStatics,ErrorLog,SimulationMode,LogPath,Speed);
		TradeMonitorUnit_catchException = new CatchException();
		TradeMonitorUnit.setName("Thread-BoardInfoExtractor-"+target);
		TradeMonitorUnit.setUncaughtExceptionHandler(TradeMonitorUnit_catchException);
		//---------------購入ユニットを生成---------------------
		this.OrderAgentUnit = new OrderAgentUnit(target,driver_order,UserProperty,TradeStatics,ErrorLog,SimulationMode,LogPath,Speed);
		OrderAgentUnit_catchException = new CatchException();
		OrderAgentUnit.setName("Thread-BoardInfoExtractor-"+target);
		OrderAgentUnit.setUncaughtExceptionHandler(OrderAgentUnit_catchException);
		//---------------売却ユニットを生成---------------------
		
		
		
		TradeOperatorUnitState = "READY";
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
	}
		
	public void run(){ 
		String SubProcessName = "Main_Loop ";
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
		String PreState = TradeOperatorUnitState;
		Calendar rightNow;
		
		while(!TradeOperatorUnitState.equals("END")){
			switch(TradeOperatorUnitState){
			
			case "READY":
				
				break;	
			case "START":	
				if (PreState.equals("READY")){
					//初回のプロセスの起動に使う
					PreState = TradeOperatorUnitState;
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				}	
				
				SimpleDateFormat D = new SimpleDateFormat("HH:mm:ss.SSS");
		    	rightNow = Calendar.getInstance();
				Now = rightNow.getTime();
				
				try{
					Thread.sleep(1000);
				}catch (InterruptedException e){
				}	
				
				if(UserProperty.UserAction.Action.equals("BUY")){// 購買行動
					synchronized (UserProperty.UserPropertyLock){
						UserProperty.cash = UserProperty.cash.subtract( UserProperty.UserAction.Price.multiply(UserProperty.UserAction.OrderStockNum));
						UserProperty.Holded = "HOLDED";
						UserProperty.UserAction.Action = "NONE";
						
						String temp  = BoardInfo.Date + "	" + D.format(Now) + "	BUY"+"	"+UserProperty.UserAction.Price+"	" + 
										UserProperty.cash+"	" + BoardInfo.MarketStatus+"	" + BoardInfo.StockStatus +"\r\n";	
						TradeOperateLog.FileWrite(temp);
						System.out.println("Buy Price="+UserProperty.UserAction.Price + " Cash = " +UserProperty.cash);
						
					}
				} 
				else if(UserProperty.UserAction.Action.equals("SELL")){// 購買行動
					synchronized (UserProperty.UserPropertyLock){
						UserProperty.cash = UserProperty.cash.add( UserProperty.UserAction.Price.multiply(UserProperty.UserAction.OrderStockNum));
						UserProperty.UserAction.Action = "NONE";
						UserProperty.Holded = "NONE";
						String temp  = BoardInfo.Date + "	" + D.format(Now) + "	SELL"+"	"+UserProperty.UserAction.Price+"	" + 
										UserProperty.cash +"	" + BoardInfo.MarketStatus+"	" + BoardInfo.StockStatus+"\r\n";						
						TradeOperateLog.FileWrite(temp);
						System.out.println("Sell Price="+UserProperty.UserAction.Price + " Cash = " +UserProperty.cash);
					}
				}
			
				
				System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				break;
			case "PAUSE":
				//System.out.println( "TradeOperatorUnit PAUSE");
				break;
			case "FINISHING":
				//---------------気配板プロセスの完了待つ-----------------------------	
				TradeOrderAgentState = "FINISHING";
				while(!TradeOrderAgentState.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}
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
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End" );
		//start any web access process 
	}
	

	public class OrderAgentUnit extends DefinedData{
		String SimulationMode ;
		String target;
	
		LogUnit TradeOrderAgentLog;  // Trade operation log
		LogUnit ErrorLog;
		UserProperty UserProperty;
		
		OrderAgentUnit(String target,WebDriver driver_order, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){

			String SubProcessName = "OrderAgent_Initiation";
			TradeOrderAgentState = "PREPARE";
			System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
			
			this.SimulationMode = SimulationMode;
			this.target = target;
			this.ErrorLog = ErrorLog;
			this.UserProperty = UserProperty;
			
			TradeOrderPageOpen(driver_order);
			
			try{
				TradeOrderAgentLog = new LogUnit(LogPath+"trade//",this.target+"TradeOrderAgent",0); // create log file
				Now = new Date();
			}catch(Exception e){
				System.out.println( e);
				ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
			}
			TradeOrderAgentState = "READY";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
		}
			
		public void run(){ 
			String SubProcessName = "OrderAgent_Main_Loop ";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
			String PreState = TradeOrderAgentState;
			Calendar rightNow;
			
			while(!TradeOrderAgentState.equals("END")){
				switch(TradeOrderAgentState){
				
				case "READY":
					
					break;	
				case "START":	
					if (PreState.equals("READY")){
						//初回のプロセスの起動に使う
						PreState = TradeOrderAgentState;
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
					}	
					
					SimpleDateFormat D = new SimpleDateFormat("HH:mm:ss.SSS");
			    	rightNow = Calendar.getInstance();
					Now = rightNow.getTime();
					
					try{
						Thread.sleep(1000);
					}catch (InterruptedException e){
					}	
					ChangeCheck();//TradeMonitoringUnitの状態確認
					ActionCheck(); //エージェントの状態を更新する
					PropertyCheck();//エージェントの実行行動を決定する
					//OrderActionExec();//情報
					



					System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
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
					TradeOrderAgentState = "END";
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
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End" );
			//start any web access process 
		}
		void TradeOrderPageOpen(WebDriver driver_order){
			String SubProcessName = "TradeOrder_PageOpen ";
			driver_order.get(ADDRESS);
			Login(driver_order, USER_NAME, PASSWORD);	
		}
		void ChangeCheck(){//TradeMonitoringUnitの状態確認
			String SubProcessName = "TradeOrder_ChangeCheck ";
		}
		void ActionCheck(){//エージェントの状態を更新する
			String SubProcessName = "TradeOrder_ActionCheck ";
		}
		
		void PropertyCheck(){//エージェントの実行行動を決定する
			String SubProcessName = "TradeOrder_PropertyCheck ";
		}
		

		void OrderActionExec(WebDriver driver_order, UserProperty UserProperty ) {	
			String SubProcessName = "TradeOrder_OrderActionExec ";
			//---------------------Login ------------------------------------------------------
			String GetURL = "https://its2.monex.co.jp/StockOrderManagement/Vcmx64FRRakr/kbodr/kai_odr/"
							+ "hyji?specifyMeig=1&meigCd=0039140000&sijo=1&nariSasiKbn=1&yukoSiteDate=1&suryo=100&kakaku=6630";
			driver_order.get(GetURL);
			//---------------------------------------------------------------------------------
		}
		
	}
	public class TradeMonitorUnit extends DefinedData{
		String SimulationMode ;
		String target;
		Random rnd ;
	
		LogUnit TradeMonitorAgentLog;  // TradeMonitor log
		LogUnit ErrorLog;
		
		String OrderState; // 待機（注文がないとき）、注文中、注文済、取消中、取消済、
		Boolean NewOrderEntry;
		Boolean NewSellEntry;
		Boolean Confirmed;
		
		BigDecimal preBuyTrend;
		BigDecimal preSellTrend;
		
		UserProperty UserProperty;
		TradeStatics DecisionTradeStaticsData;
		
		TradeMonitorUnit(String target,WebDriver driver_monitor, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){

			String SubProcessName = "TradeMonitoringUnit_Initiation";
			TradeMonitorAgentState = "PREPARE";
			System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
			
			this.SimulationMode = SimulationMode;
			this.target = target;
			this.ErrorLog = ErrorLog;
			this.UserProperty = UserProperty;
			this.DecisionTradeStaticsData = TradeStatics;
			
			this.NewSellEntry 	= false;
			this.NewOrderEntry 	= false;
			this.Confirmed		= false;
			this.OrderState ="待機";
			
			TradeMonitorPageOpen(driver_monitor);
			
			try{
				TradeMonitorAgentLog = new LogUnit(LogPath+"trade//",this.target+"TradeMonitorAgent",0); // create log file
				Now = new Date();
			}catch(Exception e){
				System.out.println( e);
				ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
			}
			TradeOrderAgentState = "READY";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
		}
			
		public void run(){ 
			String SubProcessName = "TradeMonitoringUnit_MainLoop ";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
			String PreState = TradeMonitorAgentState;
			Calendar rightNow;
			
			while(!TradeMonitorAgentState.equals("END")){
				switch(TradeMonitorAgentState){
				
				case "READY":
					
					break;	
				case "START":	
					if (PreState.equals("READY")){
						//初回のプロセスの起動に使う
						PreState = TradeMonitorAgentState;
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
					}	
					
					SimpleDateFormat D = new SimpleDateFormat("HH:mm:ss.SSS");
			    	rightNow = Calendar.getInstance();
					Now = rightNow.getTime();
					
					if(NewSellEntry || NewOrderEntry){
						OrderCheck();//注文が追加されているかどうかを確認
						if(Confirmed){ //新規注文の追加が確認できた
							NewSellEntry 	=	false;
							NewOrderEntry 	= 	false;
						}
					}
					if(Confirmed){// 注文が入った後の状態確認
						if( preBuyTrend.equals(DecisionTradeStaticsData.BuyTrend) ||preSellTrend.equals(DecisionTradeStaticsData.SellTrend)){//注文があるときのみ更新して確認する
							StateCheck();//注文の状態を確認する
							preBuyTrend 	=	DecisionTradeStaticsData.BuyTrend;  //注文状態を更新
							preSellTrend 	= 	DecisionTradeStaticsData.SellTrend;//注文状態を更新
						}
					}
					
					System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
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
					TradeMonitorAgentState = "END";
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
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End" );
			//start any web access process 
		}	
		void TradeMonitorPageOpen(WebDriver driver_monitor){
			String SubProcessName = "TradeMonitor_PageOpen ";
			driver_monitor.get(ADDRESS);
			Login(driver_monitor, USER_NAME, PASSWORD);	

		}
		void StateCheck(){//TradeMonitoringUnitの状態確認
			String SubProcessName = "TradeMonitor_StateCheck ";
			String GetURL = "https://its2.monex.co.jp/StockOrderConfirmation/gNaviX0002EIP/kbodr/odr_yak/ichiran";
			driver_monitor.get(GetURL);	
			
			//新しい注文を検知するロジック
			
			Confirmed = true; //検知したらConfirmedをtrueにする
		}
		void OrderCheck(){//TradeMonitoringUnitの状態確認
			String SubProcessName = "TradeMonitor_OrderCheck ";
			String GetURL = "https://its2.monex.co.jp/StockOrderConfirmation/gNaviX0002EIP/kbodr/odr_yak/ichiran";
			driver_monitor.get(GetURL);
			
			//新しい注文に対する対応状況を確認するロジック
		}
		public void TradeMonitorLogWrite(){
			String SubProcessName = "TradeMonitorLogWrite";
	    	Calendar rightNow;
	    	Date Now = new Date();
	    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
	    	rightNow = Calendar.getInstance();
			Now = rightNow.getTime();
			
			//String temp = TradeStatics.StaticsNumber +"	"+ BoardInfo.DataNumber+"	"+D.format(Now)+"	"; 
			
			//temp =temp + UserProperty.UserAction.ActionScore+"	";	
			//temp  = temp + "\r\n";
			//TradeMonitorAgentLog.FileWrite(temp);	
			
		}	
				
	}
	
	void Login(WebDriver driver, String user_name, String password) {	
		//---------------------Login ------------------------------------------------------
			driver.findElement(By.name("loginid")).sendKeys(user_name);
			driver.findElement(By.name("passwd")).sendKeys(password);
			driver.findElement(By.xpath("//*[@value='ログイン']")).click();
		//---------------------------------------------------------------------------------
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