package nez.ast;

import java.util.TreeMap;

import nez.util.FileBuilder;

public class ASTStatWriter extends ASTWriter {
	@Override
	public void startWriter(FileBuilder fb, AST po) {
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
