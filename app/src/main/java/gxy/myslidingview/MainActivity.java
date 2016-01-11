package gxy.myslidingview;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //判断是否强制更新，版本号相差5就强制更新，否则不强制
        UpdateManager um;
        if(systemVersion-appVersion>5)

        {
            appVersion= getVersionCode();
            um= new UpdateManager(this, true, true);
        }

        else

        {
            um = new UpdateManager(this, true, false);
        }
        um.showNoticeDialog("https://download.gxyclub.com/sp2p_gxy.apk");
    }




    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public int getVersionCode() {
        int versionCode;
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            versionCode=0;
        }
        return versionCode;
    }

    private int systemVersion = 10;
    private int appVersion =getVersionCode();





}
