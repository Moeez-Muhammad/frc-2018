package org.usfirst.frc.team6135.robot.commands.teleoputils;

import org.usfirst.frc.team6135.robot.Robot;
import org.usfirst.frc.team6135.robot.commands.teleopcommands.WristAnalog;
import org.usfirst.frc.team6135.robot.commands.teleopcommands.WristAnalogAdjust;

import edu.wpi.first.wpilibj.command.Command;

/**
 *	When this command is started and allowed to run for a set amount of time without being interrupted,
 *	it changes the default command for the wrist subsystem between the auto adjust variant and the 
 *	plain variant.
 */
public class EmergencySwitch extends Command {

	//The amount of time this command must keep running to disable auto adjustment
	protected static final double HOLD_REQUIREMENT_EMERGENCY = 2.0;
	//The amount of time this command must keep running to re-enable auto adjustment
	protected static final double HOLD_REQUIREMENT_CANCEL = 5.0;
	
    public EmergencySwitch() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	requires(Robot.wristSubsystem);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
    	Command oldDefault = Robot.wristSubsystem.getDefaultCommand();
    	double requirement = oldDefault instanceof WristAnalogAdjust ? HOLD_REQUIREMENT_EMERGENCY : HOLD_REQUIREMENT_CANCEL;
    	if(timeSinceInitialized() > requirement) {
    		Robot.wristSubsystem.setDefaultCommand(oldDefault instanceof WristAnalogAdjust ? new WristAnalog() : new WristAnalogAdjust());
    		return true;
    	}
    	return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
