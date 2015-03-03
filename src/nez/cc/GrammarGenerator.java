package nez.cc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.Grammar;
import nez.expr.Expression;
import nez.expr.Rule;
import nez.util.FileBuilder;
import nez.util.UList;
import nez.vm.Instruction;

public class GrammarGenerator {
	final protected FileBuilder file;
	GrammarGenerator(String fileName) {
		this.file = new FileBuilder(fileName);
	}
	HashMap<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>();
	public final void visit(Expression p) {
		Method m = lookupMethod("visit", p.getClass());
		if(m != null) {
			try {
				m.invoke(this, p);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		else {
			visitUndefined(p);
		}
	}

	void visitUndefined(Expression p) {
		System.out.println("undefined: " + p.getClass());
	}

	protected final Method lookupMethod(String method, Class<?> c) {
		Method m = this.methodMap.get(c);
		if(m == null) {
			String name = method + c.getSimpleName();
			try {
				m = this.getClass().getMethod(name, Instruction.class);
			} catch (NoSuchMethodException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(c, m);
		}
		return m;
	}
	
	public void generate(Grammar grammar) {
		UList<Rule> list = grammar.getRuleList();
		for(Rule r: list) {
			file.writeIndent(r.toString());
		}
		file.writeNewLine();
		file.flush();
	}
}
