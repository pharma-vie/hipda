package net.jejer.hipda.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.jejer.hipda.async.AvatarUrlTask;
import net.jejer.hipda.ui.ThreadListAdapter;
import net.jejer.hipda.utils.HiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AvatarUrlCache {

	private final static String LOG_TAG = AvatarUrlCache.class.getSimpleName();

	private final static String PREFS_NAME = "AvatarPrefsFile";
	private final static String AVATAR_URL_PREFIX = HiUtils.BaseUrl + "uc_server/data/avatar";

	private SharedPreferences avatarPrefs;
	private final int MAX_ENTRIES = 4096;
	private LRUCache<String, String> avatarMap;
	private Context mCtx;

	private Collection<String> mDirtyUids = new ArrayList<String>();
	private boolean mUpdated = false;
	private long lastSaveTime = 0;


	private static AvatarUrlCache ourInstance = new AvatarUrlCache();

	public static AvatarUrlCache getInstance() {
		return ourInstance;
	}

	private AvatarUrlCache() {
	}


	public void init(Context ctx) {
		mCtx = ctx;

		avatarMap = new LRUCache<String, String>(MAX_ENTRIES);
		avatarPrefs = mCtx.getSharedPreferences(PREFS_NAME, 0);

		load();
	}

	private void load() {
		Map<String, ?> keys = avatarPrefs.getAll();
		for (Map.Entry<String, ?> entry : keys.entrySet()) {
			avatarMap.put(entry.getKey(), entry.getValue().toString());
		}
	}

	public void put(String uid, String url) {
		if (url == null)
			url = "";
		if (url.startsWith(AVATAR_URL_PREFIX)) {
			url = url.substring(AVATAR_URL_PREFIX.length());
		}
		avatarMap.put(uid, url);
		mDirtyUids.remove(uid);
		mUpdated = true;
		save();
	}

	public String get(String uid) {
		if (!avatarMap.containsKey(uid)) {
			return "";
		}
		String url = avatarMap.get(uid);
		if (url.startsWith("/")) {
			url = AVATAR_URL_PREFIX + url;
		}
		return url;
	}

	public void markDirty(String uid) {
		if (!avatarMap.containsKey(uid) && !mDirtyUids.contains(uid)) {
			mDirtyUids.add(uid);
		}
	}

	public void fetchAvatarUrls(ThreadListAdapter threadListAdapter) {
		if (mDirtyUids.size() > 0) {
			Collection<String> temp = new ArrayList<String>();
			temp.addAll(mDirtyUids);
			mDirtyUids.clear();
			new AvatarUrlTask(mCtx, threadListAdapter, temp).execute();
		}
	}

	public boolean contains(String uid) {
		return avatarMap.containsKey(uid);
	}

	public boolean isUpdated() {
		return mUpdated;
	}

	public void setUpdated(boolean updated) {
		mUpdated = updated;
	}

	private void save() {
		long start = System.currentTimeMillis();
		if (start - lastSaveTime > 30 * 1000) {
			Collection<Map.Entry<String, String>> entries = avatarMap.getAll();
			SharedPreferences.Editor prefsWriter = avatarPrefs.edit();
			prefsWriter.clear();
			for (Map.Entry<String, String> entry : entries) {
				prefsWriter.putString(entry.getKey(), entry.getValue());
			}
			prefsWriter.apply();
			lastSaveTime = System.currentTimeMillis();
			Log.v(LOG_TAG, "save avatarPrefs, size=" + avatarMap.usedEntries() + ", time used : " + (System.currentTimeMillis() - start) + " ms");
		}
	}

}
