package nez.vm;

import nez.expr.Rule;

public class Return extends Instruction implements StackOperation {
	public Return(Compiler optimizer, Rule e) {
		super(optimizer, e, null);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opReturn();
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("ret  ## " + ((Rule)e).getLocalName());
	}

}
