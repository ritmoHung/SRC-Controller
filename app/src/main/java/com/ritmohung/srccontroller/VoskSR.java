package com.ritmohung.srccontroller;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.InputStream;

import es.dmoral.toasty.Toasty;

public class VoskSR implements RecognitionListener {

    public Context context;
    public SpeechService speechService;
    public SpeechStreamService speechStreamService;
    private AssetManager assets;
    private TextView btCmd, voskCmd, rslt;
    private ToggleButton recog;
    private Button file, mic;
    private Model model;
    public BTTX BTTX;
    private int errors;
    public int confidence;
    public boolean SP_MODE = false;

    static private final String MODEL_SOURCE_PATH = "model-en-small";
    static private final String TARGET_SOURCE_PATH = "model";
    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;

    // This is the target speech...lengthy
    static private final String targetSpeech1 = "advice sometimes my mother gives me advice she tells me to save my money for a rainy day she says that I should eat my vegetables if I want to be strong when I grow up she says that you reap what you sow I didn't know what that one meant so I asked her she said that if you're good to people they will be good to you if you do bad things then bad things will come back to you my mother is always giving me advice she says that a penny saved is a penny earned i'm still thinking about that one some of these things are difficult to understand";
    static private final String targetSpeech2 = "my mother is very wise she says that she has learned from her mistakes she tells me that she would like me not to make mistakes but she says that everyone does make mistakes the important thing is that we will learn from our mistakes my mother says that nobody is perfect my mother tells my sister that time is precious my sister wastes time and my mother doesn't like that";
    static private final String targetSpeech3 = "my mother tells my to be true to myself she says that i should not follow the crowd i should listen to my own conscience and do what i think is right";
    static private final String targetSpeech4 = "she says that it doesn't matter if you fail at something the important thing is that you try if you've done your best and that is all that matters";
    static private final String targetSpeech5 = "i listen to my mother i think she gives very good advice my mother has a lot of common sense i hope i am as wise as she is when i have children of my own sometimes i wish that she would not give me so much advice i think that i know what i'm doing but in the end i always remember what she has said and i try to live by the standards that she has set for me";
    static private final String targetSpeech6 = "take the advice that your parents give you they only have your best interests at heart";
    static private String targetSpeech;

    // Key commands
    static private final String SP = "special";
    static private final String EX = "exit";
    static private final String FRONT = "front";
    static private final String BACK = "back";
    static private final String LEFT = "left";
    static private final String RIGHT = "right";
    static private final String STOP = "stop";
    static private final String V_SHORT = "epsilon";
    static private final String SHORT = "alpha";
    static private final String LONG = "delta";
    static private final String V_LONG = "karma";
    private String command = "";





    // Constructor
    public VoskSR(Context context, Button file, Button mic, ToggleButton recog, TextView btCommand, TextView voskCommand, TextView result) {
        this.context = context;
        this.file = file;
        this.mic = mic;
        this.recog = recog;
        btCmd = btCommand;
        voskCmd = voskCommand;
        rslt = result;

        targetSpeech = targetSpeech1 + " " + targetSpeech2 + " " + targetSpeech3 + " " + targetSpeech4 + " " + targetSpeech5 + " " + targetSpeech6;
    }

