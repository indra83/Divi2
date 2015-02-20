package co.in.divi.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.io.File;
import java.util.ArrayList;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.LectureSessionProvider.FollowMeListener;
import co.in.divi.LocationManager.Breadcrumb;
import co.in.divi.LocationManager.LOCATION_SUBTYPE;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.R;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;
import co.in.divi.content.Topic;
import co.in.divi.content.Topic.Audio;
import co.in.divi.content.Topic.Image;
import co.in.divi.content.Topic.ImageSet;
import co.in.divi.content.Topic.Section;
import co.in.divi.content.Topic.VM;
import co.in.divi.content.Topic.Video;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Command;
import co.in.divi.fragment.AudioPlayerFragment;
import co.in.divi.fragment.HeaderFragment;
import co.in.divi.fragment.LessonPlanFragment;
import co.in.divi.fragment.TopicPageFragment;
import co.in.divi.ui.RecyclingImageView;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;
import co.in.divi.util.image.ImageCache.ImageCacheParams;
import co.in.divi.util.image.ImageResizer;

public class LearnActivity extends BaseActivity implements FollowMeListener {
    static final String TAG = LearnActivity.class.getSimpleName();

    ViewPager viewPager;
    int lastExpandedChapter = -1;
    ExpandableListView toc;
    DrawerLayout drawer;
    ImageButton lessonPlanButton;

    LearnMediaController lmc;
    DatabaseHelper dbHelper;
    PagerAdapter topicsAdapter;
    TOCAdapter tocAdapter;
    LoadDataTask loadDataTask;
    private OngoingTestCheckTask testCheckTask;
    private ImageResizer mImageFetcher;

    public Node[] chapters;                                                    // ! accessible to
    // fragments
    private Node displayedTopic;
    public Book currentBook;
    public File bookBaseDir;
    private DiviReference uriToLoad;                                                    // could be null
    private boolean reloadData;

    private RelativeLayout.LayoutParams lps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        dbHelper = DatabaseHelper.getInstance(this);
        topicsAdapter = new TopicPagerAdapter(getFragmentManager());
        tocAdapter = new TOCAdapter();
        ImageCacheParams cacheParams = new ImageCacheParams(this, null);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageResizer(this, 100);
        mImageFetcher.setLoadingImage(null);
        mImageFetcher.addImageCache(getFragmentManager(), cacheParams);
        setContentView(R.layout.activity_learn);
        viewPager = (ViewPager) findViewById(R.id.pager);
        toc = (ExpandableListView) findViewById(R.id.toc);
        lessonPlanButton = (ImageButton) findViewById(R.id.lessonplan_button);
        // media
        lmc = new LearnMediaController(this);
        lmc.youtubeFragment = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
        lmc.youtubeRoot = (ViewGroup) findViewById(R.id.youtubeRoot);
        lmc.audioPlayerFragment = (AudioPlayerFragment) getFragmentManager().findFragmentById(R.id.frag_audio);
        lmc.videoView = (co.in.divi.ui.TextureVideoView) findViewById(R.id.videoView);
        lmc.videoRoot = findViewById(R.id.videoRoot);
        lmc.imageView = (ImageView) findViewById(R.id.imageView);
        lmc.imageRoot = findViewById(R.id.imageRoot);
        lmc.imagePager = (ViewPager) findViewById(R.id.imagePager);
        lmc.leftArrow = (ImageView) findViewById(R.id.leftArrow);
        lmc.rightArrow = (ImageView) findViewById(R.id.rightArrow);
        lmc.init();

