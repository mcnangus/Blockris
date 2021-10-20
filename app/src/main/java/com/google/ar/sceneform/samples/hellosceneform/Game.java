package com.google.ar.sceneform.samples.hellosceneform;

/*
 * GAME
 * This class handles the functionality and rendering of the actual game.
 * At the first tap after opening the app, a single game is created.
 */

import android.content.Context;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.FrameTime;
import android.view.Gravity;

import java.util.Random;

public class Game{

    // Constants
    private static final int GAME_WIDTH = 4;
    private static final int GAME_HEIGHT = 6;
    private static final int RENDERABLE_HEIGHT = 8;    // this height is the height of the frame plus 2 blocks. this lets us drop the next falling block from above the frame
    private static final int GAME_DEPTH = 4;

    // Sceneform bits and bobs
    private AnchorNode gameAnchor;
    private ModelRenderable blockRenderable;
    private ModelRenderable wireFrameRenderable;
    private Context context;

    // Game logic variables and stuff
    private boolean isStarted = false;
    private TransformableNode[][][] blockNodeArray = new TransformableNode[GAME_WIDTH][RENDERABLE_HEIGHT][GAME_DEPTH];  // We fill up this array with cubes and set them to visible/invisible rather than moving them around
    private boolean[][][] blockArray = new boolean[GAME_WIDTH][RENDERABLE_HEIGHT][GAME_DEPTH];     // This array of booleans is the one we do our logic on. We pass it to setVisibleCubes to render it. 
    private boolean[][][] fallingBlockArray = new boolean[GAME_WIDTH][RENDERABLE_HEIGHT][GAME_DEPTH];   // This array works like the one above but only holds the currently falling block. We can compare it to the above array to check collisions
    private Random rand;
    private int score;
    private float deltaTimeCount; // Used to track how many seconds have passed since the last tick

