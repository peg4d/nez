package nez.cc;

import nez.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;

public class GrammarCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Grammar peg = config.getGrammar();
		NezGrammarGenerator gen = new NezGrammarGenerator(null);
		gen.generate(peg);
	}

}

class NezGrammarGenerator extends GrammarGenerator {
	NezGrammarGenerator(String fileName) {
		super(fileName);
	}
	
}