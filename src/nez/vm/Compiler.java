package nez.vm;

import java.util.HashMap;

import nez.Production;
import nez.expr.And;
import nez.expr.AnyChar;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.Choice;
import nez.expr.ContextSensitive;
import nez.expr.DefIndent;
import nez.expr.DefSymbol;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.expr.IsIndent;
import nez.expr.IsSymbol;
import nez.expr.IsaSymbol;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NewLeftLink;
import nez.expr.NonTerminal;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.main.Verbose;
import nez.util.FlagUtils;
import nez.util.UList;
import nez.util.UMap;

public class Compiler {
	final int option;
	
	class CodeBlock {
		Instruction head;
		int start;
		int end;
	}

	UList<Instruction> codeList;
	UMap<CodeBlock> ruleMap;
	HashMap<Integer, MemoPoint> memoMap;
	
	public Compiler(int CompilerOption) {
		this.option = CompilerOption;
		this.codeList = new UList<Instruction>(new Instruction[64]);
		this.ruleMap = new UMap<CodeBlock>();
		if(this.enablePackratParsing()) {
			this.memoMap = new HashMap<Integer, MemoPoint>();
			this.visitedMap = new UMap<String>();
		}
	}
	
	private final boolean enablePackratParsing() {
		return FlagUtils.is(this.option, Production.PackratParsing);
	}

	private final boolean enableASTConstruction() {
		return FlagUtils.is(this.option, Production.ASTConstruction);
	}

	MemoPoint issueMemoPoint(Expression e) {
		if(this.enablePackratParsing()) {
			Integer key = e.internId;
			assert(e.internId != 0);
			MemoPoint m = this.memoMap.get(key);
			if(m == null) {
				m = new MemoPoint(this.memoMap.size(), e, this.isContextSensitive(e));
				this.visitedMap.clear();
				this.memoMap.put(key, m);
			}
			return m;
		}
		return null;
	}
	
	public final int getInstructionSize() {
		return this.codeList.size();
	}
	
	public final int getMemoPointSize() {
		if(this.enablePackratParsing()) {
			return this.memoMap.size();
		}
		return 0;
	}
	
	private UMap<String> visitedMap = null;

	private boolean isContextSensitive(Expression e) {
		if(e instanceof NonTerminal) {
			String un = ((NonTerminal) e).getUniqueName();
			if(visitedMap.get(un) == null) {
				visitedMap.put(un, un);
				return isContextSensitive(((NonTerminal) e).getRule().getExpression());
			}
			return false;
		}
		for(int i = 0; i < e.size(); i++) {
			if(isContextSensitive(e.get(i))) {
				return true;
			}
		}
		return (e instanceof ContextSensitive);
	}
	
	public final Instruction encode(UList<Rule> ruleList) {
		for(Rule r : ruleList) {
			String uname = r.getUniqueName();
			CodeBlock block = new CodeBlock();
			block.head = r.getExpression().encode(this, new Return(this, r));
			block.start = codeList.size();
			this.ruleMap.put(uname, block);
			encode(block.head);
			block.end = codeList.size();
		}
		for(Instruction inst : codeList) {
			if(inst instanceof CallPush) {
				CodeBlock deref = this.ruleMap.get(((CallPush) inst).rule.getUniqueName());
				((CallPush) inst).setResolvedJump(deref.head);
			}
		}
		return this.codeList.ArrayValues[0];
	}

	void encode(Instruction inst) {
		if(inst != null) {
			if(inst.id == -1) {
				inst.id = this.codeList.size();
				this.codeList.add(inst);
				encode(inst.next);
				if(inst.next != null && inst.id + 1 != inst.next.id) {
					Instruction.labeling(inst.next);
				}
				encode(inst.branch());
				//encode(inst.branch2());
			}
		}
	}
	
	public void dump(UList<Rule> ruleList) {
		for(Rule r : ruleList) {
			String uname = r.getUniqueName();
			System.out.println(uname + ":");
			CodeBlock block = this.ruleMap.get(uname);
			for(int i = block.start; i < block.end; i++) {
				Instruction inst = codeList.ArrayValues[i];
				if(inst.label) {
					System.out.println("" + inst.id + "*\t" + inst);
				}
				else {
					System.out.println("" + inst.id + "\t" + inst);
				}
				if(inst.next != null && inst.next.id != i+1) {
					System.out.println("\tjump " + Instruction.label(inst.next));
				}
			}
		}
	}
	
