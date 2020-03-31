package com.ibm.avatarchatbot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.assistant.v1.Assistant;
import com.ibm.watson.developer_cloud.assistant.v1.model.InputData;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;
import com.worklight.wlclient.api.WLAccessTokenListener;
import com.worklight.wlclient.api.WLAuthorizationManager;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;
import com.worklight.wlclient.auth.AccessToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter mAdapter;
    private ArrayList messageArrayList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnRecord;
    private Button assistant;
    private int i = 0;
    private boolean isModelPlaced;

    DatabaseHelper myDb;

    com.ibm.watson.developer_cloud.assistant.v1.model.Context context = null;
    StreamPlayer streamPlayer;
    private boolean initialRequest;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private boolean listening = false;

    private MicrophoneInputStream capture;
    private SpeakerLabelsDiarization.RecoTokens recoTokens;
    private MicrophoneHelper microphoneHelper;

    // Watson Text-to-Speech Service on IBM Cloud
    final SpeechToText speechService =  new SpeechToText();
    final TextToSpeech textService = new TextToSpeech();
    final Assistant assistantservice = new Assistant("2018-02-16");

    // AR Core Animation
    private ArFragment arFragment;
    private ModelAnimator modelAnimator;
    Button RemoveAvatar;
    private ModelRenderable modelRenderable;

    // Location Details
    LocationManager locationManager;
    private String latitude;
    private String longitude;
    private String query;

    // Credentials & URL's
    private String SpeechToTextAPI="", SpeechToTextURL="";
    private String TextToSpeechAPI="", TextToSpeechURL="";
    private String AssistantAPI="", AssistantWorkspaceID="", AssistantURL="";
    private String CloudFunctionsURL="";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tutorial at the start of application

        AlertDialog.Builder a_builder = new AlertDialog.Builder(this);
        a_builder.setMessage("1. Rotate the device in '8' shaped position to calibrate. \n\n" +
                "2. Tap on any anchor point to Summon the Watson Avatar.\n\n" +
                "3. Click on the microphone button and start talking...\n once done talking click the microphone button again and wait for the assistant to reply.\n\n" +
                "4. Click on the cancel button to hide the Watson Avatar\n")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.cancel());
        AlertDialog alert = a_builder.create();
        alert.setTitle("Tutorial");
//        alert.show();

            // All Permissions are Granted Proceed

            // Get Location co-ordinates
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            GetLocationCoordinates getLocationCoordinates = new GetLocationCoordinates();
            getLocationCoordinates.onLocationChanged(location);

            query = getLocationCoordinates.queryparams;
            Log.d("LOCATION DETAILS2 ->", query);


        RemoveAvatar = findViewById(R.id.removeAvatar);
        RemoveAvatar.setVisibility(View.GONE);
        inputMessage = findViewById(R.id.message);
        inputMessage.setVisibility(View.GONE);
        btnSend = findViewById(R.id.btn_send);
        btnSend.setVisibility(View.GONE);
        btnRecord= findViewById(R.id.btn_record);
        btnRecord.setVisibility(View.GONE);
        assistant = findViewById(R.id.assistant);
        assistant.setVisibility(View.GONE);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setVisibility(View.GONE);


        // Array for Storing Messages

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);
        microphoneHelper = new MicrophoneHelper(this);

        // The Text Panel

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        this.inputMessage.setText("");
        this.initialRequest = true;

        // ARCore Initialization

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> createModel(hitResult.createAnchor()));
//        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);

        // Get all the Credentials From Mobile Foundation

        mobileFoundationProductsFetch();


        Toast.makeText(getApplicationContext(), "Tap Anywhere on the Anchor to Summon the Avatar Assistant",Toast.LENGTH_LONG).show();

        // Send button ActionListener

        btnSend.setOnClickListener(v -> {
            Permissions obj = new Permissions();
            if(obj.checkInternetConnection()) {
                sendMessage();
            }
        });

        // Record Button onClick Listener

        assistant.setOnClickListener(v -> speech());

        // Record Button Tap and hold ActionListener

