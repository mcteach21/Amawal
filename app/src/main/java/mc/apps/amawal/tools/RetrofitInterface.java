package mc.apps.amawal.tools;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitInterface {
    @GET("{query}")
    Call<ResponseBody> getHtmlContent(@Path("query") String query);
}
