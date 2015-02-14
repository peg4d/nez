package nez.vm;

import nez.expr.Expression;

public class Fail extends Instruction implements StackOperation {
	public Fail(Compiler optimizer, Expression e) {
		super(optimizer, e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFail();
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("fail");
	}

}
