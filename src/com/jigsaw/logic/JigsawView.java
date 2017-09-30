package com.jigsaw.logic;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class JigsawView extends SurfaceView implements SurfaceHolder.Callback { 
    /*
     * State-tracking constants
     */
    public static final int STATE_LOSE = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_READY = 3;        
    public static final int STATE_RUNNING = 4;
    public static final int STATE_WIN = 5;		
    public static final int STATE_NONE = 6;
    public static final int STATE_PEEK = 6;
    public boolean mbStarted = false;

    class JigsawThread extends Thread {
        
        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;		
        
        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;
        private Bitmap mBackgroundImage;
        private Bitmap mSourceImage;

        public JigsawThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;    
            
            Resources res = context.getResources();
            mBackgroundImage = BitmapFactory.decodeResource(res,
                    R.drawable.blank_bg);
//            mSourceImage = b

        }
        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {            	
            }
        }
        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
            }
        }        

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
            }
        }
        @Override
        public void run() {
            while (mRun) {         
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        doDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {            	
            }
			return map;
        }
        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }        
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                mMode = mode;
            }
        }        
        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
            }
        }        
        /**
         * Draws the ship, fuel/speed bars, and background to the provided
         * Canvas.
         */
        private final long MAX_WAIT_TIME = 2000;
        private void doDraw(Canvas canvas) {
        	if (canvas == null)
        		return;
        	if ( (System.currentTimeMillis()-mBeginTime) > MAX_WAIT_TIME && mMode == STATE_READY) {
        		restartGame();
        	}
        		
        	
        	canvas.drawBitmap(mBackgroundImage, new Rect(0, 0, 640, 960), new Rect(0, 0, mCanvasWidth, mCanvasHeight), null);
        	
        	if (mMode == STATE_READY) {
            	long nDiffTime = MAX_WAIT_TIME-(System.currentTimeMillis()-mBeginTime);
            	if (nDiffTime < 0)
            		nDiffTime = 0;
            	String strMiliSec = String.valueOf((int)((nDiffTime%1000)/10));
            	if (strMiliSec.length() == 1)
            		strMiliSec = "0"+strMiliSec;
            	drawString(canvas, String.format("0%d:%s", (int)(nDiffTime/1000), strMiliSec));

            	for (int k = 0; k < mTileCount; k ++) {
	        		drawPeekTile(canvas, k);
	        	}
        	}
        	if (mMode == STATE_RUNNING || mMode == STATE_WIN) {
        		float fOffsetX = 0;
        		float fOffsetY = 0;
	        	for (int k = 0; k < mTileCount; k ++) {
        			fOffsetX = 0;
        			fOffsetY = 0;
	        		if (mScrollDirection != SCROLL_NONE) {
	        			if (IsContainTileOfDirection(k, mScrollDirection)) {
	        				if (mScrollDirection < getTileX())
	        					fOffsetX = mfGestureOffsetX;
	        				else 
	        					fOffsetY = mfGestureOffsetY;
	        			}
	        		}
	        			
	        		drawTile(canvas, k, fOffsetX, fOffsetY);	        		
	        	}
        	}
        }
	}
    protected boolean IsContainTileOfDirection(int nTileIndex, int nScrollDirection) {
    	if (nScrollDirection < getTileX()) { //Is Horizen Direction
    		if (nScrollDirection == getRow(nTileIndex))
    			return true;    		
    	} else { //Is Vertical Direction
    		if (nScrollDirection == (getCol(nTileIndex)+getTileX()))
    			return true;    		
    	}
    	return false;
    }
	private void drawPeekTile(Canvas canvas, int nIndex) {
		if (mSrcBitmap == null)
			return;
		
    	int nTileX = (int)(Math.sqrt(mTileCount));
    	int nBitmapW = mSrcBitmap.getWidth();
    	int nBitmapH = mSrcBitmap.getHeight();
    	float fScale = mCanvasWidth/((float)nBitmapW)*0.9f;
    	int nLeftGap = (int)((mCanvasWidth - fScale*nBitmapW)/2);
    	int nTopGap = (int)((mCanvasHeight - fScale*nBitmapH)/2);        	
		int nW1 = (int)(nBitmapW/((float)nTileX));
		int nH1 = (int)(nBitmapH/((float)nTileX));
		int i = nIndex / nTileX;
		int j = nIndex % nTileX;
		int nRow = nIndex%nTileX;
		int nCol = nIndex/nTileX;
		int nLeft1 = (int)(nBitmapW*nRow/((float)nTileX));
		int nTop1 = (int)(nBitmapH*nCol/(float)nTileX);
		
		int nW2 = (int)(nW1*fScale);
		int nH2 = (int)(nH1*fScale);
		int nLeft2 = nW2*j + nLeftGap;
		int nTop2 = nH2*i + nTopGap;

		canvas.drawBitmap(mSrcBitmap, 
    			new Rect(nLeft1, nTop1, nLeft1+nW1, nTop1+nH1), 
    			new Rect(nLeft2, nTop2, nLeft2+nW2-1, nTop2+nH2-1), 
    			null);        			
	}
	private void drawTile(Canvas canvas, int nIndex, float fOffsetX, float fOffsetY) {
		if (mSrcBitmap == null)
			return;
		
    	int nTileX = (int)(Math.sqrt(mTileCount));
    	int nBitmapW = mSrcBitmap.getWidth();
    	int nBitmapH = mSrcBitmap.getHeight();
    	float fScale = mCanvasWidth/((float)nBitmapW)*0.9f;
    	int nWindowLeft = (int)((mCanvasWidth - fScale*nBitmapW)/2);
    	int nWindowTop = (int)((mCanvasHeight - fScale*nBitmapH)/2);  
    	int nWindowRight = (int)((mCanvasWidth + fScale*nBitmapW)/2);
    	int nWindowBottom = (int)((mCanvasHeight + fScale*nBitmapH)/2);
    	
		int nW1 = (int)(nBitmapW/((float)nTileX));
		int nH1 = (int)(nBitmapH/((float)nTileX));
		int i = nIndex / nTileX;
		int j = nIndex % nTileX;
		
		if (mAryTileIndex[i*nTileX+j] == -1)
			return;
		int nRow = mAryTileIndex[i*nTileX+j]%nTileX;
		int nCol = mAryTileIndex[i*nTileX+j]/nTileX;
		int nLeft1 = (int)(nBitmapW*nRow/((float)nTileX));
		int nTop1 = (int)(nBitmapH*nCol/(float)nTileX);
		int nRight1 = nLeft1+nW1;
		int nBottom1 = nTop1+nH1;
		

		int nW2 = (int)(nW1*fScale);
		int nH2 = (int)(nH1*fScale);
		
		int nScaleBitmapWidth = (int)(fScale*nBitmapW);
		int nScaleBitmapHeight = (int)(fScale*nBitmapH);
		
		int nLeft2 = (int)(fOffsetX+nW2*j)%(nScaleBitmapWidth) + nWindowLeft;
		int nTop2 = (int)(fOffsetY+nH2*i)%nScaleBitmapHeight + nWindowTop;
		int nRight2 = nLeft2+nW2-1;
		int nBottom2 = nTop2+nH2-1;
		
		boolean bCliped = false;
		int nLeft3, nTop3, nBottom3, nRight3;
		int nLeft4, nTop4, nBottom4, nRight4;
		nLeft3 = nLeft1;
		nRight3 = nRight1;
		nTop3 = nTop1;
		nBottom3 = nBottom1;

		nLeft4 = nLeft2;
		nRight4 = nRight2;
		nTop4 = nTop2;
		nBottom4 = nBottom2;

		int nRightGap = nRight2-nWindowRight;
		int nLeftGap = nWindowLeft-nLeft2;
		int nTopGap = nWindowTop-nTop2;
		int nBottomGap = nBottom2-nWindowBottom;
		if (nRightGap > 0) {
			nRight1 -= (nRightGap)/fScale;
			nRight2 -= (nRightGap);
			nLeft3 = nRight1;
			nLeft4 = nWindowLeft;
			nRight4 = nWindowLeft+nRightGap;
			bCliped = true;
		} else if (nLeftGap > 0){
			nLeft1 += (nLeftGap)/fScale;
			nLeft2 += (nLeftGap);
			nRight3 = nLeft1;
			nLeft4 = nWindowRight-nLeftGap;
			nRight4 = nWindowRight;
			bCliped = true;
		} else if (nTopGap > 0) {
			nTop1 += nTopGap/fScale;
			nTop2 += nTopGap;
			nBottom3 = nTop1;
			nTop4 = nWindowBottom-nTopGap;
			nBottom4 = nWindowBottom;
			bCliped = true;
		} else if (nBottomGap > 0) {
			nBottom1 -= (nBottomGap)/fScale;
			nBottom2 -= (nBottomGap);
			nTop3 = nBottom1;
			nTop4 = nWindowTop;
			nBottom4 = nWindowTop+nBottomGap;
			bCliped = true;
		}
		
		canvas.drawBitmap(mSrcBitmap, 
    			new Rect(nLeft1, nTop1, nRight1, nBottom1), 
    			new Rect(nLeft2, nTop2, nRight2, nBottom2), 
    			null);     
		drawRectString(canvas, "1", nLeft2, nTop2);
		if (bCliped == true) {
			canvas.drawBitmap(mSrcBitmap, 
	    			new Rect(nLeft3, nTop3, nRight3, nBottom3), 
	    			new Rect(nLeft4, nTop4, nRight4, nBottom4), 
	    			null);      
		}
	}
	private void drawString(Canvas canvas, String strText) {
		if (mSrcBitmap == null)
			return;
		
		int nBitmapW = mSrcBitmap.getWidth();
    	int nBitmapH = mSrcBitmap.getHeight();
    	float fScale = mCanvasWidth/((float)nBitmapW)*0.9f;
    	int nTopGap = (int)((mCanvasHeight - fScale*nBitmapH)/2);    
    	
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);	
		//paint.setColor(0x00FFFF);
		paint.setTextSize(100*fScale);
		canvas.drawText(strText, mCanvasWidth/2, nTopGap/2, paint);
	}
	private void drawRectString(Canvas canvas, String strText, float x, float y) {
//		Paint paint = new Paint();		
//		paint.setColor(0xffffffff);		
//		canvas.drawRect(x, y, x+30, y+30, paint);
	}
	
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	//
	//JigsawView Logic
	//
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	protected final int MAX_TILE_COUNT = 60;
    /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
    protected int mMode = STATE_NONE;
    
    /** Handle to the application context, used to e.g. fetch Drawables. */
    protected Context mContext;
    /** The thread that actually draws the animation */
    private JigsawThread thread;    
	public static final long FPS_MS = 1000/2;
	protected int mGameType;
	protected String mSourceFileName;
	protected int mLevel;	
	protected Bitmap mSrcBitmap;
	protected int mAryTileIndex[] = new int[MAX_TILE_COUNT];
	protected int mTileCount = 4;
    protected int mCanvasHeight = 1;
    protected int mCanvasWidth = 1;

    public JigsawView(Context context, AttributeSet attr) {
    	super(context, attr);
        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);    
        thread = new JigsawThread(holder, context, new Handler());
    }
    public JigsawView(Context context) {
		super(context);
        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);    
        thread = new JigsawThread(holder, context, new Handler());
	}
	public void setStartArgs(String sourceFile, int nType) {        
        mSourceFileName = sourceFile;
        mGameType = nType;
        if (nType == MainActivity.INTERNAL_BMP)
        	mSrcBitmap = createBitmapFromAssert(sourceFile);
        else
        	mSrcBitmap = createBitmapFromExternPath(sourceFile);
        	
        for (int i = 0; i < MAX_TILE_COUNT; i ++) {
        	mAryTileIndex[i] = i;
        }
	}	
	private Bitmap createBitmapFromAssert(String pathImage) {
		InputStream is = null;
		try {
			is = this.getResources().getAssets().open(pathImage);
		} catch(IOException e) {			
		}
		
		return BitmapFactory.decodeStream(is);		
	}	
	private Bitmap createBitmapFromExternPath(String pathImage) {
		InputStream is = null;
		try {
			Uri uri = Uri.parse(pathImage);
			return BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri));		
			
		} catch(IOException e) {			
		}
		return null;
	}	
    public JigsawThread getThread() {
        return thread;
    }	
	public void setType(int nType) {
		mGameType = nType;
	}
	public void setSourceFileName(String fileName) {
		mSourceFileName = fileName;
	}
	private long mBeginTime;
	public void setLevel(int nLevel) {
		mLevel = nLevel;
		mBeginTime = System.currentTimeMillis();
		switch(mLevel) {
		case 0:
			mTileCount = 3*3;
			break;
		case 1:
			mTileCount = 4*4;
			break;
		case 2:
			mTileCount = 5*5;
			break;
		}
		mMode = STATE_READY;
	}
	public void restartGame() {
		if (!mbStarted)
			relayoutTiles();
		mbStarted = true;
		mMode = STATE_RUNNING;
	}
	protected int getIndexFromTouchPoint(float x, float y) {
		if (mSrcBitmap == null)
			return -1;
		
    	int nTileX = (int)(Math.sqrt(mTileCount));
    	int nBitmapW = mSrcBitmap.getWidth();
    	int nBitmapH = mSrcBitmap.getHeight();
    	
    	float fScale = mCanvasWidth/((float)nBitmapW)*0.9f;
		int nW1 = (int)(nBitmapW/((float)nTileX));
		int nH1 = (int)(nBitmapH/((float)nTileX));	
    	int nLeftGap = (int)((mCanvasWidth - fScale*nBitmapW)/2);
    	int nTopGap = (int)((mCanvasHeight - fScale*nBitmapH)/2);    
    	int nRightGap = nLeftGap + nW1*nTileX;
    	int nBottomGap = nTopGap + nH1*nTileX;
		int nW2 = (int)(nW1*fScale);
		int nH2 = (int)(nH1*fScale);
		
		if (!(x >= nLeftGap && y >= nTopGap &&
			x <= nRightGap && y <= nBottomGap)) {
			return -1;
		}

		int nX = (int)((x - nLeftGap)/nW2);
		int nY = (int)((y - nTopGap)/nH2);
		int nTileSQRCount = (int)(Math.sqrt(mTileCount)); 
		if (nX >= nTileSQRCount || nY >= nTileSQRCount)
			return -1;
		
		return nY*nTileX + nX;
	}
	protected int getCol(int nTileIndex) {
		int nTileX = (int)(Math.sqrt(mTileCount));
		return (nTileIndex%nTileX);
	}
	protected int getRow(int nTileIndex) {
		int nTileX = (int)(Math.sqrt(mTileCount));
		return ((int)(nTileIndex/nTileX));
	}
	protected int getTileX() {
		return (int)(Math.sqrt(mTileCount));
	}
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (mMode == STATE_READY || mMode == STATE_PEEK)
    		return true;
    	
        float x = event.getX();
        float y = event.getY();
        Log.d("tag", "x = "+x+", y= " + y);

        mCurrentTileX = x;
    	mCurrentTileY = y;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	onTouchBegin(x, y);                
                break;
            case MotionEvent.ACTION_MOVE:
            	onTouchMove(x, y);
            	break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            	onTouchEnd(x, y);
            	break;
        }
        return true;
    }  
    int mCurrentTileIndex = -1;
    float mCurrentTileX = 0;
    float mCurrentTileY = 0;
    final int NO_POS = -100;
    private int getTile(int nTileIndex) {
    	if (nTileIndex < 0 || nTileIndex >= mTileCount)
    		return NO_POS;
    	return mAryTileIndex[nTileIndex];
    }
    private void exchangeTile(int nTile1, int nTile2) {
    	int nTemp = mAryTileIndex[nTile1];
    	mAryTileIndex[nTile1] = mAryTileIndex[nTile2];
    	mAryTileIndex[nTile2] = nTemp;
    }
    final int MAX_MIX_COUNT = 120;
	private void relayoutTiles() {
        for (int i = 0; i < MAX_TILE_COUNT; i ++) {
        	mAryTileIndex[i] = -1;
        }
        for (int i = 0; i < mTileCount; i ++) {
        	mAryTileIndex[i] = i;
        }

        for (int i = 0; i < mTileCount; i ++) {
        	int nNextIndex = ((int)(Math.random()*1000))%mTileCount;
        	int nTemp = mAryTileIndex[i];
        	mAryTileIndex[i] = mAryTileIndex[nNextIndex];
        	mAryTileIndex[nNextIndex] = nTemp;
        }
	}
	private void doScrollX(int nOffset) {
	}
	private void doScrollY(int nOffset) {
	}
	
	//タッチデータを定義する。
	private float mTouchBeginX;
	private float mTouchBeginY;	
	protected float mfGestureOffsetX;
	protected float mfGestureOffsetY;
	
	protected final int SCROLL_NONE = -1;
	protected int mScrollDirection = SCROLL_NONE;
	
    private void onTouchBegin(float x, float y) {    	
    	mTouchBeginX = x;
    	mTouchBeginY = y;
    }
    private void onTouchMove(float x, float y) {
    	int nTileIndex = getIndexFromTouchPoint(mTouchBeginX, mTouchBeginY);
    	mfGestureOffsetX = x - mTouchBeginX;
    	mfGestureOffsetY = y - mTouchBeginY;
    	
    	if (Math.abs(mfGestureOffsetX) > Math.abs(mfGestureOffsetY)) {
    		mScrollDirection = getRow(nTileIndex);
    	} else {
    		mScrollDirection = getCol(nTileIndex)+getTileX();    		
    	}
    }
    private void onTouchEnd(float x, float y) {
    	if (mScrollDirection != SCROLL_NONE) {
    		int nTileX = getTileX();
    		int nIndex = 0;
    		int nAryTile[] = new int[nTileX];
    		for (int k = 0; k < mTileCount; k ++) {
				if (IsContainTileOfDirection(k, mScrollDirection)) {
					nAryTile[nIndex] = k;
					nIndex ++;
				}    		
    		}
    		
        	int nBitmapW = mSrcBitmap.getWidth();
        	int nBitmapH = mSrcBitmap.getHeight();
    		float fScale = mCanvasWidth/((float)nBitmapW)*0.9f;
    		int nW1 = (int)(nBitmapW/((float)nTileX));
    		int nH1 = (int)(nBitmapH/((float)nTileX));
    		int nW2 = (int)(nW1*fScale);
    		int nH2 = (int)(nH1*fScale);
    		
    		int nLoopCount = 0;
    		boolean bIsLeftWay = false;
        	if (mScrollDirection < getTileX()) { //Is Horizen Direction
        		nLoopCount = (int)(Math.ceil(Math.abs(mfGestureOffsetX)/((float)nW2)));
        		if (mfGestureOffsetX < 0) {
        			bIsLeftWay = true;
        		}
        	} else { //Is Vertical Direction
        		nLoopCount = (int)(Math.ceil(Math.abs(mfGestureOffsetY)/((float)nH2)));
        		if (mfGestureOffsetY < 0) {
        			bIsLeftWay = true;
        		}
        	}
        	
        	for (int i = 0; i < nLoopCount; i ++) {
        		if (bIsLeftWay) {
        			int nTemp = getTile(nAryTile[0]);
        			for (int j = 0; j < nTileX-1; j ++) {
        				mAryTileIndex[nAryTile[j]] = mAryTileIndex[nAryTile[j+1]];        				
        			}
        			mAryTileIndex[nAryTile[nTileX-1]] = nTemp;
        		} else {
        			int nTemp = getTile(nAryTile[nTileX-1]);
        			for (int j = nTileX-1; j > 0; j --) {
        				mAryTileIndex[nAryTile[j]] = mAryTileIndex[nAryTile[j-1]];     
        			}
        			mAryTileIndex[nAryTile[0]] = nTemp;
        		}
        	}
    		
    	}
    	mScrollDirection = SCROLL_NONE;
    	
    	if (CheckWin() == true) {
    		mMode = STATE_WIN;
    		((JigsawActivity)mContext).setUnlockLogic();
    		((JigsawActivity)mContext).showDialog(JigsawActivity.WIN_MESSAGE);
    	}
    }
    private void defineGestureData() {
    	
    }
    public void setPeekState() {
    	if (mMode != STATE_RUNNING)
    		return;
    	
    	mBeginTime = System.currentTimeMillis();
    	mMode = STATE_READY;
    	
    }
    private  boolean CheckWin() {
    	for (int i = 0; i < mTileCount-1; i ++) {
    		if (mAryTileIndex[i] != i)
    			return false;
    	}
    	return true;
    }
    
    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		thread.setSurfaceSize(width, height);		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();                
                retry = false;
            } catch (InterruptedException e) {
            }
        }		
	}
}
