package com.c256.foenix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.c256.foenix.MemoryBlock.MemoryRecord;
import com.dst.util.file.DRFileUtil;
import com.fazecast.jSerialComm.SerialPort;

public class SerialTool
{
	SerialPort serial = null;

	public static final int	ADDR_NMI 		= 0xFFFA;//$FFFA/$FFFB
	public static final int	ADDR_RESET		= 0xFFFC;//$FFFC/$FFFD
	public static final int	ADDR_IRQBRK		= 0xFFFE;//$FFFE/$FFFF
	
	public static final String	SADDR_NMI 		= "FFFA";//$FFFA/$FFFB
	public static final String	SADDR_RESET		= "FFFC";//$FFFC/$FFFD
	public static final String	SADDR_IRQBRK	= "FFFE";//$FFFE/$FFFF
 
	
	public byte TxLRC = 0;
	public byte RxLRC = 0;
	public byte[] Stat0 = new byte[1];
	public byte[] Stat1 = new byte[1];
	public byte[] LRC = new byte[1];
	public String[] ports;

	public static void main(String[] args)
	{
		boolean bRead = true;

		
		CommandLineArgs arguments = new CommandLineArgs(args);
		
		String eaddress = null;
		
		SerialPort[] ports = SerialPort.getCommPorts();
		if (ports != null && ports.length > 0)
		{
			SerialPort fxdebug = null;

			if(arguments.isPortList)
			{
				for(SerialPort port : ports)
				{
					System.out.println("Port:" + port.getSystemPortName());

					System.out.println("\tDesc  :" + port.getDescriptivePortName() + "[" + port.getPortDescription() + "]");

					System.out.println("\tBaud  :" + port.getBaudRate());
					System.out.println("\tData  :" + port.getNumDataBits());
					System.out.println("\tStop  :" + port.getNumStopBits());
					System.out.println("\tParity:" + port.getParity());
					System.out.println("\tHS:" + port.getFlowControlSettings());
				}

				return;
			}
			
			if(arguments.comPortName!=null)
			{
				for(SerialPort port : ports)
				{
					if(arguments.comPortName.equals(port.getPortDescription()) ||  
					   arguments.comPortName.equals(port.getSystemPortName()))
					{
						fxdebug = port;
					}
				}
			}		
			
			if(arguments.listFile!=null && arguments.entryName!=null)
			{
				TassLabelFile tf = new TassLabelFile(arguments.listFile);
				
				eaddress = tf.toAddressString(arguments.entryName);
				int naddress = tf.toAddressInt(arguments.entryName);
				
				System.out.println("Entry " +  arguments.entryName + "  :" + eaddress + "[" + naddress + "]");
			}

			if(arguments.wdcMapFileName!=null && arguments.entryName!=null)
			{
				WDCMapFile tf = new WDCMapFile(arguments.wdcMapFileName);
				
				eaddress = tf.toAddressString(arguments.entryName);
				int naddress = tf.toAddressInt(arguments.entryName);
				
				System.out.println("Entry " +  arguments.entryName + "  :" + eaddress + "[" + naddress + "]");
			}
			
			if(fxdebug!=null)
			{
				SerialTool st = new SerialTool(fxdebug);
				if (st != null)
				{
					if (bRead)
					{
						byte[] buffer = null;

						if(arguments.s28FileName!=null)
						{
							File hex = new File(arguments.s28FileName);
							if(hex.exists())
							{
								if(arguments.kernelFile!=null)
								{
									try
									{
										File kf = new File(arguments.kernelFile);
										if(kf!=null && kf.exists())
										{
											
											File dir = null;
											
											if(kf.getParentFile()!=null)
											{
												dir = kf.getParentFile().getAbsoluteFile();
											}
											else
											{
												dir = new File(".");
											}
												
											File tempHex = File.createTempFile("kernel.", ".hex",dir);
											if(tempHex!=null)
											{
												tempHex.deleteOnExit();
												
												String[] hlines = DRFileUtil.readFileLines(hex);
												String[] klines = DRFileUtil.readFileLines(kf);
												
												FileOutputStream fout = new FileOutputStream(tempHex);
												if(fout!=null)
												{
													try
													{
														for(String line : klines)
														{
															if(!":00000001FF".equals(line))
															{
																fout.write(line.getBytes("utf-8"));
																fout.write("\r\n".getBytes("utf-8"));
															}
														}
														for(String line : hlines)
														{
															fout.write(line.getBytes("utf-8"));
															fout.write("\r\n".getBytes("utf-8"));
														}
													}
													finally
													{
														fout.close();
													}
												}
												
												arguments.s28FileName = tempHex.getAbsolutePath();
											}
										}
									}
									catch (IOException e)
									{
										e.printStackTrace();
									}
								}							
							}
							
							try
							{
								Map<String, MemoryBlock> blocks = st.importBlocks(arguments.s28FileName);
								if(blocks!=null)
								{
									if(st.beginTransfer())
									{
										for(MemoryBlock mb : blocks.values())
										{
											st.transferData(mb.getBuffer(), mb.getFullAddress());
											if(arguments.dump)
												dumpBuffer(mb.getBuffer());
										}

										MemoryBlock rvb = st.resetVector(eaddress);
										if(rvb!=null)
										{
											st.transferData(rvb.getBuffer(), rvb.getFullAddress());
										}
										
										st.endTransfer();
									}
								}
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
						else if(arguments.binaryFile!=null)
						{
							try
							{
								buffer = st.importBinaryData(arguments.binaryFile);
								if(buffer!=null)
								{
									st.sendData(buffer, arguments.startAddress, buffer.length,eaddress);

									if(arguments.dump)
										dumpBuffer(buffer);
								}
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
						else if(arguments.outputfileName!=null && arguments.startAddress!=null)
						{
							if(arguments.dataLength == null )
							{
								if(arguments.kilobytes!=null)
								{
									arguments.dataLength = "0x" + Integer.toHexString(Integer.parseInt(arguments.kilobytes,10) * 1024);
								}
							}
							
							buffer = st.fetchData(arguments.startAddress, arguments.dataLength);
							if(buffer!=null && buffer.length > 0)
							{
								try
								{
									st.exportData(arguments.outputfileName,buffer);
								}
								catch (IOException e)
								{
									e.printStackTrace();
								}

								if(arguments.dump)
									dumpBuffer(buffer);
							}
						}
						else if(arguments.fileName!=null)
						{
							try
							{
								buffer = st.importData(arguments.fileName);
								if(buffer!=null)
								{
									st.sendData(buffer, arguments.startAddress, buffer.length,eaddress);

									if(arguments.dump)
										dumpBuffer(buffer);
								}
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	private MemoryBlock resetVector(String jmpAddress)
	{
		MemoryBlock b = null;
		
		if(jmpAddress!=null)
		{
			//PreparePacket2Write(toByteOrder(jmpAddress), ADDR_RESET, 0, 2);
			b = new MemoryBlock(toByteOrder(jmpAddress),"0000",SADDR_RESET);
		}
		return b;
	}

	private static void dumpBuffer(byte[] buffer)
	{
		int perLine = 16;
		
		if(buffer!=null)
		{
			for(byte b : buffer)
			{
				String byteOut = Integer.toHexString(Byte.toUnsignedInt(b));
				if (byteOut.length() < 2)
					System.out.print("0");
	
				System.out.print(byteOut.toUpperCase());
				System.out.print(" ");
	
				perLine--;
	
				if (perLine < 0)
				{
					perLine = 16;
					System.out.println();
				}
			}
		}
	}

	public SerialTool(SerialPort selectedPort)
	{
		this.serial = selectedPort;

		SerialSettings ss = new SerialSettings();

		serial.setBaudRate(ss.getBaudRate());
		serial.setNumDataBits(ss.getDataBits());
		serial.setNumStopBits(ss.getStopBits());
		serial.setFlowControl(ss.getHandshake().getSetting());
		serial.setParity(ss.getParity());

		serial.setComPortTimeouts(2000, 2000, 2000);
		serial.setParity(ss.getParity());
	}

	public void GetFnxInDebugMode()
	{
		byte[] commandBuffer = new byte[8];
		commandBuffer[0] = 0x55; // Header
		commandBuffer[1] = (byte) 0x80; // GetFNXinDebugMode
		commandBuffer[2] = 0x00;
		commandBuffer[3] = 0x00;
		commandBuffer[4] = 0x00;
		commandBuffer[5] = 0x00;
		commandBuffer[6] = 0x00;
		commandBuffer[7] = (byte) 0xD5;
		SendMessage(commandBuffer, null);
	}

	public void ExitFnxDebugMode()
	{
		byte[] commandBuffer = new byte[8];
		commandBuffer[0] = 0x55; // Header
		commandBuffer[1] = (byte) 0x81; // ExitFNXinDebugMode
		commandBuffer[2] = 0x00;
		commandBuffer[3] = 0x00;
		commandBuffer[4] = 0x00;
		commandBuffer[5] = 0x00;
		commandBuffer[6] = 0x00;
		commandBuffer[7] = (byte) 0xD4;
		SendMessage(commandBuffer, null);
	}

	public byte[] fetchDebug(int startAddress, int size)
	{		
		byte[] buffer = null;
		
		try
		{
			if(serial.isOpen())
			{
				buffer = new byte[size];
				
				try
				{
					if(size < 2048)
					{
						PreparePacket2Read(buffer, startAddress, 0, size);
					}
					else
					{
						int BufferSize = 2048;
						int Loop = size / BufferSize;

						for (int j = 0; j < Loop; j++)
						{
							PreparePacket2Read(buffer, startAddress, j * BufferSize, BufferSize);
							startAddress += BufferSize;
						}
						
						BufferSize = (size % BufferSize);
						if (BufferSize > 0)
						{
							PreparePacket2Read(buffer, startAddress, size - BufferSize, BufferSize);
						}
					}
				}
				catch (Exception e)
				{
					buffer = null;
					e.printStackTrace();
				}
				
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return buffer;
	}
	
	
	public void sendDebug(byte[] buffer, int startAddress, int size)
	{
		try
		{
			if (serial.isOpen())
			{
				if(size <= 2048)
				{
					PreparePacket2Write(buffer, startAddress, 0, size);
				}
				else
				{
					int BufferSize = 2048;
					int Loop = size / BufferSize;
					int offset = startAddress;
					for (int j = 0; j < Loop; j++)
					{
						PreparePacket2Write(buffer, offset, j * BufferSize, BufferSize);
						offset = offset + BufferSize; 
					}
					
					BufferSize = (size % BufferSize);
					if (BufferSize > 0)
					{
						PreparePacket2Write(buffer, offset, size - BufferSize, BufferSize);
					}
				}
			}

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
	public void sendData(byte[] buffer, String startAddress, String size)
	{
		sendData(buffer,convertAddress(startAddress), convertAddress(size),null);
	}	

	public void sendData(byte[] buffer, String startAddress, int size)
	{
		sendData(buffer,convertAddress(startAddress), size,null);
	}
	
	public void sendData(byte[] buffer, String startAddress, int size,String jmpAddress)
	{
		sendData(buffer,convertAddress(startAddress), size,jmpAddress);
	}
	
	public void sendData(byte[] buffer, int startAddress, int size,String jmpAddress)
	{
		try
		{
			boolean openedPort = serial.openPort();
			
			if (serial.isOpen())
			{
				// Get into Debug mode (Reset the CPU and keep it in that state
				// and Gavin will take control of the bus)
				GetFnxInDebugMode();
				// Now's let's transfer the code
				if (size <= 2048)
				{
					// DataBuffer = The buffer where the loaded Binary File
					// resides
					// FnxAddressPtr = Pointer where to put the Data in the Fnx
					// i = Pointer Inside the data buffer
					// Size_Of_File = Size of the Payload we want to transfer
					// which ought to be smaller than 8192
					PreparePacket2Write(buffer, startAddress, 0, size);
					// UploadProgressBar.Increment(size);
				}
				else
				{
					int BufferSize = 2048;
					int Loop = size / BufferSize;
					int offset = startAddress;
					for (int j = 0; j < Loop; j++)
					{
						PreparePacket2Write(buffer, offset, j * BufferSize, BufferSize);
						offset = offset + BufferSize; // Advance the Pointer to
														// the next location
														// where to write Data
														// in the Foenix
						// UploadProgressBar.Increment(BufferSize);
					}
					BufferSize = (size % BufferSize);
					if (BufferSize > 0)
					{
						PreparePacket2Write(buffer, offset, size - BufferSize, BufferSize);
						// UploadProgressBar.Increment(BufferSize);
					}
				}

				/*
				 * 
				// Update the Reset Vectors from the Binary Files Considering
				// that the Files Keeps the Vector @ $00:FF00
				if (startAddress < 0xFF00 && (startAddress + buffer.length) > 0xFFFF
						|| (startAddress == 0x18_0000 && buffer.length > 0xFFFF))
				{
					PreparePacket2Write(buffer, 0x00FF00, 0x00FF00, 256);
				}
				*/

				if(jmpAddress!=null)
				{
					PreparePacket2Write(toByteOrder(jmpAddress), ADDR_RESET, 0, 2);
				}
				
				Thread.sleep(2000);
				// The Loading of the File is Done, Reset the FNX and Get out of
				// Debug Mode
				ExitFnxDebugMode();

				System.out.println("Transfer Done! System Reset!\n" + "Send Binary Success");
			}

		}
		catch (Exception ex)
		{
			ExitFnxDebugMode();
			System.out.println(ex.getMessage() + "Send Binary Error");
		}
	}

	private boolean waitPort(int retry)
	{
		if(retry < 5)
			retry = 5;
		
		if(!serial.isOpen())
		{
			for(int i=retry;i<0;i++)
			{
				System.out.println("Retrying port...");
				serial.openPort();
				if(!serial.isOpen())
					break;
			}
		}		
		
		return serial.isOpen();
	}
	
	public boolean beginTransfer()
	{
		boolean openedPort = false;
		
		try
		{
			int retry = 10;
			//waitPort(5);
			
			do
			{
				System.out.println("Trying to start debug...");
				waitPort(5);
				openedPort = serial.openPort();
				retry--;
			}
			while(!openedPort && retry > 0);
			
			if (serial.isOpen())
			{
				System.out.println("Started C256 debug mode...");
				GetFnxInDebugMode();
			}
			else
			{
				System.out.println("\nPort is not available.");
			}
		}
		catch (Exception ex)
		{
			ExitFnxDebugMode();
			System.out.println(ex.getMessage() + "Send Binary Error");
		}
		
		return openedPort;
	}	
	
	public void endTransfer()
	{
		try
		{
			if(serial.isOpen())
			{

				ExitFnxDebugMode();
			}
			else
			{
				System.out.println("\nPort is not available.");
			}
		}
		catch (Exception ex)
		{
			ExitFnxDebugMode();
			System.out.println(ex.getMessage() + "Send Binary Error");
		}
	}	
	
	public void transferData(byte[] buffer, int startAddress)
	{
		try
		{
			int size = buffer.length;
			
			if(serial.isOpen())
			{
				if (size <= 2048)
				{
					PreparePacket2Write(buffer, startAddress, 0, size);
				}
				else
				{
					int BufferSize = 2048;
					int Loop = size / BufferSize;
					int offset = startAddress;
					for (int j = 0; j < Loop; j++)
					{
						PreparePacket2Write(buffer, offset, j * BufferSize, BufferSize);
						offset = offset + BufferSize;
					}
					BufferSize = (size % BufferSize);
					if (BufferSize > 0)
					{
						PreparePacket2Write(buffer, offset, size - BufferSize, BufferSize);
					}
				}
			}
			else
			{
				System.out.println("\nPort is not available.");
			}

		}
		catch (Exception ex)
		{
			System.out.println(ex.getMessage());
		}
	}
	
	
	private byte[] toByteOrder(String jmpAddress)
	{
		byte[] byteAddress = new byte[2];
		
		if(jmpAddress!=null && jmpAddress.length() > 4)
		{
			jmpAddress = jmpAddress.substring(jmpAddress.length() - 4);
		}
		
		if(jmpAddress!=null && jmpAddress.length() == 4)
		{
			String bb = jmpAddress.substring(2);
			int val = (Integer.parseInt(bb,16) & 0xFF);
			byteAddress[0] = (byte) (val);
			bb = jmpAddress.substring(0,2);
			val = (Integer.parseInt(bb,16) & 0xFF);
			byteAddress[1] = (byte)(val & 0xFF);
		}
		return byteAddress;
	}

	public byte[] fetchData(String startAddress, String size)
	{
		return fetchData(convertAddress(startAddress), convertAddress(size));
	}
	
	public byte[] fetchData(int startAddress, int size)
	{
		
		byte[] buffer = null;
		
		//boolean success = false;
		try
		{
			boolean openedPort = serial.openPort();

			//System.out.println("PORT STATUS:" + openedPort);

			if (serial.isOpen())
			{
				buffer = new byte[size];
				
				try
				{
					GetFnxInDebugMode();
					if (size < 2048)
					{
						PreparePacket2Read(buffer, startAddress, 0, size);
						System.out.println("Fetch: 0x" + Integer.toHexString(startAddress).toUpperCase() + " Size:" + size + " ");
						// UploadProgressBar.Increment(size);
					}
					else
					{
						int BufferSize = 2048;
						int Loop = size / BufferSize;

						for (int j = 0; j < Loop; j++)
						{
							System.out.println("Fetch: 0x" + Integer.toHexString(startAddress).toUpperCase() + " Size:" + BufferSize + " ");
							PreparePacket2Read(buffer, startAddress, j * BufferSize, BufferSize);
							startAddress += BufferSize; // Advance the Pointer
														// to the next location
														// where to write Data
														// in the Foenix
							// UploadProgressBar.Increment(BufferSize);
							
						}
						BufferSize = (size % BufferSize);
						if (BufferSize > 0)
						{
							PreparePacket2Read(buffer, startAddress, size - BufferSize, BufferSize);
							// UploadProgressBar.Increment(BufferSize);
						}
					}

					ExitFnxDebugMode();					
				}
				catch (Exception e)
				{
					buffer = null;
					serial.closePort();
					e.printStackTrace();
				}
			}
		}
		catch (Exception ex)
		{
			buffer = null;
			ExitFnxDebugMode();
			//System.out.println(ex.getMessage() + "Fetch Data Error");
		}
		return buffer;
	}

	public void PreparePacket2Write(byte[] buffer, int FNXMemPointer, int FilePointer, int Size)
	{
		//System.out.print("Write: 0x" + Integer.toHexString(FNXMemPointer).toUpperCase() + " Size:" + Size + " ");		
		//dumpData(buffer);
		
		if("true".equalsIgnoreCase(System.getProperty("verbose","false")))
			System.out.println("Write: 0x" + Integer.toHexString(FNXMemPointer).toUpperCase() + " Size:" + Size + " ");

		// Maximum transmission size is 8192
		if (Size > 8192)
			Size = 8192;

		byte[] commandBuffer = new byte[8 + Size];
		commandBuffer[0] = 0x55; // Header
		commandBuffer[1] = 0x01; // Write 2 Memory
		commandBuffer[2] = (byte) ((FNXMemPointer >> 16) & 0xFF); // (H)24Bit
																	// Addy -
																	// Where to
																	// Store the
																	// Data
		commandBuffer[3] = (byte) ((FNXMemPointer >> 8) & 0xFF); // (M)24Bit
																	// Addy -
																	// Where to
																	// Store the
																	// Data
		commandBuffer[4] = (byte) (FNXMemPointer & 0xFF); // (L)24Bit Addy -
															// Where to Store
															// the Data
		commandBuffer[5] = (byte) ((Size >> 8) & 0xFF); // (H)16Bit Size - How
														// many bytes to Store
														// (Max 8Kbytes 4 Now)
		commandBuffer[6] = (byte) (Size & 0xFF); // (L)16Bit Size - How many
													// bytes to Store (Max
													// 8Kbytes 4 Now)
		System.arraycopy(buffer, FilePointer, commandBuffer, 7, Size);

		TxProcessLRC(commandBuffer);
		//System.out.println("Transmit Data LRC:" + TxLRC);
		// commandBuffer[Size + 7] = TxLRC;

		SendMessage(commandBuffer, null); // Tx the requested Payload Size (Plus
											// Header and LRC), No Payload to be
											// received aside of the Status.
	}

	private void dumpData(byte[] buffer)
	{
		if(buffer!=null)
		{
			for(int i=0;i<buffer.length;i++)
			{
				String bbyte = Integer.toHexString(buffer[i] & 0xFF).toUpperCase();
				if(bbyte.length() < 2)
					System.out.print("0");
				
				System.out.print(bbyte + " ");
			}
			System.out.println();
		}
	}

	public void PreparePacket2Read(byte[] receiveBuffer, int address, int offset, int size)
	{
		if (size > 0)
		{
			byte[] commandBuffer = new byte[8];
			commandBuffer[0] = 0x55; // Header
			commandBuffer[1] = 0x00; // Command READ Memory
			commandBuffer[2] = (byte) (address >> 16); // Address Hi
			commandBuffer[3] = (byte) (address >> 8); // Address Med
			commandBuffer[4] = (byte) (address & 0xFF); // Address Lo
			commandBuffer[5] = (byte) (size >> 8); // Size HI
			commandBuffer[6] = (byte) (size & 0xFF); // Size LO
			commandBuffer[7] = 0x5A;

			byte[] partialBuffer = new byte[size];
			SendMessage(commandBuffer, partialBuffer);
			System.arraycopy(partialBuffer, 0, receiveBuffer, offset, size);
		}
	}

	public void SendMessage(byte[] command, byte[] data)
	{
		// int dwStartTime = System.Environment.TickCount;
		int i;
		byte[] byte_buffer = new byte[1];

		int wroteBytes = serial.writeBytes(command, command.length, 0);
		//System.out.println("wroteBytes:" + wroteBytes);

		Stat0[0] = 0;
		Stat1[0] = 0;
		LRC[0] = 0;

		int readBytes = 0;

		do
		{
			byte_buffer[0] = (byte) 0;
			readBytes = serial.readBytes(byte_buffer, 1);
			/*
			if (byte_buffer[0] != 0)
			{
				System.out.println("VALUE:" + Integer.toHexString(byte_buffer[0] & 0x000000FF));
			}
			*/
		}
		while (byte_buffer[0] != (byte) 0xAA);

		//System.out.println("readBytes:" + readBytes);

		if (byte_buffer[0] == (byte) 0xAA)
		{
			byte[] bb = new byte[1];

			serial.readBytes(Stat0, 1);
			serial.readBytes(Stat1, 1);
			if (data != null)
			{
				for (i = 0; i < data.length; i++)
				{
					serial.readBytes(bb, 1);
					data[i] = bb[0];
				}
			}
			serial.readBytes(LRC, 1);
		}

		RxLRC = (byte) RxProcessLRC(data);
		//System.out.println("Receive Data LRC:" + RxLRC);
	}

	public int TxProcessLRC(byte[] buffer)
	{
		int i;
		TxLRC = 0;
		for (i = 0; i < buffer.length; i++)
			TxLRC = (byte) (TxLRC ^ buffer[i]);
		return TxLRC;
	}

	public int RxProcessLRC(byte[] data)
	{
		int i;
		RxLRC = (byte) 0xAA;
		RxLRC = (byte) (RxLRC ^ Stat0[0]);
		RxLRC = (byte) (RxLRC ^ Stat1[0]);
		if (data != null)
		{
			for (i = 0; i < data.length; i++)
				RxLRC = (byte) (RxLRC ^ data[i]);
		}
		RxLRC = (byte) (RxLRC ^ LRC[0]);
		return RxLRC;
	}

	public void exportData(String filename, byte[] data) throws IOException
	{
		ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filename));
		if(outputStream!=null)
		{
			try
			{
				try
				{
					outputStream.writeObject(data);
				}
				catch(Exception e)
				{
					
				}
			}
			finally
			{
				if(outputStream!=null)
					outputStream.close();
			}
		}
	}	

	public byte[] importData(String filename) throws IOException
	{
		byte[] buffer = null;
		
		try
		{
			ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filename));
			if(inputStream!=null)
			{
				try
				{
					try
					{
						buffer = (byte[]) inputStream.readObject();
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				finally
				{
					if(inputStream!=null)
						inputStream.close();
				}
			}
		}
		catch (StreamCorruptedException e)
		{
			//buffer = loadHexFile(filename);
			Map<String, MemoryBlock> blocks = loadHexFileEx(filename);
			//e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return buffer;
	}	

	public Map<String, MemoryBlock> importBlocks(String filename) throws IOException
	{
		Map<String, MemoryBlock> buffer = null;
		
		try
		{
			buffer = loadHexFileEx(filename);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return buffer;
	}	
	
	public byte[] importBinaryData(String filename) throws IOException
	{
		byte[] buffer = null;
		
		try
		{
			File fileIn =  new File(filename);
			
			if(fileIn!=null && fileIn.exists())
			{
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				if(bout!=null)
				{
					int read = DRFileUtil.copyFileToStream(fileIn,bout);
					if(read > 0)
					{
						buffer = bout.toByteArray();
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return buffer;
	}	
	
	private byte[] loadHexFile(String filename)
	{
		byte[] buffer = null;
		
		File hexFile = new File(filename);
		if(hexFile.exists())
		{
			List<Integer> memorySeg = new Vector<Integer>();
			
			String[] contents = DRFileUtil.readFileLines(hexFile);
			for(String line : contents)
			{
				//System.out.println(line);
				MemoryBlock mb = new MemoryBlock(memorySeg,line);
				//System.out.println("Read:" + mb.bytes.length);
			}
			
			int offset = 0;
			
			buffer = new byte[memorySeg.size()];
			
			for(int b : memorySeg)
			{
				buffer[offset++] = (byte)(b & 0xFF);
			}
		}
		
		return buffer;
	}

	private Map<String,MemoryBlock> loadHexFileEx(String filename)
	{
		Map<String,MemoryBlock> blocks = new HashMap<String,MemoryBlock>();
		
		byte[] buffer = null;
		
		File hexFile = new File(filename);
		if(hexFile.exists())
		{
			MemoryRecord currentSegment = null;
			List<Integer> memorySeg = new Vector<Integer>();
			
			String[] contents = DRFileUtil.readFileLines(hexFile);
			for(String hexLine : contents)
			{
				MemoryRecord mr = MemoryBlock.MemoryRecord.readHexLine(hexLine);
				if("04".equals(mr.rectype))
				{
					currentSegment = mr;
					continue;
				}
				else if("01".equals(mr.rectype))
				{
					continue;
				}
				else if("05".equals(mr.rectype))
				{
					continue;
				}
				else
				{
					MemoryBlock mb = new MemoryBlock(currentSegment,mr);
					blocks.put(mb.getFullAddressAsString(), mb);
				}
				//System.out.println(line);
				//MemoryBlock mb = new MemoryBlock(memorySeg,line);
				//System.out.println("Read:" + mb.bytes.length);
			}
			
		}
		
		return blocks;
	}	
	
	private int convertAddress(String addressString)
	{
		int address = 0;
		int radix = 10;
		
		if(addressString!=null)
		{
			addressString = addressString.toLowerCase();
			
			if(addressString.startsWith("0x"))
				radix = 16;
			
			address = Integer.parseInt(addressString.replace("0x", ""), radix);
		}
		
		return address;
	}

	
	public static class CommandLineArgs
	{
		public boolean isPortList    = false;
		public String fileName       = null;
		public String startAddress   = null;
		public String dataLength     = null;
		public String outputfileName = null;
		public String comPortName 	 = null;
		public boolean isArgError	 = false;
		public boolean dump			 = false;
		public boolean help			 = false;	
		public String hexFile		 = null;
		private String binaryFile = null;
		private String kernelFile = null;
		private String listFile = null;
		private String entryName = null;
		private String wdcMapFileName;
		private String s28FileName;
		private String kilobytes;				
		
		public CommandLineArgs(String[] args)
		{
			int len = 0;
			
			try
			{
				do
				{
					String curArg = args[len];
					
					if("--ports".equals(curArg))
					{
						isPortList = true;
					}
					else if("--help".equals(curArg))
					{
						help = true;
					}
					else if("--dump".equals(curArg))
					{
						dump = true;
					}
					else if("-f".equals(curArg))
					{
						len++;
						fileName = args[len];					
					}
					else if("-o".equals(curArg))
					{
						len++;
						outputfileName = args[len];					
					}
					else if("-a".equals(curArg))
					{
						len++;
						startAddress = args[len];					
					}
					else if("-s".equals(curArg))
					{
						len++;
						dataLength = args[len];					
					}
					else if("-p".equals(curArg))
					{
						len++;
						comPortName  = args[len];					
					} 
					else if("-d".equals(curArg))
					{
						len++;
						comPortName  = args[len];					
					} 
					else if("-h".equals(curArg))
					{
						len++;
						hexFile  = args[len];					
					} 
					else if("-k".equals(curArg))
					{
						len++;
						kernelFile  = args[len];					
					} 
					else if("-kb".equals(curArg))
					{
						len++;
						kilobytes  = args[len];					
					} 
					else if("-b".equals(curArg))
					{
						len++;
						binaryFile  = args[len];					
					} 	
					else if("-l".equals(curArg))
					{
						len++;
						listFile  = args[len];					
					} 	
					else if("-e".equals(curArg))
					{
						len++;
						entryName  = args[len];					
					} 					
					else if("-m".equals(curArg))
					{
						len++;
						wdcMapFileName  = args[len];					
					}
					else if("-s28".equals(curArg))
					{
						len++;
						s28FileName  = args[len];					
					}
					else if("-hex".equals(curArg))
					{
						len++;
						s28FileName  = args[len];					
					}
					
					
					
					len++;
				}
				while(len < args.length);
			}
			catch (Exception e)
			{
				isArgError = true;
			}
		}
		
	}	
}
