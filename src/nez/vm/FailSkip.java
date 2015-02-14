package nez.vm;

import nez.expr.Expression;

public class FailSkip extends Instruction {

	public FailSkip(Compiler optimizer, Expression e) {
		super(optimizer, e, null);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("skip");
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailSkip(this);
	}

}
