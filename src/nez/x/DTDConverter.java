package nez.x;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.expr.Expression;
import nez.expr.Factory;

public class DTDConverter extends NodeVisitor {
	
	Grammar grammar;
	DTDConverter(Grammar grammar) {
		this.grammar = grammar;
	}
	
	public void visitDoc(AST node) {
		System.out.println("DEBUG? " + node);
		for(AST subnode: node) {
			this.visit("visit", subnode);
		}
	}

	public void visitElement(AST node) {
		System.out.println("DEBUG? " + node);
		String name = node.textAt(0, "");
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	private Expression toExpression(AST node) {
		return (Expression)this.visit("to", node);
	}
	
	public Expression toEMPTY(AST node) {
		return Factory.newNonTerminal(node, grammar, "EMPTY");
	}
}
