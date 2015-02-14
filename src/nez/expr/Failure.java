package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.vm.Fail;
import nez.vm.Instruction;
import nez.vm.Compiler;

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
	public Instruction encode(Compiler bc, Instruction next) {
		return new Fail(bc, this);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append("\0");
	}

}