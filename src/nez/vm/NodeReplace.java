package nez.vm;

import nez.expr.Replace;
import nez.util.StringUtils;

public class NodeReplace extends Instruction {
	public final String value;
	public NodeReplace(Compiler optimizer, Replace e, Instruction next) {
		super(optimizer, e, next);
		this.value = e.value;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opReplace(this);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("replace " + StringUtils.quoteString('"', value, '"'));
	}
}
