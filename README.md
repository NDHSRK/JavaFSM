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
