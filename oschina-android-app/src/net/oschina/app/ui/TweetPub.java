package net.oschina.app.ui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.oschina.app.AppConfig;
import net.oschina.app.AppContext;
import net.oschina.app.AppException;
import net.oschina.app.R;
import net.oschina.app.adapter.GridViewFaceAdapter;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.Tweet;
import net.oschina.app.common.AudioRecordUtils;
import net.oschina.app.common.FileUtils;
import net.oschina.app.common.ImageUtils;
import net.oschina.app.common.MediaUtils;
import net.oschina.app.common.StringUtils;
import net.oschina.app.common.UIHelper;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 发表动弹
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-3-21
 */
public class TweetPub extends BaseActivity{
	
	private final static String TAG = "TweetPub";
	
	// 录制
	private final static byte RECORDER_STATE_RECARDING = 0x0;
	// 录制时间太短
	private final static byte RECORDER_STATE_SHORT = 0x01;
	// 发布中
	private final static byte TWEET_PUBING = 0X02;
	// 取消发布
	private final static byte RECORDER_STATE_CANALE = 0x03;
	// 普通动弹
	private final static byte TWEET_TYPE_CONTENT = 0X04;
	// 语音动弹
	private final static byte TWEET_TYPE_VOICE = 0X05;
	
	// 语音最短时间(秒)
	private final static int RECORDER_TIME_MINTIME = 1;
	// 语音最长时间(秒)
	private final static int RECORDER_TIME_MAXTIME = 60;
	
	private AppContext ac;
	private DisplayMetrics dm;
	private FrameLayout mForm;
	private ImageView mBack;
	private EditText mContent;
	private Button mPublish;
	private ImageView mFace;
	// 语音按钮
	private ImageView mAudio;
	private Button mRecorder;
	private ImageView mPick;
	private ImageView mAtme;
	private ImageView mSoftware;
	private ImageView mImage;
	private LinearLayout mClearwords;
	private TextView mNumberwords;
	private LinearLayout mRecardStatus;// 录音状态下的布局
	private LinearLayout mRecardStatusShow;// 录音状态显示
	private Handler mHandler;
	
	// 录制语音时涉及操作的控件
	private FrameLayout mvFrame;
	private RelativeLayout mvAnimArea;// 录制声音大小布局
	private ImageView mvVolume;// 录音音量
	private RelativeLayout mvCancelArea;// 取消发布布局
	private LinearLayout mvTooShort;// 录音太短
	private TextView mvTimeMess;// 录音剩余时间提示
	private AudioRecordUtils recordUtils;// 录音工具类
	
	private GridView mGridView;
	private GridViewFaceAdapter mGVFaceAdapter;
	
	private Tweet tweet;
	private File imgFile;
	private File amrFile;// 语音文件
	private String theLarge;
	private String theThumbnail;
	private InputMethodManager imm;
	
	private String tempTweetKey = AppConfig.TEMP_TWEET;
	private String tempTweetImageKey = AppConfig.TEMP_TWEET_IMAGE;
	
	public static LinearLayout mMessage;
	public static Context mContext;
	
