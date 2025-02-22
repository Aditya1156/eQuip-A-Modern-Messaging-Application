package com.example.hackathon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Import for logging
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException; // Import for specific auth exceptions
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;


public class registration extends AppCompatActivity {
    private TextView loginbut; // Use private for encapsulation
    private EditText rg_username, rg_email , rg_password, rg_repassword;
    private Button rg_signup;
    private CircleImageView rg_profileImg;
    private FirebaseAuth auth;
    private Uri imageURI;
    private String imageuri;
    private final String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"; // Make it final if it's a constant
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Initialize Firebase instances
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating Account..."); // Use string resource
        progressDialog.setCancelable(false);

        // Hide action bar
        if (getSupportActionBar() != null) { // Check for null before hiding
            getSupportActionBar().hide();
        }

        // Initialize views
        loginbut = findViewById(R.id.loginbut);
        rg_username = findViewById(R.id.rgusername);
        rg_email = findViewById(R.id.rgemail);
        rg_password = findViewById(R.id.rgpassword);
        rg_repassword = findViewById(R.id.rgrepassword);
        rg_profileImg = findViewById(R.id.profilerg0);
        rg_signup = findViewById(R.id.signupbutton);

        // Set click listeners
        loginbut.setOnClickListener(v -> { // Use lambda for conciseness
            Intent intent = new Intent(registration.this, login.class);
            startActivity(intent);
            finish();
        });

        rg_signup.setOnClickListener(v -> {
            registerUser(); // Extract registration logic to a separate method
        });

        rg_profileImg.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 10); // Use string resource
        });
    }

    private void registerUser() {
        final String name = rg_username.getText().toString().trim(); // Clearer variable name, trim whitespace
        final String email = rg_email.getText().toString().trim();
        final String password = rg_password.getText().toString().trim();
        String confirmPassword = rg_repassword.getText().toString().trim();
        final String status = "Hey I'm Using This Application"; // Consider moving to string resource

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(registration.this, "Please fill in all fields", Toast.LENGTH_SHORT).show(); // Use string resource
            return; // Exit the method early
        }

        if (!email.matches(emailPattern)) {
            rg_email.setError("Invalid email address"); // Use setError to display error on EditText
            return;
        }

        if (password.length() < 6) {
            rg_password.setError("Password must be at least 6 characters"); // Use setError
            return;
        }

        if (!password.equals(confirmPassword)) {
            rg_repassword.setError("Passwords do not match"); // Use setError, target the correct EditText
            return;
        }

        // Show progress dialog *before* starting the registration process
        progressDialog.show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User creation successful
                        String userId = auth.getCurrentUser().getUid(); // Get user ID immediately
                        DatabaseReference reference = database.getReference().child("user").child(userId);
                        StorageReference storageReference = storage.getReference().child("Upload").child(userId);

                        if (imageURI != null) {
                            storageReference.putFile(imageURI)
                                    .addOnCompleteListener(uploadTask -> {
                                        if (uploadTask.isSuccessful()) {
                                            storageReference.getDownloadUrl()
                                                    .addOnSuccessListener(uri -> {
                                                        imageuri = uri.toString();
                                                        Users user = new Users(userId, name, email, password, imageuri, status);
                                                        reference.setValue(user)
                                                                .addOnCompleteListener(databaseTask -> {
                                                                    if (databaseTask.isSuccessful()) {
                                                                        // Data saved successfully
                                                                        progressDialog.dismiss(); // Dismiss on success
                                                                        Intent intent = new Intent(registration.this, MainActivity.class);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    } else {
                                                                        // Failed to save user data
                                                                        progressDialog.dismiss(); // Dismiss on failure
                                                                        Toast.makeText(registration.this, "Failed to save user data.", Toast.LENGTH_SHORT).show(); // Use string resource
                                                                        Log.e("Registration", "Database setValue failed: ", databaseTask.getException()); // Log the error
                                                                    }
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        // Failed to get download URL
                                                        progressDialog.dismiss(); // Dismiss on failure
                                                        Toast.makeText(registration.this, "Failed to get image URL.", Toast.LENGTH_SHORT).show();
                                                        Log.e("Registration", "Get download URL failed: ", e);
                                                    });
                                        } else {
                                            // Image upload failed
                                            progressDialog.dismiss(); // Dismiss on failure
                                            Toast.makeText(registration.this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                                            Log.e("Registration", "Image upload failed: ", uploadTask.getException());
                                        }
                                    });
                        } else {
                            // No image selected, use default
                            imageuri = "https://firebasestorage.googleapis.com/v0/b/av-messenger-dc8f3.appspot.com/o/man.png?alt=media&token=880f431d-9344-45e7-afe4-c2cafe8a5257"; // Consider storing this in Firebase Remote Config

                            Users user = new Users(userId, name, email, password, imageuri, status);
                            reference.setValue(user)
                                    .addOnCompleteListener(databaseTask -> {
                                        if (databaseTask.isSuccessful()) {
                                            // Data saved successfully
                                            progressDialog.dismiss(); // Dismiss on success
                                            Intent intent = new Intent(registration.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Failed to save user data
                                            progressDialog.dismiss(); // Dismiss on failure
                                            Toast.makeText(registration.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                            Log.e("Registration", "Database setValue failed: ", databaseTask.getException());
                                        }
                                    });
                        }
                    } else {
                        // User creation failed
                        progressDialog.dismiss(); // Dismiss on failure
                        String errorMessage = "Registration failed.";
                        if (task.getException() instanceof FirebaseAuthException) {
                            errorMessage = ((FirebaseAuthException) task.getException()).getMessage(); // Get specific Firebase Auth error
                        }
                        Toast.makeText(registration.this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("Registration", "Authentication failed: ", task.getException());
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null && data.getData() != null) { // Check all conditions
            imageURI = data.getData();
            rg_profileImg.setImageURI(imageURI);
        }
    }
}
