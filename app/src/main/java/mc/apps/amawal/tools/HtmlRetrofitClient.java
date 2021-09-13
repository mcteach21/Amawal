package mc.apps.amawal.tools;

import retrofit2.Retrofit;

public class HtmlRetrofitClient {
    private static final String BASE_URL = "https://amawal.net/";
    private static final String TAG = "retrofit";

    public static RetrofitInterface getInstance() {
//        Gson gson = new GsonBuilder()
//                .setLenient()
//                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                //.addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        RetrofitInterface apiInterface = retrofit.create(RetrofitInterface.class);
        return apiInterface;
    }
}
