package nez.vm;

import nez.expr.Expression;

public class Memoize extends Instruction {
	public final int memoPoint;
	public Memoize(Compiler optimizer, Expression e, int memoPoint, Instruction next) {
		super(optimizer, e, next);
		this.memoPoint = memoPoint;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("memo " + memoPoint);
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoize(this);
	}

}
