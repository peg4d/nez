package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.Optimizer;

public class WithFlag extends Unary {
	String flagName;
	WithFlag(SourcePosition s, String flagName, Expression inner) {
		super(s, inner);
		this.flagName = flagName;
		this.matcher = inner.matcher;
	}
	@Override
	public String getPredicate() {
		return "with " + this.flagName;
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newWithFlag(this.s, this.flagName, e) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(ExpressionChecker checker, String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return this.inner.inferNodeTransition(visited);
	}
	@Override
	public Expression checkNodeTransition(ExpressionChecker checker, NodeTransition c) {
		this.inner = this.inner.checkNodeTransition(checker, c);
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		boolean removeWithout = false;
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			undefedFlags.remove(flagName);
			removeWithout = true;
		}
		Expression e = inner.removeFlag(undefedFlags);
		if(removeWithout) {
			undefedFlags.put(flagName, flagName);
		}
		return e;
	}
	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public boolean match(SourceContext context) {
		return this.inner.matcher.match(context);
	}
	
	@Override
	public Instruction encode(Optimizer optimizer, Instruction next) {
		return this.inner.encode(optimizer, next);
	}

	
}