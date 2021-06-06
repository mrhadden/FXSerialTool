package com.c256.foenix;

public class SerialSettings
{

	private int baudRate = 6000000;
	private HandShake handshake = HandShake.None;// System.IO.Ports.Handshake.None;
	private int parity = 0;
	private int dataBits = 8;
	private int stopBits = 1;
	private int readTimeout = 2000;
	private int writeTimeout = 2000;
	
	public enum HandShake
	{
		None(com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DISABLED),

		RequestToSend(com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_RTS_ENABLED
				| com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_CTS_ENABLED),

		XOnXOff(com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED
				| com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED);

		private int setting;

		public int getSetting()
		{
			return this.setting;
		}

		private HandShake(int setting)
		{
			this.setting = setting;
		}
	}

	public int getBaudRate()
	{
		return baudRate;
	}

	public void setBaudRate(int baudRate)
	{
		this.baudRate = baudRate;
	}

	public HandShake getHandshake()
	{
		return handshake;
	}

	public void setHandshake(HandShake handshake)
	{
		this.handshake = handshake;
	}

	public int getParity()
	{
		return parity;
	}

	public void setParity(int parity)
	{
		this.parity = parity;
	}

	public int getDataBits()
	{
		return dataBits;
	}

	public void setDataBits(int dataBits)
	{
		this.dataBits = dataBits;
	}

	public int getStopBits()
	{
		return stopBits;
	}

	public void setStopBits(int stopBits)
	{
		this.stopBits = stopBits;
	}

	public int getReadTimeout()
	{
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout)
	{
		this.readTimeout = readTimeout;
	}

	public int getWriteTimeout()
	{
		return writeTimeout;
	}

	public void setWriteTimeout(int writeTimeout)
	{
		this.writeTimeout = writeTimeout;
	}

}
