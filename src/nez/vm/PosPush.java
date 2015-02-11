package nez.vm;

import nez.expr.And;

public class PosPush extends Instruction {
	public PosPush(Optimizer optimizer, And e, Instruction next) {
		super(optimizer, e, e.encode(optimizer, new PosBack(optimizer, e, next)));
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opPosPush(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("pospush");
	}

}
