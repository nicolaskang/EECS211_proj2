package nachos.userprog;

import java.util.HashMap;
import java.util.LinkedList;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.machine.Coff;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
        super();
    }
    
    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
        super.initialize(args);
        
        console = new SynchConsole(Machine.console());
        
        Machine.processor().setExceptionHandler(new Runnable() {
            public void run() {
                exceptionHandler();
            }
        });
        
        /* Start of Tao's code */
        
        System.out.println( Machine.processor().getNumPhysPages()); //64
        
        // Initialize the free virtual memory.
        for (int currentPageIndex = 0; currentPageIndex < Machine.processor().getNumPhysPages(); currentPageIndex++)
            freePages.add(new TranslationEntry(0, currentPageIndex, false, false, false, false));
        
        freePagesLock = new Lock();
        /* End of Tao's code */
    }
    
    /**
     * Test the console device.
     */
    public void selfTest() {
        super.selfTest();
        
        System.out.println("Testing the console device. Typed characters");
        System.out.println("will be echoed until q is typed.");
        
        char c;
        
        do {
            c = (char) console.readByte(true);
            console.writeByte(c);
        } while (c != 'q');
        
        System.out.println("");
    }
    
    /**
     * Returns the current process.
     *
     * @return the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
        if (!(KThread.currentThread() instanceof UThread))
            return null;
        
        return ((UThread) KThread.currentThread()).process;
    }
    
    /**
     * The exception handler. This handler is called by the processor whenever a
     * user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
        Lib.assertTrue(KThread.currentThread() instanceof UThread);
        
        UserProcess process = ((UThread) KThread.currentThread()).process;
        int cause = Machine.processor().readRegister(Processor.regCause);
        process.handleException(cause);
    }
    
    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see nachos.machine.Machine#getShellProgramName
     */
    public void run() {
        super.run();
        
        UserProcess process = UserProcess.newUserProcess();
        
        String shellProgram = Machine.getShellProgramName();
        Lib.assertTrue(process.execute(shellProgram, new String[] {}));
        
        KThread.currentThread().finish();
    }
    
    /* Start of Tao’s code */
    /**
     * Acquire the requested number of pages from the free pages in <tt>this</tt>.
     * @param numPages
     * @return returnPages(TranslationEntry [])
     * @throws InadequatePagesException
     */
    TranslationEntry[] acquirePages(int numPages) throws InadequatePagesException {
        TranslationEntry[] returnPages = null;
        
        freePagesLock.acquire();
        
        if (!freePages.isEmpty() && freePages.size() >= numPages) {
            returnPages = new TranslationEntry[numPages];
            
            for (int i = 0; i < numPages; ++i) {
                returnPages[i] = freePages.remove();
                returnPages[i].valid = true;
            }
        }
        
        freePagesLock.release();
        
        if (returnPages == null)
            throw new InadequatePagesException();
        else
            return returnPages;
    }
    
    /**
     * Return the pages in <tt>pageTable</tt> to the free pages in <tt>this</tt>.
     *
     * This should only be called by a dying (i.e. completed) <tt>UserProcess</tt> instance.
     * @param pageTable
     */
    void releasePages(TranslationEntry[] pageTable) {
        freePagesLock.acquire();
        
        for (TranslationEntry te : pageTable) {
            freePages.add(te);
            te.valid = false;
        }
        
        freePagesLock.release();
    }
    /* End of Tao’s code */
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }
    
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;
    
    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    
    
    
    //huang's code
    
    private static HashMap<Integer, UserProcess> ProcessMap = new HashMap<Integer, UserProcess>();
    
    public static void addProcess(int id, UserProcess up) {
        UserProcess old = ProcessMap.put(id, up);
    }
    
    public static void removeProcess(int id) {
        UserProcess old = ProcessMap.remove(id);
    }
    
    public static UserProcess getProcess(int id) {
        return ProcessMap.get(id);
    }
    
    /* Start of Tao’s code */
    
    /**
     * A set of free pages in this kernel
     */
    private LinkedList<TranslationEntry> freePages = new LinkedList<TranslationEntry>();
    
    /**
     * A lock to protect access to the linked list of free pages.
     */
    private Lock freePagesLock;
    
    static class InadequatePagesException extends Exception {
        private static final long serialVersionUID = 6256028192007727092L;
    }
    /* End of Tao’s code */ 
}
