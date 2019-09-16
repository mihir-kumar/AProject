package com.castsoftware.jenkins.CastAIPWS;

import java.io.IOException;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.rpc.ServiceException;
import javax.xml.soap.SOAPException;

import org.apache.axis.AxisFault;
import org.apache.axis.utils.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.castsoftware.batch.CastWebService;
import com.castsoftware.batch.CastWebServiceServiceLocator;
import com.castsoftware.exception.HelperException;
import com.castsoftware.jenkins.CastAIPWS.util.AdBlock;
import com.castsoftware.jenkins.CastAIPWS.util.ArchiveBlock;
import com.castsoftware.jenkins.CastAIPWS.util.AseBlock;
import com.castsoftware.jenkins.CastAIPWS.util.BackupBlock;
import com.castsoftware.jenkins.CastAIPWS.util.Constants;
import com.castsoftware.jenkins.CastAIPWS.util.OptimizeBlock;
import com.castsoftware.jenkins.CastAIPWS.util.PublishBlock;
import com.castsoftware.jenkins.CastAIPWS.util.RaBlock;
import com.castsoftware.jenkins.CastAIPWS.util.RsBlock;
import com.castsoftware.jenkins.CastAIPWS.util.RsvBlock;
import com.castsoftware.jenkins.CastAIPWS.util.Utils;
import com.castsoftware.jenkins.data.Snapshot;
import com.castsoftware.jenkins.util.PublishEnvVarAction;
import com.castsoftware.profiles.ConnectionProfile;
import com.castsoftware.restapi.JsonResponse;
import com.castsoftware.util.CastUtil;
import com.castsoftware.util.VersionInfo;
import com.castsoftware.vps.ValidationProbesService;
import com.castsoftware.webservice.RemoteHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class CastAIPBuilder extends Builder {
  private final String               dmtWebServiceAddress;
  private String                     cmsWebServiceAddress;
  private final String               cmsWebServiceAddress1;
  private final String               cmsWebServiceAddress2;
  private final String               cmsWebServiceAddress3;
  private final String               cmsWebServiceAddress4;
  private final String               cmsWebServiceAddress5;
  private final String               appName;
  private final String               versionName;
  private String                     referenceVersion;

  // private final String referenceVersionPROD;

  private final String               castMSConnectionProfile;
  private final String               schemaPrefix;
  private final String               aadSchemaName;
  private final String               retentionPolicy;
  private String                     lastRunDate;

  private final boolean              backup;
  private final boolean              da;
  private final boolean              ad;
  private final boolean              ra;
  private final boolean              rs;
  private final boolean              archive;
  private final boolean              rav;
  private final boolean              publish;
  private final boolean              optimize;
  private final boolean              useJnlp;
  private final boolean              forceStop;
  private final boolean              aseBlock;

  public static Map<String, Integer> map     = new HashMap<String, Integer>();

  // private final String snapshotName;
  // private final String captureDate;
  private final String               workFlow;

  /**
   * Internal variables
   */
  private Date                       startingDate;
  private Date                       endingDate;
  private String                     rescanType;
  private boolean                    pqEnabled;
  private boolean                    runAnalysis;
  private int                        startAt = 0;
  private List<Snapshot>             snapshotList;

  // Fields in config.jelly must match the parameter names in the
  // "DataBoundConstructor"
  @DataBoundConstructor
  public CastAIPBuilder(String dmtWebServiceAddress, String cmsWebServiceAddress1, String cmsWebServiceAddress2,
      String cmsWebServiceAddress3, String cmsWebServiceAddress4, String cmsWebServiceAddress5, String appName,
      String referenceVersion, String versionName, String castMSConnectionProfile, BackupBlock backupBlock,
      AseBlock aseBlock, AdBlock daBlock, AdBlock adBlock, RaBlock raBlock, RsBlock rsBlock, RsvBlock ravBlock,
      PublishBlock publishBlock, ArchiveBlock archiveBlock, OptimizeBlock optimizeBlock, String workFlow,
      String schemaPrefix, String aadSchemaName, String lastRunDate, String retentionPolicy, boolean useJnlp,
      boolean forceStop)
  {
    this.dmtWebServiceAddress = dmtWebServiceAddress;
    this.cmsWebServiceAddress1 = cmsWebServiceAddress1;
    this.cmsWebServiceAddress2 = cmsWebServiceAddress2;
    this.cmsWebServiceAddress3 = cmsWebServiceAddress3;
    this.cmsWebServiceAddress4 = cmsWebServiceAddress4;
    this.cmsWebServiceAddress5 = cmsWebServiceAddress5;
    this.appName = appName;
    this.versionName = versionName;
    this.referenceVersion = referenceVersion;
    this.castMSConnectionProfile = castMSConnectionProfile;
    this.schemaPrefix = schemaPrefix;
    this.aadSchemaName = aadSchemaName;
    this.workFlow = workFlow;
    this.lastRunDate = lastRunDate;
    this.useJnlp = useJnlp;
    this.forceStop = forceStop;

    map.put("a", 1);

    // Automatically start exceptions
    if (aseBlock != null)
    {
      this.aseBlock = true;
    } else
    {
      this.aseBlock = false;
    }

    // Publish snapshot
    if (backupBlock != null)
    {
      this.backup = true;
    } else
    {
      this.backup = false;
    }

    // Deliver Code
    if (daBlock != null)
    {
      this.da = true;
    } else
    {
      this.da = false;
    }

    // Accept Delivery
    if (adBlock != null)
    {
      this.ad = true;
    } else
    {
      this.ad = false;
    }

    // Run Analysis
    if (raBlock != null)
    {
      this.ra = true;
    } else
    {
      this.ra = false;
    }

    // Run Snapshot
    if (rsBlock != null)
    {
      this.retentionPolicy = retentionPolicy;
      this.rs = true;
    } else
    {
      this.retentionPolicy = "";
      this.rs = false;
    }

    // Run Analysis Validation
    if (ravBlock != null)
    {
      this.rav = true;
    } else
    {
      this.rav = false;
    }

    // Publish snapshot
    if (publishBlock != null)
    {
      this.publish = true;
    } else
    {
      this.publish = false;
    }

    // Publish snapshot
    if (optimizeBlock != null)
    {
      this.optimize = true;
    } else
    {
      this.optimize = false;
    }

    // Archive Delivery
    if (archiveBlock != null)
    {
      this.archive = true;
    } else
    {
      this.archive = false;
    }
  }

  public boolean isUseJnlp()
  {
    return useJnlp;
  }

  private Boolean checkWebServiceCompatibility(String version)
  {
    return Constants.wsVersionCompatibility.equals(version);
  }

  public String getDmtWebServiceAddress()
  {
    return dmtWebServiceAddress;
  }

  public String getCmsWebServiceAddress()
  {
    String retVal = "";
    if (cmsWebServiceAddress == null || cmsWebServiceAddress.isEmpty())
    {
      retVal = dmtWebServiceAddress;
    } else
    {
      retVal = cmsWebServiceAddress;
    }
    return retVal;
  }

  public void setCmsWebServiceAddress(String cmsWebServiceAddress)
  {
    this.cmsWebServiceAddress = cmsWebServiceAddress;
  }

  public String getCmsWebServiceAddress1()
  {
    String retVal = "";
    if (cmsWebServiceAddress1 == null || cmsWebServiceAddress1.isEmpty())
    {
      retVal = dmtWebServiceAddress;
    } else
    {
      retVal = cmsWebServiceAddress1;
    }
    return retVal;
  }

  public String getCmsWebServiceAddress2()
  {
    return cmsWebServiceAddress2;
  }

  public String getCmsWebServiceAddress3()
  {
    return cmsWebServiceAddress3;
  }

  public String getCmsWebServiceAddress4()
  {
    return cmsWebServiceAddress4;
  }

  public String getCmsWebServiceAddress5()
  {
    return cmsWebServiceAddress5;
  }

  public String getAadSchemaName()
  {
    return aadSchemaName;
  }

  public String getSchemaPrefix()
  {
    return schemaPrefix.toLowerCase();
  }

  public String getAppName()
  {
    return appName;
  }

  public String getVersionName()
  {
    return versionName;
  }

  public String getReferenceVersion()
  {
    try
    {
      CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
      cbwsl.setCastWebServicePortEndpointAddress(getDmtWebServiceAddress());
      CastWebService cbws;
      cbws = cbwsl.getCastWebServicePort();
      referenceVersion = cbws.getPrevDmtVersion(getAppName());
    } catch (ServiceException | RemoteException e)
    {
      return "Can't access CBWS";
    }
    return referenceVersion;
  }

  // public String getReferenceVersionPROD()
  // {
  // if (referenceVersionPROD == null)
  // {
  // return "Ref";
  // } else
  // {
  // return referenceVersionPROD;
  // }
  // }

  public String getCastMSConnectionProfile()
  {
    return castMSConnectionProfile;
  }

  public boolean isBackup()
  {
    return backup;
  }

  public boolean isDa()
  {
    return da;
  }

  public boolean isAaws()
  {
    return da;
  }

  public boolean isOptimize()
  {
    return optimize;
  }

  public boolean isAd()
  {
    return ad;
  }

  public boolean isRa()
  {
    return ra;
  }

  public boolean isRs()
  {
    return rs;
  }

  public boolean isRav()
  {
    return rav;
  }

  public boolean isArchive()
  {
    return archive;
  }

  public String getRetentionPolicy()
  {
    return retentionPolicy == null ? "" : retentionPolicy;
  }

  public boolean isPublish()
  {
    return publish;
  }

  public boolean isForceStop()
  {
    return forceStop;
  }

  public String getVersionNameWithTag(Date date, EnvVars envVars)
  {

    // String rescanType;
    // rescanType = envVars.get(Constants.RESCAN_TYPE, "PROD");

    String s = versionName.replace("[TODAY]", Constants.dateFormatVersion.format(date));

    if (pqEnabled && rescanType.equalsIgnoreCase("QA"))
    {
      s = String.format("QA%s", s);
    }

    // EnvTemplater jEnv = new EnvTemplater(envVars);
    // s = jEnv.templateString(s);

    return s;
  }

  public String getWorkFlow()
  {
    return workFlow;
  }

  @SuppressWarnings("rawtypes")
  public boolean publishVariables(AbstractBuild build, BuildListener listener, String castDate)
      throws IOException, InterruptedException
  {
    boolean rslt = true;
    Date dateForToday;
    try
    {
      EnvVars envVars = build.getEnvironment(listener);
      dateForToday = Utils.convertCastDate(castDate).getTime();

      String snapshotName = "Computed on " + Constants.dateFormatVersion.format(dateForToday);

      build.addAction(new PublishEnvVarAction(Constants.CAST_DATE, castDate));
      build.addAction(new PublishEnvVarAction(Constants.DMT_WEB_SERVICE_ADDRESS, getDmtWebServiceAddress()));
      build.addAction(new PublishEnvVarAction(Constants.CMS_WEB_SERVICE_ADDRESS, getCmsWebServiceAddress()));
      build.addAction(new PublishEnvVarAction(Constants.APPLICATION_NAME, getAppName()));
      // build.addAction(
      // new PublishEnvVarAction(Constants.VERSION_NAME,
      // getVersionNameWithTag(dateForToday, envVars, rescanType)));

      boolean nullCheck = StringUtils.isEmpty(rescanType);
      /*
       * if (nullCheck == true) { build.addAction(new
       * PublishEnvVarAction(Constants.VERSION_NAME,
       * getVersionNameWithTag(dateForToday, envVars, rescanType))); } else {
       * build.addAction(new PublishEnvVarAction(Constants.VERSION_NAME,
       * rescanType + getVersionNameWithTag(dateForToday, envVars,
       * rescanType))); }
       */
      build.addAction(new PublishEnvVarAction(Constants.AAD_SCHEMA_NAME, getAadSchemaName()));
      build.addAction(new PublishEnvVarAction(Constants.SCHEMA_PREFIX, getSchemaPrefix()));
      build.addAction(new PublishEnvVarAction(Constants.CONNECTION_PROFILE, getCastMSConnectionProfile()));
      build.addAction(new PublishEnvVarAction(Constants.SNAPSHOT_NAME, snapshotName));

      build.addAction(new PublishEnvVarAction(Constants.RETENTION_POLICY, getRetentionPolicy()));

      build.addAction(new PublishEnvVarAction(Constants.WORK_FLOW, getWorkFlow()));
      build.addAction(new PublishEnvVarAction(Constants.REFERENCE_VERSION, getReferenceVersion()));
      // build.addAction(new
      // PublishEnvVarAction(Constants.REFERENCE_VERSION_PROD,
      // getReferenceVersionPROD()));

      // // build flags
      // build.addAction(new PublishEnvVarAction(Constants.RUN_BACKUP,
      // Boolean.toString(isBackup())));
      // build.addAction(new
      // PublishEnvVarAction(Constants.RUN_DELIVERY_APPLICATION,
      // Boolean.toString(isDa())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_ACCEPT_DELIVERY,
      // Boolean.toString(isAd())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_ANALYSIS,
      // Boolean.toString(isRa())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_SNAPSHOT,
      // Boolean.toString(isRs())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_VALIDATION,
      // Boolean.toString(isRav())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_PUBLISH_SNAPSHOT,
      // Boolean.toString(isPublish())));
      // build.addAction(new
      // PublishEnvVarAction(Constants.RUN_OPTIMIZE_DATABASE,
      // Boolean.toString(isOptimize())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_ARCHIVE_DELIVERY,
      // Boolean.toString(isArchive())));
      // build.addAction(new PublishEnvVarAction(Constants.RUN_JNLP_DELIVERY,
      // Boolean.toString(isUseJnlp())));

      // rslt = Utils.validateBuildVariables(build, listener);

    } catch (ParseException e)
    {
      listener.error("Unable to publish CAST Enviroment Variables: %s", e.getMessage());
      return false;
    }
    return rslt;
  }

  public int getStartAt(EnvVars envVars)
  {
    try
    {
      return Integer.parseInt(envVars.get(Constants.START_AT));
    } catch (NumberFormatException e)
    {
      return 0;
    }

  }

  public String getRescanType(EnvVars envVars)
  {
    return envVars.get(Constants.RESCAN_TYPE, "PROD");
  }

  public String getRescanType()
  {
    return rescanType==null?"PROD":rescanType;
  }

  public String getLastRunDate()
  {
    return lastRunDate;
  }

  public void setLastRunDate(String lastRunDate)
  {
    this.lastRunDate = lastRunDate;
  }

  /**
   * This is the main method the automation process.
   */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException
  {

    startingDate = new Date();

    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);

    // if start at is > zero then calculate the CAST date. Cast date can come
    // from an environment variable or the lastRunDate field found in the jelly
    // file.
    startAt = getStartAt(envVars);
    String lastRunDate = getLastRunDate();

    // handle start at here
    // calculate cast date
    String castDate = envVars.get(Constants.CAST_DATE);
    if (startAt > 0)
    {
      if (castDate == null || castDate.isEmpty())
      {
        if (lastRunDate == null || lastRunDate.isEmpty())
        {
          castDate = Constants.castDateFormat.format(new Date());
        } else
        {
          castDate = lastRunDate;
        }
      } else
      {
        lastRunDate = castDate;
      }
    } else
    { // new run, start at is zero
      castDate = Constants.castDateFormat.format(new Date());
      lastRunDate = castDate;
    }
    setLastRunDate(lastRunDate);

    listener.getLogger().println("****CAST Application Inteligence Platform****");

    listener.getLogger().println(String.format("START_AT: %d", startAt));
    listener.getLogger().println(String.format("CAST_DATE: %s", castDate));

    boolean failBuild = getWorkFlow().trim().toLowerCase().equals("no");

    listener.getLogger().println(String.format("DMT Web Service Address:  %s", getDmtWebServiceAddress()));
    // listener.getLogger().println(String.format("CMS Web Service Address:
    // %s", getCmsWebServiceAddress()));

    try
    {
      try
      {
        // Create Job list and run the first job
        CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
        cbwsl.setCastWebServicePortEndpointAddress(getDmtWebServiceAddress());
        CastWebService cbws = cbwsl.getCastWebServicePort();

        String schemaPrefix = getSchemaPrefix();
        String centralDbName = String.format("%s_central", schemaPrefix);
        String appName = getAppName();

        // Determine if the proactive quality flat is turned on in CBWS
        String strQAScan = cbws.getQAScanFlag();
        pqEnabled = false;
        if (strQAScan != null && strQAScan.toLowerCase().equals("true"))
        {
          pqEnabled = true;
        }

        if (pqEnabled)
        {
          rescanType = getRescanType(envVars);
          listener.getLogger().println("Proactive Quality has been enabled");
          listener.getLogger().println(String.format("RESCAN_TYPE: %s", rescanType));
          cbws.setCurrentScanType(appName, rescanType);
        } else
        {
          listener.getLogger().println(String
              .format("Quality Scans disabled. To enable, change the qa.scan flag to true in the properties file"));
        }

        if (!publishVariables(build, listener, castDate)
            || !Utils.validateWebServiceVersion(getDmtWebServiceAddress(), listener))
        {
          return false;
        }

        listener.getLogger().println(String.format("Sending Jenkins Console URL to AOP"));
        EnvVars envVars1 = build.getEnvironment(listener);

        listener.getLogger().println(String.format("Sent Console URL to AOP"));

        /**
         * Calculate if a snapshot should be run If this is a QA run always run
         * the snapshot If this is a PROD run and there are no prior QA runs
         * then run the snapshot
         */
        runAnalysis = true;
/*        
        String snapshots = cbws.getSnapshotList(centralDbName, appName);
        if (snapshots != null)
        {
          Gson gson = new Gson();
          Type collectionType = new TypeToken<List<Snapshot>>() {
          }.getType();
          snapshotList = gson.fromJson(snapshots, collectionType);

          if (pqEnabled && rescanType.equalsIgnoreCase("PROD"))
          {
            listener.getLogger()
                .println(String.format("%d snapshots have been identified for this application", snapshotList.size()));

            for (Snapshot s : snapshotList)
            {
              listener.getLogger().println(s.getSnapshotName());
              if (s.getSnapshotName().startsWith("QA"))
              {
                // found a QA snapshot, we don't want to run the
                // analysis
                runAnalysis = false;
                break;
              }
            }
          }
        }
*/
        
        listener.getLogger()
            .println(String.format("Proactive Quality is %s and rescan type is %s, the analysis will %s run",
                pqEnabled ? "enabled" : "disabled", rescanType, runAnalysis ? "" : "NOT"));

        if (!runSelectedTasks(build, launcher, listener))
        {
          return false || failBuild;

        }

        // if (!Utils.runJobs(build, launcher, listener,
        // this.getClass(), -1))
        // {
        // return false || failBuild;
        // }
        //
      } catch (HelperException | UnsupportedOperationException e)
      {
        listener.getLogger().println(String.format("Interrupted after: %s\n%s: %s",
            CastUtil.formatNanoTime(System.nanoTime() - startTime), e.getClass().getName(), e.getMessage()));
        return false || failBuild;
      }

    } catch (Exception ex)
    {
      listener.getLogger().println(String.format("Exception:  %s", ex.getMessage()));
      return false || failBuild;
    }

    return true;
  }

  public int addApplJnk(BuildListener listener, String cmsWebAddr)
  {
    int val = 0;
    if (map.containsKey(cmsWebAddr))
    {
      val = map.get(cmsWebAddr);
      map.put(cmsWebAddr, val + 1);
    } else
    {
      map.put(cmsWebAddr, 1);
    }
    listener.getLogger().println(String.format("Added 1 in count of analysis server: %s", cmsWebAddr));
    listener.getLogger().println(String.format("Application count: %d", val + 1));
    return val + 1;
  }

  public int deleteApplJnk(BuildListener listener, String cmsWebAddr)
  {
    int val = 0;
    if (map.containsKey(cmsWebAddr))
    {
      val = map.get(cmsWebAddr);
      map.put(cmsWebAddr, val - 1);
    }
    listener.getLogger().println(String.format("Deleted 1 in count of analysis server: %s", cmsWebAddr));
    listener.getLogger().println(String.format("Application count: %d", val - 1));
    if ((val - 1) < 0)
    {
      return 0;
    }
    return val - 1;
  }

  public int getApplCountJnk(BuildListener listener, String cmsWebAddr)
  {
    int val = 0;
    if (map.containsKey(cmsWebAddr))
    {
      val = map.get(cmsWebAddr);
    }
    listener.getLogger()
        .println(String.format("Current analysis server: %s,    Application count: %d", cmsWebAddr, val));
    return val;
  }

  public int resetApplCountJnk()
  {
    return 0;
  }

  private boolean setMinLoadedCmsAnalServer(BuildListener listener)
  {
    boolean rslt = true;

    LinkedHashSet<String> cmsSet = new LinkedHashSet<String>();
    if (getCmsWebServiceAddress1() != null && !getCmsWebServiceAddress1().isEmpty())
    {
      cmsSet.add(getCmsWebServiceAddress1());
    }
    if (getCmsWebServiceAddress2() != null && !getCmsWebServiceAddress2().isEmpty())
    {
      cmsSet.add(getCmsWebServiceAddress2());
    }
    if (getCmsWebServiceAddress3() != null && !getCmsWebServiceAddress3().isEmpty())
    {
      cmsSet.add(getCmsWebServiceAddress3());
    }
    if (getCmsWebServiceAddress4() != null && !getCmsWebServiceAddress4().isEmpty())
    {
      cmsSet.add(getCmsWebServiceAddress4());
    }
    if (getCmsWebServiceAddress5() != null && !getCmsWebServiceAddress5().isEmpty())
    {
      cmsSet.add(getCmsWebServiceAddress5());
    }

    try
    {
      int minValue = 10000;
      String minLdCmsAnalServer = "";
      CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();

      String[] cmsWebAddrs = cmsSet.toArray(new String[cmsSet.size()]);

      for (int i = 0; i < cmsWebAddrs.length; i++)
      {
        String cmsWebAddr = cmsWebAddrs[i];
        if (cmsWebAddr != null && !cmsWebAddr.isEmpty())
        {
          cbwsl.setCastWebServicePortEndpointAddress(cmsWebAddr);
          CastWebService cbws = cbwsl.getCastWebServicePort();
          // int curValue = cbws.getApplCount();
          int curValue = getApplCountJnk(listener, cmsWebAddr);
          if (curValue < minValue)
          {
            minValue = curValue;
            minLdCmsAnalServer = cmsWebAddr;
          }
        }
      }
      listener.getLogger().println(String.format("Minimum loaded server: %s", minLdCmsAnalServer));

      setCmsWebServiceAddress(minLdCmsAnalServer);
      cbwsl.setCastWebServicePortEndpointAddress(minLdCmsAnalServer);
      CastWebService cbws = cbwsl.getCastWebServicePort();
      // int add_appl = cbws.addAppl();
      int add_appl = addApplJnk(listener, minLdCmsAnalServer);

    } catch (ServiceException e)
    {
      e.printStackTrace();
      rslt = false;
    }

    return rslt;
  }

  private CastWebService getCbwsInstance()
  {
    CastWebService cbws = null;
    CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
    cbwsl.setCastWebServicePortEndpointAddress(getCmsWebServiceAddress());
    try
    {
      cbws = cbwsl.getCastWebServicePort();
    } catch (ServiceException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return cbws;
  }

  /**
   * Log the cms analysis server name selected after checking load balance.
   */
  private String logMinLoadedCmsAnalServerDtl(BuildListener listener)
  {
    try
    {
      int cnt = getApplCountJnk(listener, getCmsWebServiceAddress());

    } catch (Exception e)
    {
      e.printStackTrace();
    }
    return getCmsWebServiceAddress();
  }

  private void deleteApplCount(BuildListener listener, String cmsWebAddr)
  {
    CastWebService cbws = getCbwsInstance();
    try
    {
      // cbws.deleteAppl();
      int add_appl = deleteApplJnk(listener, cmsWebAddr);
    } catch (Exception e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private String UpdateRescanStatus(ValidationProbesService vps, String appName, String version, String castDate,
      String status, String stepIdentifier)
  {
    if (null != vps)
    {
      return vps.UpdateRescanStatus(appName, version, castDate, status, stepIdentifier, getRescanType(), aseBlock);
    } else
    {
      return "";
    }
  }

  int getBlockStep(EnvVars envVars)
  {
    int rslt = 0;
    String temp = envVars.get(Constants.BLOCK_STEP);

    if (null != temp)
    {
      try
      {
        rslt = Integer.parseInt(temp);
      } catch (NumberFormatException ex)
      {
        rslt = 0;
      }
    }

    return rslt;
  }

  private boolean runSelectedTasks(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException
  {
    boolean rslt = true;
    boolean ca = true;
    String status = "";
    String aopUrl = this.getDescriptor().getAopRestUrl();

    // add code to choose CBWS for analysis
    listener.getLogger().println("Checking for least loaded analysis server");

    if (setMinLoadedCmsAnalServer(listener))
    {

      String cmsAddr;

      cmsAddr = logMinLoadedCmsAnalServerDtl(listener);
      ValidationProbesService vps = null;
      String castDate = "";
      try
      {
        EnvVars envVars = build.getEnvironment(listener);
        castDate = envVars.get(Constants.CAST_DATE);
        int blockStep = getBlockStep(envVars);

        CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
        cbwsl.setCastWebServicePortEndpointAddress(dmtWebServiceAddress);
        CastWebService cbws = cbwsl.getCastWebServicePort();
        
        listener.getLogger().println(String.format("AOP Service URL: %s", aopUrl));
        vps = new ValidationProbesService(aopUrl);

        String versionName;
        String versionNameOverRide = envVars.get(Constants.VERSION_NAME_OVERRIDE, "");
        if (versionNameOverRide.isEmpty())
        {
          versionName = this.versionName;
          versionName = getVersionNameWithTag(Constants.castDateFormat.parse(castDate), envVars);
        } else
        {
          versionName = versionNameOverRide;
        }

        if (startAt >= Constants.RunSpecialFunctions)
        {

          switch (startAt) {
          case Constants.RunArchiveException:
            runAnalysis = true;
            startAt = Constants.RunAcceptDelivery;
            rslt = performAcceptDelivery(build, launcher, listener, versionNameOverRide);
            startAt = Constants.RunArchiveDelivery;
            rslt = performArchiveDelivery(build, launcher, listener, versionNameOverRide);

            if (rslt && null != vps)
            {
              vps.deleteException(appName, versionName);
            }
            break;
          }

        } else
        {
          if (null != vps)
          {
            vps.setSchemaNamesInAOP(appName, getSchemaPrefix());
            vps.updateCurrentRescanTypeAOP(appName, getRescanType());
          }
          if (startAt == 0)
          {
            status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_START,
                Constants.AOP_TAB_INITIAL);
          }
          // Send Jenkins Console location to AOP
          String jobName = envVars.get("JOB_NAME");
          String buildNo = envVars.get("BUILD_NUMBER");
          String consoleURL = String.format("job/%s/%s/console", jobName, buildNo);
          vps.sendJenkinsConsolInfo(appName, castDate, consoleURL);
          // JenkinsConsolURL(appName, castDate, consoleURL);

          if (isBackup()) // backup schema triplets
          {
            rslt = performBackup(build, launcher, listener);
          }

          // deliver source code
          if (ca && rslt && startAt <= Constants.RunDMT)
          {
            if (isDa() || startAt == Constants.RunDMT && blockStep != Constants.RunDMT)
            {
              status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_IN_PROGRESS,
                  Constants.AOP_TAB_PACKAGE);

              if (status.equals("Exception"))
              {
                ca = false;

                listener.getLogger().println(" ");
                listener.getLogger().println("There is already a delivery in progress for this application");
                listener.getLogger().println("It will be placed in the exeption tab in AOP");
                listener.getLogger().println("No further processing will be done");
                listener.getLogger().println(" ");

                
              } else if (runAnalysis)
              {

                rslt = performSourceDelivery(build, launcher, listener);

              } else
              {
                listener.getLogger().println(" ");
                listener.getLogger().println("This is a production run and there is at least one QA snapshot");
                listener.getLogger().println("***** Source Code Delivery Step Skipped ******");
              }

              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  rslt ? Constants.AOP_STATUS_OK : Constants.AOP_STATUS_ERROR, Constants.AOP_TAB_PACKAGE);

              if (rslt)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_CONTINUE,
                    Constants.AOP_TAB_ACCEPT);
              }

            } else
            {
              if (blockStep == Constants.RunDMT)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_CONTINUE,
                    Constants.AOP_TAB_PACKAGE);
              }
              ca = false;
            }
          }

          // accept delivery
          if (ca && rslt && startAt <= Constants.RunAcceptDelivery)
          {
            if (isAd() || startAt == Constants.RunAcceptDelivery && blockStep != Constants.RunAcceptDelivery)
            {
              if (!versionNameOverRide.isEmpty())
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate,
                    versionNameOverRide.isEmpty() ? Constants.AOP_STATUS_IN_PROGRESS : Constants.AOP_STATUS_START,
                    Constants.AOP_TAB_ACCEPT);
              }
              if (status.equals("Exception"))
              {
                ca = false;
              }

              if (ca && !status.equals("Exception"))
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_IN_PROGRESS,
                    Constants.AOP_TAB_ACCEPT);

                if (runAnalysis)
                {

                  rslt = performAcceptDelivery(build, launcher, listener, versionNameOverRide);

                } else
                {
                  listener.getLogger().println(" ");
                  listener.getLogger().println("This is a production run and there is at least one QA snapshot");
                  listener.getLogger().println("***** Source Code Delivery Step Skipped ******");
                }

                status = UpdateRescanStatus(vps, appName, versionName, castDate,
                    rslt ? Constants.AOP_STATUS_OK : Constants.AOP_STATUS_ERROR, Constants.AOP_TAB_ACCEPT);

                if (rslt)
                {
                  status = UpdateRescanStatus(vps, appName, versionName, castDate,
                      Constants.AOP_STATUS_CONTINUE + " (Analysis)", Constants.AOP_TAB_ANALYZE);
                }
              }
            } else
            {
              if (blockStep == Constants.RunAcceptDelivery)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_START,
                    Constants.AOP_TAB_ACCEPT);
              }
              ca = false;
            }
          }

          // run analysis
          if (ca && rslt && startAt <= Constants.RunAnalysis)
          {
            if (isRa() || startAt == Constants.RunAnalysis)
            {
              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  Constants.AOP_STATUS_IN_PROGRESS + " (Analysis)", Constants.AOP_TAB_ANALYZE);

              if (status.equals("Exception"))
              {
                ca = false;
              } else if (runAnalysis)
              {

                rslt = performAnalysis(build, launcher, listener, versionNameOverRide);

              } else
              {
                listener.getLogger().println(" ");
                listener.getLogger().println("This is a production run and there is at least one QA snapshot");
                listener.getLogger().println("***** Source Code Delivery Step Skipped ******");
              }

              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  (rslt ? Constants.AOP_STATUS_OK : Constants.AOP_STATUS_ERROR) + " (Analysis)",
                  Constants.AOP_TAB_ANALYZE);

              if (rslt)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate,
                    Constants.AOP_STATUS_CONTINUE + " (Snapshot)", Constants.AOP_TAB_ANALYZE);
              }

            } else
            {
              ca = false;
            }
          }

          // run snapshot
          if (ca && rslt && startAt <= Constants.RunSnapshot)
          {
            if (isRs() || startAt == Constants.RunSnapshot)
            {
              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  Constants.AOP_STATUS_IN_PROGRESS + " (Snapshot)", Constants.AOP_TAB_ANALYZE);

              if (status.equals("Exception"))
              {
                ca = false;
              } else if (runAnalysis)
              {
                rslt = performSnapshot(build, launcher, listener, versionNameOverRide);

              } else
              {
                listener.getLogger().println(" ");
                listener.getLogger().println("This is a production run and there is at least one QA snapshot");
                listener.getLogger().println("***** Source Code Delivery Step Skipped ******");
              }

              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  (rslt ? Constants.AOP_STATUS_OK : Constants.AOP_STATUS_ERROR) + " (Snapshot)",
                  Constants.AOP_TAB_ANALYZE);

              if (rslt)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_CONTINUE,
                    Constants.AOP_TAB_VALIDATE);
              }

            } else
            {
              ca = false;
            }
          }

          // run snapshot validation
          if (ca && rslt && startAt <= Constants.RunValidation)
          {
            if (isRav() || startAt == Constants.RunValidation)
            {
              status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_IN_PROGRESS,
                  Constants.AOP_TAB_VALIDATE);
              if (status.equals("Exception"))
              {
                ca = false;
              } else if (runAnalysis)
              {

                rslt = performSnapshotValidation(build, launcher, listener, versionNameOverRide);

              } else
              {
                listener.getLogger().println(" ");
                listener.getLogger().println("This is a production run and there is at least one QA snapshot");
                listener.getLogger().println("***** Source Code Delivery Step Skipped ******");
              }

              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  (rslt ? Constants.AOP_STATUS_OK : Constants.AOP_STATUS_ERROR) + " (Snapshot)",
                  Constants.AOP_TAB_VALIDATE);

              if (rslt)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_CONTINUE,
                    Constants.AOP_TAB_PUBLISH);
              }

            } else
            {
              ca = false;
            }
          }

          // publish snapshot to AAD
          if (ca && rslt && startAt <= Constants.RunPublishAAD)
          {
            if (isPublish() || startAt == Constants.RunPublishAAD)
            {
              status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_IN_PROGRESS,
                  Constants.AOP_TAB_PUBLISH);

              if (!runAnalysis)
              {

                rslt = performSnapshotValidation(build, launcher, listener, versionNameOverRide);

              } else
              {
                listener.getLogger().println(" ");
                listener.getLogger().println("This is a QA run");
                listener.getLogger().println("***** Source Code Delivery Step Skipped ******");
              }

              status = UpdateRescanStatus(vps, appName, versionName, castDate,
                  (rslt ? Constants.AOP_STATUS_OK : Constants.AOP_STATUS_ERROR), Constants.AOP_TAB_PUBLISH);

              if (rslt)
              {
                status = UpdateRescanStatus(vps, appName, versionName, castDate, Constants.AOP_STATUS_OK,
                    Constants.AOP_TAB_DONE);
              }

            } else
            {
              ca = false;
            }
          }

          if (ca && rslt && startAt <= Constants.RunArchiveDelivery)
          { // archive Delivery
            if (isArchive() || startAt == Constants.RunArchiveDelivery)
            {
              // we can't archive the delivery unless it is accepted
              // need to add a means to check the delivery status
              // if (rslt && startAt == Constants.RunArchiveDelivery && null !=
              // vps)
              // {
              // rslt = performAcceptDelivery(build, launcher, listener);
              // }

              rslt = performArchiveDelivery(build, launcher, listener, versionName);

              if (rslt && startAt == Constants.RunArchiveDelivery && null != vps)
              {
                vps.deleteException(appName, versionName);
                rslt = false;
              }
            }
          }

          if (ca && rslt && isOptimize())
          { // optimize schema triplets
            rslt = performDatabaseOptimize(build, launcher, listener);
          }

          // if (rslt && ca)
          // {
          // UpdateRescanStatus(vps, appName, versionName, castDate,
          // Constants.AOP_STATUS_OK, Constants.AOP_TAB_DONE);
          //
          // }

        }
      } catch (IOException | InterruptedException | ServiceException e)
      {
        listener.getLogger().println(String.format("Error: %s", e.getMessage()));
        e.printStackTrace();
      } catch (Exception e)
      {
        listener.getLogger().println(String.format("Unknown Error: %s", e.getMessage()));
        e.printStackTrace();
      }

      finally
      {

        deleteApplCount(listener, cmsAddr); // Decrease the application
        // count.
      }
    }

    return rslt;
  }

  private boolean performBackup(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    long startTime = System.nanoTime();
    boolean retCode = true;
    int taskId;

    EnvVars envVars = build.getEnvironment(listener);
    // int startAt;
    // try {
    // startAt = Integer.parseInt(envVars.get(Constants.START_AT));
    // } catch (NumberFormatException e) {
    // startAt = 0;
    // }
    if (startAt > Constants.RunBackup)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping backup step.", startAt));
    } else
    {
      listener.getLogger().println(" ");
      listener.getLogger().println("Backup CAST Application database tripplet");

      String castDate = envVars.get(Constants.CAST_DATE);
      String webServiceAddress = getCmsWebServiceAddress();
      String castSchemaPrefix = getSchemaPrefix();
      String appName = getAppName();
      String verName = getVersionName();
      // String verName = envVars.get(Constants.VERSION_NAME, "");

      CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
      cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
      try
      {
        CastWebService cbws = cbwsl.getCastWebServicePort();
        listener.getLogger().println(String.format("Using Web service in performBackup - %s", webServiceAddress));

        if (!Utils.validateWebServiceVersion(webServiceAddress, listener))
        {
          return false;
        }

        Calendar cal = Utils.convertCastDate(castDate);

        taskId = cbws.runBackup(castSchemaPrefix, appName, verName, cal);
        if (taskId < 0)
        {
          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          return false;
        }

        if (!Utils.getLog(cbws, taskId, startTime, listener))
        {
          return false;
        }

      } catch (ServiceException | HelperException | ParseException e)
      {
        retCode = false;
        listener.getLogger().println(String.format("%s error accured while backing up the tripplet", e.getMessage()));
      }

    }
    long diff = new Date().getTime() - totalStartTime.getTime();
    listener.getLogger().printf("Total Backup Duration: %d minutes", TimeUnit.MILLISECONDS.toMinutes(diff));
    listener.getLogger().print(" ");
    return retCode;
  }

  private boolean performSourceDelivery(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    int taskId;
    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);

    listener.getLogger().println(" ");
    if (startAt > Constants.RunDMT)
    {
      listener.getLogger().println(String.format("${START_AT} = %d, skipping delivery step.", startAt));
    } else
    {
      listener.getLogger().println("Deliver Application");

      String dmtWebServiceAddress = getDmtWebServiceAddress();
      String appName = getAppName();
      String schemaPrefix = getSchemaPrefix();

      // cbws.setSchemaNamesInAOP(appName);
      String castDate = envVars.get(Constants.CAST_DATE);
      Date castDt;
      String versionName;
      try
      {
        castDt = Constants.castDateFormat.parse(castDate);
        versionName = getVersionName().replace("[TODAY]", Constants.dateFormatVersion.format(castDt));
      } catch (ParseException e1)
      {
        listener.getLogger().printf("Unable to convert CAST DATE: %s", castDate);
        return false;
      }

      String workFlow = envVars.get(Constants.WORK_FLOW);

      listener.getLogger().println(String.format("Sent Console URL to AOP"));

      boolean isUseJnlp = Boolean.parseBoolean(envVars.get(Constants.RUN_JNLP_DELIVERY));

      boolean failBuild = workFlow.trim().toLowerCase().equals("no");
      listener.getLogger().println("DMT Web Service: " + dmtWebServiceAddress);
      listener.getLogger().println("CMS Web Service: " + getCmsWebServiceAddress());

      try
      {
        CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
        cbwsl.setCastWebServicePortEndpointAddress(dmtWebServiceAddress);
        CastWebService cbws = cbwsl.getCastWebServicePort();
        cbws.setSchemaNamesInAOP(appName, schemaPrefix);
        // listener.getLogger().println(String.format("AOP URL: %s",
        // validateionProbURL));

        cbws.setSchemaNamesInAOP(appName, schemaPrefix);
        String referenceVersion = cbws.getPrevDmtVersion(appName);

        if (referenceVersion.equals(""))
        {
          listener.getLogger().println(String.format("Issue finding previous accepted delivery version."));
          // if (validateionProbURL == null || validateionProbURL.isEmpty())
          // {
          // listener.getLogger()
          // .println("Warning: Connection to AOP is not configured - validation
          // check has not been performed");
          // } else
          // {
          // vps = new ValidationProbesService(validateionProbURL);
          //
          // if (vps != null)
          // {
          // cbws.UpdateRescanStatus(appName, versionName, castDate, "DMT -
          // Error", "DMT");
          // }
          // }
          return false;

        } else
        {
          listener.getLogger().println(String.format("Previous Version retrieved successfully: %s", referenceVersion));
        }

        if (!Utils.validateWebServiceVersion(dmtWebServiceAddress, listener)
            || !Utils.validateWebServiceVersion(cmsWebServiceAddress, listener))
        {
          return false;
        }

        String appId = cbws.getApplicationUUID(appName);
        if (appId == null)
        {
          listener.getLogger().println("appId unavaliable");
          return false;
        }

        listener.getLogger().println("\nDelivery Manager Tool - DMT");

        Calendar cal = Utils.convertCastDate(castDate);
        startTime = System.nanoTime();

        // start delivery
        if (isUseJnlp) // via aic portal
        {
          taskId = cbws.automateDeliveryJNLP(appId, appName, referenceVersion, versionName, cal);
        } else
        { // via cms
          taskId = cbws.deliveryManagerTool(appId, appName, referenceVersion, versionName, cal);
        }

        if (taskId < 0)
        { // did the job start properly
          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          return false || failBuild;
        }

        // display logs and wait for completion code
        if (!Utils.getLog(cbws, taskId, startTime, listener))
        {
          cbws.DMTLogs(appId, appName, referenceVersion, versionName, cal);
          return false;
        }

        // run delivery report
        listener.getLogger().println(" ");
        listener.getLogger().println("Delivery Report");
        int retCode = 0;
        taskId = cbws.deliveryReport(appId, appName, referenceVersion, versionName, cal);
        switch (taskId) {
        case -1:
          listener.getLogger().println("An exception has occured during the delivery report execution");
          listener.getLogger().println("See the CAST Batch Web Service mainlog for more information");
          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          break;
        case -2:
          listener.getLogger().println("Can't find java executor, please update CastAIPWS.properties file");
          break;
        case -3:
          listener.getLogger()
              .println("Can't find CASTDeliveryReporter.jar file, please update CastAIPWS.properties file");
          break;
        case -4:
          listener.getLogger().println("Delivery folder has not been set, please update CastAIPWS.properties file");
          break;
        default:
          if (!Utils.getLog(cbws, taskId, startTime, listener))
          {
            retCode = cbws.getTaskExitValue(taskId);

            if (retCode == -2)
            {
              build.addAction(new PublishEnvVarAction("BUILD_STATUS",
                  "Warning:  No Changes have been made for this delivery, analysis aborted"));
            }

            return false;
          }
          break;
        }
        listener.getLogger().println(" ");

      } catch (ServiceException | RemoteException | ParseException | HelperException e)
      {
        listener.getLogger().println(
            String.format("%s error accured while generating the packaging and delivering the code!", e.getMessage()));
        return false || failBuild;
      } catch (UnsupportedOperationException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      catch (Exception e)
      {
        listener.getLogger().println(String.format("Interrupted after: %s\n%s: %s",
            CastUtil.formatNanoTime(System.nanoTime() - startTime), e.getClass().getName(), e.getMessage()));
        return false || failBuild;
      }

      long diff = new Date().getTime() - totalStartTime.getTime();
      listener.getLogger().printf("Total Source Code Delivery Duration: %d minutes",
          TimeUnit.MILLISECONDS.toMinutes(diff));
      listener.getLogger().print(" ");

    }
    return true;
  }

  private boolean performAcceptDelivery(AbstractBuild build, Launcher launcher, BuildListener listener,
      String versionOverride) throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    int taskId;
    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);
    // int startAt;
    // try {
    // startAt = Integer.parseInt(envVars.get(Constants.START_AT));
    // } catch (NumberFormatException e) {
    // startAt = 0;
    // }
    if (startAt > Constants.RunAcceptDelivery)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping delivery acceptance step.", startAt));
    } else
    {
      listener.getLogger().println("");
      listener.getLogger().println("Accept Delivery");

      String castDate = envVars.get(Constants.CAST_DATE);
      String webServiceAddress = getCmsWebServiceAddress();
      String appName = getAppName();
      String versionName = envVars.get(Constants.VERSION_NAME, "");
      String castMSConnectionProfile = getCastMSConnectionProfile();
      String workFlow = getWorkFlow();

      if (!versionOverride.isEmpty())
      {
        versionName = versionOverride;
      }

      boolean failBuild = workFlow.trim().toLowerCase().equals("no");
      listener.getLogger().println("Web Service: " + webServiceAddress);

      CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
      cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
      try
      {
        CastWebService cbws = cbwsl.getCastWebServicePort();

        Date dateForToday = Constants.castDateFormat.parse(castDate);
        String versionNameWithTag = versionName.replace("[TODAY]", Constants.dateFormatVersion.format(dateForToday));

        listener.getLogger().println(String.format("Application Name: %s", appName));
        listener.getLogger().println(String.format("Version Name: %s", versionName));
        listener.getLogger().println(String.format("Connection Profile Name: %s", castMSConnectionProfile));

        VersionInfo vi = RemoteHelper.getVersionInfo(webServiceAddress);
        if (!checkWebServiceCompatibility(vi.getVersion()))
        {
          listener.getLogger().println(String.format("Incompatible Web Service Version %s (Supported: %s)",
              vi.getVersion(), Constants.wsVersionCompatibility));
          return false || failBuild;
        }

        // Accept Deliver
        startTime = System.nanoTime();
        Calendar cal = Utils.convertCastDate(castDate);

        int retryCount = 0;
        taskId = 0;
        while (true)
        {
          try
          {
            taskId = cbws.acceptDelivery(appName, versionName, castMSConnectionProfile, cal);
            listener.getLogger().println("Accepting delivered code ...");
            break;
          } catch (AxisFault af)
          {
            retryCount++;
            if (retryCount < 3)
            {
              listener.getLogger().printf("%d Access fault encountered\n", retryCount);
            } else
            {
              listener.getLogger().println("Aborging, too many access faults encounerd.");
              return false;
            }
          }
        }
        if (taskId < 0)
        {
          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          return false || failBuild;
        } else if (!Utils.getLog(cbws, taskId, startTime, listener))
        {
          return false;
        }

        // Set As Current Version
        listener.getLogger().println("");
        listener.getLogger().println("Set As Current Version");
        startTime = System.nanoTime();
        taskId = cbws.setAsCurrentVersion(appName, versionNameWithTag, castMSConnectionProfile, cal);

        if (taskId < 0)
        {
          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          return false || failBuild;
        }

        if (!Utils.getLog(cbws, taskId, startTime, listener))
        {
          return false;
        }

        listener.getLogger().println(" ");

      } catch (ServiceException | RemoteException | ParseException | HelperException e)
      {
        listener.getLogger().println(String.format("Interrupted after: %s\n%s: %s",
            CastUtil.formatNanoTime(System.nanoTime() - startTime), e.getClass().getName(), e.getMessage()));
        return false || failBuild;
      }
    }

    long diff = new Date().getTime() - totalStartTime.getTime();
    listener.getLogger().printf("Total Accept Delivery Duration: %d minutes", TimeUnit.MILLISECONDS.toMinutes(diff));
    listener.getLogger().print(" ");
    return true;

  }

  private boolean performAnalysis(AbstractBuild build, Launcher launcher, BuildListener listener,
      String versionOverride) throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    int taskId;
    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);

    if (startAt > Constants.RunAnalysis)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping run analysis step.", startAt));
    } else
    {
      listener.getLogger().println("");
      listener.getLogger().println("Run Analysis");

      boolean failBuild = false;
      try
      {
        String webServiceAddress = getCmsWebServiceAddress();
        String castDate = envVars.get(Constants.CAST_DATE);
        String appName = getAppName();
        String schemaPrefix = getSchemaPrefix();

        // setSchemaNamesInAOP(build, listener, appName);

        String dbPrefix = getSchemaPrefix();
        // String versionName = getVersionName();
        String versionName = envVars.get(Constants.VERSION_NAME, "");
        String castMSConnectionProfile = getCastMSConnectionProfile();
        String workFlow = getWorkFlow();
        failBuild = workFlow.trim().toLowerCase().equals("no");

        CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
        cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
        CastWebService cbws = cbwsl.getCastWebServicePort();
        cbws.setSchemaNamesInAOP(appName, schemaPrefix);
        Calendar cal = Utils.convertCastDate(castDate);

        if (!versionOverride.isEmpty())
        {
          versionName = versionOverride;
        }

        startTime = System.nanoTime();
        taskId = cbws.runAnalysis(appName, versionName, castMSConnectionProfile, cal);

        if (taskId < 0)
        {
          listener.getLogger().println(" ");
          listener.getLogger().println("Trying Analysis response less than 0");
          String validateionProbURL = cbws.getValidationProbURL();
          if (validateionProbURL == null || validateionProbURL.isEmpty())
          {

            listener.getLogger().println(" ");
            listener.getLogger().println(" ");

          } else
          {
            listener.getLogger().println(" ");
            listener.getLogger().println("Sending analyisis logs to Application Operations Portal");

            String mngtDB = String.format("%s_mngt", dbPrefix);
            String rslt = cbws.sendAnalysisLogs(mngtDB, appName, castDate);
            listener.getLogger().println("Response recieved from CBWS");
            JsonResponse response = new Gson().fromJson(rslt, JsonResponse.class);
            listener.getLogger().println("Response recieved from CBWS processed");
            if (response.getCode() < 0)
            {
              listener.getLogger().println("Error sending analyisis logs to Application Operations Portal");
            }
            listener.getLogger().println(response.getJsonString());
          }

          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          return false || failBuild;
        } else
        {

          listener.getLogger().println(" ");
          listener.getLogger().println("Trying Analysis response not less than 0");

          boolean runStatus = Utils.getLog(cbws, taskId, startTime, listener);

          String validateionProbURL = cbws.getValidationProbURL();
          if (validateionProbURL == null || validateionProbURL.isEmpty())
          {

            listener.getLogger().println(" ");
            listener.getLogger().println(" ");

          } else
          {
            listener.getLogger().println(" ");
            listener.getLogger().println("Sending analyisis logs to Application Operations Portal");

            String mngtDB = String.format("%s_mngt", dbPrefix);
            String rslt = cbws.sendAnalysisLogs(mngtDB, appName, castDate);
            listener.getLogger().println("Response recieved from CBWS");
            JsonResponse response = new Gson().fromJson(rslt, JsonResponse.class);
            listener.getLogger().println("Response recieved from CBWS processed");
            if (response.getCode() < 0)
            {
              listener.getLogger().println("Error sending analyisis logs to Application Operations Portal");
            }
            listener.getLogger().println(response.getJsonString());
          }
          return runStatus;
        }

      } catch (IOException | ServiceException | ParseException e)
      {
        listener.getLogger().println(String.format("Interrupted after: %s\n%s: %s",
            CastUtil.formatNanoTime(System.nanoTime() - startTime), e.getClass().getName(), e.getMessage()));
        return false || failBuild;
      }
    }

    long diff = new Date().getTime() - totalStartTime.getTime();
    listener.getLogger().printf("Total Analysis Duration: %d minutes", TimeUnit.MILLISECONDS.toMinutes(diff));
    listener.getLogger().print(" ");

    return true;
  }

  private boolean performSnapshot(AbstractBuild build, Launcher launcher, BuildListener listener,
      String versionOverride) throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    int taskId;
    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);
    String webServiceAddress = getCmsWebServiceAddress();
    String castSchemaPrefix = getSchemaPrefix();
    String castMSConnectionProfile = getCastMSConnectionProfile();
    String retentionPolicy = getRetentionPolicy();
    String workFlow = getWorkFlow();
    boolean failBuild = false;
    failBuild = workFlow.trim().toLowerCase().equals("no");

    if (startAt > Constants.RunSnapshot)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping run snapshot step.", startAt));
    } else
    {
      listener.getLogger().println(" ");
      listener.getLogger().println("Run Snapshot");

      try
      {
        String castDate = envVars.get(Constants.CAST_DATE);
        String appName = getAppName();

        String snapshotName = getVersionNameWithTag(Constants.castDateFormat.parse(castDate), envVars);
        String versionName = snapshotName;

        if (!versionOverride.isEmpty())
        {
          snapshotName = versionOverride + "-"
              + Constants.dateFormatVersion.format(Constants.castDateFormat.parse(castDate));
          versionName = snapshotName;
        }

        CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
        cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
        CastWebService cbws = cbwsl.getCastWebServicePort();

        String centralDbName = String.format("%s_central", castSchemaPrefix);

        if (!Utils.validateWebServiceVersion(webServiceAddress, listener))
        {
          return false;
        }

        Calendar cal = Utils.convertCastDate(castDate);

        /*
         * Run the analysis if this is a QA run or there are no existing
         * production analysis
         */
        if (runAnalysis)
        {
          if (!retentionPolicy.isEmpty() || startAt > 0)
          {
            String policyName = "";
            if (retentionPolicy.equals("12"))
              policyName = "Monthy";
            else if (retentionPolicy.equals("4"))
              policyName = "Quarterly";
            else if (retentionPolicy.equals("2"))
              policyName = "Every 6 Months";

            if (policyName.isEmpty())
              listener.getLogger().println("Snapshot retention policy has NOT been set for this application");
            else
              listener.getLogger().println(String.format("Snapshot retention policy is set to %s", policyName));

            // we always want to keep the first snapshot so,
            // make sure there are at least two snapshots to work with
            if (snapshotList != null && snapshotList.size() > 1)
            {
              // get the latest snapshot
              Snapshot latestSnapshot = snapshotList.get(snapshotList.size() - 1);
              Date lsd = Snapshot.DATE_CONVERTION.parse(latestSnapshot.getFunctionalDate());
              Calendar testDate = Calendar.getInstance();
              testDate.setTime(lsd);

              int lsdMonth = testDate.get(Calendar.MONTH);
              int cdMonth = cal.get(Calendar.MONTH);

              // is there a snapshot for the last snapshot date or
              // is the snapshot retention policy set?
              if (lsd.compareTo(Constants.castDateFormat.parse(castDate)) == 0
                  || (!policyName.isEmpty() && lsdMonth == cdMonth))
              {
                startTime = System.nanoTime();
                if (policyName.isEmpty())
                  listener.getLogger().println("Deleting snapshot to allow for replacement");
                else
                  listener.getLogger().println("Deleting snapshot to maintain the retention policy");

                taskId = cbws.deleteSnapshot(appName, castMSConnectionProfile, testDate, centralDbName);

                if (taskId < 0)
                {
                  listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
                  return false || failBuild;
                } else if (!Utils.getLog(cbws, taskId, startTime, listener))
                {
                  listener.getLogger().println(" ");
                  listener.getLogger().println("Warning:  Delete Snapshot failed");
                  listener.getLogger().println(" ");
                }
              }
            } else if (snapshotList != null && snapshotList.size() == 1 && startAt > 0)
            {
              Snapshot latestSnapshot = snapshotList.get(snapshotList.size() - 1);
              Date lsd = Snapshot.DATE_CONVERTION.parse(latestSnapshot.getFunctionalDate());

              if (lsd.compareTo(Constants.castDateFormat.parse(castDate)) == 0)
              {
                Calendar testDate = Calendar.getInstance();
                testDate.setTime(lsd);
                startTime = System.nanoTime();
                listener.getLogger().println("Deleting snapshot to allow for the creation of a new one");
                taskId = cbws.deleteSnapshot(appName, castMSConnectionProfile, testDate, centralDbName);
                if (taskId < 0)
                {
                  listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
                  return false || failBuild;
                } else if (!Utils.getLog(cbws, taskId, startTime, listener))
                {
                  listener.getLogger().println(" ");
                  listener.getLogger().println("Warning:  Delete Snapshot failed");
                  listener.getLogger().println(" ");
                }
              }
            } else
            {
              if (snapshotList == null)
              {
                listener.getLogger().println(
                    "Warning:  This is the fist snapshot fo this application or an error might have occured while retriving the snapshot list");
              } else
              {
                listener.getLogger().println("There is only one  snapshot, nothing to delete");
              }
            }
          }

          startTime = System.nanoTime();
          taskId = cbws.runSnapshot(appName, castMSConnectionProfile, snapshotName, versionName, cal,
              Boolean.toString(false), rescanType);

          if (taskId < 0)
          {
            listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
            return false || failBuild;
          } else if (!Utils.getLog(cbws, taskId, startTime, listener))
          {
            return false || failBuild;
          }

          /*
           * a new snapshot has been added so refresh the snapshot list
           */
//          String snapshots = cbws.getSnapshotList(centralDbName, appName);
//          Gson gson = new Gson();
//          Type collectionType = new TypeToken<List<Snapshot>>() {
//          }.getType();
//          snapshotList = gson.fromJson(snapshots, collectionType);

        } else
        {
          listener.getLogger().println("***** Snapshot Step Skipped ******");
          // listener.getLogger().println("This is a production run and there is
          // at least one QA snapshot");
        }

        if (pqEnabled)
        {
          /*
           * if the analysis was not run, and this is a production run then the
           * last QA snapshot needs to be converted to a production snapshot
           */
          if (!runAnalysis && rescanType.equals("PROD"))
          {
            boolean first = true;
            Collections.reverse(snapshotList);
            for (Snapshot s : snapshotList)
            {
              if (s.getSnapshotName().startsWith("QA"))
              {
                if (first)
                {
                  String newSnapshotName = s.getSnapshotName().replaceAll("QA", "");
                  cbws.renameSnapshot(centralDbName, s.getSnapshotId(), newSnapshotName);
                  first = false;
                } else
                {
                  Calendar captureDate = Calendar.getInstance();
                  Date dt = Snapshot.DATE_CONVERTION.parse(s.getFunctionalDate());
                  captureDate.setTime(dt);
                  taskId = cbws.deleteSnapshot(appName, castMSConnectionProfile, captureDate, centralDbName);
                  if (taskId < 0)
                  {
                    listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
                    return false || failBuild;
                  } else if (!Utils.getLog(cbws, taskId, startTime, listener))
                  {
                    listener.getLogger().println(" ");
                    listener.getLogger().println("Warning:  Delete Snapshot failed");
                    listener.getLogger().println(" ");
                  }
                }
              }
            }
          }

          if (!runAnalysis && !rescanType.equals("QA"))

            // if scan type is PROD, then delete all QA snapshots-here
            if (!rescanType.equals("QA"))
            {
              String snapshots = cbws.getSnapshotList(centralDbName, appName);
              Gson gson = new Gson();
              Type collectionType = new TypeToken<List<Snapshot>>() {
              }.getType();
              snapshotList = gson.fromJson(snapshots, collectionType);

              if (snapshotList != null && snapshotList.size() > 1)
              {
                for (Snapshot snap : snapshotList)
                {
                  String strSnapshotName = snap.getSnapshotName();

                  Date lsd1 = null;
                  lsd1 = Snapshot.DATE_CONVERTION.parse(snap.getFunctionalDate());
                  Calendar dateSnapshotDate = Calendar.getInstance();
                  dateSnapshotDate.setTime(lsd1);
                  if (strSnapshotName.startsWith("QA"))
                  {
                    taskId = cbws.deleteSnapshot(appName, castMSConnectionProfile, dateSnapshotDate, centralDbName);
                    if (taskId < 0)
                    {
                      listener.getLogger()
                          .println(String.format("Error deleting QA snapshot: %s", cbws.getErrorMessage(-taskId)));
                      return false || failBuild;
                    } else if (!Utils.getLog(cbws, taskId, startTime, listener))
                    {
                      listener.getLogger().println(" ");
                      listener.getLogger().println("Warning:  Delete Snapshot failed");
                      listener.getLogger().println(" ");
                    }
                  }
                }
              }
            }
        }

      } catch (IOException | ServiceException | ParseException | HelperException e)
      {
        listener.getLogger().println(String.format("%s error accured while generating the snapshot!", e.getMessage()));
        return false || failBuild;
      }
    }

    return true;

  }

  private boolean performSnapshotValidation(AbstractBuild build, Launcher launcher, BuildListener listener,
      String versionOverride) throws IOException, InterruptedException
  {
    boolean rslt = false;
    Date totalStartTime = new Date();
    long startTime = System.nanoTime();
    // ValidationProbesService vps = null;

    EnvVars envVars = build.getEnvironment(listener);
    // int startAt;
    // try {
    // startAt = Integer.parseInt(envVars.get(Constants.START_AT));
    // } catch (NumberFormatException e) {
    // startAt = 0;
    // }
    if (startAt > Constants.RunValidation)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping run snapshot validation step.", startAt));
      return true;
    } else
    {
      listener.getLogger().println("");
      listener.getLogger().println("Validate Snapshot");

      String castDate = envVars.get(Constants.CAST_DATE);
      String webServiceAddress = getCmsWebServiceAddress();
      String appName = getAppName();
      // String versionName = getVersionName();

      CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
      cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
      // boolean pass = true;
      boolean pass = true;
      CastWebService cbws = null;
      try
      {
        String versionName = getVersionNameWithTag(Constants.castDateFormat.parse(castDate), envVars);
        String snapshotName = versionName;

        if (!versionOverride.isEmpty())
        {
          snapshotName = versionOverride + Constants.dateFormatVersion.format(Constants.castDateFormat.parse(castDate));
          versionName = versionOverride;
        }

        cbws = cbwsl.getCastWebServicePort();
        String validationStopFlag = cbws.getValidationStopFlag();
        if (!Utils.validateWebServiceVersion(webServiceAddress, listener))
        {
          return false;
        }

        // Validate Snapshot Results
        listener.getLogger().println(String.format("Running Snapshot Validation for %s - %s", appName, snapshotName));
        String validateionProbURL = cbws.getValidationProbURL();
        if (validateionProbURL == null || validateionProbURL.isEmpty())
        {
          listener.getLogger()
              .println("Warning: Connection to AOP is not configured - validation check has not been performed");
        } else
        {
          // vps = new ValidationProbesService(validateionProbURL);

          StringBuffer output = new StringBuffer();

          String results = cbws.runAllChecks(appName, snapshotName);

          if (validationStopFlag != null && validationStopFlag.toLowerCase().equals("true"))
          {
            listener.getLogger().println("Warning: validation.stop flag is set to stop at validation stage");
            listener.getLogger().println("Please check AOP Rescan Validation Page for details");
            listener.getLogger().println(String.format("AOP URL: %s", validateionProbURL));

            if (results == "false")
            {
              pass = false;
            } else
            {
              pass = true;
            }
          } else
          {
            listener.getLogger()
                .println("Warning: validation.stop flag is set to false, hence this application will pass this stage.");
            listener.getLogger().println("Please check AOP Rescan Validation screen for details");
            listener.getLogger().println(String.format("AOP URL: %s",
                validateionProbURL.replace("ValidationProbesService.asmx", "Validation.aspx")));
            pass = true;
          }
          listener.getLogger().println("Sending Validation Status Message...");
          listener.getLogger().println(String.format("Sending completion message: \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"",
              appName, versionName, castDate, 5, pass));
          // cbws.UpdateRescanStatus(appName, versionName, castDate, "Validation
          // - " + (pass ? "OK" : "Error"),
          // "Validation");
          rslt = results.equalsIgnoreCase("true");

          /*
           * for (ValidationResults result : resultList) { output.setLength(0);
           * output.append(result.getCheckNumber()).append("-").append(result.
           * getTestDescription()).append(":") .append(result.getAdvice());
           * listener.getLogger().println(output);
           * 
           * if (validationStopFlag.toLowerCase().equals("true")) { if
           * (result.getAdvice().equals("NO GO")) { pass = false; } } else {
           * pass = true; } }
           */
        }
      } catch (ServiceException | RemoteException | UnsupportedOperationException | HelperException | ParseException e)
      {
        listener.getLogger()
            .println(String.format("%s error accured while backing up the CAST Tripplet", e.getMessage()));
        return false;
      } finally
      {
        listener.getLogger().println(String.format("This application has %s", (pass ? "passed" : "failed")));
        rslt = pass;
        // if (cbws != null)
        // {
        //
        //
        //
        // cbws.UpdateRescanStatus(appName, versionName, castDate, "Validation -
        // " + (pass ? "OK" : "Error"),
        // "Validation");
        // } else
        // {
        // listener.getLogger().println("Warning: link to CBWS has not been
        // initialized!");
        // }

        long diff = new Date().getTime() - totalStartTime.getTime();
        listener.getLogger().printf("Total Snapshot Validation Duration: %d minutes",
            TimeUnit.MILLISECONDS.toMinutes(diff));

        listener.getLogger().println(" ");
      }
    }
    return rslt;
  }

  private boolean performPublishToAAD(AbstractBuild build, Launcher launcher, BuildListener listener,
      String versionOverride) throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    String cmsAddr = logMinLoadedCmsAnalServerDtl(listener);
    int taskId = 0;
    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);

    // int startAt;
    // try {
    // startAt = Integer.parseInt(envVars.get(Constants.START_AT));
    // } catch (NumberFormatException e) {
    // startAt = 0;
    // }
    if (startAt > Constants.RunPublishAAD)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping publish snapshot step.", startAt));
    } else
    {
      listener.getLogger().println(" ");
      listener.getLogger().println("Publish Snapshot");

      boolean failBuild = false;
      try
      {
        String castDate = envVars.get(Constants.CAST_DATE);
        String webServiceAddress = getCmsWebServiceAddress();
        String appName = getAppName();
        String versionName = getVersionNameWithTag(Constants.castDateFormat.parse(castDate), envVars);
        String castSchemaPrefix = getSchemaPrefix();
        String aadSchemaName = getAadSchemaName();
        String workFlow = getWorkFlow();
        failBuild = workFlow.trim().toLowerCase().equals("no");

        CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
        cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
        CastWebService cbws = cbwsl.getCastWebServicePort();
        if (!Utils.validateWebServiceVersion(webServiceAddress, listener))
        {
          return false;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(Constants.castDateFormat.parse(castDate));

        startTime = System.nanoTime();

        if (pqEnabled)
        {
          if (rescanType.equals("QA"))
          {
            listener.getLogger().println(" ");
            listener.getLogger()
                .println("NO GO: Publication not allowed for QA scans. Publication action NOT PERFORMED.");

            // String validateionProbURL = cbws.getValidationProbURL();
            // ValidationProbesService vps = null;
            // if (validateionProbURL == null || validateionProbURL.isEmpty())
            // {
            // listener.getLogger()
            // .println("Warning: Connection to AOP is not configured -
            // validation check has not been performed");
            // } else
            // {
            // vps = new ValidationProbesService(validateionProbURL);
            //
            // if (vps != null)
            // {
            // vps.UpdateRescanStatus(appName, versionName, castDate, "Publish
            // Snapshot - OK", "Report");
            // }
            // }

            return true;
          } else
          {
            taskId = cbws.runPublishSnapshot(aadSchemaName, castSchemaPrefix + "_central", appName, versionName, cal);
            if (taskId < 0)
            {
              listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
              return false || failBuild;
            } else if (!Utils.getLog(cbws, taskId, startTime, listener))
            {
              return false;
            }
          }
        } else
        {
          taskId = cbws.runPublishSnapshot(aadSchemaName, castSchemaPrefix + "_central", appName, versionName, cal);
          if (taskId < 0)
          {
            listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
            return false || failBuild;
          } else if (!Utils.getLog(cbws, taskId, startTime, listener))
          {
            return false;
          }
        }

      } catch (IOException | ServiceException | ParseException | UnsupportedOperationException | HelperException e)
      {
        listener.getLogger()
            .println(String.format("%s error accured while generating the publishing the snapshot!", e.getMessage()));
        return false || failBuild;
      }
    }
    long diff = new Date().getTime() - totalStartTime.getTime();
    listener.getLogger().printf("Total AAD Publication Duration: %d minutes", TimeUnit.MILLISECONDS.toMinutes(diff));
    listener.getLogger().print("");
    return true;
  }

  private boolean performArchiveDelivery(AbstractBuild build, Launcher launcher, BuildListener listener,
      String versionName) throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    String cmsAddr = logMinLoadedCmsAnalServerDtl(listener);
    int taskId;
    long startTime = System.nanoTime();

    EnvVars envVars = build.getEnvironment(listener);
    // int startAt;
    // try {
    // startAt = Integer.parseInt(envVars.get(Constants.START_AT));
    // } catch (NumberFormatException e) {
    // startAt = 0;
    // }
    if (startAt > Constants.RunArchiveDelivery)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping run analysis step.", startAt));
    } else
    {
      if (runAnalysis)
      {
        listener.getLogger().println("");
        listener.getLogger().println("Archive Delivery");

        boolean failBuild = false;
        try
        {
          String webServiceAddress = getCmsWebServiceAddress();
          String castDate = envVars.get(Constants.CAST_DATE);
          String appName = getAppName();
          // String versionName = getVersionName();
          String castMSConnectionProfile = getCastMSConnectionProfile();
          String workFlow = getWorkFlow();
          failBuild = workFlow.trim().toLowerCase().equals("no");

          CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
          cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
          CastWebService cbws = cbwsl.getCastWebServicePort();

          Calendar cal = Utils.convertCastDate(castDate);

          startTime = System.nanoTime();
          String appId = cbws.getApplicationUUID(appName);

          // versionName = cbws.getPrevDmtVersion(appName);

          // versionName = envVars.get(Constants.VERSION_NAME, "");
          taskId = cbws.archiveDelivery(appId, versionName);
          if (taskId < 0)
          {
            listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
            return false || failBuild;
          } else if (!Utils.getLog(cbws, taskId, startTime, listener))
          {
            return false;
          }

        } catch (ServiceException | ParseException e)
        {
          listener.getLogger().println(String.format("Interrupted after: %s\n%s: %s",
              CastUtil.formatNanoTime(System.nanoTime() - startTime), e.getClass().getName(), e.getMessage()));
          return false;
        }
      }
    }
    long diff = new Date().getTime() - totalStartTime.getTime();
    listener.getLogger().printf("Total Delivery Archive Duration: %d minutes", TimeUnit.MILLISECONDS.toMinutes(diff));
    listener.getLogger().print("");
    return true;
  }

  private boolean performDatabaseOptimize(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException
  {
    Date totalStartTime = new Date();
    String cmsAddr = logMinLoadedCmsAnalServerDtl(listener);
    long startTime = System.nanoTime();
    boolean retCode = true;
    int taskId;
    EnvVars envVars = build.getEnvironment(listener);

    // int startAt;
    // try {
    // startAt = Integer.parseInt(envVars.get(Constants.START_AT));
    // } catch (NumberFormatException e) {
    // startAt = 0;
    // }
    if (startAt > Constants.RunDatabaseOptimize)
    {
      listener.getLogger().println(" ");
      listener.getLogger().println(String.format("${START_AT} = %d, skipping run snapshot step.", startAt));
    } else
    {
      listener.getLogger().println(" ");
      listener.getLogger().println("CAST Database Optimization");

      String castDate = envVars.get(Constants.CAST_DATE);
      String webServiceAddress = getCmsWebServiceAddress();
      String castSchemaPrefix = getSchemaPrefix();
      String appName = getAppName();
      String versionName = getVersionName();

      CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
      cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
      try
      {
        CastWebService cbws = cbwsl.getCastWebServicePort();

        if (!Utils.validateWebServiceVersion(webServiceAddress, listener))
        {
          return false;
        }

        Calendar cal = Utils.convertCastDate(castDate);

        taskId = cbws.runOptimization(castSchemaPrefix, appName, versionName, cal);
        if (taskId < 0)
        {
          listener.getLogger().println(String.format("Error: %s", cbws.getErrorMessage(-taskId)));
          return false;
        }

        if (!Utils.getLog(cbws, taskId, startTime, listener))
        {
          return false;
        }

      } catch (ServiceException | HelperException | ParseException e)
      {
        listener.getLogger().println(String.format("%s error accured while backing up the tripplet", e.getMessage()));
      }

    }
    long diff = new Date().getTime() - totalStartTime.getTime();
    listener.getLogger().printf("Total Database Optimization Duration: %d minutes",
        TimeUnit.MILLISECONDS.toMinutes(diff));
    listener.getLogger().print("");
    return true;
  }

  /****
   * private boolean performFinalStep(AbstractBuild build, Launcher launcher,
   * BuildListener listener) throws IOException, InterruptedException {
   * 
   * EnvVars envVars = build.getEnvironment(listener);
   * 
   * String castDate = envVars.get(Constants.CAST_DATE); String
   * webServiceAddress = envVars.get(Constants.CMS_WEB_SERVICE_ADDRESS); String
   * appName = envVars.get(Constants.APPLICATION_NAME); String versionName =
   * envVars.get(Constants.VERSION_NAME, "");
   * 
   * CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
   * cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
   * 
   * CastWebService cbws; try { cbws = cbwsl.getCastWebServicePort();
   * listener.getLogger().println("Sending process complete message to rescan
   * console"); String validateionProbURL = cbws.getValidationProbURL(); if
   * (validateionProbURL == null || validateionProbURL.isEmpty()) {
   * listener.getLogger().println("Warning: Connection to AOP is not
   * configured"); } else { // ValidationProbesService vps = new //
   * ValidationProbesService(validateionProbURL);
   * cbws.UpdateRescanStatus(appName, versionName, castDate, "Rescan Success",
   * "Rescan Success"); } } catch (ServiceException |
   * UnsupportedOperationException e) {
   * listener.getLogger().println(String.format("%s error accured while
   * finalizing analysis job.", e.getMessage())); }
   * 
   * endingDate = new Date(); long duration = endingDate.getTime() -
   * startingDate.getTime();
   * 
   * long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
   * listener.getLogger().printf("Total Job Duration: %d minutes",
   * diffInMinutes);
   * 
   * return true; }
   ***/

  /**
   * Set current scan type from AOP
   * 
   * @param appName
   */
  void setSchemaNamesInAOP(AbstractBuild build, BuildListener listener, String appName)
  {
    String webServiceAddress = getCmsWebServiceAddress();
    String schemaPrefix = getSchemaPrefix();
    CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
    cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);

    try
    {
      CastWebService cbws = cbwsl.getCastWebServicePort();

      String validateionProbURL = cbws.getValidationProbURL();
      if (validateionProbURL == null || validateionProbURL.isEmpty())
      {
        listener.getLogger().println("Warning: Connection to AOP is not configured - schema names not updated");
      } else
      {
        ValidationProbesService vps = new ValidationProbesService(validateionProbURL);

        EnvVars envVars = build.getEnvironment(listener);
        vps.setSchemaNamesInAOP(appName, schemaPrefix);

      }
    } catch (ServiceException | UnsupportedOperationException | SOAPException | IOException | InterruptedException e)
    {
      listener.getLogger().println("Error reading schema prefix from Jenkins");
    }
  }

  /**
   * Set current scan type from AOP
   * 
   * @param appName
   * @param rescanType
   */
  /*
   * void setCurrentScanType(AbstractBuild build, BuildListener listener, String
   * appName, String rescanType) { String webServiceAddress =
   * getCmsWebServiceAddress();
   * 
   * CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
   * cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
   * 
   * try { CastWebService cbws = cbwsl.getCastWebServicePort();
   * 
   * String validateionProbURL = cbws.getValidationProbURL(); if
   * (validateionProbURL == null || validateionProbURL.isEmpty()) {
   * listener.getLogger().println(
   * "Warning: Connection to AOP is not configured - validation check has not been performed"
   * ); } else { ValidationProbesService vps = new
   * ValidationProbesService(validateionProbURL);
   * 
   * EnvVars envVars = build.getEnvironment(listener);
   * vps.updateCurrentRescanTypeAOP(appName, rescanType);
   * 
   * } } catch (ServiceException | UnsupportedOperationException | SOAPException
   * | IOException | InterruptedException e) {
   * listener.getLogger().println("Error reading Rescan Type from Jenkins"); } }
   */

  /**
   * Send Jenkins console location to AOP
   * 
   * @param build
   * @param listener
   * @param appName
   * @param castDate
   */
  /*
   * void sendJenkinsConsolURL(AbstractBuild build, BuildListener listener,
   * String appName, String castDate) { String webServiceAddress =
   * getCmsWebServiceAddress();
   * 
   * CastWebServiceServiceLocator cbwsl = new CastWebServiceServiceLocator();
   * cbwsl.setCastWebServicePortEndpointAddress(webServiceAddress);
   * 
   * try { CastWebService cbws = cbwsl.getCastWebServicePort();
   * 
   * String validateionProbURL = cbws.getValidationProbURL();
   * 
   * if (validateionProbURL == null || validateionProbURL.isEmpty()) {
   * listener.getLogger().println(
   * "Warning: Connection to AOP is not configured - validation check has not been performed"
   * ); } else {
   * 
   * if (validateionProbURL == null || validateionProbURL.isEmpty()) {
   * listener.getLogger().println(
   * "Warning: Connection to AOP is not configured - Jenkins console information not sent."
   * ); } else {
   * 
   * ValidationProbesService vps = new
   * ValidationProbesService(validateionProbURL);
   * 
   * EnvVars envVars = build.getEnvironment(listener); String jobName =
   * envVars.get("JOB_NAME"); String buildNo = envVars.get("BUILD_NUMBER");
   * String consoleURL = String.format("job/%s/%s/console", jobName, buildNo);
   * vps.sendJenkinsConsolInfo(appName, castDate, consoleURL);
   * 
   * } } } catch (ServiceException | UnsupportedOperationException |
   * SOAPException | IOException | InterruptedException e) {
   * listener.getLogger().
   * println("Error sending Jenkins console information to AOP"); } }
   */

  // Overridden for better type safety.
  // If your plugin doesn't really define any property on Descriptor,
  // you don't have to do this.
  @Override
  public DescriptorImpl getDescriptor()
  {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * Descriptor for {@link CastAIPBuilder}. Used as a singleton. The class is
   * marked as public so that it can be accessed from views.
   *
   * <p>
   * See
   * <tt>src/main/resources/hudson/plugins/hello_world/CastDMTBuilder/*.jelly</tt>
   * for the actual HTML fragment for the configuration screen.
   */
  @Extension
  // This indicates to Jenkins that this is an implementation of an extension
  // point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    private String aopRestUrl;

    public String getAopRestUrl()
    {
      return aopRestUrl;
    }

    public void setAopRestUrl(String aopRestUrl)
    {
      this.aopRestUrl = aopRestUrl;
    }

    public DescriptorImpl()
    {
      load();
    }

    @SuppressWarnings("rawtypes")
    public boolean isApplicable(Class<? extends AbstractProject> aClass)
    {
      // Indicates that this builder can be used with all kinds of project
      // types
      return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName()
    {
      return "CAST AIP Automation Framework";
    }

    public FormValidation doTestConnection(@QueryParameter("dmtWebServiceAddress") final String dmtWebServiceAddress,
        @QueryParameter("cmsWebServiceAddress1") String cmsWebServiceAddress1,
        @QueryParameter("cmsWebServiceAddress2") String cmsWebServiceAddress2,
        @QueryParameter("cmsWebServiceAddress3") String cmsWebServiceAddress3,
        @QueryParameter("cmsWebServiceAddress4") String cmsWebServiceAddress4,
        @QueryParameter("cmsWebServiceAddress5") String cmsWebServiceAddress5) throws IOException, ServletException
    {

      if (dmtWebServiceAddress.isEmpty())
      {
        return FormValidation.error("Delivery Web Service Address must have a value!");
      }

      if (cmsWebServiceAddress1 == null || cmsWebServiceAddress1.isEmpty())
      {
        cmsWebServiceAddress1 = dmtWebServiceAddress;
      }

      LinkedHashSet<String> cmsSet = new LinkedHashSet<String>();
      cmsSet.add(cmsWebServiceAddress1);
      cmsSet.add(cmsWebServiceAddress2);
      cmsSet.add(cmsWebServiceAddress3);
      cmsSet.add(cmsWebServiceAddress4);
      cmsSet.add(cmsWebServiceAddress5);

      return doCheckVersionCompatibility(dmtWebServiceAddress, cmsSet);
    }

    public FormValidation doCheckVersionCompatibility(String dmtWebServiceAddress, LinkedHashSet<String> cmsSet)
    {
      boolean ok = true;
      StringBuffer returnMsg = new StringBuffer();

      try
      {
        VersionInfo dvi = RemoteHelper.getVersionInfo(dmtWebServiceAddress);
        returnMsg.append(String.format("Delivery Address Success (%s)\n", dvi.toString()));
      } catch (HelperException e)
      {
        returnMsg.append(String.format("Delivery Address Error %s\n", e.getMessage()));
        ok = false;
      }

      String[] cmsWebAddrs = cmsSet.toArray(new String[cmsSet.size()]);

      for (int i = 0; i < cmsWebAddrs.length; i++)
      {
        try
        {
          if (cmsWebAddrs[i] != null && !cmsWebAddrs[i].isEmpty())
          {
            VersionInfo dvi = RemoteHelper.getVersionInfo(cmsWebAddrs[i]);
            if (dvi.getVersion().equals(Constants.wsVersionCompatibility))
            {
              returnMsg.append(String.format("Analysis Address %d Success (%s)\n", i + 1, dvi.toString()));
            } else
            {
              returnMsg.append(String.format(
                  "WARNING ****** CAST Batch Web Service Version is not supported by this plugin ******* (%s)\n",
                  dvi.toString()));
              ok = false;
            }
          }
        } catch (HelperException e)
        {
          returnMsg.append(String.format("Analysis Address %d Error %s", i + 1, e.getMessage()));
          ok = false;
        }
      }

      if (ok)
      {
        return FormValidation.ok(returnMsg.toString());
      } else
      {
        return FormValidation.error(returnMsg.toString());
      }
    }

    public ListBoxModel doFillReferenceVersionPRODItems(
        @QueryParameter("cmsWebServiceAddress") final String cmsWebServiceAddress,
        @QueryParameter("appName") final String appName)
    {
      ListBoxModel m = new ListBoxModel();

      try
      {
        Collection<String> versions = RemoteHelper.listVersions(cmsWebServiceAddress, appName);

        for (String version : versions)
        {
          m.add(version);
        }

      } catch (HelperException e)
      {
        return m;
      }
      return m;
    }

    public ListBoxModel doFillReferenceVersionItems(
        @QueryParameter("cmsWebServiceAddress") final String cmsWebServiceAddress,
        @QueryParameter("appName") final String appName)
    {
      ListBoxModel m = new ListBoxModel();

      try
      {
        Collection<String> versions = RemoteHelper.listVersions(cmsWebServiceAddress, appName);

        for (String version : versions)
        {
          m.add(version);
        }

      } catch (HelperException e)
      {
        return m;
      }
      return m;
    }

    public ListBoxModel doFillCastMSConnectionProfileItems(
        @QueryParameter("dmtWebServiceAddress") final String dmtWebServiceAddress)
    {
      Logger log = LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME);
      log.addHandler(new ConsoleHandler());
      // log.entering(getClass().getName(), String
      // .format("doFillCastMSConnectionProfileItems
      // (dmtWebServiceAddress<%s>)", cmsWebServiceAddress));

      ListBoxModel m = new ListBoxModel();

      try
      {
        String webServiceAddress = dmtWebServiceAddress;

        Collection<ConnectionProfile> cpList = RemoteHelper.listConnectionProfiles(webServiceAddress);
        if (cpList != null)
        {
          for (ConnectionProfile cp : cpList)
          {
            m.add(cp.getName(), cp.getName());
          }
        }
      } catch (HelperException e)
      {
        return m;
      }
      return m;
    }

    public ListBoxModel doFillRetentionPolicyItems(@QueryParameter String retentionPolicy)
    {
      ListBoxModel m = new ListBoxModel(new ListBoxModel.Option("--None--", "", retentionPolicy.matches("")),
          new ListBoxModel.Option("Monthly", "12", retentionPolicy.matches("12"))
      // new
      // ListBoxModel.Option("Quarterly","4",retentionPolicy.matches("4")),
      // new ListBoxModel.Option("Every 6
      // Months","2",retentionPolicy.matches("2"))
      );
      return m;
    }

    public ListBoxModel doFillAppNameItems(@QueryParameter("dmtWebServiceAddress") final String webServiceAddress,
        @QueryParameter("status") String status)
    {
      ListBoxModel m = new ListBoxModel();

      try
      {
        Collection<String> apps = RemoteHelper.listApplications(webServiceAddress);

        for (String app : apps)
        {
          m.add(app);
        }

      } catch (HelperException e)
      {
        return m;
      }
      return m;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
    {
      aopRestUrl = formData.getString("aopRestUrl");

      save();
      return super.configure(req, formData);
    }
  }
}
