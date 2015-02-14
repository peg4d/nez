package nez.vm;

import nez.expr.Expression;

public class Nop extends Instruction {

	public Nop(Compiler bc, Expression e, Instruction next) {
		super(bc, e, next);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("nop");
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return this.next;
	}

}
