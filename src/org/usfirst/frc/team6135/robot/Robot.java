
package org.usfirst.frc.team6135.robot;

import org.usfirst.frc.team6135.robot.commands.autocommands.DrivePastBaseline;
import org.usfirst.frc.team6135.robot.commands.autocommands.SwitchAligned;
import org.usfirst.frc.team6135.robot.commands.autocommands.SwitchMiddle;
import org.usfirst.frc.team6135.robot.commands.autocommands.SwitchSide;
import org.usfirst.frc.team6135.robot.commands.autonomous.FollowTrajectory;
import org.usfirst.frc.team6135.robot.commands.defaultcommands.TeleopDrive;
import org.usfirst.frc.team6135.robot.misc.AutoPaths;
import org.usfirst.frc.team6135.robot.misc.Autonomous;
import org.usfirst.frc.team6135.robot.misc.PowerUpGameData;
import org.usfirst.frc.team6135.robot.subsystems.DriveTrain;
import org.usfirst.frc.team6135.robot.subsystems.ElevatorSubsystem;
import org.usfirst.frc.team6135.robot.subsystems.GearShiftSubsystem;
import org.usfirst.frc.team6135.robot.subsystems.IntakeSubsystem;
import org.usfirst.frc.team6135.robot.subsystems.VisionSubsystem;
import org.usfirst.frc.team6135.robot.subsystems.WristSubsystem;

