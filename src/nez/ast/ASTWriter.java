package nez.ast;


import nez.util.FileBuilder;
import nez.util.StringUtils;

public class ASTWriter {
	public final void startWriter(String file, AST node) {
		this.startWriter(new FileBuilder(file), node);
	}	
	public void startWriter(FileBuilder fb, AST node) {
		this.writeAST(fb, node);
		fb.writeNewLine();
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
}
