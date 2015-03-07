package nez.expr;

import java.util.TreeMap;

import nez.Grammar;
import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;
import nez.util.UList;
import nez.util.UMap;

public class NonTerminal extends Expression {
	public Grammar peg;
	public String  ruleName;
	String  uniqueName;
	public NonTerminal(SourcePosition s, Grammar peg, String ruleName) {
		super(s);
		this.peg = peg;
		this.ruleName = ruleName;
		this.uniqueName = this.peg.uniqueName(this.ruleName);
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Expression get(int index) {
		return null;
	}
	
	@Override
	public String getInterningKey() {
		return getUniqueName();
	}

	@Override
	public String getPredicate() {
		return getUniqueName();
	}

	public final String getLocalName() {
		return ruleName;
	}

	public final String getUniqueName() {
		return this.uniqueName;
	}
	
	public final Rule getRule() {
		return this.peg.getRule(this.ruleName);
	}
	
	public final Expression deReference() {
		Rule r = this.peg.getRule(this.ruleName);
		return (r != null) ? r.getExpression() : null;
	}
	
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		Rule r = this.getRule();
		if(r == null) {
			checker.reportWarning(s, "undefined rule: " + this.ruleName + " => created empty rule!!");
			r = this.peg.newRule(this.ruleName, Factory.newEmpty(s));
		}
		if(startNonTerminal != null && startNonTerminal.equals(this.uniqueName)) {
			checker.reportError(s, "left recursion: " + this.ruleName);
			checker.foundFatalError();
			return false;
		}
		return r.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}

	@Override
	public int inferTypestate(UMap<String> visited) {
		Rule r = this.getRule();
		return r.inferTypestate(visited);
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		Rule r = this.getRule();
		int t = r.inferTypestate();
		if(t == Typestate.BooleanType) {
			return this;
		}
		if(c.required == Typestate.ObjectType) {
			if(t == Typestate.OperationType) {
				checker.reportWarning(s, "unexpected AST operations => removed!!");
				return this.removeNodeOperator();
			}
			c.required = Typestate.OperationType;
			return this;
		}
		if(c.required == Typestate.OperationType) {
			if(t == Typestate.ObjectType) {
				checker.reportWarning(s, "expected @ => inserted!!");
				return Factory.newLink(this.s, this, -1);
			}
		}
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		Rule r = (Rule)this.getRule().removeNodeOperator();
		if(!this.ruleName.equals(r.getLocalName())) {
			return Factory.newNonTerminal(this.s, peg, r.getLocalName());
		}
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		Rule r = (Rule)this.getRule().removeFlag(undefedFlags);
		if(!this.ruleName.equals(r.getLocalName())) {
			return Factory.newNonTerminal(this.s, peg, r.getLocalName());
		}
		return this;
	}
	
	@Override
	public short acceptByte(int ch) {
		return this.deReference().acceptByte(ch);
	}

	@Override
	public Expression optimize(int option) {
		Expression e = this;
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference().optimize(option);
		}
		return e;
	}

	@Override
	public boolean match(SourceContext context) {
		return context.matchNonTerminal(this);
	}
	
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeNonTerminal(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return this.deReference().pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.deReference().examplfy(gep, sb, p);
	}


}
