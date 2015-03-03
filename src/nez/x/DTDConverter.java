package nez.x;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.util.UList;

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


	public void visitAttlist(AST node) {
		System.out.println("DEBUG? " + node);
		String name = node.textAt(0, "");
		for (AST subnode : node) {
			this.visit("visit", subnode);
		}
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitDef(AST node) {
		System.out.println("DEBUG? " + node);
		String name = node.textAt(0, "");
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}


	public void visitEntity(AST node) {
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

	public Expression toANY(AST node) {
		return Factory.newNonTerminal(node, grammar, "ANY");
	}

	public Expression toOneMore(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		l.add(toExpression(node.get(0)));
		l.add(Factory.newRepetition(node, toExpression(node.get(0))));
		return Factory.newSequence(node, l);
	}

	public Expression toChoice(AST node) {
		return null;
	}

	public Expression toSeq(AST node) {
		return null;
	}

	public Expression toID(AST node) {
		return null;
	}

	public Expression toIDREF(AST node) {
		return null;
	}

	public Expression toIDREFS(AST node) {
		return null;
	}

	public Expression toENTITY(AST node) {
		return null;
	}

	public Expression toENTITIES(AST node) {
		return null;
	}

	public Expression toNMTOKEN(AST node) {
		return null;
	}

	public Expression toNMTOKENS(AST node) {
		return null;
	}

	public Expression toREQUIRED(AST node) {
		return null;
	}

	public Expression toIMPLIED(AST node) {
		return null;
	}

	public Expression toFIXED(AST node) {
		return null;
	}

	public Expression toValue(AST node) {
		return null;
	}

	public Expression toName(AST node) {
		return null;
	}

}
