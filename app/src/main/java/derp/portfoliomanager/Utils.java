package derp.portfoliomanager;


import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

public class Utils {
    //todo limit on how many times this can be displayed in x seconds,remove fatal?
    static void showError(final Context context, String title, String msg, final boolean fatal) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (fatal)
                                    ((MainActivity)context).finish();
                            }
                        }
                ).show();
    }
}
