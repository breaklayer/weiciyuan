package org.qii.weiciyuan.ui.browser;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.MessageBean;
import org.qii.weiciyuan.support.lib.AppFragmentPagerAdapter;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.AppConfig;
import org.qii.weiciyuan.ui.Abstract.AbstractAppActivity;
import org.qii.weiciyuan.ui.Abstract.IToken;
import org.qii.weiciyuan.ui.Abstract.IWeiboMsgInfo;
import org.qii.weiciyuan.ui.basefragment.AbstractTimeLineFragment;
import org.qii.weiciyuan.ui.main.MainTimeLineActivity;
import org.qii.weiciyuan.ui.send.WriteCommentActivity;
import org.qii.weiciyuan.ui.send.WriteRepostActivity;
import org.qii.weiciyuan.ui.task.FavAsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Jiang Qi
 * Date: 12-8-1
 */
public class BrowserWeiboMsgActivity extends AbstractAppActivity implements IWeiboMsgInfo, IToken {

    private MessageBean msg;
    private String token;


    private String comment_sum = "";
    private String retweet_sum = "";

    private ViewPager mViewPager = null;

    private FavAsyncTask favTask = null;

    private ShareActionProvider mShareActionProvider;

    private GestureDetector gestureDetector;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("msg", msg);
        outState.putString("token", token);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            msg = (MessageBean) savedInstanceState.getSerializable("msg");
            token = savedInstanceState.getString("token");
        } else {
            Intent intent = getIntent();
            token = intent.getStringExtra("token");
            msg = (MessageBean) intent.getSerializableExtra("msg");
        }
        setContentView(R.layout.viewpager_layout);

        buildViewPager();
        buildActionBarAndViewPagerTitles();
    }

    private void buildViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        TimeLinePagerAdapter adapter = new TimeLinePagerAdapter(getFragmentManager());
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(onPageChangeListener);
        gestureDetector = new GestureDetector(BrowserWeiboMsgActivity.this, new MyOnGestureListener());
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void buildActionBarAndViewPagerTitles() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.detail));

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(actionBar.newTab()
                .setText(getString(R.string.weibo))
                .setTabListener(tabListener));

        actionBar.addTab(actionBar.newTab()
                .setText(getString(R.string.comments))
                .setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab()
                .setText(getString(R.string.repost))
                .setTabListener(tabListener));

    }

    ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            getActionBar().setSelectedNavigationItem(position);
            switch (position) {

                case 2:
                    ((RepostsByIdTimeLineFragment) getRepostFragment()).load();
                    break;
            }

        }
    };


    private AbstractTimeLineFragment getRepostFragment() {
        return ((AbstractTimeLineFragment) getFragmentManager().findFragmentByTag(
                RepostsByIdTimeLineFragment.class.getName()));
    }

    private AbstractTimeLineFragment getCommentFragment() {
        return ((AbstractTimeLineFragment) getFragmentManager().findFragmentByTag(
                CommentsByIdTimeLineFragment.class.getName()));
    }

    private Fragment getBrowserWeiboMsgFragment() {
        return getFragmentManager().findFragmentByTag(BrowserWeiboMsgFragment.class.getName());
    }

    ActionBar.TabListener tabListener = new ActionBar.TabListener() {
        boolean comment = false;
        boolean repost = false;

        public void onTabSelected(ActionBar.Tab tab,
                                  FragmentTransaction ft) {

            if (mViewPager.getCurrentItem() != tab.getPosition())
                mViewPager.setCurrentItem(tab.getPosition());
            if (getCommentFragment() != null)
                getCommentFragment().clearActionMode();
            if (getRepostFragment() != null)
                getRepostFragment().clearActionMode();

            switch (tab.getPosition()) {

                case 1:
                    comment = true;
                    break;
                case 2:
                    repost = true;
                    break;
                case 3:
                    break;
            }
        }

        public void onTabUnselected(ActionBar.Tab tab,
                                    FragmentTransaction ft) {
            switch (tab.getPosition()) {

                case 1:
                    comment = false;
                    break;
                case 2:
                    repost = false;
                    break;

            }
        }

        public void onTabReselected(ActionBar.Tab tab,
                                    FragmentTransaction ft) {
            switch (tab.getPosition()) {

                case 1:
                    if (comment) {
                        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            getCommentFragment().getListView().setSelection(0);
                            getCommentFragment().getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));

                        } else {

                            getCommentFragment().getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            getCommentFragment().getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                            getCommentFragment().getListView().setSelection(0);
                        }

                    }
                    break;
                case 2:
                    if (repost) {
                        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            getRepostFragment().getListView().setSelection(0);
                            getRepostFragment().getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));

                        } else {

                            getRepostFragment().getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            getRepostFragment().getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                            getRepostFragment().getListView().setSelection(0);
                        }

                    }
                    break;
                case 3:
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu_browserweibomsgactivity, menu);

        MenuItem item = menu.findItem(R.id.menu_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        buildShareActionMenu();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainTimeLineActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            case R.id.repostsbyidtimelinefragment_repost:
                intent = new Intent(this, WriteRepostActivity.class);
                intent.putExtra("token", getToken());
                intent.putExtra("id", getMsg().getId());
                intent.putExtra("msg", getMsg());
                startActivity(intent);
                return true;
            case R.id.commentsbyidtimelinefragment_comment:

                intent = new Intent(this, WriteCommentActivity.class);
                intent.putExtra("token", getToken());
                intent.putExtra("id", getMsg().getId());
                intent.putExtra("msg", getMsg());
                startActivity(intent);

                return true;

            case R.id.menu_share:

                buildShareActionMenu();
                return true;
            case R.id.menu_copy:
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("sinaweibo", getMsg().getText()));
                Toast.makeText(this, getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_fav:
                if (favTask == null || favTask.getStatus() == MyAsyncTask.Status.FINISHED) {
                    favTask = new FavAsyncTask(getToken(), msg.getId());
                    favTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                }

                return true;
        }
        return false;
    }

    private void buildShareActionMenu() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        if (msg != null) {
            sharingIntent.putExtra(Intent.EXTRA_TEXT, msg.getText());
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(sharingIntent, 0);
            boolean isIntentSafe = activities.size() > 0;
            if (isIntentSafe && mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(sharingIntent);
            }
        }
    }

    class TimeLinePagerAdapter extends
            AppFragmentPagerAdapter {

        List<Fragment> list = new ArrayList<Fragment>();


        public TimeLinePagerAdapter(FragmentManager fm) {
            super(fm);
            if (getBrowserWeiboMsgFragment() == null) {
                list.add(new BrowserWeiboMsgFragment(msg));
            } else {
                list.add(getBrowserWeiboMsgFragment());
            }
            if (getCommentFragment() == null) {
                list.add(new CommentsByIdTimeLineFragment(token, msg.getId()));
            } else {
                list.add(getCommentFragment());
            }
            if (getRepostFragment() == null) {
                list.add(new RepostsByIdTimeLineFragment(token, msg.getId(), msg));
            } else {
                list.add(getRepostFragment());
            }
        }

        @Override
        public Fragment getItem(int i) {
            return list.get(i);
        }

        @Override
        protected String getTag(int position) {
            List<String> tagList = new ArrayList<String>();
            tagList.add(BrowserWeiboMsgFragment.class.getName());
            tagList.add(CommentsByIdTimeLineFragment.class.getName());
            tagList.add(RepostsByIdTimeLineFragment.class.getName());
            return tagList.get(position);
        }

        @Override
        public int getCount() {
            return list.size();
        }
    }


    @Override
    public String getToken() {
        return token;
    }

    @Override
    public MessageBean getMsg() {
        return msg;
    }


    class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (velocityX > AppConfig.SWIPE_MIN_DISTANCE && mViewPager.getCurrentItem() == 0) {
                finish();
                return true;
            }
            return false;
        }
    }
}
