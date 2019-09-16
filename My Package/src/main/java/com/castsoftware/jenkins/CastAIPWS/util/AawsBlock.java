package com.castsoftware.jenkins.CastAIPWS.util;

import org.kohsuke.stapler.DataBoundConstructor;

public class AawsBlock 
{
	private final  String cmsWebServiceAddress; 

    @DataBoundConstructor
    public AawsBlock(String cmsWebServiceAddress)
    {
    	this.cmsWebServiceAddress=cmsWebServiceAddress;
    }

	public String getCmsWebServiceAddress()
	{
		return cmsWebServiceAddress;
	}

}
