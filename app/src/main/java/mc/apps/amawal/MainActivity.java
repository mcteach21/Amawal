package mc.apps.amawal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import mc.apps.amawal.tools.HtmlRetrofitClient;
import mc.apps.amawal.tools.RetrofitInterface;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "retrofit";
    TextView txtTitle, txtSubTitle, txtDesc;
    EditText edtAwal;
    Button btnTranslate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtTitle =  findViewById(R.id.tvTitle);
        txtSubTitle =  findViewById(R.id.tvSubTitle);
        txtDesc =  findViewById(R.id.tvDesc);

        edtAwal = findViewById(R.id.awal);
        btnTranslate = findViewById(R.id.translate);
        btnTranslate.setOnClickListener(v->scrapy());

        handleRecyclerview();
    }

    private void scrapy() {
        String awal = edtAwal.getEditableText().toString();
        hideKeyboard(btnTranslate);
        /**
         * Web scraping
         */
        Callback<ResponseBody> htmlResponseCallback = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try{
                    String document = response.body().string();
                    Document html = Jsoup.parse(document);
                    parseResponse(html);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        };

        Call<ResponseBody> call = HtmlRetrofitClient.getInstance().getHtmlContent(awal);
        call.enqueue(htmlResponseCallback);

        adapter.reset();
    }

    String[][] rubriques = {{"Tasniremt","Terminologie"},{"Agdawal","Synonyme"},
            {"Imedyaten","Exemples"},{"Addad amaruz","Etat Annexé"}};

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    private void parseResponse(Document document) {
        StringBuilder sb = new StringBuilder();

        Element definition = document.select("div#page_title_single").first();
        Element title = definition.select("h1").first();
        Element tifinagh = definition.select("span.tz").first();
        Element definition_content = definition.children().select("div").first();

        //Log.i(TAG, "Title : "+title.html()+" => "+tifinagh.html());

        txtTitle.setText(title.html());
        txtSubTitle.setText(title.html());

        Elements regions = definition_content.select("ul.meta_word_region li");
        if(!regions.isEmpty()) {
            for (Element region : regions) {
                //Log.i(TAG, region.html());
                adapter.add(region.html());
            }
        }

        Element content_field = definition_content.select("ul.content_fields li span").first();

        Elements translation_flags = content_field.select("p span.translation");
        String flag_iso;
        if(!translation_flags.isEmpty()) {
            sb.append(System.getProperty("line.separator"));
            for (Element translation_flag : translation_flags) {
                flag_iso = translation_flag.attr("class").replace("translation flag_", "");
                Log.i(TAG, flag_iso + " " + translation_flag.parent().text());

                sb.append(flag_iso + " " + translation_flag.parent().text());
                sb.append(System.getProperty("line.separator"));
                translation_flag.parent().remove();
            }
            sb.append(System.getProperty("line.separator"));
        }

        String content = content_field.html().replace("<strong>", "[").replace("</strong>", "] ").replace(":]", "]");
        String[] content_lines = content.split("<br>");
        String content_line_clean;
        for (String content_line : content_lines) {
            content_line_clean =  content_line.replace("<p>","").replace("</p>","").trim();

           for (String[] rubrique : rubriques) {
               if (content_line_clean.startsWith("["+rubrique[0])) {
                   content_line_clean = content_line_clean.replace("["+rubrique[0]+"]", "["+rubrique[0]+" ("+rubrique[1]+")]" + System.getProperty("line.separator"));
                   sb.append(System.getProperty("line.separator"));
               }
           }

            sb.append(content_line_clean);
            sb.append(System.getProperty("line.separator"));
        }



        txtDesc.setText(sb.toString());
    }


    /**
     * List
     */
    private MyCustomAdapter adapter;
    private class MyCustomAdapter extends RecyclerView.Adapter<MyCustomAdapter.MyCustomViewHolder> {
        private static final long FADE_DURATION = 2000;
        List<String> items = new ArrayList<>();

//        private ListItemClickListener listener;
//        public MyCustomAdapter(ListItemClickListener listener) {
//            this.listener = listener;
//        }

        @NonNull
        @Override
        public MyCustomAdapter.MyCustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item_view =  LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.simple_list_item , parent, false);
            return new MyCustomAdapter.MyCustomViewHolder(item_view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyCustomAdapter.MyCustomViewHolder holder, int position) {
            String region = items.get(position);

            holder.title.setText(region);

            setFadeAnimation(holder.itemView);
            setScaleAnimation(holder.itemView);
        }

        /**
         * Animtions
         * @return
         */
        private void setFadeAnimation(View view) {
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(FADE_DURATION);
            view.startAnimation(anim);
        }
        private void setScaleAnimation(View view) {
            ScaleAnimation anim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(FADE_DURATION/2);
            view.startAnimation(anim);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void reset() {
            this.items.clear();
            notifyDataSetChanged();
        }

        public void add(String cast) {
            this.items.add(cast);
            notifyDataSetChanged();
        }

        class MyCustomViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            public MyCustomViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvRegion);
                //itemView.setOnClickListener(v->listener.onClick(getAdapterPosition()));
            }
        }
    }
    private void handleRecyclerview() {
        RecyclerView recyclerview = findViewById(R.id.listRegions);
//        ListItemClickListener item_listener = (id) -> {
//            GetBiography(adapter.items.get(id).id);
//        };

        adapter = new MyCustomAdapter();//item_listener);
        recyclerview.setAdapter(adapter);
        recyclerview.setLayoutManager(new GridLayoutManager(this, 3));

        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(this,
                R.anim.layout_animation_fall_down);
        recyclerview.setLayoutAnimation(animation);
    }

}