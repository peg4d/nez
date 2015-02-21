package nez.vm;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.Source;
import nez.ast.Tag;
import nez.expr.NonTerminal;
import nez.main.Recorder;
import nez.util.UList;

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
//		if(stat != null && this.pos > pos) {
//			stat.statBacktrack(pos, this.pos);
//		}
		this.pos = pos;
	}

	public boolean failure2(nez.expr.Expression e) {
//		if(this.pos > fpos) {  // adding error location
//			this.fpos = this.pos;
//		}
		this.left = null;
		return false;
	}
	
	public final String getSyntaxErrorMessage() {
		return this.formatPositionLine("error", this.head_pos, "syntax error");
	}

	public final String getUnconsumedMessage() {
		return this.formatPositionLine("unconsumed", this.pos, "");
	}


	/* PEG4d : AST construction */

	Node base;
	public Node left;

	public final void setBaseNode(Node base) {
		this.base = base;
	}

	public final Node newNode() {
		return this.base.newNode(null, this, this.pos, this.pos, 0);
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

	
	public final void lazyLink(Node parent, int index, Node child) {
		NodeLog l = this.newNodeLog();
		l.parentNode = parent;
		l.childNode  = child;
		l.index = index;
		l.next = this.nodeStack;
		this.nodeStack = l;
		this.logStackSize += 1;
	}
	
	public final void lazyJoin(Node left) {
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

	public final void commitConstruction(int checkPoint, Node newnode) {
		NodeLog first = null;
		int objectSize = 0;
		//System.out.println("commit: " + checkPoint + " < " + this.logStackSize);
		while(checkPoint < this.logStackSize) {
			NodeLog cur = this.nodeStack;
			this.nodeStack = this.nodeStack.next;
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
				newnode.link(cur.index, cur.childNode);
				this.unusedNodeLog(cur);
			}
			//checkNullEntry(newnode);
		}
	}
	
	public final void abortConstruction(int checkPoint) {
		while(checkPoint < this.logStackSize) {
			NodeLog l = this.nodeStack;
			this.nodeStack = this.nodeStack.next;
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
		boolean debugFailStackFlag;
		Instruction jump;
		long pos;
		int  prevFailTop;
		//DataLog newPoint;
		DataLog lastLog;
	}
	
	private ContextStack[] contextStacks = null;
	private int usedStackTop;
	private int failStackTop;
	
	public final void initJumpStack(int n, MemoTable memoTable) {
		this.contextStacks = new ContextStack[n];
		this.lastAppendedLog = new DataLog();
		for(int i = 0; i < n; i++) {
			this.contextStacks[i] = new ContextStack();
		}
		this.contextStacks[0].jump = new Exit(false);
		this.contextStacks[0].debugFailStackFlag = true;
		this.contextStacks[0].lastLog = this.lastAppendedLog;
		this.contextStacks[1].jump = new Exit(true);  // for a point of the first called nonterminal
		this.failStackTop = 0;
		this.usedStackTop = 1;
		this.memoTable = memoTable;
	}

	private ContextStack newUnusedStack() {
		usedStackTop++;
		if(contextStacks.length == usedStackTop) {
			ContextStack[] newstack = new ContextStack[contextStacks.length*2];
			System.arraycopy(contextStacks, 0, newstack, 0, contextStacks.length);
			for(int i = this.contextStacks.length; i < newstack.length; i++) {
				newstack[i] = new ContextStack();
			}
			contextStacks = newstack;
		}
		return contextStacks[usedStackTop];
	}

	public final void dumpStack(String op) {
		System.out.println(op + " F="+this.failStackTop +", T=" +usedStackTop);
	}

	public final Instruction opFailPush(FailPush op) {
		ContextStack stackTop = newUnusedStack();
		stackTop.prevFailTop   = failStackTop;
		failStackTop = usedStackTop;
		stackTop.jump = op.failjump;
		stackTop.pos = pos;
		stackTop.lastLog = this.lastAppendedLog;
		assert(stackTop.lastLog != null);
		//stackTop.newPoint = this.newPoint;
		stackTop.debugFailStackFlag = true;
		return op.next;
	}

	public final Instruction opFailPop(Instruction op) {
		ContextStack stackTop = contextStacks[failStackTop];
		assert(stackTop.debugFailStackFlag);
		usedStackTop = failStackTop - 1;
		failStackTop = stackTop.prevFailTop;
		return op.next;
	}
	
	public final Instruction opFailSkip(FailSkip op) {
		ContextStack stackTop = contextStacks[failStackTop];
		assert(stackTop.debugFailStackFlag);
		stackTop.pos = this.pos;
		stackTop.lastLog = this.lastAppendedLog;
		assert(stackTop.lastLog != null);
		return op.next;
	}

	public final Instruction opFail() {
		ContextStack stackTop = contextStacks[failStackTop];
		assert(stackTop.debugFailStackFlag);
		usedStackTop = failStackTop - 1;
		failStackTop = stackTop.prevFailTop;
		this.pos = stackTop.pos;
		if(stackTop.lastLog != this.lastAppendedLog) {
			this.logAbort(stackTop.lastLog, true);
			//this.newPoint = stackTop.newPoint;
		}
		return stackTop.jump;
	}

	private final ContextStack newUnusedLocalStack() {
		ContextStack stackTop = newUnusedStack();
		assert(this.failStackTop < this.usedStackTop);
		stackTop.debugFailStackFlag = false;
		return stackTop;
	}
	
	private final ContextStack popLocalStack() {
		ContextStack stackTop = contextStacks[this.usedStackTop];
		usedStackTop--;
		assert(!stackTop.debugFailStackFlag);
		assert(this.failStackTop <= this.usedStackTop);
		return stackTop;
	}

	public final Instruction opCallPush(CallPush op) {
		ContextStack top = newUnusedLocalStack();
		top.jump = op.jump;
		return op.next;
	}
	
	public final Instruction opReturn() {
		Instruction jump = popLocalStack().jump;
		return jump;
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

	private final static int LazyLink    = 0;
	private final static int LazyCapture = 1;
	private final static int LazyTag     = 2;
	private final static int LazyReplace = 3;
	private final static int LazyLeftNew = 4;
	private final static int LazyNew     = 5;
	
	private class DataLog {
		int     type;
		long    pos;
		Object  value;
		DataLog prev;
		DataLog next;
		int id() {
			if(prev == null) return 0;
			return prev.id() + 1;
		}
		@Override
		public String toString() {
			switch(type) {
			case LazyLink:
				return "["+id()+"] link<" + this.pos + "," + this.value + ">";
			case LazyCapture:
				return "["+id()+"] cap<pos=" + this.pos + ">";
			case LazyTag:
				return "["+id()+"] tag<" + this.value + ">";
			case LazyReplace:
				return "["+id()+"] replace<" + this.value + ">";
			case LazyNew:
				return "["+id()+"] new<pos=" + this.pos + ">"  + "   ## " + this.value  ;
			case LazyLeftNew:
				return "["+id()+"] leftnew<pos=" + this.pos + "," + this.value + ">";
			}
			return "["+id()+"] nop";
		}
	}

	//private DataLog newPoint = null;
	private DataLog lastAppendedLog = null;
	private DataLog unusedDataLog = null;
	
	private final void pushDataLog(int type, long pos, Object value) {
		DataLog l;
		if(this.unusedDataLog == null) {
			l = new DataLog();
		}
		else {
			l = this.unusedDataLog;
			this.unusedDataLog = l.next;
		}
		l.type = type;
		l.pos  = pos;
		l.value = value;
		l.prev = lastAppendedLog;
		l.next = null;
		lastAppendedLog.next = l;
		lastAppendedLog = l;
	}
	
	public final Node logCommit(DataLog start) {
		assert(start.type == LazyNew);
		long spos = start.pos, epos = spos;
		Tag tag = null;
		Object value = null;
		int objectSize = 0;
		Node left = null;
		for(DataLog cur = start.next; cur != null; cur = cur.next ) {
			switch(cur.type) {
			case LazyLink:
				int index = (int)cur.pos;
				if(index == -1) {
					cur.pos = objectSize;
					objectSize++;
				}
				else if(!(index < objectSize)) {
					objectSize = index + 1;
				}
				break;
			case LazyCapture:
				epos = cur.pos;
				break;
			case LazyTag:
				tag = (Tag)cur.value;
				break;
			case LazyReplace:
				value = cur.value;
				break;
			case LazyLeftNew:
				left = commitNode(start, cur, spos, epos, objectSize, left, tag, value);
				start = cur;
				spos = cur.pos; 
				epos = spos;
				tag = null; value = null;
				objectSize = 1;
				break;
			}
		}
		return commitNode(start, null, spos, epos, objectSize, left, tag, value);
	}

	private Node commitNode(DataLog start, DataLog end, long spos, long epos,
			int objectSize, Node left, Tag tag, Object value) {
		Node newnode = this.base.newNode(tag, this, spos, epos, objectSize);
		if(left != null) {
			newnode.link(0, left);
		}
		if(value != null) {
			newnode.setValue(value);
		}
		if(objectSize > 0) {
//			System.out.println("PREV " + start.prev);
//			System.out.println(">>> BEGIN");
//			System.out.println("  LOG " + start);
			for(DataLog cur = start.next; cur != end; cur = cur.next ) {
//				System.out.println("  LOG " + cur);
				if(cur.type == LazyLink) {
					newnode.link((int)cur.pos, (Node)cur.value);
				}
			}
//			System.out.println("<<< END");
//			System.out.println("COMMIT " + newnode);
		}
		return newnode.commit();
	}

	public final void logAbort(DataLog checkPoint, boolean isFail) {
		assert(checkPoint != null);
//		if(isFail) {
//			for(DataLog cur = checkPoint.next; cur != null; cur = cur.next ) {
//				System.out.println("ABORT " + cur);
//			}
//		}
		lastAppendedLog.next = this.unusedDataLog;
		this.unusedDataLog = checkPoint.next;
		this.unusedDataLog.prev = null;

		this.lastAppendedLog = checkPoint;
		this.lastAppendedLog.next = null;
	}

	public final Instruction opNodePush(Instruction op) {
		ContextStack top = newUnusedLocalStack();
		top.lastLog = this.lastAppendedLog;
		//top.pos = this.pos;
		return op.next;
	}
	
	public final Instruction opNodeStore(NodeStore op) {
		ContextStack top = popLocalStack();
		if(top.lastLog.next != null) {
			Node child = this.logCommit(top.lastLog.next);
			logAbort(top.lastLog, false);
			if(child != null) {
				pushDataLog(LazyLink, op.index, child);
			}
			this.left = child;
			//System.out.println("LINK " + this.lastAppendedLog);
		}
		return op.next;
	}

	public final Instruction opNew(NodeNew op) {
		pushDataLog(LazyNew, this.pos, null); //op.e);
		return op.next;
	}

	public final Instruction opNodeLeftLink(NodeLeftNew op) {
		pushDataLog(LazyLeftNew, this.pos, null); // op.e);
		return op.next;
	}

	public final Node newTopLevelNode() {
		for(DataLog cur = this.lastAppendedLog; cur != null; cur = cur.prev) {
			if(cur.type == LazyNew) {
				this.left = logCommit(cur);
				logAbort(cur.prev, false);
				return this.left;
			}
		}
		return null;
	}
	
	public final Instruction opNodeTag(NodeTag op) {
		pushDataLog(LazyTag, 0, op.tag);
		return op.next;
	}

	public final Instruction opReplace(NodeReplace op) {
		pushDataLog(LazyReplace, 0, op.value);
		return op.next;
	}

	public final Instruction opCapture(NodeCapture op) {
		pushDataLog(LazyCapture, this.pos, null);
		return op.next;
	}

	// Memoization
	MemoTable memoTable;
	
	public final Instruction opLookup(Lookup op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry m = memoTable.getMemo(pos, mp.id);
		if(m != null) {
			//this.memo.hit(m.consumed);
			if(m.failed) {
				return opFail();
			}
			pos += m.consumed;
			return op.skip;
		}
		return this.opFailPush(op);
	}

	public final Instruction opLookup2(Lookup2 op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry m = memoTable.getMemo2(pos, mp.id, stateValue);
		if(m != null) {
			//this.memo.hit(m.consumed);
			if(m.failed) {
				return opFail();
			}
			pos += m.consumed;
			return op.skip;
		}
		return this.opFailPush(op);
	}

	public final Instruction opLookupNode(LookupNode op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry entry = memoTable.getMemo(pos, mp.id);
		if(entry != null) {
			if(entry.failed) {
				return opFail();
			}
			pos += entry.consumed;
			pushDataLog(LazyLink, op.index, entry.result);
			return op.skip;
		}
		this.opFailPush(op);
		return this.opNodePush(op);
	}

	public final Instruction opLookupNode2(LookupNode op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry me = memoTable.getMemo2(pos, mp.id, stateValue);
		if(me != null) {
			if(me.failed) {
				return opFail();
			}
			pos += me.consumed;
			pushDataLog(LazyLink, op.index, me.result);
			return op.skip;
		}
		this.opFailPush(op);
		return this.opNodePush(op);
	}

	public final Instruction opMemoize(Memoize op) {
		MemoPoint mp = op.memoPoint;
		ContextStack stackTop = contextStacks[this.usedStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, null, length, 0);
		return this.opFailPop(op);
	}

	public final Instruction opMemoize2(Memoize op) {
		MemoPoint mp = op.memoPoint;
		ContextStack stackTop = contextStacks[this.usedStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, null, length, stateValue);
		return this.opFailPop(op);
	}

	public final Instruction opMemoizeNode(MemoizeNode op) {
		MemoPoint mp = op.memoPoint;
		this.opNodeStore(op);
		assert(this.usedStackTop == this.failStackTop);
		ContextStack stackTop = contextStacks[this.failStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, 0);
		return this.opFailPop(op);
	}

	public Instruction opMemoizeNode2(MemoizeNode op) {
		MemoPoint mp = op.memoPoint;
		this.opNodeStore(op);
		assert(this.usedStackTop == this.failStackTop);
		ContextStack stackTop = contextStacks[this.failStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, stateValue);
		return this.opFailPop(op);
	}

	public final Instruction opMemoizeFail(MemoizeFail op) {
		MemoPoint mp = op.memoPoint;
		memoTable.setMemo(pos, mp.id, true, null, 0, 0);
		return opFail();
	}

	public final Instruction opMemoizeFail2(MemoizeFail op) {
		MemoPoint mp = op.memoPoint;
		memoTable.setMemo(pos, mp.id, true, null, 0, stateValue);
		return opFail();
	}


	// symbol table

	public Instruction opDefString(DefString op) {
		ContextStack top = popLocalStack();
		this.pushSymbolTable(op.tableName, this.substring(top.pos, this.pos));
		return op.next;
	}


}
