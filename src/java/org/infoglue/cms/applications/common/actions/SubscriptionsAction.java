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

package org.infoglue.cms.applications.common.actions;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.CategoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentTypeDefinitionController;
import org.infoglue.cms.controllers.kernel.impl.simple.InterceptionPointController;
import org.infoglue.cms.controllers.kernel.impl.simple.SubscriptionController;
import org.infoglue.cms.entities.management.CategoryVO;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.InterceptionPointVO;
import org.infoglue.cms.entities.management.SubscriptionFilterVO;
import org.infoglue.cms.entities.management.SubscriptionVO;


/** 
 * This class shows which roles has access to the siteNode.
 */

public class SubscriptionsAction extends InfoGlueAbstractAction
{
    private final static Logger logger = Logger.getLogger(SubscriptionsAction.class.getName());

	private static final long serialVersionUID = 1L;
	
	private Integer interceptionPointId = null;
	private String interceptionPointName = null;
	private String[] interceptionPointCategory = null;
	private String[] explicitInterceptionPointNames = null;
	private String entityName = null;
	private String entityId = null;
	private String extraParameters = "";
	private String returnAddress;
	private Integer[] subscribedInterceptionPointId = null;

	private Map interceptionPointsGroupsMap = new HashMap();
	private Map subscriptions = new HashMap();
	private String interceptionPointIdString = "";
	
	//Global subscriptions
	private Collection subscriptionVOList = null;
	private String name;
	private Integer subscriptionId;
	
	private List interceptionPointVOList;
	private List contentTypeDefintionVOList;
	private List categoryVOList;
	
	private static SubscriptionController subscriptionsController = SubscriptionController.getController();
	
	public String doInput() throws Exception
    {
		if(explicitInterceptionPointNames != null && explicitInterceptionPointNames.length > 0)
		{
			for(int i=0; i<explicitInterceptionPointNames.length; i++)
			{
				String interceptionPointName = explicitInterceptionPointNames[i];
				InterceptionPointVO interceptionPointVO = InterceptionPointController.getController().getInterceptionPointVOWithName(interceptionPointName);
				interceptionPointIdString += interceptionPointVO.getId() + ",";
				
				List subscriptionVOList = subscriptionsController.getSubscriptionVOList(interceptionPointVO.getId(), null, null, entityName, entityId, this.getInfoGluePrincipal().getName(), null);
				if(subscriptionVOList != null && subscriptionVOList.size() > 0)
					subscriptions.put(interceptionPointVO.getId(), true);
				else
					subscriptions.put(interceptionPointVO.getId(), false);
				
				List interceptionPointVOList = (List)interceptionPointsGroupsMap.get(interceptionPointVO.getCategory());
				if(interceptionPointVOList != null)
					interceptionPointVOList.add(interceptionPointVO);
				else
				{
					interceptionPointVOList = new ArrayList();
					interceptionPointVOList.add(interceptionPointVO);
					interceptionPointsGroupsMap.put(interceptionPointVO.getCategory(), interceptionPointVOList);
				}
			}
		}
		else
		{
			for(int i=0; i<interceptionPointCategory.length; i++)
			{
				String interceptionPointCategoryName = interceptionPointCategory[i];
				List interceptionPointVOList = InterceptionPointController.getController().getInterceptionPointVOList(interceptionPointCategoryName);
				Iterator interceptionPointVOListIterator = interceptionPointVOList.iterator();
				while(interceptionPointVOListIterator.hasNext())
				{
					InterceptionPointVO interceptionPointVO = (InterceptionPointVO)interceptionPointVOListIterator.next();
					interceptionPointIdString += interceptionPointVO.getId() + ",";
					
					List subscriptionVOList = subscriptionsController.getSubscriptionVOList(interceptionPointVO.getId(), null, null, entityName, entityId, this.getInfoGluePrincipal().getName(), null);
					if(subscriptionVOList != null && subscriptionVOList.size() > 0)
						subscriptions.put(interceptionPointVO.getId(), true);
					else
						subscriptions.put(interceptionPointVO.getId(), false);
				}
				this.interceptionPointsGroupsMap.put(interceptionPointCategoryName, interceptionPointVOList);
			}
		}
		
		return INPUT;
    }
    
