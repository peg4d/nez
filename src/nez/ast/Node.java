package nez.ast;


public interface Node {
	public Node newNode(Source source, long pos);
	public Tag  getTag();
	public void setTag(Tag tag);
	public void setValue(Object value);
	public void setEndingPosition(long pos);
	public void expandAstToSize(int newSize);
	public void commitChild(int index, Node child);
	public Node commit();
}
