package nez.expr;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.ast.AST;
import nez.ast.NodeVisitor;
import nez.ast.Tag;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class NezParser extends NodeVisitor {

	Production product;
	Grammar loaded;
	GrammarChecker checker;
	
	public NezParser() {
		product = NezParserCombinator.newGrammar().getProduction("Chunk", Production.SafeOption);
	}
	
	public Grammar load(SourceContext sc, GrammarChecker checker) {
		this.loaded = new Grammar(sc.getResourceName());
		this.checker = checker;
		while(sc.hasUnconsumed()) {
			AST ast = product.parse(sc, new AST());
			if(ast == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			if(!this.parse(ast)) {
				break;
			}
		}
		checker.verify(loaded);
		return loaded;
	}

	public Rule parseRule(Grammar peg, String res, int linenum, String text) {
		SourceContext sc = SourceContext.newStringSourceContext(res, linenum, text);
		this.loaded = peg;
		this.checker = null;
		AST ast = product.parse(sc, new AST());
		if(ast == null || !ast.is(NezTag.Rule)) {
			return null;
		}
		return toRule(ast);
	}
	
	private boolean parse(AST ast) {
		//System.out.println("DEBUG? parsed: " + ast);
		if(ast.is(NezTag.Rule)) {
			toRule(ast);
//			if(ast.size() > 3) {
//				System.out.println("DEBUG? parsed: " + ast);		
//			}
//			String ruleName = ast.textAt(0, "");
//			if(ast.get(0).is(NezTag.String)) {
//				ruleName = quote(ruleName);
//			}
//			Rule rule = loaded.getRule(ruleName);
//			Expression e = toExpression(ast.get(1));
//			if(rule != null) {
//				checker.reportWarning(ast, "duplicated rule name: " + ruleName);
//				rule = null;
//			}
//			rule = loaded.defineRule(ast.get(0), ruleName, e);
//			if(ast.size() >= 3) {
//				readAnnotations(rule, ast.get(2));
//			}
			return true;
		}
//		if(ast.is(NezTag.Import)) {
//			UList<AST> l = new UList<AST>(new AST[ast.size()-1]);
//			for(int i = 0; i < ast.size()-1;i++) {
//				l.add(ast.get(i));
//			}
//			String filePath = searchPegFilePath(ast.getSource(), ast.textAt(ast.size()-1, ""));
//			peg.imastrtGrammar(l, filePath);
//			return true;
//		}
//		if(ast.is(Tag.CommonError)) {
//			int c = ast.getSource().byteAt(ast.getSourcePosition());
//			System.out.println(ast.formatSourceMessage("error", "syntax error: ascii=" + c));
//			return false;
//		}
		ConsoleUtils.exit(1, ast.formatSourceMessage("error", "Not Nez construct: " + ast));
		return false;
	}
	
//	private String searchPegFilePath(Source s, String filePath) {
//		String f = s.getFilePath(filePath);
//		if(new File(f).exists()) {
//			return f;
//		}
//		if(new File(filePath).exists()) {
//			return filePath;
//		}
//		return "lib/"+filePath;
//	}

	Expression toExpression(AST po) {
		return (Expression)this.visit(po);
	}
	
	public Rule toRule(AST ast) {
		String ruleName = ast.textAt(0, "");
		if(ast.get(0).is(NezTag.String)) {
			ruleName = quote(ruleName);
		}
		Rule rule = loaded.getRule(ruleName);
		Expression e = toExpression(ast.get(1));
		if(rule != null) {
			checker.reportWarning(ast, "duplicated rule name: " + ruleName);
			rule = null;
		}
		rule = loaded.defineRule(ast.get(0), ruleName, e);
		if(ast.size() >= 3) {
			readAnnotations(rule, ast.get(2));
		}
		return rule;
	}
	
	private void readAnnotations(Rule rule, AST pego) {
		for(int i = 0; i < pego.size(); i++) {
			AST p = pego.get(i);
			if(p.is(NezTag.Annotation)) {
				rule.addAnotation(p.textAt(0, ""), p.get(1));
			}
		}
	}
	
	public Expression toNonTerminal(AST ast) {
		String symbol = ast.getText();
//		if(ruleName.equals(symbol)) {
//			Expression e = peg.getExpression(ruleName);
//			if(e != null) {
//				// self-redefinition
//				return e;  // FIXME
//			}
//		}
//		if(symbol.length() > 0 && !symbol.endsWith("_") && !peg.hasRule(symbol)
//				&& GrammarFactory.Grammar.hasRule(symbol)) { // comment
//			Main.printVerbose("implicit importing", symbol);
//			peg.setRule(symbol, GrammarFactory.Grammar.getRule(symbol));
//		}
		return Factory.newNonTerminal(ast, this.loaded, symbol);
	}

	private String quote(String t) {
		return "\"" + t + "\"";
	}

	public Expression toString(AST ast) {
		String name = quote(ast.getText());
		Rule r = this.loaded.getRule(name);
		if(r != null) {
			return r.getExpression();
		}
		else {
			this.checker.reportNotice(ast, "undefined terminal: " + name);
		}
		return Factory.newString(ast, StringUtils.unquoteString(ast.getText()));
	}

	public Expression toCharacter(AST ast) {
		return Factory.newString(ast, StringUtils.unquoteString(ast.getText()));
	}

	public Expression toClass(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		if(ast.size() > 0) {
			for(int i = 0; i < ast.size(); i++) {
				AST o = ast.get(i);
				if(o.is(NezTag.List)) {  // range
					l.add(Factory.newCharSet(ast, o.textAt(0, ""), o.textAt(1, "")));
				}
				if(o.is(NezTag.Class)) {  // single
					l.add(Factory.newCharSet(ast, o.getText(), o.getText()));
				}
			}
		}
		return Factory.newChoice(ast, l);
	}

	public Expression toByte(AST ast) {
		String t = ast.getText();
		if(t.startsWith("U+")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			if(c < 128) {
				return Factory.newByteChar(ast, c);					
			}
			String t2 = java.lang.String.valueOf((char)c);
			return Factory.newString(ast, t2);
		}
		int c = StringUtils.hex(t.charAt(t.length()-2)) * 16 + StringUtils.hex(t.charAt(t.length()-1)); 
		return Factory.newByteChar(ast, c);
	}

	public Expression toAny(AST ast) {
		return Factory.newAnyChar(ast);
	}

	public Expression toChoice(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[ast.size()]);
		for(int i = 0; i < ast.size(); i++) {
			Factory.addChoice(l, toExpression(ast.get(i)));
		}
		return Factory.newChoice(ast, l);
	}

	public Expression toSequence(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[ast.size()]);
		for(int i = 0; i < ast.size(); i++) {
			Factory.addSequence(l, toExpression(ast.get(i)));
		}
		return Factory.newSequence(ast, l);
	}

	public Expression toNot(AST ast) {
		return Factory.newNot(ast, toExpression(ast.get(0)));
	}

	public Expression toAnd(AST ast) {
		return Factory.newAnd(ast, toExpression(ast.get(0)));
	}

	public Expression toOption(AST ast) {
		return Factory.newOption(ast, toExpression(ast.get(0)));
	}

	public Expression toRepetition1(AST ast) {
		if(Expression.ClassicMode) {
			UList<Expression> l = new UList<Expression>(new Expression[2]);
			l.add(toExpression(ast.get(0)));
			l.add(Factory.newRepetition(ast, toExpression(ast.get(0))));
			return Factory.newSequence(ast, l);
		}
		else {
			return Factory.newRepetition1(ast, toExpression(ast.get(0)));
		}
	}

	public Expression toRepetition(AST ast) {
		if(ast.size() == 2) {
			int ntimes = StringUtils.parseInt(ast.textAt(1, ""), -1);
			if(ntimes != 1) {
				UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
				for(int i = 0; i < ntimes; i++) {
					Factory.addSequence(l, toExpression(ast.get(0)));
				}
				return Factory.newSequence(ast, l);
			}
		}
		return Factory.newRepetition(ast, toExpression(ast.get(0)));
	}

	// PEG4d TransCapturing

	public Expression toNew(AST ast) {
		if(Expression.ClassicMode) {
			Expression seq = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
			return Factory.newNew(ast, seq.toList());
		}
		else {
			Expression p = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
			return Factory.newNew(ast, false, p);
		}
	}

	public Expression toLeftNew(AST ast) {
		if(Expression.ClassicMode) {
			Expression seq = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
			return Factory.newNew(ast, seq.toList());
		}
		else {
			Expression p = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
			return Factory.newNew(ast, true, p);
		}
	}

	public Expression toLink(AST ast) {
		int index = -1;
		if(ast.size() == 2) {
			index = StringUtils.parseInt(ast.textAt(1, ""), -1);
		}
		return Factory.newLink(ast, toExpression(ast.get(0)), index);
	}

	public Expression toTagging(AST ast) {
		return Factory.newTagging(ast, Tag.tag(ast.getText()));
	}

	public Expression toReplace(AST ast) {
		return Factory.newReplace(ast, ast.getText());
	}

	//PEG4d Function
	
