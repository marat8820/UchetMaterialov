
package com.materials.accounting;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.content.*;
import org.json.*;

public class MainActivity extends Activity {

    int green = Color.rgb(18, 65, 49);
    int darkGreen = Color.rgb(7, 45, 34);
    int gold = Color.rgb(232, 195, 105);
    int white = Color.rgb(255, 250, 236);

    LinearLayout root;
    JSONArray objects;
    SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("MaterialsApp", MODE_PRIVATE);
        loadObjects();
        showHome();
    }

    void loadObjects() {
        try {
            objects = new JSONArray(
                prefs.getString("objects", "[]")
            );
        } catch (Exception e) {
            objects = new JSONArray();
        }
    }

    void saveObjects() {
        prefs.edit().putString(
            "objects", objects.toString()
        ).apply();
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    Button makeButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(white);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(darkGreen)
        );
        return b;
    }

    void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24, 30, 24, 24);
        root.setBackgroundColor(green);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        TextView title = text("📦  УЧЁТ МАТЕРИАЛОВ", 25, gold);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 15, 0, 25);
        root.addView(title);

        TextView subtitle = text("МОИ ОБЪЕКТЫ", 20, white);
        subtitle.setTypeface(null, Typeface.BOLD);
        subtitle.setPadding(0, 10, 0, 15);
        root.addView(subtitle);

        TextView count = text(
            "Активных объектов: " + objects.length(),
            16, gold
        );
        count.setPadding(0, 0, 0, 20);
        root.addView(count);

        Button add = makeButton("＋  ДОБАВИТЬ ОБЪЕКТ");
        root.addView(add, buttonParams());
        add.setOnClickListener(v -> addObject());

        TextView listTitle = text("Список объектов", 18, white);
        listTitle.setPadding(0, 25, 0, 15);
        root.addView(listTitle);

        for (int i = 0; i < objects.length(); i++) {
            JSONObject item = objects.optJSONObject(i);
            if (item == null) continue;
            if (item.optBoolean("archived", false)) continue;

            final int index = i;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(1);
            card.setPadding(18, 18, 18, 18);
            card.setBackground(shape(darkGreen, 18));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = 14;
            root.addView(card, cp);

            TextView name = text(
                item.optString("name"), 20, gold
            );
            name.setTypeface(null, Typeface.BOLD);
            card.addView(name);

            TextView address = text(
                "Адрес: " + item.optString("address"),
                14, white
            );
            address.setPadding(0, 10, 0, 5);
            card.addView(address);

            TextView responsible = text(
                "Ответственный: " + item.optString("responsible"),
                14, white
            );
            card.addView(responsible);

            TextView open = text("ОТКРЫТЬ ОБЪЕКТ  →", 15, gold);
            open.setPadding(0, 18, 0, 0);
            card.addView(open);

            card.setOnClickListener(v -> showObject(index));
        }

        setContentView(scroll);
    }

    LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, 65);
        p.bottomMargin = 12;
        return p;
    }

    void addObject() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(1);
        form.setPadding(25, 10, 25, 10);

        EditText name = new EditText(this);
        name.setHint("Название объекта");
        form.addView(name);

        EditText address = new EditText(this);
        address.setHint("Адрес объекта");
        form.addView(address);

        EditText responsible = new EditText(this);
        responsible.setHint("Ответственный");
        form.addView(responsible);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Новый объект")
            .setView(form)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(v -> {
            dialog.getButton(-1).setOnClickListener(btn -> {
                String n = name.getText().toString().trim();
                String a = address.getText().toString().trim();
                String r = responsible.getText().toString().trim();

                if (n.isEmpty()) {
                    name.setError("Введите название объекта");
                    return;
                }

                JSONObject item = new JSONObject();

                try {
                    item.put("name", n);
                    item.put("address", a);
                    item.put("responsible", r);
                    item.put("archived", false);
                    objects.put(item);
                } catch (Exception e) {
                    return;
                }

                saveObjects();
                dialog.dismiss();
                showHome();
            });
        });

        dialog.show();
    }

    void showObject(int index) {
        JSONObject item = objects.optJSONObject(index);
        if (item == null) return;

        String details =
            "Название: " + item.optString("name") +
            "\n\nАдрес: " + item.optString("address") +
            "\n\nОтветственный: " +
            item.optString("responsible");

        new AlertDialog.Builder(this)
            .setTitle("Карточка объекта")
            .setMessage(details)
            .setPositiveButton("Назад", (d, w) -> showHome())
            .setNegativeButton("Архивировать", (d, w) -> {
                item.optString("name");
                try {
                    item.put("archived", true);
                } catch (Exception e) {}
                saveObjects();
                showHome();
                Toast.makeText(
                    this, "Объект перемещён в архив",
                    Toast.LENGTH_SHORT
                ).show();
            })
            .show();
    }
}
