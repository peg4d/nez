package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Replace extends Unconsumed {
	public String value;
	Replace(SourcePosition s, String value) {
		super(s);
		this.value = value;
	}
	@Override
	public String getPredicate() {
		return "replace " + StringUtils.quoteString('"', value, '"');
	}
	@Override
	public String getInterningKey() {
		return "`" + this.value;
	}
	@Override
	public Expression checkNodeTransition(GrammarChecker checker, NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			checker.reportWarning(s, "unexpected `" + value + "` => removed");
			return Factory.newEmpty(this.s);
		}
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		return Factory.newEmpty(null);
	}
	@Override
	public boolean match(SourceContext context) {
		context.left.setValue(this.value);
		return true;
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeReplace(this, next);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return 0;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}


}