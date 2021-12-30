package com.github.osisoft.ocsClientCredsJava;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Program {
    // Step 1: get needed variables 
    static Appsettings appsettings = getAppsettings();

    // get configuration
    static String tenantId = appsettings.getTenantId();
    static String apiVersion = appsettings.getApiVersion();
    static String resource = appsettings.getResource();
	static String clientId = appsettings.getClientId();
	static String clientSecret = appsettings.getClientSecret();

    public static void main(String[] args) throws IOException {
        // Step 2: get the authentication endpoint from the discovery URL
        URL discoveryUrl = new URL(resource + "/identity/.well-known/openid-configuration");
        URLConnection request = discoveryUrl.openConnection();
        request.connect();
        JsonObject rootObj = JsonParser
                .parseReader(new InputStreamReader((InputStream) request.getContent(), StandardCharsets.UTF_8))
                .getAsJsonObject();
        String tokenUrl = rootObj.get("token_endpoint").getAsString();

		// Step 3: use the client ID and Secret to get the needed bearer token
        String accessToken = null;
        URL token = new URL(tokenUrl);
        HttpURLConnection tokenRequest = (HttpURLConnection) token.openConnection();
        tokenRequest.setRequestMethod("POST");
        tokenRequest.setRequestProperty("Accept", "application/json");
        tokenRequest.setDoOutput(true);
        tokenRequest.setDoInput(true);

        String postString = "client_id=" + URLEncoder.encode(clientId, "UTF-8") + "&client_secret="
                + URLEncoder.encode(clientSecret, "UTF-8") + "&grant_type=client_credentials";
        byte[] postData = postString.getBytes("UTF-8");
        tokenRequest.setRequestProperty("Content-Length", Integer.toString(postData.length));
        try (OutputStream stream = tokenRequest.getOutputStream()) {
            stream.write(postData);
        }

        String result;
        try (InputStream in = new BufferedInputStream(tokenRequest.getInputStream())) {
            result = org.apache.commons.io.IOUtils.toString(in, "UTF-8");
        }

        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        accessToken = response.get("access_token").getAsString();

		// Step 4: test token by calling the base tenant endpoint
        URL tenant = new URL(resource + "/api/" + apiVersion + "/Tenants/" + tenantId);
        HttpURLConnection tenantRequest = (HttpURLConnection) tenant.openConnection();
        tenantRequest.setRequestProperty("Authorization", "Bearer " + accessToken);
        tenantRequest.setRequestProperty("Accept", "application/json"); //TODO: is this required?
        tenantRequest.connect();

		// test it by making sure we got a valid http status code
        System.out.println(tenantRequest.getResponseCode());

    }


    private static Appsettings getAppsettings() {
        Gson mGson = new Gson();

        Appsettings appsettings = new Appsettings();

        try (InputStream inputStream = new FileInputStream("appsettings.json")) {
            String fileString = null;
            int bytesToRead = inputStream.available();

            if (bytesToRead > 0) {
                byte[] bytes = new byte[bytesToRead];
                inputStream.read(bytes);
                fileString = new String(bytes);
                // inputStream.close(); // TODO: is this necessary?
            }

            appsettings = mGson.fromJson(fileString, Appsettings.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return appsettings;
    }

}