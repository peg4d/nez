package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingSource;

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
	public short acceptByte(int ch) {
		return (ch == ParsingSource.EOF) ? Reject : Accept;
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
	public Instruction encode(Compiler bc, Instruction next) {
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