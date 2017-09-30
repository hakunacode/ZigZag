package com.jigsaw.logic;

import com.jigsaw.logic.JigsawView.JigsawThread;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class JigsawActivity extends Activity {
	private JigsawView mJigsawView;
	private JigsawThread mJigsawThread;
	private Context mContext;
	private int mBitmapIndex;
	private int mType;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jigsaw);

        Bundle b = this.getIntent().getExtras();
        mType = b.getInt("SourceBitmapType");
        mBitmapIndex = b.getInt("SourceBitmapIndex");
        String fileName = b.getString("SourceBitmap");
        String strTitle = b.getString("title");

        final ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle(strTitle);
        actionBar.addAction(new PeekAction());
        actionBar.addAction(new PauseAction());

        
        JigsawView v = (JigsawView)findViewById(R.id.jigsawView);//new JigsawView(this, );
        v.setStartArgs(fileName, mType);
        mJigsawThread = v.getThread();        
        mJigsawView = v;
        mContext = this;        
    }
    public void setUnlockLogic() {
    	if (mType != MainActivity.INTERNAL_BMP)
    		return;
    	SharedPreferences settings  = getApplicationContext().getSharedPreferences("jigsaw", 0);
    	int nRecordCount = settings.getInt("count", 7);
    	
    	mBitmapIndex ++;
    	if (mBitmapIndex >= nRecordCount)
    		return;
    	
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putBoolean("unlocked"+mBitmapIndex, true);
    	editor.commit();
    }
    @Override
    public void onStart() {
    	super.onStart();
        	showDialog(LEVEL_DIALOG_LIST);
    }
    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mJigsawView.getThread().pause(); // pause game when Activity pauses
    }    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mJigsawThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }    
    
    public static final int DIALOG_PAUSE_YES_NO_MESSAGE = 1;
    public static final int LEVEL_DIALOG_LIST = 2;
    public static final int WIN_MESSAGE = 3;	
    @Override
    public void onBackPressed() {
    	showDialog(DIALOG_PAUSE_YES_NO_MESSAGE);
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PAUSE_YES_NO_MESSAGE:
            return new AlertDialog.Builder(this)
                .setTitle("Are you sure quit this game?")
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	((Activity)mContext).finish();
                    }
                })
                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .setCancelable(false)
                .create();     
            
        case LEVEL_DIALOG_LIST:
            return new AlertDialog.Builder(this)
                .setTitle("Please select level.")
                .setItems(R.array.select_level_dialog_items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	mJigsawView.setLevel(which);
                    }
                })
                .setCancelable(false)
                .create();                  
        case WIN_MESSAGE: 
            return new AlertDialog.Builder(this)
            .setTitle("Congratulations!\nYou can play next level.")
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	((Activity)mContext).finish();
                }
            })
            .setCancelable(false)
            .create();     
        }
        return null;
    }
    
    private class PauseAction extends AbstractAction {
        public PauseAction() {
            super(R.drawable.ic_pause);
        }

        @Override
        public void performAction(View view) {
        	showDialog(DIALOG_PAUSE_YES_NO_MESSAGE);                 
        }
    }
    private class PeekAction extends AbstractAction {
        public PeekAction() {
            super(R.drawable.ic_peek);
        }

        @Override
        public void performAction(View view) {
        	mJigsawView.setPeekState();               
        }
    }
}