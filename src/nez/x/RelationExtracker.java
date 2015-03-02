package nez.x;

import java.util.AbstractList;

import nez.ast.Node;
import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.main.Recorder;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.UMap;

public class RelationExtracker {
	public final static int MaximumColumns = 16;
	UList<Schema> schemaList;
	UList<String> workingValueList;

	RelationExtracker() {
		this.schemaList = new UList<Schema>(new Schema[4]);
		this.workingValueList = new UList<String>(new String[MaximumColumns]);
	}
	
	public Node newNode() {
		return new RNode(this);
	}

	void recieve(RNode t) {
		Schema r = extractSchema(t);
		if(r != null) {
			matchSchema(t, r);
		}
	}
	
	Schema extractSchema(RNode t) {
		if(t.size() < 2) {
			return null;
		}
		Schema extra = new Schema();
		this.workingValueList.clear(0);
		extra.extractImpl(t, this.workingValueList);
		if(extra.size() < 3 || extra.size() == MaximumColumns - 1) {
			return null;
		}
		//System.out.println("extracted: " + extra.nameList + "\n" + t);
		return extra;
	}
	
	void matchSchema(RNode t, Schema extracted) {
		if(extracted.count > extracted.size() / 2) {
			return;
		}
		for(Schema schema: schemaList) {
			if(schema.size() == extracted.size()) {
				double sim = schema.sim(extracted);
				//System.out.println("Sim: " +sim + "\n\t" + schema + "\n\t" + extracted);
				if(sim > 0.99999) {
					schema.addRelationalData(t, this.workingValueList);
					return;
				}
			}
		}
		System.out.println("new schema extracted: dup=" + extracted.count + "," + extracted.size() + " " + extracted);
		extracted.count = 0;
		this.schemaList.add(extracted);
		extracted.addRelationalData(t, workingValueList);
		System.out.println("schema size=" + schemaList.size());
	}
	
	
	Schema findView() {
		Schema view = null;
		for(Schema schema: schemaList) {
			if(schema.view != null) {
				continue;
			}
			if(view == null) {
				view = schema;
				continue;
			}
			if(schema.contains(view)) {
				view = schema;
				continue;
			}
		}
		if(view == null) {
			return null;
		}
		for(Schema schema: schemaList) {
			if(schema.view != null) {
				continue;
			}
			if(schema == view || view.contains(schema)) {
				schema.view = view;
				continue;
			}
		}
		return view;
	}
		
	void dump() {
		Schema view = findView();
		while(view != null) {
			System.out.println("View: " + view);
			for(Schema schema: schemaList) {
				if(schema.view == view) {
					while(schema.firstData != null) {
						String[] d = schema.formatEach(view);
						System.out.println(formatCSV(d));
					}
				}
			}
			System.out.println("");
			view = findView();
		}
	}
	
