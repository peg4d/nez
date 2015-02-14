package nez.vm;

import nez.expr.Expression;

public class NodeLeftLink extends Instruction {
	public NodeLeftLink(Compiler optimizer, Expression e, Instruction next) {
		super(optimizer, e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeLeftLink(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("newleft");
	}

}
