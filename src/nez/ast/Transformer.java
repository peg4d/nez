package nez.ast;

public interface Transformer {
	public Node newNode();
	public void transform(String path, Node node);
}
