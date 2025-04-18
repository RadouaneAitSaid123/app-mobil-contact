package com.example.contact_app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ContactAdapter extends ArrayAdapter<Contact> implements Filterable {

    private List<Contact> originalList; // Tous les contacts
    private List<Contact> filteredList; // Résultat du filtre
    private ContactFilter contactFilter;

    private int[] colors = {
            Color.parseColor("#F44336"), // rouge
            Color.parseColor("#E91E63"), // rose
            Color.parseColor("#9C27B0"), // violet
            Color.parseColor("#3F51B5"), // bleu
            Color.parseColor("#03A9F4"), // bleu clair
            Color.parseColor("#009688"), // vert sarcelle
            Color.parseColor("#4CAF50"), // vert
            Color.parseColor("#FF9800"), // orange
            Color.parseColor("#795548"), // brun
            Color.parseColor("#607D8B")  // bleu gris
    };

    public ContactAdapter(Context context, List<Contact> contacts) {
        super(context, 0, contacts);
        this.originalList = new ArrayList<>(contacts); // copie de la liste d'origine
        this.filteredList = new ArrayList<>(contacts); // initialisation de la liste filtrée
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Contact getItem(int position) {
        return filteredList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Contact contact = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_contact, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvNumber = convertView.findViewById(R.id.tvNumber);
        TextView iconText = convertView.findViewById(R.id.iconText);
        int colorIndex = new Random().nextInt(colors.length);

        GradientDrawable bgShape = (GradientDrawable) iconText.getBackground();
        bgShape.setColor(colors[colorIndex]);

        assert contact != null;

        tvName.setText(contact.getName());
        tvNumber.setText(contact.getPhone());

        String firstLetter = contact.getName().substring(0, 1).toUpperCase();
        iconText.setText(firstLetter);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (contactFilter == null) {
            contactFilter = new ContactFilter();
        }
        return contactFilter;
    }

    private class ContactFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Contact> filtered = new ArrayList<>();
            List<String> existingIds = new ArrayList<>(); // Pour éviter les doublons

            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(originalList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Contact contact : originalList) {
                    // Vérifiez d'abord si le contact n'a pas déjà été ajouté
                    if (!existingIds.contains(contact.getName())) { // Ajoutez un getId() à votre modèle Contact
                        if (contact.getName().toLowerCase().contains(filterPattern) ||
                                contact.getPhone().toLowerCase().contains(filterPattern)) {
                            filtered.add(contact);
                            existingIds.add(contact.getName().toString()); // Ajoutez l'ID du contact à la liste des existants
                        }
                    }
                }
            }

            Collections.sort(filtered, (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList.clear();
            if (results.values != null) {
                filteredList.addAll((List<Contact>) results.values);
            }
            notifyDataSetChanged();
        }
    }

    public void updateOriginalList(List<Contact> newList) {
        this.originalList = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }
}
