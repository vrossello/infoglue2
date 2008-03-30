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

package org.infoglue.cms.controllers.kernel.impl.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.databeans.ComponentPropertyDefinition;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersion;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.ContentTypeAttribute;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.XMLHelper;
import org.infoglue.cms.util.sorters.ContentComparator;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.controllers.kernel.impl.simple.NodeDeliveryController;
import org.infoglue.deliver.util.CacheController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class handles all access to components and other actions.
 * 
 * @author Mattias Bogeblad
 */

public class ComponentController extends BaseController
{
    private final static Logger logger = Logger.getLogger(ComponentController.class.getName());

    /**
	 * Factory method
	 */

	public static ComponentController getController()
	{
		return new ComponentController();
	}

	/**
	 * This method returns a sorted list of components.
	 * @param sortAttribute
	 * @return
	 * @throws SystemException
	 * @throws Bug
	 */
	
/*	public List getComponentVOList(String sortAttribute, String direction, String[] allowedComponentNames, String[] disallowedComponentNames) throws SystemException, Bug, Exception
	{
		List componentVOList = null;
		
		Database db = CastorDatabaseService.getDatabase();
		try
		{
			beginTransaction(db);
			
			componentVOList = getComponentVOList(sortAttribute, direction, allowedComponentNames, disallowedComponentNames, db);
			    
			commitTransaction(db);
		}
		catch ( Exception e )
		{
		    e.printStackTrace();
			rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch a list of users in this group. Reason:" + e.getMessage(), e);			
		}		
		
		return componentVOList;
	}
*/	
	public List getComponentVOList(String sortAttribute, String direction, String[] allowedComponentNames, String[] disallowedComponentNames, InfoGluePrincipal principal) throws SystemException, Bug, Exception
	{
		List componentVOList = null;
		
		Database db = CastorDatabaseService.getDatabase();
		try
		{
			beginTransaction(db);
			
			componentVOList = getComponentVOList(sortAttribute, direction, allowedComponentNames, disallowedComponentNames, db, principal);
			    
			commitTransaction(db);
		}
		catch ( Exception e )
		{
		    e.printStackTrace();
			rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch a list of users in this group. Reason:" + e.getMessage(), e);			
		}		
		
		return componentVOList;
	}

	/**
	 * This method returns a sorted list of components within a transaction.
	 * @param sortAttribute
	 * @return
	 * @throws SystemException
	 * @throws Bug
	 */
	
	/*
	public List getComponentVOList(String sortAttribute, String direction, String[] allowedComponentNames, String[] disallowedComponentNames, Database db) throws SystemException, Bug, Exception
	{
	    String allowedComponentNamesString = "";
	    if(allowedComponentNames != null)
	    {
	        for(int i=0; i<allowedComponentNames.length; i++)
	            allowedComponentNamesString = allowedComponentNames[i] + ":";
	    }

	    String disallowedComponentNamesString = "";
	    if(disallowedComponentNames != null)
	    {
	        for(int i=0; i<disallowedComponentNames.length; i++)
	        	disallowedComponentNamesString = disallowedComponentNames[i] + ":";
	    }

	    String componentsKey = "components_" + sortAttribute + "_" + direction + "_" + allowedComponentNamesString + "_" + disallowedComponentNamesString;
	    List components = (List)CacheController.getCachedObject("componentContentsCache", componentsKey);
		if(components != null)
		{
			logger.info("There was cached components:" + components.size());
		}
		else
		{
		    components = getComponents(allowedComponentNames, disallowedComponentNames);
			Iterator componentsIterator = components.iterator();
			while(componentsIterator.hasNext())
			{
			    ContentVO contentVO = (ContentVO)componentsIterator.next();
			    
			    LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(contentVO.getRepositoryId(), db); //.getMasterLanguage(db, contentVO.getRepositoryId());
				ContentVersion contentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersion(contentVO.getId(), masterLanguage.getId(), db);
				
				String groupName = "Unknown";
				String description = "Unknown";
				
				if(contentVersion != null)
				{
				    groupName = ContentVersionController.getContentVersionController().getAttributeValue(contentVersion.getValueObject(), "GroupName", false);
				    description = ContentVersionController.getContentVersionController().getAttributeValue(contentVersion.getValueObject(), "Description", false);
				}
	
				contentVO.getExtraProperties().put("GroupName", groupName);
				contentVO.getExtraProperties().put("Description", description);
			}
			
			CacheController.cacheObject("componentContentsCache", componentsKey, components);
		}
		
		ContentComparator comparator = new ContentComparator(sortAttribute, direction, null);
		Collections.sort(components, comparator);
		
		return components;
	}
	*/

