package nez.vm;

import nez.ast.Tag;
import nez.expr.DefSymbol;

public interface ContextSensitive {
	
}

class DefString extends Instruction implements ContextSensitive {
	Tag tableName;
	public DefString(Compiler bc, DefSymbol e, Instruction next) {
		super(e, next);
		this.tableName = e.table;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("def ");
		sb.append(this.tableName);
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opDefString(this);
	}
	
}
