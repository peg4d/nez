package nez.ast;


public interface Node {
	public Node newNode(Tag tag, Source s, long spos, long epos, int size);
	public void link(int index, Node child);
	public Node commit(Object value);
	public void abort();

	public Tag  getTag();
	public void setTag(Tag tag);
	public void setEndingPosition(long pos);
	public void expandAstToSize(int newSize);	
}
