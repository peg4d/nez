package nez.ast;


import java.util.TreeMap;

import nez.util.FileBuilder;
import nez.util.StringUtils;

public class ASTWriter implements Transformer {
	@Override
	public Node newNode() {
		return new AST();
	}
	@Override
	public void transform(String path, Node node) {
		FileBuilder fb = new FileBuilder(path);
		this.writeAST(fb, (AST)node);
		fb.writeNewLine();
		fb.flush();
	}
	private void writeAST(FileBuilder fb, AST node) {
		if(node == null) {
			fb.writeIndent("null");
			return;
		}
		fb.writeIndent("(#" + node.getTag().toString()); 
		if(node.size() == 0) {
			fb.write(" "); 
			fb.write(StringUtils.quoteString('\'', node.getText(), '\''));
			fb.write(")");
		}
		else {
			fb.incIndent();
			for(int i = 0; i < node.size(); i++) {
				this.writeAST(fb, node.get(i));
			}
			fb.decIndent();
			fb.writeIndent(")"); 
		}
	}
	
	public void writeTag(FileBuilder fb, AST po) {
		TreeMap<String,Integer> m = new TreeMap<String,Integer>();
		this.tagCount(po, m);
		for(String k : m.keySet()) {
			fb.write("#" + k + ":" + m.get(k));
		}
		fb.writeNewLine();
	}

	private void tagCount(AST po, TreeMap<String,Integer> m) {
		for(int i = 0; i < po.size(); i++) {
			tagCount(po.get(i), m);
		}
		String key = po.getTag().toString();
		Integer n = m.get(key);
		if(n == null) {
			m.put(key, 1);
		}
		else {
			m.put(key, n+1);
		}
	}

}
