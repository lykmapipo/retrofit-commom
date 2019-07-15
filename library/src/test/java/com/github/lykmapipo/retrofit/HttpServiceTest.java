package com.github.lykmapipo.retrofit;

import com.github.lykmapipo.retrofit.provider.AuthProvider;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * HttpService Tests
 *
 * @author lally elias
 */

@RunWith(RobolectricTestRunner.class)
public class HttpServiceTest {
    private MockWebServer mockWebServer;
    private String baseUrl;
    private String authToken = "i3Vixpfr51EVHWHP";

    @Before
    public void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @Before
    public void before() throws Exception {
        baseUrl = mockWebServer.url("/v1/").toString();
    }

    @Test
    public void shouldExposeGsonInstance() {
        Gson gson = HttpService.getGson();
        assertNotNull("should provider gson instance for reuse", gson);
    }

    @Test
    public void shouldConvertObjectToJson() {
        User user = new User("John Doe");
        String json = "{\"name\":\"John Doe\"}";
        String converted = HttpService.toJson(user);
        assertEquals("should convert value to json", converted, json);
    }

    @Test
    public void shouldConvertJsonToObject() {
        User user = new User("John Doe");
        String json = "{\"name\":\"John Doe\"}";
        User converted = HttpService.fromJson(json, User.class);
        assertEquals("should convert json to value", converted, user);
    }

    @Test
    public void shouldCreateHttpService() throws Exception {
        // creation
        Api client = HttpService.create(Api.class, baseUrl);
        assertNotNull("should create simple client", client);

        // invocation
        String json = "[{\"name\":\"John Doe\"}]";
        MockResponse response = new MockResponse().setResponseCode(200).setBody(json);
        mockWebServer.enqueue(response);

        List<User> users = client.list().execute().body();
        RecordedRequest request = mockWebServer.takeRequest();

        assertNotNull("should make success http call", users);
        assertEquals(
                "should make correct http call",
                request.getPath(), "/v1/users"
        );
        assertEquals(
                "should set default content type header",
                request.getHeader("Content-Type"), "application/json"
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Accept"), "application/json"
        );
    }

    @Test
    public void shouldCreateHttpServiceWithAuthToken() throws Exception {
        // creation
        Api client = HttpService.create(Api.class, baseUrl, authToken);
        assertNotNull("should create simple client", client);

        // invocation
        String json = "[{\"name\":\"John Doe\"}]";
        MockResponse response = new MockResponse().setResponseCode(200).setBody(json);
        mockWebServer.enqueue(response);

        List<User> users = client.list().execute().body();
        RecordedRequest request = mockWebServer.takeRequest();

        assertNotNull("should make success http call", users);
        assertEquals(
                "should make correct http call",
                request.getPath(), "/v1/users"
        );
        assertEquals(
                "should set default content type header",
                request.getHeader("Content-Type"), "application/json"
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Accept"), "application/json"
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Authorization"), "Bearer " + authToken
        );
    }

    @Test
    public void shouldCreateHttpServiceWithAuthProvider() throws Exception {
        // creation
        Api client = HttpService.create(Api.class, baseUrl, new AuthProvider() {
            @Override
            public String getToken() {
                return authToken;
            }
        });
        assertNotNull("should create simple client", client);

        // invocation
        String json = "[{\"name\":\"John Doe\"}]";
        MockResponse response = new MockResponse().setResponseCode(200).setBody(json);
        mockWebServer.enqueue(response);

        List<User> users = client.list().execute().body();
        RecordedRequest request = mockWebServer.takeRequest();

        assertNotNull("should make success http call", users);
        assertEquals(
                "should make correct http call",
                request.getPath(), "/v1/users"
        );
        assertEquals(
                "should set default content type header",
                request.getHeader("Content-Type"), "application/json"
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Accept"), "application/json"
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Authorization"), "Bearer " + authToken
        );
    }

    @Test
    public void shouldCreateMultipartRequest() throws Exception {
        // creation
        Api client = HttpService.create(Api.class, baseUrl, new AuthProvider() {
            @Override
            public String getToken() {
                return authToken;
            }
        });

        // create parts
        HashMap<String, Object> params = new HashMap<String, Object>();
        File file = createRandomFile();
        params.put("avatar", file);
        params.put("name", "John Doe");
        List<MultipartBody.Part> parts = HttpService.createParts(params);

        // invocation
        String json = "{\"name\":\"John Doe\"}";
        MockResponse response = new MockResponse().setResponseCode(200).setBody(json);
        mockWebServer.enqueue(response);

        User user = client.create(parts).execute().body();
        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();

        assertNotNull("should make success http call", user);
        assertEquals(
                "should make correct http call",
                request.getPath(), "/v1/users"
        );
        assertEquals(
                "should make correct http call",
                request.getMethod(), "POST"
        );
        assertTrue(
                "should set default content type header",
                request.getHeader("Content-Type").contains("multipart/form-data")
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Accept"), "application/json"
        );
        assertEquals(
                "should set default accept header",
                request.getHeader("Authorization"), "Bearer " + authToken
        );
    }

    private File createRandomFile() throws IOException {
        File file = File.createTempFile("test_", ".txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write("This is the temporary file content");
        bw.close();
        return file;
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
        baseUrl = null;
        mockWebServer = null;
    }

    public interface Api {
        @GET("users")
        @Headers({
                "Accept: application/json",
        })
        Call<List<User>> list();

        @Multipart
        @POST("users")
        @Headers({
                "Accept: application/json",
        })
        Call<User> create(@Part List<MultipartBody.Part> params);
    }
}