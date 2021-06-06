package com.c256.foenix;

import java.io.File;

import com.dst.util.file.DRFileUtil;

public class WDCMapFile
{
	private String wdcMapFile = null;
	private String[] lines = null;
	
	public WDCMapFile(String tassLabelFile)
	{
		this.wdcMapFile = tassLabelFile;
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
			String[] parts = line.replace("\t","").split(" ");
			
			
			if(parts!=null && parts.length > 1)
			{
				for(String p : parts)
				{
					System.out.println("\t" +  p);
				}
				
				
				String entryName = parts[1].trim();
				
				if(entryName.startsWith(label))
				{	
					entryAddress = parts[0].trim().toUpperCase();
					System.out.println("LABEL:[" + entryName + "] @ ADDRESS: " +  entryAddress);		
					break;
				}
			}
		}
		
		return entryAddress;
	}


	private synchronized String[] getLines()
	{
		if(lines == null)
		{
			if(wdcMapFile!=null)
			{
				lines = DRFileUtil.readFileLines(new File(wdcMapFile));
			}
		}
		return lines;
	}
	
	
}