        header = (HeaderFragment) getFragmentManager().findFragmentById(R.id.header);
        headerShadow = findViewById(R.id.header_shadow);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerListener(new DrawerListener() {
            @Override
            public void onDrawerStateChanged(int state) {
                switch (state) {
                    case DrawerLayout.STATE_IDLE:
                        if (drawer.isDrawerOpen(Gravity.LEFT)) {
                            showHeader();
                        } else {
                            hideHeader();
                        }
                        break;
                    case DrawerLayout.STATE_DRAGGING:
                        break;
                    case DrawerLayout.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (slideOffset > 0.25)
                    showHeader();
                else if (slideOffset < 0.1)
                    hideHeader();
            }

            @Override
            public void onDrawerOpened(View arg0) {
                showHeader();// ensure
                if (chapters == null)
                    return;
                for (int i = 0; i < chapters.length; i++) {
                    if (getDisplayedTopic() != null && getDisplayedTopic().parentId.equals(chapters[i].id))
                        toc.expandGroup(i);
                    else
                        toc.collapseGroup(i);
                }
            }

            @Override
            public void onDrawerClosed(View arg0) {
                hideHeader();
                new ShowcaseView.Builder(LearnActivity.this)
                        .setContentTitle("Table of Contents")
                        .setContentText("Pull to access table of contents.")
                        .setTarget(new ViewTarget(findViewById(R.id.slide_toc_button)))
                        .setStyle(R.style.CustomShowcaseTheme)
                        .singleShot(11)
                        .setShowcaseEventListener(new OnShowcaseEventListener() {
                            @Override
                            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                ShowcaseView showcaseView2 = new ShowcaseView.Builder(LearnActivity.this)
                                        .setContentTitle("Swipe for next/previous topic.")
                                        .setStyle(R.style.CustomShowcaseTheme)//setStyle instead of setTarget!
                                        .hideOnTouchOutside()
                                        .singleShot(12)
                                        .build();
                                showcaseView2.setButtonPosition(lps);
                                showcaseView2.setBackgroundResource(R.drawable.swipe2);
                            }

                            @Override
                            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                            }

                            @Override
                            public void onShowcaseViewShow(ShowcaseView showcaseView) {
                            }
                        })
                        .build()
                        .setButtonPosition(lps);
            }
        });
        findViewById(R.id.slide_toc_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "on onClick - ");
                drawer.openDrawer(Gravity.LEFT);
            }
        });

        // viewPager.setPageMargin(30);
        viewPager.setAdapter(topicsAdapter);
        viewPager.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Node openedNode = Util.getTopicNodeFromIndex(chapters, position);
                setDisplayedTopic(openedNode);
                lmc.closeAll();
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int scrollState) {
                if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
                    mImageFetcher.setPauseWork(false);
                } else {
                    mImageFetcher.setPauseWork(true);
                }
            }
        });

        // setup toc
        toc.setAdapter(tocAdapter);
        toc.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mImageFetcher.setPauseWork(true);
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        toc.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                showTopic(tocAdapter.getTopicNo(groupPosition, childPosition), null);
                return true;
            }
        });
        toc.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                Node chapterNode = (Node) tocAdapter.getGroup(groupPosition);
                if (getDisplayedTopic() != null && getDisplayedTopic().parentId.equals(chapterNode.id)) {
                    // allow collapsing of selected chapters also...
                    // return true;
                }
                toc.setSelection(groupPosition);
                return false;
            }
        });
        toc.setOnGroupExpandListener(new OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                // if (lastExpandedChapter != -1 && groupPosition != lastExpandedChapter) {
                // toc.collapseGroup(lastExpandedChapter);
                // }
                // lastExpandedChapter = groupPosition;
            }
        });

        lessonPlanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LessonPlanFragment lpFragment = new LessonPlanFragment();
                FragmentManager fm = getFragmentManager();
                lpFragment.show(fm, "dialog_lp_fragment");
            }
        });

        reloadData = true;

        // Help
        lps = new RelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.help_ok_width), getResources().getDimensionPixelSize(R.dimen.help_ok_height));
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        lps.setMargins(margin, margin, margin, margin);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (LogConfig.DEBUG_ACTIVITIES)
            Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);
        reloadData = true;
        // loadData();
        // !! in our scenario onStart is always called when this happens?
    }

    @Override
    protected void onStart() {
        super.onStart();
        mImageFetcher.setExitTasksEarly(false);
        if (!userSessionProvider.getUserData().isTeacher())
            lessonPlanButton.setVisibility(View.GONE);

        // Disable lessonplan
        lessonPlanButton.setVisibility(View.GONE);

        // loadData();
        // hardware acceleration
        // Log.d(TAG, "header hw. accl. - " + header.getView().isHardwareAccelerated());
        // Log.d(TAG, "media controller hw. accl. - " + lmc.mediaController.isHardwareAccelerated());
    }

    @Override
    protected void onResume() {
        super.onResume();
        lectureSessionProvider.setFollowMeListener(this);
        if (reloadData) {
            lmc.closeAll();
            reloadData = false;
            loadData();
        } else {
            // set the current location (when coming from sleep)
            setTopicLocation();
        }
        if (testCheckTask != null)
            testCheckTask.cancel(false);
        testCheckTask = new OngoingTestCheckTask();
        testCheckTask.execute(new Void[0]);
    }

    @Override
    protected void onPause() {
        lectureSessionProvider.setFollowMeListener(null);
        lmc.closeAll(); // before super to set proper location
        super.onPause();
        if (testCheckTask != null)
            testCheckTask.cancel(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
        if (loadDataTask != null && loadDataTask.getStatus() != LoadDataTask.Status.FINISHED) {
            loadDataTask.cancel(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    @Override
    public void onBackPressed() {
        if (!lmc.handleBackPress())
            super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.zoom_out);
    }

    /*
     * Loading the book
     */
    private void loadData() {
        if (LogConfig.DEBUG_ACTIVITIES)
            Log.d(TAG, "loadData()");
        uriToLoad = null;
        currentBook = null;
        displayedTopic = null;
        // TODO: add courseId check
        try {
            if (getIntent().hasExtra(Util.INTENT_EXTRA_BOOK)) {
                this.currentBook = (Book) getIntent().getExtras().get(Util.INTENT_EXTRA_BOOK);
            } else {
                uriToLoad = new DiviReference(Uri.parse(getIntent().getExtras().getString(Util.INTENT_EXTRA_URI)));
                ArrayList<Book> books = DatabaseHelper.getInstance(this).getBooks(uriToLoad.courseId);
                for (Book book : books) {
                    if (book.id.equals(uriToLoad.bookId)) {
                        this.currentBook = book;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error loading book", e);
            Toast.makeText(this, "Error loading book!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (this.currentBook == null) {
            Toast.makeText(this, "Book not found. Please run update.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // TODO:ensure user has access to current book

        bookBaseDir = new File(((DiviApplication) getApplication()).getBooksBaseDir(currentBook.courseId), currentBook.id);
        if (loadDataTask != null && loadDataTask.getStatus() != LoadDataTask.Status.FINISHED) {
            loadDataTask.cancel(false);
        }
        loadDataTask = new LoadDataTask();
        loadDataTask.execute(new Void[0]);
    }

    private void refreshViews(Node[] newData) {
        {
            int count = 0;
            for (Node n : newData)
                count += n.getChildren().size();
            if (count == 0) {
                Toast.makeText(this, "No learn content found.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        this.chapters = newData;
        topicsAdapter.notifyDataSetChanged();
        tocAdapter.notifyDataSetChanged();
        if (uriToLoad == null) {
            // TODO: open appropriate page
            showTopic(0, null);
            // hack! force call due to bug in listener
            setDisplayedTopic(Util.getTopicNodeFromIndex(chapters, 0));
            drawer.openDrawer(Gravity.LEFT);
        } else {
            int tNo = 0;
            for (Node ch : chapters) {
                for (Node t : ch.getChildren()) {
                    if (t.id.equals(uriToLoad.itemId)) {
                        showTopic(tNo, uriToLoad.subItemId);
                        // hack! force call due to bug in listener
                        // setDisplayedTopic(t);
                        if (uriToLoad.subItemId != null)
                            openResource(uriToLoad.subItemId, uriToLoad.fragment);
                        return;
                    }
                    tNo++;
                }
            }
            Toast.makeText(LearnActivity.this, "Topic not found!", Toast.LENGTH_LONG).show();
            showTopic(0, null);
            // hack! force call due to bug in listener
            setDisplayedTopic(Util.getTopicNodeFromIndex(chapters, 0));
        }
    }

    /*
     * Topic state management
     */
    public Node getDisplayedTopic() {
        return displayedTopic;
    }

    private void setDisplayedTopic(Node newTopic) {
        if (LogConfig.DEBUG_ACTIVITIES)
            Log.d(TAG, "displaying topic:" + newTopic.id);
        if (displayedTopic != null && newTopic.id.equals(displayedTopic.id))
            return;// could be called twice!
        // update location
        locationManager.setNewLocation(LOCATION_TYPE.TOPIC, LOCATION_SUBTYPE.TOPIC_TOPIC, new DiviReference(currentBook.courseId,
                currentBook.id, DiviReference.REFERENCE_TYPE_TOPIC, newTopic.id, null), Breadcrumb.get(userSessionProvider.getCourseName(),
                currentBook.name, newTopic.parentName, newTopic.name, null), null);
        displayedTopic = newTopic;
    }

    /*
     * Navigation
     */
    void showTopic(int topicNo, String scrollId) {
        viewPager.setCurrentItem(topicNo, true);
        setDisplayedTopic(Util.getTopicNodeFromIndex(chapters, topicNo));
        if (scrollId != null) {
            // TODO: do scroll
        }
        tocAdapter.notifyDataSetChanged();
    }

    /*
     * Resource opening (audio/video) playing
     */
    public void openResource(String resourceId, String fragment) {
        Topic topic = (Topic) getDisplayedTopic().tag;
        File baseDir = new File(bookBaseDir, topic.pagePath).getParentFile();
        for (Audio audio : topic.audios) {
            if (audio.id.equals(resourceId)) {
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "Playing audio:" + audio.src);
                lmc.playAudio(baseDir, audio, fragment);
                return;
            }
        }
        for (Video video : topic.videos) {
            if (video.id.equals(resourceId)) {
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "Playing video:" + video.src);
                if (video.youtubeId != null)
                    lmc.playYoutubeVideo(video, fragment);
                else {
                    if (currentBook.streamBaseUrl != null) {
                        Uri videoUri = Uri.parse(currentBook.streamBaseUrl).buildUpon()
                                .appendPath(currentBook.courseId)
                                .appendPath(currentBook.id)
                                .appendPath(getDisplayedTopic().parentId)
                                .appendPath(getDisplayedTopic().id)
                                .appendPath(video.src)
                                .build();
                        lmc.playVideo(null, videoUri, video, fragment);
                    } else {
                        lmc.playVideo(baseDir, null, video, fragment);
                    }
                }
                return;
            }
        }
        if (topic.vms != null) {
            for (VM vm : topic.vms) {
                if (vm.id.equals(resourceId)) {
                    if (LogConfig.DEBUG_ACTIVITIES)
                        Log.d(TAG, "Opening vm:" + vm.src);
                    if (Util.isVMExists(getPackageManager(), vm.appPackage, vm.appVersionCode)) {
                        // vm exists, open vm
                        Intent intent;
                        if (vm.appActivityName != null && vm.appActivityName.length() > 0) {
                            intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName(vm.appPackage, vm.appActivityName));
                        } else {
                            intent = getPackageManager().getLaunchIntentForPackage(vm.appPackage);
                        }
                        intent.putExtra("UID", userSessionProvider.getUserData().uid);
                        intent.putExtra("COURSE_ID", getDisplayedTopic().courseId);
                        intent.putExtra("BOOK_ID", getDisplayedTopic().bookId);
                        intent.putExtra("TOPIC_ID", getDisplayedTopic().id);
                        intent.putExtra("VM_ID", vm.id);
                        intent.putExtra("VM_ACTIVITY", vm.appActivityName);
                        intent.putExtra(
                                "BREADCRUMB",
                                Breadcrumb.get(userSessionProvider.getCourseName(), currentBook.name, getDisplayedTopic().parentName,
                                        getDisplayedTopic().name, vm.title).getBreadcrumbArray());
                        // intent.putExtra("FRAGMENT", fragment);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(intent);
                    } else {
                        // vm needs to be installed/updated
                        Toast.makeText(this, "VM needs to be installed/updated...", Toast.LENGTH_SHORT).show();
                        if (Config.IS_PLAYSTORE_APP) {
                            String appUrl = "market://details?id=" + vm.appPackage;
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(appUrl));
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(baseDir, vm.src)), "application/vnd.android.package-archive");
                            startActivity(intent);
                        }
                    }
                    return;
                }
            }
        }
        for (Image image : topic.images) {
            if (image.id.equals(resourceId)) {
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "Opening image : " + image.src);
                lmc.showImage(baseDir, image, fragment);
                return;
            }
        }
        for (ImageSet iset : topic.imageSets) {
            if (iset.id.equals(resourceId)) {
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "Opening image set: " + iset.title);
                lmc.showImageSet(baseDir, iset, fragment);
                return;
            }
        }
        Toast.makeText(this, "Couldn't open the resource...", Toast.LENGTH_SHORT).show();
    }

    public void handleUrlClick(DiviReference ref) {
        if (LogConfig.DEBUG_ACTIVITIES)
            Log.d(TAG, "curbook:" + currentBook.courseId + "," + currentBook.id);
        if (LogConfig.DEBUG_ACTIVITIES)
            Log.d(TAG, "ref:" + ref.itemId + ",type:" + ref.type + ", disp:" + getDisplayedTopic().id);
        if (ref.courseId.equals(currentBook.courseId) && ref.bookId.equals(currentBook.id)) {
            Toast.makeText(LearnActivity.this, "todo", Toast.LENGTH_LONG).show();
            // // open corresponding topic
        } else {
            Toast.makeText(LearnActivity.this, "Links to other books not supported yet..", Toast.LENGTH_LONG).show();
            // TODO: link to different book; raise intent
        }
    }

    void setSubItemInLocation(String subItemId, String subItemName, LOCATION_SUBTYPE type, String fragment) {
        if (displayedTopic != null) {
            DiviReference diviRef = new DiviReference(currentBook.courseId, currentBook.id, DiviReference.REFERENCE_TYPE_TOPIC,
                    displayedTopic.id, subItemId);
            diviRef.setFragment(fragment);
            locationManager.setNewLocation(LOCATION_TYPE.TOPIC, type, diviRef, Breadcrumb.get(userSessionProvider.getCourseName(),
                    currentBook.name, displayedTopic.parentName, displayedTopic.name, subItemName), null);
        } else {
            Log.w(TAG, "should not reach here!!");
        }
    }

    void setTopicLocation() {
        if (displayedTopic != null && userSessionProvider.isLoggedIn()) {
            locationManager.setNewLocation(LOCATION_TYPE.TOPIC, LOCATION_SUBTYPE.TOPIC_TOPIC, new DiviReference(currentBook.courseId,
                    currentBook.id, DiviReference.REFERENCE_TYPE_TOPIC, displayedTopic.id, null), Breadcrumb.get(
                    userSessionProvider.getCourseName(), currentBook.name, displayedTopic.parentName, displayedTopic.name, null), null);
        }
    }

    class LoadDataTask extends AsyncTask<Void, Void, Integer> {

        Node[] newChapters;
        String courseId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            courseId = userSessionProvider.getCourseId();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                this.newChapters = dbHelper.getChapterNodes(courseId, currentBook.id, Node.NODE_TYPE_TOPIC);
            } catch (Exception e) {
                Log.e(TAG, "error loading book", e);
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 0) {
                refreshViews(this.newChapters);
            } else {
                Toast.makeText(LearnActivity.this, "Error loading book.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    class OngoingTestCheckTask extends AsyncTask<Void, Void, Integer> {
        private String mSelectionClause;
        private String[] mSelectionArgs;

        boolean isTeacher;

        public OngoingTestCheckTask() {
            long now = System.currentTimeMillis();
            mSelectionClause = Commands.STATUS + " = ? AND " + Commands.TYPE + " = ? AND " + Commands.ITEM_TYPE + " = ? AND "
                    + Commands.UID + " = ? AND " + Commands.APPLY_TIMESTAMP + " < ? AND " + Commands.END_TIMESTAMP + " > ?";
            mSelectionArgs = new String[]{"" + Command.COMMAND_STATUS_ACTIVE, "" + Command.COMMAND_CATEGORY_UNLOCK,
                    "" + Command.COMMAND_UNLOCK_ITEM_CATEGORY_TEST, userSessionProvider.getUserData().uid, "" + now, "" + now};

            isTeacher = userSessionProvider.getUserData().isTeacher();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (isTeacher)
                return 0;// don't lock for teacher
            Cursor cursor = LearnActivity.this.getContentResolver().query(UserDBContract.Commands.CONTENT_URI,
                    UserDBContract.Commands.PROJECTION_ALL, mSelectionClause, mSelectionArgs, Commands.SORT_ORDER_LATEST_FIRST);
            int foundCommands = cursor.getCount();
            if (LogConfig.DEBUG_ACTIVITIES)
                Log.d(TAG, "found live quiz commands: " + foundCommands);
            cursor.close();
            if (foundCommands > 0) {
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result > 0) {
                Toast.makeText(LearnActivity.this, "Test in progress!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    class TOCAdapter extends BaseExpandableListAdapter {

        LayoutInflater inflater;
        int normalTextColor, selectedTextColor;

        TOCAdapter() {
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            normalTextColor = getResources().getColor(R.color.text_topic_normal);
            selectedTextColor = getResources().getColor(R.color.text_topic_selected);
        }

        public int getTopicNo(int groupPosition, int childPosition) {
            int ret = 0;
            for (int i = 0; i < groupPosition; i++) {
                ret += chapters[i].getChildren().size();
            }
            ret += childPosition;
            return ret;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return chapters[groupPosition].getChildren().get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_topic, parent, false);
            }
            TextView topicTitle = (TextView) convertView.findViewById(R.id.topic_title);
            LinearLayout subtopics = (LinearLayout) convertView.findViewById(R.id.subtopics);
            // GridLayout thumbnails = (GridLayout) convertView.findViewById(R.id.thumbs);
            View separator1 = convertView.findViewById(R.id.separator1);
            View separator2 = convertView.findViewById(R.id.separator2);
            Node topicNode = (Node) getChild(groupPosition, childPosition);

            int textColor;
            if (getDisplayedTopic() != null && topicNode.id.equals(getDisplayedTopic().id)) {
                convertView.findViewById(R.id.box).setBackgroundResource(R.drawable.bg_topic_selected);
                textColor = selectedTextColor;
                separator1.setBackgroundResource(R.drawable.topic_separator_light);
                separator2.setBackgroundResource(R.drawable.topic_separator_light);
            } else {
                convertView.findViewById(R.id.box).setBackgroundResource(R.drawable.bg_topic_normal);
                textColor = normalTextColor;
                separator1.setBackgroundResource(R.drawable.topic_separator_dark);
                separator2.setBackgroundResource(R.drawable.topic_separator_dark);
            }

            topicTitle.setText(topicNode.name.toUpperCase());
            topicTitle.setTextColor(textColor);
            subtopics.removeAllViews();
            for (Section section : ((Topic) topicNode.tag).sections) {
                TextView subTopic = (TextView) inflater.inflate(R.layout.item_topic_sectiontext, parent, false);
                subTopic.setText(section.title);
                subTopic.setTextColor(textColor);
                subtopics.addView(subTopic);
            }
            File topicFileRoot = new File(bookBaseDir, ((Topic) topicNode.tag).pagePath).getParentFile();
            RecyclingImageView[] imageViews = new RecyclingImageView[]{(RecyclingImageView) convertView.findViewById(R.id.thumb_1_1),
                    (RecyclingImageView) convertView.findViewById(R.id.thumb_1_2),
                    (RecyclingImageView) convertView.findViewById(R.id.thumb_1_3),
                    (RecyclingImageView) convertView.findViewById(R.id.thumb_2_1),
                    (RecyclingImageView) convertView.findViewById(R.id.thumb_2_2),
                    (RecyclingImageView) convertView.findViewById(R.id.thumb_2_3)};
            int thumbCount = 0;
            for (Image image : ((Topic) topicNode.tag).images) {
                imageViews[thumbCount].setVisibility(View.VISIBLE);
                mImageFetcher.loadImage(new File(topicFileRoot, image.src), imageViews[thumbCount]);
                thumbCount++;
                if (thumbCount > 3)
                    break;// show at most 4 images
            }
            for (Video video : ((Topic) topicNode.tag).videos) {
                imageViews[thumbCount].setVisibility(View.VISIBLE);
                mImageFetcher.loadImage(new File(topicFileRoot, video.thumb), imageViews[thumbCount]);
                thumbCount++;
                if (thumbCount >= imageViews.length)
                    break;
            }
            if (thumbCount == 0) {
                separator2.setVisibility(View.GONE);
            } else {
                separator2.setVisibility(View.VISIBLE);
            }
            while (thumbCount < imageViews.length) {
                imageViews[thumbCount].setVisibility(View.GONE);
                thumbCount++;
            }
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return chapters[groupPosition].getChildren().size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return chapters[groupPosition];
        }

        @Override
        public int getGroupCount() {
            if (chapters == null) {
                return 0;
            }
            return chapters.length;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_chapter, parent, false);
            }
            Node chapterNode = (Node) getGroup(groupPosition);

            if (getDisplayedTopic() != null && getDisplayedTopic().parentId.equals(chapterNode.id)) {
                convertView.setBackgroundResource(R.drawable.bg_home_section_selected);
            } else {
                convertView.setBackgroundColor(Color.parseColor("#33393D"));
            }
            TextView chapterTitle = (TextView) convertView.findViewById(R.id.chapter_title);
            TextView chapterName = (TextView) convertView.findViewById(R.id.chapter_name);
            chapterTitle.setText("Chapter " + (1 + groupPosition));
            chapterName.setText(chapterNode.name);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    class TopicPagerAdapter extends FragmentStatePagerAdapter {

        public TopicPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            TopicPageFragment f = TopicPageFragment.newInstance(position);
            return f;
        }

        @Override
        public int getCount() {
            if (chapters == null) {
                return 0;
            }
            int count = 0;
            for (Node n : chapters)
                count += n.getChildren().size();
            return count;
        }

        @Override
        public int getItemPosition(Object object) {
            // force refresh on notify dataset change.
            return POSITION_NONE;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            // Log.d(TAG, "setting primary item, position:	" + position);
        }
    }

    @Override
    public void onCourseChange() {
        finish();
    }

    @Override
    public boolean tryFollowMe(Uri followUri) {
        try {
            DiviReference newRef = new DiviReference(followUri);
            if (newRef.isSameResourceAs(locationManager.getLocationRef())) {
                // follow stream
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "following stream, " + newRef.fragment);
                DiviReference streamInstruction = new DiviReference(followUri);
                lmc.processFollowMe(streamInstruction.subItemId, streamInstruction.fragment);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "stream process error?", e);
        }
        return false;
    }
}
