package nez.vm;

import nez.expr.Rule;

public class CallPush extends Instruction implements StackOperation {
	Rule rule;
	public Instruction jump = null;
	public CallPush(Compiler optimizer, Rule rule, Instruction next) {
		super(optimizer, rule, next);
		this.rule = rule;
	}

	void setResolvedJump(Instruction jump) {
		assert(this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}
	
	
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opCallPush(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("callpush " + label(jump) + "   ## " + rule.getLocalName());
	}

}
