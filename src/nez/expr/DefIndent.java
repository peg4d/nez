package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;

public class DefIndent extends Unconsumed {
	DefIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "defindent";
	}
	@Override
	public boolean match(SourceContext context) {
		String indent = context.getIndentText(context.pos);
		context.pushSymbolTable(NezTag.Indent, indent);
		return true;
	}

	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.addIndent();
		sb.append(token);
	}

}