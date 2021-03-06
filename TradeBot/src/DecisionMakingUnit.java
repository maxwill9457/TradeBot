import java.util.*;

import javax.swing.JFrame;

import java.math.BigDecimal;
import java.text.*;
import org.jfree.ui.RefineryUtilities;



public class DecisionMakingUnit extends DefinedData{// 意思決定   Trade情報により、買、維持、売の行動　また価格を決める
	
	String ProcessName = "DecisionMakingUnit";
	String SimulationMode;
	int Speed;
	String DecisionMakingUnitState;
	String target;
	String LogPath;
	
	BoardInfo BoardInfo;
	UserProperty UserProperty;
	TradeStatics TradeStatics;
	
	MindModuleUnit MindModule;
	CatchException MindModule_catchException;
	
	ShowMeigaraTimechart ShowMeigaraTimechart;
	
	RecordSeriesExtract RecordSeriesExtract;
	java.util.Timer HistoryExtractTimer;
	
	LogUnit StaticsLog; // create statics log file
	LogUnit DailyLog;
	LogUnit ErrorLog;
	
	DecisionMakingUnit(String target,String target_num,BoardInfo BoardInfo, UserProperty UserProperty,TradeStatics TradeStatics,LogUnit ErrorLog,String SimulationMode,String LogPath, int Speed){
		
		String SubProcessName = "Initiation";
		DecisionMakingUnitState = "PREPARE";
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Activating" );

		this.SimulationMode = SimulationMode;
		this.Speed = Speed;
		this.target = target;
		this.LogPath = LogPath;
		this.BoardInfo = BoardInfo;
		this.UserProperty = UserProperty;
		this.TradeStatics = TradeStatics;
		
		try{
			StaticsLog = new LogUnit(LogPath+"DecisionMakingUnit//statics//", this.target+"_Statics",1); // create statics log file	
			DailyLog = new LogUnit(LogPath, this.target+"_Daily",0); // create log file to record MindModule result
			this.ErrorLog = ErrorLog;
			//ActionDecisionLog = new LogUnit(this.target); // create log file to record ActionDecision result
		
			MindModule = new MindModuleUnit(	target,
												target_num,
												BoardInfo,
												UserProperty,
												TradeStatics,
												this.ErrorLog,
												LogPath,
												SimulationMode);// create mind module access
			MindModule_catchException = new CatchException();
			MindModule.setName("Thread-WebAccess-"+target);
			MindModule.setUncaughtExceptionHandler(MindModule_catchException);
			MindModule.start();
		
			ShowMeigaraTimechart = new ShowMeigaraTimechart(target+"タイムチャート" ,
															this.BoardInfo.Date,
															this.BoardInfo.BoardTime,
															ErrorLog );
			
			RecordSeriesExtract = new RecordSeriesExtract();
			HistoryExtractTimer= new java.util.Timer(true);
			HistoryExtractTimer.schedule(RecordSeriesExtract,0,600);//シミュレーション速度にあわせてログ取得
			
			ShowMeigaraTimechart.pack();
			RefineryUtilities.centerFrameOnScreen(ShowMeigaraTimechart);
			ShowMeigaraTimechart.setVisible(true);
		}catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
			
		DecisionMakingUnitState = "READY";
		System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Ready" );	
	}
		
