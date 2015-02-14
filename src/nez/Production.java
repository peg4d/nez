package nez;

import nez.ast.AST;
import nez.ast.Node;
import nez.expr.Expression;
import nez.expr.NezParserCombinator;
import nez.expr.NonTerminal;
import nez.expr.Rule;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.Compiler;
import nez.vm.PEG;

public class Production {
	Rule startingRule;
	UMap<Rule>           ruleMap;
	UList<Rule>          ruleList;

	Production(Rule rule) {
		this.startingRule = rule;
		this.ruleList = new UList<Rule>(new Rule[4]);
		this.ruleMap = new UMap<Rule>();
		add(0, rule);
		//dump();
	}

	private void add(int pos, Rule r) {
		if(!ruleMap.hasKey(r.getUniqueName())) {
			ruleList.add(r);
			ruleMap.put(r.getUniqueName(), r);
			add(pos, r.getExpression());
		}
	}
	
	private Expression rep = null;
	
	private void add(int pos, Expression expr) {
		if(expr instanceof NonTerminal) {
			//System.out.println("call " + ((NonTerminal) expr).getUniqueName() + " pos=" + pos + " redundant? " + checkRedundantCall(expr, pos));
			path.add(new Trace(expr, pos));
			add(pos, ((NonTerminal) expr).getRule());
		}
		if(rep == null && expr instanceof nez.expr.Repetition) {
			rep = expr;
			//System.out.println("top level repetition: " + expr);
			add(pos, expr.get(0));
			rep = null;
		}
		for(Expression se : expr) {
			add(pos, se);
			if(!(expr instanceof nez.expr.Choice)) {
				pos += count(se);
			}
		}
	}

	class Trace {
		Expression e;
		int pos;
		int count = 0;
		boolean redundant = false;
		Trace(Expression e, int pos) {
			this.e = e;
			this.pos = pos;
		}
		@Override
		public String toString() {
			return e + " pos=" + pos + " redundant? " + redundant;
		}
	}

	UList<Trace> path = new UList<Trace>(new Trace[128]);
	
	void dump() {
		for(Trace t : this.path) {
			System.out.println(t);
		}
	}

	boolean checkRedundantCall(Expression e, int pos) {
		boolean r = false;
		for(Trace t : this.path) {
			if(t.e == e && t.pos >= pos) {
				t.redundant = true;
				r = true;
			}
		}
		return r;
	}
	
//	boolean isRecursivelyVisited(NonTerminal e) {
//		for(int i = path.size() - 1; i >= 0; i--) {
//			if(path.ArrayValues[i].e == e) {
//				path.ArrayValues[i].count += 1;
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	void push(Expression e, int pos) {
//		path.add(new Trace(e, pos));
//	}
	
	int count(Expression e) {
		return (e.isAlwaysConsumed()) ? 1 : 0;
	}

	void checkBacktrack(Expression e, int pos) {
	}

	/* --------------------------------------------------------------------- */

	private Instruction compiledCode = null;
	public Instruction compile() {
		if(compiledCode == null) {
			Compiler optimizer = new Compiler();
			compiledCode = optimizer.encode(this.ruleList);
			optimizer.dump(this.ruleList);
		}
		return compiledCode;
	}
	
	public final boolean match(SourceContext s) {
		s.initJumpStack(64);
		return Instruction.run(this.compile(), s);
	}

	
	public final static void test() {
		nez.Grammar peg = NezParserCombinator.newGrammar();
		Production p = null;
		p = peg.getProduction("DIGIT");
		assert(p.match("1"));
//		p = peg.getProduction("INT");
//		assert(p.match("12"));
//		p = peg.getProduction("EOL");
//		assert(p.match("\r"));
//		assert(p.match("\n"));
//		assert(!p.match("1"));
//		p = peg.getProduction("EOT");
//		assert(p.match(""));
//		assert(!p.match("1"));
//		p = peg.getProduction("SEMI");
//		assert(p.match(";"));
//		assert(p.match(""));
//		p = peg.getProduction("NAME");
//		assert(p.match("AbcdAb1"));
//		assert(!p.match("123"));
//		p = peg.getProduction("Name");
//		AST ast = p.parseAST("Uzumaki Naruto");
//		System.out.println(ast);
		p = peg.getProduction("Expr");
		AST ast = p.parseAST("'a' 'b'");
		System.out.println(ast);

	}

	public final static void test1() {
		Grammar peg = PEG.newGrammar();
		Production p = null;
		AST node = null;
		p = peg.getProduction("DIGIT");
		assert(p.match("1"));
//		p = peg.getProduction("INT");
//		assert(p.match("12"));
//		p = peg.getProduction("EOL");
//		assert(p.match("\r"));
//		assert(p.match("\n"));
//		assert(!p.match("1"));
//		p = peg.getProduction("EOT");
//		assert(p.match(""));
//		assert(!p.match("1"));
//		p = peg.getProduction("SEMI");
//		assert(p.match(";"));
//		assert(p.match(""));
//		p = peg.getProduction("NAME");
//		assert(p.match("AbcdAb1"));
//		assert(!p.match("123"));
//		p = peg.getProduction("Name");
//		AST ast = p.parseAST("Uzumaki Naruto");
//		System.out.println(ast);
		p = peg.getProduction("Oab");
		node = p.parseAST("ab");
		System.out.println(node);

	}

	/* --------------------------------------------------------------------- */
	
	public final boolean match0(SourceContext s) {
		return this.startingRule.match(s);
	}
	
	public final boolean match(String str) {
		SourceContext sc = SourceContext.newStringSourceContext(str);
		if(match(sc)) {
			return (!sc.hasUnconsumed());
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> T parse(SourceContext sc, T base) {
		long startPosition = sc.getPosition();
		sc.setBaseNode(base);
		if(!this.match(sc)) {
			return null;
		}
		Node node = sc.getParsedNode();
		if(node == null) {
			node = base.newNode(sc, startPosition);
			node.setEndingPosition(sc.getPosition());
		}
		else {
			sc.commitConstruction(0, node);
		}
		node = node.commit();
		return (T)node;
	}

	public final AST parseAST(String str) {
		SourceContext sc = SourceContext.newStringSourceContext(str);
		return this.parse(sc, new AST());
	}

}
