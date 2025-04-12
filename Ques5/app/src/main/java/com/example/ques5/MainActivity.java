package com.example.ques5;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.provider.DocumentsContract;
import android.database.Cursor;
import android.content.ContentUris;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> openDirectoryLauncher;
    private ActivityResultLauncher<Intent> viewImageDetailsLauncher;

    private String currentPhotoPath;
    private String selectedImageDirectory;
    private GridLayout imagesGridLayout;
    private final ArrayList<File> images = new ArrayList<>();

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private final String[] READ_MEDIA_IMAGES_PERMISSIONS =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? new String[] {Manifest.permission.READ_MEDIA_IMAGES}
                    : new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        Button viewImagesButton = findViewById(R.id.viewImagesButton);
        imagesGridLayout = findViewById(R.id.imagesGridLayout);

        // Initialize ActivityResultLaunchers
        takePictureLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                // Handle the result of taking a picture
                                if (currentPhotoPath != null) {
                                    // Photo was taken successfully, save it to the selected directory
                                    saveImageToDirectory(currentPhotoPath);
                                } else {
                                    Toast.makeText(
                                                    this,
                                                    getString(R.string.error_taking_photo),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            } else {
                                Toast.makeText(
                                                this,
                                                getString(R.string.cancelled_taking_photo),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

        openDirectoryLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                                // Handle the result of selecting a directory
                                Uri uri = result.getData().getData();
                                if (uri != null) {
                                    selectedImageDirectory = getPathFromUri(uri);
                                    if (selectedImageDirectory != null) {
                                        Toast.makeText(
                                                        this,
                                                        String.format(
                                                                getString(R.string.directory_selected),
                                                                selectedImageDirectory),
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                        loadImagesFromDirectory(selectedImageDirectory); // Load images after directory selection
                                    } else {
                                        Toast.makeText(
                                                        this,
                                                        getString(R.string.failed_to_get_directory_path),
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            } else {
                                Toast.makeText(
                                                this,
                                                getString(R.string.cancelled_selecting_directory),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

        viewImageDetailsLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                // Reload images after returning from detail view, if image was deleted
                                if (selectedImageDirectory != null) {
                                    loadImagesFromDirectory(selectedImageDirectory);
                                }
                            }
                        });

        // Set click listeners for the buttons
        takePhotoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Check for permissions before taking a photo
                        if (checkCameraAndStoragePermissions()) {
                            takePhoto();
                        }
                    }
                });
        viewImagesButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Check for permissions before accessing the directory
                        if (checkReadExternalStoragePermission()) {
                            openDirectoryPicker();
                        }
                    }
                });
    }

    private boolean checkCameraAndStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permissions
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkReadExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, READ_MEDIA_IMAGES_PERMISSIONS, PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all requested permissions were granted
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Permissions were granted, proceed with the operation
                if (permissions.length > 0 && Manifest.permission.CAMERA.equals(permissions[0])) {
                    takePhoto();
                } else if (permissions.length > 0
                        && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Manifest.permission.READ_MEDIA_IMAGES.equals(permissions[0])
                        : Manifest.permission.READ_EXTERNAL_STORAGE.equals(
                        permissions[0]))) {
                    openDirectoryPicker();
                }
            } else {
                // Handle the case where permissions were denied
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_denied))
                .setMessage(getString(R.string.permission_required_message))
                .setPositiveButton(
                        getString(R.string.go_to_settings),
                        (dialog, which) -> {
                            // Open app settings
                            Intent intent =
                                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void takePhoto() {
        // Create an intent to capture a photo
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Error creating image file: " + ex.getMessage()); // Use TAG
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI =
                        FileProvider.getUriForFile(
                                this,
                                "com.example.ques5.fileprovider",
                                photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, getString(R.string.no_camera_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName, /* prefix */ ".jpg", /* suffix */ storageDir /* directory */);
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void saveImageToDirectory(String imagePath) {
        if (selectedImageDirectory == null) {
            Toast.makeText(this, getString(R.string.please_select_directory), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        File sourceFile = new File(imagePath);
        File destFile = new File(selectedImageDirectory + File.separator + sourceFile.getName());

        Log.d(TAG, "Source file: " + sourceFile.getAbsolutePath());
        Log.d(TAG, "Destination file: " + destFile.getAbsolutePath());

        try {
            // Use Java NIO for file copy
            java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Toast.makeText(
                            this,
                            String.format(
                                    getString(R.string.image_saved_to), destFile.getAbsolutePath()),
                            Toast.LENGTH_SHORT)
                    .show();
            // Delete the temporary file.
            sourceFile.delete();
            currentPhotoPath = null;
            loadImagesFromDirectory(selectedImageDirectory); //refresh
        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage(), e); // Log the full exception
            Toast.makeText(this, getString(R.string.error_saving_image), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void openDirectoryPicker() {
        // Use ACTION_OPEN_DOCUMENT_TREE to allow the user to select a directory
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); // Retain access across reboots
        openDirectoryLauncher.launch(intent);
    }

    private String getPathFromUri(Uri uri) {
        Log.d(TAG, "getPathFromUri: Uri: " + uri.toString());
        String path = null;

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            ContentResolver contentResolver = getContentResolver();
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    if (columnIndex != -1) {
                        path = cursor.getString(columnIndex);
                        Log.d(TAG, "getPathFromUri: Content URI path (MediaStore): " + path);
                        return path;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting path from MediaStore: " + e.getMessage(), e);
            }

            // Try DocumentsContract if MediaStore didn't work
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(this, uri)) { // Pass 'this' (Context)
                try {
                    String documentId = DocumentsContract.getDocumentId(uri);
                    Log.d(TAG, "getPathFromUri: DocumentsContract ID: " + documentId);

                    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                        String[] split = documentId.split(":");
                        String idType = split[0]; // Should be "image", "video", or "audio"
                        String id = split[1];
                        Uri contentUri = null;
                        if ("image".equals(idType)) {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        } else if ("video".equals(idType)) {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        } else if ("audio".equals(idType)) {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }

                        if (contentUri != null) {
                            String selection = MediaStore.MediaColumns._ID + "=?";
                            String[] selectionArgs = new String[]{id};
                            try (Cursor cursor = contentResolver.query(contentUri, new String[]{MediaStore.MediaColumns.DATA}, selection, selectionArgs, null)) {
                                if (cursor != null && cursor.moveToFirst()) {
                                    int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                                    if (columnIndex != -1) {
                                        path = cursor.getString(columnIndex);
                                        Log.d(TAG, "getPathFromUri: Content URI path (Media Provider): " + path);
                                        return path;
                                    }
                                }
                            }
                        }
                    } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                        Uri downloadUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.parseLong(documentId));
                        try (Cursor cursor = contentResolver.query(downloadUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                                if (columnIndex != -1) {
                                    path = cursor.getString(columnIndex);
                                    Log.d(TAG, "getPathFromUri: Content URI path (Downloads): " + path);
                                    return path;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting path from DocumentsContract: " + e.getMessage(), e);
                }
            }
            // Handle ACTION_OPEN_DOCUMENT_TREE URI
            else if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                String documentId = DocumentsContract.getTreeDocumentId(uri);
                Log.d(TAG, "getPathFromUri: Tree URI documentId: " + documentId);
                if (documentId != null) {
                    if (documentId.startsWith("primary:")) {
                        path = Environment.getExternalStorageDirectory().getPath() + File.separator + documentId.substring("primary:".length());
                        Log.d(TAG, "getPathFromUri:  Tree URI path: " + path);
                        return path;
                    }
                    else{
                        path = "/storage/" + documentId.replace(":", "/");
                        Log.d(TAG, "getPathFromUri:  Tree URI path: " + path);
                        return path;
                    }
                }
            }

        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
            Log.d(TAG, "getPathFromUri: File URI path: " + path);
            return path;
        }  else if ("android.resource".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
            Log.d(TAG, "getPathFromUri: Resource URI path: " + path);
            return path;
        }

        return path; // Return null if path is not found
    }



    private void loadImagesFromDirectory(String directoryPath) {
        images.clear();
        imagesGridLayout.removeAllViews(); // Clear previous images

        File directory = new File(directoryPath);
        Log.d(TAG, "loadImagesFromDirectory: Directory path: " + directoryPath);

        if (!directory.exists()) {
            Toast.makeText(this, getString(R.string.directory_does_not_exist), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            Log.d(TAG, "loadImagesFromDirectory: Number of files found: " + files.length);
            for (File file : files) {
                if (file.isFile() && isImageFile(file)) {
                    images.add(file);
                }
            }
        }
        if (images.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_images_found), Toast.LENGTH_SHORT).show();
            return;
        }

        for (File imageFile : images) {
            addImageToGrid(imageFile);
        }
    }

    private boolean isImageFile(File file) {
        String extension = getFileExtension(file.getName()).toLowerCase(Locale.getDefault());
        boolean isImage = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
        Log.d(TAG, "isImageFile: File: " + file.getAbsolutePath() + ", isImage: " + isImage);
        return isImage;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1);
            Log.d(TAG, "getFileExtension: File name: " + fileName + ", extension: " + extension);
            return extension;
        }
        Log.d(TAG, "getFileExtension: File name: " + fileName + ", extension: ");
        return "";
    }

    private void addImageToGrid(File imageFile) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(200, 200)); // Adjust size as needed
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Load the image using a background thread to avoid blocking the UI
        new Thread(
                () -> {
                    final Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    // Post the UI update back to the main thread
                    runOnUiThread(
                            () -> {
                                imageView.setImageBitmap(bitmap);
                            });
                })
                .start();

        imageView.setOnClickListener(
                v -> {
                    // Open image details activity
                    Intent intent = new Intent(this, ImageDetailsActivity.class);
                    intent.putExtra("imagePath", imageFile.getAbsolutePath());
                    viewImageDetailsLauncher.launch(intent);
                });
        imagesGridLayout.addView(imageView);
    }
}

