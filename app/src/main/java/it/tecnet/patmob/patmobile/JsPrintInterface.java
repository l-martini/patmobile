package it.tecnet.patmob.patmobile;

import android.content.Context;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;

public class JsPrintInterface {
    Context mContext;
    String macAddress = null;
    /** Instantiate the interface and set the context */
    JsPrintInterface(Context c, String macAddress) {
        mContext = c;
        this.macAddress = macAddress;
    }

    @JavascriptInterface
    public void sendCpclOverBluetooth() {
        this.printOverBluetooth(this.macAddress);
    }

    private void printOverBluetooth(final String theBtMacAddress) {
        new Thread(new Runnable() {
            public void run() {
                try {

                    // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(theBtMacAddress);

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    StringBuilder sb = new StringBuilder("! 0 200 200 438 1\r\n");
                    sb.append("PW 575\r\n")
                            .append("TONE 0\r\n")
                            .append("SPEED 2\r\n")
                            .append("ON-FEED IGNORE\r\n")
                            .append("NO-PACE\r\n")
                            .append("BAR-SENSE\r\n")
                            .append("T 5 2 10 309 Numero Inventario: 123123123")
                            .append("BT 7 0 9\r\n")
                            .append("B 128 4 30 132 36 37 123456789012\r\n")
                            .append("PRINT\r\n");
                    String cpclData = sb.toString();

                    // Send the data to printer as a byte array.
                    thePrinterConn.write(cpclData.getBytes());

                    // Make sure the data got to the printer before closing the connection
                    Thread.sleep(500);

                    // Close the insecure connection to release resources.
                    thePrinterConn.close();

                    Looper.myLooper().quit();

                } catch (Exception e) {

                    // Handle communications error here.
                    e.printStackTrace();

                }
            }
        }).start();
    }
}

