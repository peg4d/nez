package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;
import nez.util.UList;

public class IsIndent extends Terminal {
	IsIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "indent";
	}
	@Override
	public String getInterningKey() {
		return "indent";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTable(NezTag.Indent, true);
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeIsIndent(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.getSymbol(NezTag.Indent);
		sb.append(token);
	}
}