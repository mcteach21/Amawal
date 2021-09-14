package mc.apps.amawal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import mc.apps.amawal.tools.HtmlRetrofitClient;

import mc.apps.amawal.tools.Keyboard;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "retrofit";
    private static final int LATIN_BERBER_ALPHABET = 2;
    TextView txtTitle, txtSubTitle, txtDesc, txtDesc2, txtNoResult;
    EditText edtAwal;
    Button btnTranslate, displayKeyboard;
    ScrollView scrollResult;
    Keyboard kb;
    GridLayout keyboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtTitle =  findViewById(R.id.tvTitle);
        txtSubTitle =  findViewById(R.id.tvSubTitle);
        txtDesc =  findViewById(R.id.tvDesc);
        txtDesc2 =  findViewById(R.id.tvDesc2);
        txtNoResult =  findViewById(R.id.tvNoResult);

        edtAwal = findViewById(R.id.awal);
        btnTranslate = findViewById(R.id.translate);
        btnTranslate.setOnClickListener(v->scrapy());

        scrollResult = findViewById(R.id.scrollResult);

        keyboard = findViewById(R.id.keyboard);
        displayKeyboard = findViewById(R.id.btnKeybord);
        displayKeyboard.setOnClickListener(v->displayKeyboard());

        kb = new Keyboard(this, keyboard, edtAwal);

        handleRecyclerview();
    }

    boolean opened=false;
    private void displayKeyboard() {

        kb.createCustomKeyboard(LATIN_BERBER_ALPHABET,false);
        Log.i(TAG, "displayKeyboard: "+opened);
        opened = !opened;

        kb.slideKeybord(opened);
        if (opened) {
            Log.i(TAG, "Hide Keyboard! (opened= "+opened+")");
            kb.hideKeyboard();
        }
    }

    private void scrapy() {
        String awal = edtAwal.getEditableText().toString();

        kb.hideKeyboard();

        /**
         * Web scraping
         */
        Callback<ResponseBody> htmlResponseCallback = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try{
                    scrollResult.setVisibility(response.body()==null?View.GONE:View.VISIBLE);
                    txtNoResult.setVisibility(response.body()==null?View.VISIBLE:View.GONE);
                    if(response.body()!=null) {
                        String document = response.body().string();
                        Document html = Jsoup.parse(document);
                        parseResponse(html);
                    }
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

        reset();
        //adapter.reset();
    }

    String[][] rubriques = {{"Tasniremt","Terminologie"},{"Agdawal","Synonyme"},
            {"Imedyaten","Exemples"},{"Addad amaruz","Etat Annexé"}};


    private void parseResponse(Document document) {
        StringBuilder sb = new StringBuilder();

        Element definition = document.select("div#page_title_single").first();
        Element title = definition.select("h1").first();
        Element tifinagh = definition.select("span.tz").first();
        Element definition_content = definition.children().select("div").first();

        Log.i(TAG, "Title : "+title.html()+" => "+tifinagh.html());

        txtTitle.setText(title.html());
        txtSubTitle.setText(title.html());

        Elements regions = definition_content.select("ul.meta_word_region li");
        if(!regions.isEmpty()) {
            for (Element region : regions) {
                regionsAdapter.add(region.html());
            }
        }

        Element content_field = definition_content.select("ul.content_fields li span").first();

        Elements translation_flags = content_field.select("p span.translation");
        String flag_iso;
        if(!translation_flags.isEmpty()) {
            for (Element translation_flag : translation_flags) {
                flag_iso = translation_flag.attr("class").replace("translation flag_", "");
                Log.i(TAG, flag_iso + " " + translation_flag.parent().text());
                flagsAdapter.add("["+flag_iso.split("_")[0] + "] " + translation_flag.parent().text());
                translation_flag.parent().remove();
            }
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

    private void reset(){
        txtTitle.setText("");
        txtSubTitle.setText("");

        txtDesc.setText("");
        txtDesc2.setText("");

        regionsAdapter.reset();
        flagsAdapter.reset();

        if(opened) {
            opened = false;
            kb.slideKeybord(false);
        }
    }

    /**
     * List
     */
    private MyCustomAdapter regionsAdapter, flagsAdapter;
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
        RecyclerView regions = findViewById(R.id.listRegions);
        RecyclerView flags = findViewById(R.id.listFlags);
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down);
//        ListItemClickListener item_listener = (id) -> {
//
//        };
        regionsAdapter = new MyCustomAdapter();//item_listener);
        flagsAdapter = new MyCustomAdapter();//item_listener);

        regions.setAdapter(regionsAdapter);
        regions.setLayoutManager(new GridLayoutManager(this, 3));
        regions.setLayoutAnimation(animation);

        flags.setAdapter(flagsAdapter);
        flags.setLayoutManager(new GridLayoutManager(this, 2));
        flags.setLayoutAnimation(animation);
    }

}