import com.ctre.phoenix.motorcontrol.NeutralMode;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import robot.pathfinder.core.RobotSpecs;
import robot.pathfinder.core.TrajectoryParams;
import robot.pathfinder.core.Waypoint;
import robot.pathfinder.core.path.PathType;
import robot.pathfinder.core.trajectory.TankDriveTrajectory;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends TimedRobot {
	
	//SUBSYSTEMS
	public static OI oi;
	public static DriveTrain drive;
	public static IntakeSubsystem intakeSubsystem;
	public static GearShiftSubsystem gearShiftSubsystem;
	public static ElevatorSubsystem elevatorSubsystem;
	public static WristSubsystem wristSubsystem;
	public static VisionSubsystem visionSubsystem;
	
	public static Alliance color; //Red or Blue
	public static int station; //Driver station number (1, 2 or 3)
	public static PowerUpGameData gameData; //Used to tell the locations of the switch/scale plates
	
	public static final int LEFT = 1;
	public static final int RIGHT = -1;
	
	public enum GenericLocation {
		LEFT,
		MIDDLE,
		RIGHT;
	}
	public enum Auto {
		DEBUG,
		BASELINE,
		ALIGNED,
		SIDE,
		MIDDLE;
		
		static final Autonomous baseline = (location, gameData) -> {
			return new DrivePastBaseline();
		};
		static final Autonomous aligned = (location, gameData) -> {
			return gameData.getOurSwitchLocation() == location ? new SwitchAligned() : new FollowTrajectory(AutoPaths.aligned_driveForward);
		};
		static final Autonomous middle = (location, gameData) -> {
			return new SwitchMiddle(gameData.getOurSwitchLocation());
		};
		static final Autonomous side = (location, gameData) -> {
			return gameData.getOurSwitchLocation() == location ? new SwitchSide(gameData.getOurSwitchLocation()) : new DrivePastBaseline();
		};
		static final Autonomous debug = (location, gameData) -> {
			RobotSpecs robotSpecs = new RobotSpecs(100.0, 80.0, 23.0);
			TrajectoryParams params = new TrajectoryParams();
			params.waypoints = new Waypoint[] {
					new Waypoint(0.0, 0.0, Math.PI / 2),
					new Waypoint(48.0, 120.0, Math.PI / 2),
			};
			params.alpha = 200.0;
			params.segmentCount = 1000;
			params.isTank = true;
			params.pathType = PathType.QUINTIC_HERMITE;
			TankDriveTrajectory trajectory = new TankDriveTrajectory(robotSpecs, params);
			return new FollowTrajectory(trajectory);
		};
		
		public Autonomous getAutonomous() {
			switch(this) {
			case DEBUG:
				return debug;
			case BASELINE:
				return baseline;
			case ALIGNED:
				return aligned;
			case SIDE:
				return side;
			case MIDDLE:
				return middle;
			default:
				throw new RuntimeException("Invalid auto enum value");
			}
		}
	}
	
	//Autonomous command choosers
	public static SendableChooser<GenericLocation> robotLocationChooser = new SendableChooser<>();
	public static SendableChooser<Auto> prewrittenAutoChooser = new SendableChooser<>();
	
	//This keeps track of the command that runs in autonomous so we can cancel it when entering teleop
	static Command autonomousCommand;
	
	public static boolean inDebugMode = false;
	static void putTunables() {
		if(!inDebugMode)
			return;
		//Output these values to the SmartDashboard for tuning
		//They will show up as modifiable text boxes on the SmartDashboard
		SmartDashboard.putNumber("Path Follower kP", FollowTrajectory.kP);
		SmartDashboard.putNumber("Path Follower kD", FollowTrajectory.kD);
		SmartDashboard.putNumber("Path Follower kV", FollowTrajectory.kV);
		SmartDashboard.putNumber("Path Follower kA", FollowTrajectory.kA);
		SmartDashboard.putNumber("Teleop Drive Ramp Band", TeleopDrive.rampBand);
	}
	static void updateTunables() {
		//Read the textbox values and overwrite the old ones with the new ones
		//The value is not changed by default (new value = old value)
		FollowTrajectory.kP = SmartDashboard.getNumber("Path Follower kP", FollowTrajectory.kP);
		FollowTrajectory.kD = SmartDashboard.getNumber("Path Follower kD", FollowTrajectory.kD);
		FollowTrajectory.kV = SmartDashboard.getNumber("Path Follower kV", FollowTrajectory.kV);
		FollowTrajectory.kA = SmartDashboard.getNumber("Path Follower kA", FollowTrajectory.kA);
		TeleopDrive.rampBand = SmartDashboard.getNumber("Teleop Drive Ramp Band", TeleopDrive.rampBand);
	}
	
	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	@Override
	public void robotInit() {
		//Initialize our subsystems
		RobotMap.init();
		drive = new DriveTrain();
		intakeSubsystem = new IntakeSubsystem();
		gearShiftSubsystem = new GearShiftSubsystem();
		elevatorSubsystem = new ElevatorSubsystem();
		wristSubsystem = new WristSubsystem();
		
		//Initialize camera stream and vision subsystem
        visionSubsystem = new VisionSubsystem(CameraServer.getInstance().startAutomaticCapture());
        //Set camera config
        visionSubsystem.setMode(VisionSubsystem.Mode.VIDEO); //For vision, change to Mode.VISION
        
        //Get the team's colour and station number
        station = DriverStation.getInstance().getLocation();
        color = DriverStation.getInstance().getAlliance();
        //Game data is retrieved later
        
        //OI must be initialized after all subsystems, because it maps buttons to commands, and those commands
        //require subsystems to be properly initialized
        oi = new OI();
        
        //Generate all autonomous paths/trajectories
        //Takes quite some time so we do it here instead of in the autos themselves
        AutoPaths.generateAll(RobotMap.specs);
        
        //Initialize the correct auto chooser
        //Pre-written autos
        initAutoChooser();
		
        //Output the tunable values
		putTunables();
	}
	
	/**
	 * Initializes the autonomous chooser for pre-written autos
	 */
	public static void initAutoChooser() {
		//Add options to choosers
		robotLocationChooser.addObject("Left", GenericLocation.LEFT);
		robotLocationChooser.addDefault("Middle", GenericLocation.MIDDLE);
		robotLocationChooser.addObject("Right", GenericLocation.RIGHT);
		
		prewrittenAutoChooser.addDefault("Drive Past Baseline", Auto.BASELINE);
		prewrittenAutoChooser.addObject("Switch Auto: Side", Auto.SIDE);
		prewrittenAutoChooser.addObject("Switch Auto: Aligned", Auto.ALIGNED);
		prewrittenAutoChooser.addObject("Switch Auto: Middle", Auto.MIDDLE);
		
		prewrittenAutoChooser.addObject("Debug Auto", Auto.DEBUG);
		
		//Display the choosers by sending them over the SmartDashboard
		SmartDashboard.putData("Auto Mode", prewrittenAutoChooser);
		SmartDashboard.putData("Robot Location", robotLocationChooser);
	}
	
	public static double leftMaxVel, rightMaxVel, leftMaxAccel, rightMaxAccel;
	/**
	 * This function is called periodically during all robot modes.
	 * Code that needs to be run regardless of mode, such as the printing of information,
	 * can be put here.
	 */
	@Override
	public void robotPeriodic() {
		if(inDebugMode) {
			SmartDashboard.putNumber("Left Distance", Robot.drive.getLeftDistance());
	    	SmartDashboard.putNumber("Right Distance", Robot.drive.getRightDistance());
	    	double leftVel = Robot.drive.getLeftSpeed();
	    	double rightVel = Robot.drive.getRightSpeed();
	    	SmartDashboard.putNumber("Left Speed", leftVel);
	    	SmartDashboard.putNumber("Right Speed", rightVel);
	    	double[] accel = Robot.drive.getAccelerations();
	    	SmartDashboard.putNumber("Left Acceleration", accel[0]);
	    	SmartDashboard.putNumber("Right Acceleration", accel[1]);
	    	
	    	//Update max speed and accel
	    	if(Math.abs(leftVel) > Math.abs(leftMaxVel)) {
	    		leftMaxVel = leftVel;
	    	}
	    	if(Math.abs(rightVel) > Math.abs(rightMaxVel)) {
	    		rightMaxVel = rightVel;
	    	}
	    	if(Math.abs(accel[0]) > Math.abs(leftMaxAccel)) {
	    		leftMaxAccel = accel[0];
	    	}
	    	if(Math.abs(accel[1]) > Math.abs(rightMaxAccel)) {
	    		rightMaxAccel = accel[1];
	    	}
	    	SmartDashboard.putNumber("Left Max Speed", leftMaxVel);
	    	SmartDashboard.putNumber("Right Max Speed", rightMaxVel);
	    	SmartDashboard.putNumber("Left Max Acceleration", leftMaxAccel);
	    	SmartDashboard.putNumber("Right Max Acceleration", rightMaxAccel);
    	
	    	SmartDashboard.putBoolean("Elevator Top Switch", Robot.elevatorSubsystem.notAtTop());
	    	SmartDashboard.putBoolean("Elevator Bottom Switch", Robot.elevatorSubsystem.notAtBottom());
	    	SmartDashboard.putBoolean("Wrist Switch", Robot.wristSubsystem.notAtTop());
		}
    	
    	SmartDashboard.putBoolean("Drive Ramping", TeleopDrive.isRamped());
	}

	/**
	 * This function is called once each time the robot enters Disabled mode.
	 * You can use it to reset any subsystem information you want to clear when
	 * the robot is disabled.
	 */
	@Override
	public void disabledInit() {
		RobotMap.setAllMotorNeuralModes(NeutralMode.Coast);
	}

	@Override
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
	}

	/**
	 * Called when the robot first enters autonomous mode. Start auto commands here.
	 */
	@Override
	public void autonomousInit() {
		
		GenericLocation location = robotLocationChooser.getSelected();
		Auto autoMode = prewrittenAutoChooser.getSelected();
		
		//Set motors to be in brake mode
		RobotMap.setAllMotorNeuralModes(NeutralMode.Brake);
		
		runSetAuto(location, autoMode);

		//Set camera config
		visionSubsystem.setMode(VisionSubsystem.Mode.VISION);
		
		updateTunables();
	}
	
	/**
	 * Starts an autonomous command by setting the static member {@code autonomousCommand} to the given command
	 * and calling {@link Command#start() start()} on it.
	 * @param autoCommand - The command to start
	 */
	public static void startAutoCommand(Command autoCommand) {
		autonomousCommand = autoCommand;
		autoCommand.start();
	}
	public static void runSetAuto(GenericLocation location, Auto mode) {
		//Retrieve the locations of the switch plates (in game data)
		gameData = PowerUpGameData.getGameDataFromDS();
		if(gameData.isValid()) {
			System.out.println("Auto command: " + mode.getAutonomous().getCommand(location, gameData).toString());
			startAutoCommand(mode.getAutonomous().getCommand(location, gameData));
		}
	}

	/**
	 * This function is called periodically during autonomous
	 */
	@Override
	public void autonomousPeriodic() {
		Scheduler.getInstance().run();
	}

	@Override
	public void teleopInit() {
		// This makes sure that the autonomous stops running when
		// teleop starts running. If you want the autonomous to
		// continue until interrupted by another command, remove
		// this line or comment it out.
		if (autonomousCommand != null)
			autonomousCommand.cancel();
		
		//Set camera config
		visionSubsystem.setMode(VisionSubsystem.Mode.VIDEO);
		RobotMap.setAllMotorNeuralModes(NeutralMode.Coast);
		
		updateTunables();
	}

	/**
	 * This function is called periodically during operator control
	 */
	@Override
	public void teleopPeriodic() {
		Scheduler.getInstance().run();
	}

	/**
	 * This function is called periodically during test mode
	 */
	@Override
	public void testPeriodic() {
	}
}
