

package com.materials.accounting;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout root;
    int dark = Color.rgb(18, 65, 49);
    int gold = Color.rgb(232, 195, 105);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24, 30, 24, 24);
        root.setBackgroundColor(dark);

        TextView title = new TextView(this);
        title.setText("УЧЁТ МАТЕРИАЛОВ");
        title.setTextColor(gold);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 30);
        root.addView(title);

        addButton("Объекты", "Список строительных объектов");
        addButton("Материалы", "Каталог материалов");
        addButton("Приход материалов", "Регистрация поставок");
        addButton("Расход материалов", "Списание материалов");
        addButton("Остатки", "Текущие остатки");
        addButton("Отчёты Excel и PDF", "Формирование отчётов");

        setContentView(root);
    }

    void addButton(String name, String description) {
        Button button = new Button(this);
        button.setText(name);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(7, 45, 34)
            )
        );
        button.setOnClickListener(v ->
            Toast.makeText(this, description,
                Toast.LENGTH_SHORT).show()
        );

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                -1, 65
            );
        params.bottomMargin = 14;
        root.addView(button, params);
    }
}
