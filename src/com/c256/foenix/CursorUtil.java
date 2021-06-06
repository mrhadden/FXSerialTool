package com.c256.foenix;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.dst.util.network.RESTUtil;
import com.dst.util.string.DRStringUtil;

public class CursorUtil
{

	public static void main(String[] args)
	{
		try
		{
			if(args.length > 0)
			{
				File cursorJsonFile = new File(args[0]);
				if(cursorJsonFile.exists())
				{
					String cursorSource = DRStringUtil.readFileToString(cursorJsonFile);
					if(cursorSource!=null)
					{
						//System.out.println(cursorSource);
						
						List<Map<String, Object>> list = RESTUtil.jsonArrayToMapList("[" + cursorSource + "]");						
						if(list!=null && list.size() > 0)
						{
							Map<String,Object> imageData = list.get(0);
							if(imageData!=null)
							{
								//System.out.println("Name:" + (String)imageData.get("name"));
								
								List frameList = (List<String>)imageData.get("frames");
								
								//System.out.println("Swatch:" + (List)imageData.get("swatch"));
								//System.out.println("frames:" +  frameList);
								
								for(Object frames : frameList)
								{
									//System.out.println("\tframe:" +  frames);
									if(frames!=null)
									{
										int pxcount = 0;
										
										List<String> data = (List<String>)frames;
										for(String pixel : data)
										{
											if("ffffff".equalsIgnoreCase(pixel))
												System.out.print("X ");
											else if("000000".equalsIgnoreCase(pixel))
												System.out.print("O ");
											else if("4e4e4e".equalsIgnoreCase(pixel))
												System.out.print("# ");
											else if("585858".equalsIgnoreCase(pixel))
												System.out.print("# ");
											else
												System.out.print( "  ");
											
											pxcount++;
											if(pxcount == 16)
											{
												pxcount = 0;
												System.out.println();
											}										
										}
										
										for(String pixel : data)
										{
											if("ffffff".equalsIgnoreCase(pixel))
												System.out.print("0xFF,");
											else if("000000".equalsIgnoreCase(pixel))
												System.out.print("0x01,");
											else if("4e4e4e".equalsIgnoreCase(pixel))
												System.out.print("0x55,");
											else if("585858".equalsIgnoreCase(pixel))
												System.out.print("0x55,");

											else
												System.out.print("0x00,");
											
											pxcount++;
											if(pxcount == 16)
											{
												pxcount = 0;
												System.out.println();
											}										
										}
										
										
									}
								}
								
							}
						}
					}					
				}		
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
