package nez.vm;

import nez.expr.Expression;

public class FailPop extends Instruction implements StackOperation {
	public FailPop(Compiler optimizer, Expression e, Instruction next) {
		super(optimizer, e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailPop(this);
	}
	
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("failpop");
	}

}
