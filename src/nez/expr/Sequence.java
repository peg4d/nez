package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.Compiler;

public class Sequence extends ExpressionList {
	Sequence(SourcePosition s, UList<Expression> l) {
		super(s, l);
	}
	@Override
	public String getPredicate() {
		return "seq";
	}	
	@Override
	public String getInterningKey() {
		return " ";
	}
	@Override
	public boolean checkAlwaysConsumed(ExpressionChecker checker, String startNonTerminal, UList<String> stack) {
		for(Expression e: this) {
			if(e.checkAlwaysConsumed(checker, startNonTerminal, stack)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public Expression removeNodeOperator() {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			Factory.addSequence(l, e.removeNodeOperator());
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		for(Expression e: this) {
			int t = e.inferNodeTransition(visited);
			if(t == NodeTransition.ObjectType || t == NodeTransition.OperationType) {
				return t;
			}
		}
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(ExpressionChecker checker, NodeTransition c) {
		UList<Expression> l = newList();
		for(Expression e : this) {
			Factory.addSequence(l, e.checkNodeTransition(checker, c));
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = newList();
		for(int i = 0; i < this.size(); i++) {
			Expression e = get(i).removeFlag(undefedFlags);
			Factory.addSequence(l, e);
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public short acceptByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r != Unconsumed) {
				return r;
			}
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		long pos = context.getPosition();
		int mark = context.startConstruction();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).matcher.match(context))) {
				context.abortConstruction(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		for(int i = this.size() -1; i >= 0; i--) {
			Expression e = this.get(i);
			next = e.encode(bc, next);
		}
		return next;
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
		for(Expression e: this) {
			e.examplfy(gep, sb, p);
		}
	}


}
