package nez.vm;

import nez.expr.ByteChar;
import nez.util.StringUtils;

public class MatchByte extends Instruction {
	public final int byteChar;
	public MatchByte(Compiler optimizer, ByteChar e, Instruction next) {
		super(optimizer, e, next);
		this.byteChar = e.byteChar;
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		return sc.opMatchByte(this);
	}

	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("bytematch ");
		sb.append(StringUtils.stringfyByte(byteChar));
	}

}
