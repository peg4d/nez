package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Match extends Unary {
	Match(SourcePosition s, Expression inner) {
		super(s, inner);
	}
	@Override
	public String getPredicate() { 
		return "~";
	}
	@Override
	public String getInterningKey() { 
		return "~";
	}	
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newMatch(this.s, e) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this.inner.removeASTOperator();
	}
	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
	@Override
	public boolean predict(int option, int ch, boolean k) {
		return Prediction.predictUnary(this, option, ch, k);
	}
	@Override
	public void predict(int option, boolean[] dfa) {
		Prediction.predictUnary(this, option, dfa);
	}
	@Override
	public boolean match(SourceContext context) {
		return this.inner.optimized.match(context);
	}
	
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next, boolean[] dfa) {
		return this.inner.encode(bc, next, dfa);
	}

	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.inner.examplfy(gep, sb, p);
	}

}