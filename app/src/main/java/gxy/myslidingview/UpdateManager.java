package gxy.myslidingview;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;


/**
 *@author AsionReachel
 *created at 2016/1/11 12:00
 */
public class UpdateManager {
	/* 下载中 */
	private static final int DOWNLOAD = 1;
	/* 下载结束 */
	private static final int DOWNLOAD_FINISH = 2;
	/*
	 * 检测更新
	 */
	private static final int UPDATE = 3;
	/* 保存解析的XML信息 */
	HashMap<String, String> mHashMap;
	/* 下载保存路径 */
	private String mSavePath;
	/* 记录进度条数量 */
	private int progress;
	/* 版本号 */
	private long servicecode;
	private long localcode;
	/* 是否取消更新 */
	private boolean cancelUpdate = false;
	private String versionCode = "";
	private Context mContext;
	/* 更新进度条 */
	private ProgressBar mProgress;
	private Dialog mDownloadDialog;
	private boolean isupdate = true;
	private HashMap<String, String> map;
	private String downloadpath, serviceCode, updateInfo;
	private boolean flag;
	private boolean isMustUpdate;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// 正在下载
			case UPDATE:
				if (servicecode > localcode) {
					isupdate = false;
				}
				if (!isupdate) {
					// 显示提示对话框
					// showNoticeDialog();
				} else {
					Toast.makeText(mContext, "已是最新版本", Toast.LENGTH_LONG).show();
				}
				break;
			case DOWNLOAD:
				// 设置进度条位置
				mProgress.setProgress(progress);
				break;
			case DOWNLOAD_FINISH:
				// 安装文件
				installApk();
				break;
			default:
				break;
			}
		};
	};

	public UpdateManager(Context context, boolean flag, boolean isMustUpdate) {
		this.mContext = context;
		this.flag = flag;
		this.isMustUpdate = isMustUpdate;
	}

	/**
	 * 检测软件更新
	 */
	// public void checkUpdate()
	// {
	// isUpdate();
	// }

	/**
	 * 检查软件是否有更新版本
	 * 
	 * @return
	 */


	/**
	 * 获取软件版本号
	 * 
	 * @param context
	 * @return
	 */
	private int getVersionCode(Context context) {
		int versionCode = 0;
		try {
			// 获取软件版本号，对应AndroidManifest.xml下android:versionCode
			versionCode = context.getPackageManager().getPackageInfo("com.szy.update", 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	/**
	 * 显示软件更新对话框
	 */
	public void showNoticeDialog(final String downloadPath) {
		// 构造对话框
		Builder builder = new Builder(mContext);
		builder.setTitle("软件更新");
		if(isMustUpdate){
			builder.setMessage("检测到新版本，需更新后才能使用，是否立即更新？");
		}else{
		builder.setMessage("检测到新版本，是否立即更新？");
		}
		// 更新
		builder.setPositiveButton("立即更新", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				// 显示下载对话框
				showDownloadDialog(downloadPath);
			}
		});
		// 稍后更新
		if (!isMustUpdate) {
			builder.setNegativeButton("稍后更新", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					//点击稍后更新进入主界面，这里我先注释，需要再放开
//					if (flag) {
//						Intent intent = new Intent(mContext, MainActivity.class);
//						mContext.startActivity(intent);
//						((LoadingActivity)mContext).finish();
//					}

				}
			});
		}
		Dialog noticeDialog = builder.create();
		noticeDialog.show();
	}

	
	
	/**
	 * 显示软件下载对话框
	 */
	private void showDownloadDialog(String downloadPath) {
		downloadpath = downloadPath;
		// 构造软件下载对话框
		Builder builder = new Builder(mContext);
		builder.setTitle("正在更新");
		builder.setMessage(updateInfo);
		// 给下载对话框增加进度条
		final LayoutInflater inflater = LayoutInflater.from(mContext);
		View v = inflater.inflate(R.layout.softupdate_progress, null);
		mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
		builder.setView(v);
		// 取消更新
		builder.setNegativeButton("取消", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				// 设置取消状态,这里我直接用强转了，没有传activity进来了。
				cancelUpdate = true;
				if (isMustUpdate) {
					((MainActivity)mContext).finish();
				} else {
					mContext.startActivity(new Intent(mContext, MainActivity.class));
					((MainActivity)mContext).finish();
				}
				// System.exit(0);
			}
		});
		mDownloadDialog = builder.create();
		mDownloadDialog.setCanceledOnTouchOutside(false);
		mDownloadDialog.show();
		// 下载文件
		downloadApk();
	}

	/**
	 * 下载apk文件
	 */
	private void downloadApk() {
		// 启动新线程下载软件
		new downloadApkThread().start();
	}

	/**
	 * 下载文件线程
	 * 
	 * @author AsionReachel
	 * @date 2012-4-26
	 * @blog
	 */
	private class downloadApkThread extends Thread {
		@Override
		public void run() {
			try {
				// 判断SD卡是否存在，并且是否具有读写权限
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					//这里是一个日志开关打印日志，目前我先注释，需要了解的童鞋可以咨询
//					LogUtils.logiYq( downloadpath);
					// 获得存储卡的路径
					String sdpath = Environment.getExternalStorageDirectory() + "/";
					mSavePath = sdpath + "download";
					URL url = new URL(downloadpath);
					URLConnection conn=null ;
//					HttpURLConnection conn1 ;
					if(url.getProtocol().equals("http")){
						conn=(HttpURLConnection)url.openConnection();
					}else if(url.getProtocol().equals("https")){
					// 创建连接
					conn= (HttpsURLConnection) url.openConnection();
					}
					conn.connect();
					// 获取文件大小
					int length = conn.getContentLength();
					// 创建输入流
					InputStream is = conn.getInputStream();

					File file = new File(mSavePath);
					// 判断文件目录是否存在
					if (!file.exists()) {
						file.mkdir();
					}
					File apkFile = new File(mSavePath, "sp2p_gxy");
					FileOutputStream fos = new FileOutputStream(apkFile);
					int count = 0;
					// 缓存
					byte buf[] = new byte[1024];
					// 写入到文件中
					do {
						int numread = is.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) (((float) count / length) * 100);
						// 更新进度
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							// 下载完成
							mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
							break;
						}
						// 写入文件
						fos.write(buf, 0, numread);
					} while (!cancelUpdate);// 点击取消就停止下载.
					fos.close();
					is.close();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// 取消下载对话框显示
			mDownloadDialog.dismiss();
		}
	};

	/**
	 * 安装APK文件
	 */
	private void installApk() {
		File apkfile = new File(mSavePath, "sp2p_gxy");
		if (!apkfile.exists()) {
			return;
		}
		// 通过Intent安装APK文件
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(i);

	}
}