	public List getComponentVOList(String sortAttribute, String direction, String[] allowedComponentNames, String[] disallowedComponentNames, Database db, InfoGluePrincipal principal) throws SystemException, Bug, Exception
	{
		if(principal == null)
			return null;
			
	    String allowedComponentNamesString = "";
	    if(allowedComponentNames != null)
	    {
	        for(int i=0; i<allowedComponentNames.length; i++)
	            allowedComponentNamesString = allowedComponentNames[i] + ":";
	    }

	    String disallowedComponentNamesString = "";
	    if(disallowedComponentNames != null)
	    {
	        for(int i=0; i<disallowedComponentNames.length; i++)
	        	disallowedComponentNamesString = disallowedComponentNames[i] + ":";
	    }

	    String componentsKey = "components_" + sortAttribute + "_" + direction + "_" + allowedComponentNamesString + "_" + disallowedComponentNamesString + "_" + principal.getName();
	    List components = (List)CacheController.getCachedObject("componentContentsCache", componentsKey);
		if(components != null)
		{
			logger.info("There was cached components:" + components.size());
		}
		else
		{
		    components = getComponents(allowedComponentNames, disallowedComponentNames, principal, db);
			Iterator componentsIterator = components.iterator();
			while(componentsIterator.hasNext())
			{
			    ContentVO contentVO = (ContentVO)componentsIterator.next();
			    
			    LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(contentVO.getRepositoryId(), db); //.getMasterLanguage(db, contentVO.getRepositoryId());
				ContentVersion contentVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersion(contentVO.getId(), masterLanguage.getId(), db);
				
				String groupName = "Unknown";
				String description = "Unknown";
				
				if(contentVersion != null)
				{
				    groupName = ContentVersionController.getContentVersionController().getAttributeValue(contentVersion.getValueObject(), "GroupName", false);
				    description = ContentVersionController.getContentVersionController().getAttributeValue(contentVersion.getValueObject(), "Description", false);
				}
	
				contentVO.getExtraProperties().put("GroupName", groupName);
				contentVO.getExtraProperties().put("Description", description);
			}
			
			CacheController.cacheObject("componentContentsCache", componentsKey, components);
		}
		
		ContentComparator comparator = new ContentComparator(sortAttribute, direction, null);
		Collections.sort(components, comparator);
		
		return components;
	}

	
	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate"
	 */
	/*
	public List getComponents(String[] allowedComponentNames, String[] disallowedComponentNames) throws Exception
	{
		HashMap arguments = new HashMap();
		arguments.put("method", "selectListOnContentTypeName");
		
		List argumentList = new ArrayList();
		HashMap argument = new HashMap();
		argument.put("contentTypeDefinitionName", "HTMLTemplate");
		argumentList.add(argument);
		arguments.put("arguments", argumentList);
		
		List results = ContentController.getContentController().getContentVOList(arguments);
		
		if(allowedComponentNames != null && allowedComponentNames.length > 0)
		{
		    Iterator resultsIterator = results.iterator();
		    while(resultsIterator.hasNext())
		    {
		        ContentVO contentVO = (ContentVO)resultsIterator.next();
		        boolean isAllowed = false;
		        for(int i=0; i<allowedComponentNames.length; i++)
		        {
		            if(contentVO.getName().equals(allowedComponentNames[i]))
		                isAllowed = true;
		        }
		        
		        if(!isAllowed)
		            resultsIterator.remove();
		    }
		}

		if(disallowedComponentNames != null && disallowedComponentNames.length > 0)
		{
		    Iterator resultsIterator = results.iterator();
		    while(resultsIterator.hasNext())
		    {
		        ContentVO contentVO = (ContentVO)resultsIterator.next();
		        boolean isAllowed = true;
		        for(int i=0; i<disallowedComponentNames.length; i++)
		        {
		            if(contentVO.getName().equals(disallowedComponentNames[i]))
		                isAllowed = false;
		        }
		        
		        if(!isAllowed)
		            resultsIterator.remove();
		    }
		}

		return results;	
	}
	*/
	
	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate"
	 */
	
