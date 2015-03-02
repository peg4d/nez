package nez.vm;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.Source;
import nez.ast.Tag;
import nez.expr.NezTag;
import nez.expr.NonTerminal;
import nez.main.Recorder;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
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
		int len;
		SymbolTableEntry(Tag table, String indent) {
			this.table = table;
			this.utf8 = indent.getBytes();
			this.len = utf8.length;
		}
		SymbolTableEntry(Tag table, byte[] b) {
			this.table = table;
			this.utf8 = b;
			this.len = utf8.length;
		}
		final boolean match(byte[] b) {
			if(this.len == b.length) {
				for(int i = 0; i < this.len; i++) {
					if(utf8[i] != b[i]) {
						return false;
					}
				}
			}
			return true;
		}
	}

	public final void pushSymbolTable(Tag table, byte[] s) {
		this.stackedSymbolTable.add(new SymbolTableEntry(table, s));
		this.stateCount += 1;
		this.stateValue = stateCount;
	}

	public final void popSymbolTable(int stackTop) {
		this.stackedSymbolTable.clear(stackTop);
	}

	public final boolean matchSymbolTable(Tag table, byte[] symbol, boolean onlyTop) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == table) {
				if(s.match(symbol)) {
					return true;
				}
				if(onlyTop) break;
			}
		}
		return true;
	}

	
	public final int pushSymbolTable(Tag table, String s) {
		int stackTop = this.stackedSymbolTable.size();
		this.stackedSymbolTable.add(new SymbolTableEntry(table, s));
		this.stateCount += 1;
		this.stateValue = stateCount;
		return stackTop;
	}

	public final boolean matchSymbolTable(Tag table, boolean checkLastSymbolOnly) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == table) {
				if(this.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
				if(checkLastSymbolOnly) {
					break;
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
		this.contextStacks[0].jump = new IExit(false);
		this.contextStacks[0].debugFailStackFlag = true;
		this.contextStacks[0].lastLog = this.lastAppendedLog;
		this.contextStacks[1].jump = new IExit(true);  // for a point of the first called nonterminal
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

	public final Instruction opIFailPush(IFailPush op) {
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

	public final Instruction opIFailPop(Instruction op) {
		ContextStack stackTop = contextStacks[failStackTop];
		assert(stackTop.debugFailStackFlag);
		usedStackTop = failStackTop - 1;
		failStackTop = stackTop.prevFailTop;
		return op.next;
	}
	
	public final Instruction opIFailSkip(IFailSkip op) {
		ContextStack stackTop = contextStacks[failStackTop];
		if(this.pos == stackTop.pos) {
			return opIFail();
		}
		stackTop.pos = this.pos;
		stackTop.lastLog = this.lastAppendedLog;
		return op.next;
	}

	public final Instruction opIFailSkip_(IFailSkip op) {
		ContextStack stackTop = contextStacks[failStackTop];
		assert(stackTop.debugFailStackFlag);
		if(this.pos == stackTop.pos) {
			return opIFail();
		}
		stackTop.pos = this.pos;
		stackTop.lastLog = this.lastAppendedLog;
		assert(stackTop.lastLog != null);
		return op.next;
	}

	public final Instruction opIFail() {
		ContextStack stackTop = contextStacks[failStackTop];
		assert(stackTop.debugFailStackFlag);
		usedStackTop = failStackTop - 1;
		failStackTop = stackTop.prevFailTop;
		if(this.prof != null) {
			this.prof.statBacktrack(stackTop.pos, this.pos);
		}
		this.pos = stackTop.pos;
		if(stackTop.lastLog != this.lastAppendedLog) {
			this.logAbort(stackTop.lastLog, true);
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

	public final Instruction opICallPush(ICallPush op) {
		ContextStack top = newUnusedLocalStack();
		top.jump = op.jump;
		return op.next;
	}
	
	public final Instruction opIRet() {
		Instruction jump = popLocalStack().jump;
		return jump;
	}
	
	public final Instruction opIPosPush(IPosPush op) {
		ContextStack top = newUnusedLocalStack();
		top.pos = pos;
		return op.next;
	}

	public final Instruction opIPopBack(IPosBack op) {
		ContextStack top = popLocalStack();
		this.pos = top.pos;
		return op.next;
	}

	public final Instruction opIAnyChar(IAnyChar op) {
		if(this.hasUnconsumed()) {
			this.consume(1);
			return op.next;
		}
		return this.opIFail();
	}

	public final Instruction opIByteChar(IByteChar op) {
		if(this.byteAt(this.pos) == op.byteChar) {
			this.consume(1);
			return op.next;
		}
		return op.optional ? op.next : this.opIFail();
	}

	public final Instruction opIByteMap(IByteMap op) {
		int byteChar = this.byteAt(this.pos);
		if(op.byteMap[byteChar]) {
			this.consume(1);
			return op.next;
		}
		return op.optional ? op.next : this.opIFail();
	}

//	public final Instruction opMatchByteMap(IByteMap op) {
//		int byteChar = this.byteAt(this.pos);
//		if(op.byteMap[byteChar]) {
//			this.consume(1);
//			return op.next;
//		}
//		return this.opFail();
//	}

	

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
	
	public final Instruction opNodeStore(INodeStore op) {
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

	public final Instruction opINew(INew op) {
		pushDataLog(LazyNew, this.pos, null); //op.e);
		return op.next;
	}

	public final Instruction opILeftNew(ILeftNew op) {
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
	
	public final Instruction opITag(ITag op) {
		pushDataLog(LazyTag, 0, op.tag);
		return op.next;
	}

	public final Instruction opIReplace(IReplace op) {
		pushDataLog(LazyReplace, 0, op.value);
		return op.next;
	}

	public final Instruction opICapture(ICapture op) {
		pushDataLog(LazyCapture, this.pos, null);
		return op.next;
	}

	// Memoization
	MemoTable memoTable;
	
	public final Instruction opILookup(ILookup op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry m = memoTable.getMemo(pos, mp.id);
		if(m != null) {
			//this.memo.hit(m.consumed);
			if(m.failed) {
				return opIFail();
			}
			pos += m.consumed;
			return op.skip;
		}
		return this.opIFailPush(op);
	}

	public final Instruction opIStateLookup(IStateLookup op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry m = memoTable.getMemo2(pos, mp.id, stateValue);
		if(m != null) {
			//this.memo.hit(m.consumed);
			if(m.failed) {
				return opIFail();
			}
			pos += m.consumed;
			return op.skip;
		}
		return this.opIFailPush(op);
	}

	public final Instruction opLookupNode(ILookupNode op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry entry = memoTable.getMemo(pos, mp.id);
		if(entry != null) {
			if(entry.failed) {
				return opIFail();
			}
			pos += entry.consumed;
			pushDataLog(LazyLink, op.index, entry.result);
			return op.skip;
		}
		this.opIFailPush(op);
		return this.opNodePush(op);
	}

	public final Instruction opLookupNode2(ILookupNode op) {
		MemoPoint mp = op.memoPoint;
		MemoEntry me = memoTable.getMemo2(pos, mp.id, stateValue);
		if(me != null) {
			if(me.failed) {
				return opIFail();
			}
			pos += me.consumed;
			pushDataLog(LazyLink, op.index, me.result);
			return op.skip;
		}
		this.opIFailPush(op);
		return this.opNodePush(op);
	}

	public final Instruction opIMemoize(IMemoize op) {
		MemoPoint mp = op.memoPoint;
		ContextStack stackTop = contextStacks[this.usedStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, null, length, 0);
		return this.opIFailPop(op);
	}

	public final Instruction opIStateMemoize(IMemoize op) {
		MemoPoint mp = op.memoPoint;
		ContextStack stackTop = contextStacks[this.usedStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, null, length, stateValue);
		return this.opIFailPop(op);
	}

	public final Instruction opMemoizeNode(MemoizeNode op) {
		MemoPoint mp = op.memoPoint;
		this.opNodeStore(op);
		assert(this.usedStackTop == this.failStackTop);
		ContextStack stackTop = contextStacks[this.failStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, 0);
		return this.opIFailPop(op);
	}

	public Instruction opMemoizeNode2(MemoizeNode op) {
		MemoPoint mp = op.memoPoint;
		this.opNodeStore(op);
		assert(this.usedStackTop == this.failStackTop);
		ContextStack stackTop = contextStacks[this.failStackTop];
		int length = (int)(this.pos - stackTop.pos);
		memoTable.setMemo(stackTop.pos, mp.id, false, this.left, length, stateValue);
		return this.opIFailPop(op);
	}

	public final Instruction opIMemoizeFail(IMemoizeFail op) {
		MemoPoint mp = op.memoPoint;
		memoTable.setMemo(pos, mp.id, true, null, 0, 0);
		return opIFail();
	}

	public final Instruction opIStateMemoizeFail(IMemoizeFail op) {
		MemoPoint mp = op.memoPoint;
		memoTable.setMemo(pos, mp.id, true, null, 0, stateValue);
		return opIFail();
	}


	// Specialization 
	
	public final Instruction opRByteMap(RByteMap op) {
		while(true) {
			int c = this.byteAt(this.pos);
			if(!op.byteMap[c]) {
				break;
			}
			this.consume(1);
		}
		return op.next;
	}

	public final Instruction opNByteMap(NByteMap op) {
		int c = this.byteAt(this.pos);
		if(!op.byteMap[c]) {
			return op.next;
		}
		return this.opIFail();
	}

	public final Instruction opNMultiChar(NMultiChar op) {
		if(!this.match(this.pos, op.utf8)) {
			return op.next;
		}
		return this.opIFail();
	}

	public final Instruction opMultiChar(IMultiChar op) {
		if(this.match(pos, op.utf8)) {
			this.consume(op.len);
			return op.next;
		}
		return op.optional ? op.next : this.opIFail();
	}

	// symbol table

	public final Instruction opIDefSymbol(IDefSymbol op) {
		ContextStack top = popLocalStack();
		this.pushSymbolTable(op.tableName, this.subbyte(top.pos, this.pos));
		return op.next;
	}

	public final Instruction opIIsSymbol(IIsSymbol op) {
		ContextStack top = popLocalStack();
		if(this.matchSymbolTable(op.tableName, this.subbyte(top.pos, this.pos), op.checkLastSymbolOnly)) {
			return op.next;
		}
		return opIFail();
	}

	public final Instruction opIDefIndent(IDefIndent op) {
		long spos = this.getLineStartPosition(this.pos);
		byte[] b = this.subbyte(spos, this.pos);
		for(int i = 0; i < b.length; i++) {
			if(b[i] != '\t') {
				b[i] = ' ';
			}
		}
		this.pushSymbolTable(NezTag.Indent, b);
		return op.next;
	}

	private final long getLineStartPosition(long fromPostion) {
		long startIndex = fromPostion;
		if(!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			int ch = byteAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final Instruction opIIsIndent(IIsIndent op) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == NezTag.Indent) {
				if(this.match(this.pos, s.utf8)) {
					consume(s.len);
					return op.next;
				}
				return opIFail();
			}
		}
		// no indent (unconsumed)
		return op.next;
	}

	public final Instruction opITablePush(ITablePush op) {
		ContextStack top = this.newUnusedLocalStack();
		top.pos = this.stackedSymbolTable.size();
		top.prevFailTop = this.stateValue;
		return op.next;
	}

	public final Instruction opITablePop(ITablePop op) {
		ContextStack top = popLocalStack();
		this.stateValue = top.prevFailTop;
		this.popSymbolTable((int)top.pos);
		return op.next;
	}

	//<scan T 0 e>
	//<repeat T e>

	
	// Profiling
	private Prof prof;
	public final void start(Recorder rec) {
		if(rec != null) {
			rec.setFile("I.File",  this.getResourceName());
			rec.setCount("I.Size", this.length());
			this.prof = new Prof();
			this.prof.init(this.getPosition());
		}
	}

	public final void done(Recorder rec) {
		if(rec != null) {
			this.prof.parsed(rec, this.getPosition());
		}
	}

	class Prof {
		long startPosition = 0;
		long startingNanoTime = 0;
		long endingNanoTime   = 0;
		
		long FailureCount   = 0;
		long BacktrackCount = 0;
		long BacktrackLength = 0;
		
		long HeadPostion = 0;
		long LongestBacktrack = 0;
		int[] BacktrackHistgrams = null;
		
		public void init(long pos) {
			this.startPosition = pos;
			this.startingNanoTime = System.nanoTime();
			this.endingNanoTime = startingNanoTime;
			this.FailureCount = 0;
			this.BacktrackCount = 0;
			this.BacktrackLength = 0;
			this.LongestBacktrack = 0;
			this.HeadPostion = 0;
			this.BacktrackHistgrams = new int[32];
		}
		
		void parsed(Recorder rec, long consumed) {
			consumed -= this.startPosition;
			this.endingNanoTime = System.nanoTime();
			Recorder.recordLatencyMS(rec, "P.Latency", startingNanoTime, endingNanoTime);
			rec.setCount("P.Consumed", consumed);
			Recorder.recordThroughputKPS(rec, "P.Throughput", consumed, startingNanoTime, endingNanoTime);
			rec.setRatio("P.Failure", this.FailureCount, consumed);
			rec.setRatio("P.Backtrack", this.BacktrackCount, consumed);
			rec.setRatio("P.BacktrackLength", this.BacktrackLength, consumed);
			rec.setCount("P.LongestBacktrack", LongestBacktrack);
			if(Verbose.Backtrack) {
				double cf = 0;
				for(int i = 0; i < 16; i++) {
					int n = 1 << i;
					double f = (double)this.BacktrackHistgrams[i] / this.BacktrackCount;
					cf += this.BacktrackHistgrams[i];
					ConsoleUtils.println(String.format("%d\t%d\t%2.3f\t%2.3f", n, this.BacktrackHistgrams[i], f, (cf / this.BacktrackCount)));
					if(n > this.LongestBacktrack) break;
				}
			}
		}

		public final void statBacktrack(long backed_pos, long current_pos) {
			this.FailureCount ++;
			long len = current_pos - backed_pos;
			if(len > 0) {
				this.BacktrackCount = this.BacktrackCount + 1;
				this.BacktrackLength  = this.BacktrackLength + len;
				if(this.HeadPostion < current_pos) {
					this.HeadPostion = current_pos;
				}
				len = this.HeadPostion - backed_pos;
				this.countBacktrackLength(len);
				if(len > this.LongestBacktrack) {
					this.LongestBacktrack = len;
				}
			}
		}

		private void countBacktrackLength(long len) {
			int n = (int)(Math.log(len) / Math.log(2.0));
			BacktrackHistgrams[n] += 1;
		}

	}
	
}
