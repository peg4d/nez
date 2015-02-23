package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;

public class IsaSymbol extends IsSymbol implements ContextSensitive {
	IsaSymbol(SourcePosition s, Tag table) {
		super(s, table);
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