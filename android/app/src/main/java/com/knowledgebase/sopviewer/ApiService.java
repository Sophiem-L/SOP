package com.knowledgebase.sopviewer;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public interface ApiService {
    @POST("api/login")
    Call<LoginResponse> login(@Header("Authorization") String token);

    @GET("api/documents")
    Call<List<Document>> getDocuments(@Header("Authorization") String token, @retrofit2.http.Query("q") String query);

    @POST("api/documents/{id}/favorite")
    Call<Void> toggleFavorite(@Path("id") int id, @Header("Authorization") String token);

    @GET("api/documents/favorites")
    Call<List<Document>> getFavorites(@Header("Authorization") String token);

    @GET("api/articles")
    Call<List<Article>> getArticles(@Header("Authorization") String token, @retrofit2.http.Query("q") String query);

    @GET("api/sops")
    Call<List<Sop>> getSops(@Header("Authorization") String token, @retrofit2.http.Query("q") String query);

    @GET("api/categories")
    Call<List<Category>> getCategories(@Header("Authorization") String token);

    @Multipart
    @POST("api/documents")
    Call<ResponseBody> createDocument(
            @Header("Authorization") String token,
            @Part("title") RequestBody title,
            @Part("type") RequestBody type,
            @Part("category_id") RequestBody categoryId,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part file);
}
