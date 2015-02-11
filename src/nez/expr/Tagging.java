package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NodeTag;
import nez.vm.Optimizer;

public class Tagging extends Unconsumed {
	public Tag tag;
	Tagging(SourcePosition s, Tag tag) {
		super(s);
		this.tag = tag;
	}
	Tagging(SourcePosition s, String name) {
		this(s, Tag.tag(name));
	}
	@Override
	public String getPredicate() {
		return "tag " + StringUtils.quoteString('"', tag.name, '"');
	}
	@Override
	public String getInterningKey() {
		return "#" + this.tag.toString();
	}
	@Override
	public Expression checkNodeTransition(ExpressionChecker checker, NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			checker.reportWarning(s, "unexpected #" + tag.toString() + " => removed!!");
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
		context.left.setTag(this.tag);
		return true;
	}
	@Override
	public Instruction encode(Optimizer optimizer, Instruction next) {
		return new NodeTag(optimizer, this, next);
	}

}