package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
//import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
        /* Start of WU's code*/
        for (int i=0; i < 16; i++)
            fdlist[i] = new FD();
        
        fdlist[0].fn = "std0";
        fdlist[0].file = UserKernel.console.openForReading();
        delay();
        if(fdlist[0].file == null)
            System.out.println("std0 fail");
        
        fdlist[1].fn = "std1";
        fdlist[1].file = UserKernel.console.openForWriting();
        delay();
        if(fdlist[1].file == null)
            System.out.println("std1 fail");
        /* End of WU's code*/
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
        
        /* Start of Tao's code */
       /* Lib.debug(dbgProcess, "In readVirtualMemory: vaddr=" + vaddr + ", byte len="
                  + data.length + ", beginning offset=" + offset + ", length=" + length
                  + " current pid = " + getPID());*/
        
        int amount = 0;
		byte[] physicalMemory = Machine.processor().getMemory();
		/* End of Tao's code */
		//byte[] memory = Machine.processor().getMemory();
        
        /* Start of Tao's code */
        if (vaddr < 0)
            return 0;
        
        // virtual memory: [from, to]
        int fromPage = Processor.pageFromAddress(vaddr);
        int fromOffset = Processor.offsetFromAddress(vaddr);
        int toPage = Processor.pageFromAddress(vaddr + length - 1);
        int toOffset = Processor.offsetFromAddress(vaddr + length - 1);
        Lib.debug(dbgProcess, "\tVirtualMem Addr from (page " + fromPage + " offset "
                  + fromOffset + ") to (page " + toPage + " offset " + toOffset + ")");
        /* End of Tao's code */
        
//		// for now, just assume that virtual addresses equal physical addresses
//		if (vaddr < 0 || vaddr >= memory.length)
//			return 0;
//
//		int amount = Math.min(length, memory.length - vaddr);
//		System.arraycopy(memory, vaddr, data, offset, amount);
        
         /* Start of Tao's code */
        for (int i = fromPage; i <= toPage; i++) {
            Lib.debug(dbgProcess, "\t** In page " + i);
            if (!virtualToTransEntry.containsKey(i)
                || !virtualToTransEntry.get(i).valid) {
                // the current query page is invalid to access
                Lib.debug(dbgProcess, "\t Page invalid or not exist for this process");
                break;
            }
            
            int count, off;
            if (i == fromPage) {
                count = Math.min(Processor.pageSize - fromOffset, length);
                off = fromOffset;
            } else if (i == toPage) {
                count = toOffset + 1; // read [0, toOffset] from the last page
                off = 0;
            } else {
                count = Processor.pageSize;
                off = 0;
            }
            
            int srcPos = Processor.makeAddress(virtualToTransEntry.get(i).ppn, off);
            
            Lib.debug(dbgProcess, "\t *PhyMem Addr=" + srcPos + " data index=" + (offset + amount) 
                      + " count=" + count);
            System.arraycopy(physicalMemory, srcPos, data, offset + amount, count);
            amount += count;
            /* End of Tao's code */
            
		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		//byte[] memory = Machine.processor().getMemory();
        byte[] physicalMemory = Machine.processor().getMemory();// Tao's code

		int amount = 0;// Tao's code

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 /*|| vaddr >= memory.length*/)
			return 0;

        // virtual memory: [from, to)
        int fromPage = Processor.pageFromAddress(vaddr);
        int fromOffset = Processor.offsetFromAddress(vaddr);
        int toPage = Processor.pageFromAddress(vaddr + length - 1);
        int toOffset = Processor.offsetFromAddress(vaddr + length - 1);

