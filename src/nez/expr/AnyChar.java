package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;
import nez.vm.MatchAny;

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
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(GrammarChecker checker, NodeTransition c) {
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
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
		if(context.byteAt(context.pos) != context.EOF()) {
			int len = context.charLength(context.pos);
			context.consume(len);
			return true;
		}
		return context.failure2(this);
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return new MatchAny(bc, this, next);
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