package com.c256.foenix;

import java.util.List;

public class MemoryBlock
{
	private String baseAddress;
	private String address;
	private byte[] bytes;

	public byte[] getBuffer()
	{
		return bytes;
	}
	
	public int getFullAddress()
	{
		return Integer.parseUnsignedInt(getFullAddressAsString(),16);
	}
	
	public static class MemoryRecord
	{
        public String mark 		= null;
        public String reclen 	= null;
        public String offset 	= null;
        public String rectype 	= null;
        public String data 		= null;
        public String checksum 	= null;

        private MemoryRecord(String hexLine)
        {
            mark 	 = hexLine.substring(0, 1);
            reclen 	 = hexLine.substring(1, 3);
            offset 	 = hexLine.substring(3, 7);
            rectype  = hexLine.substring(7, 9);
            data 	 = hexLine.substring(9, hexLine.length() - 2);
            checksum = hexLine.substring(hexLine.length() - 2);
        }
        
        static MemoryRecord readHexLine(String hexLine)
        {
        	return new MemoryRecord(hexLine);
        }
	}
	
	public MemoryBlock(byte[] buffer,String baseAddress, String address)
	{
		this.bytes = buffer;
		this.baseAddress = baseAddress;
		this.address = address;
	}
	
	public MemoryBlock(List<Integer> memorySeg, String hexLine)
	{
		if(hexLine.startsWith(":"))
		{
            String mark 	= hexLine.substring(0, 1);
            String reclen 	= hexLine.substring(1, 3);
            String offset 	= hexLine.substring(3, 7);
            String rectype 	= hexLine.substring(7, 9);
            String data 	= hexLine.substring(9, hexLine.length() - 2);
            String checksum = hexLine.substring(hexLine.length() - 2);
			
            if("04".equals(rectype))
            {
            	baseAddress = null;//(Integer.parseInt(data, 16) & 0xFF);
            }
            else 
            {
                int bp = 0;
                bytes = new byte[data.length() / 2];
                
				for(int i=0;i<data.length();i+=2)
				{
					String hbyte = data.substring(i,i+2);
					bytes[bp++] = (byte)(Integer.parseInt(hbyte, 16) & 0xFF);
					memorySeg.add(Integer.parseInt(hbyte, 16) & 0xFF);
					//System.out.print(hbyte + " ");
				}
				//System.out.println();
            }				
		}
	}

	public MemoryBlock(MemoryRecord segment,MemoryRecord mr)
	{
		if(segment!=null && mr!=null)
		{
			int len = Integer.parseUnsignedInt(mr.reclen, 16);
			
           	baseAddress = segment.data;
           	address = mr.offset;
           	
            int bp = 0;
            bytes = new byte[mr.data.length() / 2];
            
            //System.out.println("LINE[" + baseAddress + "]:" + mr.data);
            //System.out.print("\tBYTES:");
            
            //System.out.print("Read 0x" + baseAddress + mr.offset + " Size:" + bytes.length + " ");
            
			for(int i=0;i<mr.data.length();i+=2)
			{
				String hbyte = mr.data.substring(i,i+2);
				//System.out.print(hbyte + " ");
				bytes[bp++] = (byte)(Integer.parseInt(hbyte, 16) & 0xFF);
			}
			
			//System.out.println();
		}
	}

	public String getFullAddressAsString()
	{
		return baseAddress + address;
	}


}
