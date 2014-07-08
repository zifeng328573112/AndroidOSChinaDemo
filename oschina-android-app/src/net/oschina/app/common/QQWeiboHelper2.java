package net.oschina.app.common;

import net.oschina.app.AppContext;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import com.tencent.weibo.api.TAPI;
import com.tencent.weibo.constants.OAuthConstants;
import com.tencent.weibo.oauthv1.OAuthV1;
import com.tencent.weibo.oauthv1.OAuthV1Client;
import com.tencent.weibo.utils.QHttpClient;
import com.tencent.weibo.webview.OAuthV1AuthorizeWebView;

/**
 * 注意:务必要在所在的Activity中的onActivityResult中回调调用onAuthorizeWebViewReturn
 * 
 * 支持本地分享
 * 
 * @author yeguozhong@yeah.net
 * 
 */
@SuppressLint("Instantiatable")
public class QQWeiboHelper2 {

	private final static String APP_KEY = "96f54f97c4de46e393c4835a266207f4";
	private final static String APP_SECRET = "d10f1da05b2b17db1ac18471b6cb5de6";
	private final static int AUTH_VIEW_REQUEST_CODE = 1;

	//
	private final static String OAUTH_CACHE_FILE = "OAuthV1_CACHE";

	private String oauthCallback = "null";

	private OAuthV1 oAuth;
	private static OAuthV1 OAuthV1Cache;
	private TAPI tAPI;

	private String mContent;
	private String mPicPath;

	private AppContext ac;

	public final static int REQUEST = 0;
	public final static int ACCESS = 1;
	public final static int SEND = 2;

	private Activity context;

	// 标识是否回调了onAuthorizeWebViewReturn
	private boolean isCallBack = false;

	public QQWeiboHelper2(Activity context, String content, String mPic) {
		this.context = context;
		this.mContent = content;
		this.mPicPath = mPic;
	}

	/**
	 * 主线程和网络线程交互处理
	 */
	private Handler mHandler = new Handler() {

		@SuppressLint("Instantiatable")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REQUEST:
				oAuth = (OAuthV1) msg.obj;
				if (oAuth.getStatus() == 1) {
					UIHelper.ToastMessage(context, "Request Token 授权不通过");
				} else {
					Intent intent = new Intent(context,
							OAuthV1AuthorizeWebView.class);
					intent.putExtra("oauth", oAuth);
					context.startActivityForResult(intent,
							AUTH_VIEW_REQUEST_CODE);
				}
				break;
			case ACCESS:
				oAuth = (OAuthV1) msg.obj;
				if (oAuth.getStatus() == 3) {
					UIHelper.ToastMessage(context, "Access失败");
				} else {
					OAuthV1Cache = oAuth;
					ac.saveObject(oAuth, OAUTH_CACHE_FILE);
					QQWeiboHelper2.this.sendMessage();
				}
				break;
			case SEND:
				String data = (String) msg.obj;
				if (getReturnMsg(data, "ret").equals("0")) {
					UIHelper.ToastMessage(context, "分享成功");
				} else if (getReturnMsg(data, "ret").equals("3") // access_token超时
						&& getReturnMsg(data, "errcode").equals("4")) {
					authenticate(); // 重新认证
				} else {
					UIHelper.ToastMessage(context, getReturnMsg(data, "msg"));
				}
				break;
			}
		};
	};

	/**
	 * 发送
	 * @throws NotCallBackException 
	 */
	public void send() {
		ac = (AppContext) context.getApplication();

		// 获取凭证流程优先级: 内存 ->外存 ->网络
		if (OAuthV1Cache == null) {
			OAuthV1 oa = (OAuthV1) ac.readObject(OAUTH_CACHE_FILE);
			if (oa == null) {
				authenticate();
			} else {
				oAuth = OAuthV1Cache = oa;
				sendMessage();
			}
		} else {
			oAuth = OAuthV1Cache;
			sendMessage();
		}
	}

	/**
	 * 认证
	 * 
	 * @throws Exception
	 */
	private void authenticate() {
		oAuth = new OAuthV1(oauthCallback);
		oAuth.setOauthConsumerKey(APP_KEY);
		oAuth.setOauthConsumerSecret(APP_SECRET);
		OAuthV1Client.getQHttpClient().shutdownConnection();
		OAuthV1Client.setQHttpClient(new QHttpClient());

		new Thread() {
			@Override
			public void run() {
				Message msg = mHandler.obtainMessage(REQUEST);
				try {
					msg.obj = OAuthV1Client.requestToken(oAuth);
					mHandler.sendMessage(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	/**
	 * 
	 * 必须在所在的Activity中的onActivityResult中调用该方法....
	 * 
	 */
	public void onAuthorizeWebViewReturn(int requestCode, int resultCode,
			Intent data) {

		isCallBack = true;

		if (requestCode == AUTH_VIEW_REQUEST_CODE) {
			if (resultCode == OAuthV1AuthorizeWebView.RESULT_CODE) {
				oAuth = (OAuthV1) data.getExtras().getSerializable("oauth");
				if (oAuth.getStatus() == 2) {
					UIHelper.ToastMessage(context, "获取验证码失败");
				} else {
					new Thread() {
						@Override
						public void run() {
							Message msg = mHandler.obtainMessage(ACCESS);
							try {
								msg.obj = OAuthV1Client.accessToken(oAuth);
								mHandler.sendMessage(msg);
							} catch (Exception e) {
								e.printStackTrace();
							}
						};
					}.start();
				}
			}
		}
	}

	/**
	 * 发送信息
	 * 
	 * @throws Exception
	 */
	private void sendMessage() {
		new Thread() {
			public void run() {
				Message msg = mHandler.obtainMessage(SEND);
				tAPI = new TAPI(OAuthConstants.OAUTH_VERSION_1);
				try {
					msg.obj = tAPI.addPic(oAuth, "json", mContent, "127.1.1.1",
							mPicPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
				tAPI.shutdownConnection();
				mHandler.sendMessage(msg);
			};
		}.start();
	}

	/**
	 * 
	 * 截取返回的信息
	 */
	private String getReturnMsg(String data, String key) {
		int start = data.indexOf("\"" + key + "\":");
		if (start == -1)
			return "";
		int end = data.indexOf(",", start);
		String rs = data.substring(start + 6, end);
		return rs.replaceAll("\"", "");
	}
}
