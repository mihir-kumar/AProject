package com.castsoftware.jenkins.CastAIPWS.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.axis.AxisFault;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.castsoftware.batch.CastWebService;
import com.castsoftware.exception.HelperException;
import com.castsoftware.util.CastUtil;
import com.castsoftware.util.VersionInfo;
import com.castsoftware.webservice.RemoteHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

public class Utils
{

//	static public boolean runJobs(AbstractBuild build, Launcher launcher, BuildListener listener, Class thisClass,
//			int currentStep) throws IOException, InterruptedException
//	{
//		StackTraceElement[] elementList = Thread.currentThread().getStackTrace();
//		if (elementList.length > 3 && elementList[3].getMethodName().equals("runJobs")) return true;
//
//		for (Builder b : Utils.getBuilderList(Utils.getJobTaskList(build, listener), build, listener, currentStep,
//				thisClass)) {
//
//			if (!b.perform(build, launcher, listener)) {
//				return false;
//			}
//		}
//		return true;
//	}

	static public List<Element> getJobTaskList(AbstractBuild build, BuildListener listener) throws IOException,
			InterruptedException
	{
		List<Element> retList = new ArrayList<Element>();

		// EnvVars envVars = build.getEnvironment(listener);
		String jobName = build.getProject().getName();

		AbstractProject thisJob = null;
		for (AbstractProject prj : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
			if (prj.getDisplayName().equalsIgnoreCase(jobName)) {
				thisJob = prj;
				break;
			}
		}
		if (thisJob != null) {
			XmlFile config = thisJob.getConfigFile();
			SAXBuilder builder = new SAXBuilder();

			Document document;
			try {
				document = builder.build(config.getFile());
				Element bldrs = Utils.findElement(document.getRootElement(), "builders");
				if (bldrs != null) {
					List<Element> bldrList = bldrs.getChildren();
					for (Element el : bldrList) {
						String name = el.getName();
						if (name.contains(".CastAIPWS.")) {
							retList.add(el);
						}
					}
				}
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			listener.getLogger().println(String.format("Error locating job configuration: %s",jobName));
		}
		return retList;
	}

//	static public List<Builder> getBuilderList(List<Element> taskList, AbstractBuild build, BuildListener listener,
//			int currentStep, Class thisClass) throws IOException, InterruptedException
//	{
//		List<Builder> builders = new ArrayList<Builder>();
//		EnvVars envVars = build.getEnvironment(listener);
//
//		boolean backup = Boolean.parseBoolean(envVars.get(Constants.RUN_BACKUP));
//		boolean da = Boolean.parseBoolean(envVars.get(Constants.RUN_DELIVERY_APPLICATION));
//		boolean ad = Boolean.parseBoolean(envVars.get(Constants.RUN_ACCEPT_DELIVERY));
//		boolean ra = Boolean.parseBoolean(envVars.get(Constants.RUN_ANALYSIS));
//		boolean rs = Boolean.parseBoolean(envVars.get(Constants.RUN_SNAPSHOT));
//		boolean archive = Boolean.parseBoolean(envVars.get(Constants.RUN_ARCHIVE_DELIVERY));
//		boolean rav = Boolean.parseBoolean(envVars.get(Constants.RUN_VALIDATION));
//		boolean publish = Boolean.parseBoolean(envVars.get(Constants.RUN_PUBLISH_SNAPSHOT));
//		boolean optimize = Boolean.parseBoolean(envVars.get(Constants.RUN_OPTIMIZE_DATABASE));
//
//		if (currentStep < Constants.RunBackup && backup
//				&& Utils.canRunTask(taskList, CastAIPCSSBackupBuilder.class, thisClass)) {
//			builders.add(new CastAIPCSSBackupBuilder());
//		} else if (backup && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunDMT && da && Utils.canRunTask(taskList, CastAIPDeliverBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPCSSBackupBuilder.class, thisClass)) {
//				builders.add(new CastAIPDeliverBuilder());
//			} else {
//				return builders;
//			}
//		} else if (ad && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunAcceptDelivery && ad
//				&& Utils.canRunTask(taskList, CastAIPAcceptBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPDeliverBuilder.class, thisClass)) {
//				builders.add(new CastAIPAcceptBuilder());
//			} else {
//				return builders;
//			}
//		} else if (ad && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunAnalysis && ra
//				&& Utils.canRunTask(taskList, CastAIPAnalyzeBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPAcceptBuilder.class, thisClass)) {
//				builders.add(new CastAIPAnalyzeBuilder());
//			} else {
//				return builders;
//			}
//		} else if (ra && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunSnapshot && rs
//				&& Utils.canRunTask(taskList, CastAIPSnapshotBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPAnalyzeBuilder.class, thisClass)) {
//				builders.add(new CastAIPSnapshotBuilder());
//			} else {
//				return builders;
//			}
//		} else if (rs && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunValidation && rav
//				&& Utils.canRunTask(taskList, CastAIPValidationBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPSnapshotBuilder.class, thisClass)) {
//				builders.add(new CastAIPValidationBuilder());
//			} else {
//				return builders;
//			}
//		} else if (rav && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunPublishAAD && publish
//				&& Utils.canRunTask(taskList, CastAIPPublishBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPValidationBuilder.class, thisClass)) {
//				builders.add(new CastAIPPublishBuilder());
//			} else {
//				return builders;
//			}
//		} else if (publish && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunDatabaseOptimize && optimize
//				&& Utils.canRunTask(taskList, CastAIPOptimizeDatabaseBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPPublishBuilder.class, thisClass)) {
//				builders.add(new CastAIPOptimizeDatabaseBuilder());
//			} else {
//				return builders;
//			}
//		} else if (optimize && builders.size() > 0) {
//			return builders;
//		}
//
//		if (currentStep < Constants.RunArchiveDelivery && archive
//				&& Utils.canRunTask(taskList, CastAIPArchiveDeliveryBuilder.class, thisClass)) {
//			if (Utils.canRunTask(taskList, CastAIPOptimizeDatabaseBuilder.class, thisClass)) {
//				builders.add(new CastAIPArchiveDeliveryBuilder());
//			} else {
//				return builders;
//			}
//		} else if (archive && builders.size() > 0) {
//			return builders;
//		}
//
//		builders.add(new CastAIPFinalBuilder());
//		return builders;
//	}

	static public Element findElement(Element current, String elementName)
	{
		if (current.getName() == elementName) {
			return current;
		}
		List children = current.getChildren();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			Element child = (Element) iterator.next();
			Element tempElement = findElement(child, elementName);
			if (tempElement != null) {
				return tempElement;
			}
		}
		// Didn't find the element anywhere.
		return null;
	}

