/*
 * Created on Jan 20, 2011
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.tum.cs.bayesnets.inference;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.tum.cs.bayesnets.conversion.BNDB2Inst;
import edu.tum.cs.bayesnets.core.BNDatabase;
import edu.tum.cs.bayesnets.core.BeliefNetworkEx;
import edu.tum.cs.util.FileUtil;
import edu.tum.cs.util.Stopwatch;

/**
 * a simple wrapper for the ACE2.0 inference engine, which uses arithmetic circuits
 * @author jain
 */
public class ACE extends Sampler {
	protected File acePath = null;
	public ACE(BeliefNetworkEx bn) throws Exception {	
		super(bn);
		paramHandler.add("acePath", "setAcePath");
	}
	
	public void setAcePath(String path) throws Exception {
		this.acePath = new File(path);
		if(!acePath.exists() || !acePath.isDirectory())
			throw new Exception("The given path " + path + " does not exist or is not a directory");
	}
	
	@Override
	protected SampledDistribution _infer() throws Exception {
		if(acePath == null) 
			throw new Exception("No ACE 2.0 path was given. This inference method requires ACE2.0 and the location at which it is installed must be configured");
		
		// save belief network as .xbif
		File bnFile = new File("ace.tmp.xbif");
		this.bn.save(bnFile.getPath());
		
		// compile arithmetic circuit using ace compiler
		if(verbose) System.out.println("compiling arithmetic circuit...");
		Process p = Runtime.getRuntime().exec(acePath + File.separator + "compile " + bnFile.getName());
		p.waitFor();
		
		// write evidence to .inst file
		File instFile = new File("ace.tmp.inst");
		BNDB2Inst.convert(new BNDatabase(this.bn, this.evidenceDomainIndices), instFile);
		
		// run ace inference
		if(verbose) System.out.println("evaluating...");
		p = Runtime.getRuntime().exec(acePath + File.separator + "evaluate " + bnFile.getName() + " " + instFile.getName());
		p.waitFor();
		File marginalsFile = new File(bnFile.getName() + ".marginals");
		
		// create output distribution
		if(verbose) System.out.println("reading results...");
		this.createDistribution();
		String results = FileUtil.readTextFile(marginalsFile);
		String patFloat = "(?:\\d+(\\.\\d+)?(?:E[-\\d]+)?)";
		// * get probability of the evidence
		Pattern probEvid = Pattern.compile(String.format("p \\(e\\) = (%s)", patFloat));
		Matcher m = probEvid.matcher(results);
		if(!m.find())
			throw new Exception("Could not find 'p (e)' in results");
		if(m.group(1).equals("0E0"))
			throw new Exception("The probability of the evidence is 0");
		dist.Z = Double.parseDouble(m.group(1));
		System.out.printf("probability of the evidence: %f\n", dist.Z);
		// * get posteriors
		Pattern patMarginal = Pattern.compile(String.format("p \\((.*?) \\| e\\) = \\[(%s(?:, %s)+)\\]", patFloat, patFloat)); 
		m = patMarginal.matcher(results);
		int cnt = 0;
		while(m.find()) {
			String varName = m.group(1);			
			String[] v = m.group(2).split(", ");
			int nodeIdx = this.getNodeIndex(bn.getNode(varName));
			if(v.length != dist.values[nodeIdx].length)
				throw new Exception("Marginal vector length for '" + varName + "' incorrect");
			for(int i = 0; i < v.length; i++)
				dist.values[nodeIdx][i] = Double.parseDouble(v[i]);
			cnt++;
		}		
		System.out.println(cnt + " marginals read");
		
		// clean up
		new File(bnFile.getName() + ".ac").delete();
		new File(bnFile.getName() + ".lmap").delete();
		bnFile.delete();
		instFile.delete();
		marginalsFile.delete();
		
		return dist;
	}

}