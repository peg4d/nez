package nez.expr;

import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;

public class Empty extends Unconsumed {
	Empty(SourcePosition s) {
		super(s);
	}	
	@Override
	public String getPredicate() {
		return "empty";
	}
	
	@Override
	public String getInterningKey() {
		return "";
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return next;
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}

}
