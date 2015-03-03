package nez.cc;

import nez.util.FileBuilder;
import nez.vm.Compiler;
import nez.vm.Instruction;

class JavaParserGenerator extends ParserGenerator {
	FileBuilder fb;
	JavaParserGenerator(String fileName) {
		this.fb = new FileBuilder(fileName);
	}
	void generate(Compiler cc) {
		fb.writeIndent("public class NezParser {");
		fb.incIndent();
		fb.writeIndent("public final static boolean parse() {");
		fb.incIndent();
		fb.writeIndent("long pos = 0;");
		fb.writeIndent("while(true) {");
		fb.incIndent();
		fb.writeIndent("switch(pc) {");
		fb.incIndent();
		fb.writeIndent("case 0:");
		fb.incIndent();
		boolean nested = true;
		for(Instruction inst: cc.codeList) {
			if(inst.label) {
				if(nested) {
					fb.decIndent();
				}
				fb.writeIndent("case " + inst.id + ":");
				fb.incIndent();
				nested = true;
			}
			visit(inst);
			if(inst.next != null && inst.next.id != inst.id+1) {
				fb.writeIndent("pc = " + inst.next.id + "; break;");
				fb.decIndent();
				nested = false;
			}
		}
		fb.decIndent();
		fb.writeIndent("}");
		fb.decIndent();
		fb.writeIndent("}");
		fb.decIndent();
		fb.writeIndent("}");
		fb.decIndent();
		fb.writeIndent("}");
		fb.flush();
	}
	
	@Override
	public void visitUndefined(Instruction inst) {
		fb.writeIndent("c.op" + inst.getClass().getSimpleName() + "(pc);");
	}
	
}