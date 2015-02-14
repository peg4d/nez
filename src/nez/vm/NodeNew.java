package nez.vm;

import nez.expr.Expression;

public class NodeNew extends Instruction {
	public NodeNew(Compiler optimizer, Expression e, Instruction next) {
		super(optimizer, e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNew(this);
	}

	@Override
	boolean debug() {
		return true;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("new");
	}


}
