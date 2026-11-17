package com.example.connection.ui.camera.effects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/** Owns immutable selection and the common photo / GPU effect definitions. */
public final class CameraEffectsProcessor implements AutoCloseable {
    private volatile CameraEffectState state=new CameraEffectState();
    final FaceEffectTracker tracker;
    public CameraEffectsProcessor(Context context, java.util.function.Consumer<Throwable> onError) {
        tracker=new FaceEffectTracker(context,onError);
    }
    public CameraEffectState getState() { return state; }
    public void setState(CameraEffectState value) {
        if(state.face!=value.face) tracker.reset();
        state=value;
    }
    public void resetTracking() { tracker.reset(); }

    public Bitmap processPhoto(Bitmap source, CameraEffectState snapshot) {
        Bitmap output=Bitmap.createBitmap(source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(output);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        float[] matrix=snapshot.colorMatrix();
        for(int i=4;i<20;i+=5) matrix[i]*=255;
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(matrix)));
        canvas.drawBitmap(source,0,0,paint);
        if(snapshot.face!=CameraEffectState.Face.NONE)
            new FaceEffectRenderer().draw(canvas,tracker.detect(source),snapshot.face);
        return output;
    }
    @Override public void close() { tracker.close(); }
}
