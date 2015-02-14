package nez.vm;

import nez.expr.And;

public class PosBack extends Instruction {
	public PosBack(Compiler optimizer, And e, Instruction next) {
		super(optimizer, e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opPopBack(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("back");
	}

}
