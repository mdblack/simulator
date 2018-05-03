package simulator;

public interface MemoryDevice {
    public byte getByte(int address);

    public void setByte(int address, byte value);

    public short getWord(int address);

    public int getDoubleWord(int address);

    public long getQuadWord(int address);

    public void setWord(int address, short value);

    public void setDoubleWord(int address, int value);

    public void setQuadWord(int address, long value);
}
