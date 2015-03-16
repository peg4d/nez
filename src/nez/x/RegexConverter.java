package nez.x;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.Node;
import nez.ast.NodeVisitor;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class RegexConverter extends NodeVisitor{
	HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	Grammar grammar;
	public RegexConverter(Grammar grammar) {
		this.grammar = grammar;
	}
	
	public final Object visit(Node expr, Node k) {
		Tag tag = expr.getTag();
		Method m = lookupPiMethod("pi", tag.id);
		if(m != null) {
			try {
				return m.invoke(this, expr, k);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println(expr);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	protected Method lookupPiMethod(String method, int tagId) {
		Integer key = tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			String name = method + Tag.tag(tagId).getName();
			try {
				m = this.getClass().getMethod(name, AST.class, AST.class);
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
	
	public void convert(AST e) {
		grammar.defineRule(e, "Pattern", pi(e, null));
		System.out.println("\nConverted Rule:\n");
		grammar.dump();
	}
	
	public Expression pi(AST e, AST k) {
		return (Expression) this.visit(e, k);
	}
	
	public Expression piPattern(AST e, AST k) {
		return this.pi(e.get(0), k);
	}

	public Expression piOr(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piConcatenation(AST e, AST k) {
		return toSeq(e);
	}

	public Expression piIndependentExpr(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piAnd(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piNot(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piPossessiveRepetition(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piLazyQuantifiers(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piRepetition(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piOneMoreRepetition(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piAny(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piNegativeCharacterSet(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piCharacterSet(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piCharacterRange(AST e, AST k) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	public Expression piCharacter(AST e, AST k) {
		return toExpression(e);
	}
	
	private Expression toExpression(AST node) {
		return (Expression)this.visit("to", node);
	}
	
	public Expression toCharacter(AST node) {
		String text = node.getText();
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length !=1) {
			ConsoleUtils.exit(1, "Error: not Character Literal");
		}
		return Factory.newByteChar(node, utf8[0]);
	}
	
	public Expression toEMPTY(AST node) {
		return Factory.newNonTerminal(node, grammar, "EMPTY");
	}

	public Expression toANY(AST node) {
		return Factory.newNonTerminal(node, grammar, "ANY");
	}

	public Expression toOneMore(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		l.add(toExpression(node.get(0)));
		l.add(Factory.newRepetition(node, toExpression(node.get(0))));
		return Factory.newSequence(node, l);
	}

	public Expression toZeroMore(AST node) {
		return Factory.newRepetition(node, toExpression(node.get(0)));
	}

	public Expression toChoice(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (AST subnode : node) {
			Factory.addChoice(l, toExpression(subnode));
		}
		return Factory.newChoice(node, l);
	}

	public Expression toSeq(AST node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for (AST subnode : node) {
			Factory.addSequence(l, toExpression(subnode));
		}
		return Factory.newSequence(node, l);
	}
}
