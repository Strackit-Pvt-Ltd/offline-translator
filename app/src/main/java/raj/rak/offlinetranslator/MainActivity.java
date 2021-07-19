package raj.rak.offlinetranslator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import raj.rak.offlinetranslator.bean.Language;
import raj.rak.offlinetranslator.util.Extra;

public class MainActivity extends AppCompatActivity {

    Spinner sourceSpinner, destinationSpinner;
    EditText sourceText, destinationText;
    SharedPreferences preferences;

    List<Language> sourceLanguages = new ArrayList<>();
    List<Language> destinationLanguages = new ArrayList<>();
    FirebaseLanguageIdentification languageIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("languages", MODE_PRIVATE);
        sourceSpinner = findViewById(R.id.sourceSpinner);
        destinationSpinner = findViewById(R.id.destinationSpinner);
        sourceText = findViewById(R.id.sourceText);
        destinationText = findViewById(R.id.destinationText);
        languageIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        init();
    }

    private void init() {
        Set<Integer> integers = FirebaseTranslateLanguage.getAllLanguages();
        for (Integer integer : integers) {
            Language language = new Language(integer, FirebaseTranslateLanguage.languageCodeForLanguage(integer));
            language.context = this;
            sourceLanguages.add(language);
            destinationLanguages.add(language);
        }
        sourceLanguages.add(0, new Language(0, "source"));
        sourceSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sourceLanguages));
        destinationLanguages.add(0, new Language(-1, "destination"));
        destinationSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, destinationLanguages));
        sourceSpinner.setSelection(
            sourceLanguages.indexOf(Language.getLanguageByCode(preferences.getInt("source", 0), sourceLanguages))
        );
        destinationSpinner.setSelection(
                destinationLanguages.indexOf(Language.getLanguageByCode(preferences.getInt("destination", 0), destinationLanguages))
        );
        sourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                processLanguage(s.toString());
            }
        });
    }

    private void processLanguage(final String s) {
        final Language sourceLanguage = (Language) sourceSpinner.getSelectedItem();
        final Language destinationLanguage = (Language) destinationSpinner.getSelectedItem();
        if (sourceLanguage.getCode() < 1) {
            Extra.print(this, "Please Select Source Language");
            return;
        }
        if (destinationLanguage.getCode() < 1) {
            Extra.print(this, "Please Select Destination Language");
            return;
        }
        preferences.edit().putInt("source", sourceLanguage.getCode())
                .putInt("destination", destinationLanguage.getCode()).commit();
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage.getCode())
                .setTargetLanguage(destinationLanguage.getCode()).build();
        final FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        FirebaseModelManager modelManager = FirebaseModelManager.getInstance();
        modelManager.getDownloadedModels(FirebaseTranslateRemoteModel.class)
                .addOnSuccessListener(new OnSuccessListener<Set<FirebaseTranslateRemoteModel>>() {
                    @Override
                    public void onSuccess(Set<FirebaseTranslateRemoteModel> firebaseTranslateRemoteModels) {
                        boolean source = false, destination = false;
                        for (FirebaseTranslateRemoteModel firebaseTranslateRemoteModel : firebaseTranslateRemoteModels) {
                            if (firebaseTranslateRemoteModel.getLanguage() == destinationLanguage.getCode()) {
                                destination = true;
                            }
                            if (firebaseTranslateRemoteModel.getLanguage() == sourceLanguage.getCode()) {
                                source = true;
                            }
                        }
                        if (destination && source) {
                            translate(translator, s); return;
                        }
                        download(translator, s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void translate(FirebaseTranslator translator, String s) {
        translator.translate(s).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                destinationText.setText(s);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Extra.print(MainActivity.this, "Failed to Translate");
                Log.i("Translate", e.toString());
            }
        });
    }

    private void identifyLanguage(String s) {
        Extra.loading(MainActivity.this, "Identifying Language");
        languageIdentifier.identifyLanguage(s).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Extra.cancelLoading();
                Log.i("Identified Language", s);
                for (Language language : sourceLanguages) {
                    if (language.getLanguage().equalsIgnoreCase(s)) {
                        sourceSpinner.setSelection(sourceLanguages.indexOf(language));
                        return;
                    }
                }
                Extra.print(getApplicationContext(), "Failed to Identify Language");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Extra.cancelLoading();
                Log.i("Identified Language", e.toString());
                Extra.print(getApplicationContext(), "Failed to Identify Language");
            }
        });
    }

    private void download(final FirebaseTranslator translator, final String s) {
        Extra.loading(this, "Downloading Language... Check Notification");
        translator.downloadModelIfNeeded(new FirebaseModelDownloadConditions.Builder().build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        translate(translator, s);
                        Extra.cancelLoading();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Extra.print(MainActivity.this, "Failed to Download");
                Log.i("Translator Download", e.toString());
                Extra.cancelLoading();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.downloads) {
            startActivity(new Intent(this, DownloadsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
