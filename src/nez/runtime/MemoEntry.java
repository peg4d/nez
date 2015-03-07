package nez.runtime;

import nez.ast.Node;

class MemoEntry {
	boolean failed;
	int  consumed;
	Node result;
//	int  memoPoint;
	int  stateValue = 0;
}

class MemoEntryKey extends MemoEntry {
	long key = -1;
}

class MemoEntryList extends MemoEntry {
	int memoPoint;
	MemoEntryList next;
}