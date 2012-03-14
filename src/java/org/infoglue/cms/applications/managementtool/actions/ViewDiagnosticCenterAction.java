/* ===============================================================================
 *
 * Part of the InfoGlue Content Management Platform (www.infoglue.org)
 *
 * ===============================================================================
 *
 *  Copyright (C)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2, as published by the
 * Free Software Foundation. See the file LICENSE.html for more information.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
 * Place, Suite 330 / Boston, MA 02111-1307 / USA.
 *
 * ===============================================================================
 */

package org.infoglue.cms.applications.managementtool.actions;

import java.util.ArrayList;
import java.util.List;

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.BaseController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.DigitalAssetController;
import org.infoglue.cms.controllers.kernel.impl.simple.PublicationController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.entities.management.Chat;
import org.infoglue.cms.entities.management.Message;
import org.infoglue.cms.entities.management.TableCount;
import org.infoglue.cms.entities.publishing.PublicationVO;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.CmsSessionContextListener;
import org.infoglue.deliver.applications.databeans.CacheEvictionBean;
import org.infoglue.deliver.util.ioqueue.PublicationQueue;

/**
 * This class represents the diagnostic screen of the system
 * 
 * @author mattias
 */

public class ViewDiagnosticCenterAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 1L;

	private List internalDeliverUrls = null;
	private List publicDeliverUrls = null;
	private int numberOfSiteNodes = 0;
	private int numberOfSiteNodeVersions = 0;
	private int numberOfContents = 0;
	private int numberOfContentVersions = 0;
	private int numberOfDigitalAssets = 0;
	private int numberOfUnusedDigitalAssets = 0;
		
	private String liveInstanceValidationUrl = "";
	private String returnAddress = "";

	public String doExecute() throws Exception
    {
    	this.internalDeliverUrls = CmsPropertyHandler.getInternalDeliveryUrls();
    	this.publicDeliverUrls = CmsPropertyHandler.getPublicDeliveryUrls();
    	
    	String tableName = "cmSiteNode";
    	if(CmsPropertyHandler.getUseShortTableNames().equalsIgnoreCase("true"))
    		tableName = "cmSiNo";

    	TableCount tableCount = BaseController.getTableCount(tableName);
    	if(tableCount != null)
    		numberOfSiteNodes = tableCount.getCount();

    	tableName = "cmSiteNodeVersion";
    	if(CmsPropertyHandler.getUseShortTableNames().equalsIgnoreCase("true"))
    		tableName = "cmSiNoVer";

    	tableCount = BaseController.getTableCount(tableName);
    	if(tableCount != null)
    		numberOfSiteNodeVersions = tableCount.getCount();

    	tableName = "cmContent";
    	if(CmsPropertyHandler.getUseShortTableNames().equalsIgnoreCase("true"))
    		tableName = "cmCont";

    	tableCount = BaseController.getTableCount(tableName);
    	if(tableCount != null)
    		numberOfContents = tableCount.getCount();

    	tableName = "cmContentVersion";
    	if(CmsPropertyHandler.getUseShortTableNames().equalsIgnoreCase("true"))
    		tableName = "cmContVer";

    	tableCount = BaseController.getTableCount(tableName);
    	if(tableCount != null)
    		numberOfContentVersions = tableCount.getCount();

    	tableName = "cmDigitalAsset";
    	if(CmsPropertyHandler.getUseShortTableNames().equalsIgnoreCase("true"))
    		tableName = "cmDigAsset";
    	
    	tableCount = BaseController.getTableCount(tableName);
    	if(tableCount != null)
    		numberOfDigitalAssets = tableCount.getCount();

    	tableCount = DigitalAssetController.getNumberOfUnusedAssets();
    	if(tableCount != null)
    		numberOfUnusedDigitalAssets = tableCount.getCount();
    	
        return "success";
    }

	/**
     * This action allows to clear notification queue manually.
     */
    public String doClearQueue() throws Exception
    {
    	PublicationQueue.getPublicationQueue().clearPublicationQueueBean(liveInstanceValidationUrl);
    	
        if(this.returnAddress != null && !this.returnAddress.equals(""))
        {
            this.getResponse().sendRedirect(this.returnAddress);
            return NONE;
        }
        
        return "cleared";
    }
	
	/**
     * This action allows to clear notification queue manually.
     */
    public String doViewFailedPublications() throws Exception
    {
        return "successFailedPublications";
    }
    
	public List getInternalDeliverUrls()
	{
		return internalDeliverUrls;
	}

	public List getPublicDeliverUrls()
	{
		return publicDeliverUrls;
	}

	public int getNumberOfSiteNodes()
	{
		return numberOfSiteNodes;
	}

	public int getNumberOfSiteNodeVersions()
	{
		return numberOfSiteNodeVersions;
	}

	public int getNumberOfContents()
	{
		return numberOfContents;
	}

	public int getNumberOfContentVersions()
	{
		return numberOfContentVersions;
	}

	public int getNumberOfDigitalAssets()
	{
		return numberOfDigitalAssets;
	}

	public int getNumberOfUnusedDigitalAssets()
	{
		return numberOfUnusedDigitalAssets;
	}

	public String getLiveInstanceValidationUrl() 
	{
		return liveInstanceValidationUrl;
	}

	public void setLiveInstanceValidationUrl(String liveInstanceValidationUrl) 
	{
		this.liveInstanceValidationUrl = liveInstanceValidationUrl;
	}

	public String getReturnAddress() 
	{
		return returnAddress;
	}

	public void setReturnAddress(String returnAddress) 
	{
		this.returnAddress = returnAddress;
	}

	public PublicationQueue getPublicationQueue()
	{
		return PublicationQueue.getPublicationQueue();
	}

	public String getInstancePublicationQueueManualClearTimestamp(String baseUrl)
	{
		return PublicationQueue.getPublicationQueue().getInstancePublicationQueueManualClearTimestamp(baseUrl);
	}

	public List<CacheEvictionBean> getOngoingPublicationBeans(String baseUrl)
	{
		return PublicationController.getOngoingPublicationStatusList(baseUrl);
	}

	public List<PublicationVO> getFailedPublicationVOList(String baseUrl)
	{
		return PublicationController.getController().getFailedPublicationVOList(baseUrl);
	}

	
}
