/*
IODevice.java
Michael Black, 6/10

Template of a device attached to an IO port
*/

package simulator;

public abstract class IODevice
{
	public abstract byte ioPortReadByte(int address);
	public abstract void ioPortWriteByte(int address, byte value);
        public short ioPortReadWord(int address)
        {
                return (short)((ioPortReadByte(address)&0xff) | ((ioPortReadByte(address+1)<<8)&0xff00));
        }
        public int ioPortReadLong(int address)
        {
                return (ioPortReadWord(address)&0xffff) | ((ioPortReadWord(address+2)<<16)&0xffff0000);
        }
        public void ioPortWriteWord(int address, short data)
        {
                ioPortWriteByte(address, (byte)(data&0xff));
                ioPortWriteByte(address+1, (byte)((data>>8)&0xff));
        }
        public void ioPortWriteLong(int address, int data)
        {
                ioPortWriteWord(address, (short)(data&0xffff));
                ioPortWriteWord(address+2, (short)((data>>16)&0xffff));
        }
}
