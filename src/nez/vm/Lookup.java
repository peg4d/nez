package nez.vm;

import nez.expr.Expression;

public class Lookup extends Instruction {
	public final int memoPoint;
	public final Instruction jump;
	public Lookup(Optimizer optimizer, Expression e, int memoPoint, Instruction jump, Instruction next) {
		super(optimizer, e, next);
		this.memoPoint = memoPoint;
		this.jump = labeling(jump);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("lookup " + memoPoint + " " + label(jump));
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookup(this);
	}

}
