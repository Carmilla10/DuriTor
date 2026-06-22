package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    EditText email,password;
    Button signupButton;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        email = findViewById(R.id.signupEmail);
        password = findViewById(R.id.signupPassword);
        signupButton = findViewById(R.id.signupButton);

        mAuth = FirebaseAuth.getInstance();

        signupButton.setOnClickListener(v -> {

            String userEmail = email.getText().toString();
            String userPassword = password.getText().toString();

            if(userEmail.isEmpty() || userPassword.isEmpty()){
                Toast.makeText(this,"Please fill in all fields",Toast.LENGTH_SHORT).show();
                return;
            }

            if(userPassword.length() < 6){
                Toast.makeText(this,"Password must be at least 6 characters",Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(userEmail,userPassword)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful()){

                            Toast.makeText(this,"Signup Successful",Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(SignupActivity.this,LoginActivity.class));

                            finish();

                        }else{

                            Toast.makeText(this,"Signup Failed",Toast.LENGTH_SHORT).show();

                        }

                    });

        });

    }
}