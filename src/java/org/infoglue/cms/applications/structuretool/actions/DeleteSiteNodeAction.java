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

package org.infoglue.cms.applications.structuretool.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.applications.databeans.LinkBean;
import org.infoglue.cms.controllers.kernel.impl.simple.InconsistenciesController;
import org.infoglue.cms.controllers.kernel.impl.simple.RegistryController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeControllerProxy;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;


/**
 * This action removes a siteNode from the system.
 * 
 * @author Mattias Bogeblad
 */

public class DeleteSiteNodeAction extends InfoGlueAbstractAction
{
    private final static Logger logger = Logger.getLogger(DeleteSiteNodeAction.class.getName());

	private SiteNodeVO siteNodeVO;
	private Integer siteNodeId;
	private SiteNodeVO parentSiteNodeVO;
	private Integer parentSiteNodeId;
	private Integer changeTypeId;
	private Integer repositoryId;
	private String[] registryId;

	//Used for the relatedPages control
	private Integer contentId;
	
	private List referenceBeanList = new ArrayList();
	private String returnAddress = null;
	private String originalAddress = null;
	
	public DeleteSiteNodeAction()
	{
		this(new SiteNodeVO());
	}

	public DeleteSiteNodeAction(SiteNodeVO siteNodeVO) 
	{
		this.siteNodeVO = siteNodeVO;
	}
	
	protected String doExecute() throws Exception 
	{
		this.referenceBeanList = RegistryController.getController().getReferencingObjectsForSiteNode(this.siteNodeVO.getSiteNodeId());
		if(this.referenceBeanList != null && this.referenceBeanList.size() > 0)
		{
		    return "showRelations";
		}
	    else
	    {
			try
			{
				System.out.println("SiteNode:" + this.siteNodeVO);
				this.parentSiteNodeVO = SiteNodeController.getParentSiteNode(this.siteNodeVO.getSiteNodeId());
				this.parentSiteNodeId = this.parentSiteNodeVO.getSiteNodeId();
			}
			catch(Exception e)
			{
				logger.info("The siteNode must have been a root-siteNode because we could not find a parent.");
			}

			SiteNodeControllerProxy.getSiteNodeControllerProxy().acDelete(this.getInfoGluePrincipal(), this.siteNodeVO);
	    	
			return "success";
	    }
	}
	
