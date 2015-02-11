package nez.vm;

import nez.SourceContext;
import nez.expr.Expression;

public abstract class Instruction {
	protected Expression  e;
	public Instruction next;
	int id;
	boolean label = false;
	public Instruction(Optimizer optimizer, Expression e, Instruction next) {
		this.e = e;
		this.id = -1;
		this.next = next;
	}
	
	Instruction branch() {
		return null;
	}
	
	protected static Instruction labeling(Instruction inst) {
		if(inst != null) {
			inst.label = true;
		}
		return inst;
	}
	
	protected static String label(Instruction inst) {
		return "L"+inst.id;
	}

	protected abstract void stringfy(StringBuilder sb);
	
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}
	
	boolean debug() {
		return false;
	}
	
	abstract Instruction exec(Context sc) throws TerminationException;
	
	public static boolean run(Instruction code, SourceContext sc) {
		boolean result = false;
		try {
			while(true) {
//				if(code.debug()) {
//					System.out.println("" + code);
//				}
				code = code.exec(sc);
			}
		}
		catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}
}