	public void run(){ 
		String SubProcessName = "Main_Loop ";
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Standby" );
		String PreState = DecisionMakingUnitState;
		String PreTime ="0";
		
		while(!DecisionMakingUnitState.equals("END")){
			switch(DecisionMakingUnitState){
			
			case "READY":
				//System.out.println( "DecisionMakingUnit READY");
				break;	
			case "START":	
				if (PreState.equals("READY")){
					//初回のプロセスの起動に使う
					PreState = DecisionMakingUnitState;
					MindModule.MindModuleUnitState = "START";	
					LogTitleInitial();
					System.out.println( target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Start" );
				}	
				
				//------------------renew----------------------
				
				int flag = 0; 	
				
				try{
					synchronized (BoardInfo.BoardInfoLock){
						if ( BoardInfo.Price !=null ){
							flag = 1;
						}
					}	
					if (flag==1){
						//統計量計算
						StockTreand();
						MarketTrend();
						TradeBoardTrend();
							
						if(BoardInfo.StockStatus.equals("Trade_Avaliable") && 
								(BoardInfo.MarketStatus.equals("FIRST_HALF") || BoardInfo.MarketStatus.equals("SECOND_HALF"))){
							//---------取引決定------------------------
							if (UserProperty.UserAction.ActionScore >9950.0
								&& UserProperty.UserAction.Action.equals("NONE") 
								&& !UserProperty.Holded.equals("HOLDED") ){
								// &&UserProperty.cash >  TradeStatics.PresentPrice ){ //買	すでに購入状態だと購入しない
								synchronized (UserProperty.UserPropertyLock){
									UserProperty.UserAction.Action[0]= "BUY";
									UserProperty.UserAction.Price = TradeStatics.PresentPrice; //
									UserProperty.UserAction.OrderStockNum = new BigDecimal(100.0); 
								}
							}
							else if(UserProperty.UserAction.ActionScore < 50
									&& UserProperty.UserAction.Action.equals("NONE") 
									&& !UserProperty.Holded.equals("NONE")){
									//){ //買	すでに買い担っていなければ){//売り	
								synchronized (UserProperty.UserPropertyLock){
									UserProperty.UserAction.Action[0]= "SELL";
									UserProperty.UserAction.Price = TradeStatics.PresentPrice;
									UserProperty.UserAction.OrderStockNum = new BigDecimal(100.0); 
								}
							}						
						}
							
							//--------時系列チャートを作成-------------
						if(PreTime.equals("0")){  //初回目の動的平均値
							PreTime = BoardInfo.BoardTime;
							System.out.println("TimechartRenew initial time stamp");
							ShowMeigaraTimechart.TimechartRenew(TradeStatics.PresentPrice,
																TradeStatics.PriceChange_Online_Avg+TradeStatics.PriceOpen.doubleValue(),
																TradeStatics.BuyTrend,TradeStatics.SellTrend,
																BoardInfo.Date,BoardInfo.BoardTime,TradeStatics.Dekitaka_Change,TradeStatics.VWAP);
						}
						else if (!PreTime.equals(BoardInfo.BoardTime)) {// 同一時間だとスキップ
							System.out.println("value"+TradeStatics.PresentPrice);
							ShowMeigaraTimechart.TimechartRenew(TradeStatics.PresentPrice,
																TradeStatics.PriceChange_Online_Avg+TradeStatics.PriceOpen.doubleValue(),
																TradeStatics.BuyTrend,TradeStatics.SellTrend,
																BoardInfo.Date,BoardInfo.BoardTime,TradeStatics.Dekitaka_Change,TradeStatics.VWAP);
							PreTime = BoardInfo.BoardTime;
							System.out.println( target+ "	"+ProcessName+"_"+SimulationMode+"_"+"Decision Start");
						}
							//--------------------------
						StaticsLogWrite();
					}else{
						System.out.println("off");
					}
					
				}catch (Exception e){
					System.out.println(e +" DecisionMark");
					ErrorLogWrite(ProcessName, SubProcessName+"START" ,e.toString());
				}
					

				//------------------validate-------------------
				
				//MindModule.MindModule();
				
				//------------------action--------------------- 
				
				//System.out.println( "DecisionMakingUnit START");
				break;
			case "PAUSE":
				//System.out.println( "DecisionMakingUnit PAUSE");
				break;
			case "FINISHING":
				//---------------気配板プロセスの完了待つ-----------------------------	
				MindModule.MindModuleUnitState = "FINISHING";
				while(!MindModule.MindModuleUnitState.equals("END")){
					try{
						Thread.sleep(10);
					}catch (InterruptedException e){
					}
				}
				//------------------------------------------------------------
				
				HistoryExtractTimer.cancel();
				HistoryExtractTimer = null;
				
				DaliyLogWrite(); // 一日の株結果を記録
				String TempDate = BoardInfo.Date.replaceAll("/", "");
				ShowMeigaraTimechart.TimechartSave(LogPath,target+"_"+TempDate); // 株遷移図を記録
				ShowMeigaraTimechart.dispose();
				ShowMeigaraTimechart = null;
				
				
						
				System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+SimulationMode+"_"+"Finish");
				DecisionMakingUnitState = "END";
				break;
			case "ERROR":	
				//System.out.println( "DecisionMakingUnit ERROR");
				break;
				
			}	
			try{
				Thread.sleep(500);
			}catch (InterruptedException e){
				ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
			}	
		}	
		System.out.println(target+ "	"+ProcessName+"_"+SubProcessName+"_"+"End" );
	}
	
	class PropertyManager extends DefinedData{
		
		
	}
	
