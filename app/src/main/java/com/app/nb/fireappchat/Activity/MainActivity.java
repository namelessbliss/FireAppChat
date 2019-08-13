package com.app.nb.fireappchat.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.nb.fireappchat.Adapter.MessageAdapter;
import com.app.nb.fireappchat.Model.FireAppMessage;
import com.app.nb.fireappchat.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private ListView lvMessage;
    private MessageAdapter adapter;
    private ProgressBar progressBar;
    private ImageButton photoPicker;
    private EditText etMessage;
    private Button btnSend;

    private String mUsername;
    List<FireAppMessage> fireAppMessages = new ArrayList<>();

    //Firebase instance variables
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference messageDatabaseReference;
    private ChildEventListener childEventListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseStorage firebaseStorage;
    private StorageReference chatPhotosStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        //Init firebase components
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        messageDatabaseReference = firebaseDatabase.getReference().child("messages");
        chatPhotosStorageReference = firebaseStorage.getReference().child("chat_photos");

        bindUi();


        adapter = new MessageAdapter(this, R.layout.list_view_item_message, fireAppMessages);
        lvMessage.setAdapter(adapter);

        progressBar.setVisibility(View.INVISIBLE);

        //shows an image picker to upload a image for a message
        photoPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    btnSend.setEnabled(true);
                } else {
                    btnSend.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FireAppMessage fireAppMessage = new FireAppMessage(etMessage.getText().toString(), mUsername, null);
                //add new message data
                messageDatabaseReference.push().setValue(fireAppMessage);
                etMessage.setText("");
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    //user signed out
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }


    private void bindUi() {
        lvMessage = findViewById(R.id.lvMessage);
        progressBar = findViewById(R.id.progressBar);
        photoPicker = findViewById(R.id.photoPicker);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {

            Uri selectedImageUri = data.getData();
            // get reference to store file  at folder in firebase(chat_photos)/<FILENAME>
            final StorageReference photoRef = chatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

            // upload file to Firebase storage
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            FireAppMessage fireAppMessage = new FireAppMessage(null, mUsername, downloadUrl);
                            messageDatabaseReference.push().setValue(fireAppMessage);
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signOut:
                // cerrar sesion
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        detachDatabaseReadListener();
        fireAppMessages.clear();
        adapter.notifyDataSetChanged();
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanUp() {
        mUsername = ANONYMOUS;
        fireAppMessages.clear();
        adapter.notifyDataSetChanged();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    //Deserialize message and add to chat list
                    FireAppMessage fireAppMessage = dataSnapshot.getValue(FireAppMessage.class);
                    fireAppMessages.add(fireAppMessage);
                    lvMessage.setSelection(adapter.getCount() - 1); // apply focus to last item
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            messageDatabaseReference.addChildEventListener(childEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (childEventListener != null) {
            messageDatabaseReference.removeEventListener(childEventListener);
            childEventListener = null;
        }
    }
}
