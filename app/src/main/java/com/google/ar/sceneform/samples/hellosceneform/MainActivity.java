/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.ux.ArFragment;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */

/*
 * We are just using this class to initialise our game and the AR stuff. All of the actual game code is in Game.java
 *
 * NOTICE:
 * This framework for this project was adapted from the hellosceneform sample project from the google ARCore sample library
 * It has been modified as allowed by the Apache v.2.0 license
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;


    private ArFragment arFragment;
    private boolean anchorSet = false;

    private Game theGame;             // The actual game object. we only want to create one of these on the first tap


    // NOTICE: This function came as part of the Google ARCore sample library.
    // It has been modified here in accordance with the Apache v.2.0 license
    /**
     * onCreate: activates the buttons onClick method, and creates a ArFragment that waits until
     * the user clicks a Sceneform plane in AR. When the plane is clicked the game starts
     */
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        theGame = new Game(this);

        View leftButtonListener = findViewById(R.id.button);
        leftButtonListener.setOnClickListener(this);

        View rightButtonListener = findViewById(R.id.button2);
        rightButtonListener.setOnClickListener(this);

        View forwardButtonListener = findViewById(R.id.button3);
        forwardButtonListener.setOnClickListener(this);

        View backwardButtonListener = findViewById(R.id.button4);
        backwardButtonListener.setOnClickListener(this);

        arFragment.setOnTapArPlaneListener(
            (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                if (!theGame.isStarted()) {     // Make sure this is the first game we are creating
                    if (!anchorSet) {
                        Anchor anchor = hitResult.createAnchor();
                        theGame.createGame(anchor, arFragment);
                        anchorSet = true;
                    } else {
                        theGame.restart(arFragment);
                    }

                }else{
                    theGame.gameTick();
                }

            });
    }


    // NOTICE: This function came as part of the Google ARCore sample library. It is used here in accordance with the Apache v.2.0 license
    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device. Code used from Sceneform sample:
     * https://github.com/google-ar/sceneform-android-sdk/blob/master/samples/hellosceneform/app/src/main/java/com/google/ar/sceneform/samples/hellosceneform/HelloSceneformActivity.java
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
            ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show();
            activity.finish();
            return false;
        }
        return true;
    }

    /**
     * onClick: moves the blocks around based on the button clicked
     * @param v the button that generated the onClick method
     */
    @Override
    public void onClick(View v) {
        if (theGame.isStarted()) {
            if (v == findViewById(R.id.button)) {
                theGame.userPressedLeft();
            } else if (v == findViewById(R.id.button2)) {
                theGame.userPressedRight();
            } else if (v == findViewById(R.id.button3)) {
                theGame.userPressedForward();
            } else if (v == findViewById(R.id.button4)) {
                theGame.userPressedBackward();
            }
        }
    }
}
