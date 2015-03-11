package nez.expr;

import nez.Production;
import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.runtime.Compiler;
import nez.runtime.Instruction;
import nez.util.FlagUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class ByteMap extends Terminal {
	public boolean[] byteMap; // Immutable
	ByteMap(SourcePosition s, int beginChar, int endChar) {
		super(s);
		this.byteMap = newMap();
		appendRange(this.byteMap, beginChar, endChar);
	}
	ByteMap(SourcePosition s, boolean[] b) {
		super(s);
		this.byteMap = b;
	}
	
	public final static boolean[] newMap() {
		return new boolean[257];
	}

	public final static void clear(boolean[] byteMap) {
		for(int c = 0; c < byteMap.length; c++) {
			byteMap[c] = false;
		}
	}

	public final static void appendRange(boolean[] b, int beginChar, int endChar) {
		for(int c = beginChar; c <= endChar; c++) {
			b[c] = true;
		}
	}

	public final static void appendBitMap(boolean[] dst, boolean[] src) {
		for(int i = 0; i < 256; i++) {
			if(src[i]) {
				dst[i] = true;
			}
		}
	}

	public final static void reverse(boolean[] byteMap, int option) {
		for(int i = 0; i < 256; i++) {
			byteMap[i] = !byteMap[i];
		}
		if(!FlagUtils.is(option, Production.Binary)) {
			byteMap[0] = false;
		}
	}
	

	@Override
	public String getPredicate() {
		return "byte " + StringUtils.stringfyByteMap(this.byteMap);
	}
	@Override
	public String getInterningKey() { 
		return "[" +  StringUtils.stringfyByteMap(this.byteMap);
	}
	
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return (byteMap[ch]) ? Accept : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		if(this.byteMap[context.byteAt(context.getPosition())]) {
			context.consume(1);
			return true;
		}
		return context.failure2(this);
	}
	@Override
	public Instruction encode(Compiler bc, Instruction next) {
		return bc.encodeByteMap(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		int c = 0;
		for(boolean b: this.byteMap) {
			if(b) {
				c += 1;
			}
		}
		return c;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		int c = 0;
		for(int ch = 0; ch < 127; ch++) {
			if(this.byteMap[ch]) {
				c += 1;
			}
			if(c == p) {
				sb.append((char)ch);
			}
		}
	}

}
