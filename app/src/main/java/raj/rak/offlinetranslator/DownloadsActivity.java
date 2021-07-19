package raj.rak.offlinetranslator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import raj.rak.offlinetranslator.bean.Language;
import raj.rak.offlinetranslator.util.Extra;

public class DownloadsActivity extends AppCompatActivity {

    FirebaseModelManager modelManager;
    List<Language> languages = new ArrayList<>();
    ListView listView;
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);
        getSupportActionBar().setTitle("Downloads");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        listView = findViewById(R.id.list);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, languages);
        listView.setAdapter(arrayAdapter);
        modelManager = FirebaseModelManager.getInstance();
        modelManager.getDownloadedModels(FirebaseTranslateRemoteModel.class)
                .addOnSuccessListener(new OnSuccessListener<Set<FirebaseTranslateRemoteModel>>() {
                    @Override
                    public void onSuccess(Set<FirebaseTranslateRemoteModel> firebaseTranslateRemoteModels) {
                        for (FirebaseTranslateRemoteModel firebaseTranslateRemoteModel : firebaseTranslateRemoteModels) {
                            Language language = new Language(firebaseTranslateRemoteModel.getLanguage(),
                                    firebaseTranslateRemoteModel.getLanguageCode());
                            languages.add(language);
                        }
                        arrayAdapter.notifyDataSetChanged();
                        if (languages.size() < 1) {
                            Extra.print(DownloadsActivity.this, "No Donwloads Found");
                            finish();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Extra.print(DownloadsActivity.this, "Failed to Get Downloads");
                Log.i("Downloads", e.toString());
                finish();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Language language = languages.get(position);
                delete(language);
            }
        });
    }

    private void delete(final Language language) {
        if (language.getLanguage().equalsIgnoreCase("en")) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete " + Language.getLanguageNameForCode(this, language.getLanguage()));
        builder.setMessage("Do You want to delete?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Extra.loading(DownloadsActivity.this, "Deleting");
                FirebaseTranslateRemoteModel model = new FirebaseTranslateRemoteModel.Builder(language.getCode()).build();
                modelManager.deleteDownloadedModel(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        languages.remove(language);
                        arrayAdapter.notifyDataSetChanged();
                        Extra.cancelLoading();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Extra.print(DownloadsActivity.this, "Failed to Delete");
                        Log.i("Downloads Delete", e.toString());
                        Extra.cancelLoading();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false).create().show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}
