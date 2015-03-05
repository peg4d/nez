package nez.main;

import nez.Production;
import nez.SourceContext;
import nez.ast.Node;
import nez.ast.Transformer;
import nez.util.ConsoleUtils;
import nez.util.UList;

class ParseCommand extends Command {
	@Override
	public void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Production p = config.getProduction();
		p.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			Transformer trans = config.getTransformer();
			file.start(rec);
			Node node = p.parse(file, trans.newNode());
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			if(rec != null) {
				rec.log();
			}
			trans.transform(config.getOutputFileName(file), node);
		}
	}
}

class CheckCommand extends Command {
	@Override
	public
	void exec(CommandConfigure config) {
		UList<String> failedInput = new UList<String>(new String[4]);
		Recorder rec = config.getRecorder();
		Production product = config.getProduction();
		product.disable(Production.ASTConstruction);
		product.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.start(rec);
			boolean result = product.match(file);
			file.done(rec);
			product.verboseMemo();
			if(!result) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				failedInput.add(file.getResourceName());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			if(rec != null) {
				rec.log();
			}
		}
		if(failedInput.size() > 0) {
			ConsoleUtils.exit(1, "failed: " + failedInput);
		}
	}
}

//class GrammarCommand extends Command {
//	@Override
//	void exec(CommandConfigure config) {
//		Recorder rec = config.getRecorder();
//		Grammar peg = config.getGrammar();
//		grammar.record(rec);
//		GrammarWriter w = loadGrammarWriter(config.getOuputFileType());
//		w.write(peg, config.StartingPoint);
//		if(rec != null) {
//			rec.record();
//		}
//	}
//}