    public void init() {
        BTTX = new BTTX(context, btCmd);
        StorageService.unpack(context, MODEL_SOURCE_PATH, TARGET_SOURCE_PATH, (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }


    @Override
    public void onPartialResult(String hypothesis) {
        String lastHeard = parseHypo("partial", hypothesis);
        if(lastHeard.contains(EX))
            SP_MODE = false;

        if(SP_MODE) {
            if(lastHeard.equals("")) ;
            else if(isMatch(lastHeard))
                confidence++;
            else
                errors++;

            //
            if(isConfidence()) {
                BTTX.s = "mh";
                Toasty.info(context, "Is confidence, \"mh\" sent", Toast.LENGTH_SHORT, true).show();
            }
        }
    }

    @Override
    public void onResult(String hypothesis) {
        String lastHeard = parseHypo("text", hypothesis);
        findCommand(lastHeard);

        if(SP_MODE) {
            if(lastHeard.equals("")) ;
            else if(isMatch(lastHeard))
                confidence += 2;
            else
                errors++;

            //
            if(isConfidence()) {
                BTTX.s = "mh";
                Toasty.info(context, "Is confidence, \"mh\" sent", Toast.LENGTH_SHORT, true).show();
            }
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        parseHypo("final", hypothesis);
        setUiState(STATE_DONE);
        if (speechStreamService != null)
            speechStreamService = null;
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }

    public void setUiState(int state) {
        switch(state) {
            case STATE_START:
                voskCmd.setText(R.string.preparingText);
                voskCmd.setMovementMethod(new ScrollingMovementMethod());
                file.setEnabled(false);
                mic.setEnabled(false);
                recog.setEnabled((false));
                break;
            case STATE_READY:
                voskCmd.setText(R.string.readyText);
                mic.setText(R.string.micModeText);
                file.setEnabled(true);
                mic.setEnabled(true);
                recog.setEnabled((false));
                break;
            case STATE_DONE:
                file.setText(R.string.fileModeText);
                mic.setText(R.string.micModeText);
                file.setEnabled(true);
                mic.setEnabled(true);
                recog.setEnabled((false));
                break;
            case STATE_FILE:
                voskCmd.setText(R.string.startingFileText);
                file.setText(R.string.stopFileText);
                mic.setEnabled(false);
                file.setEnabled(true);
                recog.setEnabled((false));
                break;
            case STATE_MIC:
                voskCmd.setText(R.string.sayCommand);
                mic.setText(R.string.stopMicText);
                file.setEnabled(false);
                mic.setEnabled(true);
                recog.setEnabled((true));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        voskCmd.setText(message);
        mic.setText(R.string.micModeText);
        file.setEnabled(false);
        mic.setEnabled(false);
    }

    public void recognizeFile() {
        if(speechStreamService != null) {
            setUiState(STATE_DONE);
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            setUiState(STATE_FILE);
            try {
                Recognizer rec = new Recognizer(model, 16000.f, "[\"one zero zero zero one\", " +
                        "\"oh zero one two three four five six seven eight nine\", \"[unk]\"]");

                assets = context.getAssets();
                InputStream ais = assets.open(
                        "TARGET_MALE.mp3");
                if(ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 16000);
                speechStreamService.start(this);
            } catch(IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    public void recognizeMicrophone() {
        if(speechService != null) {
            setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        }
        else {
            setUiState(STATE_MIC);
            try {
                // Recognizer rec = new Recognizer(model, 16000.f, "[\"sockeye pink coho chum chinook atlantic salmon\","[unk]"]");
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch(IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    public void pause(boolean checked) {
        if(speechService != null)
            speechService.setPause(checked);
    }



    // User defined functions
    private String parseHypo(String name, String hypothesis) {
        String lastHeard = "";
        try {
            JSONObject json = new JSONObject(hypothesis);
            if(json.has(name)) {
                // Grab content from json format and split with whitespaces
                lastHeard = json.getString(name);
                String[] resultArray = lastHeard.split("\\s+");

                // Takes out the last two words recognized
                // Takes out the only word if length = 1
                if(resultArray.length > 1) {
                    lastHeard = resultArray[resultArray.length - 2] + " " + resultArray[resultArray.length - 1];
                }
                else
                    lastHeard = resultArray[0];
                voskCmd.setText(lastHeard);
            }
        } catch(JSONException ignored) {}

        return lastHeard;
    }

    private void findCommand(String lastHeard) {
        if(!SP_MODE && !lastHeard.equals("")) {
            String[] lastHeardArray = lastHeard.split("\\s+");
            command = "";

            lastHeard = lastHeardArray[0];
            if(FRONT.contains(lastHeard)) command = "f";
            else if(BACK.contains(lastHeard)) command = "b";
            else if(LEFT.contains(lastHeard)) command = "l";
            else if(RIGHT.contains(lastHeard)) command = "r";
            else if(STOP.contains(lastHeard)) command = "s";
            else if(SP.contains(lastHeard)) {
                command = "sp";
                SP_MODE = true;
            }
            else if(EX.contains(lastHeard)) {
                command = "ex";
                SP_MODE = false;
            }

            if(lastHeardArray.length > 1) {
                lastHeard = lastHeardArray[1];
                if(V_SHORT.contains(lastHeard)) command += "e";
                else if(SHORT.contains(lastHeard)) command += "a";
                else if(LONG.contains(lastHeard)) command += "d";
                else if(V_LONG.contains(lastHeard)) command += "k";
            }
            if(!command.equals("") && (command.equals("s") || command.length() > 1)) {
                BTTX.s = command;
                if(command.equals("sp"))
                    Toasty.info(context, "Command found: " + command + "\nEnter SP mode", Toast.LENGTH_SHORT).show();
                else if(command.equals("ex"))
                    Toasty.info(context, "Command found: " + command + "\nExit SP mode", Toast.LENGTH_SHORT).show();
                else
                    Toasty.normal(context, "Command found: " + command, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isMatch(String frags) {
        // Search for matches when in SP mode
        if(SP_MODE && !frags.equals("")) {
            if(targetSpeech.contains(frags)) {
                rslt.setText(R.string.matchText);
                return true;
            }
            else
                rslt.setText(R.string.noMatchText);
        }
        else rslt.setText("");
        return false;
    }

    // Returns true if VoskSR is confidence about "Heard == Target"
    public boolean isConfidence() {
        // TODO: 2022/5/27 New added, needs more review
        if(confidence >= 10 && errors < 5) {
            confidence = 0;
            errors = 0;
            return true;
        }
        else {
            if(confidence >= 10 || errors >= 5) {
                confidence = 0;
                errors = 0;
            }
        }
        return false;
    }
}