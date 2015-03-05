package nez.expr;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Option extends Unary {
	Option(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newOption(this.s, e) : this;
	}
	@Override
	public String getPredicate() { 
		return "*";
	}
	@Override
	public String getInterningKey() { 
		return "?";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		int t = this.inner.inferTypestate(visited);
		if(t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		int required = c.required;
		Expression inn = this.inner.checkTypestate(checker, c);
		if(required != Typestate.OperationType && c.required == Typestate.OperationType) {
			checker.reportWarning(s, "unable to create objects in repetition => removed!!");
			this.inner = inn.removeNodeOperator();
			c.required = required;
		}
		else {
			this.inner = inn;
		}
		return this;
	}
	@Override 
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		//long f = context.rememberFailure();
		Node left = context.left;
		if(!this.inner.optimized.match(context)) {
			context.left = left;
			//context.forgetFailure(f);
		}
		left = null;
		return true;
	}

	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeOption(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 2;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		if(p % 2 == 0) {
			this.inner.examplfy(gep, sb, p);
		}
	}

}