	public String doV3() throws Exception 
	{
		String userSessionKey = "" + System.currentTimeMillis();

        try
        {
        	doExecute();
        	
    		String deleteSiteNodeInlineOperationDoneHeader = getLocalizedString(getLocale(), "tool.structuretool.deleteSiteNodeInlineOperationDoneHeader", this.siteNodeVO.getName());
    		String deleteSiteNodeInlineOperationViewDeletedPageParentLinkText = getLocalizedString(getLocale(), "tool.structuretool.deleteSiteNodeInlineOperationViewDeletedPageParentLinkText");
    		String deleteSiteNodeInlineOperationViewCreatedPageParentTitleText = getLocalizedString(getLocale(), "tool.structuretool.deleteSiteNodeInlineOperationViewDeletedPageParentTitleText");
    	
    	    setActionMessage(userSessionKey, deleteSiteNodeInlineOperationDoneHeader);
    										  																	
    	    addActionLink(userSessionKey, new LinkBean("parentPageUrl", deleteSiteNodeInlineOperationViewDeletedPageParentLinkText, deleteSiteNodeInlineOperationViewCreatedPageParentTitleText, deleteSiteNodeInlineOperationViewCreatedPageParentTitleText, this.originalAddress, false, "", "", "structure"));
            setActionExtraData(userSessionKey, "refreshToolbarAndMenu", "" + true);
            setActionExtraData(userSessionKey, "repositoryId", "" + this.siteNodeVO.getRepositoryId());
            setActionExtraData(userSessionKey, "siteNodeId", "" + this.siteNodeVO.getSiteNodeId());
            setActionExtraData(userSessionKey, "unrefreshedSiteNodeId", "" + parentSiteNodeId);
            setActionExtraData(userSessionKey, "unrefreshedNodeId", "" + parentSiteNodeId);
            setActionExtraData(userSessionKey, "changeTypeId", "" + this.changeTypeId);
            setActionExtraData(userSessionKey, "disableCloseLink", "true");
        }
        catch(ConstraintException ce)
        {
        	logger.warn("An error occurred so we should not complete the transaction:" + ce);

        	parentSiteNodeVO = SiteNodeControllerProxy.getController().getSiteNodeVOWithId(parentSiteNodeId);

			ce.setResult(INPUT + "V3");
			throw ce;
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            throw new SystemException(e.getMessage());
        }
    	        
        if(this.returnAddress != null && !this.returnAddress.equals(""))
        {
	        String arguments 	= "userSessionKey=" + userSessionKey + "&isAutomaticRedirect=false";
	        String messageUrl 	= returnAddress + (returnAddress.indexOf("?") > -1 ? "&" : "?") + arguments;
	        
	        this.getResponse().sendRedirect(messageUrl);
	        return NONE;
        }
        else
        {
        	return "successV3";
        }
    }

	
	public String doDeleteReference() throws Exception 
	{
	    for(int i=0; i<registryId.length; i++)
	    {
	    	String registryIdString = registryId[i];
	    	Integer registryId = new Integer(registryIdString);
	    	try
	    	{
	    		InconsistenciesController.getController().removeReferences(registryId, this.getInfoGluePrincipal());
	    	}
	    	catch(Exception e)
	    	{
	    		logger.debug("Error trying to remove reference - must be removed before...");
	    	}
	    	try
	    	{
	    		RegistryController.getController().delete(registryId);
	    	}
	    	catch(Exception e)
	    	{
	    		logger.debug("Error trying to remove reference - must be removed before...");
	    	}
	    }
	    
	    return doExecute();
	}	

	
	public String doFixPage() throws Exception 
	{
	    return "fixPage";
	}

	public String doFixPageHeader() throws Exception 
	{
	    return "fixPageHeader";
	}
	
	public void setSiteNodeId(Integer siteNodeId)
	{
		this.siteNodeVO.setSiteNodeId(siteNodeId);
	}

	public Integer getOriginalSiteNodeId()
	{
		return this.siteNodeVO.getSiteNodeId();
	}
	
	public void setParentSiteNodeId(Integer parentSiteNodeId)
	{
		this.parentSiteNodeId = parentSiteNodeId;
	}

	public void setChangeTypeId(Integer changeTypeId)
	{
		this.changeTypeId = changeTypeId;
	}

	public Integer getSiteNodeId()
	{
		return this.parentSiteNodeId;
	}
	
	public Integer getUnrefreshedSiteNodeId()
	{
		return this.parentSiteNodeId;
	}
	
	public Integer getChangeTypeId()
	{
		return this.changeTypeId;
	}
        
	public String getErrorKey()
	{
		return "SiteNodeVersion.stateId";
	}

	public void setReturnAddress(String returnAddress)
	{
		this.returnAddress = returnAddress;
	}

	public String getReturnAddress()
	{
		if(this.returnAddress != null && !this.returnAddress.equals(""))
			return this.returnAddress;
		else
			return "ViewSiteNode.action?siteNodeId=" + this.siteNodeVO.getId() + "&repositoryId=" + this.siteNodeVO.getRepositoryId();
	}

	public void setOriginalAddress(String originalAddress)
	{
		this.originalAddress = originalAddress;
	}

	public String getOriginalAddress()
	{
		return this.originalAddress;
	}

    public Integer getRepositoryId()
    {
        return repositoryId;
    }
    
    public void setRepositoryId(Integer repositoryId)
    {
        this.repositoryId = repositoryId;
    }
    
    public Integer getContentId()
    {
        return contentId;
    }
    
    public void setContentId(Integer contentId)
    {
        this.contentId = contentId;
    }
    
    public List getReferenceBeanList()
    {
        return referenceBeanList;
    }
    
    public String[] getRegistryId()
    {
        return registryId;
    }
    
    public void setRegistryId(String[] registryId)
    {
        this.registryId = registryId;
    }
}