	static public boolean canRunTask(List<Element> taskList, Class lookFor, Class base)
	{
		Boolean found = false;
		for (Element el : taskList) {
			if (el.getName().equals(base.getName())) continue;
			if (lookFor.getName().equals(el.getName())) {
				found = true;
				break;
			}
		}
		return !found;
	}

	static public boolean validateBuildVariables(AbstractBuild build, BuildListener listener)
	{
		boolean rslt = true;
		EnvVars envVars;
		try {
			envVars = build.getEnvironment(listener);
			// make sure everything is set properly
			if (envVars.get(Constants.DMT_WEB_SERVICE_ADDRESS).isEmpty()) {
				listener.getLogger().printf("%s is not set", Constants.DMT_WEB_SERVICE_ADDRESS);
				rslt = false;
			}
			if (envVars.get(Constants.CMS_WEB_SERVICE_ADDRESS).isEmpty()) {
				listener.getLogger().printf("%s is not set", Constants.CMS_WEB_SERVICE_ADDRESS);
				rslt = false;
			}
			if (envVars.get(Constants.VERSION_NAME).isEmpty()) {
				listener.getLogger().printf("%s is not set", Constants.VERSION_NAME);
				rslt = false;
			}
			if (envVars.get(Constants.CONNECTION_PROFILE).isEmpty()) {
				listener.getLogger().printf("%s is not set", Constants.CONNECTION_PROFILE);
				rslt = false;
			}
			// if (envVars.get(Constants.CAPTURE_DATE).isEmpty()) {
			// listener.getLogger().printf("%s is not set",
			// Constants.CAPTURE_DATE);
			// rslt = false;
			// }
			if (envVars.get(Constants.SNAPSHOT_NAME).isEmpty()) {
				listener.getLogger().printf("%s is not set", Constants.SNAPSHOT_NAME);
				rslt = false;
			}
			// if (envVars.get(Constants.TODAY).isEmpty()) {
			// listener.getLogger().printf("%s is not set", Constants.TODAY);
			// rslt = false;
			// }
			if (envVars.get(Constants.WORK_FLOW).isEmpty()) {
				listener.getLogger().printf("%s is not set", Constants.WORK_FLOW);
				rslt = false;
			}
		} catch (IOException | InterruptedException e) {
			listener.getLogger().println(e.getMessage());
			rslt = false;
		}
		return rslt;
	}

