package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

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
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		super.selfTest();

		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");

		//Yiwen
		char c;
		UserProcess up = null;
		UserProcess up1 = null;
		up = UserProcess.newUserProcess();
		up1 = UserProcess.newUserProcess();
		byte[] filename1 = new byte[256];
		byte[] filename2 = new byte[256];
		filename1[0] = 66;
		filename1[1] = 67;
		filename2[0] = 69;
		int fileadd1 = 0x2488;
		int fileadd2 = 0x2000;
		up.writeVirtualMemory(fileadd1, filename1, 0, 256);
		up.writeVirtualMemory(fileadd2, filename2, 0, 256);
		do {
			c = (char) console.readByte(true);
			//console.writeByte(c);
			
			switch(c){
			case 'c': System.out.println("Create File Index: " + up.handleSyscall(4, fileadd1, 0, 0, 0));break;
			case 'o': System.out.println("Open File Index: " + up.handleSyscall(5, fileadd1, 0, 0, 0));break;
			case 'r': System.out.println("Read File bytes: " + up.handleSyscall(6, 2, 0x4111, 10, 0));break;
			case 'w': System.out.println("Write File bytes: " + up.handleSyscall(7, 2, 0x5111, 10, 0));break;
			case 's': System.out.println("Close File return value: " + up.handleSyscall(8, 2, 0, 0, 0));break;
			//case 'u': System.out.println("Unlink File return value: " + UserKernel.fileSystem.remove("BC"));break;
			case 'u': System.out.println("Unlink File return value: " + up.handleSyscall(9, fileadd1, 0, 0, 0));break;
			case '1': System.out.println("Create File Index: " + up1.handleSyscall(4, fileadd1, 0, 0, 0));break;
			case '2': System.out.println("Open File Index: " + up1.handleSyscall(5, fileadd1, 0, 0, 0));break;
			case '3': System.out.println("Read File bytes: " + up1.handleSyscall(6, 2, 0x3355, 10, 0));break;
			case '4': System.out.println("Write File bytes: " + up1.handleSyscall(7, 2, 0x3665, 10, 0));break;
			case '5': System.out.println("Close File return value: " + up1.handleSyscall(8, 2, 0, 0, 0));break;
			case '6': System.out.println("Unlink File return value: " + up1.handleSyscall(9, fileadd1, 0, 0, 0));break;
			}
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
}
