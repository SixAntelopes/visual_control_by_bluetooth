package com.bignerdranch.android.visual_control_by_bluetooth;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.Vector;

public class Utils {
    //复制图片，并设置isMutable=true
    public static Bitmap copyBitmap(Bitmap bitmap){
        return bitmap.copy(bitmap.getConfig(),true);
    }
    //在bitmap中画矩形
    public static void drawRect(Bitmap bitmap,Rect rect){
        try {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            int r=99;
            int g=244;
            int b=63;
            paint.setColor(Color.rgb(r, g, b));

            paint.setStrokeWidth(1+bitmap.getWidth()/500 );
            paint.setStyle(Paint.Style.STROKE);
            //画矩形，传入rect数据结构，rect有左右上下四个矩形的坐标。
            canvas.drawRect(rect, paint);

        }catch (Exception e){
            Log.i("Utils","[*] error"+e);
        }
    }

    public static void drawJudge(Bitmap bitmap, Point[] landmark)
    {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        int r=52;
        int g=152;
        int b=219;
        paint.setColor(Color.rgb(r, g, b));

        paint.setStrokeWidth(1+bitmap.getWidth()/500 );
        paint.setStyle(Paint.Style.STROKE);

        int eyeLeftX = landmark[0].x;
        int eyeLeftY = landmark[0].y;
        int eyeRightX = landmark[1].x;
        int eyeRightY = landmark[1].y;
        int noseX = landmark[2].x;
        int noseY = landmark[2].y;
        int mouthLeftX = landmark[3].x;
        int mouthLeftY = landmark[3].y;
        int mouthRightX = landmark[4].x;
        int mouthRightY = landmark[4].y;
        canvas.drawLine(eyeLeftX,eyeLeftY,eyeRightX,eyeRightY,paint);
        canvas.drawLine(eyeLeftX,eyeLeftY,noseX,noseY,paint);
        canvas.drawLine(eyeRightX,eyeRightY,noseX,noseY,paint);
        canvas.drawLine(mouthLeftX,mouthLeftY,noseX,noseY,paint);
        canvas.drawLine(mouthRightX,mouthRightY,noseX,noseY,paint);
        canvas.drawLine(mouthLeftX,mouthLeftY,mouthRightX,mouthRightY,paint);
    }
    //  关键判断部分
    public static int judge(Rect rect, Point[] landmark)
    {
        double result = 0;
        //0-2 右眼和鼻子，0-1 左眼和鼻子
        double left = (double)(landmark[1].y - landmark[2].y) / (landmark[1].x - landmark[2].x);
        double right = (double)(landmark[0].y - landmark[2].y) / (landmark[0].x - landmark[2].x);

        left *= (landmark[1].y - landmark[2].y) * (landmark[1].y - landmark[2].y) - (landmark[1].x - landmark[2].x) * (landmark[1].x - landmark[2].x);
        right *= (landmark[0].y - landmark[2].y) * (landmark[0].y - landmark[2].y) - (landmark[0].x - landmark[2].x) * (landmark[0].x - landmark[2].x);

        result = left + right;
        if(result >= 3000)
            return 1;
        else
            if(result <= -3000)
                return 2;
        else
            return 0;
    }

    public static int newJudge(Rect rect, Point[] landmark) {

        int eyeLeftX = landmark[0].x;
        int eyeLeftY = landmark[0].y;
        int eyeRightX = landmark[1].x;
        int eyeRightY = landmark[1].y;
        int noseX = landmark[2].x;
        int noseY = landmark[2].y;
        int mouthLeftX = landmark[3].x;
        int mouthLeftY = landmark[3].y;
        int mouthRightX = landmark[4].x;
        int mouthRightY = landmark[4].y;

        //鼻子与左或右眼的距离占两眼距离一定比例，说明转头到一定程度
        double eye = (eyeRightX-eyeLeftX);
        double eyeR = eye/(eyeRightX-noseX);
        if (eyeR > 0 && eyeR < 1.2) return 1;
        double eyeL = eye/(noseX-eyeLeftX);
        if (eyeL > 0 && eyeL < 1.1) return 2;
//        if ((noseX-eyeRightX)>-10 && (noseX-mouthRightX)>-10) return 2;
        //鼻子和眼睛距离比和嘴距离大2.5倍，就后退
        eye = Math.min(eyeLeftY, eyeRightY); //todo (eyeLeftY+eyeRightY)/2.0;
        double mouth = Math.max(mouthLeftY, mouthRightY); //todo (mouthLeftY+mouthRightY)/2.0;
        double eye2mouth = mouth - eye;
        if (eye2mouth/(mouth - noseY) > 2.7) return 3;
        if (eye2mouth/(noseY - eye) > 2.7) return 4;
//        if (((noseY - eye) > (mouth - noseY)*2)) return 3;
//        if (((noseY - eye)*2.5 < (mouth - noseY))) return 4;
        return 0;

    }


    //在图中画点
    public static void drawPoints(Bitmap bitmap, Point[] landmark){
        for (int i=0;i<landmark.length;i++){
            int x=landmark[i].x;
            int y=landmark[i].y;
            //Log.i("Utils","[*] landmarkd "+x+ "  "+y);
            drawRect(bitmap,new Rect(x-1,y-1,x+1,y+1));
        }
    }
    //Flip alone diagonal
    //对角线翻转。data大小原先为h*w*stride，翻转后变成w*h*stride
    public static void flip_diag(float[]data,int h,int w,int stride){
        float[] tmp=new float[w*h*stride];
        for (int i=0;i<w*h*stride;i++) tmp[i]=data[i];
        for (int y=0;y<h;y++)
            for (int x=0;x<w;x++){
                for (int z=0;z<stride;z++)
                    data[(x*h+y)*stride+z]=tmp[(y*w+x)*stride+z];
            }
    }
    //src转为二维存放到dst中
    public static void expand(float[] src,float[][]dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                dst[y][x]=src[idx++];
    }
    //src转为三维存放到dst中
    public static void expand(float[] src,float[][][] dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                for (int c=0;c<dst[0][0].length;c++)
                    dst[y][x][c]=src[idx++];

    }
    //dst=src[:,:,1]
    public static void expandProb(float[] src,float[][]dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                dst[y][x]=src[idx++*2+1];
    }
    //box转化为rect
    public static Rect[] boxes2rects(Vector<Box> boxes){
        int cnt=0;
        for (int i=0;i<boxes.size();i++) if (!boxes.get(i).deleted) cnt++;
        Rect[] r=new Rect[cnt];
        int idx=0;
        for (int i=0;i<boxes.size();i++)
            if (!boxes.get(i).deleted)
                r[idx++]=boxes.get(i).transform2Rect();
        return r;
    }
    //删除做了delete标记的box
    public static Vector<Box> updateBoxes(Vector<Box> boxes){
        Vector<Box> b=new Vector<Box>();
        for (int i=0;i<boxes.size();i++)
            if (!boxes.get(i).deleted)
                b.addElement(boxes.get(i));
        return b;
    }
    //
    static public void showPixel(int v){
        Log.i("MainActivity","[*]Pixel:R"+((v>>16)&0xff)+"G:"+((v>>8)&0xff)+ " B:"+(v&0xff));
    }
}
