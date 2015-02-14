package nez.vm;

import nez.expr.Expression;

public class FailPush extends Instruction implements StackOperation {
	public final Instruction jump;
	FailPush(Compiler optimizer, Expression e, Instruction jump, Instruction next) {
		super(optimizer, e, next);
		this.jump = labeling(jump);
	}
	@Override
	Instruction branch() {
		return this.jump;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("failpush ");
		sb.append(label(this.jump));
		sb.append("  ## " + e);
	}

}
