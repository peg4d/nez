package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;
import nez.util.StringUtils;

public class Replace extends ASTOperation {
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
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this.checkTypestate(checker, c, "`" + value + "`");
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
}