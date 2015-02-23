package nez;

import nez.ast.SourcePosition;
import nez.expr.Expression;
import nez.expr.Rule;
import nez.util.UList;
import nez.util.UMap;

public class Grammar {
	String               resourceName;
	String               ns;
	UMap<Rule>           ruleMap;
	UList<String>        nameList;

	public Grammar(String name) {
		this.resourceName = name;
		if(name != null) {
			int loc = name.lastIndexOf('/');
			if(loc != -1) {
				name = name.substring(loc+1);
			}
			this.ns = name.replace(".nez", "");
		}
		this.ruleMap = new UMap<Rule>();
		this.nameList = new UList<String>(new String[8]);
	}

	public String getResourceName() {
		return this.resourceName;
	}
	
	public String uniqueName(String rulename) {
		return this.ns + ":" + rulename;
	}
	
	public final Rule newRule(String name, Expression e) {
		Rule r = new Rule(null, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}

	public final Rule defineRule(SourcePosition s, String name, Expression e) {
		if(!hasRule(name)) {
			nameList.add(name);
		}
		Rule r = new Rule(s, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}
	
//	public int getRuleSize() {
//		return this.ruleMap.size();
//	}

	public final boolean hasRule(String ruleName) {
		return this.ruleMap.get(ruleName) != null;
	}

	public final Rule getRule(String ruleName) {
		return this.ruleMap.get(ruleName);
	}

	public final UList<Rule> getDefinedRuleList() {
		UList<Rule> ruleList = new UList<Rule>(new Rule[this.nameList.size()]);
		for(String n : nameList) {
			ruleList.add(this.getRule(n));
		}
		return ruleList;
	}

	public final UList<Rule> getRuleList() {
		UList<Rule> ruleList = new UList<Rule>(new Rule[this.ruleMap.size()]);
		for(String n : this.ruleMap.keys()) {
			ruleList.add(this.getRule(n));
		}
		return ruleList;
	}

	public final Production getProduction(String name, int option) {
		Rule r = this.getRule(name);
		if(r != null) {
			return new Production(r, option);
		}
		return null;
	}

	public final Production getProduction(String name) {
		Rule r = this.getRule(name);
		if(r != null) {
			return new Production(r, Production.DefaultOption);
		}
		return null;
	}

	public void dump() {
		for(Rule r : this.getRuleList()) {
			System.out.println(r);
		}
	}
		

}