	public List getComponents(String[] allowedComponentNames, String[] disallowedComponentNames, InfoGluePrincipal principal) throws Exception
	{
		HashMap arguments = new HashMap();
		arguments.put("method", "selectListOnContentTypeName");
		
		List argumentList = new ArrayList();
		HashMap argument = new HashMap();
		argument.put("contentTypeDefinitionName", "HTMLTemplate");
		argumentList.add(argument);
		HashMap argument2 = new HashMap();
		argument2.put("contentTypeDefinitionName", "PagePartTemplate");
		argumentList.add(argument2);
		arguments.put("arguments", argumentList);
		
		List results = ContentControllerProxy.getController().getACContentVOList(principal, arguments);
		
		if(allowedComponentNames != null && allowedComponentNames.length > 0)
		{
		    Iterator resultsIterator = results.iterator();
		    while(resultsIterator.hasNext())
		    {
		        ContentVO contentVO = (ContentVO)resultsIterator.next();
		        boolean isAllowed = false;
		        for(int i=0; i<allowedComponentNames.length; i++)
		        {
		            if(contentVO.getName().equals(allowedComponentNames[i]))
		                isAllowed = true;
		        }
		        
		        if(!isAllowed)
		            resultsIterator.remove();
		    }
		}

		if(disallowedComponentNames != null && disallowedComponentNames.length > 0)
		{
		    Iterator resultsIterator = results.iterator();
		    while(resultsIterator.hasNext())
		    {
		        ContentVO contentVO = (ContentVO)resultsIterator.next();
		        boolean isAllowed = true;
		        for(int i=0; i<disallowedComponentNames.length; i++)
		        {
		            if(contentVO.getName().equals(disallowedComponentNames[i]))
		                isAllowed = false;
		        }
		        
		        if(!isAllowed)
		            resultsIterator.remove();
		    }
		}

		return results;	
	}


	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate"
	 */
	
	public List getComponents(String[] allowedComponentNames, String[] disallowedComponentNames, InfoGluePrincipal principal, Database db) throws Exception
	{
		HashMap arguments = new HashMap();
		arguments.put("method", "selectListOnContentTypeName");
		
		List argumentList = new ArrayList();
		HashMap argument = new HashMap();
		argument.put("contentTypeDefinitionName", "HTMLTemplate");
		argumentList.add(argument);
		HashMap argument2 = new HashMap();
		argument2.put("contentTypeDefinitionName", "PagePartTemplate");
		argumentList.add(argument2);
		arguments.put("arguments", argumentList);
		
		List results = ContentControllerProxy.getController().getACContentVOList(principal, arguments, db);
		
		if(allowedComponentNames != null && allowedComponentNames.length > 0)
		{
		    Iterator resultsIterator = results.iterator();
		    while(resultsIterator.hasNext())
		    {
		        ContentVO contentVO = (ContentVO)resultsIterator.next();
		        boolean isAllowed = false;
		        for(int i=0; i<allowedComponentNames.length; i++)
		        {
		            if(contentVO.getName().equals(allowedComponentNames[i]))
		                isAllowed = true;
		        }
		        
		        if(!isAllowed)
		            resultsIterator.remove();
		    }
		}

		if(disallowedComponentNames != null && disallowedComponentNames.length > 0)
		{
		    Iterator resultsIterator = results.iterator();
		    while(resultsIterator.hasNext())
		    {
		        ContentVO contentVO = (ContentVO)resultsIterator.next();
		        boolean isAllowed = true;
		        for(int i=0; i<disallowedComponentNames.length; i++)
		        {
		            if(contentVO.getName().equals(disallowedComponentNames[i]))
		                isAllowed = false;
		        }
		        
		        if(!isAllowed)
		            resultsIterator.remove();
		    }
		}

		return results;	
	}

