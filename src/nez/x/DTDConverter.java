package nez.x;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.expr.Expression;

public class DTDConverter extends NodeVisitor {
	int attID;
	int attDefCount = 0;
	int elementCount = 0;
	int entityCount = 0;
	Map<Integer, String> elementMap = new HashMap<>();
	Map<String, Integer> attributeMap = new HashMap<>();
	List<Integer> reqList;
	List<Integer> impList;
	

	Grammar grammar;
	DTDConverter(Grammar grammar) {
		this.grammar = grammar;
	}
	
	public void initAttCounter() {
		attID = elementCount - 1;
		attDefCount = 0;
		reqList = new ArrayList<Integer>();
		impList = new ArrayList<Integer>();
	}

	public int[] initAttDefList() {
		int[] attDefList = new int[attDefCount];
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
		for (int elementID = 0; elementID < elementCount; elementID++) {
			grammar.defineRule(node, "Element" + elementID, genElement(node, elementID));
		}
		grammar.defineRule(node, "entity", genEntityList(node));
	}



	public void visitElement(AST node) {
		System.out.println("DEBUG? " + node);
		String elementName = node.textAt(0, "");
		elementMap.put(elementCount, elementName);
		grammar.defineRule(node, "El_" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(AST node, int elementID) {
		String elementName = elementMap.get(elementID);
		if (attributeMap.containsValue(elementID)) { // check whether attribute exists
			Expression[] l = {
			grammar.newString("<" + elementName),
			grammar.newNonTerminal("At_" + elementID),
			grammar.newString(">"),
			grammar.newNonTerminal("El_" + elementID),
			grammar.newString("</" + elementName + ">")
			};
			return grammar.newSequence(l);
		}
		else {
			Expression[] l = {
			grammar.newString("<" + elementName),
			grammar.newString( ">"),
			grammar.newNonTerminal("El_" + elementID),
			grammar.newString("</" + elementName + ">")
			};
			return grammar.newSequence(l);
		}
	}

	public void visitAttlist(AST node) {
		System.out.println("DEBUG? " + node);
		initAttCounter();
		String elementName = node.textAt(0, "");
		attributeMap.put(elementName, attID);
		String attListName = "At_" + attID;
		String choiceListName = "AttChoice" + attID;
		for (AST subnode : node) {
			this.visit("visit", subnode);
		}
		int[] attDefList = initAttDefList();
		if (impList.isEmpty()) {
			grammar.defineRule(node, attListName, genCompAtt(node, attDefList));
		} else {
			int[] requiredRules = extractRequiredRule(attDefList);
			grammar.defineRule(node, choiceListName, genImpliedChoice(node));
			grammar.defineRule(node, attListName, genProxAtt(node, requiredRules));
		}
	}

	public void visitREQUIRED(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD" + attID + "_" + attDefCount++;
		reqList.add(attDefCount);
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitIMPLIED(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD" + attID + "_" + attDefCount++;
		impList.add(attDefCount);
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitFIXED(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD" + attID + "_" + attDefCount++;
		impList.add(attDefCount);
		grammar.defineRule(node, name, genFixedAtt(node));
	}


	public void visitDefault(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "AD" + attID + "_" + attDefCount++;
		impList.add(attDefCount);
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}

	public void visitEntity(AST node) {
		System.out.println("DEBUG? " + node);
		String name = "ENT_" + entityCount++;
		grammar.defineRule(node, name, toExpression(node.get(1)));
	}
	
	private Expression toExpression(AST node) {
		return (Expression)this.visit("to", node);
	}
	
	public Expression toEMPTY(AST node) {
		return grammar.newNonTerminal("EMPTY");
	}

	public Expression toANY(AST node) {
		return grammar.newNonTerminal("ANY");
	}

	public Expression toZeroMore(AST node) {
		return grammar.newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(AST node) {
		Expression[] l = {
		toExpression(node.get(0)),
		grammar.newRepetition(toExpression(node.get(0)))
		};
		return grammar.newSequence(l);
	}

	public Expression toOption(AST node) {
		return grammar.newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(AST node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (AST subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return grammar.newChoice(l);
	}

	public Expression toSeq(AST node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (AST subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return grammar.newSequence(l);
	}

	public Expression toCDATA(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "="),
				grammar.newNonTerminal("STRING"),
		};
		return grammar.newSequence(l);
	}

	public Expression toID(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "=\""),
				grammar.newNonTerminal("IDTOKEN"),
				grammar.newString("\"")
		//		grammar.newDefSymbol(Tag.tag("IDLIST"),
		//				grammar.newNonTerminal("IDTOKEN")));
		};
		return grammar.newSequence(l);

	}

	public Expression toIDREF(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "=\""),
				grammar.newNonTerminal("IDTOKEN"),
		//		(grammar.newIsaSymbol(node, Tag.tag("IDLIST")));
				grammar.newString("\"")
		};
		return grammar.newSequence(l);
	}

	public Expression toIDREFS(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "=\""),
				grammar.newNonTerminal("IDTOKENS"),
		//(grammar.newRepetition(node, grammar.newIsaSymbol(node, Tag.tag("IDLIST"))));
				grammar.newString("\"")
		};
		return grammar.newSequence(l);
	}

