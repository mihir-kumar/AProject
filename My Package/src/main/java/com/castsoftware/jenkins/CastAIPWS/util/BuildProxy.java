package com.castsoftware.jenkins.CastAIPWS.util;

import hudson.FilePath;

import java.io.Serializable;
import java.util.Calendar;


public final class BuildProxy implements Serializable 
{
	private static final long serialVersionUID = 5294002684246852237L;
	private final FilePath artifactsDir;
    private final FilePath projectRootDir;
    private final FilePath buildRootDir;
    private final FilePath executionRootDir;
    private final Calendar timestamp;

    private BuildProxy(FilePath artifactsDir,
                       FilePath projectRootDir,
                       FilePath buildRootDir,
                       FilePath executionRootDir,
                       Calendar timestamp)
    {
        this.artifactsDir = artifactsDir;
        this.projectRootDir = projectRootDir;
        this.buildRootDir = buildRootDir;
        this.executionRootDir = executionRootDir;
        this.timestamp = timestamp;
    }

    public FilePath getArtifactsDir() {
        return artifactsDir;
    }

    public FilePath getBuildRootDir() {
        return buildRootDir;
    }

    public FilePath getExecutionRootDir() {
        return executionRootDir;
    }

    public FilePath getProjectRootDir() {
        return projectRootDir;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }
}