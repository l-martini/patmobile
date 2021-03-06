package it.tecnet.patmob.patmobile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    WebView webView;
    ProgressBar progressBar;
    private PermissionRequest mPermissionRequest;
    SwipeRefreshLayout swipeRefreshLayout;
    BluetoothAdapter bluetoothAdapter;

    private final int REQUEST_PERMISSION_BLUETOOTH_CONNECT = 1;
    private final int REQUEST_PERMISSION_BLUETOOTH_ADMIN = 2;
    private final int REQUEST_PERMISSION_BLUETOOTH_SCAN = 3;
    private final int REQUEST_PERMISSION_CAMERA = 4;

    String macAddress = null;
    String applicationUrl = "https://tecnet.theoreo.it/patmob2/";

    @SuppressLint({"SetJavaScriptEnabled", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set bt adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null || (bluetoothAdapter != null && !bluetoothAdapter.isEnabled())) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage("Bluetooth non disponibile, le funzionalit?? di stampa non saranno disponibili");
            dlgAlert.setTitle("Attenzione");
            dlgAlert.setPositiveButton("OK", null);
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
        } else {
            // this.showBluetoothConnectPermission();
            // this.showBluetoothAdminPermission();
            // this.showBluetoothScanPermission();
            this.showCameraPermission();

            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : devices) {
                System.out.println("\n Device : " + device.getName() + " , " + device);
                if (device.getName().equalsIgnoreCase("XXZSJ221000063")) {
                    this.macAddress = device.getAddress();
                }
            }
        }

        getSupportActionBar().hide();

        webView = findViewById(R.id.web);
        progressBar = findViewById(R.id.progress);
        swipeRefreshLayout = findViewById(R.id.swipe);

        // setup webview
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new JsPrintInterface(this, this.macAddress), "Android");
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new myWebViewclient());
        webView.setWebChromeClient(new WebChromeClient() {
            // Grant permissions for cam
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                mPermissionRequest = request;
                final String[] requestedResources = request.getResources();
                for (String r : requestedResources) {
                    if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        // In this sample, we only accept video capture request.
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Consenti l'accesso alla telecamera")
                                .setPositiveButton("Permetti", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        mPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                                    }
                                })
                                .setNegativeButton("Nega", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        mPermissionRequest.deny();
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();

                        break;
                    }
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                super.onPermissionRequestCanceled(request);
                Toast.makeText(MainActivity.this,"Impossibile avviare telecamera, permesso negato",Toast.LENGTH_SHORT).show();
            }
        });

        webView.loadUrl(applicationUrl);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        webView.loadUrl(applicationUrl);
                    }
                }, 3000);
            }
        });

        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_orange_dark),
                getResources().getColor(android.R.color.holo_green_dark),
                getResources().getColor(android.R.color.holo_red_dark)
        );

        //  ==================== START HERE: THIS CODE BLOCK IS TO ENABLE FILE DOWNLOAD FROM THE WEB. YOU CAN COMMENT IT OUT IF YOUR APPLICATION DOES NOT REQUIRE FILE DOWNLOAD. IT WAS ADDED ON REQUEST ======//

        webView.setDownloadListener(new DownloadListener() {
            String fileName = MimeTypeMap.getFileExtensionFromUrl(applicationUrl);

            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {

                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Scaricamento in  corso...", //To notify the Client that the file is being downloaded
                        Toast.LENGTH_LONG).show();

            }
        });
        //  ==================== END HERE: THIS CODE BLOCK IS TO ENABLE FILE DOWNLOAD FROM THE WEB. YOU CAN COMMENT IT OUT IF YOUR APPLICATION DOES NOT REQUIRE FILE DOWNLOAD. IT WAS ADDED ON REQUEST ======//

    }

    private void showBluetoothConnectPermission() {
        System.out.println("###### SHOW BT CONNECT PERMISSION @@@@@@");
        int permissionCheck = ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            System.out.println("###### PERMISSION BT CONNECT GRANTED @@@@@@");
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_CONNECT)) {
                System.out.println("###### SHOW BT CONNECT DIALOG @@@@@@");
                new AlertDialog.Builder(this).setTitle("Permission needed").setMessage("Bluetooth connect is needed to print").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_PERMISSION_BLUETOOTH_CONNECT);
                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
            } else {
                requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_PERMISSION_BLUETOOTH_CONNECT);
            }
        }
    }

    private void showBluetoothAdminPermission() {
        System.out.println("###### SHOW BT ADMIN PERMISSION @@@@@@");
        int permissionCheck = ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.BLUETOOTH_ADMIN);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            System.out.println("###### PERMISSION ADMIN GRANTED @@@@@@");
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_ADMIN)) {
                System.out.println("###### SHOW BT ADMIN DIALOG @@@@@@");

                new AlertDialog.Builder(this).setTitle("Permission needed").setMessage("Bluetooth admin is needed to print").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermission(Manifest.permission.BLUETOOTH_ADMIN, REQUEST_PERMISSION_BLUETOOTH_ADMIN);
                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
            } else {
                requestPermission(Manifest.permission.BLUETOOTH_ADMIN, REQUEST_PERMISSION_BLUETOOTH_ADMIN);
            }
        }
    }

    private void showCameraPermission() {
        System.out.println("###### SHOW CAMERA PERMISSION @@@@@@");
        int permissionCheck = ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            System.out.println("###### PERMISSION CAMERA GRANTED @@@@@@");
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                System.out.println("###### SHOW CAMERA DIALOG @@@@@@");

                new AlertDialog.Builder(this).setTitle("Permission needed").setMessage("Camera is needed to read qr code").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermission(Manifest.permission.CAMERA, REQUEST_PERMISSION_CAMERA);
                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
            } else {
                requestPermission(Manifest.permission.CAMERA, REQUEST_PERMISSION_CAMERA);
            }
        }
    }

    private void showBluetoothScanPermission() {
        System.out.println("###### SHOW BT SCAN PERMISSION @@@@@@");
        int permissionCheck = ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.BLUETOOTH_SCAN);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            System.out.println("###### PERMISSION SCAN GRANTED @@@@@@");
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_ADMIN)) {
                System.out.println("###### SHOW BT SCAN DIALOG @@@@@@");

                new AlertDialog.Builder(this).setTitle("Permission needed").setMessage("Bluetooth scan is needed to print").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermission(Manifest.permission.BLUETOOTH_SCAN, REQUEST_PERMISSION_BLUETOOTH_SCAN);
                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
            } else {
                requestPermission(Manifest.permission.BLUETOOTH_SCAN, REQUEST_PERMISSION_BLUETOOTH_SCAN);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_BLUETOOTH_CONNECT:
            case REQUEST_PERMISSION_BLUETOOTH_ADMIN:
            case REQUEST_PERMISSION_BLUETOOTH_SCAN:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{permissionName}, permissionRequestCode);
    }

    public class myWebViewclient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getApplicationContext(), "Nessuna connessione internet", Toast.LENGTH_LONG).show();
            webView.loadUrl("file:///android_asset/lost.html");
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            handler.cancel();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}