	private Expression genFixedAtt(AST node) {
		String attName = node.textAt(0, "");
		String fixedValue = node.textAt(2, "");
		Expression[] l ={
				grammar.newString(attName + "=" + fixedValue),
				grammar.newNonTerminal("STRING")
		};
		return grammar.newSequence(l);
	}

	public Expression toENTITY(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l ={
		grammar.newString(attName + "=\""),
		grammar.newNonTerminal("entity"),
		grammar.newString("\"")
		};
		return grammar.newSequence(l);
	}

	public Expression toENTITIES(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "=\""),
				grammar.newNonTerminal("entities"),
				grammar.newString("\"")
		};
		return grammar.newSequence(l);
	}

	public Expression toNMTOKEN(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "="),
				grammar.newNonTerminal("NMTOKEN")
		};
		return grammar.newSequence(l);
	}

	public Expression toNMTOKENS(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "="),
				grammar.newNonTerminal("NMTOKENS")
		};
		return grammar.newSequence(l);
	}
	
	public Expression genCompAtt(AST node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 1) {
			Expression[] l = {
					grammar.newNonTerminal("AD" + attID + "_" + attlist[0]),
					grammar.newRepetition(grammar.newNonTerminal("_")),
					grammar.newNonTerminal("ENDTAG")
			};
			return grammar.newSequence(l);
		} else {
			int[][] permedList = perm(attlist);
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] target : permedList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < target.length; index++) {
					seqList[index] = grammar.newNonTerminal("AD" + attID + "_"
							+ target[index]);
				}
				seqList[listLength] = grammar.newNonTerminal("ENDTAG");
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}
	

	public Expression genProxAtt(AST node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			Expression[] l = {
					grammar.newNonTerminal("AD" + attID + "_" + attlist[0]),
					grammar.newRepetition(grammar.newNonTerminal("_")),
					grammar.newNonTerminal("ENDTAG")
			};
			return grammar.newSequence(l);
		} else {
			int[][] permedList = perm(attlist);

			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] target : permedList) {
				Expression[] seqList = new Expression[listLength + 1];
				int seqCount = 0;
				seqList[seqCount++] = grammar
						.newOption(grammar.newNonTerminal("AttChoice" + attID));
				for (int index = 0; index < target.length; index++) {
					seqList[seqCount++] = grammar.newNonTerminal("AD" + attID + "_"
							+ target[index]);
					seqList[seqCount++] = grammar.newOption(grammar.newNonTerminal("AttChoice"
							+ attID));
				}
				seqList[seqCount] = grammar.newNonTerminal("ENDTAG");
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	public Expression genImpliedChoice(AST node){
		Expression[] l = new Expression[impList.size()];
		int choiceCount = 0;
		for (Integer ruleNum : impList) {
			l[choiceCount++] = grammar.newNonTerminal( "AD_" + ruleNum);
		}
		return grammar.newChoice(l);
	}
	


	public Expression toEnum(AST node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName + "="),
				toChoice(node)
		};
		return grammar.newSequence(l);
	}

	public Expression toEntValue(AST node) {
		String replaceString = node.textAt(0, "");
		return grammar.newString(replaceString);
	}

	public Expression toElName(AST node) {
		String elementName = "El_" + node.textAt(0, "");
		return grammar.newNonTerminal(elementName);
	}
	public Expression toData(AST node) {
		return grammar.newNonTerminal("PCDATA");
	}

	private Expression genEntityList(AST node) {
		if (entityCount == 0) {
			return grammar.newNonTerminal("NotAny");
		}
		else {
			Expression[] l = new Expression[entityCount];
			for (int entityNum = 0; entityNum < entityCount; entityNum++) {
				l[entityNum] = grammar.newNonTerminal("ENT_" + entityNum);
			}
			return grammar.newChoice(l);
		}
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