	public void StockTreand(){
		//株状態識別　ストップ高　ストップ安　株高　株安
		String SubProcessName = "StockTreand_Statics_renew";
		try{
			synchronized (BoardInfo.BoardInfoLock){
				
				TradeStatics.StaticsNumber ++;
				
				TradeStatics.PresentPrice 		= new BigDecimal(BoardInfo.Price.replaceAll(",", "")); // 計算のためString からintに変換
				TradeStatics.PresentPriceChange = new BigDecimal(BoardInfo.NetChange);
				
				if(!TradeStatics.PresentPrice.equals(0))
					TradeStatics.PriceChangePercentage = TradeStatics.PresentPriceChange.divide(TradeStatics.PresentPrice);
				else{
					System.out.println(SubProcessName+"divided by 0");
					ErrorLogWrite(ProcessName, SubProcessName ,"PriceChangePercentage divided by 0");
				}
				
				
				TradeStatics.PriceOpen 			= new BigDecimal(BoardInfo.PriceOpen);  //開場株価格
				
				
				if(BoardInfo.Dekitaka.equals("—")){
					TradeStatics.Dekitaka_Change =	new BigDecimal(0.0);
					TradeStatics.Dekitaka = new BigDecimal(0.0);
				}else{
					TradeStatics.Dekitaka_Change 	= new BigDecimal(BoardInfo.Dekitaka).subtract(TradeStatics.Dekitaka); //更新するたびの出来高の差分		
					TradeStatics.Dekitaka 			= new BigDecimal(BoardInfo.Dekitaka);  //開場からの出来高
				}
				if(BoardInfo.VWAP.equals("—")){
					TradeStatics.VWAP=  TradeStatics.PresentPrice;
				}else{
					TradeStatics.VWAP = new BigDecimal(BoardInfo.VWAP);  //VWAP
				}
			}
		//}catch (NullPointerException e){
		//	System.out.println(e +" "+SubProcessName);
		//	ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
		//}	
		//-----------当日高値、低値-----------------
		
		//if (TradeStatics.HighestPrice < TradeStatics.PresentPrice || TradeStatics.HighestPrice ==0){
		//	TradeStatics.HighestPrice = TradeStatics.PresentPrice;  //最高値更新
		//}
		SubProcessName = "StockTreand_Statics_calculate";
		//try{
			if (TradeStatics.HighestPrice.compareTo(TradeStatics.PresentPrice) < 0  || TradeStatics.HighestPrice.doubleValue() ==0){
				TradeStatics.HighestPrice = TradeStatics.PresentPrice;  //最高値更新
			}
			//if (TradeStatics.LowestPrice > TradeStatics.PresentPrice || TradeStatics.LowestPrice ==0){
			//	TradeStatics.LowestPrice = TradeStatics.PresentPrice;	//最低値更新	
			//}
			if (TradeStatics.LowestPrice.compareTo(TradeStatics.PresentPrice) > 0  || TradeStatics.LowestPrice.doubleValue() ==0){
				TradeStatics.LowestPrice = TradeStatics.PresentPrice;	//最低値更新	
			}
			//-----------異動平均------------------
			if(TradeStatics.PriceChange_Online_Avg==0){ //中時間間隔平均
				TradeStatics.PriceChange_Online_Avg = TradeStatics.PresentPriceChange.doubleValue();  // 値段変化平均値の初期値
			}
			else{
				TradeStatics.PriceChange_Online_Avg = TradeStatics.PriceChange_Online_Avg*TradeStatics.PFactor 
											+ TradeStatics.PresentPriceChange.doubleValue() *(1 - TradeStatics.PFactor);
			}
			
			//-----------ストップ高判定------------------
			if(TradeStatics.PresentPriceChange.doubleValue() == BoardInfo.PriceRange){ //ストップ高かストップ安の判断  
				BoardInfo.StockStatus = "Buy_Lock";
			}
			else if (-1*TradeStatics.PresentPriceChange.doubleValue() == BoardInfo.PriceRange){
				BoardInfo.StockStatus = "Sell_Lock";
			}
			else{
				BoardInfo.StockStatus = "Trade_Avaliable";
			}
			//-----------株の変動状況-------------------
			if(TradeStatics.PresentPriceChange.doubleValue() > 0){ //ストップ高かストップ安の判断
				TradeStatics.PriceTrend = "Rising";
			}
			else if (TradeStatics.PresentPriceChange.doubleValue() < 0){
				TradeStatics.PriceTrend = "Dropping";
			}
			else if(TradeStatics.PresentPriceChange.doubleValue() == 0){
				TradeStatics.PriceTrend = "Keeping";
			}
		}catch (Exception e){
			System.out.println(e +" "+SubProcessName);
			ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
		}
	}

