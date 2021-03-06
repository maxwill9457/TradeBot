import java.util.*;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;


import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class TradeOperatorUnit extends DefinedData{// 意思決定   Trade情報により、買、維持、売の行動　また価格を決める

	String ProcessName = "TradeOperatorUnit";
	String SimulationMode;    	  	//ONLINE            	real schedule, real data,  and operation
									//OPERATION_SIMULATION  real schedule, real data, no actual operation execute 
									//OFFLINE_SIMULATUION   test schedule,　real data,  no actual operation execute 
									//TEST_DATA_SIMULATION  test schedule,　test data and no actual operation execute   
	
	String TradeOperatorUnitState;// TradeOperatorUnitのスレッド状態
	//String OrderAgentUnitState;// TradeOrderAgentのスレッド状態
	//String MonitorAgentUnitState;// TradeMonitorAgentのスレッド状態
	
	
	//UserAction OperatorAction;
	
	String OperatorState;// オペレータの状態  BUY SELL CHANCEL STANDBY  
	String OrderProcState;//注文状況 BUYING SELLING CHANCELING STANDBY  
	String MonitorState;//監視状況
	
	String target;
	String target_num;
	

	String ADDRESS = "https://www.monex.co.jp/Login/00000000/login/ipan_web/hyoji";
	String HoldingStockPath ;
	
	BoardInfo BoardInfo;
	UserProperty UserProperty;
	TradeStatics TradeStatics;
	
	FirefoxProfile profile_order 			= new FirefoxProfile(new File("D:\\invest\\project\\firefox_profile"));  
	FirefoxProfile profile_monitor_Order 	= new FirefoxProfile(new File("D:\\invest\\project\\firefox_profile")); 
	FirefoxProfile profile_monitor_Property = new FirefoxProfile(new File("D:\\invest\\project\\firefox_profile")); 
	
	WebDriver driver_order 				= new FirefoxDriver(profile_order);
	WebDriver driver_monitor_Order 		= new FirefoxDriver(profile_monitor_Order);
	WebDriver driver_monitor_Property 	= new FirefoxDriver(profile_monitor_Property );
	
	OrderAgentUnit OrderAgentUnit;
	CatchException OrderAgentUnit_catchException;
	
	OrderMonitorUnit OrderMonitorUnit;
	CatchException OrderMonitorUnit_catchException;
	
	File HoldingStockFileSet = null; //simulation 用ファイルの集合
    File[] HoldingStockFileList; //simulation用ファイルリスト
    BufferedReader FileDataBuffer; 
    

	OrderPanelUnit OrderPanelUnit;
	
	File HoldStockInfoFile;
	
	LogUnit HoldingstockLog;
	LogUnit TradeOperatorLog; // Command_Index  時間　Userアクション	値段	Operatorアクション	値段　OperatorState	
	LogUnit ErrorLog;
	
	Date Now = new Date();
	
	TradeOperatorUnit(String target,String target_num,BoardInfo BoardInfo, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){
		
		String SubProcessName = "Initiation";
		TradeOperatorUnitState = "PREPARE";
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
		
		this.SimulationMode = SimulationMode;
		this.target = target;
		this.target_num	= target_num;
		this.ErrorLog = ErrorLog;
		
		this.BoardInfo = BoardInfo;
		this.UserProperty = UserProperty;
		
		OrderPanelUnit = new OrderPanelUnit(UserProperty);
		
		//-----------OperatorAction 初期化--------------
		//this.NewOrder = false;
		//this.OperatorAction = new UserAction();
		//this.OperatorAction.ActionIndex = 0;
		//this.OperatorAction.Action[0] = "STANDBY";
		//this.OperatorAction.Action[1] = "";
		//this.OperatorAction.target = target;
		//--------------------------------------------
				
		this.OperatorState	= "STANDBY";//オペレータの状態
		this.OrderProcState		= "STANDBY";//注文状況
		this.MonitorState	= "STANDBY";//監視状況
		

		HoldingStockPath = LogPath+"TradeOperatorUnit//HoldingStock//";	
		
		TradeOperatorLog = new LogUnit(LogPath+"TradeOperatorUnit//trade//",this.target+"_TradeOperator",0); // create log file
		HoldingstockLog = new LogUnit(LogPath+"TradeOperatorUnit//trade//",this.target+"_HoldingStock",0); // create log file
		
		//---------------所持株の情報取得----------------------
		
		LoadHoldingStockInfoLoader(target , LogPath,UserProperty);
		
		//------------------------------------------------	
		
		//---------------モニターユニットを生成---------------------
		this.OrderMonitorUnit = new OrderMonitorUnit(target,target_num,driver_monitor_Order,driver_monitor_Property,UserProperty,TradeStatics,ErrorLog,SimulationMode,LogPath,Speed);
		OrderMonitorUnit_catchException = new CatchException();
		OrderMonitorUnit.setName("Thread-BoardInfoExtractor-"+target);
		OrderMonitorUnit.setUncaughtExceptionHandler(OrderMonitorUnit_catchException);
		//if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
		OrderMonitorUnit.start();	
		//}
		//---------------購入ユニットを生成---------------------
		this.OrderAgentUnit = new OrderAgentUnit(target,target_num,driver_order,UserProperty,OrderMonitorUnit.MonitorOrderInfoList,TradeStatics,ErrorLog,SimulationMode,LogPath,Speed);
		OrderAgentUnit_catchException = new CatchException();
		OrderAgentUnit.setName("Thread-BoardInfoExtractor-"+target);
		OrderAgentUnit.setUncaughtExceptionHandler(OrderAgentUnit_catchException);
		OrderAgentUnit.start();
		

		
		
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
					if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
						OrderAgentUnit.OrderAgentUnitState ="START";
						OrderMonitorUnit.MonitorAgentUnitState ="START";
					}
					else if(SimulationMode.equals("OFFLINE_SIMULATUION")||SimulationMode.equals("TEST_DATA_SIMULATION")){
						OrderAgentUnit.OrderAgentUnitState ="START";
						OrderMonitorUnit.MonitorAgentUnitState ="START";
					}
					
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				}	
				
				AgentStateCheck(OrderAgentUnit.OrderProcState);//OrderAgentの状態確認しOperatorStateを更新
				
				if(UserProperty.UserAction.ActionIndex != OrderAgentUnit.OrderAgentAction.ActionIndex){
					//UserProperty.Actionから更新がある場合、OperatiorActionの判断を行う	
					System.out.println( UserProperty.UserAction.ActionIndex+"	" +OrderAgentUnit.OrderAgentAction.ActionIndex );
					
					//現在の注文状況を確認し、新注文指示に対して行動を決める
					String TempOrderAgentInstruction = ActionDecision(UserProperty,OrderAgentUnit.OrderAgentAction);

					//金額に問題が無いのでOrderAgentにだす行動指示に出す
					if (!TempOrderAgentInstruction.equals("NoAction")&&!TempOrderAgentInstruction.equals("Error")){
						synchronized (UserProperty.UserAction.ActionLock){
							ActionExec(TempOrderAgentInstruction,UserProperty,OrderAgentUnit.OrderAgentAction); //意思決定からのアクションとオペレータ現在の状態でアクションを決める
						//OperatorState = "BUYING" "SELLING" /;
						}
					}
					else if(TempOrderAgentInstruction.equals("Error")){
						OrderAgentUnit.OrderAgentAction.ActionIndex = UserProperty.UserAction.ActionIndex;
						UserProperty.UserAction.result = "Error";
					}
				}
				else{
					//NoAction Errorの場合はスルー
					//TradeOperatorLogWrite(SubProcessName,"I","新注文指示が無い Standby",UserProperty);	;
					//System.out.println( "Indexの変更がない" );
				}
				
				try{
					Thread.sleep(1000);
				}catch (InterruptedException e){
				}	
				if(!UserProperty.HoldStockList.isEmpty()){
					String temp ="";
					for(int i=0;i<UserProperty.HoldStockList.size();i++){
						HoldStockInfo tempHoldStockInfo;
						tempHoldStockInfo = UserProperty.HoldStockList.get(i);//該当所持株情報抽出
						
						temp =i+" "+ tempHoldStockInfo.StockName+" "+tempHoldStockInfo.StockSeries +" "+
								tempHoldStockInfo.StockNum+" "+tempHoldStockInfo.SumPrice+" "+tempHoldStockInfo.PurchasePrice+"\r\n";
					}
					
					System.out.println( temp);
				}
				else{
					System.out.println( "所持株なし");
				}
				//System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
				break;
			case "PAUSE":
				//System.out.println( "TradeOperatorUnit PAUSE");
				break;
			case "FINISHING":
				//---------------気配板プロセスの完了待つ-----------------------------	
				OrderAgentUnit.OrderAgentUnitState = "FINISHING";
				while(!OrderAgentUnit.OrderAgentUnitState.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}
				OrderMonitorUnit.MonitorAgentUnitState = "FINISHING";
				while(!OrderMonitorUnit.MonitorAgentUnitState.equals("END")){
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


	
	void AgentStateCheck(String OrderState){ //OrderAgent、MonitorAgentの状態を更新
		String SubProcessName = "AgentCheck";
		String Action;
		
		if(	OrderState.equals("BUYING")){ // Agentの状態が発注中になったらoperatorの状態を待機状態から購買中に変更
			if(OperatorState.equals("WAIT_AGENT_BUY") || OperatorState.equals("WAIT_AGENT_BUY_PRICE_CHANGE") ){
		
				OperatorState = "BUYING";
				String Msg = "OperatorState変更"+OperatorState;
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				
			}
		}
		if(OrderState.equals("SELLING")){ // Agentの状態が販売中になったらoperatorの状態を待機状態から販売中に変更
			if(	OperatorState.equals("WAIT_AGENT_SELL") || OperatorState.equals("WAIT_AGENT_SELL_PRICE_CHANGE")){
				OperatorState = "SELLING";
				String Msg = "OperatorState変更"+OperatorState;
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
			}
		}
		if(OrderState.equals("CHANCELLING")){// Agentの状態が取消中になったらoperatorの状態を待機状態から取消中に変更
			if(	OperatorState.equals("WAIT_AGENT_CHANCEL")){
				OperatorState = "CHANCELLING";
				String Msg = "OperatorState変更"+OperatorState;
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
			}
		}
		
		if( OrderAgentUnit.OrderProcState.equals("FINISHED")){// Agentの注文が完了したら、operatorの状態を初期状態に戻す。
			OperatorState = "STANDBY";
			OrderAgentUnit.OrderProcState ="STANDBY";
			UserProperty.UserAction.result = "Finished";
			
			String Msg = "OperatorState変更"+OperatorState;
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
			
		}
	
	}
	
	String ActionDecision(UserProperty UserProperty, UserAction OrderAgentAction ){ //OrderAgent、MonitorAgentの状態を更新
		String SubProcessName = "OrderActionCheck";
		UserAction tempAction = UserProperty.UserAction;
		
		if(tempAction.Action[0].equals("BUY")){
			if(OperatorState.equals("BUYING")){
				if (OrderAgentAction.Price.compareTo(tempAction.Price) != 0){ 
					if(PropertyCheck("BUY",UserProperty,target)){// 
						//値段が違うため発注変更
						String Msg = "UserAction買い中 値段を変更："+ UserProperty.UserAction.Price.toString();
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
						TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
						return "ChangeBuyPrice";
					}
					else{
						String Msg = "値段を変更エラー、残高不足："+ UserProperty.UserAction.Price.toString();
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
						TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
						return "Error";
					}
					
				}
				else{
					//金額同様のためアクションなし
					OrderAgentAction.ActionIndex= tempAction.ActionIndex;
					String Msg = "UserAction買い中値段と同様のため変化なし："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"W",Msg,UserProperty);	
					return "NoAction";
				}
			}
			else if(OperatorState.equals("SELLING")){
				//売りが買いに変更、現在の売りを一旦キャンセルして買う
				if(PropertyCheck("BUY",UserProperty,target)){// 
					String Msg = "UserAction売り中から買いに変更："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
					return "ChangeOrderSelltoBuy";
				}
				else{
					String Msg = "売りに変更エラー、所持株不足："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
					return "Error";
				}	
			}
			else if(OperatorState.equals("STANDBY")){
				//現在特に行動していないため、そのまま発注
				if(PropertyCheck("BUY",UserProperty,target)){// 
					String Msg = "UserAction待機中から買いに変更："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
					return "BuyOrder";
				}
				else{
					String Msg = "注文エラー、残高不足："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
					return "Error";	
				}
			}
			else if(OperatorState.equals("WAIT_AGENT_BUYING")||OperatorState.equals("WAIT_AGENT_SELLING")){
				//待機中なので行動をしない
				return "BuyOrder";
			}
			else{
				//想定外の状態
				String Msg = "OperatorState想定外の状態："+OperatorState;
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
				return "Error";
			}
		}
		else if(tempAction.Action[0].equals("SELL")){
			if(OperatorState.equals("BUYING")){
				//売りが買いに変更、現在の発注を一旦キャンセルして売る
				if(PropertyCheck("SELL",UserProperty,target)){// 
				
					String Msg = "UserAction買いから売りに変更："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
					return "ChangeOrderBuytoSell";
				}
				else{
					String Msg = "売りに変更エラー、所持株不足："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
					return "Error";
				}
			}
			else if(OperatorState.equals("SELLING")){
				if (OrderAgentAction.Price.compareTo(tempAction.Price) != 0){ 
					//値段が違うため発注変更
					if(PropertyCheck("SELL",UserProperty,target)){// 
						String Msg = "UserAction買いから売りに変更："+ UserProperty.UserAction.Price.toString();
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
						TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
						return "ChangeSellPrice";
					}
					else{
						String Msg = "UserAction買いから売りに変更エラー　所持株は無い："+ UserProperty.UserAction.Price.toString();
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
						TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
						return "Error";
					}
				}
				else{
					//金額同様のためアクションなし
					OrderAgentAction.ActionIndex= tempAction.ActionIndex;
					String Msg = "UserActionが現在売り中値段と同様："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"W",Msg,UserProperty);	
					return "NoAction";
				}
			}
			else if(OperatorState.equals("STANDBY")){
				//現在特に行動していないため、そのまま発注
				if(PropertyCheck("SELL",UserProperty,target)){// 
					String Msg = "UserAction待機中から売りに変更："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
					return "SellOrder";
				}
				else{
					String Msg = "UserAction待機中から売りにエラー　所持株は無い："+ UserProperty.UserAction.Price.toString();
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
					return "Error";
				}
				
			}
			else if(OperatorState.equals("WAIT_AGENT_BUYING")||OperatorState.equals("WAIT_AGENT_SELLING")){
				//待機中なので行動をしない
				return "SellOrder";
			}

			else{
				//想定外の状態	
				String Msg = "OperatorState想定外の状態："+OperatorState;
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
				return "Error";
			}
			
		}else{
			String Msg = "UserProperty.UserAction想定外のアクション："+UserProperty.UserAction.Action[0]+"	"+UserProperty.UserAction.Action[1];
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
			TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
			return "Error";
		}
		
	}
	
	Boolean PropertyCheck(String UserOrderAction ,UserProperty UserProperty,String target){//エージェントの実行行動を決定する   OrderingPrice：現在購買中に預かる金額
		String SubProcessName = "PropertyCheck ";
		
		switch(UserOrderAction){
		case "BUY":
			if ((UserProperty.cash.compareTo(UserProperty.UserAction.Price.multiply(UserProperty.UserAction.OrderStockNum)))>=0){
				String Msg = "PropertyCheck残高確認："+UserProperty.cash + "必要金額："+ UserProperty.UserAction.Price.multiply(UserProperty.UserAction.OrderStockNum);
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
				return true;
			}
			else{
				String Msg = "PropertyCheck残高不足："+UserProperty.cash + "必要金額："+ UserProperty.UserAction.Price.multiply(UserProperty.UserAction.OrderStockNum);
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
				return false;	
			}
		case "SELL":
			List<HoldStockInfo> tempList = UserProperty.HoldStockList;
			HoldStockInfo	tempHoldStockInfo;
			int i=0;
			int flag = 0;
			if(!UserProperty.HoldStockList.isEmpty()){//Listになにもないものならエラー対応が必要
				for(i=0;i<UserProperty.HoldStockList.size();i++){
		
					tempHoldStockInfo = UserProperty.HoldStockList.get(i);//該当所持株情報抽出
					if(tempHoldStockInfo.StockName.equals(target)){ // 一致する所持株確認
						if (tempHoldStockInfo.StockNum.compareTo(UserProperty.UserAction.OrderStockNum)>=0){
							String Msg = "PropertyCheck持ち株数確認："+tempHoldStockInfo.StockNum + "必要株数："+ UserProperty.UserAction.OrderStockNum;
							System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
							TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
							flag++;//所持株が売却株数より多いのでOK
						}
						else{//所持株が売却株数より少ないのでNG
							String Msg = "PropertyCheck持ち株数不足："+tempHoldStockInfo.StockNum + "必要株数："+ UserProperty.UserAction.OrderStockNum;
							System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
							TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
							return false;
						}
					}		
				}
				if (flag ==0){
					String Msg = "PropertyCheck持ち株数確認できない："+target;
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
					return false; //対象所持株は無い
				}
				else{
					return true;
				}
			}
			else{
				String Msg = "PropertyCheck持ち株数確認できない："+target;
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
				return false; //所持株は無い
			}
		}
		return false; //想定外結果
	}
	
	void ActionExec(String TempOrderAgentInstruction,UserProperty UserProperty, UserAction OrderAgentAction ){ //意思決定からのアクションとオペレータ現在の状態でアクションを決める
		
		String SubProcessName = "ActionDecision";
		UserAction tempAction = UserProperty.UserAction;	
		
		String Msg;
		switch(TempOrderAgentInstruction){
		case "ChangeBuyPrice":
			// 買いに変更する
			OrderAgentAction.Action[0] = "CHANGE_BUY";   
			OrderAgentAction.Action[1] = "";
			OrderAgentAction.ActionNum =1;
			OrderAgentAction.ActionIndex = tempAction.ActionIndex;
			OrderAgentAction.Price = tempAction.Price;
			OrderAgentAction.OrderStockNum = tempAction.OrderStockNum;
			OrderAgentAction.NewOrder = true;
			tempAction.result = "Ordering";
			
			Msg="買い中OrderAgentに買い金額変更指示："+OrderAgentAction.Price.toString()+"に変更" ;
			System.out.println( OrderAgentAction.ActionIndex + Msg );
			TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);					
			OperatorState = "WAIT_AGENT_BUY";
			
			break;
		case "ChangeOrderSelltoBuy":
			//
			OrderAgentAction.Action[0] = "CHANCEL";
			OrderAgentAction.Action[1] = "BUY";
			OrderAgentAction.ActionNum =2;
			OrderAgentAction.ActionIndex= tempAction.ActionIndex;
			OrderAgentAction.Price = tempAction.Price;
			OrderAgentAction.OrderStockNum = tempAction.OrderStockNum;
			OrderAgentAction.NewOrder = true;
			tempAction.result = "Ordering";
			
			Msg="売り中OrderAgentに買い変更指示：買い"+OrderAgentAction.Price.toString()+"に変更";
			System.out.println( OrderAgentAction.ActionIndex + Msg );
			TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);				
			OperatorState = "WAIT_AGENT_BUY_PRICE_CHANGE";
			
			break;
		case "BuyOrder":
			OrderAgentAction.Action[0] = "BUY";
			OrderAgentAction.Action[1] = "";
			OrderAgentAction.ActionNum =1;
			OrderAgentAction.ActionIndex= tempAction.ActionIndex;
			OrderAgentAction.Price = tempAction.Price;
			OrderAgentAction.OrderStockNum = tempAction.OrderStockNum;
			OrderAgentAction.NewOrder = true;
			tempAction.result = "Ordering";
			
			Msg="待機中OrderAgentに買い指示："+OrderAgentAction.Price.toString()+"に変更";
			System.out.println( OrderAgentAction.ActionIndex + Msg );
			TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);				
			OperatorState = "WAIT_AGENT_BUY";

			break;
		case "ChangeOrderBuytoSell":
			OrderAgentAction.Action[0] = "CHANCEL";
			OrderAgentAction.Action[1] = "SELL";
			OrderAgentAction.ActionNum =2;
			OrderAgentAction.ActionIndex= tempAction.ActionIndex;
			OrderAgentAction.Price = tempAction.Price;
			OrderAgentAction.OrderStockNum = tempAction.OrderStockNum;
			OrderAgentAction.NewOrder = true;
			tempAction.result = "Ordering";
			
			Msg="買い中OrderAgentに売り変更指示："+OrderAgentAction.Price.toString() + "に変更";
			System.out.println( OrderAgentAction.ActionIndex + Msg );
			TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);				
			OperatorState = "WAIT_AGENT_SELL";
			
			
			break;
		case "ChangeSellPrice":
			OrderAgentAction.Action[0] = "CHANGE_SELL";
			OrderAgentAction.Action[1] = "";
			OrderAgentAction.ActionNum =1;
			OrderAgentAction.ActionIndex= tempAction.ActionIndex;
			OrderAgentAction.Price = tempAction.Price;
			OrderAgentAction.OrderStockNum = tempAction.OrderStockNum;
			OrderAgentAction.NewOrder = true;
			tempAction.result = "Ordering";
			
			Msg=" SELLINGの状態で新規SELLで新注文値段" +OrderAgentAction.Price.toString()+ "に変更";
			System.out.println( OrderAgentAction.ActionIndex + Msg );
			TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);			
			OperatorState = "WAIT_AGENT_SELL_PRICE_CHANGE";

			break;
		case "SellOrder":
			OrderAgentAction.Action[0] = "SELL";
			OrderAgentAction.Action[1] = "";
			OrderAgentAction.ActionNum =1;
			OrderAgentAction.ActionIndex= tempAction.ActionIndex;
			OrderAgentAction.Price = tempAction.Price;
			OrderAgentAction.OrderStockNum = tempAction.OrderStockNum;
			OrderAgentAction.NewOrder = true;
			tempAction.result = "Ordering";
			
			Msg = " 待機状態で新規SELL　　売り値段" +OrderAgentAction.Price.toString()+ "に入る ";
			System.out.println( OrderAgentAction.ActionIndex + Msg );
			TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
			OperatorState = "WAIT_AGENT_SELL";
			break;	
		}
		
		
	}
	
	public class OrderAgentUnit extends DefinedData{
		String SimulationMode ;
		String target;   // 注文銘柄
		String target_num;//注文銘柄株数
		String OrderAgentUnitState = "STANDBY";
		String OrderProcState = "STANDBY";//注文状況
		
		LogUnit OrderAgentLog;  // Trade operation log
		LogUnit ErrorLog;
		UserProperty UserProperty;
		
		OrderInfo OrderInfo;
		OrderInfo[] MonitorOrderInfoList;
		
		boolean NewOrder;
		UserAction OrderAgentAction;
		
		OrderAgentUnit(String target,String target_num,WebDriver driver_order, UserProperty UserProperty,OrderInfo[] MonitorOrderInfoList,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){

			String SubProcessName = "OrderAgent_Initiation";
			OrderAgentUnitState = "PREPARE";
			System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
			
			this.SimulationMode = SimulationMode;
			this.target = target;
			this.target_num = target_num;
			this.ErrorLog = ErrorLog;
			this.UserProperty = UserProperty;
			this.MonitorOrderInfoList = MonitorOrderInfoList;
			
			this.NewOrder =false;
			this.OrderAgentAction = new UserAction();
			this.OrderInfo = new OrderInfo();
			
			TradeOrderPageOpen(driver_order);//株取引ページの用意

			this.OrderAgentLog = new LogUnit(LogPath+"TradeOperatorUnit//trade//",this.target+"OrderAgent",0); // create log file

			OrderAgentUnitState = "READY";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );
		}
			
		public void run(){ 
			String SubProcessName = "OrderAgent_Main_Loop ";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
			String PreState = OrderAgentUnitState;
			Calendar rightNow;
			
			while(!OrderAgentUnitState.equals("END")){
				switch(OrderAgentUnitState){
				
				case "READY":
					break;	
				case "START":	
					if (PreState.equals("READY")){
						//初回のプロセスの起動に使う
						PreState = OrderAgentUnitState;
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
					}	
					
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}	
					OrderInfo=ChangeCheck(OrderInfo,MonitorOrderInfoList, UserProperty);//TradeMonitoringUnitの状態確認
					//ActionCheck(OrderAgentAction,NewOrder); //エージェントの状態を更新する
					if(OrderAgentAction.NewOrder){
						//PropertyCheck();//エージェントの購買力を確認する
						OrderActionExec(driver_order, OrderAgentAction,OrderInfo,MonitorOrderInfoList);//注文アクションを実行する
						OrderAgentAction.NewOrder =false;
					}
					//System.out.println( target+ "	"+ ""+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
					break;
				case "PAUSE":
					//System.out.println( "TradeOperatorUnit PAUSE");
					break;
				case "FINISHING":
					//System.out.println( "TradeOperatorUnit FINISH");
					OrderAgentUnitState = "END";
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
			Login(driver_order, UserProperty.USER_NAME, UserProperty.PASSWORD);	
			driver_order.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();;//株式取引ページに移動
		}	
		
		OrderInfo ChangeCheck(OrderInfo OrderInfo,OrderInfo[] MonitorOrderInfoList,UserProperty UserProperty){//TradeMonitoringUnitの状態確認
			String SubProcessName = "TradeOrder_ChangeCheck ";
			
			//--------------テストモードの場合　2秒後に注文したものの内容を完了とする。--------------
			if(!OrderInfo.OrderSeriesNum.equals(""))//トラッキング注文あり
				if (OrderInfo.OrderSeriesNum.equals("TEST")){//テストモードなのでそのままパス
					if (OrderInfo.Ordertype.equals("BUY")){
						HoldStockInfo tempHoldStockInfo;
						int flag =0;
						if(!UserProperty.HoldStockList.isEmpty()){//Listになにもないのでそのまま追加
							
							for(int i=0;i<UserProperty.HoldStockList.size();i++){
								
								tempHoldStockInfo = UserProperty.HoldStockList.get(i);//該当所持株情報抽出
								
								if(tempHoldStockInfo.StockName.equals(OrderInfo.StockName)){ // 一致する所持株確認
									UserProperty.Asset = UserProperty.Asset.subtract(TradeFee); //手数料の差し引き
									UserProperty.cash = UserProperty.cash.subtract(OrderInfo.OrderPrice.multiply(OrderInfo.OrderNum)).subtract(TradeFee) ;//cash-株価*株数-手数料
									
									tempHoldStockInfo.StockNum = tempHoldStockInfo.StockNum.add(OrderInfo.OrderNum); //現有株+購入分
									tempHoldStockInfo.SumPrice = tempHoldStockInfo.SumPrice.add(OrderInfo.OrderPrice.multiply(OrderInfo.OrderNum)); //現有株総合金額+購入分
									tempHoldStockInfo.PurchasePrice = tempHoldStockInfo.SumPrice.divide(tempHoldStockInfo.StockNum,3, BigDecimal.ROUND_HALF_UP);
			
									UserProperty.HoldStockList.set(i,tempHoldStockInfo);
									HoldStockInfoLogWrite( UserProperty);
									flag++;
								}
								
								
							}
						}	
						if(flag ==0){ //empty OR　リストに所持株情報が無い　新規追加
							
							UserProperty.Asset = UserProperty.Asset.subtract(TradeFee); //手数料の差し引き
							UserProperty.cash = UserProperty.cash.subtract(OrderInfo.OrderPrice.multiply(OrderInfo.OrderNum)).subtract(TradeFee) ;//cash-株価*株数-手数料
							
							tempHoldStockInfo = new HoldStockInfo();
							
							tempHoldStockInfo.StockName = target;
							tempHoldStockInfo.StockSeries = target_num;
							tempHoldStockInfo.StockNum = OrderInfo.OrderNum;
							
							tempHoldStockInfo.SumPrice = tempHoldStockInfo.SumPrice.add(OrderInfo.OrderPrice.multiply(OrderInfo.OrderNum)); //総合金額
							tempHoldStockInfo.PurchasePrice = tempHoldStockInfo.SumPrice.divide(tempHoldStockInfo.StockNum,3, BigDecimal.ROUND_HALF_UP);
							
							UserProperty.HoldStockList.add(tempHoldStockInfo);
							
							HoldStockInfoLogWrite( UserProperty);
						}

					}
					else if  (OrderInfo.Ordertype.equals("SELL")){
						HoldStockInfo tempHoldStockInfo;
						int flag =0;
						if(!UserProperty.HoldStockList.isEmpty()){//Listになにもないものならエラー対応が必要
							for(int i=0;i<UserProperty.HoldStockList.size();i++){
								
								tempHoldStockInfo = UserProperty.HoldStockList.get(i);//該当所持株情報抽出
								
								if(tempHoldStockInfo.StockName.equals(OrderInfo.StockName)){ // 一致する所持株確認
									BigDecimal netgain = (OrderInfo.OrderPrice.subtract(tempHoldStockInfo.PurchasePrice)).multiply(OrderInfo.OrderNum);
									if (netgain.compareTo(new BigDecimal(0))>0){ //利益がでる場合は所得税
										BigDecimal tax = 	 netgain.multiply(new BigDecimal("0.200"));
										netgain = netgain.multiply(new BigDecimal("0.800"));
										UserProperty.Asset = UserProperty.Asset.subtract(TradeFee).add(netgain); //手数料の差し引き
									}
									else{
										UserProperty.Asset = UserProperty.Asset.subtract(TradeFee).add(netgain); //手数料の差し引き
									}	
						
									UserProperty.cash = UserProperty.cash.add(OrderInfo.OrderPrice.multiply(OrderInfo.OrderNum)).subtract(TradeFee) ;//cash-株価*株数-手数料
									
									tempHoldStockInfo.StockNum = tempHoldStockInfo.StockNum.subtract(OrderInfo.OrderNum); //現有株+購入分
									tempHoldStockInfo.SumPrice = tempHoldStockInfo.PurchasePrice.multiply(tempHoldStockInfo.StockNum); //現有株総合金額+購入分
										
									BigDecimal value = new BigDecimal("0.0");
									if(tempHoldStockInfo.StockNum.compareTo(BigDecimal.ZERO)==0){
										UserProperty.HoldStockList.remove(i); //所持株がなくなるため削除
									}
									else{
										UserProperty.HoldStockList.set(i,tempHoldStockInfo);
									}
									HoldStockInfoLogWrite( UserProperty);
									flag++;
								}
							}
						}	
						if(flag ==0){ //empty OR　リストに所持株情報が無い　新規追加
							
							String Msg = "売り異常　所持していない株を売却しようとしている："+ OrderProcState;
							System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
							TradeOperatorLogWrite(SubProcessName,"E",Msg,UserProperty);	
							
						}
			
					}
					
					OrderInfo = new OrderInfo();//リセットできていない
					OrderProcState = "FINISHED";
					
					
					String Msg = "TEST購買完了OrderState変更："+ OrderProcState;
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
					TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
				}
				else{
					for(int i=0;i<10;i++){
						if(MonitorOrderInfoList[i].OrderSeriesNum.equals(OrderInfo.OrderSeriesNum )){
							OrderInfo.OrderState = MonitorOrderInfoList[i].OrderState;//発注状況
							if(OrderInfo.OrderState.equals("約定済")){ //取引成立確認
								OrderAgentLogWrite("I","株注文成立",OrderInfo);
								
								//OrderAgentの初期化
								OrderInfo = new OrderInfo();
								OrderProcState = "FINISHED";
								
								String Msg = "購買完了OrderState変更："+ OrderProcState;
								System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
								TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
								
								break;
							}
						}
					}		
				}
			
			return OrderInfo;
		}

		void OrderActionExec(WebDriver driver_order, UserAction OrderAction, OrderInfo OrderInfo,OrderInfo[] MonitorOrderInfoList) {
			
			String SubProcessName = "OrderActionExec ";
			int error;
			
			//--------------テストモードの場合　実際に画面操作はしない--------------
			
			for(int i=0;i<OrderAction.ActionNum;i++){ //Actionの数で複数実行
				if(OrderAction.Action[i].equals("BUY")){
					error = BuyActionExec(driver_order, OrderAction, OrderInfo,MonitorOrderInfoList);//注文操作実行
					if(error == 0){
						OrderProcState = "BUYING";
					}
					else{
						OrderProcState = "ERROR";
					}	
				}
				else if(OrderAction.Action[i].equals("SELL")){
					error =SellActionExec(driver_order, OrderAction ,MonitorOrderInfoList);//売却操作実行
					if(error == 0){
						OrderProcState = "SELLING";
					}
					else{
						OrderProcState = "ERROR";
					}
				}
				else if(OrderAction.Action[i].equals("CHANGE_BUY")||OrderAction.Action[i].equals("CHANGE_SELL")){
					error =OrderChangeExec(driver_order, OrderAction, OrderInfo,MonitorOrderInfoList);//値段変更操作実行
					if(error == 0){
						if(OrderAction.Action[i].equals("CHANGE_BUY")){
							OrderProcState = "BUYING";
						}
						if(OrderAction.Action[i].equals("CHANGE_SELL")){
							OrderProcState = "SELLING";
						}
					}
					else{
						OrderProcState = "ERROR";
					}
				}
				else if(OrderAction.Action[i].equals("CHANCEL")){
					error =OrderCancelExec(driver_order, OrderAction, OrderInfo,MonitorOrderInfoList);//注文取消操作実行
					if(error == 0){
						OrderProcState = "CHANCELLING";
					}
					else{
						OrderProcState = "ERROR";
					}
				}
				else if(OrderAction.Action[i].equals("")){
				// アクションが無いときはアクションなしで
				}
				else{
					System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"End" );
					ErrorLogWrite(ProcessName,SubProcessName,"unknown OrderAction state	"+OrderAction.Action[i]);
					OrderAgentLogWrite("E","株注文失敗："+SubProcessName,OrderInfo);
				}
			}
				
		}		
		int BuyActionExec(WebDriver driver_order, UserAction OrderAction,OrderInfo OrderInfo,OrderInfo[] MonitorOrderInfoList) {	
			String SubProcessName = "TradeOrder_BuyActionExec ";
			int returnFlag=0;
			
			//注文値段をチェックする必要がある
			
			//
			if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
			//---------------------実際に操作する場合-------------------------------------------------------------
				try{
					
			//---------------------注文ページに移動し発注する------------------------------------------------------
					driver_order.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();//株式取引ページに移動
					driver_order.findElement(By.xpath("//*[@id='focuson']")).sendKeys(target_num); //株式取引→銘柄記入
					driver_order.findElement(By.xpath("//*[@id='gn_service-']/div[6]/div[2]/div/div/div[1]/div[1]/div[1]/div[2]/dl[1]/dd/form/p[2]")).click();;//注文ページに移動
					driver_order.findElement(By.xpath("//*[@id='orderNominal']")).sendKeys("100"); //買い注文　→株数記載
					driver_order.findElement(By.xpath("//*[@id='form01']/div[3]/div[1]/div[1]/table/tbody/tr[5]/td/div[2]/span/label")).click();//買い注文　→　指値
					driver_order.findElement(By.xpath("//*[@id='idOrderPrc']")).sendKeys(OrderAction.Price.toString()); //買い注文　→金額記載
			
			//----------------------------注文実行--------------------------------------------
					driver_order.findElement(By.xpath("//*[@id='form01']/div[3]/div[1]/div[2]/div/input")).click();//買い注文（次へ）
					driver_order.findElement(By.xpath("//*[@id='gn_service-lm_buy']/div[7]/div[1]/form/div/div[1]/div[1]/div[2]/div[2]/input")).click();//内容確認　買い実施
			//-------------------------------------------------------------------------------
				
					OrderInfo.StockName = target;	//株名
					OrderInfo.StockSeriesNum = target_num;//株シリアル番号
					OrderInfo.Ordertype = "BUY" ;//発注内容　BUY SELL
					OrderInfo.OrderPrice = OrderAction.Price;//発注金額
					OrderInfo.OrderNum = new BigDecimal(100);//発注数
					OrderInfo.OrderSeriesNum = driver_order.findElement(By.className("com-block-num")).getText().replace("ご注文番号 ", "");//発注番号を取得
					
					
					
					int i=0;
					while(true){
						if(MonitorOrderInfoList[i%10].OrderSeriesNum.equals(OrderInfo.OrderSeriesNum )){
							OrderInfo.OrderState = MonitorOrderInfoList[i%10].OrderState;//発注状況
							if(OrderInfo.OrderState .equals("受付済")||OrderInfo.OrderState .equals("発注済")||OrderInfo.OrderState .equals("約定済")){
								OrderAgentLogWrite("I","買い株注文状態確認",OrderInfo);
								
								String Msg = "買い株注文番号確認完了:"+ MonitorOrderInfoList[i%10].OrderSeriesNum;
								System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
								TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
								
								break;
							}
						}
						if(i>100){
							OrderAgentLogWrite("E","株注文番号確認タイムアウト",OrderInfo);
							String Msg = "株注文番号確認タイムアウト:";
							System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
							TradeOperatorLogWrite(SubProcessName,"W",Msg,UserProperty);	
							
							//error時のやり直しを考える必要がある
							break;
						}
						try{
							Thread.sleep(50);
						}catch (InterruptedException e){
						}	
						i++;
					}
				}catch(Exception e){
					System.out.println( e);
					ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
					returnFlag = 1;
				}
			}
			else if(SimulationMode.equals("OFFLINE_SIMULATUION")||SimulationMode.equals("TEST_DATA_SIMULATION")){
			//---------------------擬似操作する場合-------------------------------------------------------------
				OrderInfo.StockName = target;	//株名
				OrderInfo.StockSeriesNum = target_num;//株シリアル番号
				OrderInfo.Ordertype = "BUY" ;//発注内容　BUY SELL
				OrderInfo.OrderPrice = OrderAction.Price;//発注金額
				OrderInfo.OrderNum = new BigDecimal(100);//発注数
				OrderInfo.OrderSeriesNum ="TEST";//発注番号を取得
				OrderAgentLogWrite("I","株注文状態確認",OrderInfo);
				
				String Msg = "買い株注文番号確認完了:"+ "TEST";
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
			
			}
			return returnFlag;
		}
		int OrderChangeExec(WebDriver driver_order, UserAction OrderAction, OrderInfo OrderInfo,OrderInfo[] MonitorOrderInfoList) {	
			String SubProcessName = "TESTOrderChangeExec ";
			int returnFlag=0;
			String OrderInfo_Row;
			int index=0;
			int flag = 0;
			//注文値段をチェックする必要がある
			
			//発注文なのに　すでに受け付け済みのものがあって注文変更の対応
			
			//チェックするロジックを書く
			
			//
			if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
				try{
				//---------------------注文一覧→注文訂正・取消ページに移動------------------------------------------------------
					driver_order.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();//株式取引ページに移動
					driver_order.findElement(By.xpath("//*[@id='gn_service-']/div[6]/div[2]/div/div/div[2]/div[1]/ul/li[2]/a")).click();//株式取引注文約定一覧ページに移動
					driver_order.findElement(By.xpath("//*[@id='form01']/div[3]/div[1]/dl[2]/dd[1]/span[2]")).click(); //昇順
					driver_order.findElement(By.xpath("//*[@id='form01']/div[3]/div[2]/div[1]/input")).click();//配置変換
					
					List<WebElement> childs = driver_order.findElements(By.xpath("//*[@id='form01']/table/tbody/tr"));
					for (WebElement e  : childs){
						OrderInfo_Row = e.getText();
						if (index%2 == 1){
							String[] tempStr;
							String[] tempInfo = OrderInfo_Row.split("\n");
						
							tempStr = tempInfo[4].split(" ");
							if (tempStr[1].equals(OrderInfo.OrderSeriesNum)){  //注文番号が一致
								String path = "//*[@id='form01']/table/tbody/tr[" + (index+1) + "]/td[8]/a";
								driver_order.findElement(By.xpath(path)).click(); //該当注文にクリック
								flag++ ;
								break;
							}
						}
						index++;
					}
					if(flag==1){
					//driver_order.findElement(By.xpath("//*[@id='form01']/table/tbody/tr[2]/td[8]/a")).click(); //注文約定一覧　取消/変更クリック　→注文訂正・取消ページに移動
						//----------------------------注文訂正内容--------------------------------------------
						driver_order.findElement(By.xpath("//*[@id='tbCorrectCancel']/tbody/tr[7]/td/div/div[2]/div/span[2]/label")).click(); //注文約定一覧　取消/変更クリック
						driver_order.findElement(By.xpath("//*[@id='idOrderPrc']")).sendKeys(OrderAction.Price.toString()); //買い注文　→金額記載
				
					//----------------------------注文実行--------------------------------------------
						driver_order.findElement(By.xpath("//*[@id='gn_stock-sm_order']/div[7]/div/form/div[1]/div[1]/div[2]/div[1]/input")).click(); //注文訂正・取消ページ　取消実行(次へ)		
						driver_order.findElement(By.xpath("//*[@id='gn_service-lm_order']/div[7]/div/form/div[2]/div[1]/div[2]/div[2]/input")).click(); //注文訂正確認　実行		
						
						OrderInfo.OrderPrice = OrderAction.Price;//発注金額変更
						
						int i=0;
						while(true){
							if(MonitorOrderInfoList[i%10].OrderSeriesNum.equals(OrderInfo.OrderSeriesNum )){
								OrderInfo.OrderState = MonitorOrderInfoList[i%10].OrderState;//発注状況
								if (OrderInfo.OrderState.equals("訂正中")||OrderInfo.OrderState.equals("訂正済")||OrderInfo.OrderState.equals("約定済") ){
									OrderAgentLogWrite("I","株注文更新確認",OrderInfo);
									
									String Msg = "株注文更新確認完了:"+ MonitorOrderInfoList[i%10].OrderSeriesNum;
									System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
									TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
									
									break;
								}
							}
							if(i>100){
								OrderAgentLogWrite("E","株注文番号確認タイムアウト",OrderInfo);
								break;
							}
							try{
								Thread.sleep(50);
							}catch (InterruptedException e){
							}	
							i++;
						}	
						OrderAgentLogWrite("I","注文金額変更済み",OrderInfo);
					}
					else if(flag ==0){
						String e = "該当注文が見当たらないが、変更注文を受けている:"+OrderInfo.OrderSeriesNum;
						System.out.println(e);
						ErrorLogWrite(ProcessName,SubProcessName, e);
					}
				}catch(Exception e){
					System.out.println( e);
					ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
					returnFlag = 2;
				}
			}
			else if(SimulationMode.equals("OFFLINE_SIMULATUION")||SimulationMode.equals("TEST_DATA_SIMULATION")){
				//---------------------擬似操作する場合-------------------------------------------------------------
					OrderInfo.OrderPrice = OrderAction.Price;//発注金額変更	
				}
			return returnFlag;
		}
		int SellActionExec(WebDriver driver_order, UserAction OrderAction,OrderInfo[] MonitorOrderInfoList ) {	
			String SubProcessName = "TradeOrder_SellActionExec ";
			int returnFlag=0;
			
			//注文値段をチェックする必要がある
			
			//チェックするロジックを書く
			
			//
			if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
				try{
				//---------------------注文ページに移動し発注する------------------------------------------------------
					driver_order.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();//株式取引ページに移動
				//driver_order.findElement(By.xpath("//*[@id='focuson']")).sendKeys(target_num); //株式取引→銘柄記入
					driver_order.findElement(By.xpath("//*[@id='gn_service-']/div[6]/div[2]/div/div/div[2]/div[1]/ul/li[1]/a")).click();;//保有残高・口座管理ページに移動
					driver_order.findElement(By.xpath("//*[@id='gn_custAsset-lm_custAsset']/div[7]/div/form[1]/table[1]/tbody/tr[2]/td[8]/a[2]")).click();;//注文 売り注文ページに移動
				
					driver_order.findElement(By.xpath("//*[@id='gn_service-lm_amount']/div[6]/div[2]/form/div[1]/div[1]/div[1]/table/tbody/tr[5]/td/div/input")).sendKeys("100"); //売り注文　→株数記載
					driver_order.findElement(By.xpath("//*[@id='gn_service-lm_amount']/div[6]/div[2]/form/div[1]/div[1]/div[1]/table/tbody/tr[6]/td/div[2]/span/label")).click();//売り注文　→　指値
					driver_order.findElement(By.xpath("//*[@id='idOrderPrc']")).sendKeys(OrderAction.Price.toString()); //売り注文　→金額記載
				
				//----------------------------注文実行--------------------------------------------
					driver_order.findElement(By.xpath("//*[@id='gn_service-lm_amount']/div[6]/div[2]/form/div[1]/div[1]/div[2]/div/input")).click();//売り注文（次へ）
					driver_order.findElement(By.xpath("//*[@id='gn_service-lm_amount']/div[7]/div[1]/form/div/div[1]/div[1]/div[2]/div[2]/input")).click();//内容確認　売り実施
				//-------------------------------------------------------------------------------
					OrderInfo.StockName = target;	//株名
					OrderInfo.StockSeriesNum = target_num;//株シリアル番号
					OrderInfo.Ordertype = "SELL" ;//発注内容　BUY SELL
					OrderInfo.OrderPrice = OrderAction.Price;//発注金額
					OrderInfo.OrderNum = new BigDecimal(100);//発注数
					OrderInfo.OrderSeriesNum = driver_order.findElement(By.className("com-block-num")).getText().replace("ご注文番号 ", "");//発注番号を取得
					
					int i=0;
					while(true){
						if(MonitorOrderInfoList[i%10].OrderSeriesNum.equals(OrderInfo.OrderSeriesNum )){
							OrderInfo.OrderState = MonitorOrderInfoList[i%10].OrderState;//発注状況
							if(OrderInfo.OrderState .equals("受付済")||OrderInfo.OrderState .equals("発注済")||OrderInfo.OrderState .equals("約定済")){
								OrderAgentLogWrite("I","売り株注文状態確認",OrderInfo);
								
								String Msg = "売り株注文番号確認:"+ MonitorOrderInfoList[i%10].OrderSeriesNum;
								System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
								TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
								break;
							}
						}
						if(i>100){
							OrderAgentLogWrite("E","株注文番号確認タイムアウト",OrderInfo);
							break;
						}
						try{
							Thread.sleep(50);
						}catch (InterruptedException e){
						}	
						i++;
					
					}
					
					OrderAgentLogWrite("I","売り注文発注済み",OrderInfo);
				}catch(Exception e){
					System.out.println( e);
					ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
					returnFlag = 3;
				}
			}
			else if(SimulationMode.equals("OFFLINE_SIMULATUION")||SimulationMode.equals("TEST_DATA_SIMULATION")){
			//---------------------擬似操作する場合-------------------------------------------------------------
				OrderInfo.StockName = target;	//株名
				OrderInfo.StockSeriesNum = target_num;//株シリアル番号
				OrderInfo.Ordertype = "SELL" ;//発注内容　BUY SELL
				OrderInfo.OrderPrice = OrderAction.Price;//発注金額
				OrderInfo.OrderNum = new BigDecimal(100);//発注数
				OrderInfo.OrderSeriesNum ="TEST";//発注番号を取得
				OrderAgentLogWrite("I","株注文状態確認",OrderInfo);
				
				String Msg = "売り株注文番号確認完了:"+ "TEST";
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
			}
			return returnFlag;
		}
		int OrderCancelExec(WebDriver driver_order, UserAction OrderAction,OrderInfo OrderInfo,OrderInfo[] MonitorOrderInfoList) {	
			String SubProcessName = "TESTOrderCancelExec ";
			int returnFlag=0;
			String OrderInfo_Row;
			int index=0;
			int flag = 0;
			
			if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
				try{
					driver_order.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();;//株式取引ページに移動
					driver_order.findElement(By.xpath("//*[@id='gn_service-']/div[6]/div[2]/div/div/div[2]/div[1]/ul/li[2]/a")).click();;//株式取引注文約定一覧ページに移動
					driver_order.findElement(By.xpath("//*[@id='form01']/div[3]/div[1]/dl[2]/dd[1]/span[2]")).click(); //昇順
					driver_order.findElement(By.xpath("//*[@id='form01']/div[3]/div[2]/div[1]/input")).click();//配置変換
						
					List<WebElement> childs = driver_order.findElements(By.xpath("//*[@id='form01']/table/tbody/tr"));
					for (WebElement e  : childs){
						OrderInfo_Row = e.getText();
						if (index%2 == 1){
							String[] tempStr;
							String[] tempInfo = OrderInfo_Row.split("\n");
						
							tempStr = tempInfo[4].split(" ");
							if (tempStr[1].equals(OrderInfo.OrderSeriesNum)){  //注文番号が一致
								String path = "//*[@id='form01']/table/tbody/tr[" + (index+1) + "]/td[8]/a";
								driver_order.findElement(By.xpath(path)).click(); //該当注文にクリック
								flag++ ;
								break;
							}
						}
						index++;
					}
					if(flag==1){
					
						//---------------------注文約定一覧から注文を取消する------------------------------------------------------
						driver_order.findElement(By.xpath("//*[@id='tbCorrectCancel']/tbody/tr[13]/td/span")).click(); //注文訂正・取消ページ　取消クリック			
						driver_order.findElement(By.xpath("//*[@id='gn_stock-sm_order']/div[7]/div/form/div[1]/div[1]/div[2]/div[1]/input")).click(); //注文訂正・取消ページ　取消実行(次へ)
						//------------------------------------------------------------------------------------------------
						int i=0;
						while(true){
							if(MonitorOrderInfoList[i%10].OrderSeriesNum.equals(OrderInfo.OrderSeriesNum )){
								OrderInfo.OrderState = MonitorOrderInfoList[i%10].OrderState;//発注状況
								if(OrderInfo.OrderState .equals("取消済")){
									
									OrderAgentLogWrite("I","注文キャンセル済み",OrderInfo);
									OrderProcState = "ORDER_CHANCELLED";
									
									String Msg = "株注文キャンセル完了:"+ "OrderInfo.OrderSeriesNum";
									System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
									TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
									
									break;
								}
								else if (OrderInfo.OrderState .equals("約定済")){ //取消するが取引が成立しまった場合
									OrderAgentLogWrite("W","成立により注文キャンセル失敗",OrderInfo);
									OrderProcState = "FINISHED";
									
									String Msg = "株注文キャンセル完了:"+ "OrderInfo.OrderSeriesNum";
									System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
									TradeOperatorLogWrite(SubProcessName,"W",Msg,UserProperty);	
									
									break;
									
								}
							}
							if(i>100){
								OrderAgentLogWrite("E","株注文番号確認タイムアウト",OrderInfo);
								String Msg = "株注文番号確認タイムアウト:";
								System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
								TradeOperatorLogWrite(SubProcessName,"W",Msg,UserProperty);	
								
								//error時のやり直しを考える必要がある
								break;
							}
							try{
								Thread.sleep(50);
							}catch (InterruptedException e){
							}	
							i++;
						}
					}
				}catch(Exception e){
					System.out.println( e);
					ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
					OrderAgentLogWrite("E","予期しないエラー："+SubProcessName,OrderInfo);
					returnFlag = 4;
				}
			}
			else if(SimulationMode.equals("OFFLINE_SIMULATUION")||SimulationMode.equals("TEST_DATA_SIMULATION")){
				OrderAgentLogWrite("I","注文キャンセル済み",OrderInfo);
				OrderProcState = "ORDER_CHANCELLED";
				
				String Msg = "株注文キャンセル完了:"+ "TEST";
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+Msg );
				TradeOperatorLogWrite(SubProcessName,"I",Msg,UserProperty);	
			}
			//-------------------------------------------------------------------------------
			return returnFlag;
		}
		void OrderAgentLogWrite(String Msg_type,String Msg,OrderInfo OrderInfo){ //create statics log file  時間　Userアクション	値段	Operatorアクション	値段　OperatorState	
			Calendar rightNow;
			Date Now = new Date();
			SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
			rightNow = Calendar.getInstance();
			Now = rightNow.getTime();
			String temp =  D.format(Now) +"	"+target+"	"+Msg_type+"	"+Msg;
			temp = temp +"	"+	OrderInfo.StockSeriesNum+"	"+	OrderInfo.StockName+"	"+	
					OrderInfo.Ordertype+"	"+	OrderInfo.OrderPrice+"	"+	OrderInfo.OrderNum+"	"+	
					OrderInfo.StockSeriesNum+"	"+	OrderInfo.OrderState+"\r\n";
			//更新待ち		
			OrderAgentLog.FileWrite(temp);
			
		}	
	}
	
	public class OrderMonitorUnit extends DefinedData{
		String SimulationMode ;
		String target;
		String target_num;
		String MonitorState = "STANDBY";//監視状況
		String MonitorAgentUnitState;
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
		
		//OrderInfo[] MonitorOrderInfoList = new OrderInfo[20];
		OrderInfo[] MonitorOrderInfoList = new OrderInfo[20];
		
		OrderMonitorUnit(String target,String target_num,WebDriver driver_monitor_Order,WebDriver driver_monitor_Property, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){

			String SubProcessName = "TradeMonitoringUnit_Initiation";
			this.MonitorAgentUnitState = "PREPARE";
			System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
			
			this.SimulationMode = SimulationMode;
			this.target = target;
			this.target_num = target_num;
			this.ErrorLog = ErrorLog;
			this.UserProperty = UserProperty;
			this.DecisionTradeStaticsData = TradeStatics;
			
			this.NewSellEntry 	= false;
			this.NewOrderEntry 	= false;
			this.Confirmed		= false;
			this.OrderState ="待機";
			
			if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
			
				for (int i =0 ; i <20;i++){ //注文一覧情報の保存先
					this.MonitorOrderInfoList[i] = new OrderInfo();
				}
				//for (int i =0 ; i <10;i++){
				//	UserProperty.HoldStockList.add(i,new HoldStockInfo());
				//}
					
				TradeMonitorOrderPageOpen(driver_monitor_Order);
				TradeMonitorPropertyPageOpen(driver_monitor_Property);
				//Test(driver_monitor);
			
			}
			else if(SimulationMode.equals("OFFLINE_SIMULATUION")||SimulationMode.equals("TEST_DATA_SIMULATION")){
				//UserProperty.Asset = new BigDecimal(1000000);
				//UserProperty.cash = new BigDecimal(1000000);
				
			}
			
			TradeMonitorAgentLog = new LogUnit(LogPath+"TradeOperatorUnit//trade//",this.target+"TradeMonitorAgent",0); // create log file

			MonitorAgentUnitState = "READY";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
		}	
		public void run(){ 
			String SubProcessName = "TradeMonitoringUnit_MainLoop ";
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
			String PreState = MonitorAgentUnitState;
			Calendar rightNow;
			
			while(!MonitorAgentUnitState.equals("END")){
				switch(MonitorAgentUnitState){
				
				case "READY":
					
					break;	
				case "START":	
					if (PreState.equals("READY")){
						//初回のプロセスの起動に使う
						PreState = MonitorAgentUnitState;
						System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
					}	
					
					if(SimulationMode.equals("ONLINE")||SimulationMode.equals("OPERATION_SIMULATION")){ 
						UserProperty.HoldStockList = PropertyCheck(UserProperty);			
						OrderCheck();
					}

					
					try{
						Thread.sleep(100);
					}catch (InterruptedException e){
					}
					
					//System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start");
					break;
				case "PAUSE":
					//System.out.println( "TradeOperatorUnit PAUSE");
					break;
				case "FINISHING":
					MonitorAgentUnitState = "END";
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
		void TradeMonitorOrderPageOpen(WebDriver driver_monitor_Order){
			String SubProcessName = "TradeMonitorOrderPageOpen ";
			driver_monitor_Order.get(ADDRESS);
			Login(driver_monitor_Order, UserProperty.USER_NAME, UserProperty.PASSWORD);	
			driver_monitor_Order.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();;//株式取引ページに移動
			driver_monitor_Order.findElement(By.xpath("//*[@id='gn_service-']/div[6]/div[2]/div/div/div[2]/div[1]/ul/li[2]/a")).click();;//株式取引注文約定一覧に移動
			
		}
		void TradeMonitorPropertyPageOpen(WebDriver driver_monitor_Property){
			String SubProcessName = "TradeMonitorPropertyPageOpen ";
			driver_monitor_Property.get(ADDRESS);
			Login(driver_monitor_Property, UserProperty.USER_NAME, UserProperty.PASSWORD);	
			driver_monitor_Property.findElement(By.xpath("//*[@id='navi-header-sub']/div[1]/ul/li[1]/a")).click();;//株式取引ページに移動
			driver_monitor_Property.findElement(By.xpath("//*[@id='gn_service-']/div[6]/div[2]/div/div/div[2]/div[1]/ul/li[1]/a")).click();;//保有残高・売却に移動
				
		}

		void OrderCheck(){//定期的に注文履歴の状況を更新
			String SubProcessName = "TradeMonitor_OrderCheck ";
			driver_monitor_Order.findElement(By.xpath("//*[@id='form01']/p[1]/a")).click();
			driver_monitor_Order.findElement(By.xpath("//*[@id='form01']/div[3]/div[1]/dl[2]/dd[1]/span[2]")).click(); //昇順
			driver_monitor_Order.findElement(By.xpath("//*[@id='form01']/div[3]/div[2]/div[1]/input")).click();//配置変換
			
			String OrderInfo_title;
			String OrderInfo_Row;
			int index=0;
			
			List<WebElement> childs = driver_monitor_Order.findElements(By.xpath("//*[@id='form01']/table/tbody/tr"));
			try{

				for (WebElement e  : childs)
				{
					OrderInfo_Row = e.getText();
					if (!OrderInfo_Row.contains("株式注文はありません")){
						if (index%2 == 1){
							//OrderInfo_title = driver_monitor.findElement(By.xpath("//*[@id='form01']/table/tbody/tr[1]")).getText();
							//OrderInfo_Row = driver_monitor.findElement(By.xpath("//*[@id='form01']/table/tbody/tr[2]")).getText();
							String[] tempStr;
							String[] tempInfo = OrderInfo_Row.split("\n");
					
							int tempIndex = (index-1)/2;
					
							MonitorOrderInfoList[tempIndex].StockName = tempInfo[0]; //銘柄名
					
							tempStr = tempInfo[1].split(" "); 
							MonitorOrderInfoList[tempIndex].StockSeriesNum  = tempStr[0]; //銘柄番号
					
							tempStr = tempInfo[3].split(" "); 
							MonitorOrderInfoList[tempIndex].Ordertype =  tempStr[0]; //売買
							tempStr[1] = tempStr[1].replace("株", "");
							MonitorOrderInfoList[tempIndex].OrderNum =  new BigDecimal(tempStr[1]);  //株数
					
							tempStr = tempInfo[4].split(" ");
							tempStr[0] = tempStr[0].replace(",", "");
							tempStr[0] = tempStr[0].replace("円", "");
							MonitorOrderInfoList[tempIndex].OrderPrice = new BigDecimal(tempStr[0]); //価格
							MonitorOrderInfoList[tempIndex].OrderSeriesNum = tempStr[1].replace("ご注文番号 ", ""); //注文番号　//変更する項目か分からない
							MonitorOrderInfoList[tempIndex].OrderState = tempInfo[7]; //注文状態
						}
					}
					index++;
				}
				System.out.println(MonitorOrderInfoList[0].OrderNum+"	"+MonitorOrderInfoList[0].OrderSeriesNum+"	"+MonitorOrderInfoList[0].OrderState);
				
			}catch(Exception e){
				System.out.println( e);
				ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
			}
				//System.out.println(OrderInfo_title);
				//System.out.println(OrderInfo_Row);
				
				//新しい注文に対する対応状況を確認するロジック
		}	
		List<HoldStockInfo> PropertyCheck(UserProperty UserProperty){
			String temp;
			String OrderInfo_Row;
			int index=0;
			int flag = 0;
			
			temp = driver_monitor_Property.findElement(By.xpath("//*[@id='gn_custAsset-lm_custAsset']/div[7]/div/form[1]/div[1]/div")).getText();
			String[] tempStr;
			String[] tempInfo = temp.split(" ");			
			UserProperty.Asset = new BigDecimal(tempInfo[1].replace("円", "").replace(",", "")); //所持現金情報を取得
			
			temp = driver_monitor_Property.findElement(By.xpath("//*[@id='gn_custAsset-lm_custAsset']/div[7]/div/form[1]/div[2]/div")).getText();
			tempInfo = temp.split(" ");
			UserProperty.cash = new BigDecimal(tempInfo[1].replace("円", "").replace(",", "")); //所持現金情報を取得

			List<WebElement> TempHoldStockList;
			List<HoldStockInfo> HoldStockList = new ArrayList<HoldStockInfo>() ;
			
			TempHoldStockList = driver_monitor_Property.findElements(By.xpath("//*[@id='gn_custAsset-lm_custAsset']/div[7]/div/form[1]/table[1]/tbody/tr"));
			for (WebElement e  : TempHoldStockList){
				OrderInfo_Row = e.getText();
				if (index%2 == 1){
					tempStr = OrderInfo_Row.split("\n");
					//HoldStockList = tempStr[0]; //銘柄名 
					if (tempStr[1].contains(target_num)){  //注文番号が一致
						HoldStockInfo tmp = new HoldStockInfo();
						tmp.StockName = tempStr[0];
						tmp.StockSeries = (tempStr[1].split(" "))[0];
						String[] tmpStr1 = tempStr[4].split(" ");
						tmp.PurchasePrice = new BigDecimal(tmpStr1[1]);
						tmp.StockNum =  new BigDecimal(tmpStr1[2].replace("株", ""));
						HoldStockList.add(tmp);
						
						flag++ ;
						break;
					}
				}
				index++;
			}
			return HoldStockList;
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
	
	public class OrderPanelUnit extends JFrame {
		private static final long serialVersionUID = 1L;
		JPanel contentPane = new JPanel();
		BorderLayout borderLayout1 = new BorderLayout();
		JTextField result = new JTextField(""); //計算結果を表示するテキストフィールド
		UserProperty UserProperty;
		
		int index =0; 
		
		OrderPanelUnit(UserProperty UserProperty){ //初期化

			String SubProcessName = "OrderPanelUnit_Initiation";
			System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );
		
			this.UserProperty = UserProperty;

			contentPane.setLayout(borderLayout1);
			this.setSize(new Dimension(250, 300));
			this.setTitle("電子式卓上計算機");
			this.setContentPane(contentPane);

			contentPane.add(result, BorderLayout.NORTH); //テキストフィールドを配置

			JPanel keyPanel = new JPanel(); //ボタンを配置するパネルを用意
			keyPanel.setLayout(new GridLayout(5, 1)); //4行4列のGridLayoutにする
			contentPane.add(keyPanel, BorderLayout.CENTER);

			keyPanel.add(new NumberButton("Buy 134","BUY",index, 134)); //ボタンをレイアウトにはめこんでいく
			keyPanel.add(new NumberButton("Buy 136","BUY",index, 136));
			keyPanel.add(new NumberButton("Sell 134","SELL",index, 134));
			keyPanel.add(new NumberButton("Sell 136","SELL",index, 136));
			//keyPanel.add(new CancelButton("Cancel","Cancel"));
	
			
			
			contentPane.add(new JButton("C"), BorderLayout.SOUTH);//Cボタンを配置する
			this.setVisible(true);

	
			System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
		}
		public class NumberButton extends JButton implements ActionListener {
			private static final long serialVersionUID = 1L;	
			String command;
			double price;
			
			public NumberButton(String keyTop,String command ,int index,double price) {
				super(keyTop); //JButtonクラスのコンストラクタを呼び出す
				this.command = command;
				this.price = price;
				this.addActionListener(this); //このボタンにアクションイベントのリスナを設定
			}

			public void actionPerformed(ActionEvent evt) {
				String keyNumber = this.getText(); //ボタンの名前を取り出す
				index++;
				UserProperty.UserAction.NewOrder =true;
				UserProperty.UserAction.ActionIndex = index;
				UserProperty.UserAction.Action[0] = command;
				UserProperty.UserAction.ActionNum = 1;
				UserProperty.UserAction.Price = new BigDecimal(price);
				UserProperty.UserAction.OrderStockNum =  new BigDecimal(100);
				UserProperty.UserAction.result = "New order";
				
				System.out.println(index +"	"+keyNumber );	
			}
		}
		public class CancelButton extends JButton implements ActionListener {
			private static final long serialVersionUID = 1L;
			String command;

			
			public CancelButton(String keyTop, String command) {
				super(keyTop); //JButtonクラスのコンストラクタを呼び出す
				this.command = command;
				this.addActionListener(this); //このボタンにアクションイベントのリスナを設定
			}

			public void actionPerformed(ActionEvent evt) {
				String keyNumber = this.getText(); //ボタンの名前を取り出す
				index++;
				UserProperty.UserAction.NewOrder =true;
				UserProperty.UserAction.ActionIndex = index;
				UserProperty.UserAction.Action[0] = command;
//				UserProperty.UserAction.ActionNum = 1;
				
				System.out.println(keyNumber);	
			}
		}
}
	
	void Login(WebDriver driver,String user_name, String password) {	
		//---------------------Login ------------------------------------------------------
			driver.findElement(By.name("loginid")).sendKeys(user_name);
			driver.findElement(By.name("passwd")).sendKeys(password);
			driver.findElement(By.className("text-button")).click();
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

	void TradeOperatorLogWrite(String SubProcess,String Msg_type,String Msg,UserProperty UserProperty){ //create statics log file  時間　Userアクション	値段	Operatorアクション	値段　OperatorState	
		Calendar rightNow;
		Date Now = new Date();
    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
    	rightNow = Calendar.getInstance();
		Now = rightNow.getTime();
	
	
		String temp =  D.format(Now) +"	"+target+"	"+SubProcess+"	"+Msg_type +"	"+Msg+"	index:";
		temp = temp + UserProperty.UserAction.ActionIndex+"	"+UserProperty.UserAction.Action[0]+"	"+OperatorState+"	"+UserProperty.UserAction.Price+"	"+UserProperty.UserAction.OrderStockNum+"	"+UserProperty.UserAction.result + "\r\n";
		//temp = temp + OrderAgentUnit.OrderAgentAction.ActionIndex+"	"+ OrderAgentUnit.OrderAgentAction.Action[0]+"	"+ OrderAgentUnit.OrderAgentAction.Action[1]+
		//		"	"+ OrderAgentUnit.OrderAgentAction.Price+"	"+OrderAgentUnit.OrderAgentAction.OrderStockNum+"\r\n";
		TradeOperatorLog.FileWrite(temp);
	
	}
	void LoadHoldingStockInfoLoader(String target ,String LogPath, UserProperty UserProperty){ //　HoldStockInfolog file 読み込み  時間　Userアクション	値段	Operatorアクション	値段　OperatorState	
		String SubProcessName = "LoadHoldingStockInfoLoader";	
	    try{
	    	HoldStockInfoFile = new File(LogPath+"//TradeOperatorUnit//trade//"+target+"_HoldingStock.txt");
    		FileDataBuffer = new BufferedReader(new FileReader(HoldStockInfoFile)); //9用確認
	    	Boolean fin = false;
	    	
	    	String temp;
	    	
	    	while((temp= FileDataBuffer.readLine())!=null){

	    		String[] tempStr = temp.split(" ");
	    		    		
	    		if (tempStr.length>1){
	    			if(tempStr[0].equals("Asset")){
	    				UserProperty.Asset = new BigDecimal(tempStr[1]);
	    			}
	    			if(tempStr[0].equals("Cash")){
	    				UserProperty.cash = new BigDecimal(tempStr[1]);
	    			}
	    			
	    			if (tempStr[1].equals(target)){
	    				int flag =0;
	    				HoldStockInfo tempHoldStockInfo =  new HoldStockInfo();
					
	    				if(!UserProperty.HoldStockList.isEmpty()){//Listになにもないのでそのまま追加
						
	    					for(int i=0;i<UserProperty.HoldStockList.size();i++){
							
	    						tempHoldStockInfo = UserProperty.HoldStockList.get(i);//該当所持株情報抽出
							
	    						if(tempHoldStockInfo.StockName.equals(target)){ // 一致する所持株確認							
	    							tempHoldStockInfo.StockName = tempStr[1];
	    							tempHoldStockInfo.StockSeries = tempStr[2];
	    							tempHoldStockInfo.StockNum = new BigDecimal(tempStr[3]);
	    							tempHoldStockInfo.SumPrice = new BigDecimal(tempStr[4]);
	    							tempHoldStockInfo.PurchasePrice = new BigDecimal(tempStr[5]);
	    							
	    							if(tempHoldStockInfo.StockNum.compareTo(BigDecimal.ZERO)==0){
										UserProperty.HoldStockList.remove(i); //所持株がなくなるため削除
									}
									else{
										UserProperty.HoldStockList.set(i,tempHoldStockInfo);
									}
	    							flag++;
	    						}	
	    					}
	    				}
	    				if(flag ==0){ //empty OR　リストに所持株情報が無い　新規追加
												
	    					tempHoldStockInfo.StockName = tempStr[1];
	    					tempHoldStockInfo.StockSeries = tempStr[2];
		    				tempHoldStockInfo.StockNum = new BigDecimal(tempStr[3]);
		    				tempHoldStockInfo.SumPrice = new BigDecimal(tempStr[4]);
		    				tempHoldStockInfo.PurchasePrice = new BigDecimal(tempStr[5]);
	    				
		    				UserProperty.HoldStockList.add(tempHoldStockInfo);//持ち株リストを追加
	    				}
	    			}
	    		}
	    	}
	    }catch(Exception e){
	   		System.out.println("DataFileLoader error" );
	   		e.printStackTrace();
	   		ErrorLogWrite(ProcessName, SubProcessName , e.toString());
	   	}
	    System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"DataFileLoading_Finished" );
	
	
	}
	void HoldStockInfoLogWrite(UserProperty UserProperty){ // HoldStockInfolog file 書き込み  時間　Userアクション	値段	Operatorアクション	値段　OperatorState	

		List<HoldStockInfo> tempList = UserProperty.HoldStockList;
		HoldStockInfo	tempHoldStockInfo;
		
		String temp;
		
		Calendar rightNow;
		Date Now = new Date();
    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    	rightNow = Calendar.getInstance();
		Now = rightNow.getTime();
		
		temp =  D.format(Now)+"\r\n";
		temp = temp + "Asset "+ UserProperty.Asset+"\r\n";
		temp = temp + "Cash "+UserProperty.cash+"\r\n";
		HoldingstockLog.FileWrite(temp);
		int i=0;
		if(!UserProperty.HoldStockList.isEmpty()){//Listになにもないものならエラー対応が必要
			for(i=0;i<UserProperty.HoldStockList.size();i++){
	
				tempHoldStockInfo = tempList.get(i);//該当所持株情報抽出
					
				temp =i+1+" "+ tempHoldStockInfo.StockName+" "+tempHoldStockInfo.StockSeries +" "+
							tempHoldStockInfo.StockNum+" "+tempHoldStockInfo.SumPrice+" "+tempHoldStockInfo.PurchasePrice+"\r\n";
				HoldingstockLog.FileWrite(temp);
						
				}
		}
		else{
			HoldingstockLog.FileWrite( i +" NULL\r\n");
			
		}
		HoldingstockLog.FileWrite("END\r\n");
		
	}
	
	
}

