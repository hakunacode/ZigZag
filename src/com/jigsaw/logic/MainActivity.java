package com.jigsaw.logic;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

import br.com.dina.ui.widget.UITableView;
import br.com.dina.ui.widget.UITableView.ClickListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
    
	UITableView tableView;
    public static final int INTERNAL_BMP = 0;
    private static final int EXTERN_BMP = -1;
    
    private void clearSaveData() {
    	SharedPreferences settings  = getApplicationContext().getSharedPreferences("jigsaw", 0);
    	boolean bIsInit = settings.getBoolean("isInit", false);
    	if (bIsInit) {
    		return;
    	}
    	
    	SharedPreferences.Editor editor = settings.edit();
    	int nCount = 7;
    	editor.putInt("count", nCount);
    	editor.putBoolean("unlocked0", true);
    	for (int i = 1; i < nCount; i ++) {
    		editor.putBoolean("unlocked"+i, false);
    	}
    	editor.commit();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);       
        clearSaveData();
        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle("Home");
        actionBar.addAction(new DeviceAction());
        
        tableView = (UITableView) findViewById(R.id.tableView);        
        createList();        
        Log.d("MainActivity", "total items: " + tableView.getCount());
    }
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d("tag", "OnResume");
    	tableView.clear();
    	reloadData();
    	tableView.commit();
    }
    String mTitleAry[] = {
    		"Jigsaw Example1",
    		"Jigsaw Example2",
    		"Jigsaw Example3",
    		"Jigsaw Example4",
    		"Jigsaw Example5",
    		"Jigsaw Example6",
    		"Jigsaw Example7",    		
    };
    private void createList() {
    	CustomClickListener listener = new CustomClickListener();
    	tableView.setClickListener(listener);   
    	reloadData();
    	tableView.commit();
    }
    private static int mRecordCount;
    public static int getRecordCount() {
    	return mRecordCount;
    }
    public void reloadData() {
    	SharedPreferences settings  = getApplicationContext().getSharedPreferences("jigsaw", 0);
    	mRecordCount = settings.getInt("count", 7);
    	for (int i = 0; i < mRecordCount; i ++) {
    		boolean bUnLocked = settings.getBoolean("unlocked"+i, true);
    		int nDrawID = R.drawable.i0+i;
    		if (!bUnLocked)
    			nDrawID = R.drawable.none;
    		tableView.addBasicItem(nDrawID, mTitleAry[i], "with images");
    	}
    }
    
    private class CustomClickListener implements ClickListener {

		@Override
		public void onClick(int index) {
			SharedPreferences settings  = getApplicationContext().getSharedPreferences("jigsaw", 0);
			boolean bUnLocked = settings.getBoolean("unlocked"+index, true);
			if (!bUnLocked)
				return;
			
			Intent i = new Intent(MainActivity.this, JigsawActivity.class);
			i.putExtra("title", mTitleAry[index]);
			i.putExtra("SourceBitmapIndex", index);
			i.putExtra("SourceBitmapType", INTERNAL_BMP);
			i.putExtra("SourceBitmap", (index)+".png");
			startActivity(i);
		}    	
    }        
    @Override
    protected Dialog onCreateDialog(int id) {
        return new AlertDialog.Builder(this)
        .setTitle(R.string.select_device_dialog)
        .setItems(R.array.select_device_dialog_items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	if (which == 2) {
            		return;
            	} else if (which == 1) {
            		openCameraIntent();
            	} else if (which == 0) {
            		openGalleryIntent();
            	}
            }
        })
        .create();
    }
    //Camera controller
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri fileUri;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		if (resultCode != RESULT_OK) {
			return;
		}
		
		switch (requestCode) {
			case 1: {
				Intent i = new Intent(MainActivity.this, JigsawActivity.class);		
				i.putExtra("title", "Gallery");
				i.putExtra("SourceBitmapType", EXTERN_BMP);
				Uri uri = data.getData();				
				i.putExtra("SourceBitmap", uri.toString());
				
				startActivity(i);					
				break;
			} //from gallery
	
			case 100: {
				Intent i = new Intent(MainActivity.this, JigsawActivity.class);
				i.putExtra("title", "Camera");
				i.putExtra("SourceBitmapType", EXTERN_BMP);
				Uri uri = fileUri;
				i.putExtra("SourceBitmap", uri.toString());
				startActivity(i);
				break;
			} // from camera
		}
	}
    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    public Uri getOutputMediaFileUri() {
		return fileUri;
    	
    }
    private void openGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }
    
    private void openCameraIntent() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);        

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
        Log.d("tag", fileUri.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        // start the image capture Intent
        startActivityForResult(intent, 100);
    }
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
          return Uri.fromFile(getOutputMediaFile(type));
    }
    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES), "JIGSAW");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("JIGSAW", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
    private class DeviceAction extends AbstractAction {
        public DeviceAction() {
            super(R.drawable.ic_device);
        }

        @Override
        public void performAction(View view) {
        	showDialog(0);                 
        }
    }
}