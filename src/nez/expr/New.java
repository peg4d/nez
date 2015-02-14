package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NodeCapture;
import nez.vm.NodeNew;
import nez.vm.Compiler;

public class New extends ExpressionList {
	int prefetchIndex = 0;
	New(SourcePosition s, UList<Expression> list) {
		super(s, list);
	}
	@Override
	public String getPredicate() { 
		return "new";
	}
	@Override
	public String getInterningKey() {
		return "{}";
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
		return NodeTransition.ObjectType;
	}
	@Override
	public Expression checkNodeTransition(ExpressionChecker checker, NodeTransition c) {
		if(c.required != NodeTransition.ObjectType) {
			checker.reportWarning(s, "unexpected { .. => removed!");
			return this.removeNodeOperator();
		}
		c.required = NodeTransition.OperationType;
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			Expression e = get(i).removeFlag(undefedFlags);
			Factory.addSequence(l, e);
		}
		return Factory.newNew(s, l);
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
		long startIndex = context.getPosition();
//
////		ParsingObject left = context.left;
//		for(int i = 0; i < this.prefetchIndex; i++) {
//			if(!this.get(i).matcher.match(context)) {
//				context.rollback(startIndex);
//				return false;
//			}
//		}
		int mark = context.startConstruction();
		Node newnode = context.newNode();
		context.left = newnode;
		for(int i = 0; i < this.size(); i++) {
			if(!this.get(i).matcher.match(context)) {
				context.abortConstruction(mark);
				context.rollback(startIndex);
				newnode = null;
				return false;
			}
		}
		newnode.setEndingPosition(context.getPosition());
		context.left = newnode;
		return true;
	}
	
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		next = new NodeCapture(bc, this, next);
		for(int i = this.size() -1; i >= 0; i--) {
			Expression e = this.get(i);
			next = e.encode(bc, next);
		}
		return new NodeNew(bc, this, next);
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