package nez.vm;

import nez.expr.Expression;
import nez.expr.Link;

public interface Memoization {

}

class Lookup extends FailPush implements Memoization {
	final MemoPoint memoPoint;
	final Instruction skip;
	public Lookup(Compiler bc, Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(bc, e, failjump, next);
		this.memoPoint = m;
		this.skip = labeling(skip);
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookup(this);
	}
}

class Lookup2 extends Lookup {
	public Lookup2(Compiler bc, Expression e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(bc, e, m, next, skip, failjump);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookup2(this);
	}
}

class Memoize extends Instruction implements Memoization {
	final MemoPoint memoPoint;
	public Memoize(Compiler optimizer, Expression e, MemoPoint m, Instruction next) {
		super(optimizer, e, next);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoize(this);
	}
}

class Memoize2 extends Memoize {
	public Memoize2(Compiler bc, Expression e, MemoPoint m, Instruction next) {
		super(bc, e, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoize2(this);
	}
}

class MemoizeFail extends Fail implements Memoization {
	MemoPoint memoPoint;
	MemoizeFail(Compiler optimizer, Expression e, MemoPoint m) {
		super(optimizer, e);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeFail(this);
	}
}

class MemoizeFail2 extends MemoizeFail {
	MemoizeFail2(Compiler optimizer, Expression e, MemoPoint m) {
		super(optimizer, e, m);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeFail2(this);
	}
}

class LookupNode extends Lookup {
	int index;
	public LookupNode(Compiler bc, Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(bc, e, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookupNode(this);
	}
}

class LookupNode2 extends LookupNode {
	int index;
	public LookupNode2(Compiler bc, Link e, MemoPoint m, Instruction next, Instruction skip, Instruction failjump) {
		super(bc, e, m, next, skip, failjump);
		this.index = e.index;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opLookupNode2(this);
	}
}

class MemoizeNode extends NodeStore implements Memoization {
	MemoPoint memoPoint;
	public MemoizeNode(Compiler optimizer, Link e, MemoPoint m, Instruction next) {
		super(optimizer, e, next);
		this.memoPoint = m;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName() + " " + this.memoPoint.id);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeNode(this);
	}
}

class MemoizeNode2 extends MemoizeNode {
	public MemoizeNode2(Compiler bc, Link e, MemoPoint m, Instruction next) {
		super(bc, e, m, next);
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMemoizeNode2(this);
	}
}