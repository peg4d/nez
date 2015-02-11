package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.vm.Fail;
import nez.vm.Instruction;
import nez.vm.Optimizer;

public class Failure extends Unconsumed {
	Failure(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "fail";
	}
	@Override
	public String getInterningKey() {
		return "!!";
	}
	@Override
	public short acceptByte(int ch) {
		return Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.failure2(this);
	}
	@Override
	public Instruction encode(Optimizer optimizer, Instruction next) {
		return new Fail(optimizer, this);
	}

}