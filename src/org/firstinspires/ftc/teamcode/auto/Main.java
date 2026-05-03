package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.*;
import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.teamcode.auto.fsm.BasicIntakeFSM;
import org.firstinspires.ftc.teamcode.auto.fsm.BasicTeleOpFSM;
import org.firstinspires.ftc.teamcode.auto.fsm.DecodeTeleOpFSM;

public class Main {

    //**TODO In our application we usually have two gamepads,
    // FTC Gamepad1 and Gamepad2. To support this configuration
    // on Windows we'll need a hub.

    public static void main(String[] args) throws Exception {

        //**TODO TestGamepads only needs --numGamepads [1 | 2]
        // TestGamepads testGamepads = new TestGamepads(numGamepads);

        // Get the number of gamepads and the number of artifacts
        // to intake from the command line.
        // --numGamepads [1 | 2]
        // --artifactsToIntake [0 - 3]

        if (args.length != 4 || !args[0].equals("--numGamepads") || !args[2].equals("--artifactsToIntake"))
            throw new AutonomousRobotException("Main", "Invalid argument list");

        int numGamepads = Integer.parseInt(args[1]);
        int artifactsToIntake = Integer.parseInt(args[3]);

        BasicIntakeFSM intakeFSM = new BasicIntakeFSM(numGamepads, artifactsToIntake);

        // Generic version.
        //DecodeTeleOpFSM teleOpFSM = new DecodeTeleOpFSM(numGamepads, artifactsToIntake);
        //teleOpFSM.runIntakeFSM();

        System.exit(0);
    }

}
