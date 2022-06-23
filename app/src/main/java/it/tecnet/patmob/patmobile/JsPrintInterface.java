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

    /**
     * Instantiate the interface and set the context
     */
    JsPrintInterface(Context c, String macAddress) {
        mContext = c;
        this.macAddress = macAddress;
    }

    @JavascriptInterface
    public void sendCpclOverBluetooth(String numeroInventario, String modello) {
        this.printOverBluetooth(this.macAddress, numeroInventario, modello);
    }

    private void printOverBluetooth(final String theBtMacAddress, final String numeroInventario, final String modello) {
        new Thread(new Runnable() {
            public void run() {
                try {

                    // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(theBtMacAddress);

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    StringBuilder sb = new StringBuilder("! 0 200 200 405 1\r\n");
                    sb.append("PW 575\r\n")
                            .append("TONE 0\r\n")
                            .append("SPEED 2\r\n")
                            .append("ON-FEED IGNORE\r\n")
                            .append("NO-PACE\r\n")
                            .append("BAR-SENSE\r\n")
                            .append("CENTER\r\n")
                            .append("B QR 203 50 M 2 U 8\r\n")
                            .append("MA,99999999\r\n")
                            .append("ENDQR\r\n")
                            .append("CENTER\r\n")
                            .append("T 5 2 0 233 Numero Inventario:")
                            .append(numeroInventario)
                            .append("\r\n")
                            .append("CENTER\r\n")
                            .append("T 5 0 0 292 ")
                            .append(modello)
                            .append("\r\n")
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

