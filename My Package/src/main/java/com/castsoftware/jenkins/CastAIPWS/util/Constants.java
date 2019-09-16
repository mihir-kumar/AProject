package com.castsoftware.jenkins.CastAIPWS.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Constants
{
	static public final String wsVersionCompatibility = "1.5";
	static public final String wsBuildCompatibility = "18";
	static public final DateFormat dateFormatVersion = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
	static public final DateFormat castDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	static public final int  RunBackup = 0;
	static public final int  RunDMT = 1;
	static public final int  RunAcceptDelivery = 2;
	static public final int  RunAnalysis = 3;
	static public final int  RunSnapshot = 4;
	static public final int  RunValidation = 5;
	static public final int  RunPublishAAD = 6;
	static public final int  RunSnapshotReport = 7;
	static public final int  RunDatabaseOptimize = 8;
	static public final int  RunArchiveDelivery = 9;

	static public final int  RunSpecialFunctions = 20;
  static public final int  RunArchiveException = 20;

	
  static public final String AOP_TAB_INITIAL = "ADP Flagged";
  static public final String AOP_TAB_PACKAGE = "DMT";
  static public final String AOP_TAB_ACCEPT = "Accept";
  static public final String AOP_TAB_ANALYZE = "Analysis";
  static public final String AOP_TAB_VALIDATE = "Validation";
  static public final String AOP_TAB_PUBLISH = "Report";
  static public final String AOP_TAB_DONE = "Rescan Success";

  static public final String AOP_STATUS_START  = "Process Started";
  static public final String AOP_STATUS_IN_PROGRESS = "Processing";
  static public final String AOP_STATUS_ERROR = "Processing Error";
  static public final String AOP_STATUS_OK = "Process Completed";
  static public final String AOP_STATUS_CONTINUE = "Process Continuing";
	
	//Jenkins Environment Variable Id's
	static public final String START_AT="START_AT";
	static public final String CAST_DATE="CAST_DATE";
	static public final String DMT_WEB_SERVICE_ADDRESS="CAST_DMT_WEB_SERVICE_ADDRESS";
	static public final String CMS_WEB_SERVICE_ADDRESS="CAST_CMS_WEB_SERVICE_ADDRESS";
	static public final String CMS_WEB_SERVICE_ADDRESS1="CAST_CMS_WEB_SERVICE_ADDRESS1";
	static public final String CMS_WEB_SERVICE_ADDRESS2="CAST_CMS_WEB_SERVICE_ADDRESS2";
	static public final String CMS_WEB_SERVICE_ADDRESS3="CAST_CMS_WEB_SERVICE_ADDRESS3";
	static public final String CMS_WEB_SERVICE_ADDRESS4="CAST_CMS_WEB_SERVICE_ADDRESS4";
	static public final String CMS_WEB_SERVICE_ADDRESS5="CAST_CMS_WEB_SERVICE_ADDRESS5";
	static public final String APPLICATION_NAME="CAST_APPL_NAME";
  static public final String VERSION_NAME="CAST_APPL_VERSION";
  static public final String VERSION_NAME_OVERRIDE="VERSION_NAME_OVERRIDE";
	static public final String VERSION_NAME_PROD="CAST_APPL_VERSION_PROD";
	static public final String CONNECTION_PROFILE="CAST_CONNECTION_PROFILE";
	static public final String SCHEMA_PREFIX="CAST_SCHEMA_PREFIX";
	static public final String AAD_SCHEMA_NAME="CAST_AAD_SCHEMA_NAME";
	static public final String CAPTURE_DATE="CAST_CAPTURE_DATE";	
	static public final String RETENTION_POLICY="CAST_SNAPSHOT_RETENTION_POLICY";	
	static public final String SNAPSHOT_NAME="CAST_SNAPSHOT_NAME";	
	static public final String WORK_FLOW="CAST_WORK_FLOW";
  static public final String RESCAN_TYPE="RESCAN_TYPE";
  static public final String BLOCK_STEP="BLOCK";

	// build flags
	static public final String RUN_BACKUP="CAST_RUN_BACKUP";
	static public final String RUN_DELIVERY_APPLICATION="CAST_DELIVERY_APPLICATION";
	static public final String RUN_ACCEPT_DELIVERY="CAST_ACCEPT_DELIVERY";
	static public final String RUN_ANALYSIS="CAST_RUN_ANALYSIS";
	static public final String REFERENCE_VERSION="CAST_REFERENCE_VERSION";
	static public final String REFERENCE_VERSION_PROD="CAST_REFERENCE_VERSION_PROD";
	static public final String RUN_SNAPSHOT="CAST_RUN_SNAPSHOT";
	static public final String RUN_VALIDATION="CAST_RUN_SNAPSHOT_VALIDATION";
	static public final String RUN_PUBLISH_SNAPSHOT="CAST_PUBLISH_SNAPSHOT";
	static public final String RUN_OPTIMIZE_DATABASE="CAST_OPTIMIZE_DATABASE";
	static public final String RUN_ARCHIVE_DELIVERY="CAST_ARCHIVE_DELIVERY";	
	static public final String RUN_JNLP_DELIVERY="CAST_RUN_JNLP_DELIVERY";	
}