	public void MarketTrend(){
		//市場平均変化率
		String SubProcessName = "MarketTreand_Statics_calculate";
		try{
		
			synchronized (BoardInfo.BoardInfoLock){
				
				TradeStatics.PresentMarket = new BigDecimal(BoardInfo.Market);//現在の日経平均価格			
				TradeStatics.PresentMarketChange =new BigDecimal(BoardInfo.MarketNetChange); //日経平均の上昇落下価格
				if(!TradeStatics.PresentMarket.equals(0))
					TradeStatics.MarketChangePercentage = TradeStatics.PresentMarketChange.divide(TradeStatics.PresentMarket);
				else{
					System.out.println(SubProcessName+"divided by 0");
					ErrorLogWrite(ProcessName, SubProcessName ,"MarketChangePercentage divided by 0");
				}
				
			}
			if(TradeStatics.MarketChange_Online_Avg==0){ //中時間間隔平均
				TradeStatics.MarketChange_Online_Avg = TradeStatics.PresentMarketChange.doubleValue(); //平均値の初期化 
			}
			else{
				TradeStatics.MarketChange_Online_Avg = TradeStatics.MarketChange_Online_Avg*TradeStatics.MFactor 
											+ TradeStatics.PresentMarketChange.doubleValue() *(1 - TradeStatics.MFactor);
			}
			
			//-----------日経平均の変動状況-------------------
			if(TradeStatics.PresentMarketChange.doubleValue() > 0){ //ストップ高かストップ安の判断
				TradeStatics.MarketTrend = "Rising";
			}
			else if (TradeStatics.PresentMarketChange.doubleValue() < 0){
				TradeStatics.MarketTrend = "Dropping";
			}
			else if (TradeStatics.PresentMarketChange.doubleValue() == 0){
				TradeStatics.MarketTrend = "Keeping";
			}
		}catch (Exception e){
			System.out.println(e +" "+SubProcessName);
			ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
		}
	}
	
	public void TradeBoardTrend(){
		
		String SubProcessName = "TradeBoardTrend_Statics_calculate";
		try{
		
			synchronized (BoardInfo.BoardInfoLock){
				//for(int i = 0 ; i < 23; i++){
					 // 成り行き
					TradeStatics.NariyukiBuy = new BigDecimal(BoardInfo.Board[0][0]);//現在の成り行き買
					TradeStatics.NariyukiSell =  new BigDecimal(BoardInfo.Board[0][2]);//現在の成り行き売り
					
					//現在の気配板の価格以外で買い、売りたい人
					TradeStatics.OverSell = new BigDecimal(BoardInfo.Board[1][0]);// 現在の気配板の価格以上で売る人
					TradeStatics.UnderBuy = new BigDecimal(BoardInfo.Board[22][2]);// 現在の気配板の価格以以下で買う人
					
					BigDecimal TempBuyTrend  = new BigDecimal(0.0);
					BigDecimal TempSellTrend = new BigDecimal(0.0);
					
					for(int i = BoardInfo.SellIndex; i > 1; i--){
						
						TradeStatics.Board[i][0] =  new BigDecimal(BoardInfo.Board[i][0]);//現在のボード注文情報
						TradeStatics.Board[i][1] =  new BigDecimal(BoardInfo.Board[i][1]);//注文値段
						TempSellTrend = TempSellTrend.add(TradeStatics.Board[i][0]);
						
					}
					TradeStatics.SellTrend = TempSellTrend.add(TradeStatics.OverSell);
					for(int i = BoardInfo.BuyIndex; i < 22; i++){
						TradeStatics.Board[i][2] =  new BigDecimal(BoardInfo.Board[i][2]);//現在のボード注文情報
						TradeStatics.Board[i][1] =  new BigDecimal(BoardInfo.Board[i][1]);//注文値段
						TempBuyTrend = TempBuyTrend.add(TradeStatics.Board[i][2]);
		
							//String tempUnderBuy = BoardInfo.Board[i][2].replaceAll(",", "");//現在の気配板の価格以下で買う人
							//TradeStatics.UnderBuy = Double.parseDouble(tempUnderBuy);// 計算のためString からdoubleに変換			
					}	
					TradeStatics.BuyTrend = TempBuyTrend.add(TradeStatics.UnderBuy);
				//}		
			}
		}catch (Exception e){
			System.out.println(e +" "+SubProcessName);
			ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
		}
	}
	
	void LogTitleInitial(){
		//Log Label 
		String temp; 
		temp =  "StaticsNumber	DataNumber	yyyy/MM/dd	HH:mm:ss.SSS	";
		temp = temp + "MarketChange_Online_Avg	PriceChange_Online_Avg	OverSell	TradeStatics.OverSell	";
		temp =temp + "Sell Board[i][0]"; 
		temp = temp + "Overbuy	TradeStatics.OverBuy	"; 		
		temp =temp + "buy Board[i][0]";
		
		temp  = temp + "\r\n";
		StaticsLog.FileWrite(temp);	
	}
	
