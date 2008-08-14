package edu.tum.cs.logic;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import edu.tum.cs.tools.StringTool;

public class Disjunction extends ComplexFormula {

	public Disjunction(Collection<Formula> children) {
		super(children);
	}
	
	public Disjunction(Formula ... children) {
		super(children);
	}
	
	public String toString() {
		return "(" + StringTool.join(" v ", children) + ")";
	}
	
	@Override
	public boolean isTrue(IPossibleWorld w) {
		for(Formula child : children)
			if(child.isTrue(w))
				return true;
		return false;
	}

	@Override
	public Formula toCNF() {
		Set<Formula> clause = new HashSet<Formula>();
		Collection<Conjunction> conjunctions = new Vector<Conjunction>();
		for(Formula child : children) {
			child = child.toCNF();
			if(child instanceof Conjunction) {
				conjunctions.add((Conjunction)child);
			}
			else if(child instanceof Disjunction) {
				clause.addAll(Arrays.asList(((Disjunction)child).children));
			}
			else if(child instanceof TrueFalse) {
				if(((TrueFalse)child).isTrue())
					return child;
			}
			else {
				clause.add(child);
			}
		}
		if(conjunctions.isEmpty())			
			return clause.size() == 1 ? clause.iterator().next() : new Disjunction(clause);
		else {
	        // apply distributivity
	        // use the first conjunction to distribute: (C_1 ^ ... ^ C_n) v RD = (C_1 v RD) ^ ... ^  (C_n v RD)
			Iterator<Conjunction> i = conjunctions.iterator();			
			Formula[] conjuncts = conjunctions.iterator().next().children;
			while(i.hasNext())
				clause.add(i.next());
			Formula RD = new Disjunction(clause);
			Vector<Formula> elems = new Vector<Formula>();
			for(Formula Ci : conjuncts)
				elems.add(new Disjunction(Ci, RD));
			return new Conjunction(elems).toCNF();
		}
	}	
}
