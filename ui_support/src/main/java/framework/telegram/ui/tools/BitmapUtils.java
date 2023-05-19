package framework.telegram.ui.tools;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bitmap处理类,所有静态方法中,参数传入的Bitmap均不会被释放,参数传出的Bitmap需要调用者手动释放
 */
public class BitmapUtils {

    /**
     * bitmap转为base64
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * base64转为bitmap
     *
     * @param base64Data
     * @return
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 获取视频缩略图
     *
     * @param videoPath
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getVideoThumbnail(String videoPath, int width, int height) {
        // 获取视频的缩略图
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
        if (bitmap != null && !bitmap.isRecycled()) {
            Bitmap sampleBitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            if (!bitmap.equals(sampleBitmap) && !bitmap.isRecycled()) {
                bitmap.recycle();
                System.gc();
            }
            return sampleBitmap;
        } else {
            return null;
        }
    }

    /**
     * 获取View截图
     *
     * @param v
     * @return
     */
    public static Bitmap getViewBitmap(View v) {
        try {
            v.clearFocus();
            v.setPressed(false);
        } catch (Exception e) {
        }

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }

        v.buildDrawingCache();

        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null || cacheBitmap.isRecycled()) {
            return null;
        }

        Bitmap bitmap = cloneBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    public static Bitmap makeViewBitmap(View v) {
        v.layout(v.getLeft(), v.getTop(), v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = v.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.TRANSPARENT);
        v.draw(canvas);
        canvas.save();

        return bitmap;
    }

    /**
     * 获取View截图
     *
     * @param v
     * @param width
     * @param height
     * @return
     */
    public static Bitmap makeViewBitmap(View v, int width, int height) {
        return makeViewBitmap(v, width, height, Color.TRANSPARENT);
    }

    public static Bitmap makeViewBitmap(View v, int width, int height, int bgColor) {
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        v.layout(v.getLeft(), v.getTop(), v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);
        v.draw(canvas);
        canvas.save();

        return bitmap;
    }

    /**
     * 叠加两张Bitmap
     *
     * @param background
     * @param foreground
     * @return
     */
    public static Bitmap toConformBitmap(Bitmap background, Bitmap foreground) {
        if (background == null || foreground == null) {
            return null;
        }

        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        //create the new blank bitmap 创建一个新的和SRC长度宽度一样的位图
        Bitmap newbmp = Bitmap.createBitmap(bgWidth, bgHeight, Config.ARGB_8888);
        Canvas cv = new Canvas(newbmp);
        //draw bg into
        cv.drawBitmap(background, 0, 0, null);//在 0，0坐标开始画入bg
        //draw fg into
        cv.drawBitmap(foreground, 0, 0, null);//在 0，0坐标开始画入fg ，可以从任意位置画入
        //save all clip
        cv.save();//保存
        //store
        cv.restore();//存储

        return newbmp;
    }

    /**
     * 合并两张Bitmap为一张
     *
     * @param top
     * @param bottom
     * @return Bitmap
     */
    public static Bitmap montageBitmap(Bitmap top, Bitmap bottom) {
        if (top == null || bottom == null) {
            return null;
        }

        int topWidth = top.getWidth();
        int topHeight = top.getHeight();
        int bottomWidth = bottom.getWidth();
        int bottomHeight = bottom.getHeight();
        int newWidth = topWidth > bottomWidth ? topWidth : bottomWidth;
        int newHeight = topHeight + bottomHeight;
        Bitmap newmap = Bitmap.createBitmap(newWidth, newHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(top, (newWidth - topWidth) / 2, 0, null);
        canvas.drawBitmap(bottom, (newWidth - bottomWidth) / 2,
                topHeight, null);
        canvas.save();
        canvas.restore();

        return newmap;
    }

    public static Bitmap montageBitmapVertical(Bitmap top, Bitmap bottom, int tmargin) {
        if (top == null || bottom == null) {
            return null;
        }

        int topWidth = top.getWidth();
        int topHeight = top.getHeight();
        int bottomWidth = bottom.getWidth();
        int bottomHeight = bottom.getHeight();

        Bitmap newmap = Bitmap.createBitmap(bottomWidth > topWidth ? bottomWidth : topWidth, topHeight + bottomHeight + tmargin, Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(bottom, (newmap.getWidth() - bottomWidth) / 2, topHeight + tmargin, null);
        canvas.drawBitmap(top, (newmap.getWidth() - topWidth) / 2, 0, null);
        canvas.save();
        canvas.restore();

        return newmap;
    }

    public static Bitmap montageBitmapWithBottomCenter(Bitmap top, Bitmap bottom, int rlmargin) {
        if (top == null || bottom == null) {
            return null;
        }

        int bottomWidth = bottom.getWidth();
        int bottomHeight = bottom.getHeight();
        int minBottom = bottomHeight > bottomWidth ? bottomWidth : bottomHeight;

        if (rlmargin < 0 || rlmargin > minBottom || rlmargin > minBottom / 2) {
            return null;
        }

        Bitmap newTopBmp = zoomBitmap(top, bottomWidth - rlmargin * 2, bottomWidth - rlmargin * 2);
        Bitmap newmap = Bitmap.createBitmap(bottomWidth, bottomHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(bottom, 0, 0, null);
        canvas.drawBitmap(newTopBmp, rlmargin, (newmap.getHeight() - newTopBmp.getHeight()) / 2, null);
        canvas.save();
        canvas.restore();

        newTopBmp.recycle();

        return newmap;
    }

    public static Bitmap montageBitmapWithBottomHorizontalCenter(Bitmap top, Bitmap bottom, int rltmargin) {
        if (top == null || bottom == null) {
            return null;
        }

        int bottomWidth = bottom.getWidth();
        int bottomHeight = bottom.getHeight();
        int minBottom = bottomHeight > bottomWidth ? bottomWidth : bottomHeight;

        if (rltmargin < 0 || rltmargin > minBottom || rltmargin > minBottom / 2) {
            return null;
        }

        Bitmap newTopBmp = zoomBitmap(top, minBottom - rltmargin * 2, minBottom - rltmargin * 2);
        Bitmap newmap = Bitmap.createBitmap(bottomWidth, bottomHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(bottom, 0, 0, null);
        canvas.drawBitmap(newTopBmp, rltmargin, rltmargin, null);
        canvas.save();
        canvas.restore();

        newTopBmp.recycle();

        return newmap;
    }

    /**
     * 保存Bitmap到文件
     *
     * @param savePath
     * @param image
     * @throws RuntimeException
     */
    public static void saveBitmap(File savePath, Bitmap image) throws RuntimeException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            if (savePath.exists()) {
                savePath.delete();
            }

            fos = new FileOutputStream(savePath);
            bos = new BufferedOutputStream(fos);
            image.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            bos.flush();
            fos.getFD().sync();
            bos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据宽度裁剪一样高度的Bitmap
     *
     * @param bitmap
     * @param y
     * @return
     */
    public static Bitmap cropPictureHeightWithWidth(Bitmap bitmap, int y) {
        if (bitmap == null) {
            return null;
        }

        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();

        int newH = Math.min(w, h - y);

        return Bitmap.createBitmap(bitmap, (w - newH) / 2, y, newH, newH, null, false);
    }

    /**
     * 复制Bitmap
     *
     * @param source
     * @return
     */
    public static Bitmap cloneBitmap(Bitmap source) {
        if (source == null) {
            return null;
        }

        return source.copy(Config.ARGB_8888, false);
    }

    /**
     * 缩放Bitmap
     *
     * @param bitmap    所要转换的bitmap
     * @param newWidth  新的宽
     * @param newHeight 新的高
     * @return 指定宽高的bitmap
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public static Bitmap zoomBitmapFromFile(File file, int reqWidth, int reqHeight) {
        Bitmap bitmap = decodeSampledBitmapFromFile(file, reqWidth, reqHeight);
        Bitmap newBitmap = zoomBitmap(bitmap, reqWidth, reqHeight);
        if (newBitmap != bitmap && bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }

        return newBitmap;
    }

    /**
     * 计算缩放比例
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;

        return calculateInSampleSize(width, height, reqWidth, reqHeight);
    }

    /**
     * 计算缩放比例
     *
     * @param width
     * @param height
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width < height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }

        return inSampleSize;
    }

    /**
     * 获取缩略图
     *
     * @param file
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromFile(File file, int reqWidth,
                                                     int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        InputStream is = null;
        try {
            is = new FileInputStream(new File(file.getAbsolutePath()));
            return BitmapFactory.decodeStream(is, new Rect(), options);
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取缩略图
     *
     * @param data
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromBytes(byte[] data, int reqWidth,
                                                      int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * 获取缩略图
     *
     * @param bitmap
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromBitmap(Bitmap bitmap, int reqWidth, int reqHeight) {
        if (bitmap == null) {
            return null;
        }

        // Calculate inSampleSize
        final Options options = new Options();
        options.inSampleSize = calculateInSampleSize(bitmap.getWidth(), bitmap.getHeight(), reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        byte[] data = bitmap2Bytes(bitmap);
        if (data != null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } else {
            return bitmap;
        }
    }

    /**
     * Bitmap转byte数组
     *
     * @param bm
     * @return
     */
    private static byte[] bitmap2Bytes(Bitmap bm) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取图片宽高
     *
     * @param file
     * @return
     */
    public static int[] getBitmapSize(File file) {
        try {
            final Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return new int[]{options.outWidth, options.outHeight};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    /**
     * 获取图片旋转值
     *
     * @param imageFilePath
     * @return
     */
    public static int getExifRotation(String imageFilePath) {
        if (imageFilePath == null)
            return 0;

        try {
            ExifInterface exif = new ExifInterface(imageFilePath);
            // We only recognize a subset of orientation tag values
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 获取Bitmap旋转值
     *
     * @param tempFile
     * @param bitmap
     * @return
     */
    public static int getExifRotation(File tempFile, Bitmap bitmap) {
        try {
            // 将图片保存
            FileOutputStream b = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);
            b.flush();
            b.close();
            return getExifRotation(tempFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 旋转Bitmap
     *
     * @param angle
     * @param bitmap
     * @return
     */
    public static Bitmap rotaingBitmap(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 灰度处理Bitmap
     *
     * @param bitmap
     * @return
     */
    public static final Bitmap grey(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap greyBitmap = Bitmap.createBitmap(width, height,
                Config.ARGB_8888);
        Canvas canvas = new Canvas(greyBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return greyBitmap;
    }

    /**
     * 圆形处理Bitmap
     *
     * @param bitmap
     * @return
     */
    public static Bitmap circular(Bitmap bitmap) {
        Bitmap output;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Config.ARGB_8888);
        } else {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        float r = 0;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            r = bitmap.getHeight() / 2;
        } else {
            r = bitmap.getWidth() / 2;
        }

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * 圆角处理Bitmap
     *
     * @param bitmap
     * @param radius
     * @return
     */
    public static Bitmap round(Bitmap bitmap, int radius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = radius;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * 透明度处理Bitmap
     *
     * @param bitmap
     * @param alpha
     * @return
     */
    public static final Bitmap alpha(Bitmap bitmap, int alpha) {
        float[] matrixItems = new float[]{1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 0, 0, alpha / 255f, 0, 0, 0, 0, 0, 1};
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap alphaBitmap = Bitmap.createBitmap(width, height,
                Config.ARGB_8888);
        Canvas canvas = new Canvas(alphaBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix(matrixItems);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return alphaBitmap;
    }
}
