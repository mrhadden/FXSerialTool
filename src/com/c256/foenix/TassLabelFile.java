package com.c256.foenix;

import java.io.File;

import com.dst.util.file.DRFileUtil;

public class TassLabelFile
{
	private String tassLabelFile = null;
	private String[] lines = null;
	
	public TassLabelFile(String tassLabelFile)
	{
		this.tassLabelFile = tassLabelFile;
	}
	
	
	int toAddressInt(String label)
	{
		String[] lines = getLines();
		
		int entryAddress = Integer.parseInt(toAddressString(label),16);
		
		return entryAddress;
	}
	
	String toAddressString(String label)
	{
		String entryAddress = "0xFFFFFF";
		String[] lines = getLines();
		
		for(String line : lines)
		{
			System.out.println("Line:" +  line);
			String[] parts = line.split("\t");
			if(parts!=null && parts.length > 5)
			{
				for(String p : parts)
				{
					System.out.println("\t" +  p);
				}
				
				if(parts[5].startsWith(label))
				{	
					entryAddress = parts[0].toUpperCase().replace(".","").trim();
					System.out.println("LABEL:[" + parts[5] + "] @ ADDRESS: " +  entryAddress);					
				}
			}
		}
		
		return entryAddress;
	}


	private synchronized String[] getLines()
	{
		if(lines == null)
		{
			if(tassLabelFile!=null)
			{
				lines = DRFileUtil.readFileLines(new File(tassLabelFile));
			}
		}
		return lines;
	}
	
	
}
