package raj.rak.offlinetranslator.bean;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.List;

public class Language {

    private int code;
    private String language;
    public Context context;

    public Language(int code, String language) {
        this.code = code;
        this.language = language;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public static String getLanguageNameForCode(Context context, String s) {
        String json = null;
        if (context == null)
            return s;
        try {
            InputStream is = context.getAssets().open("language.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (object.getString("Code").equalsIgnoreCase(s)) {
                    return object.getString("Language");
                }
            }
            return s;
        } catch (Exception e) {
            return s;
        }
    }

    public static Language getLanguageByCode(int code, List<Language> languages) {
        for (Language language : languages)
            if (language.getCode() == code)
                return language;
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        if (code == 0) return "Select Source Language";
        if (code == -1) return "Select Destination Language";
        return getLanguageNameForCode(context, language);
    }
}

