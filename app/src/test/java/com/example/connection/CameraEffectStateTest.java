package com.example.connection;

import com.example.connection.ui.camera.effects.CameraEffectState;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraEffectStateTest {
    @Test public void changingAndResettingGroupsIsIndependent() {
        CameraEffectState state=new CameraEffectState().withFace(CameraEffectState.Face.BUNNY)
                .withColor(CameraEffectState.Color.WARM).withIntensity(.4f);
        assertEquals(CameraEffectState.Face.BUNNY,state.withColor(CameraEffectState.Color.ORIGINAL).face);
        assertEquals(CameraEffectState.Color.WARM,state.withFace(CameraEffectState.Face.NONE).color);
        assertEquals(.4f,state.withColor(CameraEffectState.Color.COOL).intensity,0);
        assertEquals(CameraEffectState.Face.BUNNY,state.face);
    }
    @Test public void zeroIntensityAlwaysPreservesInput() {
        float[] identity=new CameraEffectState().colorMatrix();
        for(CameraEffectState.Color color:CameraEffectState.Color.values())
            assertArrayEquals(identity,new CameraEffectState().withColor(color).withIntensity(0).colorMatrix(),0);
    }
    @Test public void monochromeProducesEqualChannelsAndPreservesAlpha() {
        float[] m=new CameraEffectState().withColor(CameraEffectState.Color.MONO).withIntensity(1).colorMatrix();
        for(int i=0;i<5;i++) { assertEquals(m[i],m[5+i],.000001f); assertEquals(m[i],m[10+i],.000001f); }
        assertArrayEquals(new float[]{0,0,0,1,0},java.util.Arrays.copyOfRange(m,15,20),0);
    }
    @Test public void intensityClampsAndDoesNotAllowNaN() {
        assertEquals(0,new CameraEffectState().withIntensity(-1).intensity,0);
        assertEquals(1,new CameraEffectState().withIntensity(2).intensity,0);
        assertEquals(.7f,new CameraEffectState().withIntensity(Float.NaN).intensity,0);
    }
}
