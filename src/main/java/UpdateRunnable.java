import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import net.java.games.input.Component;
import net.java.games.input.Controller;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateRunnable implements Runnable {

    private Controller controller;
    private SerialPort serialPort;

    public UpdateRunnable(Controller controller, SerialPort serialPort) {
        this.controller = controller;
        this.serialPort = serialPort;
    }

    private AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public void start() {
        serialPort.openPort();
        worker = new Thread(this);
        worker.start();
        System.out.println("\u001b[32;1mEnabled!\u001b[0m");
    }

    public void stop() {
        running.set(false);
        serialPort.closePort();
        System.out.println("\u001b[31;1mDisabled!\u001b[0m");
    }

    @Override
    public void run() {
        running.set(true);
        System.out.println("RUNNING");
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            @Override
            public void serialEvent(SerialPortEvent event)
            {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;
                byte[] newData = new byte[serialPort.bytesAvailable()];
                int numRead = serialPort.readBytes(newData, newData.length);
//                System.out.println(new String(newData));
            }
        });
        while(running.get()) {
//            System.out.println("SENDING");
            controller.poll();

//            System.out.println(controller.getComponent(Component.Identifier.Axis.X).getPollData());
//            System.out.println(controller.getComponent(Component.Identifier.Axis.Y).getPollData());
            float x = controller.getComponent(Component.Identifier.Axis.X).getPollData();
            float y = controller.getComponent(Component.Identifier.Axis.Y).getPollData();

            boolean aButton = controller.getComponent(Component.Identifier.Button.A).getPollData() == 1.0f;
            boolean bButton = controller.getComponent(Component.Identifier.Button.B).getPollData() == 1.0f;
            boolean xButton = controller.getComponent(Component.Identifier.Button.X).getPollData() == 1.0f;
            boolean yButton = controller.getComponent(Component.Identifier.Button.Y).getPollData() == 1.0f;

            byte buttons = (byte) ((aButton ? 1 : 0) + (bButton ? 2 : 0) + (xButton ? 4 : 0) + (yButton ? 8 : 0));

            double left = y - x;
            double right = y + x;

            if(left > 1) left = 1;
            if(left < -1) left = -1;
            if(right > 1) right = 1;
            if(right < -1) right = -1;

            byte leftVelocity = (byte) Math.floor(left * 127.99);
            byte rightVelocity = (byte) Math.floor(right * 127.99);
//            System.out.println("L="+leftVelocity);

            byte[] packet = new byte[]{leftVelocity, rightVelocity, buttons, (byte)255};

//            System.out.println(Arrays.toString(packet));

            serialPort.writeBytes(packet, 4);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
}
