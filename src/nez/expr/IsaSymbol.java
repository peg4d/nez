package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.util.UMap;

public class IsaSymbol extends Terminal {
	Tag table;
	IsaSymbol(SourcePosition s, Tag table) {
		super(s);
		this.table = table;
	}
	@Override
	public String getPredicate() {
		return "isa " + table.name;
	}
	@Override
	public String getInterningKey() {
		return "isa " + table.name;
	}
	@Override
	public boolean checkAlwaysConsumed(ExpressionChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(ExpressionChecker checker, NodeTransition c) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTable(table);
	}

	@Override
	protected int pattern(GEP gep) {
		return gep.countTable(table);
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		int size = gep.countTable(table);
		String token = gep.getOneOfSymbol(table, p % size);
		sb.append(token);
	}

}