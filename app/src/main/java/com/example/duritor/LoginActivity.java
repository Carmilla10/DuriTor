package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText email,password;
    Button loginButton;
    TextView goSignup;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.loginEmail);
        password = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        goSignup = findViewById(R.id.goSignup);

        mAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(v -> {

            String userEmail = email.getText().toString();
            String userPassword = password.getText().toString();

            mAuth.signInWithEmailAndPassword(userEmail,userPassword)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful()){

                            Toast.makeText(this,"Login Successful",Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(LoginActivity.this,MainActivity.class));

                            finish();

                        }else{

                            Toast.makeText(this,"Login Failed",Toast.LENGTH_SHORT).show();

                        }

                    });

        });

        goSignup.setOnClickListener(v -> {

            startActivity(new Intent(LoginActivity.this,SignupActivity.class));

        });

    }
}