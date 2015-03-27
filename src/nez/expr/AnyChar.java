package nez.expr;

import java.util.TreeMap;

import nez.Production;
import nez.SourceContext;
import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class AnyChar extends Terminal {
	AnyChar(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "any";
	}
	@Override
	public String getInterningKey() { 
		return ".";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this;
	}
	@Override
	public Expression removeASTOperator() {
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	
	@Override
	public short acceptByte(int ch, int option) {
		if(UFlag.is(option, Production.Binary)) {
			return (ch == Source.BinaryEOF) ? Prediction.Reject : Prediction.Accept;
		}
		else {
			return (ch == Source.BinaryEOF || ch == 0) ? Prediction.Reject : Prediction.Accept;
		}
	}
	
	@Override
	public boolean predict(int option, int ch, boolean k) {
		return Prediction.predictAnyChar(option, ch, k);
	}

	@Override
	public void predict(int option, boolean[] dfa) {
		Prediction.predictAnyChar(option, dfa);
	}

	@Override
	public boolean match(SourceContext context) {
		if(context.byteAt(context.getPosition()) != context.EOF()) {
			int len = context.charLength(context.getPosition());
			context.consume(len);
			return true;
		}
		return context.failure2(this);
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeMatchAny(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append(".");
	}
}