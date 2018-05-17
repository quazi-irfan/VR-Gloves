package net.qirfan;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.style.BaseStyles;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class GuiState extends AbstractAppState {

    static SerialPort serialPort;

    SimpleApplication application;
    Node guiNode;
    AppStateManager stateManager;

    TextField comPortTxt;
    static int tickValue;
    int torqueApplied;
    Label tickValueLbl;
    TextField torqueValueTxt;
    final int NO_TORQUE_VALUE = 0;
    final float ENCODER_TICK_MULTIPLIER = 6.42f;
    final int MOTOR_TORQUE_LIMIT = 225;

    GameState gameState;

    public GuiState(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.application = (SimpleApplication) app;
        this.guiNode = this.application.getGuiNode();
        this.stateManager = this.application.getStateManager();

        GuiGlobals.initialize(application);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        Container guiContainer = new Container();
        guiNode.attachChild(guiContainer);

        guiContainer.setLocalTranslation(100, 150, 0);

        comPortTxt = new TextField("COM6");
        guiContainer.addChild(comPortTxt);
        Button connectBtn = new Button("Connect");
        guiContainer.addChild(connectBtn);
        tickValueLbl = new Label("");
        guiContainer.addChild(tickValueLbl);
        Button calibrateBtn = new Button("Calibrate");
        guiContainer.addChild(calibrateBtn);
        torqueValueTxt = new TextField("80");
        guiContainer.addChild(torqueValueTxt);
        Button releaseBtn = new Button("Release");
        guiContainer.addChild(releaseBtn);

        connectBtn.addClickCommands(new Command<Button>() {
            public void execute(Button button) {
                GuiState.this.serialPort = new SerialPort(comPortTxt.getText());
//                GuiState.this.serialPort = new SerialPort("COM3");

                // setup
                try {
                    GuiState.this.serialPort.openPort();
                    GuiState.this.serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                } catch (SerialPortException e) {
                    System.out.println("Failed set settings to serial port.");
                    e.printStackTrace();
                }

                if (!GuiState.this.serialPort.isOpened()) {
                    System.out.println("Failed to open SerialPort");
                    System.exit(1);
                }

                // register listener to listen to the continuous tick values
                try {
                    GuiState.this.serialPort.addEventListener(serialEvent);
                } catch (SerialPortException e) {
                    System.out.println("Failed to register event listener.");
                    e.printStackTrace();
                }
            }
        });

        calibrateBtn.addClickCommands(new Command<Button>()
        {
            public void execute(Button button)
            {
                if (GuiState.this.serialPort != null && GuiState.this.serialPort.isOpened())
                {
                    // send 31338
                    writeIntValueToSerialPort(31338);

                    GuiState.this.tickValue = 0; // set tick value to internal variable
                    GuiState.this.tickValueLbl.setText(Integer.toString(tickValue)); // set tick value to GUI element

                    GuiState.this.calibrated = true; // so other parts of the system knows the joint is calibrated
                    writeIntValueToSerialPort(NO_TORQUE_VALUE);
                }
                else
                {
                    System.out.println("Initialize and open a SerialPort first.");
                }
            }
        });

        releaseBtn.addClickCommands(new Command<Button>() {
            public void execute(Button button) {
                GuiGlobals.getInstance().requestFocus(application.getRootNode());
            }
        });
    }


    SerialPortEventListener serialEvent = new SerialPortEventListener() {
        public void serialEvent(SerialPortEvent serialPortEvent) {
            try {
                byte[] byteData = serialPort.readBytes(2);
                String tempTickValueStr = Integer.toString((int) byteData[1]);

                GuiState.this.tickValue = parseAsInt(tempTickValueStr);
                GuiState.this.tickValueLbl.setText(String.valueOf(GuiState.this.tickValue));
            } catch (SerialPortException e) {
                System.out.println("Failed to read from the serial port");
                e.printStackTrace();
            }
        }
    };

    static boolean calibrated = false;
    static boolean alreadyHit = false; // assuming initially there is no collision
    static boolean alreadyReleased = false;

    Quaternion topJointQuaternion = new Quaternion();
    float temp = 0;
    static float timeInSec = 0;
    int previousTickValue;
    int torqueValue = 20;
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if(parseAsInt(torqueValueTxt.getText()) > MOTOR_TORQUE_LIMIT)
        {
            torqueValueTxt.setText(String.valueOf(MOTOR_TORQUE_LIMIT));
        } else if(parseAsInt(torqueValueTxt.getText()) < -MOTOR_TORQUE_LIMIT)
        {
            torqueValueTxt.setText(String.valueOf(-MOTOR_TORQUE_LIMIT));
        }

        if (calibrated) {
            temp += tpf;
            if(temp >= .25)
            {
                System.out.printf("%.2f %.2f %d\n", timeInSec, tickValue * ENCODER_TICK_MULTIPLIER, torqueApplied);
                timeInSec += .25f;
                temp = 0;
            }

            // update the joint with the incoming tick value
            topJointQuaternion.fromAngles(-(tickValue * ENCODER_TICK_MULTIPLIER) * FastMath.DEG_TO_RAD, 0, 0);
            application.getRootNode().getChild("TopJointRoot").setLocalRotation(topJointQuaternion);

            if(GameState.isBox){
                if (GameState.isHit && alreadyHit == false) {
                    writeIntValueToSerialPort(Integer.parseInt(torqueValueTxt.getText()));
                    GuiGlobals.getInstance().requestFocus(application.getRootNode());
                    alreadyHit = true;
                    alreadyReleased = false;
                    GameState.obstacleGeo.getMaterial().getParam("Color").setValue(ColorRGBA.Red);
                }

                if (!GameState.isHit && !alreadyReleased) {
                    writeIntValueToSerialPort(NO_TORQUE_VALUE);
                    alreadyReleased = true;
                    alreadyHit = false;
                }
            }
            else
            {
                if (GameState.isHit) {
                    // first collision
                    if (alreadyHit == false) {
                        writeIntValueToSerialPort(torqueValue);
                        alreadyHit = true;
                        previousTickValue = tickValue;
                    }

                    // digging deeper, repeat collision
                    if (alreadyHit && Math.abs(tickValue - previousTickValue) > 1) {
                        if (tickValue > previousTickValue)
                            torqueValue *= 2;
                        else
                            torqueValue /= 2;
                        writeIntValueToSerialPort(torqueValue);
                        previousTickValue = tickValue;
                    }

                    ColorRGBA c = new ColorRGBA();
                    c.set(255f, 204f - (tickValue * 10), 204f - (tickValue * 10), 1f);
                    GameState.obstacleGeo.getMaterial().getParam("Color").setValue(new ColorRGBA(c.getRed() / 255, c.getGreen() / 255, c.getBlue() / 255, 1f));

                }

                if(!GameState.isHit){
                    writeIntValueToSerialPort(NO_TORQUE_VALUE);
                    alreadyHit = false;
                    torqueValue = 20;
                }
            }
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        if (serialPort != null && serialPort.isOpened()) {
            try {
                writeIntValueToSerialPort(31337);
                serialPort.closePort();
            } catch (SerialPortException e) {
                System.out.println("Failed to close serial port.");
                e.printStackTrace();
            }
        }

    }

    private void writeIntValueToSerialPort(int value) {
        if(!(value == 31338 || value == 31337))
        {
            if(value >= MOTOR_TORQUE_LIMIT)
                value = MOTOR_TORQUE_LIMIT;
            else if(value <= -MOTOR_TORQUE_LIMIT)
                value = -MOTOR_TORQUE_LIMIT;
        }

        torqueApplied = value; // set the class variable, so other parts of the class knows the value of torque applied

        byte lowerEightByte = (byte) value;
        byte upperEightByte = (byte) (value >> 8);
        byte[] sendTorqueBytes = {upperEightByte, lowerEightByte};
        try {
            serialPort.writeBytes(sendTorqueBytes);
        } catch (SerialPortException e) {
            System.out.println("Error sending torque value:: " + value);
            e.printStackTrace();
        }
    }

    public int parseAsInt(String s)
    {
        int intValue = 0;
        try{
            intValue = Integer.parseInt(s);
        }catch (Exception e){
            intValue = 0;
        }

        return intValue;
    }

}