	private static final int MAX_TEXT_LENGTH = 160;//最大输入字数
	private static final String TEXT_ATME = "@请输入用户名 ";
	private static final String TEXT_SOFTWARE = "#请输入软件名#";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tweet_pub);
		
		mContext = this;
		//软键盘管理类
		imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		
		//初始化基本视图
		this.initView();
		//初始化表情视图
		this.initGridView();
	}
    @Override
	protected void onDestroy() {
    	deleteVoiceFile();
    	mContext = null;
    	super.onDestroy();
	}
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if(mGridView.getVisibility() == View.VISIBLE){
    		//隐藏表情
    		hideFace();
    	}
    }
    
    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		if(mGridView.getVisibility() == View.VISIBLE) {
    			//隐藏表情
    			hideFace();
    		}else{
    			return super.onKeyDown(keyCode, event);
    		}
    	}
    	return true;
    }
    
	//初始化视图控件
    private void initView()
    {   
    	// 获得手机屏幕的像素大小
    	ac = (AppContext)getApplication();
    	dm = new DisplayMetrics();  
		getWindowManager().getDefaultDisplay().getMetrics(dm);
    	mForm = (FrameLayout)findViewById(R.id.tweet_pub_form);
    	mBack = (ImageView)findViewById(R.id.tweet_pub_back);
    	mMessage = (LinearLayout)findViewById(R.id.tweet_pub_message);
    	mImage = (ImageView)findViewById(R.id.tweet_pub_image);
    	mPublish = (Button)findViewById(R.id.tweet_pub_publish);
    	mContent = (EditText)findViewById(R.id.tweet_pub_content);
    	mFace = (ImageView)findViewById(R.id.tweet_pub_footbar_face);
    	// 录音的按钮
    	mAudio = (ImageView) findViewById(R.id.tweet_pub_footbar_audio);
    	mRecorder = (Button) findViewById(R.id.tweet_pub_footbar_recarder);
    	mPick = (ImageView)findViewById(R.id.tweet_pub_footbar_photo);
    	mAtme = (ImageView)findViewById(R.id.tweet_pub_footbar_atme);
    	mSoftware = (ImageView)findViewById(R.id.tweet_pub_footbar_software);
    	mHandler = new Handler();
    	// 录音状态下的相关布局
    	mRecardStatus = (LinearLayout) findViewById(R.id.tweet_pub_record_status);
    	mRecardStatusShow = (LinearLayout) findViewById(R.id.tweet_pub_record_status_show);
    	mClearwords = (LinearLayout)findViewById(R.id.tweet_pub_clearwords);
    	mNumberwords = (TextView)findViewById(R.id.tweet_pub_numberwords);
    	recordUtils = new AudioRecordUtils();
    	
    	// 录制语音时涉及操作的控件
    	mvFrame = (FrameLayout) findViewById(R.id.tweet_pub_voice_rcd_hint_rcding);
    	mvAnimArea = (RelativeLayout) findViewById(R.id.tweet_pub_voice_rcd_hint_anim_area);// 录制声音大小布局
    	mvVolume = (ImageView) findViewById(R.id.tweet_pub_voice_rcd_hint_anim);// 录音状态动画
    	mvCancelArea = (RelativeLayout) findViewById(R.id.tweet_pub_voice_rcd_hint_cancel_area);// 取消发布布局
    	mvTooShort = (LinearLayout) findViewById(R.id.tweet_pub_voice_rcd_hint_tooshort);// 录音太短
    	mvTimeMess = (TextView) findViewById(R.id.tweet_pub_record_status_time_mes);// 录音剩余时间提示
    	
    	mBack.setOnClickListener(UIHelper.finish(this));
    	mPublish.setOnClickListener(publishClickListener);
    	mImage.setOnLongClickListener(imageLongClickListener);
    	mFace.setOnClickListener(faceClickListener);
    	mAudio.setOnClickListener(audioClickListener);
    	mRecorder.setOnTouchListener(recorderTouchListener);
    	mPick.setOnClickListener(pickClickListener);
    	mAtme.setOnClickListener(atmeClickListener);
    	mSoftware.setOnClickListener(softwareClickListener);
    	mClearwords.setOnClickListener(clearwordsClickListener);   	
    	//@某人
    	String atme = getIntent().getStringExtra("at_me");
    	int atuid = getIntent().getIntExtra("at_uid",0);
    	if(atuid > 0){
    		tempTweetKey = AppConfig.TEMP_TWEET + "_" + atuid;
    		tempTweetImageKey = AppConfig.TEMP_TWEET_IMAGE + "_" + atuid;
    	}
    	
    	//编辑器添加文本监听
    	mContent.addTextChangedListener(new TextWatcher() {		
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//保存当前EditText正在编辑的内容
				((AppContext)getApplication()).setProperty(tempTweetKey, s.toString());
				//显示剩余可输入的字数
				mNumberwords.setText((MAX_TEXT_LENGTH - s.length()) + "");
			}		
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}		
			public void afterTextChanged(Editable s) {}
		});
    	//编辑器点击事件
    	mContent.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//显示软键盘
				showIMM();
			}
		});
    	//设置最大输入字数
    	InputFilter[] filters = new InputFilter[1];  
    	filters[0] = new InputFilter.LengthFilter(MAX_TEXT_LENGTH);
    	mContent.setFilters(filters);
    	
    	//显示临时编辑内容
		UIHelper.showTempEditContent(this, mContent, tempTweetKey);
		//显示临时保存图片
		String tempImage = ((AppContext)getApplication()).getProperty(tempTweetImageKey);
		if(!StringUtils.isEmpty(tempImage)) {
    		Bitmap bitmap = ImageUtils.loadImgThumbnail(tempImage, 100, 100);
    		if(bitmap != null) {
    			imgFile = new File(tempImage);
				mImage.setImageBitmap(bitmap);
				mImage.setVisibility(View.VISIBLE);
			}
		}
		
		if(atuid > 0 && mContent.getText().length() == 0){
			mContent.setText(atme);
    		mContent.setSelection(atme.length());//设置光标位置
		}
    }
    
    //初始化表情控件
    private void initGridView() {
    	mGVFaceAdapter = new GridViewFaceAdapter(this);
    	mGridView = (GridView)findViewById(R.id.tweet_pub_faces);
    	mGridView.setAdapter(mGVFaceAdapter);
    	mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//插入的表情
				SpannableString ss = new SpannableString(view.getTag().toString());
				Drawable d = getResources().getDrawable((int)mGVFaceAdapter.getItemId(position));
				d.setBounds(0, 0, 35, 35);//设置表情图片的显示大小
				ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
				ss.setSpan(span, 0, view.getTag().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);				 
				//在光标所在处插入表情
				mContent.getText().insert(mContent.getSelectionStart(), ss);				
			}    		
    	});
    }
    
    private void showIMM() {
    	mFace.setTag(1);
    	showOrHideIMM();
    }
    private void showFace() {
		mFace.setImageResource(R.drawable.widget_bar_keyboard);
		mFace.setTag(1);
		mGridView.setVisibility(View.VISIBLE);
    }
    private void hideFace() {
    	mFace.setImageResource(R.drawable.widget_bar_face);
		mFace.setTag(null);
		mGridView.setVisibility(View.GONE);
    }
    private void showOrHideIMM() {
    	if(mFace.getTag() == null){
			//隐藏软键盘
			imm.hideSoftInputFromWindow(mContent.getWindowToken(), 0);
			//显示表情
			showFace();				
		}else{
			//显示软键盘
			imm.showSoftInput(mContent, 0);
			//隐藏表情
			hideFace();
		}
    }
    
    private View.OnClickListener faceClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			showOrHideIMM();
		}
	};
	// 显示录制操作选项
	private void showRecareder() {
		mForm.setVisibility(View.GONE);
		mFace.setVisibility(View.GONE);
		mAudio.setTag(1);
    	mAudio.setImageResource(R.drawable.widget_bar_keyboard);
    	mRecorder.setVisibility(View.VISIBLE);
    	mPick.setVisibility(View.GONE);
    	mAtme.setVisibility(View.GONE);
    	mSoftware.setVisibility(View.GONE);
    	mRecardStatus.setVisibility(View.VISIBLE);
    	mPublish.setVisibility(View.INVISIBLE);
	}
	// 隐藏录制操作选项
	private void hideRecareder() {
		mAudio.setTag(null);
		mForm.setVisibility(View.VISIBLE);
		mFace.setVisibility(View.VISIBLE);
    	mAudio.setImageResource(R.drawable.widget_bar_audio);
    	mRecorder.setVisibility(View.GONE);
    	mPick.setVisibility(View.VISIBLE);
    	mAtme.setVisibility(View.VISIBLE);
    	mSoftware.setVisibility(View.VISIBLE);
    	mRecardStatus.setVisibility(View.GONE);
    	mvTimeMess.setVisibility(View.INVISIBLE);
    	mPublish.setVisibility(View.VISIBLE);
	}
	
	// 显示录音操作
	private void showOrHideRecarder() {
		//隐藏软键盘
		imm.hideSoftInputFromWindow(mContent.getWindowToken(), 0);
		//隐藏表情
		hideFace();
		mvTooShort.setVisibility(View.GONE);
		if (mAudio.getTag() == null) {
			showRecareder();
		} else {
			hideRecareder();
		}
	}
	
	// 语音按钮监听事件
	private View.OnClickListener audioClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			// 用户如果没有登录
			if(!ac.isLogin()){
				UIHelper.showLoginDialog(TweetPub.this);
				return;
			}
			showOrHideRecarder();
		}
	};
	
	private void showRecarderStatus(byte type) {
		switch (type) {
		case RECORDER_STATE_RECARDING:
			mvAnimArea.setVisibility(View.VISIBLE);
			mvCancelArea.setVisibility(View.GONE);
			break;
		case RECORDER_STATE_CANALE:
			mvAnimArea.setVisibility(View.GONE);
			mvCancelArea.setVisibility(View.VISIBLE);
			break;
		case RECORDER_STATE_SHORT:
			mvFrame.setVisibility(View.GONE);
			mRecardStatusShow.setVisibility(View.VISIBLE);
    		mvTooShort.setVisibility(View.VISIBLE);
    		mHandler.postDelayed(new Runnable() {
				public void run() {
	        		mRecardStatusShow.setVisibility(View.GONE);
	        		mvTooShort.setVisibility(View.GONE);
	        		mvFrame.setVisibility(View.VISIBLE);
				}
			}, 1000);
			break;
		default:
			break;
		}
	}
	// 定时任务间隔时间
	private static final int POLL_INTERVAL = 300;

	// 录音音量状态展示
	private Runnable mPollTask = new Runnable() {
		public void run() {
			double amp = recordUtils.getAmplitude();
			updateDisplay(amp);
			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		}
	};
	// 录音计时器
	private Runnable mTimerTask = new Runnable() {
		int i = 0;
		public void run() {
			if (!isRecording) return;
			i++;
			if (i == RECORDER_TIME_MAXTIME) {
				IS_OVERTIME = true;
				i = 0;
				mvTimeMess.setVisibility(View.INVISIBLE);
				return;
			}
			if (i >= RECORDER_TIME_MAXTIME - 10) {
				if (i == RECORDER_TIME_MAXTIME - 10) {
					// 震动
					Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(300);
				}
				mvTimeMess.setVisibility(View.VISIBLE);
				mvTimeMess.setText("录音时间还剩下" + (RECORDER_TIME_MAXTIME - i) + "秒");
			}
			mHandler.postDelayed(mTimerTask, 1000);
		}
	};
	// 录音的路径
	private String savePath;
	// 语音动弹文件名
	private String fileName;
	// 是否正在录音中
	private boolean isRecording = false;
	// 是否超时
	private boolean IS_OVERTIME = false;
	// 开始录制语音动弹
	private void startRecorder(String name) {
		if (StringUtils.isEmpty(savePath))
			return;
		recordUtils.start(savePath, name);
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		mHandler.postDelayed(mTimerTask, 1000);
	}
	// 停止录音操作
	private void stopRecorder() {
		mRecardStatusShow.setVisibility(View.GONE);
		mHandler.removeCallbacks(mPollTask);
		mHandler.removeCallbacks(mTimerTask);
		recordUtils.stop();
		amrFile = new File(savePath, fileName);
		mvVolume.setImageResource(R.drawable.tweet_amp1);
	}
	
	// 删除录音文件
	private void deleteVoiceFile() {
		File newPath = new File(savePath + fileName);
		boolean res = newPath.delete();
		Log.i(TAG, res + "");
	}
	
	// 语音录制按钮触摸事件
	private View.OnTouchListener recorderTouchListener = new View.OnTouchListener() {
		long startVoiceT = 0;// 开始时间
		long endVoiceT = 0;// 结束世间
		int startY = 0;// 开始的Y
		byte state = RECORDER_STATE_RECARDING;
		public boolean onTouch(View v, MotionEvent event) {
			// 超时
			if (IS_OVERTIME) {
				mvTimeMess.setVisibility(View.INVISIBLE);
				stopRecorder();
				mRecorder.setBackgroundResource(R.color.weight_bar_buttonup);
				mRecorder.setText("按住  说话");
				// 状态为取消
	        	if (state == RECORDER_STATE_CANALE || state == TWEET_PUBING) {
	        		deleteVoiceFile();
	        		amrFile = null;
	        		if (state == RECORDER_STATE_CANALE)
	        			IS_OVERTIME = false;
	        		return false;
	        	}
				if (state != TWEET_PUBING) {
					state = TWEET_PUBING;
					pubTweet(TWEET_TYPE_VOICE, "#语音分享#");
				}
				return false;
			}
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isRecording = true;
				savePath = getRecorderPath();
				MediaPlayer md = MediaPlayer.create(TweetPub.this, R.raw.notificationsound);
				md.start();
				// 提示音播放完开始录音
				md.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					public void onCompletion(MediaPlayer mp) {
						startRecorder(fileName);
					}
				});
				IS_OVERTIME = false;
				mRecorder.setBackgroundResource(R.color.weight_bar_buttondown);
				mRecorder.setText("松开  结束");
				// 按下时记录录音文件名
				String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
				fileName = ac.getLoginUid() + "_" + timeStamp + ".amr";// 语音动弹命名
				startY = (int) event.getY();
				startVoiceT = System.currentTimeMillis();
				// 隐藏软键盘
				mRecardStatusShow.setVisibility(View.VISIBLE);
				break;
			case MotionEvent.ACTION_MOVE:
				int tempY = (int) event.getY();
				if (Math.abs(startY - tempY) > dm.heightPixels / 3) {
					// 取消
					state = RECORDER_STATE_CANALE;
					showRecarderStatus(RECORDER_STATE_CANALE);
				} else {
					// 录音
					state = RECORDER_STATE_RECARDING;
					showRecarderStatus(RECORDER_STATE_RECARDING);
				}
				break;
	        case MotionEvent.ACTION_UP:
	        	isRecording = false;
	        	mRecorder.setBackgroundResource(R.color.weight_bar_buttonup);
	        	mRecorder.setText("按住  说话");
	        	endVoiceT = System.currentTimeMillis();
	        	long voiceT = endVoiceT - startVoiceT;
	        	// 停止录音
	        	stopRecorder();
	        	// 录音小于最小时间
	        	if (voiceT < RECORDER_TIME_MINTIME * 1000 || state == RECORDER_STATE_CANALE) {
	        		deleteVoiceFile();
	        		amrFile = null;
	        		if (voiceT < RECORDER_TIME_MINTIME * 1000)
	        			showRecarderStatus(RECORDER_STATE_SHORT);
	        		if (state == RECORDER_STATE_CANALE)
	        			mvTimeMess.setVisibility(View.INVISIBLE);
	        		return false;
	        	}
	        	pubTweet(TWEET_TYPE_VOICE, "#语音分享#");
	        	break;
			default:
				break;
			}
			return false;
		}
	};
	
	
    
	private View.OnClickListener pickClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			//隐藏软键盘
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
			//隐藏表情
			hideFace();		
			CharSequence[] items = {
					TweetPub.this.getString(R.string.img_from_album),
					TweetPub.this.getString(R.string.img_from_camera)
			};
			imageChooseItem(items);
		}
	};
	
	private View.OnClickListener atmeClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			//显示软键盘
			showIMM();			
    		//在光标所在处插入“@用户名”
			int curTextLength = mContent.getText().length();
			if(curTextLength < MAX_TEXT_LENGTH) {
				String atme = TEXT_ATME;
				int start,end;
				if((MAX_TEXT_LENGTH - curTextLength) >= atme.length()) {
					start = mContent.getSelectionStart() + 1;
					end = start + atme.length() - 2;
				} else {
					int num = MAX_TEXT_LENGTH - curTextLength;
					if(num < atme.length()) {
						atme = atme.substring(0, num);
					}
					start = mContent.getSelectionStart() + 1;
					end = start + atme.length() - 1;
				}
				if(start > MAX_TEXT_LENGTH || end > MAX_TEXT_LENGTH) {
					start = MAX_TEXT_LENGTH;
					end = MAX_TEXT_LENGTH;
				}
				mContent.getText().insert(mContent.getSelectionStart(), atme);
				mContent.setSelection(start, end);//设置选中文字
			}
		}
	};
	
	private View.OnClickListener softwareClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			//显示软键盘
			showIMM();
			
			//在光标所在处插入“#软件名#”
			int curTextLength = mContent.getText().length();
			if(curTextLength < MAX_TEXT_LENGTH) {
				String software = TEXT_SOFTWARE;
				int start,end;
				if((MAX_TEXT_LENGTH - curTextLength) >= software.length()) {
					start = mContent.getSelectionStart() + 1;
					end = start + software.length() - 2;
				} else {
					int num = MAX_TEXT_LENGTH - curTextLength;
					if(num < software.length()) {
						software = software.substring(0, num);
					}					
					start = mContent.getSelectionStart() + 1;
					end = start + software.length() - 1;
				}
				if(start > MAX_TEXT_LENGTH || end > MAX_TEXT_LENGTH) {
					start = MAX_TEXT_LENGTH;
					end = MAX_TEXT_LENGTH;
				}
				mContent.getText().insert(mContent.getSelectionStart(), software);
	    		mContent.setSelection(start, end);//设置选中文字
			}
		}
	};
	
	private View.OnClickListener clearwordsClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			String content = mContent.getText().toString();
			if(!StringUtils.isEmpty(content)){
				UIHelper.showClearWordsDialog(v.getContext(), mContent, mNumberwords);
			}
		}
	};
	
	private View.OnLongClickListener imageLongClickListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			//隐藏软键盘
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
			
			new AlertDialog.Builder(v.getContext())
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(getString(R.string.delete_image))
			.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//清除之前保存的编辑图片
					((AppContext)getApplication()).removeProperty(tempTweetImageKey);
					imgFile = null;
					mImage.setVisibility(View.GONE);
					dialog.dismiss();
				}
			})
			.setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create().show();
			return true;
		}
		
	};
	
	/**
	 * 获得语音动弹保存的路径
	 * @return
	 */
	private String getRecorderPath() {
		return FileUtils.getAppCache(TweetPub.this, "voiceTweet");
	}
	
	/**
	 * 操作选择
	 * @param items
	 */
	public void imageChooseItem(CharSequence[] items )
	{
		AlertDialog imageDialog = new AlertDialog.Builder(this).setTitle(R.string.ui_insert_image).setIcon(android.R.drawable.btn_star).setItems(items,
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int item)
				{
					//手机选图
					if( item == 0 )
					{
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
						intent.addCategory(Intent.CATEGORY_OPENABLE); 
						intent.setType("image/*"); 
						startActivityForResult(Intent.createChooser(intent, "选择图片"),ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD); 
					}
					//拍照
					else if( item == 1 )
					{	
						String savePath = "";
						//判断是否挂载了SD卡
						String storageState = Environment.getExternalStorageState();		
						if(storageState.equals(Environment.MEDIA_MOUNTED)){
							savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OSChina/Camera/";//存放照片的文件夹
							File savedir = new File(savePath);
							if (!savedir.exists()) {
								savedir.mkdirs();
							}
						}
						
						//没有挂载SD卡，无法保存文件
						if(StringUtils.isEmpty(savePath)){
							UIHelper.ToastMessage(TweetPub.this, "无法保存照片，请检查SD卡是否挂载");
							return;
						}

						String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
						String fileName = "osc_" + timeStamp + ".jpg";//照片命名
						File out = new File(savePath, fileName);
						Uri uri = Uri.fromFile(out);
						
						theLarge = savePath + fileName;//该照片的绝对路径
						
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
						startActivityForResult(intent, ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA);
					}   
				}}).create();
		
		 imageDialog.show();
	}
	
	@Override 
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{ 
    	if(resultCode != RESULT_OK) return;
		
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.what == 1 && msg.obj != null){
					//显示图片
					mImage.setImageBitmap((Bitmap)msg.obj);
					mImage.setVisibility(View.VISIBLE);
				}
			}
		};
		
		new Thread(){
			public void run() 
			{
				Bitmap bitmap = null;
				
		        if(requestCode == ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD) 
		        {         	
		        	if(data == null)  return;
		        	
		        	Uri thisUri = data.getData();
		        	String thePath = ImageUtils.getAbsolutePathFromNoStandardUri(thisUri);
		        	
		        	//如果是标准Uri
		        	if(StringUtils.isEmpty(thePath))
		        	{
		        		theLarge = ImageUtils.getAbsoluteImagePath(TweetPub.this,thisUri);
		        	}
		        	else
		        	{
		        		theLarge = thePath;
		        	}
		        	
		        	String attFormat = FileUtils.getFileFormat(theLarge);
		        	if(!"photo".equals(MediaUtils.getContentType(attFormat)))
		        	{
		        		Toast.makeText(TweetPub.this, getString(R.string.choose_image), Toast.LENGTH_SHORT).show();
		        		return;
		        	}
		        	
		        	//获取图片缩略图 只有Android2.1以上版本支持
		    		if(AppContext.isMethodsCompat(android.os.Build.VERSION_CODES.ECLAIR_MR1)){
		    			String imgName = FileUtils.getFileName(theLarge);
		    			bitmap = ImageUtils.loadImgThumbnail(TweetPub.this, imgName, MediaStore.Images.Thumbnails.MICRO_KIND);
		    		}
		        	
		        	if(bitmap == null && !StringUtils.isEmpty(theLarge))
		        	{
		        		bitmap = ImageUtils.loadImgThumbnail(theLarge, 100, 100);
		        	}
		        }
		        //拍摄图片
		        else if(requestCode == ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA)
		        {	
		        	if(bitmap == null && !StringUtils.isEmpty(theLarge))
		        	{
		        		bitmap = ImageUtils.loadImgThumbnail(theLarge, 100, 100);
		        	}
		        }
		        
				if(bitmap!=null)
				{
					//存放照片的文件夹
					String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OSChina/Camera/";
					File savedir = new File(savePath);
					if (!savedir.exists()) {
						savedir.mkdirs();
					}
					
					String largeFileName = FileUtils.getFileName(theLarge);
					String largeFilePath = savePath + largeFileName;
					//判断是否已存在缩略图
					if(largeFileName.startsWith("thumb_") && new File(largeFilePath).exists()) 
					{
						theThumbnail = largeFilePath;
						imgFile = new File(theThumbnail);
					} 
					else 
					{
						//生成上传的800宽度图片
						String thumbFileName = "thumb_" + largeFileName;
						theThumbnail = savePath + thumbFileName;
						if(new File(theThumbnail).exists())
						{
							imgFile = new File(theThumbnail);
						}
						else
						{
							try {
								//压缩上传的图片
								ImageUtils.createImageThumbnail(TweetPub.this, theLarge, theThumbnail, 800, 80);
								imgFile = new File(theThumbnail);
							} catch (IOException e) {
								e.printStackTrace();
							}	
						}						
					}					
					//保存动弹临时图片
					((AppContext)getApplication()).setProperty(tempTweetImageKey, theThumbnail);
					
					Message msg = new Message();
					msg.what = 1;
					msg.obj = bitmap;
					handler.sendMessage(msg);
				}				
			};
		}.start();
    }
	
	/**
	 * 发布动弹
	 * @param tweetType 动弹类型
	 * @param content 文本动弹的内容
	 */
	private void pubTweet(final byte tweetType, String content) {
		IS_OVERTIME = false;
		if(!ac.isLogin()){
			UIHelper.showLoginDialog(TweetPub.this);
			return;
		}	
		
		mMessage.setVisibility(View.VISIBLE);
		mForm.setVisibility(View.GONE);
		
		tweet = new Tweet();
		tweet.setAuthorId(ac.getLoginUid());
		tweet.setBody(content);
		if (tweetType == TWEET_TYPE_CONTENT) {
			tweet.setImageFile(imgFile);
		}
		if (tweetType == TWEET_TYPE_VOICE) {
			// 设置语音动弹的文件
			tweet.setAmrFile(amrFile);
			mvTimeMess.setVisibility(View.INVISIBLE);
		}
		
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				if(msg.what == 1){
					//清除之前保存的编辑内容
					ac.removeProperty(tempTweetKey,tempTweetImageKey);
					finish();
				}else{
					mMessage.setVisibility(View.GONE);
					if (tweetType == TWEET_TYPE_CONTENT) 
						mForm.setVisibility(View.VISIBLE);
					else
						showOrHideRecarder();
				}
			}				
		};
		
		new Thread(){
			public void run() {
				Message msg =new Message();
				Result res = null;
				int what = 0;
				try {
					res = ac.pubTweet(tweet);
					what = 1;
					msg.what = 1;
					msg.obj = res;
	            } catch (AppException e) {
	            	e.printStackTrace();
					msg.what = -1;
					msg.obj = e;
	            }
				handler.sendMessage(msg);
				UIHelper.sendBroadCastTweet(TweetPub.this, what, res, tweet);
			}
		}.start();
	}
	
	private View.OnClickListener publishClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			//隐藏软键盘
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
			
			String content = mContent.getText().toString();
			if(StringUtils.isEmpty(content)){
				UIHelper.ToastMessage(v.getContext(), "请输入动弹内容");
				return;
			}
			// 调用动弹发布方法
			pubTweet(TWEET_TYPE_CONTENT, content);
		}
	};
	
	// 根据录制音量的大小定时更新状态图片
	private void updateDisplay(double signalEMA) {
		
		switch ((int) signalEMA) {
		case 0:
		case 1:
			mvVolume.setImageResource(R.drawable.tweet_amp1);
			break;
		case 2:
		case 3:
			mvVolume.setImageResource(R.drawable.tweet_amp2);
			break;
		case 4:
		case 5:
			mvVolume.setImageResource(R.drawable.tweet_amp3);
			break;
		case 6:
		case 7:
			mvVolume.setImageResource(R.drawable.tweet_amp4);
			break;
		case 8:
		case 9:
			mvVolume.setImageResource(R.drawable.tweet_amp5);
			break;
		case 10:
		case 11:
			mvVolume.setImageResource(R.drawable.tweet_amp6);
			break;
		default:
			mvVolume.setImageResource(R.drawable.tweet_amp7);
			break;
		}
	}
}
