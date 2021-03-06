import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class LogUnit {
	
	File file;
	FileWriter fw;
	BufferedWriter bw;

	String FileDirectory ;
	String LogUnitState;
	
	
	
	LogUnit(String Path, String FileName,double TimeFlag){ //TimeFlag 0 フラグなし
		LogUnitState = "PREPARE";
		System.out.println( FileName+ "	LogUnit Activating" );
		if(TimeFlag ==1){
			file = new File(FileDirectory(Path,FileName));
		}else{
			FileDirectory=Path+FileName+".txt";
			file = new File(FileDirectory);
		}
		FileOpen(FileName);
		LogUnitState = "READY";
		System.out.println(  FileName+"	LogUnit Ready" );
		
	}
	
	
	public String FileDirectory(String Path, String FileName){
		Calendar myCal = Calendar.getInstance();
		DateFormat myFormat = new SimpleDateFormat("yyyyMMdd");
		String myName = FileName+"_"+myFormat.format(myCal.getTime()) + ".txt";
		
		FileDirectory = Path+myName;
		return FileDirectory;
	}
	
	public void FileOpen(String target){
		
		System.out.println(FileDirectory);
		
		try{
			
			file.createNewFile();	  
		}catch(IOException e){
			System.out.println(e);
		}
		
	}
	public void FileWrite(String data){
		
		fw = null;
		bw = null;
		
		try{
			fw = new FileWriter(file,true);
			bw = new BufferedWriter(fw);
			
			bw.write(data);
			bw.close();
			
		}catch(IOException e){
			  System.out.println(e);
		}
	}
	public void FileRead(){
		
	}
	
	public void FileClose(){
		try{
			bw.close();
		}catch(IOException e){
			  System.out.println(e);
		}
	}
	

}