	static public boolean getLog(CastWebService cbws, int taskId, long startTime, BuildListener listener)
	{
		int returnCode;
		int feedbackCounter;
		int logIndex;
		String logString;
		Gson gson = new Gson();
		Type collectionType = new TypeToken<Collection<String>>()
		{
		}.getType();
		Collection<String> tmp;
		List<String> logLines;

		try {
			feedbackCounter = 0;
			logIndex = 0;
			int retryCount = 0;
			while (true)
			{
				try 
				{
					do {
						// Get Log
						Thread.sleep(1000);
						
						logString = cbws.getTaskOutput(taskId, logIndex);
						tmp = gson.fromJson(logString, collectionType);
						if (tmp != null) {
							logLines = new ArrayList<String>(tmp);
							logIndex += logLines.size();
							for (String s : logLines)
								listener.getLogger().println(s);
		
							feedbackCounter++;
							if (feedbackCounter % 300 == 0)
								listener.getLogger().println(
										String.format(" %s...", CastUtil.formatNanoTime(System.nanoTime() - startTime)));
						}
					} while (cbws.isTaskRunning(taskId));
					break;
				} catch (AxisFault af) {
					retryCount++;
					if (retryCount<3)
					{
						listener.getLogger().printf("%d Access fault encountered\n",retryCount);
					} else {
				 	 	listener.getLogger().println("Aborging, too many access faults encounerd.");
				 	 	return false;
					}
				}
			}
			
			// Get Log
			 retryCount = 0;
			 while (true)
			{
				try 
				{
					logString = cbws.getTaskOutput(taskId, logIndex);
					tmp = gson.fromJson(logString, collectionType);
					if (tmp != null) {
						logLines = new ArrayList<String>(tmp);
						logIndex += logLines.size();
						for (String s : logLines)
							listener.getLogger().println(s);
					}
					break;
				} catch (AxisFault af) {
					retryCount++;
					if (retryCount<3)
					{
						listener.getLogger().printf("%d Access fault encountered\n",retryCount);
					} else {
				 	 	listener.getLogger().println("Aborging, too many access faults encounerd.");
				 	 	return false;
					}
				}
			}
			
			listener.getLogger().println(
					String.format("Duration: %s", CastUtil.formatNanoTime(System.nanoTime() - startTime)));

			returnCode = cbws.getTaskExitValue(taskId);

			listener.getLogger().println(String.format("Return Code: %d", returnCode));
		} catch (RemoteException | InterruptedException e) {
			listener.getLogger().printf("Web Service Connection Error: %s", e.getMessage());
			return false;
		}

		listener.getLogger().println(" ");

		return (returnCode == 0) ? true : false;
	}

	static public boolean validateWebServiceVersion(String webServiceAddress, BuildListener listener)
			throws HelperException
	{
		VersionInfo vi = RemoteHelper.getVersionInfo(webServiceAddress);
		if (Constants.wsVersionCompatibility.equals(vi.getVersion())) {
			return true;
		} else {
			listener.getLogger().println(
					String.format("Incompatible Web Service Version %s (Supported: %s)", vi.getVersion(),
							Constants.wsVersionCompatibility));
			return false;
		}
	}

	static public Calendar convertCastDate(String castDate) throws ParseException
	{
		Calendar cal = Calendar.getInstance();
		Date dateForToday = Constants.castDateFormat.parse(castDate);
		cal.setTime(dateForToday);
		return cal;
	}
}
