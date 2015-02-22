package nez.main;

import nez.util.ConsoleUtils;

public class Verbose {
	public static boolean General = true;
	public static boolean VirtualMachine = false;
	public static boolean PackratParsing = false;
	
	public static void setAll() {
		General = true;
		VirtualMachine = true;
		PackratParsing = true;
	}

	public final static void println(String msg) {
		if(General) {
			ConsoleUtils.println("verbose: " + msg);
		}
	}

}
