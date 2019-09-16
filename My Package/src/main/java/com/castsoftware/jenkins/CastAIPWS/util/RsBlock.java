package com.castsoftware.jenkins.CastAIPWS.util;

import org.kohsuke.stapler.DataBoundConstructor;

public class RsBlock
{
	private final String retentionPolicy;

	@DataBoundConstructor
	public RsBlock(String retentionPolicy)
	{
		this.retentionPolicy = retentionPolicy;
	}

	public String getRetentionPolicy()
	{
		return retentionPolicy;
	}
	
}
