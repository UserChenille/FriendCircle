package razerdp.github.com.lib.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.socks.library.KLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import razerdp.github.com.lib.manager.compress.CompressManager;

/**
 * Created by 大灯泡 on 2018/1/10.
 */
public class BitmapUtil {

    private static final String TAG = "BitmapUtil";

    /**
     * 获取图像的宽高
     **/
    public static int[] getImageSize(String path) {
        int[] size = {-1, -1};
        if (path == null) {
            return size;
        }
        File file = new File(path);
        if (file.exists() && !file.isDirectory()) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                InputStream is = new FileInputStream(path);
                BitmapFactory.decodeStream(is, null, options);
                size[0] = options.outWidth;
                size[1] = options.outHeight;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    public static int calculateSampleSize(BitmapFactory.Options options, int width, int height) {
        int w = options.outWidth;
        int h = options.outHeight;
        float candidateW = w / width;
        float candidateH = h / height;
        float candidate = Math.max(candidateW, candidateH);
        candidate = (float) (candidate + 0.5);

        if (candidate < 1.0) {
            return 1;
        }

        return (int) candidate;
    }

    public static Bitmap loadBitmap(Context c, String filePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError e) {
            return BitmapFactory.decodeFile(filePath);
        }
    }

    /**
     * 指定宽高加载bitmap
     */
    public static Bitmap loadBitmap(Context c, String filePath, int width, int height) {
        boolean needCalculateSampleSize = width > -1 && height > -1;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);
            if (needCalculateSampleSize) {
                options.inSampleSize = calculateSampleSize(options, width, height);
            }
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError e) {
            //oom，加载尝试加载一半
            if (needCalculateSampleSize) {
                return loadBitmap(c, filePath, (int) (width * 0.5), (int) (height * 0.5));
            } else {
                return loadBitmap(c, filePath);
            }
        }
    }


    /**
     * 保存bitmap图片
     *
     * @return 是否保存成功
     */
    public static boolean saveBitmap(String bitName, Bitmap bitmap, String suffix) {
        try {
            File temp = File.createTempFile("temp", suffix, new File(FileUtil.getNameDelLastPath(bitName)));
            FileOutputStream fOut = null;
            fOut = new FileOutputStream(temp);
            if (suffix.equals(CompressManager.PNG)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            }
            fOut.flush();
            fOut.close();
            if (temp.exists()) {
                File f = new File(bitName);
                if (f.exists()) {
                    f.delete();
                }
                FileUtil.moveFile(temp.getAbsolutePath(), bitName);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    public static boolean compressBmpToFile(Bitmap bmp, File file, int sizeKB, String suffix) {
        if (bmp == null) {
            return false;
        }
        int compressCount = 0;
        boolean isJpg = TextUtils.equals(suffix, CompressManager.JPG);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;
        bmp.compress(isJpg ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, options, baos);
        long sizeInKb = baos.toByteArray().length >> 10;
        KLog.d(TAG, "执行第1次压缩,压缩前质量  >>  " + sizeInKb + "kb");
        while (sizeInKb > sizeKB && (options - 5) > 0) {
            compressCount++;
            baos.reset();
            options -= 5;
            bmp.compress(isJpg ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, options, baos);
            sizeInKb = baos.toByteArray().length >> 10;
            KLog.d(TAG, "第" + compressCount + "次压缩完成,压缩后质量  >>  " + sizeInKb + "kb");
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } catch (OutOfMemoryError e) {
            System.gc();
            return false;
        }
    }
}
