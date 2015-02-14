package nez.vm;

import nez.expr.Expression;

public class MatchAny extends Instruction {
	public MatchAny(Compiler optimizer, Expression e, Instruction next) {
		super(optimizer, e, next);
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMatchAny(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("matchany");
	}

}
