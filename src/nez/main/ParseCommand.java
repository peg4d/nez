package nez.main;

import nez.Production;
import nez.SourceContext;
import nez.ast.AST;
import nez.util.ConsoleUtils;

class ParseCommand extends Command {

	@Override
	void exec(Option opt) {
		Production p = opt.getProduction(opt.StartingPoint);
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + opt.StartingPoint);
		}
		while(opt.hasInput()) {
			SourceContext file = opt.getInputSourceContext();
			AST ast = p.parse(file, new AST());
			if(ast == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
			}
			else {
				if(file.hasUnconsumed()) {
					ConsoleUtils.println(file.getUnconsumedMessage());
				}
				ConsoleUtils.println(ast);
			}
		}
	}
	
}