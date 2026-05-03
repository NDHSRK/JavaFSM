package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.teamcode.auto.fsm.DecodeIntakeFSM;

public class Main {

    //**TODO In our application we usually have two gamepads,
    // FTC Gamepad1 and Gamepad2. To support this configuration
    // on Windows we'll need a hub.

    public static void main(String[] args) throws Exception {

        // Get the number of gamepads and the number of artifacts
        // to intake from the command line.
        // --numGamepads [1 | 2]

        if (args.length != 2 || !args[0].equals("--artifactsToIntake"))
            throw new AutonomousRobotException("Main", "Invalid argument list");

        int numGamepads = Integer.parseInt(args[1]);

        // TestGamepads testGamepads = new TestGamepads(numGamepads);

        DecodeIntakeFSM intakeFSM = new DecodeIntakeFSM(numGamepads);

        // Generic version.
        //DecodeGenericFSM teleOpFSM = new DecodeGenericFSM(numGamepads);
        //teleOpFSM.runIntakeFSM();

        System.exit(0);
    }

}
