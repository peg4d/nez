package nez.expr;

import nez.SourceContext;



public interface Recognizer {
	public boolean match(SourceContext context);
}
