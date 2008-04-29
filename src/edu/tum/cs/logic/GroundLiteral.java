package edu.tum.cs.logic;

public class GroundLiteral extends Formula {
	protected boolean isPositive;
	protected GroundAtom gndAtom;
	
	public GroundLiteral(boolean isPositive, GroundAtom gndAtom) {
		this.gndAtom = gndAtom;
		this.isPositive = isPositive;
	}
	
	@Override
	public boolean isTrue(PossibleWorld w) {
		boolean v = w.isTrue(gndAtom);
		return isPositive ? v : !v;
	}
}
