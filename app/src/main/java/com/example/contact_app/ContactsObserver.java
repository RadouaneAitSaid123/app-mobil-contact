package com.example.contact_app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;

import java.util.List;

public class ContactsObserver extends ContentObserver {
    private Context context;

    public ContactsObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

        // Vérifier si le changement concerne l'ajout de nouveaux contacts
        if (uri != null && uri.toString().contains(ContactsContract.Contacts.CONTENT_URI.toString())) {
            // Récupérer les nouveaux contacts
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null
            );

            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                if (nameIndex != -1 && numberIndex != -1) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(nameIndex);
                        String number = cursor.getString(numberIndex);
                        Contact newContact = new Contact(name, number);

                        // Ajouter à la liste de l'activité
                        ((MainActivity) context).getContactList().add(newContact);

                        // Ajouter à la base de données distante si nécessaire
                        ((MainActivity) context).saveContactsToDatabase(newContact);
                    }
                }
                cursor.close();  // N'oubliez pas de fermer le curseur pour éviter les fuites de mémoire
            }
        }
    }
}



