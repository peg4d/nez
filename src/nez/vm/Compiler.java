package nez.vm;

import java.util.HashMap;

import nez.expr.Choice;
import nez.expr.Expression;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Rule;
import nez.util.UList;
import nez.util.UMap;

public class Compiler {
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
    
	MemoPoint getMemoPoint(Expression e) {
		Integer key = e.internId;
		assert(e.internId != 0);
		MemoPoint m = this.memoMap.get(key);
		if(m == null) {
			m = new MemoPoint(this.memoMap.size(), e);
			this.memoMap.put(key, m);
		}
		return m;
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
//			if(inst instanceof FailPush) {
//				encode(((FailPush)inst).jump);
//			}
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


	
}
