package nez.expr;
import java.util.AbstractList;
import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public abstract class Expression extends AbstractList<Expression> implements Recognizer {
	public final static boolean ClassicMode = false;
	SourcePosition s = null;
	int    internId   = 0;
	
	public Expression optimized;
	int    optimizedOption = -1;
	
	Expression(SourcePosition s) {
		this.s = s;
		this.internId = 0;
		this.optimized = this;
	}
	
	public final SourcePosition getSourcePosition() {
		return this.s;
	}

	public final int getId() {
		return this.internId;
	}
	
	final boolean isInterned() {
		return (this.internId > 0);
	}
	
	final Expression intern() {
		return Factory.intern(this);
	}

	public abstract String getPredicate();
	public abstract String getInterningKey();
	
	public final boolean isAlwaysConsumed() {
		return this.checkAlwaysConsumed(null, null, null);
	}

	public final int inferTypestate() {
		return this.inferTypestate(null);
	}

	public abstract boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack);
	public void checkGrammar(GrammarChecker checker) { }
	public abstract int inferTypestate(UMap<String> visited);
	public abstract Expression checkTypestate(GrammarChecker checker, Typestate c);

	public abstract Expression removeASTOperator();
	public abstract Expression removeFlag(TreeMap<String, String> undefedFlags);
	
	public static boolean hasReachableFlag(Expression e, String flagName, UMap<String> visited) {
		if(e instanceof WithFlag) {
			if(flagName.equals(((WithFlag) e).flagName)) {
				return false;
			}
		}
		for(Expression se : e) {
			if(hasReachableFlag(se, flagName, visited)) {
				return true;
			}
		}
		if(e instanceof IfFlag) {
			return flagName.equals(((IfFlag) e).flagName);
		}
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal)e;
			String un = ne.getUniqueName();
			if(!visited.hasKey(un)) {
				visited.put(un, un);
				Rule r = ne.getRule();
				return hasReachableFlag(r.body, flagName, visited);
			}
		}
		return false;
	}

	public static boolean hasReachableFlag(Expression e, String flagName) {
		return hasReachableFlag(e, flagName, new UMap<String>());
	}

	public final static short Unconsumed = 0;
	public final static short Accept = 1;
	public final static short Reject = 2;
	public abstract short acceptByte(int ch, int option);
	
	public final Expression optimize(int option) {
		if(this.optimizedOption != option) {
			optimizeImpl(option);
			this.optimizedOption = option;
		}
		return this.optimized;
	}
	
	void optimizeImpl(int option) {
		this.optimized = this;
	}

	@Override
	public String toString() {
		return new GrammarFormatter().format(this);
	}

	public final UList<Expression> toList() {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		if(this.size() > 1) {
			for(Expression e : this) {
				l.add(e);
			}
		}
		else {
			l.add(this);
		}
		return l;
	}

	public final void visit(GrammarVisitor visitor) {
		visitor.visit(this);
	}

	public abstract Instruction encode(RuntimeCompiler bc, Instruction next, boolean[] dfa);
//	public Instruction encode(Compiler bc, Instruction next) {
//		// todo
//		return next;
//	}

	protected abstract int pattern(GEP gep);
	protected abstract void examplfy(GEP gep, StringBuilder sb, int p);

	@Override
	public boolean match(SourceContext context) {
		throw new RuntimeException("old one");
	}


}
