package com.example.ques5;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;  // Import for TextView
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ImageDetailsActivity";
    private String imagePath;
    private ImageView detailImageView; // Changed from fullImageView to detailImageView
    private Button deleteButton;
    private TextView nameTextView;
    private TextView pathTextView;
    private TextView sizeTextView;
    private TextView dateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);  // Create a new layout file

        // Initialize UI elements
        detailImageView = findViewById(R.id.detailImageView);
        deleteButton = findViewById(R.id.deleteButton);
        nameTextView = findViewById(R.id.detailNameTextView);
        pathTextView = findViewById(R.id.detailPathTextView);
        sizeTextView = findViewById(R.id.detailSizeTextView);
        dateTextView = findViewById(R.id.detailDateTextView);


        // Get the image path from the intent
        imagePath = getIntent().getStringExtra("imagePath");
        if (imagePath == null) {
            Log.e(TAG, "Image path is null");
            finish();
            return;
        }

        // Load and display the full-screen image
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            detailImageView.setImageBitmap(bitmap);

            // Set the image details
            nameTextView.setText("Name: " + imageFile.getName());
            pathTextView.setText("Path: " + imageFile.getAbsolutePath());
            sizeTextView.setText("Size: " + formatFileSize(imageFile.length())); // Implement this
            Date date = new Date(imageFile.lastModified());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            dateTextView.setText("Date Taken: " + dateFormat.format(date));

        } else {
            Log.e(TAG, "Image file does not exist: " + imagePath);
            finish();
            return;
        }

        // Set click listener for the delete button
        deleteButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Delete Image")
                    .setMessage("Are you sure you want to delete this image?")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // Delete the image
                        boolean deleted = deleteImage();
                        if (deleted) {
                            // Return to the gallery view and indicate that the image was deleted
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("isDeleted", true);
                            setResult(Activity.RESULT_OK, resultIntent);
                            finish(); // Close this activity
                        } else {
                            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        // Do nothing, just close the dialog
                        dialog.dismiss();
                    })
                    .show();
        });
    }

    private boolean deleteImage() {
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            return imageFile.delete();
        } else {
            return false;
        }
    }

    private String formatFileSize(long sizeInBytes) {
        long kb = sizeInBytes / 1024;
        if (kb < 1024) {
            return kb + " KB";
        }
        long mb = kb / 1024;
        if (mb < 1024) {
            return mb + " MB";
        }
        long gb = mb / 1024;
        return gb + " GB";
    }
}

