package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.runtime.Compiler;
import nez.runtime.Instruction;
import nez.util.UList;
import nez.util.UMap;

public class IsSymbol extends Terminal implements ContextSensitive {
	public Tag table;
	Expression symbolExpression = null;
	public boolean checkLastSymbolOnly = false;
	IsSymbol(SourcePosition s, boolean checkLastSymbolOnly, Tag table) {
		super(s);
		this.table = table;
		this.checkLastSymbolOnly = false;
	}
	public final Expression getSymbolExpression() {
		return this.symbolExpression;
	}
	@Override
	public String getPredicate() {
		return (checkLastSymbolOnly ? "is " : "isa ") + table.name;
	}
	@Override
	public String getInterningKey() {
		return this.getPredicate();
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		if(this.symbolExpression == null) {
			this.symbolExpression = checker.getSymbolExpression(table.name);
			if(this.symbolExpression == null) { 
				checker.reportError(s, "undefined table " + table.name);
			}
		}
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTable(table, this.checkLastSymbolOnly);
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeIsSymbol(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.getSymbol(table);
		sb.append(token);
	}
}