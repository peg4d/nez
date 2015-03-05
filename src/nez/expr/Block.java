package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

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
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
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
	public Instruction encode(Compiler bc, Instruction next) {
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