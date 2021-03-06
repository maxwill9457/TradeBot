import javax.swing.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

public class ShowMeigaraTable extends JFrame{
	
	String ProcessName = "ShowMeigaraTable";
	//---------気配板用変数------------
	DefaultTableModel model;
	JTable table;
	JPanel panel;
	JScrollPane sp;
	
	LogUnit ErrorLog;
	
	
	private String[] TimeInfo = {"A","B","C"};
	private String[][] Infotable = new String[27][3];
	//-----------------------------

	//public static void main(String[] args){
	//   SwingTest test = new SwingTest("SwingTest");

	//   test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//   test.setVisible(true);
	// }
    
	//気配板
	ShowMeigaraTable(String title,String[][] tabledata,String Time, String Date,String Market,String MarketNetChange,String Price,String NetChange,Object Lock,String Dekitaka,String VWAP, LogUnit ErrorLog){
		
		this.ErrorLog = ErrorLog;
		String SubProcessName = "Initiation";
		
		this.setTitle(title);
		this.setBounds( 20, 20, 300, 500);
		this.setLocation(50, 25);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
			
		try{
			Infotable[0][0] = "時間";
			Infotable[0][1] = Date;
			Infotable[0][2] = Time;
			Infotable[1][0] = "市場情報";
			Infotable[1][1] = Market;
			Infotable[1][2] = MarketNetChange;
			Infotable[2][0] = "株原価";
			Infotable[2][1] = Price;
			Infotable[2][2] = NetChange;
			Infotable[3][0] = "出来高/VWAP";
			Infotable[3][1] = Dekitaka;
			Infotable[3][2] = VWAP;
		
			for (int x=0 ; x<23; x++){ // update Trade Board
				for (int y=0 ; y<3; y++){
					Infotable[x+4][y] = tabledata[x][y];
				}
			}
		
			this.model= new DefaultTableModel(Infotable,TimeInfo);
			this.table = new JTable(model);

			this.panel = new JPanel();
			this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.PAGE_AXIS));
			//this.panel.setLayout(null);
		
		
			this.panel.add(this.table);
			//this.panel.add(this.table);
    
			getContentPane().add(this.panel, BorderLayout.CENTER);
		}catch (Exception e){
			System.out.println( e+"_tradeboard");
			ErrorLogWrite(ProcessName, SubProcessName , e.toString());
		}
	}
  
	//public void Renew(String[][] tabledata,String date, String time ){
	public void BoardRenew(String[][] tabledata,String Time,String Date,String Market,String MarketNetChange,String Price,String NetChange,Object Lock,String Dekitaka,String VWAP ){
		
		String SubProcessName = "BoardRenew";
		//Date.set
		try{
		  //Infotable = {{Date,Time},{Shijyo,ShijyoNetChange},{Price,NetChangePercent}};
			//String[][] temptable = new String[27][3];
			Infotable[0][0] = "時間";
			Infotable[0][1] = Date;
			Infotable[0][2] = Time;
			Infotable[1][0] = "市場情報";
			Infotable[1][1] = Market;
			Infotable[1][2] = MarketNetChange;
			Infotable[2][0] = "株原価";
			Infotable[2][1] = Price;
			Infotable[2][2] = NetChange;
			Infotable[3][1] = Dekitaka;
			Infotable[3][2] = VWAP;
			
			for (int x=0 ; x<23; x++){ // update Trade Board
				for (int y=0 ; y<3; y++){
					Infotable[x+4][y] = tabledata[x][y];
				}
			}
		
			model.setDataVector(Infotable, TimeInfo);
			table = new JTable(model);	
			table.repaint();
		
		//t_table.updateUI();
		}catch (NullPointerException e){
			System.out.println( e+"_tradeboard");
			ErrorLogWrite(ProcessName, SubProcessName , e.toString());
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