package com.example.contact_app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ContactService contactService;
    private ContactAdapter adapter;
    private ListView lv;
    private ArrayList<Contact> contactList = new ArrayList<>();
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private ContactsObserver contactsObserver; // ✅ Stocker l'observer
    private ImageView imgAdd;
    private CardView cardView;
    private TextView nameEditText, numberEditText;
    private Button saveButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imgAdd = findViewById(R.id.imgAdd);
        cardView = findViewById(R.id.cardView);
        nameEditText = findViewById(R.id.nameEditText);
        numberEditText = findViewById(R.id.numberEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        lv = findViewById(R.id.listView);
        adapter = new ContactAdapter(this, contactList);
        lv.setAdapter(adapter);


        if (imgAdd == null || cardView == null) {
            Log.e("MainActivity", "imgAdd ou cardView est null. Vérifiez les IDs dans le fichier XML.");
            return;
        }

        cardView.setVisibility(View.GONE);

        imgAdd.setOnClickListener(v -> {
            cardView.setVisibility(View.VISIBLE);
            findViewById(R.id.overlay).setVisibility(View.VISIBLE);
        });

        saveButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String number = numberEditText.getText().toString().trim();
            if (name.isEmpty() || number.isEmpty()) {
                Toast.makeText(MainActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            Contact newContact = new Contact(name, number);
            contactList.add(newContact);

            // Trier la liste après ajout
            Collections.sort(contactList, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

            // Mettre à jour l'adaptateur
            adapter.updateOriginalList(contactList);

            adapter.notifyDataSetChanged();

            saveContactsToDatabase(newContact);
            syncContacts();
            nameEditText.setText("");
            numberEditText.setText("");
            cardView.setVisibility(View.GONE);
            findViewById(R.id.overlay).setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, "Contact ajouté", Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> {
            cardView.setVisibility(View.GONE);
            findViewById(R.id.overlay).setVisibility(View.GONE);
            nameEditText.setText("");
            numberEditText.setText("");
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Contact selectedContact = adapter.getItem(position);
            String phoneNumber = selectedContact.getPhone();

            ArrayAdapter<String> adapter2 = getStringArrayAdapter();

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setAdapter(adapter2, (dialog, which) -> {
                switch (which) {
                    case 0:
                        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            startActivity(callIntent);
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                        }
                        break;
                    case 1:
                        Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                        smsIntent.putExtra("sms_body", "");
                        startActivity(smsIntent);
                        break;
                }
            });
            builder.show();
        });


        // Récupérer le ContentResolver pour les contacts
        ContentResolver contentResolver = getContentResolver();
        contactsObserver = new ContactsObserver(new Handler(), this); // ✅ Création unique
        contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // Configuration Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.6:8088")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        contactService = retrofit.create(ContactService.class);

        getContactsFromApi();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            syncContacts();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contactsObserver != null) { // ✅ Désenregistrer proprement
            getContentResolver().unregisterContentObserver(contactsObserver);
        }
    }

    public List<Contact> getContactList() {
        return contactList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();

        searchView.setQueryHint("Rechercher un contact");

        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setHintTextColor(Color.BLACK);
        searchEditText.setTextColor(Color.BLACK);
        searchEditText.setTypeface(Typeface.SANS_SERIF);
        searchEditText.setTextSize(16);

        View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        searchPlate.setBackgroundResource(R.drawable.circle_background);

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        return true;
    }

//    public void getContacts() {
//        ContentResolver contentResolver = getContentResolver();
//        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                null, null, null, null);
//
//        if (cursor != null) {
//            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
//            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//
//            if (nameIndex != -1 && numberIndex != -1) {
//                while (cursor.moveToNext()) {
//                    String name = cursor.getString(nameIndex);
//                    String number = cursor.getString(numberIndex);
//                    Contact contact = new Contact(name, number);
//                    contactList.add(contact);
//
//                }
//            }
//            cursor.close();
//        }
//
//        // Trier la liste par ordre alphabétique
//        Collections.sort(contactList, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
//
//        adapter.notifyDataSetChanged();
//        checkAndSyncContacts(); // Vérifier et synchroniser les contacts distants
//
//    }

    private void getContactsFromApi() {
        Log.d("API", "Tentative de récupération des contacts...");
        contactService.getContacts().enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                Log.d("API", "Réponse reçue. Code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d("API", "Nombre de contacts: " + (response.body() != null ? response.body().size() : 0));
                    List<Contact> contacts = response.body();
                    if (contacts != null) {
                        contactList.clear();
                        contactList.addAll(contacts);
                        adapter.updateOriginalList(contactList);
                        Collections.sort(contactList, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
                        adapter.notifyDataSetChanged();

                    } else {
                        Log.e("API", "Erreur: Liste de contacts vide");
                    }
                } else {
                    try {
                        Log.e("API", "Erreur: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                Log.e("API", "Échec: " + t.getMessage(), t);
            }
        });
    }

//    private void checkAndSyncContacts() {
//        contactService.getContacts().enqueue(new Callback<List<Contact>>() {
//            @Override
//            public void onResponse(@NonNull Call<List<Contact>> call, @NonNull Response<List<Contact>> response) {
//                if (response.isSuccessful()) {
//                    List<Contact> remoteContacts = response.body();
//                    if (remoteContacts != null) {
//                        for (Contact remoteContact : remoteContacts) {
//                            boolean exists = false;
//                            for (Contact localContact : contactList) {
//                                if (localContact.getName().equals(remoteContact.getName()) &&
//                                        localContact.getPhone().equals(remoteContact.getPhone())) {
//                                    exists = true;
//                                    break;
//                                }
//                            }
//                            if (!exists) {
//                                contactList.add(remoteContact);
//                            }
//                        }
//                        adapter.notifyDataSetChanged();
//                    }
//                } else {
//                    Log.d("TAG", "Erreur lors de la récupération des contacts distants");
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<List<Contact>> call, @NonNull Throwable t) {
//                Log.d("TAG", "Erreur de connexion : " + t.getMessage());
//            }
//        });
//    }


    public void saveContactsToDatabase(Contact contact) {
        contactService.addContact(contact).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Contact> call, @NonNull Response<Contact> response) {
                if (response.isSuccessful()) {
                    new Handler().postDelayed(() -> getContactsFromApi(), 1000);
                    Log.d("TAG", "Contact ajouté avec succès");
                } else {
                    Log.d("TAG", "Erreur lors de l'ajout du contact");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Contact> call, @NonNull Throwable t) {
                Log.d("TAG", "Erreur de connexion : " + t.getMessage());
            }
        });
    }

    private void syncContacts() {
        getContactsFromApi();
        adapter.updateOriginalList(contactList);
        adapter.notifyDataSetChanged();
    }

    private ArrayAdapter<String> getStringArrayAdapter() {
        String[] options = {"Appeler", "Envoyer un SMS"};
        int[] icons = {R.drawable.phone, R.drawable.sms};

        return new ArrayAdapter<>(MainActivity.this, R.layout.dialog_item, R.id.text, options) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ImageView icon = view.findViewById(R.id.icon);
                icon.setImageResource(icons[position]);
                return view;
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContactsFromApi();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
