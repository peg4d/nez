package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.util.UMap;

public class IsSymbol extends Terminal implements ContextSensitive {
	public Tag table;
	Expression symbolExpression = null;
	IsSymbol(SourcePosition s, Tag table) {
		super(s);
		this.table = table;
	}
	@Override
	public String getPredicate() {
		return "is " + table.name;
	}
	@Override
	public String getInterningKey() {
		return "is " + table.name;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(GrammarChecker checker, NodeTransition c) {
		if(this.symbolExpression == null) {
			this.symbolExpression = checker.getSymbolExpression(table.name);
			if(this.symbolExpression == null) { 
				checker.reportError(s, "undefined table " + table.name);
			}
		}
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTableTop(table);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.getSymbol(table);
		sb.append(token);
	}
}