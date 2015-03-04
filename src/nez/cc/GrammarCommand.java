package nez.cc;

import java.lang.reflect.Constructor;

import nez.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UMap;

public class GrammarCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Grammar peg = config.getGrammar();
		GrammarGenerator gen = loadGenerator(config.getOutputFileName());
		gen.generate(peg);
	}
	
	static UMap<Class<?>> classMap = new UMap<Class<?>>();
	static void regist(String type, String className) {
		try {
			Class<?> c = Class.forName(className);
			classMap.put(type, c);
		} catch (ClassNotFoundException e) {
			Verbose.println("unfound class: " + className);
		}
	}
	
	static {
		regist("mouse", "nez.cc.MouseGrammarGenerator");
		regist("lua",   "nez.cc.LPegGrammarGenerator");
		regist("lpeg",  "nez.cc.LPegGrammarGenerator");
	}
	
	final GrammarGenerator loadGenerator(String output) {
		if(output != null) {
			GrammarGenerator gen = null;
			String type = output;
			String fileName = null; // stdout
			int loc = output.lastIndexOf('.');
			if(loc > 0) {
				type = output.substring(loc+1);
				fileName = output;
			}
			Class<?> c = classMap.get(type);
			if(c == null) {
				fileName = null;
				try {
					c = Class.forName(output);
				} catch (ClassNotFoundException e) {
					ConsoleUtils.exit(1, "unknown output type: " + output);
				}
			}
			try {
				Constructor<?> ct = c.getConstructor(String.class);
				gen = (GrammarGenerator)ct.newInstance(fileName);
			}
			catch(Exception e) {
				ConsoleUtils.exit(1, "unable to load: " + output + " due to " + e);
			}
			return gen;
		}
		return new NezGrammarGenerator(null);
	}

}

class NezGrammarGenerator extends GrammarGenerator {
	NezGrammarGenerator(String fileName) {
		super(fileName);
	}
	
}