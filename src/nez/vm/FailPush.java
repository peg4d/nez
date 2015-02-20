package nez.vm;

import nez.expr.Expression;

public class FailPush extends Instruction implements StackOperation {
	public final Instruction failjump;
	FailPush(Compiler optimizer, Expression e, Instruction failjump, Instruction next) {
		super(optimizer, e, next);
		this.failjump = labeling(failjump);
	}
	@Override
	Instruction branch() {
		return this.failjump;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opFailPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("failpush ");
		sb.append(label(this.failjump));
		sb.append("  ## " + e);
	}

}
