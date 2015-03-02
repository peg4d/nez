package nez.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class NodeVisitor {
	HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	public final Object visit(String method, Node node) {
		Tag tag = node.getTag();
		Method m = lookupMethod(method, tag.id);
		if(m != null) {
			try {
				return m.invoke(this, node);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println(node);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public final Object visit(Node node) {
		return visit("to", node);
	}

	public final boolean isSupported(String method, String tagName) {
		return lookupMethod(method, Tag.tag(tagName).id) != null;
	}
	
	protected Method lookupMethod(String method, int tagId) {
		Integer key = tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			String name = method + Tag.tag(tagId).getName();
			try {
				m = this.getClass().getMethod(name, AST.class);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(key, m);
		}
		return m;
	}
}
