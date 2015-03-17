package nez.x;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.ast.AST;
import nez.ast.Node;
import nez.expr.GrammarChecker;
import nez.expr.NezParser;
import nez.expr.Rule;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.util.ConsoleUtils;

public class DemoCommand extends Command {
	@Override
	public void exec(CommandConfigure config) {
		Production product = config.getProduction();
		Grammar peg = product.getRule().getGrammar();
		String start = config.StartingPoint;
		NezParser parser = new NezParser();
		boolean grammarAdded = false;
		String line = null;
		int linenum = 0;
		while((line = readMultiLine(start + "? ", "")) != null) {
			linenum++;
			if(line.startsWith("\\")) {
				String s = line.substring(1);
				Production p = peg.getProduction(s);
				if(p == null) {
					ConsoleUtils.println("Undefined Rule: " + s);
					ConsoleUtils.println("Rules: " + peg.getDefinedRuleList());
					ConsoleUtils.println("To switch the rule, type \\Rule");
					continue;
				}
				for(Rule r : p.getRuleList()) {
					ConsoleUtils.println(r.getLocalName() + " = " + r.getExpression());
				}
				start = line.substring(1);
				product = p;
				continue;
			}
			int loc = line.indexOf("=");
			if(loc > 0) {
				Rule r = parser.parseRule(peg, "<stdin>", linenum, line);
				if(r != null) {
					grammarAdded = true;
					start = r.getLocalName();
					continue;
				}
			}
			if(grammarAdded) {
				new GrammarChecker(0).verify(peg);
				product = peg.getProduction(start);
				assert(product != null);
				grammarAdded = false;
			}
			SourceContext sc = SourceContext.newStringSourceContext("<stdio>", linenum, line);
			Node node = product.parse(sc, new AST());
			if(node == null) {
				ConsoleUtils.println(sc.getSyntaxErrorMessage());
				continue;
			}
			ConsoleUtils.println(node + "\n");
		}
	}
	
}
