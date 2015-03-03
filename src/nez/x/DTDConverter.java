package nez.x;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.util.UList;

public class DTDConverter extends NodeVisitor {
	//	int AttID = 0;
	//	int DefCount = 0;
	

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
		String name = "El_" + node.textAt(0, "");
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitAttlist(AST node) {
		System.out.println("DEBUG? " + node);
		String name = node.textAt(0, "");
		//		AttID++;
		//		DefCount = 0;
		for (AST subnode : node) {
			this.visit("visit", subnode);
		}
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}
	//attlist = 'ATTLIST' _* {@Name _* (@Def)* #Attlist} _*

	public void visitDef(AST node) {
		System.out.println("DEBUG? " + node);
		//		String name = "AD_" + AttID + "_" + DefCount++;
		//		grammar.defineRule(node, name, toExpression(node.get(1)));
	}
	//Def = { @Name _* @Type _* @DefaultDecl _* #Def}

	public void visitEntity(AST node) {
		System.out.println("DEBUG? " + node);
		String name = node.textAt(0, "");
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}
	//Entity = { @Name @EntityValue #Entity}
	
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

	public Expression toZeroMore(AST node) {
		return Factory.newRepetition(node, toExpression(node.get(0)));
	}

	public Expression toChoice(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (AST subnode : node) {
			Factory.addChoice(l, toExpression(subnode));
		}
		return Factory.newChoice(node, l);
	}

	public Expression toSeq(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (AST subnode : node) {
			Factory.addSequence(l, toExpression(subnode));
		}
		return Factory.newSequence(node, l);
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

	public Expression toEntValue(AST node) {
		String replaceString = "&" + node.textAt(0, "") + ";";
		return Factory.newString(node, replaceString);
	}

	public Expression toName(AST node) {
		return null;
	}

	public Expression toElName(AST node) {
		String elementName = "El_" + node.textAt(0, "");
		return Factory.newNonTerminal(node, grammar, elementName);
	}
	public Expression toData(AST node) {
		return Factory.newNonTerminal(node, grammar, "PCdata");
	}

}
