package nez.cc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.vm.Instruction;

public class ParserGenerator {
	HashMap<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>();
	public final void visit(Instruction inst) {
		Method m = lookupMethod("visit", inst.getClass());
		if(m != null) {
			try {
				m.invoke(this, inst);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		else {
			visitUndefined(inst);
		}
	}
	
	void visitUndefined(Instruction inst) {
		System.out.println("undefined: " + inst.getClass());
	}
		
	private final Method lookupMethod(String method, Class<?> c) {
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

}
