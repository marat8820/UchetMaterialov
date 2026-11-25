package com.materials.accounting;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private final int green = Color.rgb(18, 65, 49);
    private final int darkGreen = Color.rgb(7, 45, 34);
    private final int gold = Color.rgb(232, 195, 105);
    private final int white = Color.rgb(255, 250, 236);

    private LinearLayout root;
    private JSONArray objects;
    private SharedPreferences prefs;
    private int selectedObject = -1;

    private final String[] units = {
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
            showObject(selectedObject);
        } else {
            super.onBackPressed();
        }
    }

    private void loadObjects() {
        try {
            objects = new JSONArray(prefs.getString("objects", "[]"));
        } catch (Exception e) {
            objects = new JSONArray();
        }
    }

    private void saveObjects() {
        prefs.edit().putString("objects", objects.toString()).apply();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button makeButton(String value) {
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

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, 60);
        p.bottomMargin = 12;
        return p;
    }

    private void prepareScreen(String titleText) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 25, 24, 24);
        root.setBackgroundColor(green);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        TextView title = text(titleText, 24, gold);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 15, 0, 25);
        root.addView(title);

        setContentView(scroll);
    }

    private void showHome() {
        selectedObject = -1;
        prepareScreen("📦  УЧЁТ МАТЕРИАЛОВ");

        TextView subtitle = text("МОИ ОБЪЕКТЫ", 20, white);
        subtitle.setTypeface(null, Typeface.BOLD);
        root.addView(subtitle);

        int active = 0;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject item = objects.optJSONObject(i);
            if (item != null && !item.optBoolean("archived", false)) {
                active++;
            }
        }

        TextView count = text("Активных объектов: " + active, 16, gold);
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
            if (item == null || item.optBoolean("archived", false)) {
                continue;
            }

            final int index = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(18, 18, 18, 18);
            card.setBackground(shape(darkGreen, 18));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = 14;
            root.addView(card, cp);

            TextView name = text(item.optString("name"), 20, gold);
            name.setTypeface(null, Typeface.BOLD);
            card.addView(name);

            TextView address = text(
                "Адрес: " + item.optString("address"), 14, white
            );
            address.setPadding(0, 10, 0, 5);
            card.addView(address);

            TextView responsible = text(
                "Ответственный: " + item.optString("responsible"),
                14, white
            );
            card.addView(responsible);

            JSONArray materials = item.optJSONArray("materials");
            int materialCount = materials == null ? 0 : materials.length();

            TextView info = text("Материалов: " + materialCount, 14, gold);
            info.setPadding(0, 10, 0, 0);
            card.addView(info);

            TextView open = text("ОТКРЫТЬ ОБЪЕКТ  →", 15, gold);
            open.setPadding(0, 18, 0, 0);
            card.addView(open);

            card.setOnClickListener(v -> showObject(index));
        }
    }

    private void addObject() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
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

        dialog.setOnShowListener(v ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        name.setError("Введите название объекта");
                        return;
                    }

                    JSONObject item = new JSONObject();
                    try {
                        item.put("name", n);
                        item.put("address", address.getText().toString().trim());
                        item.put("responsible", responsible.getText().toString().trim());
                        item.put("archived", false);
                        item.put("materials", new JSONArray());
                        objects.put(item);
                    } catch (Exception e) {
                        Toast.makeText(this, "Не удалось сохранить объект",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveObjects();
                    dialog.dismiss();
                    showHome();
                })
        );

        dialog.show();
    }

    private void showObject(int index) {
        JSONObject item = objects.optJSONObject(index);
        if (item == null) return;

        selectedObject = index;
        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) {
            materials = new JSONArray();
            try {
                item.put("materials", materials);
                saveObjects();
            } catch (Exception ignored) { }
        }

        prepareScreen("🏗  ОБЪЕКТ");

        TextView name = text(item.optString("name"), 23, gold);
        name.setTypeface(null, Typeface.BOLD);
        root.addView(name);

        TextView address = text("Адрес: " + item.optString("address"), 15, white);
        address.setPadding(0, 12, 0, 5);
        root.addView(address);

        TextView responsible = text(
            "Ответственный: " + item.optString("responsible"), 15, white
        );
        responsible.setPadding(0, 0, 0, 20);
        root.addView(responsible);

        Button materialButton = makeButton("📦  МАТЕРИАЛЫ");
        root.addView(materialButton, buttonParams());
        materialButton.setOnClickListener(v -> showMaterials(index));

        Button history = makeButton("📋  ИСТОРИЯ ОПЕРАЦИЙ");
        root.addView(history, buttonParams());
        history.setOnClickListener(v -> showHistory(index));

        Button transfer = makeButton("⇄  ПЕРЕМЕСТИТЬ МАТЕРИАЛЫ");
        root.addView(transfer, buttonParams());
        transfer.setOnClickListener(v -> showTransfer(index));

        Button excel = makeButton("📊  СОХРАНИТЬ ОТЧЁТ EXCEL");
        root.addView(excel, buttonParams());
        excel.setOnClickListener(v -> createExcelReport(index));

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
                    } catch (Exception ignored) { }
                    saveObjects();
                    showHome();
                })
                .show()
        );
    }



    private int pendingExcelObjectIndex = -1;

    private void createExcelReport(int objectIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        String fileName = item.optString("name", "Объект")
            .replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (fileName.isEmpty()) fileName = "Объект";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.putExtra(Intent.EXTRA_TITLE, "Отчёт_" + fileName + ".xlsx");
        pendingExcelObjectIndex = objectIndex;
        startActivityForResult(intent, 501);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 501 && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            try {
                writeExcelReport(data.getData(), pendingExcelObjectIndex);
                Toast.makeText(this, "Отчёт Excel сохранён", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка сохранения отчёта: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == 501) pendingExcelObjectIndex = -1;
    }

    private void writeExcelReport(android.net.Uri uri, int objectIndex)
            throws Exception {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) throw new IOException("Объект не найден");

        JSONArray materials = item.optJSONArray("materials");
        String objectName = item.optString("name", "");

        OutputStream output = getContentResolver().openOutputStream(uri);
        if (output == null) throw new IOException("Не удалось открыть файл");

        ZipOutputStream zip = new ZipOutputStream(output);
        try {
            addExcelEntry(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>");

            addExcelEntry(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>");

            addExcelEntry(zip, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"Текущее состояние\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "<sheet name=\"Поступления\" sheetId=\"2\" r:id=\"rId2\"/>"
                + "<sheet name=\"Расходы\" sheetId=\"3\" r:id=\"rId3\"/>"
                + "</sheets></workbook>");

            addExcelEntry(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>"
                + "</Relationships>");

            // Лист 1. Текущий остаток материалов на выбранном объекте.
            StringBuilder balanceRows = new StringBuilder();
            balanceRows.append(excelRow(new String[]{
                "№", "Материал", "Единица измерения", "Остаток"
            }, null));

            if (materials != null) {
                int number = 1;
                for (int i = 0; i < materials.length(); i++) {
                    JSONObject material = materials.optJSONObject(i);
                    if (material == null) continue;
                    balanceRows.append(excelRow(new String[]{
                        String.valueOf(number++),
                        material.optString("name", ""),
                        material.optString("unit", "шт."),
                        formatNumber(material.optDouble("quantity", 0))
                    }, new boolean[]{true, false, false, true}));
                }
            }
            addExcelEntry(zip, "xl/worksheets/sheet1.xml",
                excelSheet(balanceRows.toString()));

            // Лист 2. Только поступления. Перемещения сюда не включаются.
            StringBuilder receiptRows = new StringBuilder();
            receiptRows.append(excelRow(new String[]{
                "Дата", "Материал", "Количество", "Единица измерения", "Объект"
            }, null));
            if (materials != null) {
                for (int i = 0; i < materials.length(); i++) {
                    JSONObject material = materials.optJSONObject(i);
                    if (material == null) continue;
                    appendExcelRecords(receiptRows, material, "receipts", objectName);
                }
            }
            addExcelEntry(zip, "xl/worksheets/sheet2.xml",
                excelSheet(receiptRows.toString()));

            // Лист 3. Только расходы. Перемещения сюда не включаются.
            StringBuilder expenseRows = new StringBuilder();
            expenseRows.append(excelRow(new String[]{
                "Дата", "Материал", "Количество", "Единица измерения", "Объект"
            }, null));
            if (materials != null) {
                for (int i = 0; i < materials.length(); i++) {
                    JSONObject material = materials.optJSONObject(i);
                    if (material == null) continue;
                    appendExcelRecords(expenseRows, material, "expenses", objectName);
                }
            }
            addExcelEntry(zip, "xl/worksheets/sheet3.xml",
                excelSheet(expenseRows.toString()));
        } finally {
            zip.close();
        }
    }

    private void appendExcelRecords(StringBuilder rows, JSONObject material,
            String key, String objectName) {
        JSONArray records = material.optJSONArray(key);
        if (records == null) return;
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            if (record == null) continue;
            rows.append(excelRow(new String[]{
                record.optString("date", ""),
                material.optString("name", ""),
                formatNumber(record.optDouble("quantity", 0)),
                material.optString("unit", "шт."),
                objectName
            }, new boolean[]{false, false, true, false, false}));
        }
    }

    private String excelSheet(String rows) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<sheetData>" + rows + "</sheetData></worksheet>";
    }

    private String excelRow(String[] values, boolean[] numeric) {
        StringBuilder row = new StringBuilder("<row>");
        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "" : values[i];
            boolean isNumeric = numeric != null && i < numeric.length && numeric[i];
            if (isNumeric) {
                String normalized = value.replace(",", ".");
                try {
                    double number = Double.parseDouble(normalized);
                    row.append("<c t=\"n\"><v>")
                        .append(number).append("</v></c>");
                } catch (Exception e) {
                    row.append("<c t=\"inlineStr\"><is><t>")
                        .append(xmlEscape(value))
                        .append("</t></is></c>");
                }
            } else {
                row.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(xmlEscape(value))
                    .append("</t></is></c>");
            }
        }
        row.append("</row>");
        return row.toString();
    }

    private String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private void addExcelEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes("UTF-8"));
        zip.closeEntry();
    }

    private void showHistory(int objectIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        selectedObject = objectIndex;
        prepareScreen("📋  ИСТОРИЯ ОПЕРАЦИЙ");

        TextView objectName = text(item.optString("name"), 18, gold);
        objectName.setPadding(0, 0, 0, 15);
        root.addView(objectName);

        JSONArray materials = item.optJSONArray("materials");
        ArrayList<JSONObject> history = new ArrayList<>();
        if (materials != null) {
            for (int i = 0; i < materials.length(); i++) {
                JSONObject material = materials.optJSONObject(i);
                if (material == null) continue;

                JSONArray receipts = material.optJSONArray("receipts");
                if (receipts != null) {
                    for (int j = 0; j < receipts.length(); j++) {
                        JSONObject record = receipts.optJSONObject(j);
                        if (record == null) continue;
                        try {
                            JSONObject event = new JSONObject(record.toString());
                            event.put("type", "ПОСТУПЛЕНИЕ");
                            event.put("material", material.optString("name"));
                            event.put("unit", material.optString("unit", "шт."));
                            history.add(event);
                        } catch (Exception ignored) { }
                    }
                }

                JSONArray expenses = material.optJSONArray("expenses");
                if (expenses != null) {
                    for (int j = 0; j < expenses.length(); j++) {
                        JSONObject record = expenses.optJSONObject(j);
                        if (record == null) continue;
                        try {
                            JSONObject event = new JSONObject(record.toString());
                            event.put("type", "РАСХОД");
                            event.put("material", material.optString("name"));
                            event.put("unit", material.optString("unit", "шт."));
                            history.add(event);
                        } catch (Exception ignored) { }
                    }
                }

                JSONArray transfers = material.optJSONArray("transfers");
                if (transfers != null) {
                    for (int j = 0; j < transfers.length(); j++) {
                        JSONObject record = transfers.optJSONObject(j);
                        if (record == null) continue;
                        try {
                            JSONObject event = new JSONObject(record.toString());
                            String direction = event.optString("direction", "");
                            event.put("type", "OUT".equals(direction)
                                ? "ПЕРЕМЕЩЕНИЕ → " + event.optString("otherObject")
                                : "ПЕРЕМЕЩЕНИЕ ← " + event.optString("otherObject"));
                            event.put("material", material.optString("name"));
                            event.put("unit", material.optString("unit", "шт."));
                            history.add(event);
                        } catch (Exception ignored) { }
                    }
                }
            }
        }

        Collections.sort(history, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject first, JSONObject second) {
                String a = first.optString("date", "");
                String b = second.optString("date", "");
                try {
                    SimpleDateFormat parser = new SimpleDateFormat(
                        "dd.MM.yyyy HH:mm", Locale.getDefault());
                    Date dateA = parser.parse(a);
                    Date dateB = parser.parse(b);
                    if (dateA == null || dateB == null) return b.compareTo(a);
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return b.compareTo(a);
                }
            }
        });

        if (history.isEmpty()) {
            TextView empty = text("Операций пока нет", 16, white);
            empty.setPadding(0, 20, 0, 20);
            root.addView(empty);
        } else {
            TextView total = text("Всего операций: " + history.size(), 16, gold);
            total.setPadding(0, 0, 0, 15);
            root.addView(total);

            for (JSONObject event : history) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(18, 16, 18, 16);
                card.setBackground(shape(darkGreen, 16));
                LinearLayout.LayoutParams cp =
                    new LinearLayout.LayoutParams(-1, -2);
                cp.bottomMargin = 12;
                root.addView(card, cp);

                String type = event.optString("type", "ОПЕРАЦИЯ");
                TextView typeText = text(type, 18, gold);
                typeText.setTypeface(null, Typeface.BOLD);
                card.addView(typeText);

                TextView materialText = text(
                    "Материал: " + event.optString("material"), 16, white);
                materialText.setPadding(0, 7, 0, 4);
                card.addView(materialText);

                String sign;
                if ("ПОСТУПЛЕНИЕ".equals(type)
                        || type.startsWith("ПЕРЕМЕЩЕНИЕ ←")) {
                    sign = "+";
                } else {
                    sign = "−";
                }
                TextView quantityText = text(
                    "Количество: " + sign
                        + formatNumber(event.optDouble("quantity", 0))
                        + " " + event.optString("unit", "шт."), 16, white);
                card.addView(quantityText);

                TextView dateText = text(
                    "Дата: " + event.optString("date", "не указана"), 14, white);
                dateText.setPadding(0, 5, 0, 0);
                card.addView(dateText);
            }
        }

        Button back = makeButton("← НАЗАД К ОБЪЕКТУ");
        root.addView(back, buttonParams());
        back.setOnClickListener(v -> showObject(objectIndex));
    }

    private void showMaterials(int objectIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        selectedObject = objectIndex;
        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) {
            materials = new JSONArray();
            try {
                item.put("materials", materials);
                saveObjects();
            } catch (Exception ignored) { }
        }

        // Сохраняем окончательную ссылку для обработчиков кнопок
        final JSONArray savedMaterials = materials;

        prepareScreen("📦  МАТЕРИАЛЫ");

        TextView objectName = text(item.optString("name"), 18, gold);
        objectName.setPadding(0, 0, 0, 15);
        root.addView(objectName);

        Button add = makeButton("＋ ДОБАВИТЬ МАТЕРИАЛ");
        root.addView(add, buttonParams());
        add.setOnClickListener(v -> addMaterial(objectIndex, -1));

        if (savedMaterials.length() == 0) {
            TextView empty = text("Пока материалов нет", 16, white);
            empty.setPadding(0, 20, 0, 20);
            root.addView(empty);
        }

        for (int i = 0; i < savedMaterials.length(); i++) {
            JSONObject material = savedMaterials.optJSONObject(i);
            if (material == null) continue;

            final int materialIndex = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(18, 16, 18, 16);
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

            TextView amount = text(
                "Количество: " + formatNumber(material.optDouble("quantity", 0))
                    + " " + material.optString("unit", "шт."),
                16, white
            );
            amount.setPadding(0, 8, 0, 12);
            card.addView(amount);

            Button receipt = makeButton("＋ ПОСТУПЛЕНИЕ");
            card.addView(receipt, buttonParams());
            receipt.setOnClickListener(v -> addReceipt(objectIndex, materialIndex));

            Button expense = makeButton("− РАСХОД");
            card.addView(expense, buttonParams());
            expense.setOnClickListener(v -> addExpense(objectIndex, materialIndex));

            Button edit = makeButton("ИЗМЕНИТЬ");
            card.addView(edit, buttonParams());
            edit.setOnClickListener(v -> addMaterial(objectIndex, materialIndex));

            Button delete = makeButton("УДАЛИТЬ");
            card.addView(delete, buttonParams());
            delete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle("Удалить материал?")
                    .setMessage("Материал будет удалён из списка этого объекта.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Удалить", (d, w) -> {
                        JSONArray updated = new JSONArray();
                        for (int j = 0; j < savedMaterials.length(); j++) {
                            if (j != materialIndex) {
                                JSONObject keep = savedMaterials.optJSONObject(j);
                                if (keep != null) updated.put(keep);
                            }
                        }
                        try {
                            item.put("materials", updated);
                        } catch (Exception ignored) { }
                        saveObjects();
                        showMaterials(objectIndex);
                    })
                    .show()
            );
        }

        Button back = makeButton("← НАЗАД К ОБЪЕКТУ");
        root.addView(back, buttonParams());
        back.setOnClickListener(v -> showObject(objectIndex));
    }


    private void showTransfer(int sourceObjectIndex) {
        JSONObject sourceObject = objects.optJSONObject(sourceObjectIndex);
        if (sourceObject == null) return;

        JSONArray sourceMaterials = sourceObject.optJSONArray("materials");
        if (sourceMaterials == null || sourceMaterials.length() == 0) {
            Toast.makeText(this, "Сначала добавьте материалы на этот объект",
                Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Integer> destinationIndexes = new ArrayList<>();
        ArrayList<String> destinationNames = new ArrayList<>();
        for (int i = 0; i < objects.length(); i++) {
            JSONObject candidate = objects.optJSONObject(i);
            if (candidate == null || i == sourceObjectIndex
                    || candidate.optBoolean("archived", false)) {
                continue;
            }
            destinationIndexes.add(i);
            destinationNames.add(candidate.optString("name", "Объект"));
        }

        if (destinationIndexes.isEmpty()) {
            Toast.makeText(this, "Нет другого активного объекта для перемещения",
                Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(25, 10, 25, 10);

        TextView destinationLabel = text("Объект-получатель", 14, Color.BLACK);
        form.addView(destinationLabel);

        Spinner destinationSpinner = new Spinner(this);
        ArrayAdapter<String> destinationAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, destinationNames);
        destinationAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(destinationAdapter);
        form.addView(destinationSpinner);

        TextView materialLabel = text("Материал для перемещения", 14, Color.BLACK);
        materialLabel.setPadding(0, 15, 0, 0);
        form.addView(materialLabel);

        ArrayList<Integer> materialIndexes = new ArrayList<>();
        ArrayList<String> materialNames = new ArrayList<>();
        for (int i = 0; i < sourceMaterials.length(); i++) {
            JSONObject material = sourceMaterials.optJSONObject(i);
            if (material == null) continue;
            materialIndexes.add(i);
            materialNames.add(material.optString("name") + " — остаток: "
                + formatNumber(material.optDouble("quantity", 0)) + " "
                + material.optString("unit", "шт."));
        }

        Spinner materialSpinner = new Spinner(this);
        ArrayAdapter<String> materialAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, materialNames);
        materialAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        materialSpinner.setAdapter(materialAdapter);
        form.addView(materialSpinner);

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Количество для перемещения");
        quantityInput.setInputType(
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(quantityInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Перемещение материалов")
            .setView(form)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Переместить", null)
            .create();

        dialog.setOnShowListener(v ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    if (materialIndexes.isEmpty()) {
                        Toast.makeText(this, "Нет доступных материалов",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int destinationPosition = destinationSpinner.getSelectedItemPosition();
                    int materialPosition = materialSpinner.getSelectedItemPosition();
                    if (destinationPosition < 0 || materialPosition < 0) return;

                    int destinationIndex = destinationIndexes.get(destinationPosition);
                    int materialIndex = materialIndexes.get(materialPosition);

                    JSONObject sourceMaterial = sourceMaterials.optJSONObject(materialIndex);
                    JSONObject destinationObject = objects.optJSONObject(destinationIndex);
                    if (sourceMaterial == null || destinationObject == null) return;

                    String value = quantityInput.getText().toString()
                        .trim().replace(',', '.');
                    double amount;
                    try {
                        amount = Double.parseDouble(value);
                        if (amount <= 0 || Double.isNaN(amount)
                                || Double.isInfinite(amount)) {
                            throw new NumberFormatException();
                        }
                    } catch (Exception e) {
                        quantityInput.setError("Введите количество больше нуля");
                        return;
                    }

                    double sourceBalance = sourceMaterial.optDouble("quantity", 0);
                    if (amount > sourceBalance) {
                        quantityInput.setError("Недостаточно материала. Доступно: "
                            + formatNumber(sourceBalance));
                        return;
                    }

                    String materialName = sourceMaterial.optString("name");
                    String unit = sourceMaterial.optString("unit", "шт.");
                    JSONArray destinationMaterials =
                        destinationObject.optJSONArray("materials");
                    if (destinationMaterials == null) {
                        destinationMaterials = new JSONArray();
                    }

                    JSONObject destinationMaterial = null;
                    for (int i = 0; i < destinationMaterials.length(); i++) {
                        JSONObject candidate = destinationMaterials.optJSONObject(i);
                        if (candidate != null
                                && candidate.optString("name").trim()
                                    .equalsIgnoreCase(materialName.trim())
                                && candidate.optString("unit", "шт.").equals(unit)) {
                            destinationMaterial = candidate;
                            break;
                        }
                    }

                    boolean createDestinationMaterial = destinationMaterial == null;
                    if (createDestinationMaterial) {
                        destinationMaterial = new JSONObject();
                    }

                    String date = new SimpleDateFormat(
                        "dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
                    String sourceName = sourceObject.optString("name");
                    String destinationName =
                        destinationObject.optString("name");

                    try {
                        sourceMaterial.put("quantity", sourceBalance - amount);
                        destinationMaterial.put("name", materialName);
                        destinationMaterial.put("unit", unit);
                        destinationMaterial.put("quantity",
                            destinationMaterial.optDouble("quantity", 0) + amount);

                        if (createDestinationMaterial) {
                            destinationMaterials.put(destinationMaterial);
                        }

                        JSONObject outgoing = new JSONObject();
                        outgoing.put("quantity", amount);
                        outgoing.put("date", date);
                        outgoing.put("direction", "OUT");
                        outgoing.put("otherObject", destinationName);

                        JSONArray sourceTransfers =
                            sourceMaterial.optJSONArray("transfers");
                        if (sourceTransfers == null) sourceTransfers = new JSONArray();
                        sourceTransfers.put(outgoing);
                        sourceMaterial.put("transfers", sourceTransfers);

                        JSONObject incoming = new JSONObject();
                        incoming.put("quantity", amount);
                        incoming.put("date", date);
                        incoming.put("direction", "IN");
                        incoming.put("otherObject", sourceName);

                        JSONArray destinationTransfers =
                            destinationMaterial.optJSONArray("transfers");
                        if (destinationTransfers == null) {
                            destinationTransfers = new JSONArray();
                        }
                        destinationTransfers.put(incoming);
                        destinationMaterial.put("transfers", destinationTransfers);

                        sourceObject.put("materials", sourceMaterials);
                        destinationObject.put("materials", destinationMaterials);
                    } catch (Exception e) {
                        Toast.makeText(this, "Не удалось выполнить перемещение",
                            Toast.LENGTH_LONG).show();
                        return;
                    }

                    saveObjects();
                    dialog.dismiss();
                    showObject(sourceObjectIndex);
                    Toast.makeText(this, "Материал перемещён на объект: "
                        + destinationName, Toast.LENGTH_LONG).show();
                })
        );

        dialog.show();
    }

    private void addMaterial(int objectIndex, int materialIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) {
            materials = new JSONArray();
            try {
                item.put("materials", materials);
            } catch (Exception ignored) { }
        }

        JSONObject old = materialIndex >= 0
            ? materials.optJSONObject(materialIndex) : null;

        final JSONObject existingMaterial = old;
        final JSONArray savedMaterials = materials;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(25, 5, 25, 5);

        EditText name = new EditText(this);
        name.setHint("Название материала");
        if (existingMaterial != null) {
            name.setText(existingMaterial.optString("name"));
        }
        form.addView(name);

        TextView unitLabel = text("Единица измерения", 14, Color.BLACK);
        form.addView(unitLabel);

        Spinner unitSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, units
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        unitSpinner.setAdapter(adapter);

        if (existingMaterial != null) {
            String oldUnit = existingMaterial.optString("unit", "шт.");
            for (int i = 0; i < units.length; i++) {
                if (units[i].equals(oldUnit)) {
                    unitSpinner.setSelection(i);
                    break;
                }
            }
        }
        form.addView(unitSpinner);

        EditText quantity = new EditText(this);
        quantity.setHint("Количество");
        quantity.setInputType(
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        if (existingMaterial != null) {
            quantity.setText(formatNumber(
                existingMaterial.optDouble("quantity", 0)
            ));
        } else {
            quantity.setText("0");
        }
        form.addView(quantity);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(existingMaterial == null ? "Новый материал" : "Изменить материал")
            .setView(form)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(v ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    String n = name.getText().toString().trim();
                    String q = quantity.getText().toString().trim().replace(',', '.');

                    if (n.isEmpty()) {
                        name.setError("Введите название материала");
                        return;
                    }

                    double amount;
                    try {
                        amount = Double.parseDouble(q);
                        if (amount < 0 || Double.isNaN(amount)
                                || Double.isInfinite(amount)) {
                            throw new NumberFormatException();
                        }
                    } catch (Exception e) {
                        quantity.setError("Введите правильное количество");
                        return;
                    }

                    JSONObject material = existingMaterial;
                    if (material == null) {
                        material = new JSONObject();
                    }

                    try {
                        material.put("name", n);
                        material.put("unit", unitSpinner.getSelectedItem().toString());
                        material.put("quantity", amount);

                        if (existingMaterial == null) {
                            savedMaterials.put(material);
                        }
                        item.put("materials", savedMaterials);
                    } catch (Exception e) {
                        Toast.makeText(this, "Не удалось сохранить материал",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveObjects();
                    dialog.dismiss();
                    showMaterials(objectIndex);
                })
        );

        dialog.show();
    }

    private void addReceipt(int objectIndex, int materialIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) return;

        JSONObject material = materials.optJSONObject(materialIndex);
        if (material == null) return;

        EditText quantity = new EditText(this);
        quantity.setHint("Количество поступившего материала");
        quantity.setInputType(
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(25, 10, 25, 10);

        TextView info = text(
            "Материал: " + material.optString("name") + "\nТекущий остаток: "
                + formatNumber(material.optDouble("quantity", 0)) + " "
                + material.optString("unit", "шт."),
            15, Color.BLACK
        );
        form.addView(info);
        form.addView(quantity);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Поступление материала")
            .setView(form)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Добавить", null)
            .create();

        dialog.setOnShowListener(v ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    String value = quantity.getText().toString().trim().replace(',', '.');
                    double received;
                    try {
                        received = Double.parseDouble(value);
                        if (received <= 0 || Double.isNaN(received)
                                || Double.isInfinite(received)) {
                            throw new NumberFormatException();
                        }
                    } catch (Exception e) {
                        quantity.setError("Введите количество больше нуля");
                        return;
                    }

                    double current = material.optDouble("quantity", 0);
                    try {
                        material.put("quantity", current + received);

                        JSONArray receipts = material.optJSONArray("receipts");
                        if (receipts == null) receipts = new JSONArray();

                        JSONObject receiptRecord = new JSONObject();
                        String date = new SimpleDateFormat(
                            "dd.MM.yyyy HH:mm", Locale.getDefault()
                        ).format(new Date());
                        receiptRecord.put("quantity", received);
                        receiptRecord.put("date", date);
                        receipts.put(receiptRecord);
                        material.put("receipts", receipts);
                        item.put("materials", materials);
                    } catch (Exception e) {
                        Toast.makeText(this, "Не удалось сохранить поступление",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveObjects();
                    dialog.dismiss();
                    showMaterials(objectIndex);
                    Toast.makeText(this, "Поступление добавлено", Toast.LENGTH_SHORT).show();
                })
        );

        dialog.show();
    }

    private void addExpense(int objectIndex, int materialIndex) {
        JSONObject item = objects.optJSONObject(objectIndex);
        if (item == null) return;

        JSONArray materials = item.optJSONArray("materials");
        if (materials == null) return;

        JSONObject material = materials.optJSONObject(materialIndex);
        if (material == null) return;

        EditText quantity = new EditText(this);
        quantity.setHint("Количество израсходованного материала");
        quantity.setInputType(
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(25, 10, 25, 10);

        TextView info = text(
            "Материал: " + material.optString("name") + "\nТекущий остаток: "
                + formatNumber(material.optDouble("quantity", 0)) + " "
                + material.optString("unit", "шт."),
            15, Color.BLACK
        );
        form.addView(info);
        form.addView(quantity);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Расход материала")
            .setView(form)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Списать", null)
            .create();

        dialog.setOnShowListener(v ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    String value = quantity.getText().toString().trim().replace(',', '.');
                    double spent;
                    try {
                        spent = Double.parseDouble(value);
                        if (spent <= 0 || Double.isNaN(spent)
                                || Double.isInfinite(spent)) {
                            throw new NumberFormatException();
                        }
                    } catch (Exception e) {
                        quantity.setError("Введите количество больше нуля");
                        return;
                    }

                    double current = material.optDouble("quantity", 0);
                    if (spent > current) {
                        quantity.setError(
                            "Недостаточно материала. Доступно: " + formatNumber(current)
                        );
                        Toast.makeText(this, "Нельзя списать больше остатка",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        material.put("quantity", current - spent);

                        JSONArray expenses = material.optJSONArray("expenses");
                        if (expenses == null) expenses = new JSONArray();

                        JSONObject expenseRecord = new JSONObject();
                        String date = new SimpleDateFormat(
                            "dd.MM.yyyy HH:mm", Locale.getDefault()
                        ).format(new Date());
                        expenseRecord.put("quantity", spent);
                        expenseRecord.put("date", date);
                        expenses.put(expenseRecord);
                        material.put("expenses", expenses);
                        item.put("materials", materials);
                    } catch (Exception e) {
                        Toast.makeText(this, "Не удалось сохранить расход",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveObjects();
                    dialog.dismiss();
                    showMaterials(objectIndex);
                    Toast.makeText(this, "Расход записан", Toast.LENGTH_SHORT).show();
                })
        );

        dialog.show();
    }

    private String formatNumber(double value) {
        DecimalFormatSymbols symbols =
            new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("0.##", symbols);
        return format.format(value);
    }
}
