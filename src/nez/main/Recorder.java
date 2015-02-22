package nez.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class Recorder {
	final String logFile;
	Recorder(String logFile) {
		this.logFile = logFile;
	}
	
	class DataPoint {
		String key;
		Object value;
		DataPoint(String key, Object value) {
			this.key = key;
			this.value = value;
		}
	}
	
	private UList<DataPoint> dataPointList = new UList<DataPoint>(new DataPoint[64]);
	private UMap<DataPoint> dataPointMap = new UMap<DataPoint>();

	private void setDataPoint(String key, Object value) {
		if(!this.dataPointMap.hasKey(key)) {
			DataPoint d = new DataPoint(key, value);
			this.dataPointMap.put(key, d);
			this.dataPointList.add(d);
		}
		else {
			DataPoint d = this.dataPointMap.get(key);
			d.value = value;
		}
	}

	public final void setText(String key, String value) {
		this.setDataPoint(key, value);
	}

	public final void setFile(String key, String file) {
		int loc = file.lastIndexOf('/');
		if(loc > 0) {
			file = file.substring(loc+1);
		}
		this.setDataPoint(key, file);
	}

	public final void setCount(String key, long v) {
		this.setDataPoint(key, new Long(v));
	}

	public final void setDouble(String key, double d) {
		this.setDataPoint(key, d);
	}

	public final void setRatio(String key, long v, long v2) {
		double d = v;
		double d2 = v2;
		this.setDataPoint(key, new Double(d/d2));
	}

	public final String formatCommaSeparateValue() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");
		sb.append(sdf1.format(new Date()));
		for(DataPoint d : this.dataPointList) {
			sb.append(",");
			sb.append(d.key);
			sb.append(":,");
			if(d.value instanceof Double) {
				sb.append(String.format("%.5f", d.value));
			}
			else {
				sb.append(d.value);
			}
		}
		return sb.toString();
	}
	
	public final void record() {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.logFile, true)))) {
			String csv = formatCommaSeparateValue();
		    ConsoleUtils.println("writing .. " + this.logFile + " " + csv);
			out.println(csv);
//			double cf = 0;
//			for(int i = 0; i < 16; i++) {
//				int n = 1 << i;
//				double f = (double)this.backtrackCount[i] / this.BacktrackCount;
//				cf += this.backtrackCount[i];
//				System.out.println(String.format("%d\t%d\t%2.3f\t%2.3f", n, this.backtrackCount[i], f, (cf / this.BacktrackCount)));
//				if(n > this.WorstBacktrackSize) break;
//			}
		}
		catch (IOException e) {
			ConsoleUtils.exit(1, "Can't write csv log: " + this.logFile);
		}
	}

