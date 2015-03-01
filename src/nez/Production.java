package nez;

import nez.ast.AST;
import nez.ast.Node;
import nez.expr.Expression;
import nez.expr.NezParserCombinator;
import nez.expr.NonTerminal;
import nez.expr.Rule;
import nez.main.Recorder;
import nez.main.Verbose;
import nez.util.FlagUtils;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;
import nez.vm.MemoTable;
import nez.x.PEG;

public class Production {
	Rule start;
	UMap<Rule>           ruleMap;
	UList<Rule>          ruleList;

	Production(Rule start, int option) {
		this.start = start;
		this.ruleList = new UList<Rule>(new Rule[4]);
		this.ruleMap = new UMap<Rule>();
		this.setOption(option);
		add(0, start);
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
	/* memoization configuration */
	
	private Instruction compiledCode = null;
	private int option;
	
	private void setOption (int option) {
		if(this.option != option) {
			this.compiledCode = null; // recompile
		}
		if(FlagUtils.is(option, PackratParsing) && this.defaultMemoTable == null) {
			this.defaultMemoTable = MemoTable.newElasticTable(0, 0, 0);
		}
		this.option = option;
	}

	public final void enable(int option) {
		setOption(this.option | option);
	}

	public final void disable(int option) {
		setOption(FlagUtils.unsetFlag(this.option, option));
	}

	private MemoTable defaultMemoTable;
	private int windowSize = 32;
	private int memoPointSize;
	private int InstructionSize;

	public void config(MemoTable memoTable) {
		this.defaultMemoTable = memoTable;
	}
	
	private MemoTable getMemoTable(SourceContext sc) {
		if(memoPointSize == 0) {
			return MemoTable.newNullTable(sc.length(), this.windowSize, this.memoPointSize);
		}
		return this.defaultMemoTable.newMemoTable(sc.length(), this.windowSize, this.memoPointSize);
	}

	public Instruction compile() {
		if(compiledCode == null) {
			Compiler bc = new Compiler(this.option);
			compiledCode = bc.encode(this.ruleList);
			this.InstructionSize  = bc.getInstructionSize();
			this.memoPointSize = bc.getMemoPointSize();
			if(Verbose.VirtualMachine) {
				bc.dump(this.ruleList);
			}
		}
		return compiledCode;
	}
	
	public final boolean match(SourceContext s) {
		if(FlagUtils.is(this.option, Production.ClassicMode)) {
			return this.start.match(s);
		}
		else {
			Instruction pc = this.compile();
			s.initJumpStack(64, getMemoTable(s));
			boolean matched = Instruction.run(pc, s);
			if(matched) {
				s.newTopLevelNode();
			}
			return matched;
		}
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
			node = base.newNode(null, sc, startPosition, sc.getPosition(), 0);
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

	public final void record(Recorder rec) {
		if(rec != null) {
			this.enable(Production.Profiling);
			this.compile();
			rec.setFile("G.File", this.start.getGrammar().getResourceName());
			rec.setCount("G.NonTerminals", this.ruleMap.size());
			rec.setCount("G.Instruction", this.InstructionSize);
			rec.setCount("G.MemoPoint", this.memoPointSize);
		}
	}

	/* --------------------------------------------------------------------- */
	/* Production Option */
	
	public final static int ClassicMode = 1;
	public final static int ASTConstruction = 1 << 1;
	public final static int PackratParsing  = 1 << 2;
	public final static int Optimization    = 1 << 3;
	public final static int Specialization  = 1 << 4;
	public final static int Prediction      = 1 << 5;
	public final static int New             = 1 << 6;
	public final static int Binary = 1 << 10;
	public final static int Profiling = 1 << 11;

	public final static int DefaultOption = ASTConstruction | PackratParsing | Optimization | Specialization | Prediction ;
	public final static int SafeOption = ASTConstruction | Optimization;
	
	public final static String stringfyOption(int option, String delim) {
		StringBuilder sb = new StringBuilder();
		if(FlagUtils.is(option, Production.ClassicMode)) {
			sb.append(delim);
			sb.append("classic");
		}
		if(FlagUtils.is(option, Production.ASTConstruction)) {
			sb.append(delim);
			sb.append("ast");
		}
		if(FlagUtils.is(option, Production.PackratParsing)) {
			sb.append(delim);
			sb.append("memo");
		}
		if(FlagUtils.is(option, Production.Optimization)) {
			sb.append(delim);
			sb.append("opt.");
		}
		if(FlagUtils.is(option, Production.Specialization)) {
			sb.append(delim);
			sb.append("spe.");
		}
		if(FlagUtils.is(option, Production.Prediction)) {
			sb.append(delim);
			sb.append("predict");
		}
		if(FlagUtils.is(option, Production.Profiling)) {
			sb.append(delim);
			sb.append("prof");
		}
		String s = sb.toString();
		if(s.length() > 0) {
			return s.substring(delim.length());
		}
		return s;
	}


}
