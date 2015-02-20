package nez.vm;

import java.util.HashMap;

import nez.ast.Node;

public abstract class MemoTable {
	abstract void setMemo(long pos, int memoPoint, boolean failed, Node result, int consumed, int stateValue);
//	abstract void setMemo(long pos, int memoPoint, Node result, int consumed);
//	abstract void setFailure(long pos, int memoPoint);
	abstract MemoEntry getMemo(long pos, int memoPoint);
	abstract MemoEntry getMemo2(long pos, int memoPoint, int stateValue);
//	abstract void setMemo2(long pos, int id, int stateValue, Node left, int length);
//	abstract void setFailure2(long pos, int memoPoint, int stateValue);

	int CountStored;
	int CountUsed;
	int CountInvalidated;

	void initStat() {
		this.CountStored = 0;
		this.CountUsed = 0;
		this.CountInvalidated = 0;
	}

//	protected void stat(NezLogger stat) {
//		stat.setText("Memo", this.getClass().getSimpleName());
//		stat.setCount("MemoUsed",    this.MemoUsed);
//		stat.setCount("MemoConflicted",  this.MemoStateConflicted);
//		stat.setCount("MemoStored",  this.MemoStored);
//		stat.setRatio("Used/Stored", this.MemoUsed, this.MemoStored);
//		stat.setRatio("Conflicted/Stored", this.MemoStateConflicted, this.MemoStored);
//		stat.setRatio("HitRatio",    this.MemoUsed, this.MemoMissed);
//	}
	
	static MemoTable newNullTable(int len, int w, int n) {
		return new NullTable(len, w, n);
	}

	static MemoTable newElasticTable(int len, int w, int n) {
		return new ElasticTable(len, w, n);
	}

	static MemoTable newPackratHashTable(int len, int w, int n) {
		return new PackratHashTable(len, w, n);
	}
	
}

class NullTable extends MemoTable {
	NullTable(int len, int w, int n) {
		this.initStat();
	}
	
	@Override
	void setMemo(long pos, int memoPoint, boolean failed, Node result,
			int consumed, int stateValue) {
		this.CountStored += 1;
	}

	@Override
	MemoEntry getMemo(long pos, int id) {
		return null;
	}

	@Override
	MemoEntry getMemo2(long pos, int id, int stateValue) {
		return null;
	}

}

class ElasticTable extends MemoTable {
	private MemoEntryKey[] memoArray;
	private final int shift;

	ElasticTable(int len, int w, int n) {
		this.memoArray = new MemoEntryKey[w * n + 1];
		for(int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntryKey();
			this.memoArray[i].key = -1;
		}
		this.shift = (int)(Math.log(n) / Math.log(2.0)) + 1;
		this.initStat();
	}
	
	protected long longkey(long pos, int memoPoint, int shift) {
		return ((pos << shift) | memoPoint) & Long.MAX_VALUE;
	}
	
	@Override
	void setMemo(long pos, int memoPoint, boolean failed, Node result, int consumed, int stateValue) {
		long key = longkey(pos, memoPoint, shift);
		int hash =  (int)(key % memoArray.length);
		MemoEntryKey m = this.memoArray[hash];
		m.failed = failed;
		m.result = result;
		m.consumed = consumed;
		m.stateValue = stateValue;
		this.CountStored += 1;
	}

	@Override
	protected final MemoEntry getMemo(long pos, int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash =  (int)(key % memoArray.length);
		MemoEntryKey m = this.memoArray[hash];
		if(m.key == key) {
			this.CountUsed += 1;
			return m;
		}
		return null;
	}

	@Override
	protected final MemoEntry getMemo2(long pos, int memoPoint, int stateValue) {
		long key = longkey(pos, memoPoint, shift);
		int hash =  (int)(key % memoArray.length);
		MemoEntryKey m = this.memoArray[hash];
		if(m.key == key) {
			if(m.stateValue == stateValue) {
				this.CountUsed += 1;
				return m;
			}
			this.CountInvalidated += 1;
		}
		return null;
	}

}

class PackratHashTable extends MemoTable {
	HashMap<Long, MemoEntryList> memoMap;
	private MemoEntryList UnusedMemo = null;

	PackratHashTable(int len, int w, int n) {
		this.memoMap = new HashMap<Long, MemoEntryList>(w * n);
	}
	
	private final MemoEntryList newMemo() {
		if(UnusedMemo != null) {
			MemoEntryList m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			return m;
		}
		else {
			return new MemoEntryList();
		}
	}
	
	protected final void unusedMemo(MemoEntryList m) {
		MemoEntryList s = m;
		while(m.next != null) {
			m = m.next;
		}
		m.next = this.UnusedMemo;
		UnusedMemo = s;
	}
	
	@Override
	protected MemoEntry getMemo(long pos, int memoPoint) {
		MemoEntryList m = this.memoMap.get(pos);
		while(m != null) {
			if(m.memoPoint == memoPoint) {
				this.CountUsed += 1;
				return m;
			}
			m = m.next;
		}
		return m;
	}

	@Override
	protected MemoEntry getMemo2(long pos, int memoPoint, int stateValue) {
		MemoEntryList m = this.memoMap.get(pos);
		while(m != null) {
			if(m.memoPoint == memoPoint) {
				if(m.stateValue == stateValue) {
					this.CountUsed += 1;
					return m;					
				}
				this.CountInvalidated += 1;
			}
			m = m.next;
		}
		return m;
	}

	@Override
	void setMemo(long pos, int memoPoint, boolean failed, Node result,
			int consumed, int stateValue) {
		MemoEntryList m = newMemo();
		m.failed = failed;
		m.memoPoint = memoPoint;
		m.stateValue = stateValue;
		m.result = result;
		m.consumed = consumed;
		Long key = pos;
		m.next = this.memoMap.get(key);
		this.memoMap.put(key, m);
		this.CountStored += 1;
	}


}