//        assistant.setOnTouchListener((v, event) -> {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    recordMessage();
//
//                } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                    stopRecording();
//                }
//                return false;
//            });
    }

    private void InitializeWatsonServices(){

        // Watson Speech to Text Initialization

        speechService.setUsernameAndPassword("apikey", SpeechToTextAPI);
        speechService.setEndPoint(SpeechToTextURL);

        // Watson Text to Speech Initialization

        textService.setUsernameAndPassword("apikey", TextToSpeechAPI);
        textService.setEndPoint(TextToSpeechURL);

        // Watson Assistant Initialization

        assistantservice.setUsernameAndPassword("apikey", AssistantAPI);
        assistantservice.setEndPoint(AssistantURL);

        // Cloud Function Initialization

        cloudfunctionApicall(query);


    }

    private void mobileFoundationProductsFetch() {
        try {

            URI adapterPath = new URI("/adapters/avatarchatbot/resource/mfpapi");

            WLResourceRequest request = new WLResourceRequest(adapterPath, WLResourceRequest.GET);

            request.send(new WLResponseListener() {
                @Override
                public void onSuccess(WLResponse wlResponse) {
                    String responseText = wlResponse.getResponseText();
                    Log.d("MobileFirst Response -> ", responseText);
                    getJSON(responseText);
                }

                @Override
                public void onFailure(WLFailResponse wlFailResponse) {
                    String errorMsg = wlFailResponse.getErrorMsg();
                    Log.d("InvokeFail", errorMsg);
                }
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void getJSON(String responseText) {
        try {
            JSONObject productObject = new JSONObject(responseText);

                SpeechToTextAPI = productObject.getString("s2tapi");
                SpeechToTextURL = productObject.getString("s2turl");
                TextToSpeechAPI = productObject.getString("t2sapi");
                TextToSpeechURL = productObject.getString("t2surl");
                AssistantAPI = productObject.getString("assistantapi");
                AssistantURL = productObject.getString("assistanturl");
                AssistantWorkspaceID = productObject.getString("assistantworkspaceid");
                CloudFunctionsURL = productObject.getString("cloudfuncurl");

                Log.d("JSON OBJECT -> ", String.valueOf(productObject));

                InitializeWatsonServices();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void cloudfunctionApicall(String query) {
        // Instantiate the RequestQueue.
        String testquery = "?lat=13.046500&lon=77.619926";
        String query2 = "?lat=13.047715&lon=77.622689";
        RequestQueue queue = Volley.newRequestQueue(this);
        String url =CloudFunctionsURL+query;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> Toast.makeText(getApplicationContext(), response,Toast.LENGTH_LONG).show(),
                error -> Toast.makeText(getApplicationContext(), "something went wrong storing the co-ordinates",Toast.LENGTH_LONG).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        cloudProcessing();
    }

    private void cloudProcessing() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url =CloudFunctionsURL+"?process=1";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> Toast.makeText(getApplicationContext(), response,Toast.LENGTH_LONG).show(),
                error -> Toast.makeText(getApplicationContext(), "something went wrong processing...",Toast.LENGTH_SHORT).show());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void onUpdate(FrameTime frameTime){

        if(isModelPlaced)
            return;

        Frame frame = arFragment.getArSceneView().getArFrame();

        Collection<Plane> planes = null;
        if (frame != null) {
            planes = frame.getUpdatedTrackables(Plane.class);
        }

        if (planes != null) {
            for (Plane plane : planes) {

                if(plane.getTrackingState() == TrackingState.TRACKING) {
                    Anchor anchor = plane.createAnchor(plane.getCenterPose());

                    // Call the function
//                    createModel(anchor);

                    break;
                }
            }
        }
    }

    private void createModel(Anchor anchor) {

        //Build the Avatar Model
        isModelPlaced = true;

        ModelRenderable
                .builder()
                .setSource(this, Uri.parse("female.sfb"))
                .build()
                .thenAccept(modelRenderable -> {
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                    RemoveAvatar.setVisibility(View.VISIBLE);

                    // Set the position of the Avatar

                    TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                    node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 0));
//                    node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 54));
                    node.setParent(anchorNode);
                    node.setRenderable(modelRenderable);

                    this.modelRenderable = modelRenderable;

                    sendMessage();

                    // Set Static Center Position of the Avatar

//                    arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
//                        Camera camera = arFragment.getArSceneView().getScene().getCamera();
//                        Ray ray = camera.screenPointToRay(1080/2f, 1920/2f);
//                        Vector3 newPosition = ray.getPoint(1f);
//                        node.setLocalPosition(newPosition);
//                    });

                    // Set Static Position of the Avatar

//                    SkeletonNode skeletonNode = new SkeletonNode();
//                    skeletonNode.setParent(anchorNode);
//                    skeletonNode.setRenderable(modelRenderable);

                    assistant.setVisibility(View.VISIBLE);

                    // Remove Avatar from the Sceneform

                    RemoveAvatar.setOnClickListener(v -> {
                        RemoveAvatar.setVisibility(View.GONE);
                        arFragment.getArSceneView().getScene().removeChild(anchorNode);
                        Intent intent = new Intent(this, ArNavigation.class);
                        startActivity(intent);
                    });
                });

    }

    private void animateModel(ModelRenderable modelRenderable) {

        if(modelAnimator != null && modelAnimator.isRunning())
            modelAnimator.end();

        int animationCount = modelRenderable.getAnimationDataCount();

        if(i== animationCount)
            i=0;

        AnimationData animationData = modelRenderable.getAnimationData(i);
        modelAnimator = new ModelAnimator(animationData, modelRenderable);
        modelAnimator.start();
        i++;

    }

    private void speech(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                "com.domain.app");

        SpeechRecognizer recognizer = SpeechRecognizer
                .createSpeechRecognizer(this.getApplicationContext());
        RecognitionListener listener = new RecognitionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> voiceResults = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (voiceResults == null) {
                    System.out.println("No voice results");
                } else {
                    System.out.println("Printing matches: ");
                    for (String match : voiceResults) {
                        showMicText(match);
                    }
                    sendMessage();
                }
            }


            @Override
            public void onReadyForSpeech(Bundle params) {
                System.out.println("Ready for speech");
            }

            /**
             *  ERROR_NETWORK_TIMEOUT = 1;
             *  ERROR_NETWORK = 2;
             *  ERROR_AUDIO = 3;
             *  ERROR_SERVER = 4;
             *  ERROR_CLIENT = 5;
             *  ERROR_SPEECH_TIMEOUT = 6;
             *  ERROR_NO_MATCH = 7;
             *  ERROR_RECOGNIZER_BUSY = 8;
             *  ERROR_INSUFFICIENT_PERMISSIONS = 9;
             *
             * @param error code is defined in SpeechRecognizer
             */
            @Override
            public void onError(int error) {
                System.err.println("Error listening for speech: " + error);
            }

            @Override
            public void onBeginningOfSpeech() {
                System.out.println("Speech starting");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEndOfSpeech() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // TODO Auto-generated method stub

            }
        };
        recognizer.setRecognitionListener(listener);
        recognizer.startListening(intent);
    }

    // Android Speach to Text

    public void getSpeechInput(View view) {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    showMicText(result.get(0));
                    sendMessage();
                }
                break;
        }
    }

    private void sendMessage() {

        final String inputmessage = this.inputMessage.getText().toString().trim();
        if(!this.initialRequest) {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
        }
        else
        {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("100");
            this.initialRequest = false;
            // Toast.makeText(getApplicationContext(),"Tap and hold on the Mic to Start, Leave the Mic to Stop",Toast.LENGTH_LONG).show();

        }

        this.inputMessage.setText("");
        mAdapter.notifyDataSetChanged();

        Thread thread = new Thread(() -> {
            try {
                InputData input = new InputData.Builder(inputmessage).build();

                //WORKSPACES are now SKILLS

                MessageOptions options = new MessageOptions.Builder().workspaceId(AssistantWorkspaceID).input(input).context(context).build();
                MessageResponse response = assistantservice.message(options).execute();
                Log.i(TAG, "run: "+response);

                String outputText = "";
                int length=response.getOutput().getText().size();
                Log.i(TAG, "run: "+length);

                if(length>1) {
                    for (int i = 0; i < length; i++) {
                        outputText += '\n' + response.getOutput().getText().get(i).trim();
                    }
                }
                else
                    outputText = response.getOutput().getText().get(0);
                Log.i(TAG, "run: "+outputText);

                //Passing Context of last conversation

                if(response.getContext() !=null)
                {
                    //context.clear();
                    context = response.getContext();

                }
                final Message outMessage=new Message();
                if(response!=null)
                {
                    if(response.getOutput()!=null && response.getOutput().containsKey("text"))
                    {
                        ArrayList responseList = (ArrayList) response.getOutput().get("text");
                        if(null !=responseList && responseList.size()>0){
                            outMessage.setMessage(outputText);
                            outMessage.setId("2");
                        }

                        // Play the Audio

                        Thread thread1 = new Thread(() -> {
                            Message audioMessage;
                            try {

                                audioMessage = outMessage;
                                Log.d("TEST-> ", "The msg is:'"+outMessage.getMessage()+"'");
                                streamPlayer = new StreamPlayer();
                                if(audioMessage != null && !audioMessage.getMessage().isEmpty()) {
                                    SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                                            .text(audioMessage.getMessage())
                                            .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                                            .accept(SynthesizeOptions.Accept.AUDIO_WAV)
                                            .build();
                                    streamPlayer.playStream(textService.synthesize(synthesizeOptions).execute());
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        });
                        thread1.start();

                        jumpIntent(outMessage.getMessage());
                        messageArrayList.add(outMessage);
                        animateModel(modelRenderable);
                    }

                    runOnUiThread(() -> {
                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1) {
                            recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    //Record a message via Watson Speech to Text

    private void recordMessage() {
        capture = microphoneHelper.getInputStream(true);

        new Thread(() -> {
            try {
                speechService.recognizeUsingWebSocket(getRecognizeOptions(capture), new MicrophoneRecognizeDelegate());
            } catch (Exception e) {
                showError(e);
            }
        }).start();

        Toast.makeText(MainActivity.this,"Listening....leave to Stop", Toast.LENGTH_LONG).show();
    }

    private void stopRecording() {
        try {
            microphoneHelper.closeInputStream();
            listening = false;
            Toast.makeText(MainActivity.this, "You said: "+this.inputMessage.getText().toString().trim(), Toast.LENGTH_LONG).show();
            sendMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions(InputStream audio) {
        return new RecognizeOptions.Builder()
                .audio(audio)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                //TODO: Uncomment this to enable Speaker Diarization
                //.speakerLabels(true)
                .build();
    }

    private void jumpIntent(String message) {

        Log.d("Compare->", message+" = Please follow the path.");

        if ("Please follow the path.".equals(message)){
            Intent intent = new Intent(this, ArNavigation.class);
            startActivity(intent);
        }
    }

private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {

    @Override
    public void onTranscription(SpeechRecognitionResults speechResults) {
        System.out.println(speechResults);
        //TODO: Uncomment this to enable Speaker Diarization
            /*SpeakerLabelsDiarization.RecoTokens recoTokens = new SpeakerLabelsDiarization.RecoTokens();
            if(speechResults.getSpeakerLabels() !=null)
            {
                recoTokens.add(speechResults);
                Log.i("SPEECHRESULTS",speechResults.getSpeakerLabels().get(0).toString());


            }*/
        if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
            String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
            showMicText(text);
        }
    }

    @Override public void onConnected() {

    }

    @Override public void onError(Exception e) {
        showError(e);
        enableMicButton();
    }

    @Override public void onDisconnected() {
        enableMicButton();
    }

    @Override
    public void onInactivityTimeout(RuntimeException runtimeException) {

    }

    @Override
    public void onListening() {

    }

    @Override
    public void onTranscriptionComplete() {

    }
}

    public void showError(final Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        });
    }

    public void showMicText(final String text) {
        runOnUiThread(() -> inputMessage.setText(text));
    }

    public void enableMicButton() {
        runOnUiThread(() -> btnRecord.setEnabled(true));
    }

}

