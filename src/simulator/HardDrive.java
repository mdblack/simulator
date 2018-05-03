package simulator;

import java.util.Scanner;

public class HardDrive extends IODevice {
    public static final int ERR_STAT = 0x01;
    public static final int INDEX_STAT = 0x02;
    public static final int ECC_STAT = 0x04;
    public static final int DRQ_STAT = 0x08;
    public static final int SEEK_STAT = 0x10;
    public static final int SRV_STAT = 0x10;
    public static final int WRERR_STAT = 0x20;
    public static final int READY_STAT = 0x40;
    public static final int BUSY_STAT = 0x80;
    public static final int IDE_CMD_RESET = 0x04;
    public static final int IDE_CMD_DISABLE_IRQ = 0x02;
    public static final int ETF_TRANSFER_STOP = 0x01;
    public static final int ETF_SECTOR_WRITE = 0x02;
    public static final int ETF_SECTOR_READ = 0x03;
    public static final int ETF_DUMMY_TRANSFER_STOP = 0x06;
    public static final int ETF_ATAPI_COMMAND = 4;
    public static final int ETF_ATAPI_COMMAND_REPLY_END = 5;
    public static final int WIN_IDENTIFY = 0xec;
    public static final int WIN_ATAPI_IDENTIFY = 0xa1;
    public static final int WIN_PACKETCMD = 0xa0;
    public static final int WIN_READ = 0x20;
    public static final int WIN_READ_ONCE = 0x21;
    public static final int WIN_WRITE = 0x30;
    public static final int WIN_WRITE_ONCE = 0x31;
    public static final int WIN_DIAGNOSE = 0x90;
    public static final int WIN_SRST = 8;
    public static final int CD_MODE_SENSE_10 = 0x5a;
    public static final int CD_READ_10 = 0x28;
    public static final int ATAPI_PACKET_SIZE = 12;
    public static final int ATAPI_INT_REASON_IO = 2;
    public static final int ATAPI_INT_REASON_CD = 1;

    public Drive[] drive;
    private int currentDrive = -1;

    private static final int ioBase1 = 0x1f0;
    private static final int ioBase2 = 0x3f6;
    private static final int irq = 14;

    private Computer computer;

    public String saveState() {
        String state = "";
        state += currentDrive + ":";
        if (drive[0] == null)
            state += "0:";
        else
            state += "1:" + drive[0].saveState() + ":";
        if (drive[1] == null)
            state += "0:";
        else
            state += "1:" + drive[1].saveState() + ":";
        return state;
    }

    public void loadState(String state) {
        int s = 0;
        String[] states = state.split(":");
        currentDrive = Integer.parseInt(states[s++]);
        if (states[s++].equals("1"))
            drive[0].loadState(states[s++]);
        if (states[s++].equals("1"))
            drive[1].loadState(states[s++]);
    }

    public HardDrive(Computer computer) {
        this.computer = computer;

        drive = new Drive[2];
//		drive[0]=new Drive(0,new Disk("harddisk.img"),306,4,17);

        if (computer.bootgui.diskIncluded[2]) {
//			if (computer.applet!=null)
//				drive[0]=new Drive(0,new Disk(computer.getClass().getResource(computer.bootgui.diskImage[2])),computer.bootgui.cylinders[2],computer.bootgui.heads[2],computer.bootgui.sectors[2],computer.bootgui.isCD[2]);
//			else
            drive[0] = new Drive(0, new Disk(computer.bootgui.diskImage[2]), computer.bootgui.cylinders[2], computer.bootgui.heads[2], computer.bootgui.sectors[2], computer.bootgui.isCD[2]);
        }

        if (computer.bootgui.diskIncluded[3]) {
//			if (computer.applet!=null)
//				drive[1]=new Drive(1,new Disk(computer.getClass().getResource(computer.bootgui.diskImage[3])),computer.bootgui.cylinders[3],computer.bootgui.heads[3],computer.bootgui.sectors[3],computer.bootgui.isCD[3]);
//			else
            drive[1] = new Drive(1, new Disk(computer.bootgui.diskImage[3]), computer.bootgui.cylinders[3], computer.bootgui.heads[3], computer.bootgui.sectors[3], computer.bootgui.isCD[3]);
        }


        currentDrive = 0;

        computer.ioports.requestPorts(this, getPorts(), "IDE Controller", new String[]{"", "Error code", "Number of sectors", "Sector", "Cylinder (low)", "Cylinder (high)", "Drive Select", "Status", "Command"});
    }

