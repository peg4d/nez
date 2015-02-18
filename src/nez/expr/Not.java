package nez.expr;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Not extends Unary {
	Not(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() { 
		return "!";
	}
	@Override
	public String getInterningKey() { 
		return "!";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(GrammarChecker checker, NodeTransition c) {
		int t = this.inner.inferNodeTransition(null);
		if(t == NodeTransition.ObjectType || t == NodeTransition.OperationType) {
			this.inner = this.inner.removeNodeOperator();
		}
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		/* the code below works only if a single character in !(e) */
		/* we must accept 'i' for !'int' 'i' */
//		if(r == Accept || r == LazyAccept) {
//			return Reject;
//		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		long pos = context.getPosition();
		//long f   = context.rememberFailure();
		Node left = context.left;
		if(this.inner.matcher.match(context)) {
			context.rollback(pos);
			context.failure2(this);
			left = null;
			return false;
		}
		else {
			context.rollback(pos);
			//context.forgetFailure(f);
			context.left = left;
			left = null;
			return true;
		}
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newNot(this.s, e) : this;
	}

	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeNot(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		int max = 0;
		for(Expression p: this) {
			int c = p.pattern(gep);
			if(c > max) {
				max = c;
			}
		}
		return max;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}



}