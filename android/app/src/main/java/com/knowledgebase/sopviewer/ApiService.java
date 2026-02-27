package com.knowledgebase.sopviewer;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
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

        @POST("api/logout")
        Call<Void> logout(@Header("Authorization") String token);

        @GET("api/documents")
        Call<List<Document>> getDocuments(@Header("Authorization") String token,
                        @retrofit2.http.Query("q") String query,
                        @retrofit2.http.Query("sort") String sort,
                        @retrofit2.http.Query("category_id") Integer categoryId);

        @POST("api/documents/{id}/favorite")
        Call<Void> toggleFavorite(@Path("id") int id, @Header("Authorization") String token);

        @GET("api/documents/favorites")
        Call<List<Document>> getFavorites(@Header("Authorization") String token);

        @GET("api/articles")
        Call<List<Article>> getArticles(@Header("Authorization") String token, @retrofit2.http.Query("q") String query,
                        @retrofit2.http.Query("sort") String sort);

        @GET("api/sops")
        Call<List<Sop>> getSops(@Header("Authorization") String token, @retrofit2.http.Query("q") String query,
                        @retrofit2.http.Query("sort") String sort);

        @GET("api/categories")
        Call<List<Category>> getCategories(@Header("Authorization") String token);

        @Multipart
        @POST("api/documents")
        Call<ResponseBody> createDocument(
                        @Header("Authorization") String token,
                        @Part("title") RequestBody title,
                        @Part("type") RequestBody type,
                        @Part("category_id") RequestBody categoryId,
                        @Part("category_name") RequestBody categoryName,
                        @Part("description") RequestBody description,
                        @Part("status") RequestBody status,
                        @Part MultipartBody.Part file);

        @Headers("Content-Type: application/json")
        @POST("api/user/update")
        Call<LoginResponse> updateProfile(@Header("Authorization") String token,
                        @retrofit2.http.Body java.util.Map<String, String> fields);

        @GET("api/user")
        Call<User> getProfile(@Header("Authorization") String token);

        /** HR/Admin: list documents awaiting approval */
        @GET("api/documents/pending")
        Call<List<Document>> getPendingDocuments(@Header("Authorization") String token);

        /** HR/Admin: approve or reject a document */
        @FormUrlEncoded
        @POST("api/documents/{id}/status")
        Call<ResponseBody> updateDocumentStatus(
                        @Path("id") int id,
                        @Header("Authorization") String token,
                        @Field("status") String status,
                        @Field("note") String note);

        @Headers("Content-Type: application/json")
        @POST("api/user/update-password")
        Call<ResponseBody> updatePassword(@Header("Authorization") String token,
                        @retrofit2.http.Body java.util.Map<String, String> body);

        /** Get the authenticated user's notifications */
        @GET("api/notifications")
        Call<List<Notification>> getNotifications(@Header("Authorization") String token);

        /** Mark a single notification as read */
        @POST("api/notifications/{id}/read")
        Call<ResponseBody> markNotificationRead(@Path("id") int id,
                        @Header("Authorization") String token);

        /** Mark all notifications as read */
        @POST("api/notifications/read-all")
        Call<ResponseBody> markAllNotificationsRead(@Header("Authorization") String token);

        /** Upload a profile avatar image */
        @Multipart
        @POST("api/user/upload-avatar")
        Call<ResponseBody> uploadAvatar(@Header("Authorization") String token,
                        @Part MultipartBody.Part avatar);
}
