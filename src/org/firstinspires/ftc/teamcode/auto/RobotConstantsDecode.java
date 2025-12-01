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

    // Obelisk patterns indexed by AprilTag identifiers
    // Tag family: 36h11
    public static final int MAX_ARTIFACTS_IN_PATTERN = 3;

    // Obelisk tag id 21: pattern GPP
    public static final Integer obeliskAprilTagGPP = Integer.valueOf(21);

    // Obelisk tag id 22: pattern PGP
    public static final Integer obeliskAprilTagPGP = Integer.valueOf(22);

    // Obelisk tag id 23: pattern PPG
    public static final Integer obeliskAprilTagPPG = Integer.valueOf(23);

    public static final Map<Integer, List<ArtifactColor>> obeliskPatterns;

    static {
        obeliskPatterns = new HashMap<>();
        obeliskPatterns.put(obeliskAprilTagGPP, new ArrayList<>(Arrays.asList(ArtifactColor.GREEN, ArtifactColor.PURPLE, ArtifactColor.PURPLE)));
        obeliskPatterns.put(obeliskAprilTagPGP, new ArrayList<>(Arrays.asList(ArtifactColor.PURPLE, ArtifactColor.GREEN, ArtifactColor.PURPLE)));
        obeliskPatterns.put(obeliskAprilTagPPG, new ArrayList<>(Arrays.asList(ArtifactColor.PURPLE, ArtifactColor.PURPLE, ArtifactColor.GREEN)));
    }

    // AprilTag identifiers
    // DECODE Tag family: 36h11
    // BLUE goal tag id 20
    // RED goal tag id 24
    public enum GoalAprilTag {
        TAG_NPOS(-1),
        BLUE_GOAL_TAG(20), RED_GOAL_TAG(24);

        private final int numericAprilTagId;

        GoalAprilTag(int pNumericId) {
            numericAprilTagId = pNumericId;
        }

        public int getNumericId() {
            return numericAprilTagId;
        }

        // Given the numeric id of an AprilTag return its
        // enumeration.
        public static GoalAprilTag getEnumValue(int pNumericId) {
            GoalAprilTag[] tagValues = GoalAprilTag.values();
            for (GoalAprilTag tagValue : tagValues) {
                if (tagValue.numericAprilTagId == pNumericId)
                    return tagValue;
            }

            return GoalAprilTag.TAG_NPOS; // no match
        }
    }
}