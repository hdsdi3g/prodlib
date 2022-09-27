package tv.hd3g.jobkit.engine;

interface SpoolJobStatus {

	String getJobName();

	String getSpoolName();

	int getJobPriority();

	Supervisable getSupervisable();

}