	public void checkAndAutoCreateContents(Integer siteNodeId, Integer languageId, Integer masterLanguageId, String assetKey, Integer newComponentId, Document document, Integer componentContentId, InfoGluePrincipal principal) throws Exception, SystemException, Bug, ConstraintException
	{
		Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
        	checkAndAutoCreateContents(db, siteNodeId, languageId, masterLanguageId, assetKey, newComponentId, document, componentContentId, principal);
        	
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
	}
	
	public void checkAndAutoCreateContents(Database db, Integer siteNodeId, Integer languageId, Integer masterLanguageId, String assetKey, Integer newComponentId, Document document, Integer componentContentId, InfoGluePrincipal principal) throws Exception, SystemException, Bug, ConstraintException
	{
		List componentPropertyDefinitions = ComponentPropertyDefinitionController.getController().getComponentPropertyDefinitions(db, componentContentId, masterLanguageId);
		Iterator componentPropertyDefinitionsIterator = componentPropertyDefinitions.iterator();
		while(componentPropertyDefinitionsIterator.hasNext())
		{
			ComponentPropertyDefinition componentPropertyDefinition = (ComponentPropertyDefinition)componentPropertyDefinitionsIterator.next();
			System.out.println("componentPropertyDefinition:" + componentPropertyDefinition);
			if(componentPropertyDefinition.getAutoCreateContent())
			{
				System.out.println("componentPropertyDefinition vill ha en auto create:" + componentPropertyDefinition);
				String method = componentPropertyDefinition.getAutoCreateContentMethod();
				String path = componentPropertyDefinition.getAutoCreateContentPath();
				String allowedContentTypeNames = componentPropertyDefinition.getAllowedContentTypeNames();
				String allowedContentTypeName = allowedContentTypeNames.split(",")[0];
				ContentTypeDefinitionVO createContentTypeDefinitionVO = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName(allowedContentTypeName, db);
				System.out.println("method:" + method);
				System.out.println("path:" + path);
				System.out.println("createContentTypeDefinitionVO:" + createContentTypeDefinitionVO.getName());
				if(path.indexOf("/") == 0)
					path = path.substring(1);
				
				SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId, db);
				ContentVO parentContentVO = ContentController.getContentController().getRootContent(db, siteNodeVO.getRepositoryId(), principal.getName(), true).getValueObject();
				if(method.equals("siteStructure"))
				{
					String siteNodePath = SiteNodeController.getController().getSiteNodePath(siteNodeVO.getId(), db);
					System.out.println("siteNodePath:" + siteNodePath);
					parentContentVO = ContentController.getContentController().getContentVOWithPath(siteNodeVO.getRepositoryId(), path + siteNodePath, true, principal, db);
				}
				else if(method.equals("fixedPath"))
				{
					parentContentVO = ContentController.getContentController().getContentVOWithPath(siteNodeVO.getRepositoryId(), path, true, principal, db);
				}
				
				ContentVO autoContentVO = new ContentVO();
				autoContentVO.setName(siteNodeVO.getName() + " - " + componentPropertyDefinition.getName());
				autoContentVO.setCreatorName(principal.getName());
				autoContentVO.setIsBranch(false);

				StringBuffer sb = new StringBuffer();
				sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><article xmlns=\"x-schema:ArticleSchema.xml\"><attributes>");
				List contentTypeAttributes = ContentTypeDefinitionController.getController().getContentTypeAttributes(createContentTypeDefinitionVO.getSchemaValue());
				Iterator contentTypeAttributesIterator = contentTypeAttributes.iterator();
				while(contentTypeAttributesIterator.hasNext())
				{
					ContentTypeAttribute contentTypeAttribute = (ContentTypeAttribute)contentTypeAttributesIterator.next();
					String initialValue = contentTypeAttribute.getContentTypeAttribute("initialData").getContentTypeAttributeParameterValue().getValue("label");
					if(initialValue == null || initialValue.trim().equals(""))
						initialValue = "State " + contentTypeAttribute.getName();
					sb.append("<" + contentTypeAttribute.getName() + "><![CDATA[" + initialValue + "]]></" + contentTypeAttribute.getName() + ">");
				}
				sb.append("</attributes></article>");
				
				String defaultValue = sb.toString(); 

				ContentVersionVO autoContentVersionVO = new ContentVersionVO();
				autoContentVersionVO.setVersionComment("Automatically created");
				autoContentVersionVO.setVersionModifier(principal.getName());
				autoContentVersionVO.setVersionValue(defaultValue);
				
				autoContentVO = ContentController.getContentController().create(db, parentContentVO.getId(), createContentTypeDefinitionVO.getId(), siteNodeVO.getRepositoryId(), autoContentVO).getValueObject();
				ContentVersionController.getContentVersionController().create(autoContentVO.getId(), languageId, autoContentVersionVO, null, db);
				
				Locale locale = LanguageController.getController().getLocaleWithId(languageId, db);
				LanguageVO masterLanguageVO = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId(), db);		

