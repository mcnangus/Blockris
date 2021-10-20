package com.google.ar.sceneform.samples.hellosceneform;

import org.junit.Test;

import static org.junit.Assert.*;

public class GameTest {
    
    private final boolean T = true;
    private final boolean F = false;

    @Test
    public void isCollidedTest() {
        boolean[][][] a =  {{{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                            {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                            {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                            {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}}};

        boolean[][][] b =  {{{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                            {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                            {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                            {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}}};
        assertFalse(Game.isCollided(a, b));

        a[3][5][2] = T;
        b[3][5][1] = T;
        assertFalse(Game.isCollided(a, b));

        a[0][5][0] = T;
        b[0][5][0] = T;
        assertTrue(Game.isCollided(a, b));

        b[0][5][0] = F;
        b[3][5][1] = F;
        // set all of an to true
        for (int x=0;x<4;x++){
            for (int y=0;y<8;y++){
                for (int z=0;z<4;z++){
                    a[x][y][z] = T;
                }
            }
        }
        assertFalse(Game.isCollided(a, b));
    }

    @Test
    public void blocksAtBottomTest() {
        boolean[][][] bottomCase = {{{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                    {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                    {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                    {{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}}};
        assertFalse(Game.blocksAtBottom(bottomCase));
        bottomCase[0][3][2] = true;
        assertFalse(Game.blocksAtBottom(bottomCase));
        bottomCase[0][2][0] = true;
        assertFalse(Game.blocksAtBottom(bottomCase));
        bottomCase[2][0][2] = true;                     // add a block on the bottom
        assertTrue(Game.blocksAtBottom(bottomCase));
    }


    boolean[][][] lossCase1 =   {{{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, T, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}}};

    boolean[][][] lossCase2 =   {{{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{T, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}}};

    boolean[][][] fineCase =   {{{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}},
                                {{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T},{F, F, F, F},{F, F, F, F},{F, F, F, F},{F, F, F, F}}};
    
    @Test
    public void checkForLossTest() {
        assertTrue(Game.checkForLoss(lossCase1));
        assertTrue(Game.checkForLoss(lossCase2));
        assertFalse(Game.checkForLoss(fineCase));
    }


    @Test
    public void copyArrayTest() {
        boolean[][][] a = {{{T, T, F, F},{F, T, T, T},{T, T, F, T},{F, F, F, T}},
            {{F, T, F, T},{F, F, F, F},{F, T, T, T},{F, F, F, T}},
            {{T, T, F, T},{F, T, T, F},{T, F, T, F},{T, F, F, F}},
            {{T, F, F, T},{T, T, F, T},{T, T, F, T},{F, F, T, T}}};
        boolean[][][] b = {{{F}}};
        assertEquals(b, Game.copyArray(b));
        assertEquals(a, Game.copyArray(a));
    }
}