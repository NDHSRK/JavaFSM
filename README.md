This IntelliJ project contains three implementations of a Finite State
Machine for FTC.

BasicTeleOpFSM\
A reconstruction of the FSM from Brogan Pratt's video https://www.youtube.com/watch?v=RweqIqouYqM
adapted to the use of JInput for the gamepad and the enumeration for the states
declared in DecodeGenericFSM.

DecodeIntakeFSM\
A more elaborate FSM that uses Pratt's switch/case structure with the inclusion
of "guard conditions", "action routines", and both a "next state" and a "next 
event". The purpose is to align this version of the FSM more closely with the
general model of a finite state machine. This FSM implements a subset of the
logic from Team 4348's TeleOp for the Decode game.

DecodeGenericFSM\
An FSM that supports generic enumerations for states and events and generic
functional Suppliers for guard conditions and action routines. This FSM relies
on its own dispatching mechanism and does not require a switch/case.

Configure JInput into an IntelliJ Project - based on a Google AI Overview

To use JInput with IntelliJ IDEA, you need to
add the JInput library and its native dependencies to your project. The general process involves adding the JInput JAR file as a library and then specifying the location of the required native dynamic-link libraries (.dll files for Windows, .jnilib for macOS, etc.).

Step 1: Obtain JInput Files
First, download the JInput library files. You will need:

    1. The main jinput.jar file.
    2. The native library files for your operating system (e.g., jinput-dx8.dll, jinput-dx8_64.dll, jinput-raw.dll, jinput-raw_64.dll for Windows). These are often provided in a separate JAR (e.g., jinput-platform-<version>-natives-all.jar) which you will need to unzip to extract the DLLs.

    **!!** [5/11/2026 The jar files are not available on the JInput website or Github repo. I put in a Google query "where can I find jinput-2.0.10-natives-all.jar for JInput?" and got two links:
    Where to Download/Find It:
     Maven Central Repository (Sonatype): You can directly download the jar file, including jinput-2.0.10-natives-all.jar, from this official repository.
     SciView Jars Repository: A mirror or similar repository that hosts the jinput-2.0.10-natives-all.jar (listed as jinput-2.0.10-natives-all.jar-20240422112900).]
    
    **!!** [5/11/2026 I tried maven central but Firefox gave me a security error.
    So I got the two jar files from https://sites.imagej.net/SciView/jars/] 

Step 2: Add JInput JAR to your IntelliJ Project

    Open Project Structure: In IntelliJ IDEA, go to File > Project Structure (or press Ctrl+Alt+Shift+S).
    Select Libraries: In the left-hand menu, select Libraries under the Platform Settings section.
    Add New Library: Click the + icon and select Java.
    Select jinput.jar: Navigate to the directory where you saved jinput.jar, select it, and click OK.
    Specify Module: When prompted, select the module(s) you want to add the library to and click OK.

    **!!** [5/9/2026 had to add jinput-2.0.10-natives-all.jar in the same manner]

Step 3: Link Native Libraries
The crucial step for JInput is specifying where the native libraries (DLLs, etc.) are located, or you will encounter a java.library.path error.
Option A: Use the java.library.path VM parameter (Recommended for running/debugging)

    Open Run/Debug Configurations: Go to Run > Edit Configurations....
    Add VM Options: In the VM options field, add the following line, replacing /path/to/natives/dir with the actual path to the folder containing your extracted native files (e.g., jinput-dx8.dll):

    -Djava.library.path=/path/to/natives/dir ->
 
    **!!** [5/9/2026 *the next line is required*]
    -Djava.library.path="C:\IdeaProjects\JavaFSM\Files\jinput\jinput-2.0.10-natives-all"

    Apply Changes: Click Apply and then OK. 

**!!** [5/9/2026 do *not* use this option]
Option B: Configure the Library in Project Structure

    Open Project Structure: Go to File > Project Structure > Libraries.
    Edit Native Library Location: Expand the jinput.jar library entry. [**!!** 5/9/2026 the entry did *not* expand]
    Specify Path: Double-click Native library location.
    Select Directory: Choose the folder containing your native files and click OK.

To see the results of a correct configuration see the screenshots in the Files directory of this project.
