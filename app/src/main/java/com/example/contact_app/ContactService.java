package com.example.contact_app;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ContactService {
    @GET("/api/contacts/all")
    Call<List<Contact>> getContacts();

    @POST("/api/contacts/one")
    Call<Contact> addContact(@Body Contact contact);


}
