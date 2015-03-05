package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.StringUtils;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Tagging extends ParsingOperation {
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
		return "tag " + StringUtils.quoteString('"', tag.getName(), '"');
	}
	@Override
	public String getInterningKey() {
		return "#" + this.tag.getName();
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this.checkTypestate(checker, c, "#" + tag.getName());
	}
	@Override
	public boolean match(SourceContext context) {
		context.left.setTag(this.tag);
		return true;
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeTagging(this, next);
	}
}