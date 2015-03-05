package nez.expr;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public abstract class ParsingOperation extends Expression {
	ParsingOperation(SourcePosition s) {
		super(s);
	}

	@Override
	public Expression get(int index) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.OperationType;
	}
	public Expression checkTypestate(GrammarChecker checker, Typestate c, String operator) {
		if(c.required != Typestate.OperationType) {
			checker.reportWarning(s, "unexpected " + operator + " .. => removed!");
			return Factory.newEmpty(s);
		}
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		return Factory.newEmpty(s);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Unconsumed;
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {

	}


}
