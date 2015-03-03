package nez.cc;

import nez.vm.Compiler;
import nez.vm.Instruction;

class JavaParserGenerator extends ParserGenerator {
	JavaParserGenerator(String fileName) {
		super(fileName);
	}
	void generate(Compiler cc) {
		file.writeIndent("public class NezParser {");
		file.incIndent();
		file.writeIndent("public final static boolean parse() {");
		file.incIndent();
		file.writeIndent("long pos = 0;");
		file.writeIndent("while(true) {");
		file.incIndent();
		file.writeIndent("switch(pc) {");
		file.incIndent();
		file.writeIndent("case 0:");
		file.incIndent();
		boolean nested = true;
		for(Instruction inst: cc.codeList) {
			if(inst.label) {
				if(nested) {
					file.decIndent();
				}
				file.writeIndent("case " + inst.id + ":");
				file.incIndent();
				nested = true;
			}
			visit(inst);
			if(inst.next != null && inst.next.id != inst.id+1) {
				file.writeIndent("pc = " + inst.next.id + "; break;");
				file.decIndent();
				nested = false;
			}
		}
		file.decIndent();
		file.writeIndent("}");
		file.decIndent();
		file.writeIndent("}");
		file.decIndent();
		file.writeIndent("}");
		file.decIndent();
		file.writeIndent("}");
		file.flush();
	}
	
	@Override
	public void visitUndefined(Instruction inst) {
		file.writeIndent("c.op" + inst.getClass().getSimpleName() + "(pc);");
	}
	
}