				String entity = "Content";
				Integer entityId  = new Integer(autoContentVO.getId());
				String propertyName = "" + componentPropertyDefinition.getName();
				
				System.out.println("FFFFFF:" + XMLHelper.serializeDom(document.getDocumentElement(), new StringBuffer()).toString());
				
				ComponentController.getController().addComponentPropertyBinding(document, locale, siteNodeId, languageId, masterLanguageVO.getId(), entity, entityId, propertyName, newComponentId, path, assetKey, principal);

				System.out.println("GGGGG:" + XMLHelper.serializeDom(document.getDocumentElement(), new StringBuffer()).toString());
			}
		}
	}

	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public void addComponentPropertyBinding(Document document,
											Locale locale,
											Integer siteNodeId, 
											Integer languageId, 
											Integer masterLanguageId,
											String entity,
											Integer entityId,
											String propertyName,
											Integer componentId,
											String path,
											String assetKey,
											InfoGluePrincipal principal) throws Exception
	{
		//logger.info("************************************************************");
		//logger.info("* doAddComponentPropertyBinding                            *");
		//logger.info("************************************************************");
		//logger.info("siteNodeId:" + this.siteNodeId);
		//logger.info("languageId:" + this.languageId);
		//logger.info("contentId:" + this.contentId);
		//logger.info("componentId:" + this.componentId);
		//logger.info("slotId:" + this.slotId);
		//logger.info("specifyBaseTemplate:" + this.specifyBaseTemplate);
		//logger.info("assetKey:" + assetKey);
				
		String componentPropertyXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + propertyName + "']";
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
		System.out.println("AAAAAAAAAAAAAAAAAAAAAA:" + componentPropertyXPath + ":" + anl.getLength());
		if(anl.getLength() == 0)
		{
			String componentXPath = "//component[@id=" + componentId + "]/properties";
			//logger.info("componentXPath:" + componentXPath);
			NodeList componentNodeList = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
			System.out.println("AAAAAAAAAAAAAAAAAAAAAA:" + componentXPath + ":" + componentNodeList.getLength());
			if(componentNodeList.getLength() > 0)
			{
				Element componentProperties = (Element)componentNodeList.item(0);
				if(entity.equalsIgnoreCase("SiteNode"))
				    addPropertyElement(componentProperties, propertyName, path, "siteNodeBinding", locale);
				else
				    addPropertyElement(componentProperties, propertyName, path, "contentBinding", locale);
				
				anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
				System.out.println("anl:" + anl.getLength());
			}
		}
		
		System.out.println("anl after:" + anl);
		if(anl.getLength() > 0)
		{
			Element component = (Element)anl.item(0);
			component.setAttribute("path", path);
			//component.setAttribute("path_" + locale.getLanguage(), path);
			NamedNodeMap attributes = component.getAttributes();
			logger.debug("NumberOfAttributes:" + attributes.getLength() + ":" + attributes);
			
			System.out.println("11111:" + XMLHelper.serializeDom(component, new StringBuffer()).toString());
			
			List removableAttributes = new ArrayList();
			for(int i=0; i<attributes.getLength(); i++)
			{
				Node node = attributes.item(i);
				logger.debug("Node:" + node.getNodeName());
				if(node.getNodeName().startsWith("path_"))
				{
					removableAttributes.add("" + node.getNodeName());
				}
			}
			
			Iterator removableAttributesIterator = removableAttributes.iterator();
			while(removableAttributesIterator.hasNext())
			{
				String attributeName = (String)removableAttributesIterator.next();
				logger.debug("Removing node:" + attributeName);
				component.removeAttribute(attributeName);
			}
			
			NodeList children = component.getChildNodes();
			for(int i=0; i < children.getLength(); i++)
			{
				Node node = children.item(i);
				component.removeChild(node);
			}
			
			addBindingElement(component, entity, entityId, assetKey);
			
			System.out.println("22222:" + XMLHelper.serializeDom(component, new StringBuffer()).toString());
		}
	}

	/**
	 * This method fetches the template-string.
	 */
    
	private String getPageComponentsString(Integer siteNodeId, Integer languageId, InfoGluePrincipal principal) throws SystemException, Exception
	{
		String template = null;
    	
		try
		{
			ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, new Integer(-1)).getBoundContent(principal, siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());

			if(contentVO == null)
				throw new SystemException("There was no template bound to this page which makes it impossible to render.");	
			
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), languageId);
			if(contentVersionVO == null)
			{
				SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
				LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());
				contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getLanguageId());
			}
			
			template = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO.getId(), "ComponentStructure", false);
			
			if(template == null)
				throw new SystemException("There was no template bound to this page which makes it impossible to render.");	
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}

		return template;
	}

	/**
	 * This method creates a parameter for the given input type.
	 * This is to support form steering information later.
	 */
	
	private Element addPropertyElement(Element parent, String name, String path, String type, Locale locale)
	{
		Element element = parent.getOwnerDocument().createElement("property");
		element.setAttribute("name", name);
		
		if(type.equalsIgnoreCase("siteNodeBinding") || type.equalsIgnoreCase("contentBinding"))
		{
			element.setAttribute("path", path);
			element.setAttribute("path_" + locale.getLanguage(), path);
		}
		else
		{
			element.setAttribute("path_" + locale.getLanguage(), path);
		}
		
		element.setAttribute("type", type);
		parent.appendChild(element);
		return element;
	}

	/**
	 * This method creates a parameter for the given input type.
	 * This is to support form steering information later.
	 */
	
	private Element addBindingElement(Element parent, String entity, Integer entityId, String assetKey)
	{
		Element element = parent.getOwnerDocument().createElement("binding");
		element.setAttribute("entityId", entityId.toString());
		element.setAttribute("entity", entity);
		if(assetKey != null && !assetKey.equals(""))
			element.setAttribute("assetKey", assetKey);
		
		parent.appendChild(element);
		
		return element;
	}

    /* (non-Javadoc)
     * @see org.infoglue.cms.controllers.kernel.impl.simple.BaseController#getNewVO()
     */
    public BaseEntityVO getNewVO()
    {
        return null;
    }
	
	
}