    public String doExecute() throws Exception
    {
    	String[] interceptionPointIds = interceptionPointIdString.split(",");
    	for(int i=0; i<interceptionPointIds.length; i++)
    	{
    		String key = "subscription_" + interceptionPointIds[i] + "_" + extraParameters;
    		System.out.println("removing key:" + key);
			List subscriptionVOList = subscriptionsController.getSubscriptionVOList(interceptionPointId, null, null, entityName, entityId, this.getInfoGluePrincipal().getName(), null);
    		Iterator<SubscriptionVO> subscriptionVOListIterator = subscriptionVOList.iterator();
    		while(subscriptionVOListIterator.hasNext())
    		{
    			SubscriptionVO subscriptionVO = subscriptionVOListIterator.next();
    			subscriptionsController.delete(subscriptionVO);
    		}
    	}
    	
    	System.out.println("subscribedInterceptionPointId:" + subscribedInterceptionPointId);
	    for(int i=0; i<subscribedInterceptionPointId.length; i++)
    	{
    		Integer interceptionPointId = subscribedInterceptionPointId[i];
    		System.out.println("interceptionPointId:" + interceptionPointId);
    		SubscriptionVO subscriptionVO = new SubscriptionVO();
    		subscriptionVO.setInterceptionPointId(interceptionPointId);
        	subscriptionVO.setName(name);
        	subscriptionVO.setIsGlobal(false);
    		subscriptionVO.setEntityName(entityName);
    		subscriptionVO.setEntityId(entityId);
    		subscriptionVO.setUserName(this.getInfoGluePrincipal().getName());
    		
    		subscriptionsController.create(subscriptionVO);
    	}
    	
        System.out.println();
        System.out.println("returnAddress:" + this.returnAddress);
        if(this.returnAddress != null && !this.returnAddress.equals(""))
        {
	        String arguments = "isAutomaticRedirect=false&message=Prenumsparad!&actionLinks=link1,L�nk 1,Testl�nk,Det h�r �r en l�nk till CG-channel,http://www.cgchannel.com,http://www.iconarchive.com/icons/zakar/shining-z/Casque-SZ-24x24.png;link1,L�nk Lala,Testl�nk 2,Det h�r �r en l�nk till Silo-forumet,http://www.silo3d.com/forum/,http://www.iconarchive.com/icons/zakar/shining-z/Deamontools-SZ-24x24.png";
	        String messageUrl = returnAddress + (returnAddress.indexOf("?") > -1 ? "&" : "?") + arguments;
	        
	        System.out.println("messageUrl:" + messageUrl);
	        this.getResponse().sendRedirect(messageUrl);
	        return NONE;
        }
        else
        {
        	return SUCCESS;
        }
    }

    
	public String doInputGlobalSubscriptions() throws Exception
    {
		this.subscriptionVOList = subscriptionsController.getSubscriptionVOList(null, null, new Boolean(true), null, null, this.getInfoGluePrincipal().getName(), null);
		this.contentTypeDefintionVOList = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOList();
		this.categoryVOList = CategoryController.getController().findAllActiveCategories();

		this.interceptionPointVOList = new ArrayList();
		List allInterceptionPointVOList = InterceptionPointController.getController().getInterceptionPointVOList();
		Iterator allInterceptionPointVOListIterator = allInterceptionPointVOList.iterator();
		while(allInterceptionPointVOListIterator.hasNext())
		{
			InterceptionPointVO interceptionPointVO = (InterceptionPointVO)allInterceptionPointVOListIterator.next();
			String name = interceptionPointVO.getName();
			if(name.equals("ContentVersion.Publish") ||
			   name.equals("SiteNodeVersion.Publish") ||
			   name.equals("Content.ExpireDateComingUp") || 
			   name.equals("SiteNode.ExpireDateComingUp"))
			{
				this.interceptionPointVOList.add(interceptionPointVO);	
			}
		}
		
		/*
		Content.Write
	    Content.Create
	    Content.Delete
	    Content.Move
	    Content.SubmitToPublish
	    Content.ChangeAccessRights
	    Content.CreateVersion
	    ContentVersion.Delete
	    ContentVersion.Write
	    ContentVersion.Publish
	    SiteNodeVersion.Write
	    SiteNodeVersion.CreateSiteNode
	    SiteNodeVersion.DeleteSiteNode
	    SiteNodeVersion.MoveSiteNode
	    SiteNodeVersion.SubmitToPublish
	    SiteNodeVersion.ChangeAccessRights
	    SiteNodeVersion.Publish
	    Publication.Write
		*/
		
		return "inputGlobalSubscriptions";
    }
    
