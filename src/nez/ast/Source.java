package nez.ast;

public interface Source {
	public final static int EOF = 256; 

	public String  getResourceName();
	public int     byteAt(long pos);
	public long    length();
	public boolean match(long pos, byte[] text);

	public String  substring(long startIndex, long endIndex);
	public long    linenum(long pos);
	
	public String formatPositionLine(String messageType, long pos, String message);


}



