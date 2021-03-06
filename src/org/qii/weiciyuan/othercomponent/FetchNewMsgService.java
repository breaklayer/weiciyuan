package org.qii.weiciyuan.othercomponent;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;
import org.qii.weiciyuan.BuildConfig;
import org.qii.weiciyuan.bean.AccountBean;
import org.qii.weiciyuan.bean.CommentListBean;
import org.qii.weiciyuan.bean.MessageListBean;
import org.qii.weiciyuan.bean.UnreadBean;
import org.qii.weiciyuan.dao.maintimeline.MainCommentsTimeLineDao;
import org.qii.weiciyuan.dao.maintimeline.MainMentionsTimeLineDao;
import org.qii.weiciyuan.dao.maintimeline.MentionsCommentTimeLineDao;
import org.qii.weiciyuan.dao.unread.UnreadDao;
import org.qii.weiciyuan.support.database.DatabaseManager;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.ui.preference.SettingActivity;

import java.util.Calendar;
import java.util.List;

/**
 * User: Jiang Qi
 * Date: 12-7-31
 */
public class FetchNewMsgService extends Service {

    //close service between 1 clock and 8 clock
    private static final int NIGHT_START_TIME_HOUR = 1;
    private static final int NIGHT_END_TIME_HOUR = 7;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean value = sharedPref.getBoolean(SettingActivity.DISABLE_FETCH_AT_NIGHT, true);
        if (value) {

            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour >= NIGHT_START_TIME_HOUR && hour <= NIGHT_END_TIME_HOUR) {

                if (BuildConfig.DEBUG)
                    Toast.makeText(getApplicationContext(), "between 1 and 8 clock,stop service", Toast.LENGTH_SHORT).show();

                stopSelf();
                return super.onStartCommand(intent, flags, startId);
            } else {

                startFetchNewMsg();
            }

        } else {

            startFetchNewMsg();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startFetchNewMsg() {
        new GetAccountDBTask().executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
    }


    private class GetAccountDBTask extends MyAsyncTask<Void, Void, List<AccountBean>> {

        @Override
        protected List<AccountBean> doInBackground(Void... params) {
            List<AccountBean> accountBeanList = DatabaseManager.getInstance().getAccountList();
            if (accountBeanList.size() > 0) {
                return accountBeanList;
            } else {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onCancelled(List<AccountBean> accountBeans) {
            super.onCancelled(accountBeans);
            stopSelf();
        }

        @Override
        protected void onPostExecute(List<AccountBean> accountBeans) {
            for (AccountBean account : accountBeans) {
                new SimpleTask(account).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }


    class SimpleTask extends MyAsyncTask<Void, Void, Void> {
        WeiboException e;
        AccountBean accountBean;
        CommentListBean commentResult;
        MessageListBean mentionStatusesResult;
        CommentListBean mentionCommentsResult;
        UnreadBean unreadBean;

        public SimpleTask(AccountBean bean) {
            accountBean = bean;
        }

        @Override
        protected Void doInBackground(Void... params) {

            String token = accountBean.getAccess_token();
            try {
                UnreadDao unreadDao = new UnreadDao(token, accountBean.getUid());
                unreadBean = unreadDao.getCount();

                int unreadCommentCount = unreadBean.getCmt();
                int unreadMentionStatusCount = unreadBean.getMention_status();
                int unreadMentionCommentCount = unreadBean.getMention_cmt();

                if (unreadCommentCount > 0) {
                    MainCommentsTimeLineDao commentDao = new MainCommentsTimeLineDao(token).setCount(String.valueOf(unreadCommentCount));
                    commentResult = commentDao.getGSONMsgListWithoutClearUnread();
                }

                if (unreadMentionStatusCount > 0) {
                    MainMentionsTimeLineDao mentionDao = new MainMentionsTimeLineDao(token).setCount(String.valueOf(unreadMentionStatusCount));
                    mentionStatusesResult = mentionDao.getGSONMsgListWithoutClearUnread();
                }

                if (unreadMentionCommentCount > 0) {
                    MainCommentsTimeLineDao dao = new MentionsCommentTimeLineDao(token).setCount(String.valueOf(unreadMentionCommentCount));
                    mentionCommentsResult = dao.getGSONMsgListWithoutClearUnread();
                }

            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
                return null;
            }
            if (unreadBean == null) {
                cancel(true);
            }


            return null;

        }

        @Override
        protected void onPostExecute(Void sum) {

            sendNewMsgBroadcast();

            stopSelf();
            super.onPostExecute(sum);
        }

        @Override
        protected void onCancelled(Void stringIntegerMap) {
            stopSelf();
            super.onCancelled(stringIntegerMap);
        }

        private void sendNewMsgBroadcast() {

            Intent intent = new Intent(MentionsAndCommentsReceiver.ACTION);
            intent.putExtra("account", accountBean);
            intent.putExtra("comment", commentResult);
            intent.putExtra("repost", mentionStatusesResult);
            intent.putExtra("mention_comment", mentionCommentsResult);
            intent.putExtra("unread", unreadBean);
            sendOrderedBroadcast(intent, null);

        }
    }


}
