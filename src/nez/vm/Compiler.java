package nez.vm;

import java.util.HashMap;

import nez.expr.Choice;
import nez.expr.ContextSensitive;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NewLeftLink;
import nez.expr.NonTerminal;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Tagging;
import nez.util.UList;
import nez.util.UMap;

public class Compiler {
	boolean disableASTConstruction = false;
	boolean disablePackratParsing  = false;

	UList<Instruction> codeList;
	UMap<CodeBlock> ruleMap;
	
	class CodeBlock {
		Instruction head;
		int start;
		int end;
	}
	
	public Compiler() {
		this.codeList = new UList<Instruction>(new Instruction[64]);
		this.ruleMap = new UMap<CodeBlock>();
	}
	
	HashMap<Integer, MemoPoint> memoMap = new HashMap<Integer, MemoPoint>();
    
	MemoPoint issueMemoPoint(Expression e) {
		Integer key = e.internId;
		assert(e.internId != 0);
		MemoPoint m = this.memoMap.get(key);
		if(m == null) {
			this.visitedMap.clear();
			m = new MemoPoint(this.memoMap.size(), e, this.isContextSensitive(e));
			this.memoMap.put(key, m);
		}
		return m;
	}
	
	private UMap<String> visitedMap = new UMap<String>();

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

	public final Instruction encodeRepetition(Repetition p, Instruction next) {
		FailSkip skip = new FailSkip(this, p);
		Instruction start = p.get(0).encode(this, skip);
		skip.next = start;
		return new FailPush(this, p, next, start);
	}

	public final Instruction encodeOption(Option p, Instruction next) {
		Instruction pop = new FailPop(this, p, next);
		return new FailPush(this, p, next, p.get(0).encode(this, pop));
	}

	public final Instruction encodeNot(Not p, Instruction next) {
		Instruction fail = new FailPop(this, p, new Fail(this, p));
		return new FailPush(this, p, next, p.get(0).encode(this, fail));
	}

	public final Instruction encodeChoice(Choice p, Instruction next) {
		Instruction nextChoice = p.get(p.size()-1).encode(this, next);
		for(int i = p.size() -2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new FailPush(this, e, nextChoice, e.encode(this, new FailPop(this, e, next)));
		}
		return nextChoice;
	}

	public final Instruction encodeSequence(Expression p, Instruction next) {
		for(int i = p.size() -1; i >= 0; i--) {
			Expression e = p.get(i);
			next = e.encode(this, next);
		}
		return next;
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next) {
		if(!this.disablePackratParsing) {
			Rule r = p.getRule();
			if(r.isPurePEG()) {
				Expression ref = Factory.resolveNonTerminal(r.getExpression());
				MemoPoint m = this.issueMemoPoint(ref);
				if(m != null) {
					Instruction inside = new CallPush(this, r, new Memoize(this, p, m, next));
					return newLookup(p, m, next, inside, new MemoizeFail(this, p, m));
				}
			}
		}	
		return new CallPush(this, p.getRule(), next);
	}
	
	private Lookup newLookup(Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		if(m.contextSensitive) {
			return new Lookup2(this, e, m, next, skip, failjump);
		}
		return new Lookup(this, e, m, next, skip, failjump);
	}
	

	
	
	// AST Construction
	
	public final Instruction encodeLink(Link p, Instruction next) {
		if(this.disableASTConstruction) {
			return p.get(0).encode(this, next);
		}
		else if(!this.disablePackratParsing) {
			Expression inner = Factory.resolveNonTerminal(p.get(0));
			MemoPoint m = this.issueMemoPoint(inner);
			if(m != null) {
				Instruction inside = inner.encode(this, new MemoizeNode(this, p, m, next));
				return new LookupNode(this, p, m, next, inside, new MemoizeFail(this, p, m));
			}
		}	
		return new NodePush(this, p, p.get(0).encode(this, new NodeStore(this, p, next)));
	}

	public final Instruction encodeNew(New p, Instruction next) {
		if(this.disableASTConstruction) {
			return this.encodeSequence(p, next);
		}
		else {
			return new NodeNew(this, p, this.encodeSequence(p, new NodeCapture(this, p, next)));
		}
	}

	public final Instruction encodeLeftNew(NewLeftLink p, Instruction next) {
		if(this.disableASTConstruction) {
			return this.encodeSequence(p, next);
		}
		else {
			return new NodeLeftLink(this, p, this.encodeSequence(p, new NodeCapture(this, p, next)));
		}
	}
		
	public final Instruction encodeTagging(Tagging p, Instruction next) {
		if(this.disableASTConstruction) {
			return next;
		}
		else {
			return new NodeTag(this, p, next);
		}
	}

	public final Instruction encodeReplace(Replace p, Instruction next) {
		if(this.disableASTConstruction) {
			return next;
		}
		else {
			return new NodeReplace(this, p, next);
		}
	}

	
}
