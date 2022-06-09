package it.tecnet.patmob.patmobile;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class JsPrintInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    JsPrintInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void stampaEtichetta(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }
}

