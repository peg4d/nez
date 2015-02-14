package nez.vm;

import nez.expr.Link;

public class NodeStore extends Instruction {
	public final int index;
	public NodeStore(Compiler optimizer, Link e, Instruction next) {
		super(optimizer, e, next);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeStore(this);
	}
	
	@Override
	boolean debug() {
		return true;
	}
	
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("nodestore " + index);
	}


}
