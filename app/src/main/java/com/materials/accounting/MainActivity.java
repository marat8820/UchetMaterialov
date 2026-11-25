
package com.materials.accounting;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {

    int green = Color.rgb(18, 65, 49);
    int darkGreen = Color.rgb(7, 45, 34);
    int gold = Color.rgb(232, 195, 105);
    int white = Color.rgb(255, 250, 236);

    LinearLayout root;
    JSONArray objects;
    SharedPreferences prefs;
    int selectedObject = -1;

    String[] units = {
        "шт.", "м", "м²", "м³", "кг", "упаковка"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("MaterialsApp", MODE_PRIVATE);
        loadObjects();
        showHome();
    }

    @Override
    public void onBackPressed() {
        if (selectedObject >= 0) {
            selectedObject = -1;
            showHome();
        } else {
            super.onBackPressed();
        }
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

    LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, 60);
        p.bottomMargin = 12;
        return p;
    }

    void prepareScreen(String titleText) {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24, 25, 24, 24);
        root.setBackgroundColor(green);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        TextView title = text(titleText, 24, gold);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 15, 0, 25);
        root.addView(title);

        setContentView(scroll);
    }

    void showHome() {
        selectedObject = -1;
        prepareScreen("📦  УЧЁТ МАТЕРИАЛОВ");

        TextView subtitle = text("МОИ ОБЪЕКТЫ", 20, white);
        subtitle.setTypeface(null, Typeface.BOLD);
        root.addView(subtitle);

        int active = 0;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject item = objects.optJSONObject(i);
            if (item != null &&
                !item.optBoolean("archived", false)) {
                active++;
            }
        }

        TextView count = text(
            "Активных объектов: " + active, 16, gold
        );
        count.setPadding(0, 12, 0, 20);
        root.addView(count);

        Button add = makeButton("＋ ДОБАВИТЬ ОБЪЕКТ");
        root.addView(add, buttonParams());
        add.setOnClickListener(v -> addObject());

        TextView listTitle = text("Список объектов", 18, white);
        listTitle.setPadding(0, 20, 0, 15);
        root.addView(listTitle);

        for (int i = 0; i < objects.length(); i++) {
            JSONObject item = objects.optJSONObject(i);
            if (item == null ||
                item.optBoolean("archived", false)) continue;

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
                "Ответственный: " +
                item.optString("responsible"),
                14, white
            );
            card.addView(responsible);

            JSONArray materials = item.optJSONArray("materials");
            int materialCount =
                materials == null ? 0 : materials.length();

            TextView info = text(
                "Материалов: " + materialCount,
                14, gold
            );
            info.setPadding(0, 10, 0, 0);
            card.addView(info);

            TextView open = text(
                "ОТКРЫТЬ ОБЪЕКТ  →", 15, gold
            );
            open.setPadding(0, 18, 0, 0);
            card.addView(open);

            card.setOnClickListener(v -> showObject(index));
        }
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

                if (n.isEmpty()) {
                    name.setError("Введите название объекта");
                    return;
                }

                JSONObject item = new JSONObject();

                try {
                    item.put("name", n);
                    item.put("address",
                        address.getText().toString().trim());
                    item.put("responsible",
                        responsible.getText().toString().trim());
                    item.put("archived", false);
                    item.put("materials", new JSONArray());

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

        selectedObject = index;

        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) {
            materials = new JSONArray();
            try {
                item.put("materials", materials);
                saveObjects();
            } catch (Exception e) {}
        }

        prepareScreen("🏗  ОБЪЕКТ");

        TextView name = text(
            item.optString("name"), 23, gold
        );
        name.setTypeface(null, Typeface.BOLD);
        root.addView(name);

        TextView address = text(
            "Адрес: " + item.optString("address"),
            15, white
        );
        address.setPadding(0, 12, 0, 5);
        root.addView(address);

        TextView responsible = text(
            "Ответственный: " + item.optString("responsible"),
            15, white
        );
        responsible.setPadding(0, 0, 0, 20);
        root.addView(responsible);

        Button materialButton = makeButton("📦  МАТЕРИАЛЫ");
        root.addView(materialButton, buttonParams());
        materialButton.setOnClickListener(
            v -> showMaterials(index)
        );

        Button back = makeButton("← НАЗАД К ОБЪЕКТАМ");
        root.addView(back, buttonParams());
        back.setOnClickListener(v -> showHome());

        Button archive = makeButton("АРХИВИРОВАТЬ ОБЪЕКТ");
        root.addView(archive, buttonParams());
        archive.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Архивировать объект?")
                .setMessage("Объект исчезнет из активного списка. Его данные сохранятся.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Архивировать", (d, w) -> {
                    try {
                        item.put("archived", true);
                    } catch (Exception e) {}
                    saveObjects();
                    showHome();
                })
                .show()
        );
    }

    void showMaterials(int index) {
        JSONObject item = objects.optJSONObject(index);
        if (item == null) return;

        selectedObject = index;

        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) {
            materials = new JSONArray();
            try {
                item.put("materials", materials);
            } catch (Exception e) {}
        }

        prepareScreen("📦  МАТЕРИАЛЫ");

        TextView objectName = text(
            "Объект: " + item.optString("name"),
            16, gold
        );
        objectName.setPadding(0, 0, 0, 20);
        root.addView(objectName);

        Button add = makeButton("＋ ДОБАВИТЬ МАТЕРИАЛ");
        root.addView(add, buttonParams());
        add.setOnClickListener(v -> addMaterial(index, -1));

        TextView heading = text("Список материалов", 18, white);
        heading.setPadding(0, 20, 0, 15);
        root.addView(heading);

        if (materials.length() == 0) {
            TextView empty = text(
                "Материалов пока нет. Нажмите «Добавить материал».",
                15, white
            );
            empty.setPadding(0, 10, 0, 20);
            root.addView(empty);
        }

        for (int i = 0; i < materials.length(); i++) {
            JSONObject material = materials.optJSONObject(i);
            if (material == null) continue;

            final int materialIndex = i;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(1);
            card.setPadding(16, 16, 16, 16);
            card.setBackground(shape(darkGreen, 16));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = 12;
            root.addView(card, cp);

            TextView materialName = text(
                material.optString("name"), 19, gold
            );
            materialName.setTypeface(null, Typeface.BOLD);
            card.addView(materialName);

            TextView quantity = text(
                "Количество: " +
                formatNumber(material.optDouble("quantity", 0)) +
                " " + material.optString("unit"),
                16, white
            );
            quantity.setPadding(0, 10, 0, 12);
            card.addView(quantity);

            Button edit = makeButton("ИЗМЕНИТЬ");
            card.addView(edit, buttonParams());
            edit.setOnClickListener(v ->
                addMaterial(index, materialIndex)
            );

            Button delete = makeButton("УДАЛИТЬ");
            card.addView(delete, buttonParams());
            delete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle("Удалить материал?")
                    .setMessage(material.optString("name"))
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Удалить", (d, w) -> {
                        JSONArray current =
                            item.optJSONArray("materials");
                        if (current != null) {
                            current.remove(materialIndex);
                        }
                        saveObjects();
                        showMaterials(index);
                    })
                    .show()
            );
        }

        Button back = makeButton("← НАЗАД К ОБЪЕКТУ");
        root.addView(back, buttonParams());
        back.setOnClickListener(v -> showObject(index));
    }

    String formatNumber(double number) {
        if (number == Math.floor(number)) {
            return String.valueOf((long) number);
        }
        return String.valueOf(number);
    }

    void addMaterial(int objectIndex, int materialIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) {
            materials = new JSONArray();
            try {
                item.put("materials", materials);
            } catch (Exception e) {}
        }

        JSONObject old = null;
        if (materialIndex >= 0) {
            old = materials.optJSONObject(materialIndex);
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(1);
        form.setPadding(25, 5, 25, 5);

        EditText name = new EditText(this);
        name.setHint("Название материала");
        if (old != null) name.setText(old.optString("name"));
        form.addView(name);

        TextView unitLabel = text("Единица измерения", 14, Color.BLACK);
        form.addView(unitLabel);

        Spinner unitSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            units
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        unitSpinner.setAdapter(adapter);

        if (old != null) {
            String oldUnit = old.optString("unit", "шт.");
            for (int i = 0; i < units.length; i++) {
                if (units[i].equals(oldUnit)) {
                    unitSpinner.setSelection(i);
                    break;
                }
            }
        }

        form.addView(unitSpinner);

        EditText quantity = new EditText(this);
        quantity.setHint("Начальное количество");
        quantity.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        if (old != null) {
            quantity.setText(
                formatNumber(old.optDouble("quantity", 0))
            );
        } else {
            quantity.setText("0");
        }

        form.addView(quantity);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(old == null ?
                "Новый материал" : "Изменить материал")
            .setView(form)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(v -> {
            dialog.getButton(-1).setOnClickListener(btn -> {
                String n = name.getText().toString().trim();
                String q = quantity.getText().toString()
                    .trim().replace(',', '.');

                if (n.isEmpty()) {
                    name.setError("Введите название материала");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(q);
                    if (amount < 0 ||
                        Double.isNaN(amount) ||
                        Double.isInfinite(amount)) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    quantity.setError("Введите правильное количество");
                    return;
                }

                // Исправление ошибки Java:
                // сохраняем ссылки в final-переменные
                final JSONObject existingMaterial = old;
                final JSONArray savedMaterials = materials;

                JSONObject material = existingMaterial;

                if (material == null) {
                    material = new JSONObject();
                }

                try {
                    material.put("name", n);
                    material.put("unit",
                        unitSpinner.getSelectedItem().toString());
                    material.put("quantity", amount);

                    if (existingMaterial == null) {
                        savedMaterials.put(material);
                    }
                } catch (Exception e) {
                    return;
                }

                saveObjects();
                dialog.dismiss();
                showMaterials(objectIndex);
            });
        });

        dialog.show();
    }
}