    /**
     * Game: Instantiates a game and loads 3D assets
     * @param theContext context from main activity
     */
    public Game(Context theContext) {
        context = theContext;
        rand = new Random();
        deltaTimeCount = 0.0f;

        // Load our wireframe model  - This code was adapted from Google's ARCore sample library in accordance with the Apache v.2.0 license
        ModelRenderable.builder()
            .setSource(context, R.raw.wireframe)
            .build()
            .thenAccept(renderable -> wireFrameRenderable = renderable)
            .exceptionally(
                throwable -> {
                    Toast toast =
                        Toast.makeText(context, "Unable to load frame renderable", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                });

        // Load our block model  - This code was adapted from Google's ARCore sample library in accordance with the Apache v.2.0 license
        ModelRenderable.builder()
            .setSource(context, R.raw.block)
            .build()
            .thenAccept(renderable -> blockRenderable = renderable)
            .exceptionally(
                throwable -> {
                    Toast toast =
                        Toast.makeText(context, "Unable to load block renderable", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                });

    }

    /**
     * createGame: Display the wireframe mesh at the area tapped and begin dropping the blocks.
     * @param anchor the point tapped. Where the game will occur in the world
     * @param arFragment AR fragment from main activity
     */
    public void createGame(Anchor anchor, ArFragment arFragment){

        Toast.makeText(context, "New Game", Toast.LENGTH_SHORT).show();

        // Frame listener to control the blocks falling
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onFrameUpdate);

        // Create the test cube and add to the ancor
        gameAnchor = new AnchorNode(anchor);
        gameAnchor.setParent(arFragment.getArSceneView().getScene());
        TransformableNode frameNode = new TransformableNode(arFragment.getTransformationSystem());
        frameNode.setParent(gameAnchor);
        frameNode.setRenderable(wireFrameRenderable);
        frameNode.getScaleController().setSensitivity(0);   // prevent transformations of the frame
        frameNode.getRotationController().setRotationRateDegrees(0);


        // Place a block at every point in the array. The renderables are kept null, because we change these later
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    blockNodeArray[x][y][z] = new TransformableNode(arFragment.getTransformationSystem());
                    blockNodeArray[x][y][z].setParent(gameAnchor);
                    Vector3 localPos = blockNodeArray[x][y][z].getLocalPosition();

                    // Move each block so that it's in it's spot
                    localPos.x -= 0.375;
                    localPos.x += x*0.25;
                    localPos.z -= 0.375;
                    localPos.z += z*0.25;
                    localPos.y += y*0.25;
                    blockNodeArray[x][y][z].setLocalPosition(localPos);

                    // Make each block invisible (and then we can make them visible when we need to)
                    blockNodeArray[x][y][z].setRenderable(null);
                    /*if (rand.nextBoolean()) {
                        blockNodeArray[x][y][z].setRenderable(null);    // We can make blocks invisible by setting their renderable to null.
                    }else{
                        blockNodeArray[x][y][z].setRenderable(blockRenderable);
                    }*/
                }
            }
        }

        isStarted = true;   // start the game
        deltaTimeCount = 0.0f;
        score = 0;

        getNextBlock(); // get the first block

    }

    /**
     * gameTick: Is called each tick of the game. Not every frame, but each time we want the blocks to move one block down.
     */
    public void gameTick(){

        // First let's see what will happen if we move the falling blocks down one, check to see if there are any collisions
        // If there are no collisions, we are good to go! Move the falling blocks down one.
        // If there is a collision, don't drop the falling blocks but add them to the landed blocks array and go from there

        if (isCollided(blockArray, dropFallingBlocks(fallingBlockArray)) || blocksAtBottom(fallingBlockArray)){
            // There is a collision if we let the blocks fall one more block! so let's add them to the landed blocks array
            blockArray = combineArrays(blockArray, fallingBlockArray);
            // Check if there are any full levels and tetris them
            blockArray = tetrisRows(blockArray);
            // Check if the player has stacked too high and lost like a loser
            if (checkForLoss(blockArray)){
                blockArray = new boolean[GAME_WIDTH][RENDERABLE_HEIGHT][GAME_DEPTH];
                fallingBlockArray = new boolean[GAME_WIDTH][RENDERABLE_HEIGHT][GAME_DEPTH];
                setVisibleBlocks(blockArray);
                isStarted = false;          // player has lost!
                Toast.makeText(context, "Game over, final score: "+score, Toast.LENGTH_LONG).show();
            }
            // Get the next block if we haven't lost
            if (isStarted){
                getNextBlock();
            }
        }else{
            fallingBlockArray = dropFallingBlocks(fallingBlockArray);   // Safe from collisions, so move our blocks down, render, and wait for next tick.
        }

        // Render the new blocks
        setVisibleBlocks(combineArrays(blockArray, fallingBlockArray));

        // Now we can wait for the next tick.
    }

    /**
     * getNextBlock: Places the next random block into the falling block array
     */
    public void getNextBlock(){
        // clear the array
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    fallingBlockArray[x][y][z] = false;
                }
            }
        }
        int nextBlockSelection = rand.nextInt(11);
        int nextBlockX = rand.nextInt(4);
        int nextBlockZ = rand.nextInt(4);

        // Program in the next block
        if (nextBlockSelection == 0){
            fallingBlockArray[nextBlockX][6][nextBlockZ] = true;  // Single 1x1 block
        }

        if (nextBlockSelection == 1 || nextBlockSelection == 2){    // These blocks can come up twice as likely cos they're the nicest
            int nextBlockXLimited = rand.nextInt(3);
            fallingBlockArray[nextBlockXLimited][6][nextBlockZ] = true;  // 2x1 horizontal on x
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZ] = true;
        }

        if (nextBlockSelection == 3 || nextBlockSelection == 4){
            int nextBlockZLimited = rand.nextInt(3);
            fallingBlockArray[nextBlockX][6][nextBlockZLimited] = true;  // 2x1 horizontal on y
            fallingBlockArray[nextBlockX][6][nextBlockZLimited+1] = true;
        }

        if (nextBlockSelection == 5 || nextBlockSelection == 6){
            fallingBlockArray[nextBlockX][6][nextBlockZ] = true;  // 2x1 vertical
            fallingBlockArray[nextBlockX][7][nextBlockZ] = true;
        }

        if (nextBlockSelection == 7){
            int nextBlockZLimited = rand.nextInt(3);
            int nextBlockXLimited = rand.nextInt(3);
            fallingBlockArray[nextBlockXLimited][6][nextBlockZLimited] = true;    // X X  block
            fallingBlockArray[nextBlockXLimited][6][nextBlockZLimited+1] = true;  // X
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZLimited] = true;
        }

        if (nextBlockSelection == 8){
            int nextBlockZLimited = rand.nextInt(3);
            int nextBlockXLimited = rand.nextInt(3);
            fallingBlockArray[nextBlockXLimited][6][nextBlockZLimited] = true;    // X X  block
            fallingBlockArray[nextBlockXLimited][6][nextBlockZLimited+1] = true;  // X
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZLimited+1] = true;
        }

        if (nextBlockSelection == 9){
            int nextBlockZLimited = rand.nextInt(3);
            int nextBlockXLimited = rand.nextInt(3);
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZLimited] = true;    // X X  block
            fallingBlockArray[nextBlockXLimited][6][nextBlockZLimited+1] = true;    // X
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZLimited+1] = true;
        }

        if (nextBlockSelection == 10){
            int nextBlockZLimited = rand.nextInt(3);
            int nextBlockXLimited = rand.nextInt(3);
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZLimited] = true;    // X X  block
            fallingBlockArray[nextBlockXLimited][6][nextBlockZLimited] = true;      // X
            fallingBlockArray[nextBlockXLimited+1][6][nextBlockZLimited+1] = true;
        }
    }

    /**
     * isCollided: lets us know if there's any colliding blocks in the two arrays passed
     * @param c first array
     * @param d second array
     * @return true if there is a point in the array where both arrays are true, otherwise false
     */
    public static boolean isCollided(boolean[][][] c, boolean[][][] d){
        boolean[][][] a = copyArray(c);
        boolean[][][] b = copyArray(d);
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    if (a[x][y][z] && b[x][y][z]){  // Collision!
                        return true;
                    }
                }
            }
        }
        return false;   // no coliisions
    }

    /**
     * blocksatbottom: returns true if there are blocks on the floor of the play area. used to tell if the falling blocks have hit the floor
     * @param a
     * @return
     */
    public static boolean blocksAtBottom(boolean[][][] a){
        for (int x=0;x<GAME_WIDTH;x++){
            for (int z=0;z<GAME_DEPTH;z++){
                if (a[x][0][z]){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * dropFallingBlocks: moves all the true blocks in the falling blocks array down one.
     * @param b the falling blocks array
     * @return the same array where every block is shifted down one
     */
    public boolean[][][] dropFallingBlocks(boolean[][][] b){
        boolean[][][] a = copyArray(b);
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT-1;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    a[x][y][z] = a[x][y+1][z];  // this has the effect of moving all the blocks down one (i hope)
                }
            }
        }
        for (int x=0;x<GAME_WIDTH;x++){
            for (int z=0;z<GAME_DEPTH;z++){
                a[x][7][z] = false;         // clear the top layer manually cos it gets missed by that fancy algorithm
            }
        }
        return a;
    }


    /**
     * combineArrays: returns an array of the two passed arrays where a true in either results in a true in the result.
     * @param c first array
     * @param d second array
     * @return both arrays after passing through an OR function
     */
    public boolean[][][] combineArrays(boolean[][][] c, boolean[][][] d){
        boolean[][][] a = copyArray(c);
        boolean[][][] b = copyArray(d);
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    a[x][y][z] = a[x][y][z] || b[x][y][z];  // I'm proud of this
                }
            }
        }
        return a;
    }


    /**
     * tetrisRows: Checks every level of the array. If there is a row that's full, remove it and move all the blocks above it down.
     * @param a the input arrray
     * @return the array with operations applied
     */
    public boolean[][][] tetrisRows(boolean[][][] a){
        boolean hasTetrised = false;
        for (int y=0;y<GAME_HEIGHT;y++){
            boolean isBlankSquare = false;
            for (int x=0;x<GAME_WIDTH;x++){
                for (int z=0;z<GAME_DEPTH;z++){
                    if (!a[x][y][z]){
                        isBlankSquare = true;
                    }
                }
            }
            if (!isBlankSquare){    // Tetris'ed a row! Good job.
                for (int x=0;x<GAME_WIDTH;x++){
                    for (int yy=y;yy<RENDERABLE_HEIGHT-1;yy++){
                        for (int z=0;z<GAME_DEPTH;z++){
                            a[x][yy][z] = a[x][yy+1][z];  // this has the effect of moving all the blocks down one (i hope)
                        }
                    }
                }
                for (int x=0;x<GAME_WIDTH;x++){
                    for (int z=0;z<GAME_DEPTH;z++){
                        a[x][7][z] = false;         // clear the top layer manually cos it gets missed by that fancy algorithm
                    }
                }
                hasTetrised = true;
                score++;
                y--;        // Since the row above fell into the row we're looking at now we need to do this row again
            }
        }
        if (hasTetrised){Toast.makeText(context, "Score: "+score, Toast.LENGTH_SHORT).show();}
        return a;
    }

    /**
     * checkForLoss: Checks all the blocks at the top of the game. If there is a block there, the player has lost the game (stacked too high)
     * @param a array to check
     * @return true if player has lost, otherwise false
     */
    public static boolean checkForLoss(boolean[][][] a){
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=GAME_HEIGHT-1;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    if (a[x][y][z]){
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * setVisibleBlocks: Takes a 3D bool array and makes the true blocks visible in the world.
     * @param boolArray The array of booleans to set to visible
     */
    public void setVisibleBlocks(boolean[][][] boolArray){
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    if (boolArray[x][y][z]){
                        blockNodeArray[x][y][z].setRenderable(blockRenderable);
                    }else{
                        blockNodeArray[x][y][z].setRenderable(null);
                    }
                }
            }
        }
    }

    public static boolean[][][] copyArray(boolean[][][] a){
        boolean[][][] b = new boolean[a.length][a[0].length][a[0][0].length];
        for (int x=0;x<a.length;x++){
            for (int y=0;y<a[0].length;y++){
                for (int z=0;z<a[0][0].length;z++){
                    b[x][y][z] = a[x][y][z];
                }
            }
        }
        return b;
    }

    /**
     * isStarted: lets us know when the game has started (the player has tapped in the world and a frame has been summoned)
     * @return true if the game is being played, false if otherwise.
     */
    public boolean isStarted(){
        return isStarted;
    }

    /**
     * userPressedLeft: moves a falling block left when there are no collisions
     */
    public void userPressedLeft() {
        boolean[][][] a = copyArray(fallingBlockArray);
        boolean blockAtEdge = false;

        for (int y=0;y<RENDERABLE_HEIGHT;y++){
            for (int z=0;z<GAME_DEPTH;z++){
                if (a[0][y][z]){
                    blockAtEdge = true;
                }
            }
        }

        if (!blockAtEdge) {
            for (int x=0;x<GAME_WIDTH-1;x++){
                for (int y=0;y<RENDERABLE_HEIGHT;y++){
                    for (int z=0;z<GAME_DEPTH;z++){
                        a[x][y][z] = a[x+1][y][z];
                    }
                }
            }
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    a[GAME_WIDTH-1][y][z] = false;
                }
            }

            if (!isCollided(blockArray, a)) {
                fallingBlockArray = a;
                setVisibleBlocks(combineArrays(blockArray, fallingBlockArray));
            }
        }
    }



    /**
     * userPressedRight: moves a falling block right when there are no collisions
     */
    public void userPressedRight() {
        boolean[][][] a = copyArray(fallingBlockArray);
        boolean blockAtEdge = false;

        for (int y=0;y<RENDERABLE_HEIGHT;y++){
            for (int z=0;z<GAME_DEPTH;z++){
                if (a[GAME_WIDTH-1][y][z]){
                    blockAtEdge = true;
                }
            }
        }

        if (!blockAtEdge) {
            for (int x=GAME_WIDTH-1;x>0;x--){
                for (int y=0;y<RENDERABLE_HEIGHT;y++){
                    for (int z=0;z<GAME_DEPTH;z++){
                        a[x][y][z] = a[x-1][y][z];
                    }
                }
            }
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    a[0][y][z] = false;
                }
            }

            if (!isCollided(blockArray, a)) {
                fallingBlockArray = a;
                setVisibleBlocks(combineArrays(blockArray, fallingBlockArray));
            }
        }
    }

    /**
     * userPressedForward: moves a falling block forward when there are no collisions
     */
    public void userPressedForward() {
        boolean[][][] a = copyArray(fallingBlockArray);
        boolean blockAtEdge = false;

        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                if (a[x][y][0]){
                    blockAtEdge = true;
                }
            }
        }

        if (!blockAtEdge) {
            for (int x=0;x<GAME_WIDTH;x++){
                for (int y=0;y<RENDERABLE_HEIGHT;y++){
                    for (int z=0;z<GAME_DEPTH-1;z++){
                        a[x][y][z] = a[x][y][z+1];
                    }
                }
            }
            for (int x=0;x<GAME_WIDTH;x++){
                for (int y=0;y<RENDERABLE_HEIGHT;y++){
                    a[x][y][GAME_DEPTH-1] = false;
                }
            }

            if (!isCollided(blockArray, a)) {
                fallingBlockArray = a;
                setVisibleBlocks(combineArrays(blockArray, fallingBlockArray));
            }
        }
    }

    /**
     * userPressedBackward: moves a falling block backward when there are no collisions
     */
    public void userPressedBackward() {
        boolean[][][] a = copyArray(fallingBlockArray);
        boolean blockAtEdge = false;

        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                if (a[x][y][GAME_DEPTH-1]){
                    blockAtEdge = true;
                }
            }
        }

        if (!blockAtEdge) {
            for (int x=0;x<GAME_WIDTH;x++){
                for (int y=0;y<RENDERABLE_HEIGHT;y++){
                    for (int z=GAME_DEPTH-1;z>0;z--){
                        a[x][y][z] = a[x][y][z-1];
                    }
                }
            }
            for (int x=0;x<GAME_WIDTH;x++){
                for (int y=0;y<RENDERABLE_HEIGHT;y++){
                    a[x][y][0] = false;
                }
            }

            if (!isCollided(blockArray, a)) {
                fallingBlockArray = a;
                setVisibleBlocks(combineArrays(blockArray, fallingBlockArray));
            }
        }
    }

    /**
     * restart: Instantiates a new game
     * @param arFragment from main activity
     */
    public void restart(ArFragment arFragment) {
        // Place a block at every point in the array. The renderables are kept null, because we change these later
        for (int x=0;x<GAME_WIDTH;x++){
            for (int y=0;y<RENDERABLE_HEIGHT;y++){
                for (int z=0;z<GAME_DEPTH;z++){
                    blockNodeArray[x][y][z] = new TransformableNode(arFragment.getTransformationSystem());
                    blockNodeArray[x][y][z].setParent(gameAnchor);
                    Vector3 localPos = blockNodeArray[x][y][z].getLocalPosition();

                    // Move each block so that it's in it's spot
                    localPos.x -= 0.375;
                    localPos.x += x*0.25;
                    localPos.z -= 0.375;
                    localPos.z += z*0.25;
                    localPos.y += y*0.25;
                    blockNodeArray[x][y][z].setLocalPosition(localPos);

                    // Make each block invisible (and then we can make them visible when we need to)
                    blockNodeArray[x][y][z].setRenderable(null);
                }
            }
        }

        isStarted = true;   // start the game
        score = 0;
        deltaTimeCount = 0.0f;

        getNextBlock(); // get the first block
    }

    private void onFrameUpdate(FrameTime frameTime) {
        deltaTimeCount += frameTime.getDeltaSeconds();
        if (deltaTimeCount > 1.0f){
            deltaTimeCount = 0.0f;
            gameTick();
        }
    }

}
