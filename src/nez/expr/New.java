package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class New extends SequentialExpression {
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
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
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
	public int inferTypestate(UMap<String> visited) {
		return Typestate.ObjectType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		if(c.required != Typestate.ObjectType) {
			checker.reportWarning(s, "unexpected { .. => removed!");
			return this.removeNodeOperator();
		}
		c.required = Typestate.OperationType;
		for(Expression p: this) {
			p.checkTypestate(checker, c);
		}
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
		return bc.encodeNew(this, next);
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