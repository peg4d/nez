package nez.vm;

import nez.expr.Expression;

public class MemoizeFail extends Instruction {
	public final int memoPoint;
	public MemoizeFail(Optimizer optimizer, Expression e, int memoPoint, Instruction next) {
		super(optimizer, e, next);
		this.memoPoint = memoPoint;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("memofail " + memoPoint);
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeFail(this);
	}

}
