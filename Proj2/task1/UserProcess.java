package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;

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
        
        //yiwen
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
        
        virtualToTransEntry = new HashMap<Integer, TranslationEntry>();
        
        
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
            
        }
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
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }
        
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
        
        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;
        
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
                vpn = section.getFirstVPN() + i;
                
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
    
    //yiwen
    private int handleCreate(int a0) {
        if(a0 < 0){
            System.out.println("bad address");
            return -1;
        }
        
        String fn = readVirtualMemoryString(a0, 256);
        delay();
        if(toRemove.contains(fn)){
            System.out.println("it is to be deleted");
            return -1;
        }
        /*for(int i = 0; i < 16; i++){
        	if(fdlist[i].fn.equals(fn))
         if(fdlist[i].forRemove){
         System.out.println("it is for removing");
         return -1;
         }
         }*/
        
        for(int i = 0; i < 16; i++){
            if(fdlist[i].fn.equals(fn)){
                System.out.println("Already Exists in this process, truncate it");
                OpenFile theFile  = UserKernel.fileSystem.open(fn, true);
                delay();
                if (theFile == null) {
                    System.out.println("fail to truncate file");
                    return -1;
                }
                fdlist[i].file = theFile;
                return i;
            }
        }
        
        System.out.println(fn);
        
        OpenFile theFile  = UserKernel.fileSystem.open(fn, true);
        delay();
        
        if (theFile == null) {
            System.out.println("fail to create file");
            return -1;
        }
        int fileIndex = findNewFD();
        delay();
        if (fileIndex < 0 || fileIndex >= 16){
            System.out.println("no available file descriptor");
            UserKernel.fileSystem.remove(fn);
            return -1;
        }
        
        if(FDcounter.containsKey(fn) && FDcounter.get(fn) >= 1){
            System.out.println("FD exists in other process, truncate file");
            FDcounter.put(fn, FDcounter.get(fn)+1);
        }else
            FDcounter.put(fn, 1);
        fdlist[fileIndex].file = theFile;
        fdlist[fileIndex].fn = fn;
        //fdlist[fileIndex].forRemove = false;
        //fdlist[fileIndex].opened = false;
        return fileIndex;
    }
    
    //yiwen
    private int handleOpen(int a0) {
        if(a0 < 0){
            System.out.println("bad address");
            return -1;
        }
        String fn = readVirtualMemoryString(a0, 256);
        delay();
        
        if(toRemove.contains(fn)){
            System.out.println("it is to be deleted");
            return -1;
        }
        /*for(int i = 0; i < 16; i++){
        	if(fdlist[i].fn.equals(fn))
         if(fdlist[i].forRemove){
         System.out.println("it is for removing");
         return -1;
         }
         }*/
        
        
        for(int i = 0; i < 16; i++){
            if(fdlist[i].fn.equals(fn)){
                System.out.println("Already Opened");
                return -1;
            }
        }
        
        /*int fileindex = findFDfromName(fn);
         if(fileindex != -1){
         OpenFile theFile  = UserKernel.fileSystem.open(fn, false);
         delay();
         if (theFile == null){
         System.out.println("File Not Exist");
         return -1;
         }
         fdlist[fileindex].file = theFile;
         //fdlist[fileindex].opened = true;
         return fileindex;
         }*/
        
        OpenFile theFile  = UserKernel.fileSystem.open(fn, false);
        delay();
        
        if (theFile == null){
            System.out.println("File Not Exist");
            return -1;
        }
        int fileindex = findNewFD();
        delay();
        if (fileindex < 0 || fileindex >= 16){
            System.out.println("no available file descriptor");
            return -1;
        }
        
        if(FDcounter.containsKey(fn)){
            FDcounter.put(fn, FDcounter.get(fn)+1);
        }else{
            FDcounter.put(fn, 1);
        }
        fdlist[fileindex].fn = fn;
        fdlist[fileindex].file = theFile;
        //fdlist[fileindex].opened = true;
        return fileindex;
    }
    
    //yiwen
    private int handleRead(int a0, int a1, int a2) {
        
        if(a0 < 0 || a0 >= 16){
            System.out.println("wrong File Descriptor index");
            return -1;
        }
        
        if(a1 < 0){
            System.out.println("bad buffer address");
            return -1;
        }
        
        if(a2 < 0){
            System.out.println("negative buffer size");
            return -1;
        }
        
        byte[] buffer = new byte[a2];
        
        int fileindex = a0;
        /*if (fileindex < 0 || fileindex >= 16){
         System.out.println("Wrong file index");
         return -1;
         }*/
	    	  
        if (fdlist[fileindex].file == null){
            System.out.println("Empty File Descriptor");
            return -1;
        }
        
        
        
        /*String fn = fdlist[fileindex].fn;
         fileindex = -1;
         boolean openflag = false;
         for(int i = 0; i < 16; i++){
         if(fdlist[i].fn.equals(fn)){
         if(fdlist[i].opened)
         openflag = true;
         else
         fileindex = i;
         }
         }
         
         if(fileindex == -1 || openflag == false)
         return -1;*/
        if (a2 == 0)
            return 0;
        
        FD fd = fdlist[fileindex];
        
        int count1 = fd.file.read(buffer, 0, a2);
        delay();
        if (count1 < 0){
            System.out.println("read failure");
            return -1;
        }
        System.out.println("Read " + cfount1 + " bytes");
        
        int count2 = writeVirtualMemory(a1, buffer, 0, count1);
        delay();
        if (count2 < 0){
            System.out.println("write failure");
            return -1;
        }
        System.out.println("Write " + count2 + " bytes to memory");
        
        return count2;
    }
    
    
    //yiwen
    private int handleWrite(int a0, int a1, int a2) {
        
        int retget;
        
        if(a0 < 0 || a0 >= 16){
            System.out.println("wrong FD index");
            return -1;
        }
        
        if(a1 < 0){
            System.out.println("bad buffer address");
            return -1;
        }
        
        if(a2< 0){
            System.out.println("negative buffer size");
            return -1;
        }
        
        if (fdlist[a0].file == null){
            System.out.println("Empty File Descriptor");
            return -1;
        }
        
        /*if(fdlist[a0].opened == false){
        	System.out.println("file not opened yet, cannot write");
        	return -1;
         }*/
        
        int fileindex = a0;
        //String fn = fdlist[fileindex].fn;
        /*fileindex = -1;
         for(int i = 0; i < 16; i++){
         if(fdlist[i].fn.equals(fn)){
         if(fdlist[i].opened)
         fileindex = i;
         }
         }
         
         if(fileindex == -1)
         return -1;*/
        
        if (a2 == 0)
            return 0;
        
        FD fd = fdlist[fileindex];
        
        
        byte[] buffer = new byte[a2];
        
        int count = readVirtualMemory(a1, buffer);
        delay();
        
        if (count < 0){
            System.out.println("read failure");
            return -1;
        }
        
        retget = fd.file.write(buffer, 0, count);
        delay();
        
        if (retget < 0){
            System.out.println("write failure");
            return -1;
        }
        
        if(retget < a2){
            System.out.println("not all bytes written, disk may be full");
            return -1;
        }
        
        return retget;
    }
    
    //yiwen
    private int handleClose(int a0) {
        
        boolean retget = true;
        
        if (a0 < 0 || a0 >= 16){
            System.out.println("bad index");
            return -1;
        }
        
        FD fd = fdlist[a0];
        if (fdlist[a0].file == null){
            System.out.println("Empty File Descriptor");
            return -1;
        }
        
        /*if(fdlist[a0].opened == false){
        	System.out.println("File already closed");
        	return -1;
         }*/
        
        fd.file.close();
        fd.file = null;
        //fd.opened = false;
        /*String fn = fd.fn;
         
         
         
         boolean tempremove = fd.forRemove;
         if (tempremove) {
        	int count = 0;
        	for(int i = 0; i < 16; i++){
         if(fdlist[i].fn.equals(fn))
         count++;
        	}
        	if(count == 1){
         System.out.println("This is the last reference, file is deleted");
         retget = UserKernel.fileSystem.remove(fd.fn);
         delay();
        	}
         }
         
         fd.forRemove = false;*/
        FDcounter.put(fd.fn, FDcounter.get(fd.fn)-1);
        
        if(FDcounter.get(fd.fn) == 0){
            if(toRemove.contains(fd.fn)){
                retget = UserKernel.fileSystem.remove(fd.fn);
                if(retget){
                    toRemove.remove(fd.fn);
                    System.out.println("it is the last FD, successfully removed the file");
                }
            }else{
                FDcounter.remove(fd.fn);
            }
        }
        
        fd.fn = "";
        
        return retget ? 0 : -1;
    }
    
    //yiwen
    private int handleUnlink(int a0) {
        boolean retget = true;
        
        if (a0 < 0){
            System.out.println("bad address");
            return -1;
        }
        
        String fn = readVirtualMemoryString(a0, 256);
        delay();
        
        int fileindex = findFDfromName(fn);
        delay();
        
        if(toRemove.contains(fn)){
            System.out.println("already in toRemove, close it");
            return handleClose(fileindex);
        }
        
        if (fileindex < 0){
            System.out.println("file not found in this process but may exist in other processes");
            if(!FDcounter.containsKey(fn) || FDcounter.get(fn) == 0){
                retget = UserKernel.fileSystem.remove(fn);
                if(toRemove.contains(fn))
                    toRemove.remove(fn);
                if(FDcounter.containsKey(fn))
                    FDcounter.remove(fn);
                if(retget)
                    System.out.println("successfully removed");
                return retget ? 0 : -1;
            }
            toRemove.add(fn);
            System.out.println("add to toRemove");
            return -1;
        }
        else {
            System.out.println("file found in this process");
            /*boolean opened = false;
             for(int i = 0; i < 16; i++){
             if(fdlist[i].fn.equals(fn)){
             fdlist[i].forRemove = true;
             if(fdlist[i].opened){
             opened = true;
             }
             }
             }
             if(opened){
             System.out.println("file still open, wait to be closed");
             return -1;
             }
             
             for(int i = 0; i < 16; i++){
             if(fdlist[i].fn.equals(fn)){
             fdlist[i].fn = "";
             fdlist[i].opened = false;
             fdlist[i].forRemove = false;
             fdlist[i].file.close();
             fdlist[i].file = null;
             }
             }*/
            fdlist[fileindex].fn = "";
            fdlist[fileindex].file.close();
            fdlist[fileindex].file= null;
            
            FDcounter.put(fn, FDcounter.get(fn)-1);
            
            if(FDcounter.get(fn) == 0){
                retget = UserKernel.fileSystem.remove(fn);
                if(toRemove.contains(fn))
                    toRemove.remove(fn);
                FDcounter.remove(fn);
                System.out.println("successfully removed");
                return retget?0:-1;
            }else{
                System.out.println("other process contains FD, added to toRemove");
                toRemove.add(fn);
                retget = false;
            }
            //fdlist[fileindex].opened = false;
        }
        
        return retget ? 0 : -1;
    }
    
    // yiwen
    private void delay() {
        long time = Machine.timer().getTime();
        int amount = 1000;
        ThreadedKernel.alarm.waitUntil(amount);
        Lib.assertTrue(Machine.timer().getTime() >= time + amount);
    }
    
    // yiwen
    private int countFiles(String fn){
        int count = 0;
        for (int i = 0; i < 16; i++) {
            if (fdlist[i].fn.equals(fn))
                count++;
        }
        
        return count;
    }
    
	   //yiwen
    public int findFDfromName(String fn) {
        
        for (int i = 0; i < 16; i++) {
            if (fdlist[i].fn.equals(fn))
                return i;
        }
        
        return -1;
    }
    
    private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
    syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
    syscallRead = 6, syscallWrite = 7, syscallClose = 8,
    syscallUnlink = 9;
    
    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr>
     * <td>syscall#</td>
     * <td>syscall prototype</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td><tt>void halt();</tt></td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td><tt>void exit(int status);</tt></td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td><tt>int  join(int pid, int *status);</tt></td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td><tt>int  creat(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td><tt>int  open(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td><tt>int  read(int fd, char *buffer, int size);
     * 								</tt></td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td><tt>int  write(int fd, char *buffer, int size);
     * 								</tt></td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td><tt>int  close(int fd);</tt></td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td><tt>int  unlink(char *name);</tt></td>
     * </tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0 the first syscall argument.
     * @param a1 the second syscall argument.
     * @param a2 the third syscall argument.
     * @param a3 the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
                
                //yiwen
            case syscallCreate:
                return handleCreate(a0);
                
            case syscallOpen:
                return handleOpen(a0);
                
            case syscallRead:
                return handleRead(a0, a1, a2);
                
            case syscallWrite:
                return handleWrite(a0, a1, a2);
                
            case syscallClose:
                return handleClose(a0);
                
            case syscallUnlink:
                return handleUnlink(a0);
                // yiwen end
                
                
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
    
    
    
    
    //yiwen
    public int findNewFD() {
        for (int i = 0; i < 16; i++) {
            if (fdlist[i].file == null)
                return i; 
        }
        
        return -1;
    }
    //yiwen
    public class FD {
        public FD(){
            //forRemove = false;
            fn = "";
            file = null;
            //opened = false;
        }
        
        //public  boolean  forRemove;
        public  String   fn;
        public  OpenFile file;
        //public boolean opened;
    } 
    
    //yiwen
    private FD fdlist[] = new FD[16];
    
    public static Hashtable<String, Integer> FDcounter = new Hashtable<String, Integer>();
    public static HashSet<String> toRemove = new HashSet<String>();
    
    /*Start of Tao's code*/
    private int pid;
    
    /** A map from virtual memory to Translation entry: <vaddr, TranslationEntry>. */
    private HashMap<Integer, TranslationEntry> virtualToTransEntry = null;
    /*End of Tao's code*/
}
