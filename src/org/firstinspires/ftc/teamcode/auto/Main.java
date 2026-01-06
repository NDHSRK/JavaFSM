package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.auto.fsm.FSM6Container;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        //SDL2ControllerManager controllerManager;
        //controllerManager = new SDL2ControllerManager();
        // failed on the above line with
        //Exception in thread "main" java.lang.NoClassDefFoundError: com/badlogic/gdx/controllers/ControllerManager
        // Apparently more dependencies need to be added but I can't find an example
        // with a complete build.gradle file or a list of jars/dlls.
        /*
        The class ControllerManager belongs to the optional gdx-controllers extension. You need to verify it is present in your build configuration file (e.g., build.gradle for Gradle projects or pom.xml for Maven projects)
         */

        //## 1/15/2022 GenericFSM, GenericFSM2 - 4 are retained as documentation for various
        // attempts and false starts.
        FSM6Container fsm6C = new FSM6Container();
        fsm6C.testFSM6();
    }
}
