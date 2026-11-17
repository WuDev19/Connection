package com.example.connection.ui.camera.effects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** Vector artwork in face-local coordinates; shared by the camera GPU overlay and stills. */
public final class FaceEffectRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static PointF inverseRotation(float x, float y, int rotation) {
        switch (rotation) {
            case 90: return new PointF(y, 1-x);
            case 180: return new PointF(1-x, 1-y);
            case 270: return new PointF(1-y, x);
            default: return new PointF(x,y);
        }
    }

    public void draw(Canvas canvas, float[] points, CameraEffectState.Face effect) {
        if (points == null || points.length < 936 || effect == CameraEffectState.Face.NONE) return;
        float w = canvas.getWidth(), h = canvas.getHeight();
        float ex = points[263*2]*w-points[33*2]*w;
        float ey = points[263*2+1]*h-points[33*2+1]*h;
        float width = (float)Math.hypot((points[454*2]-points[234*2])*w,
                (points[454*2+1]-points[234*2+1])*h);
        if (width < 8) return;
        canvas.save();
        canvas.translate((points[33*2]+points[263*2])*w/2,
                (points[33*2+1]+points[263*2+1])*h/2);
        canvas.rotate((float)Math.toDegrees(Math.atan2(ey,ex)));
        canvas.scale(width, width);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        switch (effect) {
            case CAT:
                ear(canvas,-.37f,-.42f,false); ear(canvas,.37f,-.42f,false); break;
            case BUNNY:
                ear(canvas,-.26f,-.45f,true); ear(canvas,.26f,-.45f,true); break;
            case CROWN:
                paint.setColor(0xffffd54f);
                polygon(canvas,-.43f,-.44f,-.48f,-.78f,-.23f,-.63f,0,-.9f,.23f,-.63f,.48f,-.78f,.43f,-.44f);
                paint.setColor(0xffff6a91);
                for(int i=-1;i<=1;i++) canvas.drawCircle(i*.23f,-.52f,.045f,paint);
                break;
            case HORNS:
                paint.setColor(0xffeb4162);
                polygon(canvas,-.45f,-.36f,-.5f,-.8f,-.16f,-.38f);
                polygon(canvas,.45f,-.36f,.5f,-.8f,.16f,-.38f); break;
            case SUNGLASSES:
            case HEART_GLASSES:
                paint.setColor(0xff242036); paint.setStrokeWidth(.035f);
                canvas.drawLine(-.1f,0,.1f,0,paint);
                canvas.drawLine(-.46f,-.035f,-.3f,0,paint);
                canvas.drawLine(.3f,0,.46f,-.035f,paint);
                for (int side : new int[]{-1,1}) {
                    if (effect == CameraEffectState.Face.HEART_GLASSES) {
                        paint.setColor(0xfff45a98); heart(canvas,side*.23f,0,.21f);
                        paint.setColor(0xff65264d); heart(canvas,side*.23f,-.004f,.155f);
                    } else {
                        paint.setColor(0xee171a2d);
                        canvas.drawRoundRect(side*.23f-.19f,-.11f,side*.23f+.19f,.13f,.065f,.065f,paint);
                        paint.setColor(0x995ecbdf); paint.setStrokeWidth(.018f);
                        canvas.drawLine(side*.23f-.09f,-.06f,side*.23f+.02f,.035f,paint);
                    }
                }
                break;
            case WHISKERS:
                paint.setColor(0xff69445c); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeWidth(.012f);
                for (int s : new int[]{-1,1}) for(int i=-1;i<=1;i++)
                    canvas.drawLine(s*.16f,.2f+i*.035f,s*.46f,.2f+i*.085f,paint);
                paint.setColor(0xfff19eb7); heart(canvas,0,.17f,.065f); break;
            case FRECKLES:
                paint.setColor(0xbb9b6446);
                for(int s : new int[]{-1,1}) for(int i=0;i<9;i++)
                    canvas.drawCircle(s*(.15f+(i%3)*.065f),.15f+(i/3)*.045f+(i%2)*.017f,.009f,paint);
                break;
            case BLUSH:
                for(int s : new int[]{-1,1}) {
                    paint.setShader(new RadialGradient(s*.3f,.2f,.14f,0xaaff7197,Color.TRANSPARENT, Shader.TileMode.CLAMP));
                    canvas.drawCircle(s*.3f,.2f,.14f,paint);
                }
                paint.setShader(null); break;
            case CHEEK_HEARTS:
                paint.setColor(0xefff689e);
                heart(canvas,-.3f,.18f,.1f); heart(canvas,.3f,.18f,.1f); break;
            default: break;
        }
        canvas.restore();
    }

    private void ear(Canvas c,float x,float y,boolean bunny) {
        c.save(); c.translate(x,y); c.rotate(x<0?-12:12);
        paint.setColor(0xfffff2ed);
        if(bunny) c.drawOval(-.12f,-.55f,.12f,.05f,paint);
        else polygon(c,-.18f,.08f,0,-.36f,.18f,.08f);
        paint.setColor(0xfff3a9c5);
        if(bunny) c.drawOval(-.065f,-.46f,.065f,-.02f,paint);
        else polygon(c,-.1f,.02f,0,-.23f,.1f,.02f);
        c.restore();
    }
    private void heart(Canvas c,float x,float y,float r) {
        Path p=new Path(); p.moveTo(x,y+r);
        p.cubicTo(x-r*2,y-r*.3f,x-r*.6f,y-r*1.5f,x,y-r*.45f);
        p.cubicTo(x+r*.6f,y-r*1.5f,x+r*2,y-r*.3f,x,y+r);
        c.drawPath(p,paint);
    }
    private void polygon(Canvas c,float... xy) {
        Path p=new Path(); p.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) p.lineTo(xy[i],xy[i+1]);
        p.close(); c.drawPath(p,paint);
    }
}
