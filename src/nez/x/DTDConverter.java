package nez.x;

import java.util.ArrayList;
import java.util.List;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.util.UList;

public class DTDConverter extends NodeVisitor {
	int attID = 0;
	int defCount = 0;
	List<Integer> reqList;
	List<Integer> impList;
	

	Grammar grammar;
	DTDConverter(Grammar grammar) {
		this.grammar = grammar;
	}
	
	public void initAttCounter() {
		attID++;
		defCount = 0;
		reqList = new ArrayList<Integer>();
		impList = new ArrayList<Integer>();
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
		initAttCounter();
		for (AST subnode : node) {
			this.visit("visit", subnode);
		}
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}
	//attlist = 'ATTLIST' _* {@Name _* (@Def)* #Attlist} _*

	public void visitREQUIRED(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD_" + attID + "_" + defCount++;
		reqList.add(defCount);
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitIMPLIED(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD_" + attID + "_" + defCount++;
		impList.add(defCount);
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitFIXED(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD_" + attID + "_" + defCount++;
		impList.add(defCount);
		grammar.defineRule(node, name, toFixedAtt(node));
	}


	public void visitDefault(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD_" + attID + "_" + defCount++;
		impList.add(defCount);
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

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

	public Expression toZeroMore(AST node) {
		return Factory.newRepetition(node, toExpression(node.get(0)));
	}

	public Expression toOneMore(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		l.add(toExpression(node.get(0)));
		l.add(Factory.newRepetition(node, toExpression(node.get(0))));
		return Factory.newSequence(node, l);
	}

	public Expression toOption(AST node) {
		return Factory.newOption(node, toExpression(node.get(0)));
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

	public Expression toCDATA(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "=" ));
		l.add(Factory.newNonTerminal(node, grammar, "STRING"));
		return Factory.newSequence(node, l);
	}

	public Expression toID(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[3]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "=\""));
		l.add(Factory.newNonTerminal(node, grammar, "IDTOKEN"));
		l.add(Factory.newString(node, "\""));
		//		l.add(Factory.newDefSymbol(node, Tag.tag("IDLIST"),
		//				Factory.newNonTerminal(node, grammar, "IDTOKEN")));
		return Factory.newSequence(node, l);
	}

	public Expression toIDREF(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[3]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "=\""));
		l.add(Factory.newNonTerminal(node, grammar, "IDTOKEN"));
		//		l.add(Factory.newIsaSymbol(node, Tag.tag("IDLIST")));
		l.add(Factory.newString(node, "\""));
		return Factory.newSequence(node, l);
	}

	public Expression toIDREFS(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[3]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "=\""));
		l.add(Factory.newNonTerminal(node, grammar, "IDTOKENS"));
		//l.add(Factory.newRepetition(node, Factory.newIsaSymbol(node, Tag.tag("IDLIST"))));
		l.add(Factory.newString(node, "\""));
		return Factory.newSequence(node, l);
	}

	private Expression toFixedAtt(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		String attName = node.textAt(0, "");
		String fixedValue = node.textAt(2, "");
		l.add(Factory.newString(node, attName + "=" + fixedValue));
		l.add(Factory.newNonTerminal(node, grammar, "STRING"));
		return Factory.newSequence(node, l);
	}

	public Expression toENTITY(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[3]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "=\""));
		l.add(Factory.newNonTerminal(node, grammar, "entity"));
		l.add(Factory.newString(node, "\""));
		return Factory.newSequence(node, l);
	}

	public Expression toENTITIES(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[3]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "=\""));
		l.add(Factory.newNonTerminal(node, grammar, "entities"));
		l.add(Factory.newString(node, "\""));
		return Factory.newSequence(node, l);
	}

	public Expression toNMTOKEN(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "="));
		l.add(Factory.newNonTerminal(node, grammar, "NMTOKEN"));
		return Factory.newSequence(node, l);
	}

	public Expression toNMTOKENS(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "="));
		l.add(Factory.newNonTerminal(node, grammar, "NMTOKENS"));
		return Factory.newSequence(node, l);
	}

	public Expression toEnum(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		String attName = node.getParent().textAt(0, "");
		l.add(Factory.newString(node, attName + "="));
		l.add(toChoice(node));
		return Factory.newSequence(node, l);
	}

	public Expression toEntValue(AST node) {
		String replaceString = "&" + node.textAt(0, "") + ";";
		return Factory.newString(node, replaceString);
	}

	public Expression toElName(AST node) {
		String elementName = "El_" + node.textAt(0, "");
		return Factory.newNonTerminal(node, grammar, elementName);
	}
	public Expression toData(AST node) {
		return Factory.newNonTerminal(node, grammar, "PCdata");
	}
}


