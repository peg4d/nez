package nez.expr;

import java.util.TreeMap;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.util.UList;

public class ExpressionChecker {
	
	public final static int Undefined         = -1;
	public final static int BooleanType       = 0;
	public final static int ObjectType        = 1;
	public final static int OperationType     = 2;

	public int required = BooleanType;

	boolean foundFlag  = false;

	public ExpressionChecker(int optimizedLevel) {
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
			System.out.println(s.formatSourceMessage("error", message));
		}
	}

	public void reportWarning(SourcePosition s, String message) {
		if(s != null) {
			System.out.println(s.formatSourceMessage("warning", message));
		}
	}

	public void reportNotice(SourcePosition s, String message) {
		if(s != null) {
			System.out.println(s.formatSourceMessage("notice", message));
		}
	}

	public void exit(int exit, String message) {
		System.exit(exit);
	}
	
	public void verify(Grammar grammar) {
//		if(stats != null) {
//			stats.setText("PEG", this.getName());
//			stats.setCount("PEG.NonTerminal", definedNameList.size());
//		}
		UList<String> stack = new UList<String>(new String[64]);
		for(Rule r: grammar.getDefinedRuleList()) {
			r.checkAlwaysConsumed(this, null, stack);
		}
		if(this.foundError) {
			this.exit(1, "PegError found");
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
//		if(stats != null) {
//			stats.setCount("PEG.NormalizedRules", definedNameList.size());
//			int count = 0;
//			UMap<String> flagMap = new UMap<String>();
//			for(int i = 0; i < definedNameList.size(); i++) {
//				ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
//				flagMap.clear();
//				if(ParsingDef.checkContextSensitivity(rule.expr, flagMap)) {
//					count+=1;
//				}
//			}
//			stats.setCount("PEG.ContextSensitiveRules", count);
//			stats.setRatio("PEG.ContextSensitivity", count, definedNameList.size());
//		}
//		try {
//			new Optimizer().optimize(this, stats);
//		}
//		catch(Exception e) {
//			System.out.println("Optimizer error: " + e);
//		}
//		ParsingContext context = new ParsingContext(null);
//		for(int i = 0; i < definedNameList.size(); i++) {
//			ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
//			if(rule.getGrammar() == this) {
//				rule.testExample1(this, context);
//			}
//		}

		// TODO Auto-generated method stub
		
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
