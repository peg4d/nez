package nez.ast;


public interface Node {
	public Node newNode(Tag tag, Source source, long spos, long epos, int objectSize);
	public void setValue(Object value);
	public void link(int index, Node child);
	public Node commit();

	public Tag  getTag();
	public void setTag(Tag tag);
	public void setEndingPosition(long pos);
	public void expandAstToSize(int newSize);	
}