	public void StaticsLogWrite(){
		String SubProcessName = "StaticsLogWrite";
    	Calendar rightNow;
    	Date Now = new Date();
    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
    	rightNow = Calendar.getInstance();
		Now = rightNow.getTime();
		
		String temp = TradeStatics.StaticsNumber +"	"+ BoardInfo.DataNumber+"	"+D.format(Now)+"	"; 
		
		temp =temp + TradeStatics.MarketChange_Online_Avg +"	"+ TradeStatics.PriceChange_Online_Avg +"	";		
		
		temp =temp + "OverSell	" + TradeStatics.OverSell.toString() +"	Sell	";

		for(int i = BoardInfo.SellIndex; i > 1; i--){
			temp =temp + TradeStatics.Board[i][0].toString()+"	"; 
			temp =temp + TradeStatics.Board[i][1].toString()+"	"; 
		}
		
		temp = temp + "UnderBuy	" + TradeStatics.UnderBuy +"	buy	"; 
		
		for(int i = BoardInfo.BuyIndex; i < 22; i++){
			temp = temp +TradeStatics.Board[i][2].toString()+"	"; 
			temp = temp +TradeStatics.Board[i][1].toString()+"	"; 	
		}	
		
		temp  = temp + "\r\n";
		StaticsLog.FileWrite(temp);
		
	}
	
	public void DaliyLogWrite(){
		
		String temp  = BoardInfo.Date + "	" +TradeStatics.PresentMarket.toString() + "	" 
						+ TradeStatics.LowestPrice.toString() + "	" + TradeStatics.PresentPrice.toString() + "	" + TradeStatics.HighestPrice.toString() + "\r\n";
		DailyLog.FileWrite(temp);
	
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
	
	class RecordSeriesExtract extends TimerTask{  //一定時間間隔で時系列特徴値を収集
		public void run() {
			//日経平均の時系列
			
	    	String SubProcessName = "RecordSeriesExtract ";
	    	Calendar rightNow;
	    	Date Now = new Date();
	    	SimpleDateFormat D = new SimpleDateFormat("yyyy/MM/dd	HH:mm:ss.SSS");
	    	rightNow = Calendar.getInstance();
			Now = rightNow.getTime();
			
	    	switch(DecisionMakingUnitState){
			
			case "READY":	
				break;
				
			case "START":
			
				if (TradeStatics.MarketChangePercentageSeries.StaticsNumber.size()==0){
					TradeStatics.MarketChangePercentageSeries.StaticsNumber.add(TradeStatics.StaticsNumber);
					TradeStatics.MarketChangePercentageSeries.list.add(TradeStatics.MarketChangePercentage);
				}
				else{
					TradeStatics.MarketChangePercentageSeries.StaticsNumber.set(TradeStatics.MarketChangePercentageSeries.index%18,TradeStatics.StaticsNumber);
					TradeStatics.MarketChangePercentageSeries.list.set(TradeStatics.MarketChangePercentageSeries.index%18,TradeStatics.MarketChangePercentage);	
				}
				//株価の時系列
				if (TradeStatics.PriceChangePercentageSeries.StaticsNumber.size()==0){
					TradeStatics.PriceChangePercentageSeries.StaticsNumber.add(TradeStatics.StaticsNumber);
					TradeStatics.PriceChangePercentageSeries.list.add(TradeStatics.PresentMarketChange);
				}
				else{
					TradeStatics.PriceChangePercentageSeries.StaticsNumber.set(TradeStatics.PriceChangePercentageSeries.index%18,TradeStatics.StaticsNumber);
					TradeStatics.PriceChangePercentageSeries.list.set(TradeStatics.PriceChangePercentageSeries.index%18,TradeStatics.PresentMarketChange);	
				}
				//売買注文数遷移
				if (TradeStatics.BuySellRateSeries.StaticsNumber.size()==0){
					TradeStatics.BuySellRateSeries.StaticsNumber.add(TradeStatics.StaticsNumber);
					TradeStatics.BuySellRateSeries.list.add(TradeStatics.OverSell.divide(TradeStatics.UnderBuy));
				}
				else{
					TradeStatics.BuySellRateSeries.StaticsNumber.set(TradeStatics.PriceChangePercentageSeries.index%18,TradeStatics.StaticsNumber);
					TradeStatics.BuySellRateSeries.list.set(TradeStatics.PriceChangePercentageSeries.index%18,TradeStatics.OverSell.divide(TradeStatics.UnderBuy));	
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
	

	
}


