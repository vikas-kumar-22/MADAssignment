package com.example.ques1;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    EditText inputValue;
    Spinner fromUnit, toUnit;
    Button convertButton, clearButton;
    TextView resultText;

    String[] units = {"Feet", "Inches", "Centimeters", "Meters", "Yards"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputValue = findViewById(R.id.inputValue);
        fromUnit = findViewById(R.id.fromUnit);
        toUnit = findViewById(R.id.toUnit);
        convertButton = findViewById(R.id.convertButton);
        clearButton = findViewById(R.id.clearButton);
        resultText = findViewById(R.id.resultText);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        fromUnit.setAdapter(adapter);
        toUnit.setAdapter(adapter);

        convertButton.setOnClickListener(v -> {
            String input = inputValue.getText().toString();
            if (input.isEmpty()) {
                resultText.setText("Please enter a value");
                return;
            }

            double value = Double.parseDouble(input);
            String from = fromUnit.getSelectedItem().toString();
            String to = toUnit.getSelectedItem().toString();

            double result = convertLength(value, from, to);
            resultText.setText(String.format("%.2f %s", result, to));
        });

        clearButton.setOnClickListener(v -> {
            inputValue.setText("");
            resultText.setText("Result");
        });
    }

    double convertLength(double value, String from, String to) {
        double inMeters = toMeters(value, from);
        return fromMeters(inMeters, to);
    }

    double toMeters(double value, String unit) {
        switch (unit) {
            case "Feet": return value * 0.3048;
            case "Inches": return value * 0.0254;
            case "Centimeters": return value / 100;
            case "Yards": return value * 0.9144;
            case "Meters": return value;
            default: return value;
        }
    }

    double fromMeters(double value, String unit) {
        switch (unit) {
            case "Feet": return value / 0.3048;
            case "Inches": return value / 0.0254;
            case "Centimeters": return value * 100;
            case "Yards": return value / 0.9144;
            case "Meters": return value;
            default: return value;
        }
    }
}
