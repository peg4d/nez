package nez.vm;



public class Exit extends Instruction {
	boolean status;
	public Exit(boolean status) {
		super(null, null, null);
		this.status = status;
	}
	@Override
	protected void stringfy(StringBuilder sb) {
		sb.append("exit");
	}
	@Override
	Instruction exec(Context sc) throws TerminationException {
		throw new TerminationException(status);
	}

}
