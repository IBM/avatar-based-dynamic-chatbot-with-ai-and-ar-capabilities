package com.ibm.avatarchatbot;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
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
    private Button resolve, routeToMyDesk;

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

        myDb = new DatabaseHelper(this);

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

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {

            if(!isPlaced) {
                anchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.getTrackable().createAnchor(
                        hitResult.getHitPose().compose(Pose.makeTranslation(0, 0, 0))));
                appAnchorState = AppAnchorState.HOSTING;
                showToast("Hosting...");
                createModel(anchor, 999);
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
            createModel(resolvedAnchor, 999);
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
                createModel(resolvedAnchor, i);
            }
        });
    }

    // Toast Helper Method

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    // Create AR Core Model

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createModel(Anchor anchor, int i) {

        if (i == 2)
            ModelRenderable
                    .builder()
                    .setSource(this, Uri.parse("1358 Stop Sign.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> placeModel(anchor, modelRenderable, i));
        else
            ModelRenderable
                    .builder()
                    .setSource(this, Uri.parse("arrow.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> placeModel(anchor, modelRenderable, i));
    }

    // Place Sceneform Model with all the vector transformations

    private void placeModel(Anchor anchor, ModelRenderable modelRenderable, int i) {

        AnchorNode anchorNode = new AnchorNode(anchor);
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        if (i == 0)
            node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 15));
        else if (i == 1)
            node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 0));
        else if (i == 4)
            node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 25));
        else if (i == 3)
            node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 25));
        else if (i == 2)
            node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), -75));
        node.setParent(anchorNode);
        node.setRenderable(modelRenderable);
    }
}
