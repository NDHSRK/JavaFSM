package org.firstinspires.ftc.teamcode.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobotConstantsDecode {

    public enum OpMode {
        // Decode Autonomous OpModes
        BLUE_NEAR(OpModeType.COMPETITION),
        BLUE_FAR(OpModeType.COMPETITION),
        RED_NEAR(OpModeType.COMPETITION),
        RED_FAR(OpModeType.COMPETITION),

        TEST(OpModeType.AUTO_TEST), TEST_PRE_MATCH(OpModeType.AUTO_TEST),
        AUTO_NO_DRIVE(OpModeType.AUTO_TEST),

        // TeleOp OpModes
        TELEOP_FULL(OpModeType.COMPETITION),
        TELEOP_NO_DRIVE(OpModeType.TELEOP_TEST),
        TELEOP_TEST(OpModeType.TELEOP_TEST),

        // Indication that an OpMode has not yet been assigned.
        OPMODE_NPOS(OpModeType.PSEUDO_OPMODE);

        public enum OpModeType {COMPETITION, AUTO_TEST, TELEOP_TEST, PSEUDO_OPMODE}

        private final OpModeType opModeType;

        OpMode(OpModeType pOpModeType) {
            opModeType = pOpModeType;
        }

        public OpModeType getOpModeType() {
            return opModeType;
        }
    }

    // The InternalCameraId identifies each unique camera and its position on
    // the robot.
    public enum InternalWebcamId {
        FRONT_WEBCAM, REAR_WEBCAM, WEBCAM_NPOS
    }

    //**TODO Does DECODE need CAMERA_STREAM_PREVIEW from CenterStage?
    public enum ProcessorIdentifier {
        RAW_FRAME, APRIL_TAG, PROCESSOR_NPOS
    }

    public enum LimelightPipeline {
        TEST(0), IDLE(1), CUSTOM_PYTHON_SNAPSCRIPT(2), OBELISK(3),
        BLUE_GOAL(4), RED_GOAL(5), NPOS(-1);

        private final int pipelineIndex;

        LimelightPipeline(int pPipelineIndex) {
            pipelineIndex = pPipelineIndex;
        }

        public int getPipelineIndex() {
            return pipelineIndex;
        }
    }

    // Associate an indicator light to show with each artifact.
    // The color does not have to be the same as that of the artifact.
    public enum ArtifactColor {
        GREEN, PURPLE
    }

    public static final int MAX_ARTIFACTS_IN_PATTERN = 3;
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 3;

}