    public String doGlobalSubscriptions() throws Exception
    {
    	SubscriptionVO subscriptionVO = new SubscriptionVO();
    	subscriptionVO.setIsGlobal(true);
    	subscriptionVO.setInterceptionPointId(interceptionPointId);
    	subscriptionVO.setName(name);
    	subscriptionVO.setUserName(this.getInfoGluePrincipal().getName());
    	
    	List<SubscriptionFilterVO> subscriptionFilterVOList = new ArrayList<SubscriptionFilterVO>();
    	
    	int i=0;
    	String filterType = this.getRequest().getParameter("filterType_" + i);
    	while(filterType != null && !filterType.equals("") && filterType != "-1")
    	{
	    	String[] filterConditions = this.getRequest().getParameterValues("filterCondition_" + i);
	    	StringBuffer filterConditionStringBuffer = new StringBuffer();
	    	for(int j=0; j<filterConditions.length; j++)
	    	{
	    		if(j > 0)
	    			filterConditionStringBuffer.append(",");
	    		filterConditionStringBuffer.append(URLEncoder.encode(filterConditions[j], "utf-8"));
	    	}
	    	
	    	String andParagraph = this.getRequest().getParameter("andParagraph_" + i);
	    	if(andParagraph == null || andParagraph.equals(""))
	    		andParagraph = "true";
	    	
	    	SubscriptionFilterVO subscriptionFilterVO = new SubscriptionFilterVO();
	    	subscriptionFilterVO.setFilterType(filterType);
	    	subscriptionFilterVO.setFilterCondition(filterConditionStringBuffer.toString());
	    	subscriptionFilterVO.setIsAndCondition(new Boolean(andParagraph));
	    	subscriptionFilterVOList.add(subscriptionFilterVO);

	    	i++;
	    	filterType = this.getRequest().getParameter("filterType_" + i);
    	}
    	
    	subscriptionsController.create(subscriptionVO, subscriptionFilterVOList);
    	
    	return "successGlobalSubscriptions";
    }

    public String doUpdateGlobalSubscription() throws Exception
    {
    	SubscriptionVO subscriptionVO = subscriptionsController.getSubscriptionVOWithId(subscriptionId);

    	subscriptionVO.setIsGlobal(true);
    	subscriptionVO.setInterceptionPointId(interceptionPointId);
    	subscriptionVO.setName(name);
    	
    	List<SubscriptionFilterVO> subscriptionFilterVOList = new ArrayList<SubscriptionFilterVO>();
    	
    	int i=0;
    	String filterType = this.getRequest().getParameter("filterType_" + i);
    	System.out.println("filterType[" + i + "]:" + filterType);
    	while(filterType != null && !filterType.equals("") && filterType != "-1")
    	{
	    	String[] filterConditions = this.getRequest().getParameterValues("filterCondition_" + i);
	    	StringBuffer filterConditionStringBuffer = new StringBuffer();
	    	for(int j=0; j<filterConditions.length; j++)
	    	{
	    		if(j > 0)
	    			filterConditionStringBuffer.append(",");
	    		filterConditionStringBuffer.append(URLEncoder.encode(filterConditions[j], "utf-8"));
	    	}

	    	String andParagraph = this.getRequest().getParameter("andParagraph_" + i);
	    	if(andParagraph == null || andParagraph.equals(""))
	    		andParagraph = "true";
	    	
	    	SubscriptionFilterVO subscriptionFilterVO = new SubscriptionFilterVO();
	    	subscriptionFilterVO.setFilterType(filterType);
	    	subscriptionFilterVO.setFilterCondition(filterConditionStringBuffer.toString());
	    	subscriptionFilterVO.setIsAndCondition(new Boolean(andParagraph));
	    	subscriptionFilterVOList.add(subscriptionFilterVO);

	    	i++;
	    	filterType = this.getRequest().getParameter("filterType_" + i);
	    	System.out.println("filterType[" + i + "]:" + filterType);
    	}
    	
    	subscriptionsController.update(subscriptionVO, subscriptionFilterVOList);
    	
    	return "successGlobalSubscriptions";
    }

    public String doDeleteGlobalSubscription() throws Exception
    {
    	subscriptionsController.delete(subscriptionId);
    	
    	return "successGlobalSubscriptions";
    }
    
