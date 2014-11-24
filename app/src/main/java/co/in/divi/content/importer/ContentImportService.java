package co.in.divi.content.importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import co.in.divi.ContentUpdateManager;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.activity.HomeActivity;
import co.in.divi.content.AllowedAppsProvider;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.TagModel;
import co.in.divi.content.Topic;
import co.in.divi.content.Topic.VM;
import co.in.divi.content.TopicXmlParser;
import co.in.divi.content.importer.BookDefinition.AssessmentDefinition;
import co.in.divi.content.importer.BookDefinition.ChapterDefinition;
import co.in.divi.content.importer.BookDefinition.TopicDefinition;
import co.in.divi.content.importer.BookDefinition.TopicNode;
import co.in.divi.content.questions.BaseQuestion;
import co.in.divi.content.questions.FillBlank_QuestionXmlParser;
import co.in.divi.content.questions.Label_QuestionXmlParser;
import co.in.divi.content.questions.MCQ_QuestionXmlParser;
import co.in.divi.content.questions.Match_QuestionXmlParser;
import co.in.divi.fragment.questions.QuestionFragmentFactory;
import co.in.divi.model.ContentUpdates;
import co.in.divi.util.ImporterConfig;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

import com.google.gson.Gson;

/**
 * Imports content:
 * 
 * 1. SD card content
 * 
 * 2. Web download content.
 * 
 * @author Indra
 * 
 */
public class ContentImportService extends IntentService {
	private static final String	TAG	= ContentImportService.class.getSimpleName();

	ContentUpdateManager		contentUpdateManager;
	private Handler				mHandler;

	public ContentImportService() {
		super("ContentImportService");
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "starting ContentImportService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		contentUpdateManager = ContentUpdateManager.getInstance(this);
		mHandler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "handling : " + intent.getDataString());
		Gson gson = new Gson();

