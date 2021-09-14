package mc.apps.amawal.tools;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.gridlayout.widget.GridLayout;

import mc.apps.amawal.R;

public class Keyboard {
    private static final String TAG = "retrofit";

    private Context  context;
    private EditText edtSearch;
    private GridLayout keyboard;

    private int maxIndice = 26;
    private int currentKeyboard = 1; //fr 2:ber(latin) 3:tifinaɣ

    public Keyboard(Context context, GridLayout keyboard, EditText edtSearch) {
        this.context = context;
        this.edtSearch = edtSearch;
        this.keyboard = keyboard;
    }

    private String format(String text) {
        return text.substring(0, 1).toUpperCase()+text.substring(1).toLowerCase();
    }

    private String[] getAlphabet(int index){
        currentKeyboard = index;
        if(currentKeyboard==2)
            return new String[]{
                    "a", "b", "c", "č", "d", "ḍ", "e", "ɛ", "f", "g", "ǧ", "ɣ", "h", "ḥ", "i", "j", "k", "l", "m", "n", "q", "r", "ṛ", "s", "ṣ", "t", "ṭ", "u", "w", "x", "y", "z", "ẓ"

                    // "A", "B", "C", "Č", "D", "Ḍ", "E", "Ɛ", "F", "G", "Ǧ", "Ɣ", "H", "Ḥ", "I", "J", "K", "L", "M", "N", "Q", "R", "Ṛ", "S", "Ṣ", "T", "Ṭ", "U", "W", "X", "Y", "Z", "Ẓ"
                    // "ⴰ", "ⴱ", "ⵛ", "ⴷ", "ⴻ", "ⴼ", "ⴳ", "ⵀ", "ⵉ", "ⵊ", "ⴽ", "ⵍ", "ⵎ", "ⵏ", "ⵄ", "ⵃ", "ⵇ", "ⵔ", "ⵙ", "ⵚ", "ⵜ", "ⵓ", "ⵖ", "ⵡ", "ⵅ", "ⵢ", "ⵣ"
//                    "a", "b", "g", "d", "Ḍ", "e", "f", "k", "h", "Ḥ", "ɛ", "kh", "q", "i", "j", "l",
//                    "m", "n", "u", "r", "Ṛ", "ɣ", "s", "Ṣ", "sh", "t", "Ṭ", "w", "y", "z", "Ẓ", "w",
//                    "bh", "gh", "dj", "dj", "d", "Ḍh", "kh", "h", "p", "th", "č", "v"
            };
        else
            return new String[]{
                    "a","b", "c", "d", "e","f","g", "h","i", "j","k","l","m","n", "o","p","q","r","s","t","u","v","w","x", "y","z","<x"
            };
    }

    boolean opened=false;
    public void createCustomKeyboard(int index, boolean next) {
        keyboard.removeAllViews();

        String[] keys = getAlphabet(index);
        if(next){
            for (int i = maxIndice+1; i < keys.length; i++) {
                addKeytoKeyboard(index, i, keys[i]);
            }
            addKeytoKeyboard(index,keys.length + 1, "<x");

            addKeytoKeyboard(index,maxIndice+1, "<<");

            this.opened=false;
        }else {
            for (int i = 0; i < maxIndice; i++)
                addKeytoKeyboard(index, i, keys[i]);

            addKeytoKeyboard(index, maxIndice, "<x");
            if(keys.length>maxIndice)
                addKeytoKeyboard(index,maxIndice+1, "..");
        }
        keyboard.setUseDefaultMargins(true);
    }
    public void addKeytoKeyboard(int index, int i, String txt) {
        int margin = 6;
        float scale = context.getResources().getDisplayMetrics().density;

        Button btn = new Button(context);
        btn.setId(i);

        if(txt.equals("<x")) {
            btn.setTag("backspace");
            btn.setBackgroundResource(R.drawable.button_cmd_shape);
        }else {
            btn.setTag("");
            btn.setText(txt);
            btn.setBackgroundResource(R.drawable.button_key_shape);
        }

        btn.setAllCaps(false);

        //int color = txt.equals("..") ? R.color.primaryTextColor: R.color.material_red_a200;
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (45 * scale + 0.5f), (int) (45 * scale + 0.5f));
        params.setMargins(margin, margin, margin, margin);
        btn.setLayoutParams(params);


        //btn.setTextColor(context.getResources().getColor(color,null));
        btn.setOnClickListener(view -> writeWord(index, view));
        keyboard.addView(btn);
    }

    public void writeWord(int index, View view) {
        Button btn = (Button) view;
        if(btn.getTag().equals("backspace")){
            edtSearch.setText("");
        }else {
            String text = btn.getText().toString();
            switch (text) {
                case "..":
                    createCustomKeyboard(index, true);
                    break;
                case "<<":
                    createCustomKeyboard(index, false);
                    break;
                default:
                    String resultText = edtSearch.getText().toString() + text;
                    edtSearch.setText(resultText);
            }
        }
        edtSearch.setSelection(edtSearch.getText().length());
    }

    public void hideKeyboard(){

        View view = ((Activity) context).getCurrentFocus();

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public void slideKeybord(boolean open){
        int currentHeight = (open) ? 0 : 800;
        int newHeight = (open)?800:0;

        ValueAnimator slideAnimator = ValueAnimator.ofInt(currentHeight, newHeight).setDuration(300);
        slideAnimator.addUpdateListener(valueAnimator -> {
            int value = (int) valueAnimator.getAnimatedValue();
            keyboard.getLayoutParams().height = value;
            keyboard.requestLayout();
        });

        AnimatorSet set = new AnimatorSet();
        set.play(slideAnimator);

        set.setInterpolator( new AccelerateDecelerateInterpolator());
        set.start();
    }
}
