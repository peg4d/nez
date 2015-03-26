package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;

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
	public short acceptByte(int ch, int option) {
		return (byteChar == ch) ? Prediction.Accept : Prediction.Reject;
	}
	
	@Override
	public boolean predict(int option, int ch, boolean k) {
		return (byteChar == ch) ? true : false;
	}

	@Override
	public void predict(int option, boolean[] dfa) {
		for(int c = 0; c < dfa.length; c++) {
			dfa[c] = false;
		}
		dfa[byteChar] = true;
	}

	@Override
	public boolean match(SourceContext context) {
		if(context.byteAt(context.getPosition()) == this.byteChar) {
			context.consume(1);
			return true;
		}
		return context.failure2(this);
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next, boolean[] dfa) {
		return bc.encodeByteChar(this, next, dfa);
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