    public String doGetSubscriptionForm() throws Exception
    {
    	SubscriptionVO subscriptionVO = subscriptionsController.getSubscriptionVOWithId(subscriptionId);
    	
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append("<form action=\"Subscriptions!updateGlobalSubscription.action\" name=\"inputForm\" method=\"post\">");
    	sb.append("<input type=\"hidden\" name=\"subscriptionId\" value=\"" + subscriptionVO.getId() + "\" style=\"border: 0px;\"/>");
    	sb.append("<fieldset style=\"width: 90%; border: 0px solid red; margin: 0px; padding-left: 10px;\">");
    	
    	sb.append("	<h3>Subscription basics</h3>");
    	sb.append("	<p style=\"clear: both;\">");
    	
    	sb.append("	<label for=\"name\">Name</label>");
    	sb.append("	<input type=\"text\" name=\"name\" value=\"" + subscriptionVO.getName() + "\"/>");
    	sb.append("	</p>");
		
		sb.append("	<p style=\"clear: both;\">");
    	sb.append("	<label for=\"interceptionPointId\">Type</label>");
    	sb.append("	<select name=\"interceptionPointId\">");
    	sb.append("		<option value=\"33\" " + (subscriptionVO.getInterceptionPointId().intValue() == 33 ? "selected='selected'" : "") + ">Content.Published</option>");
    	sb.append("		<option value=\"22\" " + (subscriptionVO.getInterceptionPointId().intValue() == 22 ? "selected='selected'" : "") + ">Content.Delete</option>");
    	sb.append("	</select>");
    	sb.append("	</p>");
			
    	sb.append("	<div style=\"clear: both;\"></div>");
		
    	sb.append("	<h4 style=\"border-bottom: 1px solid #bbb;\">Filters</h4>");

    	this.contentTypeDefintionVOList = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOList();
		this.categoryVOList = CategoryController.getController().findAllActiveCategories();
		
    	int i = 0;
    	int size = subscriptionVO.getSubscriptionFilterVOList().size();
    	Iterator<SubscriptionFilterVO> subscriptionFilterVOListIterator = subscriptionVO.getSubscriptionFilterVOList().iterator();
    	while(subscriptionFilterVOListIterator.hasNext())
    	{
    		SubscriptionFilterVO subscriptionFilterVO = subscriptionFilterVOListIterator.next();
    		
	    	sb.append("	<div id=\"filterRow_" + i + "\" class=\"formRow\" style=\"min-height: 50px; border: 0px solid red; border-bottom: 1px solid #bbb;\">");
	    	sb.append("		<label for=\"filterType_" + i + "\">Filter type:</label>");
	    	sb.append("		<select id=\"filterType_" + i + "\" name=\"filterType_" + i + "\" onchange=\"updateConditionInput(" + i + ");\">");
	    	sb.append("			<option value=\"0\"" + (subscriptionFilterVO.getFilterType().equals("0") ? "selected='selected'" : "") + ">Content types</option>");
	    	sb.append("			<option value=\"1\"" + (subscriptionFilterVO.getFilterType().equals("1") ? "selected='selected'" : "") + ">Categories</option>");
	    	sb.append("		</select>");
			
	   		sb.append("		<label for=\"filterCondition_" + i + "\">Filter condition:</label>");
	   		sb.append("		<select id=\"filterCondition_" + i + "\" name=\"filterCondition_" + i + "\" multiple=\"multiple\" size=\"3\">");
	   		
	   		if(subscriptionFilterVO.getFilterType().equals("0"))
	   		{
	   			Iterator contentTypeDefintionVOListIterator = contentTypeDefintionVOList.iterator();
	   			while(contentTypeDefintionVOListIterator.hasNext())
	   			{
	   				ContentTypeDefinitionVO ctd = (ContentTypeDefinitionVO)contentTypeDefintionVOListIterator.next();
	   				sb.append("<option value=\"" + ctd.getId() + "\"" + (hasValue(subscriptionFilterVO.getFilterCondition(), ctd.getId().toString()) ? "selected='selected'" : "") + ">" + ctd.getName() + "</option>");
	   			}
	   		}
	   		else if(subscriptionFilterVO.getFilterType().equals("1"))
	   		{
	   			Iterator categoryVOListIterator = categoryVOList.iterator();
	   			while(categoryVOListIterator.hasNext())
	   			{
	   				CategoryVO categoryVO = (CategoryVO)categoryVOListIterator.next();
	   				sb.append("<option value=\"" + categoryVO.getId() + "\"" + (hasValue(subscriptionFilterVO.getFilterCondition(), categoryVO.getId().toString()) ? "selected='selected'" : "") + ">" + categoryVO.getName() + "</option>");
	   			}	   			
	   		}
	   		
	   		sb.append("		</select>");
					
	   		sb.append("		&nbsp; <a id=\"removeFilterRowLink" + i + "\" href=\"javascript:removeFilterRow(" + i + ");\">Remove filter</a>");
					
	   		if(size-1 > i)
	   			sb.append("		<p id=\"andParagraph_" + i + "\" style=\"padding: 30px; display: block;\">");
	   		else
	   			sb.append("		<p id=\"andParagraph_" + i + "\" style=\"padding: 30px; display: none;\">");
		   		
	   		sb.append("			<label for=\"isAndCondition_" + i + "\">isAndCondition</label>");
			sb.append("			<select id=\"isAndCondition_" + i + "\" name=\"isAndCondition_0\">';");
			sb.append("				<option value=\"true\"" + (subscriptionFilterVO.getIsAndCondition().booleanValue() ? "selected='selected'" : "") + ">AND</option>';");
			sb.append("				<option value=\"false\"" + (!subscriptionFilterVO.getIsAndCondition().booleanValue() ? "selected='selected'" : "") + ">OR</option>';");
			sb.append("			</select>");
			sb.append("		</p>");
			sb.append("	</div>");
									
			sb.append("	<div id=\"break_" + i + "\" style=\"clear:both\"></div>");
			
			i++;
	    }
    	
		sb.append("	<br/>");
		sb.append("	<a href=\"javascript:addFilterRow();\">Add filter</a>");
		sb.append("	<br/>");
		sb.append("	<br/>");
			
		sb.append("	<input type=\"submit\" value=\"Save\"/>");
		sb.append("</fieldset>");
		sb.append("</form>");
		sb.append("<script type='text/javascript'>i=" + i + ";</script>");
    	
		this.getResponse().setContentType("text/plain");
        this.getResponse().getWriter().println(sb.toString());
        
        return NONE;
    }

