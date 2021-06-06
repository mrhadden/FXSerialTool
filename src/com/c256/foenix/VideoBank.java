package com.c256.foenix;

public class VideoBank
{

	public static void main(String[] args)
	{
		long videoBankSize[] = {32767,65535,65535,65535,65535,32767}; 
		long videoBankOffset[] = new long[videoBankSize.length];
		
		long offsetEnd = 0L;
		
		for(int v = 0;v<videoBankSize.length;v++)
		{
			long firstBankAddress = 0;
			long lastBankAddress  = videoBankSize[v];
			long firstTranslatedAddress = offsetEnd + 1;
			
			videoBankOffset[v] = offsetEnd;
			
			
			offsetEnd += (videoBankSize[v] );
			long beginOffset = (offsetEnd - videoBankSize[v]);
			//pixelLocation = (( (y) * 640) + (x) );
			
			
			
			System.out.println("Bank " + v + ":");
			System.out.println("=====================");
			System.out.println("Size:" + videoBankSize[v]);
			
			
			System.out.println("First Address:" + firstBankAddress);
			System.out.println("Last  Address:" + lastBankAddress);
			System.out.println("FirstTranslated Address:" + firstTranslatedAddress);
			
			
			
			System.out.println("Begin:" + beginOffset);
			System.out.println("End:" + offsetEnd);
			
			System.out.println();			
		}
		
		for(int v = 0;v<videoBankSize.length;v++)
		{
			System.out.println("Bank " + v + ":" + videoBankSize[v] + " " + videoBankOffset[v]);
		}
		
		long x = 128;
		long y = 51;
		long pixelOffset = (( (y) * 640) + (x) );
		
		System.out.println("(" + x + "," + y + ") @ " + pixelOffset + " in " + getBank(pixelOffset) );

		x = 127;
		y = 51;
		pixelOffset = (( (y) * 640) + (x) );
		
		System.out.println("(" + x + "," + y + ") @ " + pixelOffset + " in " + getBank(pixelOffset) );
	
		
		
		/*
		for(int y = 0;y<480;y++)
		{
			for(int x = 0;x<639;x++)
			{
				long pixelLocation = (( (y) * 640) + (x) );
				
				long mod = pixelLocation % 32767;
				
				//System.out.println("(" + x + "," + y + "): " + mod);
				
				
				if(pixelLocation % 32767 == 0)
				{
					System.out.println("(" + x + "," + y + "): " + pixelLocation);
				}
				
			}
		}
		*/
		int lastBank = 0;
		for(y = 0;y<480;y++)
		{
			for(x = 0;x<639;x++)
			{
				
				pixelOffset = (( (y) * 640) + (x) );
				
				int bank =  getBank(pixelOffset);
				
				if(bank>lastBank)
				{
					lastBank = bank;
					System.out.println("Bank Switch y = " + y + " x = " + x + " @ " + pixelOffset + " in " + getBank(pixelOffset) );
				}
								
				
			}
		}
		
		
	}
	
	public static int getBank(long address)
	{
		if(address >  294907)
			return 5;
		if(address >  229372)
			return 4;
		if(address >  163837)
			return 3;
		if(address >  98302)
			return 2;
		if(address >  32767)
			return 1;
		
		return 0;
	}
	

}
