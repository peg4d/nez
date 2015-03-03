package nez.main;

import java.io.IOException;

import nez.util.ConsoleUtils;
import nez.util.UMap;


public abstract class Command {
	public final static String  ProgName  = "Nez";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 1;
	public final static int     MinerVersion = 0;
	public final static int     PatchLevel   = 0;
	public final static String  Version = "" + MajorVersion + "." + MinerVersion + "." + PatchLevel;
	public final static String  Copyright = "Copyright (c) 2014-2015, Nez project authors";
	public final static String  License = "BSD-Style Open Source";

	public final static void main(String[] args) {
		CommandConfigure config = new CommandConfigure();
		config.parseCommandOption(args);
		Command com = config.getCommand();
		com.exec(config);
	}

	public abstract void exec(CommandConfigure config);
	
	public final static void displayVersion() {
		ConsoleUtils.println(ProgName + "-" + Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.println(Copyright);
	}
	
	private static jline.ConsoleReader ConsoleReader = null;

	public final static String readMultiLine(String prompt, String prompt2) {
		if(ConsoleReader == null) {
			displayVersion();
			try {
				ConsoleReader = new jline.ConsoleReader();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		String line = readSingleLine(prompt);
//		if(line == null) {
//			System.exit(0);
//		}
//		if(prompt2 != null) {
//			int level = 0;
//			while((level = CheckBraceLevel(line)) > 0) {
//				String line2 = readSingleLine(prompt2);
//				line += "\n" + line2;
//			}
//			if(level < 0) {
//				line = "";
//				ConsoleUtils.println(" .. canceled");
//			}
//		}
		if(line != null) {
			ConsoleReader.getHistory().addToHistory(line);
		}
		return line;
	}

	private final static String readSingleLine(String prompt) {
		try {
			return ConsoleReader.readLine(prompt);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// command database 
	
	private static UMap<Command> commandTable = new UMap<Command>();

	public static void load(String name, String className) {
		try {
			Class<?> c = Class.forName(className);
			commandTable.put(name, (Command)c.newInstance());
		}
		catch(Exception e) {
			Verbose.println("undefined command: " + name + " due to " + e);
		}
	}

	static {
		load("check", "nez.main.CheckCommand");
		load("parse", "nez.main.ParseCommand");
		load("rel", "nez.x.RelationCommand");
		load("cc", "nez.cc.GeneratorCommand");
		load("peg", "nez.cc.GrammarCommand");
	}
	
	public static final Command getCommand(String name) {
		return commandTable.get(name);
	}
	
}

