package mc.apps.amawal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import mc.apps.amawal.tools.HtmlAmawalRetrofitClient;

import mc.apps.amawal.tools.HtmlGlosbeRetrofitClient;
import mc.apps.amawal.tools.Keyboard;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "retrofit";
    private static final int LATIN_BERBER_ALPHABET = 2;
    private static final String KAB_LANG_CODE = "kab";
    private static final String FR_LANG_CODE = "fr";
    private static final String RUB_EXAMPLES = "Imedyaten";

    LinearLayout blocs_container;
    TextView txtTitle, txtSubTitle, txtNoResult;
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
        txtNoResult =  findViewById(R.id.tvNoResult);

        edtAwal = findViewById(R.id.awal);
        btnTranslate = findViewById(R.id.translate);
        btnTranslate.setOnClickListener(v->scrapy());

        scrollResult = findViewById(R.id.scrollResult);
        blocs_container = findViewById(R.id.blocs_container);

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
                    blocs_container.setVisibility(response.body()==null?View.INVISIBLE:View.VISIBLE);

                    if(response.body()!=null) {
                        String document = response.body().string();
                        Document html = Jsoup.parse(document);

                        String url = response.raw().request().url().toString();
                        if(url.contains(HtmlAmawalRetrofitClient.BASE_URL)){
                            parseAmawalResponse(html);
                        }else{
                            parseGlosbeResponse(html);
                        }
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

        Call<ResponseBody> call1 = HtmlAmawalRetrofitClient.getInstance().getHtmlContent(awal);
        call1.enqueue(htmlResponseCallback);

        Call<ResponseBody> call2 = HtmlGlosbeRetrofitClient.getInstance().getHtml2Content(KAB_LANG_CODE, FR_LANG_CODE, awal);
        call2.enqueue(htmlResponseCallback);

        reset();
    }

    String[][] rubriques = {
            {"Tasniremt","Terminologie"},{"Agdawal","Synonyme"},
            {"Imedyaten","Exemples"},{"Addad amaruz","Etat Annexé"},
            {"Assaɣen uffiɣen","Relations.."}
    };

    private String findRubriqueFr(String kab_rubrique){
        Optional<String[]> result = Arrays.stream(rubriques).filter(rub -> rub[0].equals(kab_rubrique)).findFirst();
        return result.isPresent()?result.get()[1]:"";
    }

    private void parseGlosbeResponse(Document document) {
        Elements translations = document.select("h3.translation span");
        if(!translations.isEmpty())
            for (Element translation : translations)
                Log.i(TAG, "Glosbe : "+translation.html());

        //exemples
        StringBuilder content = new StringBuilder();
        Elements examples = document.select(".tmem__item");
        if(!examples.isEmpty())
            for (Element example : examples)
                content.append(example.child(0).text()+" : "+example.child(1).text()+"\n");

        // TODO : display..
        // addToContainer(content.toString(), R.drawable.container_rounded_brown, R.color.primaryTextColor, false);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void parseAmawalResponse(Document document) {

        Element definition = document.select("div#page_title_single").first();
        Element title = definition.select("h1").first();
        //Element tifinagh = definition.select("span.tz").first();
        Element definition_content = definition.children().select("div").first();

        txtTitle.setText(title.html());
        txtSubTitle.setText(title.html());

        Elements regions = definition_content.select("ul.meta_word_region li");
        if(!regions.isEmpty())
            for (Element region : regions)
                regionsAdapter.add(region.html());

        regions_list.setVisibility(regions.isEmpty()?View.GONE:View.VISIBLE);

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

        flags_list.setVisibility(translation_flags.isEmpty()?View.GONE:View.VISIBLE);

        String content = content_field.html().replace("<p></p>", "").replace("<strong>", "[")
                .replace("</strong>", "] ").replace(":]", "]");

        String rubrique_title;
        int index;
        HashMap<Integer, String> rubriques_contents= new HashMap<>();
        for (String[] rubrique : rubriques) {


            rubrique_title = rubrique[0];
            index = content.indexOf("["+rubrique_title+"]");
            if(index>-1)
                rubriques_contents.put(index, rubrique_title);


//            Log.i(TAG, "Rubrique : "+rubrique_title);
//            Log.i(TAG, Arrays.toString(rubrique));
        }

        LinkedHashMap<Integer, String> sorted_rubriques_contents = getSortedHashMap(rubriques_contents);
        Optional<Map.Entry<Integer, String>> first_rubrique = sorted_rubriques_contents.entrySet().stream().findFirst();

        Integer first_rub_index = first_rubrique.isPresent()?first_rubrique.get().getKey():content.trim().length();

        Log.i(TAG, "==================================");
        for ( Map.Entry<Integer, String> entry : sorted_rubriques_contents.entrySet()) {
            Log.i(TAG, entry.getKey()+" "+entry.getValue());
        }
        Log.i(TAG, "==================================");
        addToContainer(cleanHtml(content.trim().subSequence(0, first_rub_index-1).toString()), R.drawable.container_rounded_blue, R.color.primaryLightColor, false);

        final String[] current_rubrique = {""};
        final int[] start_index = {-1};

        sorted_rubriques_contents.entrySet().stream().forEach(
                i -> {
                    if(start_index[0] >-1) {
                        String content_clean = cleanHtml(content.trim().subSequence(start_index[0] + current_rubrique[0].length() + 2, i.getKey() - 1).toString());
                        String rubrique = current_rubrique[0]+" ("+findRubriqueFr(current_rubrique[0])+")";
                        int background = RUB_EXAMPLES.equals(current_rubrique[0])?R.drawable.container_rounded_teal:R.drawable.container_rounded_grey;

                        Log.i(TAG, "=>"+rubrique);

                        addToContainer(rubrique, R.drawable.container_rounded_transparent, R.color.white, true);
                        addToContainer(content_clean, background, R.color.primaryTextColor, false);

                    }
                    current_rubrique[0] = i.getValue();
                    start_index[0] = i.getKey();
                    Log.i(TAG, "current_rubrique[0] = "+current_rubrique[0]+" start_index[0]="+start_index[0]);
                }
        );

        //last one!
        if(!"".equals(current_rubrique[0])) {
            String content_clean = cleanHtml(content.trim().subSequence(start_index[0] + current_rubrique[0].length() + 2, content.trim().length() - 1).toString());
            String rubrique = current_rubrique[0] + " (" + findRubriqueFr(current_rubrique[0]) + ")";
            int background = RUB_EXAMPLES.equals(current_rubrique[0]) ? R.drawable.container_rounded_teal : R.drawable.container_rounded_grey;
            addToContainer(rubrique, R.drawable.container_rounded_transparent, R.color.white, true);
            addToContainer(content_clean, background, R.color.primaryTextColor, false);
        }
    }

    private String cleanHtml(String html) {
        return  html.replace("<p>","").replace("</p>","").replace("<br>",System.getProperty("line.separator")).trim();
    }



    List<View> blocs_in_container = new ArrayList();
    private void reset(){
        txtTitle.setText("");
        txtSubTitle.setText("");

//        txtDesc.setText("");
//        txtDesc2.setText("");
        for (View bloc: blocs_in_container)
            blocs_container.removeView(bloc);

        regionsAdapter.reset();
        flagsAdapter.reset();

        if(opened) {
            opened = false;
            kb.slideKeybord(false);
        }
    }
    @NonNull
    private LinkedHashMap<Integer, String> getSortedHashMap(HashMap<Integer, String> rubriques_contents) {
        LinkedHashMap<Integer, String> sorted_rubriques_contents = rubriques_contents.entrySet().stream().sorted((i1, i2) -> i1.getKey().compareTo(i2.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        return sorted_rubriques_contents;
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
    RecyclerView regions_list, flags_list;

    private void handleRecyclerview() {
        regions_list = findViewById(R.id.listRegions);
        flags_list = findViewById(R.id.listFlags);
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down);
//        ListItemClickListener item_listener = (id) -> {
//
//        };
        regionsAdapter = new MyCustomAdapter();//item_listener);
        flagsAdapter = new MyCustomAdapter();//item_listener);

        regions_list.setAdapter(regionsAdapter);
        regions_list.setLayoutManager(new GridLayoutManager(this, 3));
        regions_list.setLayoutAnimation(animation);

        flags_list.setAdapter(flagsAdapter);
        flags_list.setLayoutManager(new GridLayoutManager(this, 2));
        flags_list.setLayoutAnimation(animation);
    }

    /**
     * Tools
     */

    class CustomTextView extends androidx.appcompat.widget.AppCompatTextView {
        public CustomTextView(Context context) {
            super(context, null, R.style.bloc_title_style);
        }
    }
    private void addToContainer(String content, int background, int color, boolean title){

        AppCompatTextView tv = new AppCompatTextView(this, null, R.style.bloc_title_style);
        tv.setText(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(castFromDP(16), title?castFromDP(8):0, castFromDP(16), 0);
        tv.setLayoutParams(params);

        //  tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

        if(title) {
            tv.setTextAppearance(this, R.style.blocTitleStyle);
        }else{
            tv.setTextAppearance(this, R.style.blocContentStyle);
            tv.setBackground(getResources().getDrawable(background, null));
            tv.setTextColor(getResources().getColor(color,null));
            tv.setMinHeight(castFromDP(48));
            tv.setTextIsSelectable(true);
        }
        blocs_container.addView(tv);
        blocs_in_container.add(tv);

        Animation animation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.slide_right_in);
        animation.setStartOffset(0);
        tv.startAnimation(animation);
    }

    private int castFromDP(int size) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getResources().getDisplayMetrics());
    }
}