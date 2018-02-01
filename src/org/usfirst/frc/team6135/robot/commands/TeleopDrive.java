package org.usfirst.frc.team6135.robot.commands;

import org.usfirst.frc.team6135.robot.OI;
import org.usfirst.frc.team6135.robot.Robot;
import org.usfirst.frc.team6135.robot.RobotMap;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 *	Handles driving
 *	Called as a default command by DriveTrain
 */
public class TeleopDrive extends Command {
	
	private static final int X_AXIS = 4;
    private static final int Y_AXIS = 1;
    
    private static final double DEADZONE = 0.15;

    public TeleopDrive() {
        // Use requires() here to declare subsystem dependencies
        requires(Robot.drive);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	double x = Math.abs(OI.xboxController.getRawAxis(X_AXIS))>DEADZONE?OI.xboxController.getRawAxis(X_AXIS):0;
        double y = Math.abs(OI.xboxController.getRawAxis(Y_AXIS))>DEADZONE?-OI.xboxController.getRawAxis(Y_AXIS):0;
        double l = Math.max(-RobotMap.DRIVE_SPEED, Math.min(RobotMap.DRIVE_SPEED, y + x));//constrain to [-1,1]
        double r = Math.max(-RobotMap.DRIVE_SPEED, Math.min(RobotMap.DRIVE_SPEED, y - x));
        Robot.drive.setMotorsVBus(l, r);
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.drive.setMotorsVBus(0, 0);
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	Robot.drive.setMotorsVBus(0, 0);
    }
}