    public int[] getPorts() {
        return new int[]{ioBase1 + 0, ioBase1 + 1, ioBase1 + 2, ioBase1 + 3, ioBase1 + 4, ioBase1 + 5, ioBase1 + 6, ioBase1 + 7, ioBase2};
    }

    public void ioPortWriteByte(int address, byte data) {
        if (address >= ioBase2)
            writeCommand(data);
        else
            writeIDE(address, data);
    }

    public void ioPortWriteWord(int address, short data) {
        switch (address - ioBase1) {
            case 0:
            case 1:
                writeDataWord(data);
                break;
            default:
                ioPortWriteByte(address, (byte) data);
                ioPortWriteByte(address + 1, (byte) (data >>> 8));
        }
    }

    public void ioPortWriteLong(int address, int data) {
        switch (address - ioBase1) {
            case 0:
            case 1:
            case 2:
            case 3:
                writeDataLong(data);
                break;
            default:
                ioPortWriteWord(address, (short) data);
                ioPortWriteWord(address + 2, (short) (data >>> 16));
        }
    }

    public byte ioPortReadByte(int address) {
        if (address >= ioBase2)
            return drive[currentDrive].status;
        else
            return (byte) readIDE(address);
    }

    public short ioPortReadWord(int address) {
        switch (address - ioBase1) {
            case 0:
            case 1:
                short word = (short) readDataWord();
                return word;
            default:
                return (short) ((0xff & ioPortReadByte(address)) | (0xff00 & (ioPortReadByte(address + 1) << 8)));
        }
    }

    public int ioPortReadLong(int address) {
        switch (address - ioBase1) {
            case 0:
            case 1:
            case 2:
            case 3:
                int word = readDataLong();
                return word;
            default:
                return (0xffff & ioPortReadWord(address)) | (0xffff0000 & (ioPortReadWord(address + 2) << 16));
        }
    }

    private void writeCommand(int data) {
        if (drive[0] == null) return;
        if (((drive[0].command & IDE_CMD_RESET) == 0) && ((data & IDE_CMD_RESET) != 0)) {
            drive[0].status = (byte) (BUSY_STAT | SEEK_STAT);
            drive[0].error = 1;
            if (drive[1] != null) drive[1].status = (byte) (BUSY_STAT | SEEK_STAT);
            if (drive[1] != null) drive[1].error = 1;
        } else if (((drive[0].command & IDE_CMD_RESET) != 0) && ((data & IDE_CMD_RESET) == 0)) {
            if (!drive[0].isCDROM)
                drive[0].status = (byte) (READY_STAT | SEEK_STAT);
            else
                drive[0].status = 0;
            drive[0].setSignature();
            if (drive[1] != null) {
                if (!drive[1].isCDROM)
                    drive[1].status = (byte) (READY_STAT | SEEK_STAT);
                else
                    drive[1].status = 0;
            }
            if (drive[1] != null) drive[1].setSignature();
        }
        drive[0].command = (byte) data;
        if (drive[1] != null) drive[1].command = (byte) data;
    }