//	public Expression toDebug(AST ast) {
//		return Factory.newDebug(toExpression(ast.get(0)));
//	}

	public Expression toMatch(AST ast) {
		return Factory.newMatch(ast, toExpression(ast.get(0)));
	}

//	public Expression toCatch(AST ast) {
//		return Factory.newCatch();
//	}
//
//	public Expression toFail(AST ast) {
//		return Factory.newFail(Utils.unquoteString(ast.textAt(0, "")));
//	}

	public Expression toIf(AST ast) {
		return Factory.newIfFlag(ast, ast.textAt(0, ""));
	}

	public Expression toWith(AST ast) {
		return Factory.newWithFlag(ast, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toWithout(AST ast) {
		return Factory.newWithoutFlag(ast, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toBlock(AST ast) {
		return Factory.newBlock(ast, toExpression(ast.get(0)));
	}

	public Expression toDef(AST ast) {
		return Factory.newDefSymbol(ast, Tag.tag(ast.textAt(0, "")), toExpression(ast.get(1)));
	}

	public Expression toIs(AST ast) {
		return Factory.newIsSymbol(ast, Tag.tag(ast.textAt(0, "")));
	}

	public Expression toIsa(AST ast) {
		return Factory.newIsaSymbol(ast, Tag.tag(ast.textAt(0, "")));
	}

	public Expression toDefIndent(AST ast) {
		return Factory.newDefIndent(ast);
	}

	public Expression toIndent(AST ast) {
		return Factory.newIndent(ast);
	}

//	public Expression toScan(AST ast) {
//		return Factory.newScan(Integer.parseInt(ast.get(0).getText()), toExpression(ast.get(1)), toExpression(ast.get(2)));
//	}
//	
//	public Expression toRepeat(AST ast) {
//		return Factory.newRepeat(toExpression(ast.get(0)));
//	}
	
}