    private boolean hasValue(String setValuesAsCommaseperatedString, String value)
    {
    	String[] setValues = setValuesAsCommaseperatedString.split(",");
    	for(int i=0; i<setValues.length; i++)
    	{
    		String setValue = setValues[i];
    		if(setValue.equals(value))
    			return true;
    	}
    	
    	return false;
    }
    
	public String getReturnAddress()
	{
		return returnAddress;
	}

	public void setReturnAddress(String returnAddress)
	{
		this.returnAddress = returnAddress;
	}

	public Integer getInterceptionPointId()
	{
		return this.interceptionPointId;
	}

	public void setInterceptionPointId(Integer interceptionPointId)
	{
		this.interceptionPointId = interceptionPointId;
	}

	public String getExtraParameters()
	{
		return this.extraParameters;
	}

	public String getInterceptionPointName()
	{
		return this.interceptionPointName;
	}

	public void setExtraParameters(String extraParameters)
	{
		this.extraParameters = extraParameters;
	}

	public void setInterceptionPointName(String interceptionPointName)
	{
		this.interceptionPointName = interceptionPointName;
	}

	public String[] getInterceptionPointCategory()
	{
		return this.interceptionPointCategory;
	}

	public void setInterceptionPointCategory(String[] interceptionPointCategory)
	{
		this.interceptionPointCategory = interceptionPointCategory;
	}

	public Map getInterceptionPointsGroupsMap()
	{
		return interceptionPointsGroupsMap;
	}

	public String[] getExplicitInterceptionPointNames()
	{
		return explicitInterceptionPointNames;
	}

	public void setExplicitInterceptionPointNames(String[] explicitInterceptionPointNames)
	{
		this.explicitInterceptionPointNames = explicitInterceptionPointNames;
	}

	public Map getSubscriptions()
	{
		return subscriptions;
	}

	public Integer[] getSubscribedInterceptionPointId()
	{
		return subscribedInterceptionPointId;
	}

	public void setSubscribedInterceptionPointId(Integer[] subscribedInterceptionPointId)
	{
		this.subscribedInterceptionPointId = subscribedInterceptionPointId;
	}

	public String getInterceptionPointIdString()
	{
		return interceptionPointIdString;
	}

	public void setInterceptionPointIdString(String interceptionPointIdString)
	{
		this.interceptionPointIdString = interceptionPointIdString;
	}

	public String getEntityName()
	{
		return entityName;
	}

	public void setEntityName(String entityName)
	{
		this.entityName = entityName;
	}

	public String getEntityId()
	{
		return entityId;
	}

	public void setEntityId(String entityId)
	{
		this.entityId = entityId;
	}

	public Collection getSubscriptionVOList()
	{
		return subscriptionVOList;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List getContentTypeDefintionVOList()
	{
		return contentTypeDefintionVOList;
	}

	public List getCategoryVOList()
	{
		return categoryVOList;
	}

	public Integer getSubscriptionId()
	{
		return subscriptionId;
	}

	public void setSubscriptionId(Integer subscriptionId)
	{
		this.subscriptionId = subscriptionId;
	}

	public List getInterceptionPointVOList()
	{
		return interceptionPointVOList;
	}
}