    private void writeIDE(int address, int data) {
        if (drive[0] == null) return;
        switch (address & 0x7) {
            case 0:
                break;
            case 1:
                drive[0].feature = (byte) data;
                if (drive[1] != null) drive[1].feature = (byte) data;
                break;
            case 2:
                drive[0].nSector = 0xff & data;
                if (drive[1] != null) drive[1].nSector = 0xff & data;
                break;
            case 3:
                drive[0].sector = (byte) data;
                if (drive[1] != null) drive[1].sector = (byte) data;
                break;
            case 4:
                drive[0].lcyl = (byte) data;
                if (drive[1] != null) drive[1].lcyl = (byte) data;
                break;
            case 5:
                drive[0].hcyl = (byte) data;
                if (drive[1] != null) drive[1].hcyl = (byte) data;
                break;
            case 6:
                drive[0].select = (byte) ((data & ~0x10) | 0xa0);
                if (drive[1] != null) drive[1].select = (byte) (data | 0x10 | 0xa0);
                currentDrive = (data >> 4) & 1;
                break;

            case 7:
                if (drive[currentDrive] == null || drive[currentDrive].disk == null) break;
                switch (data & 0xff) {
                    case WIN_IDENTIFY:
                        if (drive[currentDrive].isCDROM) {
                            drive[currentDrive].setSignature();
                        } else {
                            drive[currentDrive].identify();
                            drive[currentDrive].status = (byte) (READY_STAT | SEEK_STAT);
                            drive[currentDrive].transferStart(drive[currentDrive].ioBuffer, 0, 512, ETF_TRANSFER_STOP);
                        }
                        drive[currentDrive].setIRQ();
                        break;
                    case WIN_ATAPI_IDENTIFY:
                        if (drive[currentDrive].isCDROM) {
                            drive[currentDrive].atapiIdentify();
                            drive[currentDrive].status = (byte) (READY_STAT | SEEK_STAT);
                            drive[currentDrive].transferStart(drive[currentDrive].ioBuffer, 0, 512, ETF_TRANSFER_STOP);
                        }
                        break;
                    case WIN_DIAGNOSE:
                        drive[currentDrive].setSignature();
                        drive[currentDrive].status = 0;
                        drive[currentDrive].error = 1;
                        break;
                    case WIN_SRST:
                        if (drive[currentDrive].isCDROM) {
                            drive[currentDrive].setSignature();
                            drive[currentDrive].status = 0;
                            drive[currentDrive].error = 1;
                        }
                        break;
                    case WIN_PACKETCMD:
                        if (drive[currentDrive].isCDROM) {
                            drive[currentDrive].nSector = 1;
                            drive[currentDrive].transferStart(drive[currentDrive].ioBuffer, 0, ATAPI_PACKET_SIZE, ETF_ATAPI_COMMAND);
                        }
                        break;
                    case WIN_READ:
                    case WIN_READ_ONCE:
                        drive[currentDrive].requiredNumberOfSectors = 1;
                        drive[currentDrive].sectorRead();
                        break;
                    case WIN_WRITE:
                    case WIN_WRITE_ONCE:
                        drive[currentDrive].error = 0;
                        drive[currentDrive].status = SEEK_STAT | READY_STAT;
                        drive[currentDrive].requiredNumberOfSectors = 1;
                        drive[currentDrive].transferStart(drive[currentDrive].ioBuffer, 0, 512, ETF_SECTOR_WRITE);
                        break;
                    default:
                        System.out.printf("Bad drive data: %x\n", data);
                        System.exit(0);
                }
                break;
        }
    }

    private int readIDE(int address) {
        if (currentDrive == -1) return 0;
        if (drive[currentDrive] == null || drive[currentDrive].disk == null) return 0;
        switch (address & 0x7) {
            case 0:
                return 0xff;
            case 1:
                return drive[currentDrive].error;
            case 2:
                return drive[currentDrive].nSector;
            case 3:
                return drive[currentDrive].sector;
            case 4:
                return drive[currentDrive].lcyl;
            case 5:
                return drive[currentDrive].hcyl;
            case 6:
                return drive[currentDrive].select;
            default:
            case 7:
                computer.interruptController.setIRQ(irq, 0);
                return drive[currentDrive].status;
        }
    }