		Intent i = new Intent(this, HomeActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		Notification note = new NotificationCompat.Builder(this).setContentTitle("Importing book").setContentText("blah blah")
				.setSmallIcon(R.drawable.divi_logo_w).setContentIntent(pi).setOngoing(true).build();
		startForeground(1337, note);

		File updateFolder;
		updateFolder = ((DiviApplication) getApplication()).getTempDir();
		if (updateFolder.exists())
			Util.deleteRecursive(updateFolder);
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "update folder exists? " + updateFolder.exists());
		updateFolder = ((DiviApplication) getApplication()).getTempDir();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "update folder exists? " + updateFolder.exists());
		boolean unzipSuccess;
		try {
			// try {
			// Log.d(TAG, "coopyiing");
			// Util.copy(, new File(updateFolder, "upd.zip"));
			// } catch (IOException e) {
			// Log.e(TAG, "error copying zip file", e);
			// unzipSuccess = false;
			// }
			unzipSuccess = Util.unzip(getContentResolver().openInputStream(intent.getData()), updateFolder);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "opening download failed", e);
			unzipSuccess = false;
		}

		if (!unzipSuccess) {
			mHandler.postAtFrontOfQueue(new Runnable() {
				@Override
				public void run() {
					contentUpdateManager.importFailed("Unzip failed, download corrupted?");
				}
			});
			return;
		}

		File bookDefFile = new File(updateFolder, ImporterConfig.BOOK_DEFINITION_FILE_NAME);
		File tagsFile = new File(updateFolder, ImporterConfig.BOOK_TAGS_FILE);
		// json
		try {
			BookDefinition bookDef = gson.fromJson(Util.openJSONFile(bookDefFile), BookDefinition.class);
			ContentUpdates.Update updateDef = contentUpdateManager.getCurrentUpdate();
			TagModel bookTags[] = new TagModel[0];
			if (tagsFile.exists())
				bookTags = (TagModel[]) new Gson().fromJson(Util.openJSONFile(tagsFile), TagModel[].class);
			if (LogConfig.DEBUG_CONTENT_IMPORT) {
				Log.d(TAG, "upd def json:" + new Gson().toJson(updateDef));
				Log.d(TAG, "book def ersion:" + new Gson().toJson(bookDef));
				Log.d(TAG, "tags json:" + new Gson().toJson(bookTags));
			}
			if (updateDef.isDropboxImport)
				bookDef.courseId = UserSessionProvider.getInstance(this).getCourseId();

			File toDir = new File(((DiviApplication) getApplication()).getBooksBaseDir(bookDef.courseId), bookDef.bookId);
			// parse all topic & assessment xmls and fill in the bookDef
			for (ChapterDefinition chapter : bookDef.chapters) {
				// parse topics
				chapter.topicNodes = new ArrayList<BookDefinition.TopicNode>();
				for (TopicDefinition topicDef : chapter.topics) {
					File topicXmlFile = new File(new File(new File(updateFolder, chapter.id), topicDef.id), "topic.xml");
					TopicNode topicNode = null;
					if (!topicXmlFile.exists()) {
						// must be a patch, check already existing content
						topicXmlFile = new File(new File(new File(toDir, chapter.id), topicDef.id), "topic.xml");
						if (topicXmlFile.exists())
							topicNode = new TopicXmlParser().getNodeFromXml(topicDef.name, topicXmlFile,
									toDir.toURI().relativize(topicXmlFile.toURI()).getPath());
					} else {
						topicNode = new TopicXmlParser().getNodeFromXml(topicDef.name, topicXmlFile,
								updateFolder.toURI().relativize(topicXmlFile.toURI()).getPath());
					}
					if (!topicXmlFile.exists()) {
						Log.w(TAG, "topic xml not found! - " + topicXmlFile.toString());
						continue;
					}
					if (topicNode == null) {
						Log.w(TAG, "topic parsing failed - " + topicXmlFile.toString());
						continue;
					}
					chapter.topicNodes.add(topicNode);
				}

				// parse assessments
				chapter.assessmentNodes = new ArrayList<AssessmentFileModel>();
				for (AssessmentDefinition assDef : chapter.assessments) {
					File assJsonFile = new File(new File(new File(updateFolder, chapter.id), assDef.id), "assessments.json");
					if (!assJsonFile.exists()) {
						// must be patch, point to already existing content
						assJsonFile = new File(new File(new File(toDir, chapter.id), assDef.id), "assessments.json");
					}
					if (!assJsonFile.exists()) {
						Log.w(TAG, "assessment json not found, skipping - " + assJsonFile.toString());
						continue;
					}
					final AssessmentFileModel assFileDef = gson.fromJson(Util.openJSONFile(assJsonFile), AssessmentFileModel.class);
					if (assFileDef.questions.length == 0)
						continue;// ignore if no questions
					// fill question text
					for (int qIndex = 0; qIndex < assFileDef.questions.length; qIndex++) {
						String questionType = assFileDef.questions[qIndex].type;
						File questionXmlFile = new File(assJsonFile.getParentFile(), assFileDef.questions[qIndex].id + "/question.xml");
						BaseQuestion q;
						if (QuestionFragmentFactory.QUESTION_TYPE_MCQ.equals(questionType)
								|| QuestionFragmentFactory.QUESTION_TYPE_TORF.equals(questionType)) {
							q = new MCQ_QuestionXmlParser().getQuestionFromXml(questionXmlFile, null);
						} else if (QuestionFragmentFactory.QUESTION_TYPE_LABEL.equals(questionType)) {
							q = new Label_QuestionXmlParser().getQuestionFromXml(questionXmlFile, null);
						} else if (QuestionFragmentFactory.QUESTION_TYPE_FILLBLANK.equals(questionType)) {
							q = new FillBlank_QuestionXmlParser().getQuestionFromXml(questionXmlFile, null);
						} else if (QuestionFragmentFactory.QUESTION_TYPE_MATCH.equals(questionType)) {
							q = new Match_QuestionXmlParser().getQuestionFromXml(questionXmlFile, null);
						} else {
							Log.w(TAG, "unknown question type!" + questionType);
							throw new RuntimeException("Unknown question type");
						}
						assFileDef.questions[qIndex].text = q.questionText;
						assFileDef.questions[qIndex].metadata = q.metadata;
					}
					assFileDef.assessmentId = assDef.id;
					assFileDef.randomizerSeed = System.currentTimeMillis();
					assFileDef.content = new Gson().toJson(assFileDef);
					chapter.assessmentNodes.add(assFileDef);
				}
			}
			// validate that we have correct book.
			if (updateDef.isDropboxImport
					|| (updateDef != null && updateDef.bookId.equals(bookDef.bookId) && updateDef.courseId.equals(bookDef.courseId) && updateDef.bookVersion == bookDef.version)) {
				if (LogConfig.DEBUG_CONTENT_IMPORT)
					Log.d(TAG, "We got correct book, copy files and update DB");

				// first refresh referenced apps
				int deleted = getContentResolver().delete(AllowedAppsProvider.Apps.CONTENT_URI,
						AllowedAppsProvider.Apps.COLUMN_COURSE_ID + " = ? AND " + AllowedAppsProvider.Apps.COLUMN_BOOK_ID + " = ? ",
						new String[] { bookDef.courseId, bookDef.bookId });
				if (LogConfig.DEBUG_CONTENT_IMPORT) {
					Log.d(TAG, "deleted apps: " + deleted);
					Log.d(TAG, "begin apps entry insert: ");
				}
				// now insert all apps
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (int chIndex = 0; chIndex < bookDef.chapters.length; chIndex++) {
					ChapterDefinition chapterNode = bookDef.chapters[chIndex];
					for (int topIndex = 0; topIndex < chapterNode.topicNodes.size(); topIndex++) {
						TopicNode topicNode = chapterNode.topicNodes.get(topIndex);
						Topic t = new Gson().fromJson(topicNode.content, Topic.class);
						if (t.vms != null && t.vms.length > 0) {
							for (VM vm : t.vms) {
								if (LogConfig.DEBUG_CONTENT_IMPORT)
									Log.d(TAG, "inserting vm - " + vm.title + ", src: " + vm.src);
								File apkFile = null;
								if (vm.src != null)
									apkFile = new File(new File(new File(toDir, chapterNode.id), topicNode.id), vm.src);
								ContentValues appCV = new ContentValues();
								appCV.put(AllowedAppsProvider.Apps.COLUMN_COURSE_ID, bookDef.courseId);
								appCV.put(AllowedAppsProvider.Apps.COLUMN_BOOK_ID, bookDef.bookId);
								appCV.put(AllowedAppsProvider.Apps.COLUMN_NAME, vm.title);
								appCV.put(AllowedAppsProvider.Apps.COLUMN_PACKAGE, vm.appPackage);
								appCV.put(AllowedAppsProvider.Apps.COLUMN_VERSION_CODE, vm.appVersionCode);
								appCV.put(AllowedAppsProvider.Apps.COLUMN_SHOW_IN_APPS, vm.showInApps ? 1 : 0);
								appCV.put(AllowedAppsProvider.Apps.COLUMN_APK_PATH, apkFile == null ? "" : apkFile.getAbsolutePath());
								ops.add(ContentProviderOperation.newInsert(AllowedAppsProvider.Apps.CONTENT_URI).withValues(appCV).build());
							}
						}
					}
				}
				if (ops.size() > 0) {
					getContentResolver().applyBatch(AllowedAppsProvider.AUTHORITY, ops);
				}
				// end ref apps

				DatabaseHelper.getInstance(this).resetBook(bookDef);
				boolean bookSuccess = DatabaseHelper.getInstance(this).insertBook(bookDef, new Gson().toJson(bookTags));

				if (bookSuccess) {
					// copy the resources
					File fromDir = updateFolder;
					// replace
					if (updateDef.strategy.equalsIgnoreCase("replace")) {
						if (LogConfig.DEBUG_CONTENT_IMPORT)
							Log.d(TAG, "Strategy: replace, deleting folder");
						// EBUSY bug fix
						if (toDir.exists()) {
							final File to = new File(toDir.getAbsolutePath() + System.currentTimeMillis());
							boolean renameSuccess = toDir.renameTo(to);
							if (LogConfig.DEBUG_CONTENT_IMPORT)
								Log.d(TAG, "rename(delete)? " + renameSuccess);
							Util.deleteRecursive(to);
						}
						// end bug fix
					} else {// patch
						// TODO: prune book after download
					}
					if (LogConfig.DEBUG_CONTENT_IMPORT)
						Log.d(TAG, "to exists? " + toDir.exists());
					// now copy
					// boolean madeDirs = toDir.mkdirs();
					// if (Config.DEBUG) {
					// Log.d(TAG, "made dirs? " + madeDirs);
					// Log.d(TAG, "copying files...");
					// Log.d(TAG, "from:" + fromDir.toString());
					// Log.d(TAG, "to:" + toDir.toString());
					// }
					boolean renameSuccess = Util.moveFolder(fromDir, toDir);
					// Util.copyFolder(fromDir, toDir);
					if (LogConfig.DEBUG_CONTENT_IMPORT) {
						Log.d(TAG, "from exists? " + fromDir.exists());
						Log.d(TAG, "to exists? " + toDir.exists());
						Log.d(TAG, "renameSuccess? " + renameSuccess);
					}
				}
				mHandler.postAtFrontOfQueue(new Runnable() {
					@Override
					public void run() {
						contentUpdateManager.importCompleted();
					}
				});
			} else {
				if (LogConfig.DEBUG_CONTENT_IMPORT)
					Log.d(TAG, "update file validation failed");
				mHandler.postAtFrontOfQueue(new Runnable() {
					@Override
					public void run() {
						contentUpdateManager.importFailed("Update file validation failed( id/version mismatch?)");
					}
				});
			}
			// } catch (IOException ioe) {
			// Log.w(TAG, "copy failed", ioe);
			// final String msg = ioe.getMessage();
			// mHandler.postAtFrontOfQueue(new Runnable() {
			// @Override
			// public void run() {
			// contentUpdateManager.importFailed(msg);
			// }
			// });
		} catch (Exception e) {
			Log.w(TAG, "import failed", e);
			final String msg = e.getMessage();
			mHandler.postAtFrontOfQueue(new Runnable() {
				@Override
				public void run() {
					contentUpdateManager.importFailed(msg);
				}
			});
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "killing");
		stopForeground(true);
	}
}
