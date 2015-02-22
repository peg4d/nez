package nez.main;

import nez.Production;
import nez.SourceContext;
import nez.ast.AST;
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
			long t1 = System.nanoTime();
			AST ast = p.parse(file, new AST());
			if(ast == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			long t2 = System.nanoTime();
			Recorder.recordLatencyMS(rec, "Latency", t1, t2);
			Recorder.recordThroughputKPS(rec, "Throughput", file.length(), t1, t2);
			//new ASTWriter().startWriter(config.getOutputFileName(file), ast);
			if(rec != null) {
				rec.record();
			}
		}
	}
	
}