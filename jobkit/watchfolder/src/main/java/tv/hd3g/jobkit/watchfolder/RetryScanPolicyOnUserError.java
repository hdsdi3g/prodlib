package tv.hd3g.jobkit.watchfolder;

public enum RetryScanPolicyOnUserError {

	/**
	 * Let founded file in database, but don't resend it to FolderActivity.onAfterScan
	 */
	IGNORE_FOUNDED_FILE,
	/**
	 * Reset status for founded file in database: it will be resend to FolderActivity.onAfterScan (and we hope that's time, the user process will be ok)
	 */
	RETRY_FOUNDED_FILE;

}
