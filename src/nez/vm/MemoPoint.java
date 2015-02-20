package nez.vm;

import nez.expr.Expression;

public class MemoPoint {
	final int id;
	final Expression e;
	final boolean contextSensitive;

	int memoHit = 0;
	long hitLength = 0;
	int  maxLength = 0;
	int memoMiss = 0;
	
	MemoPoint(int id, Expression e, boolean contextSensitive) {
		this.id = id;
		this.e = e;
		this.contextSensitive = contextSensitive;
	}
	
	void hit(int consumed) {
		this.memoHit += 1;
		this.hitLength += consumed;
		if(this.maxLength < consumed) {
			this.maxLength = consumed;
		}
	}
	
	final double ratio() {
		if(this.memoMiss == 0.0) return 0.0;
		return (double)this.memoHit / this.memoMiss;
	}

	final double length() {
		if(this.memoHit == 0) return 0.0;
		return (double)this.hitLength / this.memoHit;
	}

	final int count() {
		return this.memoMiss + this.memoHit;
	}

	protected final boolean checkUseless() {
		if(this.memoMiss == 32) {
			if(this.memoHit < 2) {          
				return true;
			}
		}
		if(this.memoMiss % 64 == 0) {
			if(this.memoHit == 0) {
				return true;
			}
//			if(this.hitLength < this.memoHit) {
//				enableMemo = false;
//				disabledMemo();
//				return;
//			}
			if(this.memoMiss / this.memoHit > 10) {
				return true;
			}
		}
		return false;
	}


}
