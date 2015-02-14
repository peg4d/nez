package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.util.UMap;

public class DefSymbol extends Unary {
	public final Tag table;
	DefSymbol(SourcePosition s, Tag table, Expression inner) {
		super(s, inner);
		this.table = table;
	}
	@Override
	public String getPredicate() {
		return "def " + table.name;
	}
	@Override
	public String getInterningKey() {
		return "def " + table.name;
	}
	@Override
	public boolean checkAlwaysConsumed(ExpressionChecker checker, String startNonTerminal, UList<String> stack) {
		if(!this.checkAlwaysConsumed(checker, startNonTerminal, stack) && checker != null) {
			checker.reportWarning(s, "unconsumed expression: " + this.inner);
		}
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(ExpressionChecker checker, NodeTransition c) {
		int t = this.inner.inferNodeTransition(null);
		if(t != NodeTransition.BooleanType) {
			this.inner = this.inner.removeNodeOperator();
		}
		return this;
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newDefSymbol(this.s, this.table, e) : this;
	}
	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}
	@Override
	public boolean match(SourceContext context) {
		long startIndex = context.getPosition();
		if(this.inner.matcher.match(context)) {
			long endIndex = context.getPosition();
			String s = context.substring(startIndex, endIndex);
			context.pushSymbolTable(table, s);
			return true;
		}
		return false;
	}
	
	// Utilities
	
	public static boolean checkContextSensitivity(Expression e, UMap<String> visitedMap) {
		if(e.size() > 0) {
			for(int i = 0; i < e.size(); i++) {
				if(checkContextSensitivity(e.get(i), visitedMap)) {
					return true;
				}
			}
			return false;
		}
		if(e instanceof NonTerminal) {
			String un = ((NonTerminal) e).getUniqueName();
			if(visitedMap.get(un) == null) {
				visitedMap.put(un, un);
				return checkContextSensitivity(((NonTerminal) e).getRule().getExpression(), visitedMap);
			}
			return false;
		}
		if(e instanceof Indent || e instanceof IsSymbol || e instanceof IsaSymbol) {
			return true;
		}
		return false;
	}
	
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		StringBuilder sb2 = new StringBuilder();
		inner.examplfy(gep, sb2, p);
		String token = sb2.toString();
		gep.addTable(table, token);
		sb.append(token);
	}
	
}