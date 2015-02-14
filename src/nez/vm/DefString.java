package nez.vm;

import nez.ast.Tag;
import nez.expr.DefSymbol;

public class DefString extends Instruction {
	Tag tableName;
	public DefString(Compiler optimizer, DefSymbol e, Instruction next) {
		super(optimizer, e, next);
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
