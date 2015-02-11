package nez.vm;

import nez.expr.Expression;

public class NodeCapture extends Instruction {
	public NodeCapture(Optimizer optimizer, Expression e, Instruction next) {
		super(optimizer, e, next);
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opCapture(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("capture");
	}
	
}
