package nez.cc;

import nez.expr.And;
import nez.expr.AnyChar;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.Choice;
import nez.expr.Empty;
import nez.expr.Expression;
import nez.expr.ExpressionList;
import nez.expr.Failure;
import nez.expr.LeftNew;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NonTerminal;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.Tagging;
import nez.expr.Unary;
import nez.util.StringUtils;

import org.peg4d.Utils;

public class MouseGrammarGenerator extends GrammarGenerator {
	public MouseGrammarGenerator(String fileName) {
		super(fileName);
	}
	
	@Override
	public void visitRule(Rule rule) {
		Expression e = rule.getExpression();
		file.writeIndent(rule.getLocalName());
		file.incIndent();
		file.writeIndent("= ");
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					file.writeIndent("/ ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
		file.writeIndent(";");
		file.decIndent();
	}	
	
	public void visitEmpty(Empty e) {
		file.write("\"\"");
	}

	public void visitFailure(Failure e) {
		file.write("!\"\"/*failure*/");
	}

	public void visitNonTerminal(NonTerminal e) {
		file.write(e.getLocalName());
	}
	
	public void visitByteChar(ByteChar e) {
		file.write(StringUtils.stringfyByte(e.byteChar));
	}

	public void visitByteMap(ByteMap e) {
		file.write(StringUtils.stringfyCharClass(e.charMap));
	}
	
	public void visitAnyChar(AnyChar e) {
		file.write("_");
	}

	protected void visit(String prefix, Unary e, String suffix) {
		if(prefix != null) {
			file.write(prefix);
		}
		if(/*e.get(0) instanceof String ||*/ e.get(0) instanceof NonTerminal || e.get(0) instanceof New) {
			this.visit(e.get(0));
		}
		else {
			file.write("(");
			this.visit(e.get(0));
			file.write(")");
		}
		if(suffix != null) {
			file.write(suffix);
		}
	}

	public void visitOption(Option e) {
		this.visit( null, e, "?");
	}
	
	public void visitRepetition(Repetition e) {
		this.visit(null, e, "*");
	}
	
	public void visitAnd(And e) {
		this.visit( "&", e, null);
	}
	
	public void visitNot(Not e) {
		this.visit( "!", e, null);
	}

	public void visitTagging(Tagging e) {
		file.write("/* #");
		file.write(e.tag.toString());
		file.write("*/");
	}
	
	public void visitValue(Replace e) {
		//file.write(StringUtils.quoteString('`', e.value, '`'));
	}
	
	public void visitLink(Link e) {
//		String predicate = "@";
//		if(e.index != -1) {
//			predicate += "[" + e.index + "]";
//		}
//		this.visit(predicate, e, null);
		this.visit(e.get(0));
	}

	protected void visitSequenceImpl(ExpressionList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				file.write(" ");
			}
			int n = appendAsString(l, i);
			if(n > i) {
				i = n;
				continue;
			}
			Expression e = l.get(i);
			if(e instanceof Choice || e instanceof Sequence) {
				file.write("( ");
				visit(e);
				file.write(" )");
				continue;
			}
			visit(e);
		}
	}

	private int appendAsString(ExpressionList l, int start) {
		int end = l.size();
		String s = "";
		for(int i = start; i < end; i++) {
			Expression e = l.get(i);
			if(e instanceof ByteChar) {
				char c = (char)(((ByteChar) e).byteChar);
				if(c >= ' ' && c < 127) {
					s += c;
					continue;
				}
			}
			end = i;
			break;
		}
		if(s.length() > 1) {
			file.write(Utils.quoteString('"', s, '"'));
		}
		return end - 1;
	}
	
	public void visitSequence(Sequence e) {
		this.visitSequenceImpl(e);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				file.write(" / ");
			}
			visit(e.get(i));
		}
	}

	public void visitNew(New e) {
		file.write("( ");
		this.visitSequenceImpl(e);
		file.write(" )");
	}

	public void visitLeftNew(LeftNew e) {
		file.write("( ");
		this.visitSequenceImpl(e);
		file.write(" )");
	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("/* Mouse Unsupported <");
		file.write(e.getPredicate());
		for(Expression se : e) {
			file.write(" ");
			visit(se);
		}
		file.write("> */");
	}

}
