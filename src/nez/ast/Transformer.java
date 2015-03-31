package nez.ast;

public interface Transformer {
//	public SyntaxTree newNode();
	public void transform(String path, SyntaxTree node);
}
