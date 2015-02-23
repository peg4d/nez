package nez.main;

import nez.Production;
import nez.SourceContext;
import nez.ast.Node;
import nez.ast.Transformer;
import nez.util.ConsoleUtils;

class ParseCommand extends Command {
	@Override
	void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Production p = config.getProduction(config.StartingPoint);
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
		}
		p.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.record(rec);
			Transformer trans = config.getTransformer();
			long t1 = System.nanoTime();
			Node node = p.parse(file, trans.newNode());
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			long t2 = System.nanoTime();
			Recorder.recordLatencyMS(rec, "Latency", t1, t2);
			Recorder.recordThroughputKPS(rec, "Throughput", file.length(), t1, t2);
			trans.transform(config.getOutputFileName(file), node);
			if(rec != null) {
				rec.record();
			}
		}
	}
}

class CheckCommand extends Command {
	@Override
	void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Production p = config.getProduction(config.StartingPoint);
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
		}
		p.disable(Production.ASTConstruction);
		p.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.record(rec);
			long t1 = System.nanoTime();
			if(!p.match(file)) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			long t2 = System.nanoTime();
			Recorder.recordLatencyMS(rec, "Latency", t1, t2);
			Recorder.recordThroughputKPS(rec, "Throughput", file.length(), t1, t2);
			if(rec != null) {
				rec.record();
			}
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