	public static String formatCSV(String[] rel) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < rel.length; i++) {
			if(i > 0) {
				sb.append(",");
			}
			sb.append(formatCSVValue(rel[i]));
		}
		return sb.toString();
	}
	
	public static String formatCSVValue(String v) {
		if(v == null) {
			return "";
		}
		if(isNumber(v)) {
			return v;
		}
		return StringUtils.quoteString('"', v, '"');
	}

	public final static boolean isNumber(String v) {
		int s = 0;
		int dot = 0;
		if(v.length() > 0 && v.charAt(0) == '-') {
			s = 1;
		}
		for(int i = s; i < v.length(); i++) {
			char c = v.charAt(i);
			if(c == '.') {
				if(dot > 0) return false;
				dot++;
			}
			if(!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	public void record(Recorder rec) {
		// TODO Auto-generated method stub
	}
}

class Schema {
	UMap<Integer> names;
	UList<String> nameList;
	RelationalData firstData;
	RelationalData lastData;
	int count = 0;
	Schema view = null;
	
	Schema() {
		names = new UMap<Integer>();
		nameList = new UList<String>(new String[4]);
		clearData();
	}

	void clearData() {
		firstData = null;
		lastData = null;
		count = 0;
	}
	
	int size() {
		return this.nameList.size();
	}
	
	boolean contains(String name) {
		return this.names.hasKey(name);
	}

	boolean contains(Schema r) {
		if(this.size() > r.size()) {
			for(String n : r.nameList) {
				if(!this.contains(n)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	
	@Override
	public String toString() {
		return this.nameList.toString();
	}
	

	double sim(Schema r) {
		int interSection = 0;
		for(String n : this.nameList) {
			if(r.contains(n)) {
				interSection += 1;
			}
		}
		return (double)interSection / (this.size() + r.size() - interSection);
	}
	
	Schema union(Schema s, Schema s2) {
		int n1 = s.size(), n2 = s2.size();
		if(n1 < n2) {
			return union(s2, s); // the first one is larger
		}
		int m = n1 * n2;
		Schema u = new Schema();
		for(int i = 0; i < m; i++) {
			/* The following is to merge two name lists in preserving
			 * partial orders ..
			 */
			if(i % n1 == 0) {
				add(s.nameList.ArrayValues[i/n1], null, null);
			}
			if(i % n2 == 0) {
				add(s2.nameList.ArrayValues[i/n2], null, null);
			}
		}
		return u;
	}

	void extract(RNode t, UList<String> wlist) {
		this.extractImpl(t, wlist);
	}
	
	void extractImpl(RNode t, UList<String> wlist) {
		if(t.size() == 2) {
			if(t.get(0).size() == 0 && t.get(1).size() == 0) {
				add(t.get(0).getText(), t.get(1).getText(), wlist);
				return;
			}
		}
		if(t.size() == 0) {
			add(t.getTag().getName(), t.getText(), wlist);
		}
		for(RNode sub: t) {
			if(this.nameList.size() < RelationExtracker.MaximumColumns) {
				extractImpl(sub, wlist);
			}
		}
	}
	
	private void add(String key, String value, UList<String> wlist) {
		Integer n = this.names.get(key);
		if(n != null) {
			n = n + 1;
			this.names.put(key, n);
			key = key+'#'+n;
			this.count++;
		}
		else {
			n = 1;
			this.names.put(key, n);
		}
		this.nameList.add(key);
		if(wlist != null) {
			wlist.add(value);
		}
	}
	
	void addRelationalData(RNode t, UList<String> workingStringList) {
		RelationalData d = new RelationalData(t, workingStringList);
		if(firstData == null) {
			firstData = d;
			lastData = d;
		}
		else {
			lastData.next = d;
			lastData = d;
		}
		count++;
	}

	int indexOf(String name) {
		for(int i = 0; i < this.nameList.size(); i++) {
			if(name.equals(this.nameList.ArrayValues[i])) {
				return i;
			}
		}
		return -1;
	}
	
	String[] formatEach() {
		String[] rel = firstData.columns;
		firstData = firstData.next;
		return rel;
	}

	String[] formatEach(Schema view) {
		String[] rel = new String[view.size()+1];
		rel[0] = String.valueOf(firstData.pos);
		int column = 1;
		for(String n : view.nameList) {
			int index = indexOf(n);
			if(index != -1) {
				rel[column] = firstData.columns[index];
			}
			column ++;
		}
		firstData = firstData.next;
		return rel;
	}

}

class RelationalData {
	long pos;
	String[] columns;
	RelationalData next;
	RelationalData(RNode t, UList<String> l) {
		this.pos = t.getSourcePosition();
		this.columns = new String[l.size()];
		System.arraycopy(l.ArrayValues, 0, this.columns, 0, this.columns.length);
	}
}

class RNode extends AbstractList<RNode> implements Node, SourcePosition {
	RelationExtracker  tracker;
	private Source    source;
	private Tag       tag;
	private long      pos;
	private int       length;
	private Object    value  = null;
	RNode               parent = null;
	private RNode       subTree[] = null;

	public RNode(RelationExtracker tracker) {
		this.tracker    = tracker;
		this.tag        = Tag.tag("Text");
		this.source     = null;
		this.pos        = 0;
		this.length     = 0;
	}

	private RNode(RelationExtracker tracker, Tag tag, Source source, long pos, long epos, int size) {
		this.tracker = tracker;
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = (int)(epos - pos);
		if(size > 0) {
			this.subTree = new RNode[size];
		}
	}

	@Override
	public Node commit() {
		tracker.recieve(this);
		return this;
	}

	@Override
	public Node newNode(Tag tag, Source source, long spos, long epos, int size) {
		return new RNode(this.tracker, tag == null ? this.tag : tag, source, spos, epos, size);
	}

	@Override
	public void setValue(Object value) {
		this.value = value;
	}
	

	@Override
	public void link(int index, Node child) {
		this.set(index, (RNode)child);
	}


	@Override
	public Tag getTag() {
		return this.tag;
	}

	@Override
	public void setTag(Tag tag) {
		this.tag = tag;
	}

	@Override
	public void setEndingPosition(long pos) {
		this.length = (int)(pos - this.getSourcePosition());
	}

	@Override
	public final void expandAstToSize(int newSize) {
		if(newSize > this.size()) {
			this.resizeAst(newSize);
		}
	}

	public final RNode getParent() {
		return this.parent;
	}

	public Source getSource() {
		return this.source;
	}

	public long getSourcePosition() {
		return this.pos;
	}

	@Override
	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatPositionLine(type, this.getSourcePosition(), msg);
	}

	public int getLength() {
		return this.length;
	}
	
	public final boolean is(Tag t) {
		return this.tag == t;
	}
	
	
	public final boolean isEmptyToken() {
		return this.length == 0;
	}
	
	public final String getText() {
		if(this.value != null) {
			return this.value.toString();
		}
		if(this.source != null) {
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.getLength());
			return this.value.toString();
		}
		return "";
	}

	// subTree[]
	
	@Override
	public final int size() {
		if(this.subTree == null) {
			return 0;
		}
		return this.subTree.length;
	}

	@Override
	public final RNode get(int index) {
		return this.subTree[index];
	}

	public final RNode get(int index, RNode defaultValue) {
		if(index < this.size()) {
			return this.subTree[index];
		}
		return defaultValue;
	}

	@Override
	public final RNode set(int index, RNode node) {
		RNode oldValue = null;
		if(!(index < this.size())){
			this.expandAstToSize(index+1);
		}
		oldValue = this.subTree[index];
		this.subTree[index] = node;
		node.parent = this;
		return oldValue;
	}

	private void resizeAst(int size) {
		if(this.subTree == null && size > 0) {
			this.subTree = new RNode[size];
		}
		else if(size == 0){
			this.subTree = null;
		}
		else if(this.subTree.length != size) {
			RNode[] newast = new RNode[size];
			if(size > this.subTree.length) {
				System.arraycopy(this.subTree, 0, newast, 0, this.subTree.length);
			}
			else {
				System.arraycopy(this.subTree, 0, newast, 0, size);
			}
			this.subTree = newast;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	final void stringfy(String indent, StringBuilder sb) {
		sb.append("\n");
		sb.append(indent);
		sb.append("(#");
		sb.append(this.tag.name);
		if(this.subTree == null) {
			sb.append(" ");
			StringUtils.formatQuoteString(sb, '\'', this.getText(), '\'');
			sb.append(")");
		}
		else {
			String nindent = "   " + indent;
			for(int i = 0; i < this.size(); i++) {
				if(this.subTree[i] == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				}
				else {
					this.subTree[i].stringfy(nindent, sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append(")");
		}
	}
	
	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}
	
}

