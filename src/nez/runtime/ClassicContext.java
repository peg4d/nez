package nez.runtime;

import nez.ast.SyntaxTree;

public abstract class ClassicContext extends Context {

	public boolean failure2(nez.expr.Expression e) {
//		if(this.pos > fpos) {  // adding error location
//			this.fpos = this.pos;
//		}
		//this.left = null;
		return false;
	}
	private class NodeLog {
		NodeLog next;
		SyntaxTree parentNode;
		int  index;
		SyntaxTree childNode;
	}

	private NodeLog nodeStack = null;
	private NodeLog unusedNodeLog = null;
	private int     logStackSize = 0;
	
	private NodeLog newNodeLog() {
		if(this.unusedNodeLog == null) {
			return new NodeLog();
		}
		NodeLog l = this.unusedNodeLog;
		this.unusedNodeLog = l.next;
		l.next = null;
		return l;
	}

	private void unusedNodeLog(NodeLog log) {
		log.childNode = null;
		log.next = this.unusedNodeLog;
		this.unusedNodeLog = log;
	}

	
	public final void lazyLink(SyntaxTree parent, int index, SyntaxTree child) {
		NodeLog l = this.newNodeLog();
		l.parentNode = parent;
		l.childNode  = child;
		l.index = index;
		l.next = this.nodeStack;
		this.nodeStack = l;
		this.logStackSize += 1;
	}
	
	public final void lazyJoin(SyntaxTree left) {
		NodeLog l = this.newNodeLog();
		l.childNode  = left;
		l.index = -9;
		l.next = this.nodeStack;
		this.nodeStack = l;
		this.logStackSize += 1;
	}

//	private final void checkNullEntry(Node o) {
//		for(int i = 0; i < o.size(); i++) {
//			if(o.get(i) == null) {
//				o.set(i, new Node(emptyTag, this.source, 0));
//			}
//		}
//	}

	public final int startConstruction() {
		return logStackSize;
	}

//	public final void commitConstruction(int checkPoint, SyntaxTree newnode) {
//		NodeLog first = null;
//		int objectSize = 0;
//		//System.out.println("commit: " + checkPoint + " < " + this.logStackSize);
//		while(checkPoint < this.logStackSize) {
//			NodeLog cur = this.nodeStack;
//			this.nodeStack = this.nodeStack.next;
//			this.logStackSize--;
//			if(cur.index == -9) { // lazyCommit
//				commitConstruction(checkPoint, cur.childNode);
//				unusedNodeLog(cur);
//				break;
//			}
//			if(cur.parentNode == newnode) {
//				cur.next = first;
//				first = cur;
//				objectSize += 1;
//			}
//			else {
//				unusedNodeLog(cur);
//			}
//		}
//		if(objectSize > 0) {
//			newnode.expandAstToSize(objectSize);
//			for(int i = 0; i < objectSize; i++) {
//				NodeLog cur = first;
//				first = first.next;
//				if(cur.index == -1) {
//					cur.index = i;
//				}
//				newnode.link(cur.index, cur.childNode);
//				this.unusedNodeLog(cur);
//			}
//			//checkNullEntry(newnode);
//		}
//	}
//	
//	public final void abortConstruction(int checkPoint) {
//		while(checkPoint < this.logStackSize) {
//			NodeLog l = this.nodeStack;
//			this.nodeStack = this.nodeStack.next;
//			this.logStackSize--;
//			unusedNodeLog(l);
//		}
//		//assert(checkPoint == this.logStackSize);
//	}

//	public final int pushSymbolTable(Tag table, String s) {
//		int stackTop = this.stackedSymbolTable.size();
//		this.stackedSymbolTable.add(new SymbolTableEntry(table, s));
//		this.stateCount += 1;
//		this.stateValue = stateCount;
//		return stackTop;
//	}
//
//	public final boolean matchSymbolTable(Tag table, boolean checkLastSymbolOnly) {
//		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
//			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
//			if(s.table == table) {
//				if(this.match(this.pos, s.utf8)) {
//					this.consume(s.utf8.length);
//					return true;
//				}
//				if(checkLastSymbolOnly) {
//					break;
//				}
//			}
//		}
//		return this.failure2(null);
//	}
	
//	UList<NonTerminal> stackedNonTerminals;
//	int[]         stackedPositions;
//	
//	class StackTrace {
//		StackTrace prev;
//		NonTerminal[] NonTerminals;
//		int[]    Positions;
//		StackTrace() {
//			this.NonTerminals = new NonTerminal[stackedNonTerminals.size()];
//			this.Positions = new int[stackedNonTerminals.size()];
//			System.arraycopy(stackedNonTerminals.ArrayValues, 0, NonTerminals , 0, NonTerminals.length);
//			System.arraycopy(stackedPositions, 0, Positions, 0, Positions.length);
//		}
//		@Override
//		public String toString() {
//			StringBuilder sb = new StringBuilder();
//			for(int n = 0; n < NonTerminals.length; n++) {
//				if(n > 0) {
//					sb.append("\n");
//				}
//				sb.append(formatPositionLine(this.NonTerminals[n].ruleName, this.Positions[n], "pos="+this.Positions[n]));
////				sb.append(this.NonTerminals[n]);
////				sb.append("#");
////				source.linenum()
////				sb.append()this.Positions[n]);
//			}
//			return sb.toString();
//		}
//	}
//
//	void initCallStack() {
////		if(Main.DebugLevel > 0) {
////			this.stackedNonTerminals = new UList<NonTerminal>(new NonTerminal[256]);
////			this.stackedPositions = new int[4096];
////		}
//	}
		
//	public final boolean matchNonTerminal(NonTerminal e) {
////		if(this.stackedNonTerminals != null) {
////			int pos = this.stackedNonTerminals.size();
////			this.stackedNonTerminals.add(e);
////			stackedPositions[pos] = (int)this.pos;
////			boolean b = e.deReference().matcher.match(this);
////			this.stackedNonTerminals.clear(pos);
////			return b;
////		}
//		return e.deReference().optimized.match((SourceContext)this); // FIXME
//	}
	
//	protected MemoTable memoTable = null;
//
//	public void initMemo(MemoizationManager conf) {
//		this.memoTable = (conf == null) ? new NoMemoTable(0, 0) : conf.newTable(this.source.length());
//	}
//
//	final MemoEntry getMemo(long keypos, int memoPoint) {
//		return this.memoTable.getMemo(keypos, memoPoint);
//	}
//
//	final void setMemo(long keypos, int memoPoint, Node result, int length) {
//		this.memoTable.setMemo(keypos, memoPoint, result, length);
//	}
//
//	final MemoEntry getMemo2(long keypos, int memoPoint, int stateValue) {
//		return this.memoTable.getMemo2(keypos, memoPoint, stateValue);
//	}
//
//	final void setMemo2(long keypos, int memoPoint, int stateValue, Node result, int length) {
//		this.memoTable.setMemo2(keypos, memoPoint, stateValue, result, length);
//	}
	
//	HashMap<String, Integer> repeatMap = new HashMap<String, Integer>();
//	
//	public final void setRepeatExpression(String rule, int value) {
//		this.repeatMap.put(rule, value);
//	}
//	
//	public final int getRepeatValue(String rule) {
//		return this.repeatMap.get(rule);
//	}
//	
//	public final String getRepeatByteString(long startIndex) {
//		return this.source.substring(startIndex, this.pos);
//	}

}
