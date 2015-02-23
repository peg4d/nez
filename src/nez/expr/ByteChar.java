package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class ByteChar extends Terminal {
	public int byteChar;
	ByteChar(SourcePosition s, int ch) {
		super(s);
		this.byteChar = ch;
	}
	@Override
	public String getPredicate() {
		return "byte " + byteChar;
	}
	@Override
	public String getInterningKey() { 
		return "'" + byteChar;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return (byteChar == ch) ? Accept : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		if(context.byteAt(context.pos) == this.byteChar) {
			context.consume(1);
			return true;
		}
		return context.failure2(this);
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeByteChar(this, next);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return this.size();
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append((char)this.byteChar);
	}

}
