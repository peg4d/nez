package nez.expr;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.Optimizer;

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
	public Instruction encode(Optimizer optimizer, Instruction next) {
		return next;
	}

}
