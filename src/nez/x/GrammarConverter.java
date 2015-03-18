package nez.x;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.util.FileBuilder;

public abstract class GrammarConverter extends NodeVisitor {
	final protected FileBuilder file;
	final protected Grammar grammar;
	public GrammarConverter(Grammar peg, String name) {
		this.file = new FileBuilder(name);
		this.grammar = peg;
	}
	public abstract String getDesc();
	public abstract void convert(AST node);
}
