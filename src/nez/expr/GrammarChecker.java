package nez.expr;

import java.util.TreeMap;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class GrammarChecker {
	
	public final static int Undefined         = -1;
	public final static int BooleanType       = 0;
	public final static int ObjectType        = 1;
	public final static int OperationType     = 2;

	public int required = BooleanType;

	boolean foundFlag  = false;

	public GrammarChecker(int checkerLevel) {
		// TODO Auto-generated constructor stub
	}

	public final void foundFlag() {
		this.foundFlag = true;
	}

	boolean foundError = false;
	
	public final void foundFatalError() {
		this.foundError = true;
	}
	
	public void reportError(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("error", message));
		}
	}

	public void reportWarning(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("warning", message));
		}
	}

	public void reportNotice(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("notice", message));
		}
	}

	public void exit(int exit, String message) {
		ConsoleUtils.exit(exit, message);
	}
	
	public void verify(Grammar grammar) {
		UList<String> stack = new UList<String>(new String[64]);
		UMap<String> visited = new UMap<String>();
		for(Rule r: grammar.getDefinedRuleList()) {
			if(r.isTerminal) {
				continue;
			}
			r.minlen = -1;  // reset for all checking
			r.checkAlwaysConsumed(this, null, stack);
			visited.clear();
			checkPhase1(r.getExpression(), r.getUniqueName(), visited, 0);
		}
		if(this.foundError) {
			ConsoleUtils.exit(1, "FatalGrammarError");
		}
		// type check
		for(Rule r: grammar.getRuleList()) {
			if(r.isTerminal) {
				continue;
			}
			this.checkPhase2(r.getExpression());
			if(r.refCount == 0 && !r.isPublic) {
				this.reportWarning(r.s, "unused nonterminal definition");
			}
			r.checkTypestate(this, new Typestate());
		}		
		// interning
		for(Rule r: grammar.getRuleList()) {
			if(r.isTerminal) {
				continue;
			}
			if(Verbose.Grammar) {
				r.dump();
			}
			r.internRule();
		}
		if(this.foundFlag) {
			TreeMap<String,String> undefedFlags = new TreeMap<String,String>();
			for(Rule r: grammar.getRuleList()) {
				r.removeExpressionFlag(undefedFlags);
			}
		}
	}
	
	void checkPhase1(Expression p, String ruleName, UMap<String> visited, int depth) {
		p.checkPhase1(this, ruleName, visited, depth);
		for(Expression e: p) {
			this.checkPhase1(e, ruleName, visited, depth);
		}
	}

	void checkPhase2(Expression p) {
		p.checkPhase2(this);
		for(Expression e: p) {
			this.checkPhase2(e);
		}
	}
	
	private UMap<Expression> tableMap; 

	final void setSymbolExpresion(String tableName, Expression e) {
		if(tableMap == null) {
			tableMap = new UMap<Expression>();
		}
		tableMap.put(tableName, e);
	}

	final Expression getSymbolExpresion(String tableName) {
		if(tableMap != null) {
			return tableMap.get(tableName);
		}
		return null;
	}
	
	
}