    private int readDataWord() {
        if (currentDrive == -1) return 0;
        int data = 0;
        data |= 0xff & drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++];
        data |= 0xff00 & (drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] << 8);
        if (drive[currentDrive].dataBufferOffset >= drive[currentDrive].dataBufferEnd)
            drive[currentDrive].endTransfer(drive[currentDrive].endTransferFunction);
        return data;
    }

    private int readDataLong() {
        if (currentDrive == -1) return 0;
        int data = 0;
        data |= 0xff & drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++];
        data |= 0xff00 & (drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] << 8);
        data |= 0xff0000 & (drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] << 16);
        data |= 0xff000000 & (drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] << 24);
        if (drive[currentDrive].dataBufferOffset >= drive[currentDrive].dataBufferEnd)
            drive[currentDrive].endTransfer(drive[currentDrive].endTransferFunction);
        return data;
    }

    private void writeDataWord(int data) {
        if (currentDrive == -1) return;
        drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] = (byte) data;
        drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] = (byte) (data >> 8);
        if (drive[currentDrive].dataBufferOffset >= drive[currentDrive].dataBufferEnd)
            drive[currentDrive].endTransfer(drive[currentDrive].endTransferFunction);
    }

    private void writeDataLong(int data) {
        if (currentDrive == -1) return;
        drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] = (byte) data;
        drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] = (byte) (data >> 8);
        drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] = (byte) (data >> 16);
        drive[currentDrive].dataBuffer[drive[currentDrive].dataBufferOffset++] = (byte) (data >> 24);
        if (drive[currentDrive].dataBufferOffset >= drive[currentDrive].dataBufferEnd)
            drive[currentDrive].endTransfer(drive[currentDrive].endTransferFunction);
    }


    class Drive {
        public boolean isCDROM;
        public int cylinders, heads, sectors;
        public byte status, command, error, feature, select, hcyl, lcyl, sector;
        public int nSector, endTransferFunction;
        public byte[] ioBuffer;
        private int ioBufferSize, ioBufferIndex;
        public byte[] dataBuffer;
        public int dataBufferOffset, dataBufferEnd;
        public boolean identifySet;
        public byte[] identifyData;
        public Disk disk;
        private int requiredNumberOfSectors = 1;
        private int id;
        private int lba;
        private int cdSectorSize;
        public int packetTransferSize;
        public int elementaryTransferSize;

        public String saveState() {
            String state = "";
            state += (isCDROM ? 1 : 0) + " " + cylinders + " " + heads + " " + sectors + " " + status + " " + command + " " + error + " " + feature + " " + select + " " + hcyl + " " + lcyl + " " + sector + " ";
            state += nSector + " " + endTransferFunction + " " + ioBufferSize + " " + ioBufferIndex + " " + dataBufferOffset + " " + dataBufferEnd + " " + (identifySet ? 1 : 0) + " ";
            state += requiredNumberOfSectors + " " + id + " " + lba + " " + cdSectorSize + " " + packetTransferSize + " " + elementaryTransferSize + " ";
            for (int i = 0; i < ioBuffer.length; i++)
                state += ioBuffer[i] + " ";
            if (dataBuffer == null)
                state += "-1 ";
            else {
                state += dataBuffer.length + " ";
                for (int i = 0; i < dataBuffer.length; i++)
                    state += dataBuffer[i] + " ";
            }
            if (identifyData == null)
                state += "-1 ";
            else {
                state += identifyData.length + " ";
                for (int i = 0; i < identifyData.length; i++)
                    state += identifyData[i] + " ";
            }
            return state;
        }

        public void loadState(String state) {
            Scanner s = new Scanner(state);
            isCDROM = s.nextInt() == 1;
            cylinders = s.nextInt();
            heads = s.nextInt();
            sectors = s.nextInt();
            status = s.nextByte();
            command = s.nextByte();
            error = s.nextByte();
            feature = s.nextByte();
            select = s.nextByte();
            hcyl = s.nextByte();
            lcyl = s.nextByte();
            sector = s.nextByte();
            nSector = s.nextInt();
            endTransferFunction = s.nextInt();
            ioBufferSize = s.nextInt();
            ioBufferIndex = s.nextInt();
            dataBufferOffset = s.nextInt();
            dataBufferEnd = s.nextInt();
            identifySet = s.nextInt() == 1;
            requiredNumberOfSectors = s.nextInt();
            id = s.nextInt();
            lba = s.nextInt();
            cdSectorSize = s.nextInt();
            packetTransferSize = s.nextInt();
            elementaryTransferSize = s.nextInt();
            for (int i = 0; i < ioBuffer.length; i++)
                ioBuffer[i] = s.nextByte();
            int size;
            size = s.nextInt();
            if (size != -1) {
                dataBuffer = new byte[size];
                for (int i = 0; i < dataBuffer.length; i++)
                    dataBuffer[i] = s.nextByte();
            }
            size = s.nextInt();
            if (size != -1) {
                identifyData = new byte[size];
                for (int i = 0; i < identifyData.length; i++)
                    identifyData[i] = s.nextByte();
            }
        }

        public Drive(int id, Disk disk, int cylinders, int heads, int sectors, boolean isCDROM) {
            this.isCDROM = isCDROM;
            this.id = id;
            this.disk = disk;
            this.cylinders = cylinders;
            this.heads = heads;
            this.sectors = sectors;
            ioBuffer = new byte[16 * 512 + 4];
            identifyData = new byte[512];
            identifySet = false;
            reset();
            refreshGUI();
        }

        public void refreshGUI() {
            String name = "Hard disk ";
            if (id == 0) name += "C:";
            else name += "D:";
            if (computer.diskGUI[2 + id] != null)
                computer.diskGUI[2 + id].redraw(name, cylinders, heads, sectors, disk);
            if (computer.sectorGUI[2 + id] != null)
                computer.sectorGUI[2 + id].redraw(name, cylinders, heads, sectors, disk);
        }

        public void setSignature() {
            select &= 0xf0;
            nSector = 1;
            sector = 1;
            lcyl = 0;
            hcyl = 0;
            if (isCDROM) {
                lcyl = (byte) 0x14;
                hcyl = (byte) 0xeb;
            }
        }

        public void setIRQ() {
            computer.interruptController.setIRQ(irq, 1);
        }

        public void setSector(int sectorNumber) {
            if ((select & 0x40) != 0) {
                select = (byte) ((select & 0xf0) | (sectorNumber >>> 24));
                hcyl = (byte) (sectorNumber >>> 16);
                lcyl = (byte) (sectorNumber >>> 8);
                sector = (byte) sectorNumber;
            } else {
                int cyl = (int) (sectorNumber / (heads * sectors));
                int r = (int) (sectorNumber % (heads * sectors));
                hcyl = (byte) (cyl >>> 8);
                lcyl = (byte) (cyl);
                select = (byte) ((select & 0xf0) | ((r / sectors) & 0x0f));
                sector = (byte) ((r % sectors) + 1);
            }
        }

        public void sectorWrite() {
            status = READY_STAT | SEEK_STAT;
            int sectorNumber = getSector();
            int n = nSector;
            if (n > requiredNumberOfSectors)
                n = requiredNumberOfSectors;
            if (computer.diskGUI[id + 2] != null) computer.diskGUI[id + 2].write(sectorNumber);
            if (computer.sectorGUI[id + 2] != null) computer.sectorGUI[id + 2].write(sectorNumber);
            disk.write(sectorNumber, ioBuffer, n);
            nSector -= n;
            if (nSector == 0)
                transferStop();
            else {
                int n1 = nSector;
                if (n1 > requiredNumberOfSectors)
                    n1 = requiredNumberOfSectors;
                transferStart(ioBuffer, 0, 512 * n1, ETF_SECTOR_WRITE);
            }
            setSector(sectorNumber + n);
            setIRQ();
        }

        public void sectorRead() {
            status = READY_STAT | SEEK_STAT;
            error = 0;
            int sectorNumber = getSector();
            int n = nSector;
            if (computer.diskGUI[2 + id] != null) computer.diskGUI[2 + id].read(sectorNumber);
            if (computer.sectorGUI[2 + id] != null) computer.sectorGUI[2 + id].read(sectorNumber);
            if (n == 0)
                transferStop();
            else {
                if (n > requiredNumberOfSectors)
                    n = requiredNumberOfSectors;
                disk.read(sectorNumber, ioBuffer, n);
                transferStart(ioBuffer, 0, 512 * n, ETF_SECTOR_READ);
                setIRQ();
                setSector(sectorNumber + n);
                nSector -= n;
            }
        }


        public void atapiIdentify() {

            if (identifySet) {
                System.arraycopy(identifyData, 0, ioBuffer, 0, identifyData.length);
                return;
            }

            for (int i = 0; i < 512; i++) {
                ioBuffer[i] = (byte) 0;
            }
            int num = (2 << 14) | (5 << 8) | (1 << 7) | (2 << 5) | (0 << 0);
            for (int i = 0; i < 16; i++)
                ioBuffer[i] = (byte) ((num >> (8 * i)) & 0xff);
            ioBuffer[40] = 3;
            ioBuffer[43] = 512 / 256;
            ioBuffer[44] = 4;
            ioBuffer[96] = 1;
            ioBuffer[98] = 0;
            ioBuffer[99] = 2;
            ioBuffer[106] = 3;
            ioBuffer[126] = 3;
            ioBuffer[127] = 1;
            ioBuffer[128] = 1;
            ioBuffer[130] = (byte) 0xb4;
            ioBuffer[132] = (byte) 0xb4;
            ioBuffer[134] = 0x2c;
            ioBuffer[135] = 1;
            ioBuffer[136] = (byte) 0xb4;
            ioBuffer[142] = 30;
            ioBuffer[144] = 30;
            ioBuffer[160] = 0x1e;

            System.arraycopy(ioBuffer, 0, identifyData, 0, identifyData.length);
            identifySet = true;
        }

        public void identify() {
            if (identifySet) {
                System.arraycopy(identifyData, 0, ioBuffer, 0, identifyData.length);
                return;
            }
            for (int i = 0; i < 512; i++)
                ioBuffer[i] = 0;
            ioBuffer[0] = 0x40;
            ioBuffer[1] = 0;
            ioBuffer[2] = (byte) cylinders;
            ioBuffer[3] = (byte) (cylinders >>> 8);
            ioBuffer[6] = (byte) heads;
            ioBuffer[7] = (byte) (heads >>> 8);
            ioBuffer[8] = (byte) (512 * sectors);
            ioBuffer[9] = (byte) ((512 * sectors) >>> 8);
            ioBuffer[10] = (byte) 512;
            ioBuffer[11] = (byte) (512 >>> 8);
            ioBuffer[12] = (byte) sectors;
            ioBuffer[13] = (byte) (sectors >>> 8);
            ioBuffer[20] = 0;
            ioBuffer[40] = (byte) 3;
            ioBuffer[41] = 0;
            ioBuffer[42] = (byte) 512;
            ioBuffer[43] = (byte) (512 >>> 8);
            ioBuffer[44] = (byte) 4;
            ioBuffer[45] = 0;
            ioBuffer[46] = 0;
            ioBuffer[54] = 0;
            ioBuffer[94] = (byte) 1;
            ioBuffer[95] = 0;
            ioBuffer[96] = 1;
            ioBuffer[97] = 0;
            ioBuffer[98] = 0;
            ioBuffer[99] = 0;
            ioBuffer[102] = 0;
            ioBuffer[103] = (byte) 2;
            ioBuffer[104] = 0;
            ioBuffer[105] = (byte) 2;
            ioBuffer[106] = (byte) 7;
            ioBuffer[107] = 0;
            ioBuffer[108] = (byte) cylinders;
            ioBuffer[109] = (byte) (cylinders >>> 8);
            ioBuffer[110] = (byte) heads;
            ioBuffer[111] = (byte) (heads >>> 8);
            ioBuffer[112] = (byte) sectors;
            ioBuffer[113] = (byte) (sectors >>> 8);
            ioBuffer[114] = (byte) ((cylinders * heads * sectors));
            ioBuffer[115] = (byte) ((cylinders * heads * sectors) >>> 8);
            ioBuffer[116] = (byte) ((cylinders * heads * sectors) >>> 16);
            ioBuffer[117] = (byte) ((cylinders * heads * sectors) >>> 24);
            ioBuffer[120] = (byte) ((cylinders * heads * sectors));
            ioBuffer[121] = (byte) ((cylinders * heads * sectors) >>> 8);
            ioBuffer[122] = (byte) ((cylinders * heads * sectors) >>> 16);
            ioBuffer[123] = (byte) ((cylinders * heads * sectors) >>> 24);
            ioBuffer[200] = (byte) nSector;
            ioBuffer[201] = (byte) (nSector >>> 8);
            ioBuffer[202] = (byte) (nSector >>> 16);
            ioBuffer[203] = (byte) (nSector >>> 24);
            System.arraycopy(ioBuffer, 0, identifyData, 0, identifyData.length);
            identifySet = true;
        }

        public void transferStart(byte[] buffer, int offset, int size, int endTransferFunction) {
            this.endTransferFunction = endTransferFunction;
            dataBuffer = buffer;
            dataBufferOffset = offset;
            dataBufferEnd = size + offset;
            status |= DRQ_STAT;
        }

        public void transferStop() {
            endTransferFunction = ETF_TRANSFER_STOP;
            dataBuffer = ioBuffer;
            dataBufferEnd = 0;
            dataBufferOffset = 0;
            status &= ~DRQ_STAT;
        }

        private int getSector() {
            if ((select & 0x40) != 0)
                return ((select & 0x0f) << 24) | ((0xff & hcyl) << 16) | ((0xff & lcyl) << 8) | (0xff & sector);
            else
                return ((((0xff & hcyl) << 8) | (0xff & lcyl)) * heads * sectors) + ((select & 0xf) * sectors) + ((0xff & sector) - 1);

        }

        public void endTransfer(int mode) {
            switch (mode) {
                case ETF_TRANSFER_STOP:
                    transferStop();
                    break;
                case ETF_SECTOR_WRITE:
                    sectorWrite();
                    break;
                case ETF_SECTOR_READ:
                    sectorRead();
                    break;
                case ETF_DUMMY_TRANSFER_STOP:
                    dummyTransferStop();
                    break;
                case ETF_ATAPI_COMMAND:
                    atapiCommand();
                    break;
                case ETF_ATAPI_COMMAND_REPLY_END:
                    atapiCommandReplyEnd();
                    break;
            }
        }

        public void reset() {
            select = (byte) 0xa0;
            status = READY_STAT;
            setSignature();
            endTransferFunction = ETF_DUMMY_TRANSFER_STOP;
            endTransfer(ETF_DUMMY_TRANSFER_STOP);
        }

        private void dummyTransferStop() {
            dataBuffer = ioBuffer;
            dataBufferEnd = 0;
            ioBuffer[0] = (byte) 0xff;
            ioBuffer[1] = (byte) 0xff;
            ioBuffer[2] = (byte) 0xff;
            ioBuffer[3] = (byte) 0xff;
        }

        public int getCylinders() {
            return cylinders;
        }

        public int getHeads() {
            return heads;
        }

        public int getSectors() {
            return sectors;
        }

        public void atapiCommand() {
            switch (0xff & ioBuffer[0]) {
                case CD_MODE_SENSE_10: {
                    int maxLength = ioBuffer[7] * 256 + ioBuffer[8];
                    int action = (0xff & ioBuffer[2]) >>> 6;
                    int code = ioBuffer[2] & 0x3f;
                    switch (action) {
                        case 0:
                            switch (code) {
                                case 1:
                                    ioBuffer[0] = 22;
                                    ioBuffer[2] = 0x70;
                                    ioBuffer[3] = 0;
                                    ioBuffer[4] = 0;
                                    ioBuffer[5] = 0;
                                    ioBuffer[6] = 0;
                                    ioBuffer[7] = 0;
                                    ioBuffer[8] = 1;
                                    ioBuffer[9] = 6;
                                    ioBuffer[10] = 0;
                                    ioBuffer[11] = 5;
                                    ioBuffer[12] = 0;
                                    ioBuffer[13] = 0;
                                    ioBuffer[14] = 0;
                                    ioBuffer[15] = 0;
                                    atapiCommandReply(16, maxLength);
                                    break;
                                case 0x2a:
                                    ioBuffer[0] = 34;
                                    ioBuffer[2] = 0x70;
                                    ioBuffer[3] = 0;
                                    ioBuffer[4] = 0;
                                    ioBuffer[5] = 0;
                                    ioBuffer[6] = 0;
                                    ioBuffer[7] = 0;
                                    ioBuffer[8] = 0x2a;
                                    ioBuffer[9] = 0x12;
                                    ioBuffer[10] = 0;
                                    ioBuffer[11] = 0;
                                    ioBuffer[12] = 0x70;
                                    ioBuffer[13] = 3 << 5;
                                    ioBuffer[14] = (1 << 0) | (1 << 3) | (1 << 5);
                                    ioBuffer[15] = 0;
                                    ioBuffer[16] = 2;
                                    ioBuffer[17] = (byte) 0xc2;
                                    ioBuffer[18] = 0;
                                    ioBuffer[19] = 2;
                                    ioBuffer[20] = 2;
                                    ioBuffer[21] = 0;
                                    ioBuffer[22] = 2;
                                    ioBuffer[23] = (byte) 0xc2;
                                    ioBuffer[24] = 0;
                                    ioBuffer[25] = 0;
                                    ioBuffer[26] = 0;
                                    ioBuffer[27] = 0;
                                    atapiCommandReply(28, maxLength);
                                    break;
                            }
                    }
                }
                break;
                case CD_READ_10:
                    int numSectors;
                    numSectors = ioBuffer[7] * 256 + ioBuffer[8];
                    int lba = (int) ((((ioBuffer[2] & 0xff) << 24) + ((ioBuffer[3] & 0xff) << 16) + ((ioBuffer[4] & 0xff) << 8) + (ioBuffer[5] & 0xff)));
                    if (numSectors == 0) {
                        atapiCommandOk();
                        break;
                    }
                    atapiCommandRead(lba, numSectors, 2048);
                    break;
                default:
                    System.out.println("Unimplemented ATAPI command " + ioBuffer[0]);
                    System.exit(0);
            }
        }

        public void atapiCommandOk() {
            error = 0;
            status = READY_STAT;
            nSector = (nSector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
            setIRQ();
        }

        public void atapiCommandRead(int lba, int numSectors, int sectorSize) {
            this.lba = lba;
            packetTransferSize = numSectors * sectorSize;
            elementaryTransferSize = 0;
            ioBufferIndex = sectorSize;
            cdSectorSize = sectorSize;
            status = READY_STAT;
            atapiCommandReplyEnd();
        }

        public void atapiCommandReply(int size, int maxSize) {
            lba = -1;
            packetTransferSize = Math.min(size, maxSize);
            elementaryTransferSize = 0;
            ioBufferIndex = 0;
            status = READY_STAT;
            atapiCommandReplyEnd();
        }

        public void atapiCommandReplyEnd() {
            if (packetTransferSize <= 0) {
                transferStop();
                status = READY_STAT;
                nSector = (nSector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
                setIRQ();
            } else {
                if (lba != -1 && ioBufferIndex >= cdSectorSize) {
                    cdReadSector(lba, ioBuffer, cdSectorSize);
                    lba++;
                    ioBufferIndex = 0;
                }
                if (elementaryTransferSize > 0) {
                    int size = Math.min(cdSectorSize - ioBufferIndex, elementaryTransferSize);
                    transferStart(ioBuffer, ioBufferIndex, size, ETF_ATAPI_COMMAND_REPLY_END);
                    packetTransferSize -= size;
                    elementaryTransferSize -= size;
                    ioBufferIndex += size;
                } else {
                    nSector = (nSector & ~7) | ATAPI_INT_REASON_IO;
                    int byteCountLimit = (0xff & lcyl) | (0xff00 & (hcyl << 8));
                    if (byteCountLimit == 0xffff)
                        byteCountLimit--;
                    int size = packetTransferSize;
                    if (size > byteCountLimit) {
                        if ((byteCountLimit & 1) != 0)
                            byteCountLimit--;
                        size = byteCountLimit;
                    }
                    lcyl = (byte) size;
                    hcyl = (byte) (size >>> 8);
                    elementaryTransferSize = size;
                    if (lba != -1)
                        size = Math.min(cdSectorSize - ioBufferIndex, size);
                    transferStart(ioBuffer, ioBufferIndex, size, ETF_ATAPI_COMMAND_REPLY_END);
                    packetTransferSize -= size;
                    elementaryTransferSize -= size;
                    ioBufferIndex += size;
                    setIRQ();
                }
            }
        }

        private void cdReadSector(int lba, byte[] buffer, int sectorSize) {
            switch (sectorSize) {
                case 2048:
                    disk.read((lba) << 2, buffer, 4);
                    break;
                case 2352:
                    disk.read((lba) << 2, buffer, 4);
                    System.arraycopy(buffer, 0, buffer, 16, 2048);
                    buffer[0] = 0;
                    for (int i = 1; i < 11; i++)
                        buffer[i] = (byte) 0xff;
                    buffer[11] = 0;
                    buffer[12] = (byte) (((lba + 150) / 75) / 60);
                    buffer[13] = (byte) (((lba + 150) / 75) % 60);
                    buffer[14] = (byte) ((lba + 150) % 75);
                    buffer[15] = 1;
                    for (int i = 2064; i < 2352; i++)
                        buffer[i] = 0;
                    break;
            }
        }
    }
}
