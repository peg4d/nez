package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Compiler;
import nez.vm.Instruction;

public class Choice extends ExpressionList {
	Choice(SourcePosition s, UList<Expression> l) {
		super(s, l);
	}
	@Override
	public String getPredicate() {
		return "/";
	}
	@Override
	public String getInterningKey() {
		return "/";
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		boolean afterAll = true;
		for(Expression e: this) {
			if(!e.checkAlwaysConsumed(checker, startNonTerminal, stack)) {
				if(stack == null) {  // reconfirm 
					return false;
				}
				afterAll = false;
			}
		}
		return afterAll;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		if(this.size() > 0) {
			return this.get(0).inferNodeTransition(visited);
		}
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(GrammarChecker checker, NodeTransition c) {
		int required = c.required;
		UList<Expression> l = newList();
		for(Expression e : this) {
			c.required = required;
			Factory.addChoice(l, e.checkNodeTransition(checker, c));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public Expression removeNodeOperator() {
		UList<Expression> l = newList();
		for(Expression e : this) {
			Factory.addChoice(l, e.removeNodeOperator());
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			Factory.addChoice(l, e.removeFlag(undefedFlags));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public short acceptByte(int ch) {
		boolean hasUnconsumed = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r == Accept) {
				return r;
			}
			if(r == Unconsumed) {
				hasUnconsumed = true;
			}
		}
		return hasUnconsumed ? Unconsumed : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		//long f = context.rememberFailure();
		Node left = context.left;
		for(int i = 0; i < this.size(); i++) {
			context.left = left;
			if(this.get(i).matcher.match(context)) {
				//context.forgetFailure(f);
				left = null;
				return true;
			}
		}
		//assert(context.isFailure());
		left = null;
		return false;
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeChoice(this, next);
	}
	
	// optimize
	Expression[] matchCase = null;
	boolean selfChoice = false;
	int startIndex = -1;
	int endIndex = 257;
	final void optimize(int level) {
		if(level > 0 && !(this.matcher instanceof ByteMap)) {
			boolean byteMap[] = new boolean[257];
			if(isByteMap(level, byteMap)) {
				this.matcher = Factory.newByteMap(s, byteMap);
				return;
			}
		}
		if(level > 0 && this.matchCase != null) {
			Expression[] matchCase = new Expression[257];
			Expression fails = Factory.newFailure(s);
			for(int ch = 0; ch <= 256; ch++) {
				Expression sub = selectChoice(ch, fails);
				if(startIndex == -1 && sub != fails) {
					startIndex = ch;
				}
				if(startIndex != -1 && sub == fails) {
					endIndex = ch;
				}
				if(sub instanceof Choice) {
					if(sub == this) {
						/* this is a rare case where the selected choice is the parent choice */
						/* this cause the repeated calls of the same matchers */
						this.selfChoice = true;
					}
					else {
						((Choice)sub).optimize(level);
					}
				}
			}
			this.matchCase = matchCase;
		}
	}
	
	private final boolean isByteMap(int level, boolean[] byteMap) {
		for(Expression e : this) {
			e = Factory.resolveNonTerminal(e);
			if(e.matcher instanceof ByteChar) {
				byteMap[((ByteChar) e).byteChar] = true;
				continue;
			}
			if(e.matcher instanceof ByteMap) {
				boolean[] bitMap = ((ByteMap)e).charMap;
				for(int c1 = 0; c1 < byteMap.length; c1++) {
					if(bitMap[c1]) {
						byteMap[c1] = true;
					}
				}
				continue;
			}
			if(e instanceof Choice) {
				if(!((Choice)e).isByteMap(level, byteMap)) {
					return false;
				}
				continue;
			}
			return false;
		}
		return true;
	}

	private Expression selectChoice(int ch, Expression failed) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		selectChoice(ch, failed, l);
		if(l.size() == 0) {
			l.add(failed);
		}
		return Factory.newChoice(s, l);
	}

	private void selectChoice(int ch, Expression failed, UList<Expression> l) {
		for(Expression e : this) {
			e = Factory.resolveNonTerminal(e);
			if(e instanceof Choice) {
				((Choice)e).selectChoice(ch, failed, l);
			}
			else {
				short r = e.acceptByte(ch);
				//System.out.println("~ " + GrammarFormatter.stringfyByte(ch) + ": r=" + r + " in " + e);
				if(r != Expression.Reject) {
					l.add(e);
				}
			}
		}
	}
	
	@Override
	protected int pattern(GEP gep) {
		return this.size();
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.get(p % size()).examplfy(gep, sb, p);
	}



}
