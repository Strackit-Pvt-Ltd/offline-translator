package raj.rak.offlinetranslator.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

public class Extra {

    private static Toast toast;
    private static AlertDialog dialog;

    public static void print(Context context, String s) {
        if (toast != null)
            toast.cancel();
        toast = Toast.makeText(context, s, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void loading(Context context, String s) {
        cancelLoading();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(s);
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(15, 15, 15, 15);
        layout.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(context);
        layout.addView(progressBar);
        builder.setView(layout);
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }

    public static void cancelLoading() {
        try {
            dialog.cancel();
        } catch (Exception e) {}
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void alert(Context context, String title, String message, String ok) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null)
            builder.setTitle(title);
        if (message != null)
            builder.setMessage(message);
        if (ok != null)
            builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        builder.create().show();
    }

    public static String removeCharacter(String s){
        if (s == null || s.trim().isEmpty()) return "";
        char[] num=s.toCharArray();
        s="";
        for(char c:num){
            if(Character.isDigit(c) || Character.isLetter(c))
                s+=c;
        }
        return s;
    }

    public static Object asyncHttp(String link, String data) throws IOException {
        URL url = new URL(link);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(data);
        writer.flush();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String s1;
        while ((s1 = reader.readLine()) != null)
            builder.append(s1);
        return builder.toString();
    }

    public static int getRandomInt() {
        return new Random().nextInt(60000) + 1;
    }
}
