package mc.apps.amawal.tools;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitInterface {
    @GET("{query}")
    Call<ResponseBody> getHtmlContent(@Path("query") String query);


    //kab/fr/Aɣrum
    @GET("{lang1}/{lang2}/{query}")
    Call<ResponseBody> getHtml2Content(@Path("lang1") String lang1, @Path("lang2") String lang2, @Path("query") String query);
}
