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

	public int[] initAttDefList() {
		int[] attDefList = new int[defCount];
		for (int i = 0; i < attDefList.length; i++) {
			attDefList[i] = i;
		}
		return attDefList;
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
		String attListName = "At_" + node.textAt(0, "");
		String choiceListName = "AC_" + attID;
		initAttCounter();
		for (AST subnode : node) {
			this.visit("visit", subnode);
		}
		int[] attDefList = initAttDefList();
		if (impList.isEmpty()) {
			grammar.defineRule(node, attListName, toCompAtt(node, attDefList));
		} else {
			int[] requiredRules = extractRequiredRule(attDefList);
			grammar.defineRule(node, choiceListName, toImpChoice(node));
			grammar.defineRule(node, attListName, toApproAtt(node, requiredRules));
		}
	}

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
	
	public Expression toCompAtt(AST node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 1) {
			UList<Expression> l = new UList<Expression>(new Expression[3]);
			l.add(Factory.newNonTerminal(node, grammar, "AD_" + attlist[0]));
			l.add(Factory.newRepetition(node, Factory.newNonTerminal(node, grammar, "_")));
			l.add(Factory.newNonTerminal(node, grammar, "ENDTAG"));
			return Factory.newSequence(node, l);
		} else {
			int[][] permedList = perm(attlist);
			UList<Expression> choiceList = new UList<Expression>(
					new Expression[permedList.length]);
			for (int[] target : permedList) {
				UList<Expression> sequenceList = new UList<Expression>(
						new Expression[listLength + 1]);
				for (int index = 0; index < target.length; index++) {
					sequenceList.add(Factory.newNonTerminal(node, grammar, "AD_" + target[index]));
				}
				sequenceList.add(Factory.newNonTerminal(node, grammar, "ENDTAG"));
				choiceList.add(Factory.newSequence(node, sequenceList));
			}
			return Factory.newChoice(node, choiceList);
		}
	}
	

	public Expression toApproAtt(AST node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			UList<Expression> l = new UList<Expression>(new Expression[3]);
			l.add(Factory.newNonTerminal(node, grammar, "AD_" + attlist[0]));
			l.add(Factory.newRepetition(node, Factory.newNonTerminal(node, grammar, "_")));
			l.add(Factory.newNonTerminal(node, grammar, "ENDTAG"));
			return Factory.newSequence(node, l);
		} else {
			int[][] permedList = perm(attlist);
			UList<Expression> choiceList = new UList<Expression>(
					new Expression[permedList.length]);
			for (int[] target : permedList) {
				UList<Expression> sequenceList = new UList<Expression>(
						new Expression[listLength + 1]);
				for (int index = 0; index < target.length; index++) {
					sequenceList.add(Factory.newNonTerminal(node, grammar, "AD_" + target[index]));
				}
				sequenceList.add(Factory.newNonTerminal(node, grammar, "ENDTAG"));
				choiceList.add(Factory.newSequence(node, sequenceList));
			}
			return Factory.newChoice(node, choiceList);
		}
	}

	public Expression toImpChoice(AST node){
		UList<Expression> l = new UList<Expression>(new Expression[impList.size()]);
		for (Integer ruleNum : impList) {
			Factory.addChoice(l, Factory.newNonTerminal(node, grammar, "AD_" + ruleNum));
		}
		return Factory.newChoice(node, l);
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

	private final int[] extractRequiredRule(int[] attlist) {
		int[] buf = new int[512];
		int arrIndex = 0;
		for (int requiredNum : attlist) {
			if (reqList.contains(requiredNum)) {
				buf[arrIndex++] = requiredNum;
			}
		}
		int[] target = new int[arrIndex];
		for (int i = 0; i < arrIndex; i++) {
			target[i] = buf[i];
		}
		return target;
	}

	private int[][] perm(int[] attlist) {
		Permutation permutation = new Permutation(attlist);
		return permutation.getPermList();
	}

}


