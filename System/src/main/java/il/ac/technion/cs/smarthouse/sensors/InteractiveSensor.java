package il.ac.technion.cs.smarthouse.sensors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import il.ac.technion.cs.smarthouse.networking.messages.MessageType;
import il.ac.technion.cs.smarthouse.networking.messages.SensorMessage;
import il.ac.technion.cs.smarthouse.networking.messages.SensorMessage.IllegalMessageBaseExecption;

/**
 * This class represents a sensor that can get instructions and operate
 * accordingly.
 * 
 * @author Yarden
 * @since 31.3.17
 */
public abstract class InteractiveSensor extends Sensor {
    private static Logger log = LoggerFactory.getLogger(InteractiveSensor.class);

    protected int instPort;
    protected Socket instSocket;
    protected PrintWriter instOut;
    protected BufferedReader instIn;
    protected InstructionHandler handler;
    protected long period;

    public InteractiveSensor(final String commname, final String id, 
                                final List<String> observationSendingPaths, final List<String> instructionRecievingPaths,
                                final int systemPort, final int instPort) {
        super(commname, id, observationSendingPaths, instructionRecievingPaths, systemPort);
        this.instPort = instPort;
    }

    /**
     * Registers the sensor its instructions TCP connection with the system.
     * This method must be called before any instructions are sent.
     * 
     * @return <code>true</code> if registration was successful,
     *         <code>false</code> otherwise
     */
    public boolean registerInstructions() {
        try {
            instSocket = new Socket(systemIP, instPort);
            instOut = new PrintWriter(instSocket.getOutputStream(), true);
            instIn = new BufferedReader(new InputStreamReader(instSocket.getInputStream()));
        } catch (final IOException e) {
            log.error("I/O error occurred when the sensor's instructions socket was created", e);
        }
        
        try{
            final String $ = new SensorMessage(MessageType.REGISTRATION, this).send(instOut, instIn);
            return $ != null && new SensorMessage($).isSuccesful();
        }catch(IllegalMessageBaseExecption e){}
        return false;
    }

    /**
     * Sets the operation to be made when instruction is received. This method
     * must be called before sending instructions.
     */
    public void setInstructionHandler(final InstructionHandler h) {
        handler = h;
    }

    /**
     * If there is an incoming instruction, extracts it and executes it.
     * 
     * @return <code>true</code> if the instruction was completed successfully
     *         (or if there is no waiting instruction), <code>false</code>
     *         otherwise
     */
    public boolean operate() {

        try {
            if(instIn.ready()){
               String[] inst = instIn.readLine().split("");
               //todo: elia document external the instruction format
               return handler.applyInstruction(inst[0], Arrays.asList(inst).stream().skip(1).reduce("", (a,b)-> a+ " "+b)); 
            }
            return false;
        } catch (final IOException e) {
            log.error("I/O error occurred", e);
            return false;
        }
    }

    /**
     * Checks for waiting instructions by polling the connection repeatedly
     * every specified period of time.
     * 
     * @param p
     *            The polling period, in milliseconds
     */
    public void pollInstructions(final long p) {
        period = p;
        new Timer().schedule(new TimerTask() {
                                    
                                    @Override
                                    public void run() {
                                       operate();
                                        
                                    }
                                }
                                            , 0, period);
    }
}