	// encoding 

	public final Instruction encodeMatchAny(AnyChar p, Instruction next) {
		return new MatchAny(p, next);
	}

	public final Instruction encodeByteChar(ByteChar p, Instruction next) {
		return new IByteChar(p, false, next);
	}

	public final Instruction encodeByteMap(ByteMap p, Instruction next) {
		return new IByteMap(p, false, next);
	}

	public Instruction encodeFail(Expression p) {
		return new Fail(p);
	}

	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		if(FlagUtils.is(option, Production.Specialization)) {
			Expression inner = p.get(0).optimize(option);
			if(inner instanceof ByteChar) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new RByteMap((ByteChar)inner, next);
			}
			if(inner instanceof ByteMap) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new RByteMap((ByteMap)inner, next);
			}
		}
		FailSkip skip = new FailSkip(this, p);
		Instruction start = p.get(0).encode(this, skip);
		skip.next = start;
		return new FailPush(p, next, start);
	}

	public final Instruction encodeOption(Option p, Instruction next) {
		if(FlagUtils.is(option, Production.Specialization)) {
			Expression inner = p.get(0).optimize(option);
			if(inner instanceof ByteChar) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new IByteChar((ByteChar)inner, true, next);
			}
			if(inner instanceof ByteMap) {
				Verbose.noticeOptimize("Specialization", p, inner);
				return new IByteMap((ByteMap)inner, true, next);
			}
		}
		Instruction pop = new FailPop(this, p, next);
		return new FailPush(p, next, p.get(0).encode(this, pop));
	}

	public final Instruction encodeAnd(And p, Instruction next) {
		Instruction inner = p.get(0).encode(this, new PosBack(p, next));
		return new PosPush(p, inner);
	}

	public final Instruction encodeNot(Not p, Instruction next) {
		if(FlagUtils.is(option, Production.Specialization)) {
			Expression inn = p.get(0).optimize(option);
			if(inn instanceof ByteMap) {
				Verbose.noticeOptimize("Specilization", p);
				return new NByteMap((ByteMap)inn, next);
			}
			if(inn instanceof ByteChar) {
				Verbose.noticeOptimize("Specilization", p);
				return new NByteMap((ByteChar)inn, next);
			}
			if(inn instanceof Sequence && ((Sequence)inn).isMultiChar()) {
				Verbose.noticeOptimize("Specilization", p);
				return new NMultiChar((Sequence)inn, next);
			}
		}
		Instruction fail = new FailPop(this, p, new Fail(p));
		return new FailPush(p, next, p.get(0).encode(this, fail));
	}

	public final Instruction encodeSequence(Expression p, Instruction next) {
		Expression pp = p.optimize(option);
		if(pp != p) {
			if(pp instanceof ByteMap) {
				Verbose.noticeOptimize("ByteMap", p, pp);
				return encodeByteMap((ByteMap)pp, next);
			}
		}
		Instruction nextStart = next;
		for(int i = p.size() -1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = e.encode(this, nextStart);
		}
		if(pp != p) {
			if(pp instanceof Choice) {
				Verbose.noticeOptimize("Prediction", p, pp);
				next = pp.get(0).encode(this, new FailPop(this, p, next));
				return new FailPush(p, nextStart, next);
			}
		}
		return nextStart;
	}

	public final Instruction encodeChoice(Choice p, Instruction next) {
		Expression pp = p.optimize(option);
		if(pp instanceof ByteMap) {
			Verbose.noticeOptimize("ByteMap", p, pp);
			return encodeByteMap((ByteMap)pp, next);
		}
		return this.encodeUnoptimizedChoice(p, next);
	}

	public final Instruction encodeUnoptimizedChoice(Choice p, Instruction next) {
		Instruction nextChoice = p.get(p.size()-1).encode(this, next);
		for(int i = p.size() -2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new FailPush(e, nextChoice, e.encode(this, new FailPop(this, e, next)));
		}
		return nextChoice;
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next) {
		Expression pp = p.optimize(option);
		if(pp instanceof ByteChar || pp instanceof ByteMap || pp instanceof AnyChar) {
			Verbose.noticeOptimize("Inlining", p, pp);
			return pp.encode(this, next);
		}
		if(this.enablePackratParsing()) {
			Rule r = p.getRule();
			if(r.isPurePEG()) {
				Expression ref = Factory.resolveNonTerminal(r.getExpression());
				MemoPoint m = this.issueMemoPoint(ref);
				if(m != null) {
					Instruction inside = new CallPush(this, r, newMemoize(p, m, next));
					return newLookup(p, m, inside, next, newMemoizeFail(p, m));
				}
			}
		}	
		return new CallPush(this, p.getRule(), next);
	}
	
	private Instruction newLookup(Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		if(m.contextSensitive) {
			return new Lookup2(e, m, next, skip, failjump);
		}
		return new Lookup(e, m, next, skip, failjump);
	}

	private Instruction newMemoize(Expression e, MemoPoint m, Instruction next) {
		if(m.contextSensitive) {
			return new Memoize2(e, m, next);
		}
		return new Memoize(e, m, next);
	}

	private Instruction newMemoizeFail(Expression e, MemoPoint m) {
		if(m.contextSensitive) {
			return new MemoizeFail2(e, m);
		}
		return new MemoizeFail2(e, m);
	}

	
	// AST Construction
	
	public final Instruction encodeLink(Link p, Instruction next) {
		if(this.enableASTConstruction()) {
			if(this.enablePackratParsing()) {
				Expression inner = Factory.resolveNonTerminal(p.get(0));
				MemoPoint m = this.issueMemoPoint(inner);
				if(m != null) {
					Instruction inside = inner.encode(this, newMemoizeNode(p, m, next));
					return newLookupNode(p, m, inside, next, new MemoizeFail(p, m));
				}
			}
			return new NodePush(p, p.get(0).encode(this, new NodeStore(p, next)));
		}
		return p.get(0).encode(this, next);
	}

	private Instruction newLookupNode(Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		if(m.contextSensitive) {
			return new LookupNode2(e, m, next, skip, failjump);
		}
		return new LookupNode(e, m, next, skip, failjump);
	}

	private Instruction newMemoizeNode(Link e, MemoPoint m, Instruction next) {
		if(m.contextSensitive) {
			return new MemoizeNode2(e, m, next);
		}
		return new MemoizeNode(e, m, next);
	}

	
	public final Instruction encodeNew(New p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new NodeNew(p, this.encodeSequence(p, new NodeCapture(p, next)));
		}
		return this.encodeSequence(p, next);
	}

	public final Instruction encodeLeftNew(NewLeftLink p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new NodeLeftNew(p, this.encodeSequence(p, new NodeCapture(p, next)));
		}
		return this.encodeSequence(p, next);
	}
		
	public final Instruction encodeTagging(Tagging p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new NodeTag(p, next);
		}
		return next;
	}

	public final Instruction encodeReplace(Replace p, Instruction next) {
		if(this.enableASTConstruction()) {
			return new NodeReplace(p, next);
		}
		return next;
	}
	
	public final Instruction encodeDefSymbol(DefSymbol p, Instruction next) {
		Instruction inner = p.get(0).encode(this, new IDefSymbol(p, next));
		return new PosPush(p, inner);
	}
	public final Instruction encodeIsSymbol(IsSymbol p, Instruction next) {
		Instruction inner = p.get(0).encode(this, new IIsSymbol(p, false, next));
		return new PosPush(p, inner);
	}
	public final Instruction encodeIsaSymbol(IsaSymbol p, Instruction next) {
		Instruction inner = p.get(0).encode(this, new IIsSymbol(p, true, next));
		return new PosPush(p, inner);
	}
	public final Instruction encodeDefIndent(DefIndent p, Instruction next) {
		return new IDefIndent(p, next);
	}
	public final Instruction encodeIsIndent(IsIndent p, Instruction next) {
		return new IIsIndent(p, next);
	}

}
