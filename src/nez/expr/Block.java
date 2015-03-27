package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Block extends Unary {
	Block(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() {
		return "block";
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newBlock(this.s, e) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return this.inner.inferTypestate(visited);
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		this.inner = this.inner.checkTypestate(checker, c);
		return this;
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
		int stateValue = context.stateValue;
		String indent = context.getIndentText(context.getPosition());
		int stackTop = context.pushSymbolTable(NezTag.Indent, indent);
		boolean b = this.inner.optimized.match(context);
		context.popSymbolTable(stackTop);
		context.stateValue = stateValue;
		return b;
	}
	
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeBlock(this, next);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		int stacktop = gep.beginBlock();
		this.inner.examplfy(gep, sb, p);
		gep.endBlock(stacktop);
	}

}