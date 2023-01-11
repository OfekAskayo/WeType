package com.school.wetype;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {

    private FirebaseListAdapter<ChatMessage> adapter;
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );
    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION, false);
                        if (fineLocationGranted != null && fineLocationGranted) {
                            // Precise location access granted.
                        } else if (coarseLocationGranted != null && coarseLocationGranted) {
                            // Only approximate location access granted.
                        } else {
                            // No location access granted.
                        }
                    }
            );
    private FusedLocationProviderClient fusedLocationClient;

    Intent batteryStatus;
    AirplaneModeBroadcastReceiver airplaneModeBroadcastReceiver;

    private CameraManager cameraManager;
    private String getCameraID;
    private boolean torchModeEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        torchModeEnabled = false;

        IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, batteryChanged);

        this.airplaneModeBroadcastReceiver = new AirplaneModeBroadcastReceiver(this);
        IntentFilter airplaneModeChanged = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        airplaneModeChanged.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        this.registerReceiver(airplaneModeBroadcastReceiver, airplaneModeChanged);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            /* startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .build(),
                    SIGN_IN_REQUEST_CODE
            );*/

            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build());

            // Create and launch sign-in intent
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setTheme(R.style.Theme_WeType)
                    .build();
            signInLauncher.launch(signInIntent);
        } else {
            Toast.makeText(this,
                    "Welcome " + FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getDisplayName(),
                    Toast.LENGTH_LONG)
                    .show();

            displayChatMessages();
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = findViewById(R.id.input);
                String messageText = input.getText().toString();

                MainActivity.this.sendChatMessage(messageText);

                input.setText("");
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // cameraManager to interact with camera devices
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        // Exception is handled, because to check whether
        // the camera resource is being used by another
        // service or not.

        try {
            // O means back camera unit,
            // 1 means front camera unit
            getCameraID = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(MainActivity.this,
                                        "You have been signed out.",
                                        Toast.LENGTH_LONG)
                                        .show();

                                // Close activity
                                finish();
                            }
                        });
                return true;
            case R.id.menu_flashlight:
                MainActivity.this.torchModeEnabled = !MainActivity.this.torchModeEnabled;

                try {
                    cameraManager.setTorchMode(getCameraID, MainActivity.this.torchModeEnabled);

                    if (MainActivity.this.torchModeEnabled) {
                        Toast.makeText(MainActivity.this, "Flashlight is turned ON", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Flashlight is turned OFF", Toast.LENGTH_SHORT).show();
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.menu_location:
                item.setEnabled(false);
                item.getIcon().setAlpha(72);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationPermissionRequest.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                    return true;
                }

                fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                    @NonNull
                    @Override
                    public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                        return null;
                    }

                    @Override
                    public boolean isCancellationRequested() {
                        return false;
                    }
                }).addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                            LocationChatMessage chatMessage = new LocationChatMessage(FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getDisplayName());

                            chatMessage.setLocationText(MainActivity.this, location);

                            MainActivity.this.sendChatMessage(chatMessage);
                        } else {
                            Toast.makeText(MainActivity.this, "Couldn't get current location, please try again later", Toast.LENGTH_LONG).show();
                        }

                        item.setEnabled(true);
                        item.getIcon().setAlpha(153);
                    }
                });
                return true;
            case R.id.menu_battery_level:
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                float batteryPct = level * 100 / (float)scale;

                String messageText = "My battery level is: " + batteryPct + "%";

                sendChatMessage(messageText);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            // FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Toast.makeText(this,
                    "Successfully signed in. Welcome!",
                    Toast.LENGTH_LONG)
                    .show();
            displayChatMessages();
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            Toast.makeText(this,
                    "We couldn't sign you in. Please try again later.",
                    Toast.LENGTH_LONG)
                    .show();

            // Close the app
            finish();
        }
    }

    private void displayChatMessages() {
        ListView listOfMessages = findViewById(R.id.list_of_messages);

        FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                .setLayout(R.layout.message)
                .setQuery(FirebaseDatabase.getInstance().getReference(), ChatMessage.class)
                .build();

        adapter = new FirebaseListAdapter<ChatMessage>(options) {
            @Override
            protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {
                // Get references to the views of message.xml
                TextView messageText = (TextView) v.findViewById(R.id.message_text);
                TextView messageUser = (TextView) v.findViewById(R.id.message_user);
                TextView messageTime = (TextView) v.findViewById(R.id.message_time);

                // Set their text
                messageText.setText(model.getMessageText());
                messageUser.setText(model.getMessageUser());

                // Format the date before showing it
                messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
                        model.getMessageTime()));
            }
        };

        listOfMessages.setAdapter(adapter);
    }

    public void sendChatMessage(String messageText) {
        // Write a message to the database
//                FirebaseDatabase database = FirebaseDatabase.getInstance();
//                database.getReference("message").setValue("Hello, World!");
//                database.getReference("message").push().setValue("Hello World! Push");

        // Read the input field and push a new instance
        // of ChatMessage to the Firebase database
        FirebaseDatabase.getInstance()
                .getReference()
                .push()
                .setValue(new ChatMessage(messageText,
                        FirebaseAuth.getInstance()
                                .getCurrentUser()
                                .getDisplayName())
                );
    }

    public void sendChatMessage(ChatMessage chatMessage) {
        // Write a message to the database
//                FirebaseDatabase database = FirebaseDatabase.getInstance();
//                database.getReference("message").setValue("Hello, World!");
//                database.getReference("message").push().setValue("Hello World! Push");

        // Read the input field and push a new instance
        // of ChatMessage to the Firebase database
        FirebaseDatabase.getInstance()
                .getReference()
                .push()
                .setValue(chatMessage);
    }
}