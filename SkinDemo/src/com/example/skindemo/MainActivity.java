package com.example.skindemo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView mTextView;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTextView = (TextView) this.findViewById(R.id.tv);
        mTextView.setText("鹅鹅鹅 曲项向天歌");
        //getResources()这个东西会不同
        mTextView.setTextColor(this.getResources().getColor(R.color.tv_color));

        //获取某个文件夹下的某个文件
        /**
         * 1. 确定需要的文件是否存在 - checkFileExistence()
         */
        checkFileExistence();
    }

    private String mPath;
    private static final String FILENAME = "ResDemo.apk";
    private String mDir;

    /**
     * 确定需要的文件是否存在
     * 文件放在sdcard根目录下面 名为ResDemo.apk
     * 
     * @return
     */
    private boolean checkFileExistence() {
        mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        mDir = mPath + FILENAME;
        Log.d("gxq", "dir: " + mDir);
        File skinDir = new File(mDir);
        boolean isExist = skinDir.exists();
        Log.d("gxq", "skinDir.exists(): " + isExist);
        if (isExist) {
            mTextView.setText(mDir);
            getSpecificResources();
        }
        return false;
    }

    /**
     * 反射调用android.content.res.AssetManager类，新建个实例，调用隐藏的方法addAssetPath(String
     * path)将未安装APK文件的添加进去，然后用这个AssetManager来构建出一个Resource实例就好了
     */
    private void getSpecificResources() {
        try {
            Class<?> class_AssetManager = Class.forName("android.content.res.AssetManager");
            Object assetMag = class_AssetManager.newInstance();
            /**
             * Add an additional set of assets to the asset manager. This can be
             * either a directory or ZIP file. Not for use by applications. Returns
             * the cookie of the added asset, or 0 on failure. {@hide}
             */
            Method method_addAssetPath = class_AssetManager.getDeclaredMethod("addAssetPath", String.class);
            method_addAssetPath.invoke(assetMag, mDir);
            Resources res = mContext.getResources();
            Constructor<?> constructor_Resources = Resources.class.getConstructor(class_AssetManager, res
                    .getDisplayMetrics().getClass(), res.getConfiguration().getClass());
            res = (Resources) constructor_Resources.newInstance(assetMag, res.getDisplayMetrics(),
                    res.getConfiguration());

            //通过resource name / package name 来获取未安装apk中的资源id
            //resource name
            String resName = mContext.getResources().getResourceName(R.color.tv_color);
            resName = resName.substring(resName.indexOf(":") + 1);
            Log.d("gxq", "resName: " + resName);
            //package name
            String pkgName = getPkgName();
            int id = res.getIdentifier(resName, null, pkgName);
            Log.d("gxq", "id in resdemo: " + id);
            mTextView.setText("Congratulations~~ 撒花~~~");
            mTextView.setTextColor(res.getColor(id));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPkgName() {
        String PATH_PackageParser = "android.content.pm.PackageParser";
        try {
            // 反射得到pkgParserCls对象并实例化,有参数  
            Class<?> pkgParserCls = Class.forName(PATH_PackageParser);
            Class<?>[] typeArgs = { String.class };
            Constructor<?> pkgParserCt = pkgParserCls.getConstructor(typeArgs);
            Object[] valueArgs = { mDir };
            Object pkgParser = pkgParserCt.newInstance(valueArgs);

            // 从pkgParserCls类得到parsePackage方法  
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();// 这个是与显示有关的, 这边使用默认  
            typeArgs = new Class<?>[] { File.class, String.class, DisplayMetrics.class, int.class };
            Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage", typeArgs);

            valueArgs = new Object[] { new File(mDir), mDir, metrics, 0 };

            // 执行pkgParser_parsePackageMtd方法并返回  
            Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);

            // 从返回的对象得到名为"applicationInfo"的字段对象  
            if (pkgParserPkg == null) {
                return null;
            }
            Field appInfoFld = pkgParserPkg.getClass().getDeclaredField("applicationInfo");

            // 从对象"pkgParserPkg"得到字段"appInfoFld"的值  
            if (appInfoFld.get(pkgParserPkg) == null) {
                return null;
            }
            ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);
            String pkgName = info.packageName;
            Log.d("gxq", "pkgName： " + pkgName);
            return pkgName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
