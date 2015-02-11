package nez.vm;

import nez.expr.Expression;
import nez.expr.Repetition;

public class FailPush extends Instruction{
	public final Instruction jump;
	public FailPush(Optimizer optimizer, Expression e, Instruction jump, Instruction next) {
		super(optimizer, e, next);
		this.jump = labeling(jump);
	}
	public FailPush(Optimizer optimizer, Repetition e, Instruction next) {
		super(optimizer, e, null);
		this.next = e.get(0).encode(optimizer, this);
		this.jump = labeling(next);
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
