/*
Clock.java
Michael Black, 6/10

Simulates the system clock.
*/
package simulator;
public class Clock
{
	public static final long INSTRUCTIONS_PER_SECOND=40000000;

	private static final int MAX_DEVICES=100;

	long ticks;

	ClockedDevice[] devices = new ClockedDevice[MAX_DEVICES];
	int validDevices=0;

	public void Clock()
	{
		ticks=0;
	}

	//called on each instruction
	public void cycle()
	{
		ticks++;
		for (int i=0; i<validDevices; i++)
			devices[i].onClockTick(ticks);
	}

	public void registerDevice(ClockedDevice device)
	{
		devices[validDevices++]=device;
	}

	public long getTime()
	{
		return ticks;
	}

	public long getTickRate()
	{
		return INSTRUCTIONS_PER_SECOND;
	}

	public static interface ClockedDevice
	{
		public void onClockTick(long ticks);
	}
}
