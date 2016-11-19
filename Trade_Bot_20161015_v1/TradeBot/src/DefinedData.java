import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;


public class DefinedData extends Thread{

	BoardInfo BoardInfo = new BoardInfo();
	UserProperty UserProperty = new UserProperty();
	TradeStatics TradeStatics = new TradeStatics();

	public class BoardInfo{

		
		int DataNumber = 0;
		int PreDataNumber = 0;
		String  Date;  //システム日付
		String 	time;  //システム時間
		String  MarketOpen;
		String  PriceOpen;
		String  Market;
		String  MarketNetChange;
		String 	Price;
		String 	NetChange;
		String 	NetChangePercent;
		String  Dekitaka;
		String  VWAP;
		String[][]	Board = new String[23][3];
		Boolean	Board_flag = false;
		Boolean	trigger=false ;
		
		double PriceRange;
		String StockStatus;  // 取引可能かどうかを判定する　"Buy_Lock"  "Trade_Avaliable"  "Sell_Lock"
		String MarketStatus;
		
		int SellIndex;
		int BuyIndex;
		String BoardTime; //取得データ時間
		String AttributeTime;
		Object BoardInfoLock = new Object();
	}
	
	public class UserProperty{
		String USER_NAME ;
		String PASSWORD ;
		
		BigDecimal cash = new BigDecimal(1000000.0);	//所持現金
		BigDecimal NetGain = new BigDecimal(0.0);	//損益
		BigDecimal cost;			//手数料	
		String Holded = "NONE"; //NONE  HOLDED 株を所持しているかどうか
		UserAction UserAction = new UserAction();
		
		Object UserPropertyLock = new Object();
		
		public class UserAction{
			String Action = "NONE";	//BUY ,BUY_CHANGE ,BUY_CANCEL,
			               			//SELL,SELL_CHANGE,SELL_CANCEL,WAIT
									//NONE
			String target;
			BigDecimal Price;      		//購入価格
			BigDecimal OrderStockNum;		//購入株数
			double ActionScore;	//行動決定値　0~10 sell 11~90 wait 91~100 buy
		}
	}
	
	public class OrderDetail{
		String Action;	//BUY ,BUY_CHANGE ,BUY_CANCEL,
						//SELL,SELL_CHANGE,SELL_CANCEL,WAIT
						//NONE
		String target;
		BigDecimal Price;
		BigDecimal OrderStockNum;
		
	}
	
	public class TradeStatics{
		
		int StaticsNumber =1;
		BigDecimal [][]	Board = new BigDecimal[23][3];
		
		BigDecimal PriceOpen;
		BigDecimal PresentPrice; // 現在の株価格
		double PFactor = 0.99;
		double PriceChange_Online_Avg = 0.0; // 株価格の分析点数
		String PriceTrend;   // 株の動向の分類
		
		BigDecimal MarketOpen;
		BigDecimal PresentMarket; // 現在の日経平均
		double MFactor = 0.999;
		double MarketChange_Online_Avg = 0.0;      // 日経平均の分析点数
		String MarketTrend;        // 日経平均動向の分類
		
		BigDecimal PresentPriceChange; // 現在の株価上昇落下値
		BigDecimal PresentMarketChange; //現在の日経平均上昇落下値
		
		BigDecimal Dekitaka= new BigDecimal(0.0);
		BigDecimal Dekitaka_Change ;
		BigDecimal VWAP;
			
		BigDecimal BuyTrend; // 気配板での買い気配
		BigDecimal SellTrend; //気配板での売り気配
		
		BigDecimal NariyukiBuy; //取引低支持の成り行き買
		BigDecimal NariyukiSell; //取引低支持の成り行き売り
		BigDecimal OverSell;
		BigDecimal UnderBuy;
		
		BigDecimal HighestPrice= new BigDecimal(0.0);
		BigDecimal LowestPrice= new BigDecimal(0.0);
		
		public class StackInvironment{
		
		}
		public class HoldingStack{
			
		}
	}
	
	class ThrowException implements Runnable {

		public void run() {
			try {
				Thread.sleep((long) Math.random());
				throw new RuntimeException("RutimeException");
			} catch (InterruptedException e) {
			}
		}
	}

	class CatchException implements UncaughtExceptionHandler {
		public void uncaughtException(Thread t, Throwable e) {
			System.out.println(t.getName());
		}
	}
	
	
	
}
