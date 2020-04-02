package com.ibm.avatarchatbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArNavigation extends AppCompatActivity {

    // Layout Items Declaration

    private CustomARFragment arFragment;
    private EditText eTAnchorID;
    private Button resolve, routeToMyDesk, info;

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED
    }

    List<String> anchorPoints = new ArrayList<>();

    // Flags Declaration

    private boolean isPlaced = false;
    private boolean devmode = false;

    // Ar Fragments Declaration

    private Anchor anchor;
    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    DatabaseHelper myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_navigation);

        // Map the XML file to JAVA

        eTAnchorID = findViewById(R.id.eTAnchorID);
        resolve = findViewById(R.id.resolve);
        routeToMyDesk = findViewById(R.id.routeToMyDesk);
        info = findViewById(R.id.infopage);

        info.setOnClickListener(view -> {
            Intent intent = new Intent(this, Info.class);
            startActivity(intent);
        });

        myDb = new DatabaseHelper(this);


        AlertDialog.Builder a_builder = new AlertDialog.Builder(this);
        a_builder.setMessage(
                "1. Tap on any anchor point to place the arrow.\n\n" +
                "2. Wait until you see a toast message saying \"Anchor Hosted Successfully!\"\n once done relaunch the application and place the next arrow.\n\n" +
                "3. Repeat this process until you have successfully trained your desired path.\n")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.cancel());
        AlertDialog alert = a_builder.create();
        alert.setTitle("Training Path Tutorial");
        alert.show();


        // Toggle Developer Mode Settings

        if(devmode){
            eTAnchorID.setVisibility(View.VISIBLE);
            resolve.setVisibility(View.VISIBLE);

        } else {
            eTAnchorID.setVisibility(View.GONE);
            resolve.setVisibility(View.GONE);

        }

        // Set Preferences

        prefs = getSharedPreferences("anchorId", MODE_PRIVATE);
        editor = prefs.edit();

        // AR Core Mapping

        arFragment = (CustomARFragment) getSupportFragmentManager().findFragmentById(R.id.fragment2);

        // AR Core Anchor points Tap Listener
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {

            if(!isPlaced) {
                anchor = Objects.requireNonNull(arFragment.getArSceneView()
                        .getSession())
                        .hostCloudAnchor(hitResult.getTrackable()
                                .createAnchor(
                                        hitResult.getHitPose()
                                                .compose(
                                                        Pose.makeTranslation(0, 0.5f, 0)
                                                )
                                )
                        );

                appAnchorState = AppAnchorState.HOSTING;
                showToast("Hosting...");
                createModel(anchor);
                isPlaced = true;
            }
        });

        // AR Core Anchor points uploaded Event Listener

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

            if(appAnchorState != AppAnchorState.HOSTING)
                return;
            Anchor.CloudAnchorState cloudAnchorState = anchor.getCloudAnchorState();

            if(cloudAnchorState.isError())
                showToast(cloudAnchorState.toString());

            else if (cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
                appAnchorState = AppAnchorState.HOSTED;

                String anchorId = anchor.getCloudAnchorId();
                editor.putString("anchorId", anchorId);
                editor.apply();

                showToast("Anchor Hosted Successfully! Anchor Id: "+ anchorId);
                Log.d("AnchorID ->", "Anchor Hosted Successfully! Anchor Id: "+ anchorId);
                boolean isInserted = myDb.insertData(anchorId);

                if (isInserted)
                    showToast("Anchor inserted into Database");
                else
                    showToast("Anchor not inserted please try again");
                eTAnchorID.setText(anchorId);
            }
        });

        // Resolve Button Action Listener

        resolve.setOnClickListener(view -> {
//            String anchorId = prefs.getString("anchorId", "null");

            String anchorId = eTAnchorID.getText().toString();
            if(anchorId.equals("null")) {
                showToast("No Anchor ID Found");
                return;
            }

            Anchor resolvedAnchor = Objects.requireNonNull(arFragment.getArSceneView().getSession()).resolveCloudAnchor(anchorId);
            createModel(resolvedAnchor);
        });

        // Route Button Action Listener

        routeToMyDesk.setOnClickListener(view -> {

            Cursor res = myDb.getAllData();
            if (res.getCount() == 0)
                return;

            StringBuilder buffer = new StringBuilder();
            while(res.moveToNext()){
                anchorPoints.add(res.getString(1));
                showToast(buffer.toString());
            }

            String anchorId;

//            anchorPoints.add("ua-7b3740e360f63ea5c919a5b3deed2d02");
//            anchorPoints.add("ua-6b128d0377b2f1066f7a76a428462f50");
//            anchorPoints.add("ua-eea0d87b4686d29e89e5e89db75da09b");

            for (int i=0; i< anchorPoints.size(); i++){
                anchorId = anchorPoints.get(i);
                if(anchorId.equals("null")) {
                    showToast("No Anchor ID Found");
                    return;
                }
                Anchor resolvedAnchor = Objects.requireNonNull(arFragment.getArSceneView().getSession()).resolveCloudAnchor(anchorId);
                createModel(resolvedAnchor);
            }
        });
    }

    // Toast Helper Method

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    // Create AR Core Model

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createModel(Anchor anchor) {

            ModelRenderable
                    .builder()
                    .setSource(this, Uri.parse("model.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> placeModel(anchor, modelRenderable));
    }

    // Place Sceneform Model with all the vector transformations

    private void placeModel(Anchor anchor, ModelRenderable modelRenderable) {

        AnchorNode anchorNode = new AnchorNode(anchor);
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 205));
        node.setParent(anchorNode);
        node.setRenderable(modelRenderable);
    }
}