//	long BacktrackCount = 0;
//	long BacktrackLength = 0;
//	
//	long MostProccedPostion = 0;
//	long WorstBacktrackSize = 0;
//	long WorstBacktrackPosition = 0;
//	int[] backtrackCount = null;
//	
//	public void init() {
//		this.BacktrackCount = 0;
//		this.BacktrackLength = 0;
//		this.WorstBacktrackPosition = 0;
//		this.WorstBacktrackSize = 0;
//		this.MostProccedPostion = 0;
//		this.backtrackCount = new int[32];
//	}
//
//	public final boolean statBacktrack(long backed_pos, long current_pos) {
//		boolean maximumConsumed = false;
//		this.BacktrackCount = this.BacktrackCount + 1;
//		this.BacktrackLength  = this.BacktrackLength + current_pos - backed_pos;
//		if(this.MostProccedPostion < current_pos) {
//			this.MostProccedPostion = current_pos;
//			maximumConsumed = true;
//		}
//		long len = this.MostProccedPostion - backed_pos;
//		this.countBacktrackLength(len);
//		if(len > this.WorstBacktrackSize) {
//			this.WorstBacktrackSize = len;
//			this.WorstBacktrackPosition = backed_pos;
//		}
//		return maximumConsumed;
//	}
//
//	private void countBacktrackLength(long len) {
//		int n = (int)(Math.log(len) / Math.log(2.0));
//		backtrackCount[n] += 1;
//	}
//
//	public void statParsingContext(ParsingContext p, long ErapsedTime) {
//		System.gc(); // meaningless ?
//
//		this.addDataPoint(new TextDataPoint("Parser", p.getName()));
//		this.setCount("Optimization", Main.OptimizationLevel);
//		this.setCount("BacktrackBufferSize", MemoizationManager.BacktrackBufferSize);
//
//		String fileName = p.source.getResourceName();
//		if(fileName.lastIndexOf('/') > 0) {
//			fileName = fileName.substring(fileName.lastIndexOf('/')+1);
//		}
//		
//		long total = Runtime.getRuntime().totalMemory();
//		long free =  Runtime.getRuntime().freeMemory();
//		long consumed = p.getPosition();
//		this.setText("FileName", fileName);
//		if(p.source.length() == consumed) {
//			this.setText("Recognition", "(Success)");
//		}
//		else {
//			this.setText("Recognition", "(Failure)");
//		}
//		this.setCount1("FileSize", p.source.length());
//		this.setCount1("Consumed", consumed);
//		this.setCount("Latency", ErapsedTime);  // ms
//		this.setRatio("Throughput", consumed, ErapsedTime);
//
//		this.setCount("UsedHeap", total - free);
//		this.setRatio1("Heap/File", (total - free), consumed);		
//
//		
//		this.setRatio("Backtrack/Consumed", this.BacktrackLength, consumed);
//		this.setCount("Backtrack.Count", this.BacktrackCount);
//		this.setRatio("Backtrack.BackPerByte", this.BacktrackCount, consumed);
//		this.setCount("Backtrack.Worst", this.WorstBacktrackSize);
//		this.setRatio("Backtrack.Average", this.BacktrackLength, this.BacktrackCount);
////		this.setRatio("Backtrack1", this.backtrackCount[0], this.BacktrackCount);
////		this.setRatio("Backtrack2", this.backtrackCount[1], this.BacktrackCount);
////		this.setRatio("Backtrack4", this.backtrackCount[2], this.BacktrackCount);
////		this.setRatio("Backtrack8", this.backtrackCount[3], this.BacktrackCount);
////		this.setRatio("Backtrack16", this.backtrackCount[4], this.BacktrackCount);
////		this.setRatio("Backtrack32", this.backtrackCount[5], this.BacktrackCount);
////		this.setRatio("Backtrack64", this.backtrackCount[6], this.BacktrackCount);
////		this.setRatio("Backtrack128", this.backtrackCount[7], this.BacktrackCount);
////		this.setRatio1("Backtrack256", this.backtrackCount[8], this.BacktrackCount);
////		this.setRatio1("Backtrack512", this.backtrackCount[9], this.BacktrackCount);
////		this.setRatio1("Backtrack1024", this.backtrackCount[10], this.BacktrackCount);
//		
//		p.memoTable.stat(this);
////		ckeckRepeatCounter();
//
////		if(pego != null) {
////			this.statObject(pego);
////		}
////		//p.peg.updateStat(this);
////		this.setText("StatId", id);
//		
////		this.writeCSV();
//		//System.out.println("WorstBacktrack: " + p.source.substring(this.WorstBacktrackPosition, this.WorstBacktrackPosition + this.WorstBacktrackSize));
//	}
//	
//	
//	
//	
//	public void statBacktrack(long pos, long pos2) {
//		// TODO Auto-generated method stub
//		
//	}
	
	public final static void recordLatencyMS(Recorder rec, String key, long nanoT1, long nanoT2) {
		if(rec != null) {
			long t = (nanoT2 - nanoT1) / 1000; // [micro second]
			rec.setDouble(key + "[ms]", t / 1000.0);
		}
	}

	public final static void recordLatencyS(Recorder rec, String key, long nanoT1, long nanoT2) {
		if(rec != null) {
			long t = (nanoT2 - nanoT1) / 1000; // [micro second]
			rec.setDouble(key + "[s]", t / 10000000.0);
		}
	}

	public final static void recordThroughputKPS(Recorder rec, String key, long length, long nanoT1, long nanoT2) {
		if(rec != null) {
			long micro = (nanoT2 - nanoT1) / 1000; // [micro second]
			double sec = micro / 1000000.0;
			double thr = length / sec / 1024;
			rec.setDouble(key + "[KiB/s]", thr);
		}
	}


}
