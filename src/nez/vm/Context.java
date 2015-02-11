package nez.vm;

import nez.Recorder;
import nez.SourceContext;
import nez.ast.Node;
import nez.ast.Source;
import nez.ast.Tag;
import nez.expr.NonTerminal;
import nez.util.UList;
import nez.vm.MemoTable.MemoEntry;

public abstract class Context implements Source {
	
	/* parsing position */
	public   long   pos;
	private  long   head_pos;
	Recorder stat   = null;
	
	public final long getPosition() {
		return this.pos;
	}
	
	final void setPosition(long pos) {
		this.pos = pos;
	}
	
	public boolean hasUnconsumed() {
		return this.pos != length();
	}
	
	public final boolean consume(int length) {
		this.pos += length;
		if(head_pos < pos) {
			this.head_pos = pos;
//			if(this.stackedNonTerminals != null) {
//				this.maximumFailureTrace = new StackTrace();
//			}
		}
		return true;
	}

	public final void rollback(long pos) {
		if(stat != null && this.pos > pos) {
			stat.statBacktrack(pos, this.pos);
		}
		this.pos = pos;
	}

	public boolean failure2(nez.expr.Expression e) {
//		if(this.pos > fpos) {  // adding error location
//			this.fpos = this.pos;
//		}
		this.left = null;
		return false;
	}

	/* PEG4d : AST construction */

	Node base;
	public Node left;

	public final void setBaseNode(Node base) {
		this.base = base;
	}

	public final Node newNode() {
		return this.base.newNode(this, this.pos);
	}
	
	public final Node getParsedNode() {
		return this.left;
	}

	private class NodeLog {
		NodeLog next;
		Node parentNode;
		int  index;
		Node childNode;
	}

	private NodeLog logStack = null;
	private NodeLog unusedLog = null;
	private int     logStackSize = 0;
	
	private NodeLog newNodeLog() {
		if(this.unusedLog == null) {
			return new NodeLog();
		}
		NodeLog l = this.unusedLog;
		this.unusedLog = l.next;
		l.next = null;
		return l;
	}

	private void unusedNodeLog(NodeLog log) {
		log.childNode = null;
		log.next = this.unusedLog;
		this.unusedLog = log;
	}

	public final void lazyLink(Node parent, int index, Node child) {
		NodeLog l = this.newNodeLog();
		l.parentNode = parent;
		l.childNode  = child;
		l.index = index;
		l.next = this.logStack;
		this.logStack = l;
		this.logStackSize += 1;
	}
	
