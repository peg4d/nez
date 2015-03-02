package nez.x;

import nez.Production;
import nez.SourceContext;
import nez.ast.Node;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.util.ConsoleUtils;

public class RelationCommand extends Command {
	static {
		CommandConfigure.addCommand("rel", new RelationCommand());
	}
	@Override
	public void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Production p = config.getProduction(config.StartingPoint);
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
		}
		p.record(rec);
		RelationExtracker rel = new RelationExtracker();
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.start(rec);
			Node node = p.parse(file, rel.newNode());
			file.done(rec);
			if(node == null) {
				ConsoleUtils.exit(1, file.getSyntaxErrorMessage());
				break;
			}
			if(rec != null) {
				rel.record(rec);
				rec.log();
			}
		}
		rel.dump();
	}
}
