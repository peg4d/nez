package nez.x;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.ast.AST;
import nez.ast.Node;
import nez.ast.Transformer;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.util.ConsoleUtils;

public class ConverterCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Production p = config.getProduction(config.StartingPoint);
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
		}
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
			String outputfile = config.getOutputFileName();
			if (outputfile == null) {
				outputfile = file.getResourceName() + ".nez";
				int index = outputfile.indexOf("/");
				while(index > -1) {
					outputfile = outputfile.substring(index+1);
					index = outputfile.indexOf("/");
				}
				outputfile = "gen/" + outputfile;
			}
			RegexConverter conv = new RegexConverter(new Grammar(outputfile));
			conv.convert((AST) node);
		}
	}
}
