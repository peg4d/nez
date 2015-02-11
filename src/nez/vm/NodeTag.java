package nez.vm;

import nez.ast.Tag;
import nez.expr.Tagging;
import nez.util.StringUtils;

public class NodeTag extends Instruction {
	public final Tag tag;
	public NodeTag(Optimizer optimizer, Tagging e, Instruction next) {
		super(optimizer, e, next);
		this.tag = e.tag;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opNodeTag(this);
	}	

	@Override
	boolean debug() {
		return true;
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("tag " + StringUtils.quoteString('"', tag.name, '"'));
	}
}
