package nez.vm;

import nez.expr.ByteMap;
import nez.util.StringUtils;

public class MatchByteMap extends Instruction {
	public final boolean[] byteMap;
	public MatchByteMap(Optimizer optimizer, ByteMap e, Instruction next) {
		super(optimizer, e, next);
		this.byteMap = e.charMap;
	}

	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMatchByteMap(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("mapmatch ");
		sb.append(StringUtils.stringfyByteMap(byteMap));
	}

}
