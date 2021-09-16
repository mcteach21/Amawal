package mc.apps.amawal.tools;

import retrofit2.Retrofit;

public class HtmlGlosbeRetrofitClient {
    private static final String BASE_URL = "https://fr.glosbe.com/";
    private static final String TAG = "retrofit";
    public static RetrofitInterface getInstance() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build();

        RetrofitInterface apiInterface = retrofit.create(RetrofitInterface.class);
        return apiInterface;
    }
}
