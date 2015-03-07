package nez.x;

import nez.ast.Tag;
import nez.expr.Capture;
import nez.expr.Choice;
import nez.expr.Expression;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NonTerminal;
import nez.expr.Replace;
import nez.expr.Tagging;
import nez.expr.Typestate;
import nez.util.UList;

public abstract class Type {
	abstract Type dup();
	abstract void tag(Tag t);
	abstract void link(int index, Type t);
	abstract void stringfy(StringBuilder sb);
	
	public static Type infer(Expression e) {
		Continuation c = Continuation.newContinuation(e, null);
		if(c != null) {
			c.dump();
		}
		return typeUnion(null, c);
	}
	
	static Type typeUnion(Type t0, Continuation c) {
		Type t1 = typeSingleton(t0, c);
		if(c != null && c.choice != null) {
			UList<Type> l = null;
			c = c.choice;
			while(c != null) {
				Type t2 = dup(t0);
				t2 = typeSingleton(t2, c.choice);
				if(l == null) {
					l = new UList<Type>(new Type[2]);
					l.add(t2);
				}
				c = c.choice;
			}
			if(l != null) {
				t1 = new UnionType(t1, l);
			}
		}
		return t1;
	}
	static Type dup(Type t) {
		return t == null ? null : t.dup();
	}
	static Type typeSingleton(Type t0, Continuation c) {
		Type t = t0;
		while(c != null) {
			Expression p = c.e;
			if(p instanceof New) {
				t = new AtomType();
			}
			if(p instanceof NonTerminal) {
				t = new RefType((NonTerminal)p);
			}
			if(p instanceof Tagging /*&& t != null*/) {
				t.tag(((Tagging) p).tag);
			}
			if(p instanceof Link /*&& t != null*/) {
				Type t2 = typeUnion(null, Continuation.newContinuation(p.get(0), null));
				if(t != null) {
					t.link(((Link) p).index, t2);
				}
			}
			c = c.next;
		}
		return t;
	}

}

/* A = 'a' A 'a' / 'a' ; */

class Continuation {
	Expression e;
	Continuation next;
	Continuation choice;
	Continuation(Expression e, Continuation next) {
		this.e = e;
		this.next = next;
		this.choice = null;
	}
	void addChoice(Continuation c) {
		Continuation self = this;
		while(self.choice != null) {
			self = self.choice;
		}
		self.choice = c;
	}
	static Continuation newContinuation(Expression e, Continuation next) {
		if(e.inferTypestate() == Typestate.BooleanType) {
			return next;
		}
		if(e instanceof Replace || e instanceof Capture) {
			return next; // ignored
		}
		if(e instanceof Link) {
			next = new Continuation(e, next);
		}
		if(e instanceof Choice) {
			Continuation firstChoice = null;
			for(Expression p: e) {
				Continuation c = newContinuation(p, next);
				if(firstChoice == null) {
					firstChoice = c;
				}
				else {
					firstChoice.addChoice(c);
				}
			}
			return firstChoice;
		}
		if(e.size() > 0) {
			for(Expression p: e) {
				next = newContinuation(p, next);
			}
			return next;
		}
		return new Continuation(e, next);
	}
	void dump() {
		Continuation c = this;
		while(c != null) {
			System.out.print(c.e);
			System.out.print(",");
			c = c.next;
		}
		System.out.println();
	}
}

class AtomType extends Type {
	Tag tag = null;
	Type[] record;
	int size = 0;
	int last = 0;
	AtomType() {
		this.record = new Type[2];
	}
	@Override
	Type dup() {
		AtomType t = new AtomType();
		t.record = new Type[this.record.length];
		System.arraycopy(this.record, 0, t.record, 0, size);
		t.size = this.size;
		t.last = this.last;
		return t;
	}
	@Override
	void tag(Tag tag) {
		this.tag = tag;
	}
	@Override
	void link(int index, Type t) {
		if(index < 0) {
			index = last;
			last++;
		}
		else {
			if(last <= index) {
				last = index + 1;
			}
		}
		if(!(index < this.record.length)) {
			expand(index);
		}
		this.record[index] = t;
	}
	
	private void expand(int index) {
		int newsize = this.record.length * 2;
		if(!(index < newsize)) {
			newsize = index + 1;
		}
		Type[] newrecord = new Type[newsize];
		System.arraycopy(this.record, 0, newrecord, 0, this.record.length);
		this.record = newrecord;
	}
	
	@Override
	void stringfy(StringBuilder sb) {
		sb.append("#");
		sb.append((this.tag == null) ? "Text" : this.tag.getName());
		if(size > 0) {
			sb.append("(");
			for(int i = 0; i < size; i++) {
				if(i > 0) {
					sb.append(",");
				}
				if(this.record[i] == null) {
					sb.append("empty");
				}
				else {
					this.record[i].stringfy(sb);
				}
			}
			sb.append(")");
		}
	}
}

class RefType extends Type {
	NonTerminal p;
	RefType(NonTerminal p) {
		this.p = p;
	}
	@Override
	Type dup() {
		RefType t = new RefType(p);
		return t;
	}
	@Override
	void tag(Tag tag) {

	}
	@Override
	void link(int index, Type t) {

	}
	
	@Override
	void stringfy(StringBuilder sb) {
		sb.append(this.p.getUniqueName());
	}
}

class UnionType extends Type {
	Type[] union;
	UnionType(Type t, UList<Type> l) {
		this.union = new Type[l.size() + 1];
		this.union[0] = t;
		int c = 1;
		for(Type t2 : l) {
			this.union[c] = t2; c++;
		}
	}
	private UnionType(Type[] u) {
		this.union = new Type[u.length];
		for(int i = 0; i < u.length; i++) {
			this.union[i] = u[i].dup();
		}
	}
	@Override
	Type dup() {
		return new UnionType(this.union);
	}
	@Override
	void tag(Tag tag) {
		for(Type t : this.union) {
			t.tag(tag);
		}
	}
	@Override
	void link(int index, Type t2) {
		for(Type t : this.union) {
			t.link(index, t2);
		}
	}
	@Override
	void stringfy(StringBuilder sb) {
		for(int i = 0; i < union.length; i++) {
			if(i > 0) {
				sb.append("|");
			}
			this.union[i].stringfy(sb);
		}
	}
}


