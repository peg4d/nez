package nez.vm;

import nez.expr.Link;

public class NodePush extends Instruction {
	public NodePush(Compiler optimizer, Link e, Instruction next) {
		super(optimizer, e, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodePush(this);
	}

	@Override
	boolean debug() {
		return true;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("nodepush");
	}

}
