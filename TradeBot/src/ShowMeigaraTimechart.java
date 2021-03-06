
import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.imageio.ImageIO;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ChartUtilities;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;


public class ShowMeigaraTimechart extends ApplicationFrame {
	String ProcessName = "ShowMeigaraTable";
	/** The time series data. */
	private TimeSeries series_Price;
	private TimeSeries series_Avg;
	private TimeSeries series_VWAP; //20161009 出来高、VWAP表示追加
	private TimeSeries series_Dekitaka;//20161009 出来高、VWAP表示追加
	private TimeSeries series_SellBoard;
	private TimeSeries series_BuyBoard;

	/** The most recent value added. */
	private BigDecimal lastPrice;
	private double Average;
	private BigDecimal Dekitaka;//20161009 出来高、VWAP表示追加
	private BigDecimal VWAP;//20161009 出来高、VWAP表示追加
	    
	JFreeChart chart;
	JFreeChart chart1;
	JFreeChart chart2;//20161009 出来高用追加
	JPanel content;
	    
	LogUnit ErrorLog;
	    
	/**
	 * Constructs a new demonstration application.
	  *
	  * @param title  the frame title.
	  */
	public ShowMeigaraTimechart(final String title , final String Date, final String time, LogUnit ErrorLog) {
   
	    super(title);
	    String SubProcessName = "Initiation";
	    this.ErrorLog = ErrorLog;
	    try{   
	    	this.series_Price = new TimeSeries("Stock Data", Millisecond.class);
	        this.series_Avg = new TimeSeries("Average Data", Millisecond.class);
	        this.series_VWAP = new TimeSeries("Average VWAP", Millisecond.class);//20161009 出来高、VWAP表示追加
	        this.series_Dekitaka = new TimeSeries("Average Dekitaka", Millisecond.class);//20161009 出来高、VWAP表示追加
	        this.series_SellBoard = new TimeSeries("Sell Board Data", Millisecond.class);
	        this.series_BuyBoard = new TimeSeries("Buy Board Data", Millisecond.class);
	        
	        final TimeSeriesCollection dataset = new TimeSeriesCollection(this.series_Price);
	        dataset.addSeries(this.series_Avg);
	        dataset.addSeries(this.series_VWAP);//20161009 出来高、VWAP表示追加
	        
	        final TimeSeriesCollection Board_dataset = new TimeSeriesCollection(this.series_SellBoard);
	        Board_dataset.addSeries(this.series_BuyBoard);
	        //final TimeSeriesCollection Board_dataset1 = new TimeSeriesCollection(this.series_BuyBoard);
	        final TimeSeriesCollection dataset2 = new TimeSeriesCollection(this.series_Dekitaka);
	        
	        CategoryAxis domainAxis = new CategoryAxis("time");
	        CombinedDomainCategoryPlot plot = new CombinedDomainCategoryPlot(domainAxis);
	        
	        chart = createChart(dataset,"Price");//20161009 出来高、VWAP表示追加
	        chart1 = createChart(Board_dataset,"Ordering Stock");
	        //chart1 = createChart_TwoAxis(Board_dataset,Board_dataset1);
	        chart2 = createChart(dataset2,"Dealed Stock");//20161009 出来高、VWAP表示追加
	       
	        
	        final ChartPanel chartPanel = new ChartPanel(chart);
	        final ChartPanel chartPanel1 = new ChartPanel(chart1);
	        final ChartPanel chartPanel2 = new ChartPanel(chart2);//20161009 出来高、VWAP表示追加

	        content = new JPanel(new BorderLayout());
	        
	        content.add(chartPanel,BorderLayout.NORTH);
	        content.add(chartPanel1,BorderLayout.CENTER);
	        content.add(chartPanel2,BorderLayout.SOUTH);//20161009 出来高、VWAP表示追加

	        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 300));
	        chartPanel1.setPreferredSize(new java.awt.Dimension(1000, 300));
	        chartPanel2.setPreferredSize(new java.awt.Dimension(1000, 300));//20161009 出来高、VWAP表示追加
	        setContentPane(content);
	              
	    }catch(Exception e){
			System.out.println( e);
			ErrorLogWrite(ProcessName,SubProcessName, e.toString() );
		}
	}

	private JFreeChart createChart(final XYDataset dataset,String title) {
		
	    final JFreeChart result = ChartFactory.createTimeSeriesChart(
	    	title, 
	        "Time", 
	        "Value",
	        dataset, 
	        true, 
	        true, 
	        false
	    );
	    final XYPlot plot = result.getXYPlot();
	    ValueAxis domainAxis  = plot.getDomainAxis();
	    domainAxis .setAutoRange(true);
	        
	    domainAxis.setFixedAutoRange(25200000.0);  // 60 seconds  
	    ValueAxis rangeAxis  = plot.getRangeAxis(); 
	    rangeAxis .setAutoRange(true);
	    
	    return result;
	}
	private JFreeChart createChart_TwoAxis(final XYDataset dataset ,XYDataset dataset1) {
	    final JFreeChart result = ChartFactory.createTimeSeriesChart(
	        "Dynamic Data Demo", 
	        "Time", 
	        "Value",
	        dataset, 
	        true, 
	        true, 
	        false
	    );
	    final XYPlot plot = result.getXYPlot();
	    ValueAxis domainAxis  = plot.getDomainAxis();
	    domainAxis.setAutoRange(true);
	    domainAxis.setFixedAutoRange(25200000.0);  // 60 seconds
	    
	    ValueAxis rangeAxis  = plot.getRangeAxis();
	    rangeAxis.setLabel("Sell Trend");
	   
	    final ValueAxis rangeAxis1 = new NumberAxis("Buy Trend");
	    
	    plot.setDataset(1, dataset1);
	    plot.setRangeAxis(1, rangeAxis1);
	      
	    rangeAxis.setAutoRange(true);
	    rangeAxis1.setAutoRange(true);
	    plot.mapDatasetToRangeAxis(1, 1);
	    
	    final XYLineAndShapeRenderer  render = new XYLineAndShapeRenderer(true,false);
	    
	    plot.setRenderer(1, render);
	    render.setSeriesPaint (1, ChartColor.BLUE);
	    
	    return result;
	}
	  
	public void TimechartRenew(BigDecimal Price, double Average,final BigDecimal BuyTrend,final BigDecimal SellTrend, String Date, String Time,BigDecimal Dekitaka,BigDecimal VWAP) {
		String SubProcessName = "TimechartRenew";
		try{
			
	    	this.lastPrice = Price;
	    	this.Average   = Average;
	    	this.Dekitaka = Dekitaka;
	    	this.VWAP = VWAP;
	    	System.out.println("Date = " + Date+ " Time = " + Time );
	    	int Year 	= Integer.valueOf(Date.substring(0,4));
	    	int Month 	= Integer.valueOf(Date.substring(5,7));
	    	int Day 	= Integer.valueOf(Date.substring(8,10));
	    	
	    	int Hour 	= Integer.valueOf(Time.substring(0,2));
	    	int Minute 	= Integer.valueOf(Time.substring(3,5));
	    	int Second 	= Integer.valueOf(Time.substring(6,8));
	    	int Millisecond = 0;
	    	//int Millisecond = Integer.valueOf(Time.substring(9,12));
	    	
	    	final Millisecond now = new Millisecond(Millisecond,Second,Minute,Hour,Day,Month,Year);
	    	System.out.println("Now = " + now.toString());
	        
	    	this.series_Price.addOrUpdate(now, this.lastPrice);
	    	this.series_Avg.addOrUpdate(now, this.Average);
	    	this.series_VWAP.addOrUpdate(now, this.VWAP);//20161009 出来高、VWAP表示追加
	    	this.series_Dekitaka.addOrUpdate(now, this.Dekitaka);//20161009 出来高、VWAP表示追加
	    	this.series_SellBoard.addOrUpdate(now, SellTrend);
	    	this.series_BuyBoard.addOrUpdate(now, BuyTrend);
	    		
	    }catch(Exception e){
	    	 System.out.println("timechart renew error = " + e);
	    	 ErrorLogWrite(ProcessName, SubProcessName , e.toString());
	    }
	}
	    
	public void TimechartSave(String LogPath, String FileName ){
		String SubProcessName = "TimechartSave";
		File file = new File(LogPath+"chart//"+FileName+".jpeg");
		try { 
			int w = content.getWidth(), h = content.getHeight();

            BufferedImage image = new BufferedImage( w, h,
                                  BufferedImage.TYPE_INT_RGB );
            Graphics2D g2 = image.createGraphics();
            content.paint( g2 );
            g2.dispose();
            
			ImageIO.write( image, "jpeg", file );
			
			//ChartUtilities.saveChartAsJPEG(file, chart, 1000, 1080);
		} catch (IOException e) {
			e.printStackTrace();
			ErrorLogWrite(ProcessName, SubProcessName ,e.toString());
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
	
