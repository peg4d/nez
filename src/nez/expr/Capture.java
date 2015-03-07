package nez.expr;

import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;

public class Capture extends ParsingOperation {
	int shift;
	Capture(SourcePosition s, int shift) {
		super(s);
		this.shift = shift;
	}
	@Override
	public String getPredicate() { 
		return "}";
	}
	@Override
	public String getInterningKey() {
		return shift == 0 ? "}" : "}"+shift;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this.checkTypestate(checker, c, "}");
	}
	
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeCapture(this, next);
	}
}