//      int amount = Math.min(length, memory.length - vaddr);
//		System.arraycopy(data, offset, memory, vaddr, amount);
        
        /* Start of Tao's code */
        for (int i = fromPage; i <= toPage; i++) {
            if (!virtualToTransEntry.containsKey(i)
                || !virtualToTransEntry.get(i).valid
                || virtualToTransEntry.get(i).readOnly) {
                // the current query page is invalid to access
                break;
            }
            
            int count, off;
            if (i == fromPage) {
                count = Math.min(Processor.pageSize - fromOffset, length);
                off = fromOffset;
            } else if (i == toPage) {
                count = toOffset + 1;
                off = 0;
            } else {
                count = Processor.pageSize;
                off = 0;
            }
            
            int dstPos = Processor.makeAddress(virtualToTransEntry.get(i).ppn, off);
            System.arraycopy(data, offset + amount, physicalMemory, dstPos, count);
            
            amount += count;
        }
        /* End of Tao's code */

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
        Lib.debug(dbgProcess, "\tBegin parsing each section...");// Tao's code
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable"); // Tao's code
				return false;
			}
			numPages += section.getLength();
		}
        Lib.debug(dbgProcess, "\t--->complete! Section pages: " + numPages);

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();
        Lib.debug(dbgProcess, "\tentry point: " + initialPC); // Tao's code

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;
        Lib.debug(dbgProcess, "\tstack initial point: " + initialSP);// Tao's code
		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
            /*Lib.debug(dbgProcess, "\t(load) write arguments " + i
                      + ", owner PID = " + getPID()); // Tao's code*/
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
            

		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 *
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
         /* Start of Tao's code */
        Lib.debug(dbgProcess, "UserProcess.loadSections");
        Lib.debug(dbgProcess, "\tNeed " + numPages + " pages, free pages #: "
                  + UserKernel.freePages.size());
        UserKernel.fpLock.acquire();
         /* End of Tao's code */
        
		//if (numPages > Machine.processor().getNumPhysPages()) {
        if (numPages > UserKernel.freePages.size()) { // Tao's code
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
            UserKernel.fpLock.release();//
			return false;
		}
        
        /* Start of Tao's code */
        // allocate the page table now
        int pagesCount = 0;
        pageTable = new TranslationEntry[numPages];
        /* End of Tao's code */

		// load sections
        int vpn = -1, ppn = -1;// Tao's code
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
            boolean readOnly = section.isReadOnly(); // Tao's code

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
                
                /* Start of Tao's code */
                // now find a free physical page
                Lib.assertTrue(!UserKernel.freePages.isEmpty());
                ppn = UserKernel.freePages.pollFirst();
                /* End of Tao's code */
                
				//section.loadPage(i, vpn);
                
                /* Start of Tao's code */
                section.loadPage(i, ppn);
                // register this page
                pageTable[pagesCount++] =
                new TranslationEntry(vpn, ppn, true, readOnly, false, false);
                /* End of Tao's code */
			}
		}
        
        /* Start of Tao's code */
        // register remaining pages for stack and arguments (XXX: not sure)
        Lib.assertTrue(vpn >= 0 && ppn >= 0);
        Lib.debug(dbgProcess, "\tStill has " + (numPages - pagesCount) + " pages");
        while (pagesCount < numPages) {
            Lib.assertTrue(!UserKernel.freePages.isEmpty());
            ppn = UserKernel.freePages.pollFirst();
            pageTable[pagesCount++] =
            new TranslationEntry(++vpn, ppn, true, false, false, false);
        }
        
        // fill up the virtual -> translation entry map
        for (int i = 0; i < pageTable.length; i++) {
            virtualToTransEntry.put(pageTable[i].vpn, pageTable[i]);
        }
        
        UserKernel.fpLock.release();
        /* End of Tao's code */
        
        //Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\"): complete!");
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
        /* Start of Tao's code */
        UserKernel.fpLock.acquire();
        
        // Put back the using physical pages to free list again
        for (TranslationEntry entry : virtualToTransEntry.values()) {
            UserKernel.freePages.add(entry.ppn);
        }
        
        UserKernel.fpLock.release();
        /* End of Tao's code */
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallHalt:
				return handleHalt();

			default:
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
				Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
			case Processor.exceptionSyscall:
				int result = handleSyscall(processor.readRegister(Processor.regV0),
						processor.readRegister(Processor.regA0),
						processor.readRegister(Processor.regA1),
						processor.readRegister(Processor.regA2),
						processor.readRegister(Processor.regA3));
				processor.writeRegister(Processor.regV0, result);
				processor.advancePC();
				break;

			default:
				Lib.debug(dbgProcess, "Unexpected exception: "
						+ Processor.exceptionNames[cause]);
				Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';
        
    /*Start of Tao's code*/
    private int pid;

	/** A map from virtual memory to Translation entry: <vaddr, TranslationEntry>. */
    private HashMap<Integer, TranslationEntry> virtualToTransEntry = null;
	/*End of Tao's code*/
}
