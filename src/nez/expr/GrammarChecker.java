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

	public GrammarChecker(int optimizedLevel) {
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
		for(Rule r: grammar.getDefinedRuleList()) {
			r.minlen = -1;  // reset for all checking
			r.checkAlwaysConsumed(this, null, stack);
			if(Verbose.Grammar) {
				ConsoleUtils.println(r.getUniqueName() + " = " + r.getExpression());
			}
			checkSymbolTable(r.getExpression());
		}
		if(this.foundError) {
			ConsoleUtils.exit(1, "FatalGrammarError");
		}
		// type check
		for(Rule r: grammar.getRuleList()) {
			r.checkNodeTransition(this, new NodeTransition());
		}
		// interning
		for(Rule r: grammar.getRuleList()) {
			r.internRule();
		}
		if(this.foundFlag) {
			TreeMap<String,String> undefedFlags = new TreeMap<String,String>();
			for(Rule r: grammar.getRuleList()) {
				r.removeExpressionFlag(undefedFlags);
			}
		}
//		ParsingContext context = new ParsingContext(null);
//		for(int i = 0; i < definedNameList.size(); i++) {
//			ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
//			if(rule.getGrammar() == this) {
//				rule.testExample1(this, context);
//			}
//		}
// TODO Auto-generated method stub
		
	}

	private UMap<Expression> tableMap; 
	private void checkSymbolTable(Expression p) {
		if(p instanceof DefSymbol) {
			DefSymbol def = (DefSymbol)p;
			if(tableMap == null) {
				tableMap = new UMap<Expression>();
			}
			tableMap.put(def.table.name, def.inner); 
		}
		for(Expression e: p) {
			this.checkSymbolTable(e);
		}
	}
	
	public final Expression getSymbolExpression(String tableName) {
		if(this.tableMap != null) {
			return this.tableMap.get(tableName);
		}
		return null;
	}
	
//	final void optimizeConstructor(Constructor holder) {
//		if(is(O_LazyObject)) {
//			int prefetchIndex = 0;
//			for(int i = 0; i < holder.size(); i++) {
//				Expression sub = holder.get(i);
//				if(sub.hasObjectOperation()) {
//					break;
//				}
//				prefetchIndex = i + 1;
//			}
//			if(prefetchIndex > 0) {
//				CountLazyObject += 1;
//				holder.prefetchIndex = prefetchIndex;
//			}
//		}
//	}
//
//	final void optimizeSequence(Sequence holder) {
//		if(is(O_SpecLexer) && holder.size() == 2 && holder.get(0) instanceof Not && holder.get(1) instanceof Any) {
//			Expression inner = ((Not)holder.get(0)).inner;
//			if(this.is(O_Inline) && inner instanceof NonTerminal) {
//				inner = resolveNonTerminal(inner);
//			}
//			if(inner instanceof Byte) {
//				holder.matcher = new ByteMapMatcher(((Byte) inner).byteChar);
//				CountSpecLexer += 1;
//				return;
//			}
//			Recognizer m = inner.matcher;
//			if(m instanceof ByteMapMatcher) {
//				holder.matcher = new ByteMapMatcher(((ByteMapMatcher) m), false);
//				CountSpecLexer += 1;
//				return;
//			}
//			//System.out.println("not any " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
//		}
//		if(is(O_SpecString)) {
//			byte[] u = new byte[holder.size()];
//			for(int i = 0; i < holder.size(); i++) {
//				Expression inner = resolveNonTerminal(holder.get(i));				
//				if(inner instanceof Byte) {
//					u[i] = (byte)((Byte) inner).byteChar;
//					continue;
//				}
//				return;
//			}
//			holder.matcher = new StringMatcher(u);
//			CountSpecString += 1;
//			return;
//		}
//	}
	
}