	public final void lazyJoin(Node left) {
		NodeLog l = this.newNodeLog();
		l.childNode  = left;
		l.index = -9;
		l.next = this.logStack;
		this.logStack = l;
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

	public final void commitConstruction(int checkPoint, Node newnode) {
		NodeLog first = null;
		int objectSize = 0;
		//System.out.println("commit: " + checkPoint + " < " + this.logStackSize);
		while(checkPoint < this.logStackSize) {
			NodeLog cur = this.logStack;
			this.logStack = this.logStack.next;
			this.logStackSize--;
			if(cur.index == -9) { // lazyCommit
				commitConstruction(checkPoint, cur.childNode);
				unusedNodeLog(cur);
				break;
			}
			if(cur.parentNode == newnode) {
				cur.next = first;
				first = cur;
				objectSize += 1;
			}
			else {
				unusedNodeLog(cur);
			}
		}
		if(objectSize > 0) {
			newnode.expandAstToSize(objectSize);
			for(int i = 0; i < objectSize; i++) {
				NodeLog cur = first;
				first = first.next;
				if(cur.index == -1) {
					cur.index = i;
				}
				newnode.commitChild(cur.index, cur.childNode);
				this.unusedNodeLog(cur);
			}
			//checkNullEntry(newnode);
		}
	}
	
	public final void abortConstruction(int checkPoint) {
		while(checkPoint < this.logStackSize) {
			NodeLog l = this.logStack;
			this.logStack = this.logStack.next;
			this.logStackSize--;
			unusedNodeLog(l);
		}
		//assert(checkPoint == this.logStackSize);
	}

	/* context-sensitivity parsing */
	/* <block e> <indent> */
	/* <def T e>, <is T>, <isa T> */
	
	public int stateValue = 0;
	private int stateCount = 0;
	private UList<SymbolTableEntry> stackedSymbolTable = new UList<SymbolTableEntry>(new SymbolTableEntry[4]);

	private class SymbolTableEntry {
		Tag table;  // T in <def T e>
		byte[] utf8;
		SymbolTableEntry(Tag table, String indent) {
			this.table = table;
			this.utf8 = indent.getBytes();
		}
	}
	
	public final int pushSymbolTable(Tag table, String s) {
		int stackTop = this.stackedSymbolTable.size();
		this.stackedSymbolTable.add(new SymbolTableEntry(table, s));
		this.stateCount += 1;
		this.stateValue = stateCount;
		return stackTop;
	}

	public final void popSymbolTable(int stackTop) {
		this.stackedSymbolTable.clear(stackTop);
	}

	public final boolean matchSymbolTableTop(Tag table) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == table) {
				if(this.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
				break;
			}
		}
		return this.failure2(null);
	}

	public final boolean matchSymbolTable(Tag table) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == table) {
				if(this.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
			}
		}
		return this.failure2(null);
	}
	
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
		
	public final boolean matchNonTerminal(NonTerminal e) {
//		if(this.stackedNonTerminals != null) {
//			int pos = this.stackedNonTerminals.size();
//			this.stackedNonTerminals.add(e);
//			stackedPositions[pos] = (int)this.pos;
//			boolean b = e.deReference().matcher.match(this);
//			this.stackedNonTerminals.clear(pos);
//			return b;
//		}
		return e.deReference().matcher.match((SourceContext)this); // FIXME
	}
	
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
	

	// ----------------------------------------------------------------------
	
	// Instruction 
	
	class ContextStack {
		Instruction jump;
		long pos;
		Node node;
		int  prevFailTop;
		int  prevLocalTop;
		int  nodeCheckPoint;
	}
	private ContextStack[] jumpStacks = null;
	private int unusedStackTop;
	private int failStackTop;
	private int localStackTop;
	
	public final void initJumpStack(int n) {
		this.jumpStacks = new ContextStack[n];
		for(int i = 0; i < n; i++) {
			this.jumpStacks[i] = new ContextStack();
		}
		this.jumpStacks[0].jump = new Exit(false);
		this.jumpStacks[1].jump = new Exit(true);  // for a point of the first called nonterminal
		this.failStackTop = 0;
		this.localStackTop = 1;
		this.unusedStackTop = 2;
	}

	private ContextStack newUnusedStack() {
		if(jumpStacks.length == unusedStackTop) {
			ContextStack[] newstack = new ContextStack[jumpStacks.length*2];
			System.arraycopy(jumpStacks, 0, newstack, 0, jumpStacks.length);
			for(int i = this.jumpStacks.length; i < newstack.length; i++) {
				newstack[i] = new ContextStack();
			}
			jumpStacks = newstack;
		}
		unusedStackTop++;
		return jumpStacks[unusedStackTop - 1];
	}

	public final Instruction opFailPush(FailPush op) {
		ContextStack top = newUnusedStack();
		top.prevFailTop   = failStackTop;
		top.prevLocalTop = localStackTop;
		top.jump = op.jump;
		top.pos = pos;
		top.node = this.left;
		top.nodeCheckPoint = this.startConstruction();
		failStackTop = unusedStackTop - 1;
		return op.next;
	}

	public final Instruction opFail() {
		ContextStack top = jumpStacks[failStackTop];
		this.unusedStackTop = failStackTop;
		this.failStackTop = top.prevFailTop;
		this.localStackTop = top.prevLocalTop;
		this.pos = top.pos;
		this.left = top.node;
		this.abortConstruction(top.nodeCheckPoint);
		return top.jump;
	}

	public final Instruction opFailPop(FailPop op) {
		ContextStack top = jumpStacks[failStackTop];
		this.unusedStackTop = failStackTop;
		this.failStackTop = top.prevFailTop;
		this.localStackTop = top.prevLocalTop;
		return op.next;
	}

	private final ContextStack newUnusedLocalStack() {
		ContextStack top = newUnusedStack();
		top.prevLocalTop = localStackTop;
		localStackTop = unusedStackTop - 1;
		return top;
	}

	private final ContextStack popLocalStack() {
		ContextStack top = jumpStacks[localStackTop];
		unusedStackTop = localStackTop;
		localStackTop = top.prevLocalTop;
		return top;
	}

	public final Instruction opCallPush(CallPush op) {
		ContextStack top = newUnusedLocalStack();
		top.jump = op.jump;
		return op.next;
	}
	
	public final Instruction opReturn() {
		return popLocalStack().jump;
	}

	public final Instruction opNodePush(NodePush op) {
		ContextStack top = newUnusedLocalStack();
		top.node = this.left;
		top.pos = this.startConstruction();
		//this.left = null;
		return op.next;
	}
	
	public final Instruction opNodeStore(NodeStore op) {
		ContextStack top = popLocalStack();
		Node parent = top.node;
		this.commitConstruction((int)top.pos, this.left);
		this.left = this.left.commit();
		lazyLink(parent, op.index, this.left);
		this.left = parent;
		return op.next;
	}
	
	public final Instruction opPosPush(PosPush op) {
		ContextStack top = newUnusedLocalStack();
		top.pos = pos;
		return op.next;
	}

	public final Instruction opPopBack(PosBack op) {
		ContextStack top = popLocalStack();
		this.pos = top.pos;
		return op.next;
	}

	public final Instruction opMatchAny(MatchAny op) {
		if(this.hasUnconsumed()) {
			this.consume(1);
			return op.next;
		}
		return this.opFail();
	}

	public final Instruction opMatchByte(MatchByte op) {
		if(this.byteAt(this.pos) == op.byteChar) {
			this.consume(1);
			return op.next;
		}
		return this.opFail();
	}

	public final Instruction opMatchByteMap(MatchByteMap op) {
		int byteChar = this.byteAt(this.pos);
		if(op.byteMap[byteChar]) {
			this.consume(1);
			return op.next;
		}
		return this.opFail();
	}

	public final Instruction opNew(NodeNew op) {
		this.left = this.base.newNode(this, this.pos);
		return op.next;
	}

	public final Instruction opNodeLeftLink(NodeLeftLink op) {
		Node left = this.left;
		Node newnode = this.base.newNode(this, this.pos);
		this.lazyJoin(left);
		this.lazyLink(newnode, 0, left);
		this.left = newnode;
		return op.next;
	}

	public final Instruction opNodeTag(NodeTag op) {
		this.left.setTag(op.tag);
		return op.next;
	}

	public final Instruction opReplace(NodeReplace op) {
		this.left.setValue(op.value);
		return op.next;
	}

	public final Instruction opCapture(NodeCapture op) {
		this.left.setEndingPosition(this.pos);
		return op.next;
	}

	// Memoization
	MemoTable memoTable;
	
	public final Instruction opLookup(Lookup op) {
		MemoEntry m = memoTable.getMemo2(pos, op.memoPoint, stateValue);
		if(m != null) {
			//this.memo.hit(m.consumed);
			if(m.consumed == -1) {
				return opFail();
			}
			pos += m.consumed;
			if(m.result != null/*MemoizationManager.NonTransition*/) {
				this.left = m.result;
			}
			return op.jump;
		}
		return op.next;
	}

	public final Instruction opMemoize(Memoize op) {
		// opFailTop
		ContextStack top = jumpStacks[failStackTop];
		this.unusedStackTop = failStackTop;
		this.failStackTop = top.prevFailTop;
		this.localStackTop = top.prevLocalTop;
		// End of opFailTop

		int length = (int)(pos - top.pos);
		memoTable.setMemo2(pos, op.memoPoint, stateValue, (this.left == top.node) ? null : this.left, length);
//		if(Tracing && memo.checkUseless()) {
//		enableMemo = false;
//		disabledMemo();
//	}
		return op.next;
	}
	
	public Instruction opMemoizeFail(MemoizeFail op) {
		memoTable.setMemo2(pos, op.memoPoint, stateValue, null, -1);
		return opFail();
	}

	// symbol table

	public Instruction opDefString(DefString op) {
		ContextStack top = popLocalStack();
		this.pushSymbolTable(op.tableName, this.substring(top.pos, this.pos));
		return op.next;
	}
		

}
