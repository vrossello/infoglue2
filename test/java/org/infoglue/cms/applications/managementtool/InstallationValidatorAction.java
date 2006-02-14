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

package org.infoglue.cms.applications.managementtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.AvailableServiceBindingController;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.DigitalAssetController;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeVersionControllerProxy;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.DigitalAssetVO;
import org.infoglue.cms.entities.management.AvailableServiceBinding;
import org.infoglue.cms.entities.management.AvailableServiceBindingVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.ValidationItem;
import org.infoglue.cms.entities.structure.ServiceBinding;
import org.infoglue.cms.entities.structure.ServiceBindingVO;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVersion;
import org.infoglue.cms.entities.structure.SiteNodeVersionVO;
import org.infoglue.cms.exception.SystemException;

import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.NotificationMessage;
import org.infoglue.deliver.util.HttpHelper;

import webwork.action.Action;

/**
 * @author Mattias Bogeblad
 *
 * This is a class responsible for doing basic system controls. It will check that the different parts of the system
 * works as it should after installation. 
 */

public class InstallationValidatorAction extends InfoGlueAbstractAction
{
    private Boolean validateDB = new Boolean(true);
    private Boolean validateFileSystem = new Boolean(true);
    private Boolean validateSystemSettings = new Boolean(true);
    private Boolean validateEntities = new Boolean(true);
    
    private List validatedItems = new ArrayList();
    
    /**
     * Just returns the input screen.
     */
    
    public String doInput()
    {
        return Action.INPUT;
    }
    
    /**
     * This method does all the tests. Here follows a list of what it tries to do:
     * 
     * 1. Insert a content, content version and a digital asset to the db.
     * 2. Read all the items created in 1.
     * 3. Dump the digital asset to file to ensure asset handling.
     * 4. Delete the items created in 1.
     * 5. Validate that the correct content types are available as well as other system settings.
     * 6. Validate all the tables and check so all entities can be written/read/updated/deleted.
     * 7. Print system settings?
     * 8. Check that indexes are in place.
     */
    
    public String doExecute() throws Exception
    {
        Database db = CastorDatabaseService.getDatabase();
        
        db.begin();

        try
        {
            validateDB(db);           
            validateDigitalAssetHandling(db);
            validateCacheNotification();
                
	        db.commit();
        }
        catch(Exception e)
        {
            getLogger().error("An error occurred so we should not complete the transaction:" + e, e);
            db.rollback();
            throw new SystemException(e.getMessage());
        }
        finally
        {
            db.close();
        }

        try
        {
            validateSiteNodes();
        }
        catch(Exception e)
        {
            getLogger().error("An error occurred so we should not complete the transaction:" + e, e);
            throw new SystemException(e.getMessage());
        }

        return Action.SUCCESS;
    }
    
    private void validateDB(Database db)
    {
        String name = "Database access";
        String description = "We try to create, read and delete a language to validate the database communication and O/R setup.";

        try
        {
            createAndDeleteLanguage(db);
            addValidationItem(name, description, true, "Test succeeded");
        }
        catch(Exception e)
        {
            addValidationItem(name, description, false, "An error occurred: " + e.getMessage());
        }
    }

    private void validateDigitalAssetHandling(Database db)
    {
        String name = "Digital Asset handling";
        String description = "We try to upload assets of different sizes and also tries to dump them afterwards to the filesystem.";

        try
        {
            DigitalAssetVO digitalAssetVO = createDigitalAsset(db);
            db.commit();
            db.begin();
            dumpDigitalAsset(digitalAssetVO, db);
            db.commit();
            db.begin();
            deleteDigitalAsset(digitalAssetVO, db);
            addValidationItem(name, description, true, "Test succeeded");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            addValidationItem(name, description, false, "An error occurred: " + e.getMessage());
        }
    }

    private void validateCacheNotification()
    {
        String name = "Cache notifications";
        String description = "We try to verify the cache update action on all known servers.";

        try
        {
    		String appPrefix = "internalDeliverUrl";
    		
    		int i = 0;
    		String deliverUrl = null;
    		while((deliverUrl = CmsPropertyHandler.getProperty(appPrefix + "." + i)) != null)
    		{ 
    		    String address = deliverUrl + "/UpdateCache!test.action";
    			
    			try
    			{
	    		    HttpHelper httpHelper = new HttpHelper();
	    			String response = httpHelper.getUrlContent(address);
	    			if(response.indexOf("test ok") == -1)
	    			    throw new Exception("Got wrong answer");
	    			
	    			addValidationItem(name, description, true, "Test succeeded on " + address + ": " + response);
    			}
    			catch(Exception e)
    			{
    			    addValidationItem(name, description, false, "Test failed on " + address + ":" + e.getMessage());
    			}
    			i++;
    		}

    		appPrefix = "publicDeliverUrl";
    		
    		i = 0;
    		deliverUrl = null;
    		while((deliverUrl = CmsPropertyHandler.getProperty(appPrefix + "." + i)) != null)
    		{ 
    		    String address = deliverUrl + "/UpdateCache!test.action";
    			
    			try
    			{
	    		    HttpHelper httpHelper = new HttpHelper();
	    			String response = httpHelper.getUrlContent(address);
	    			if(response.indexOf("test ok") == -1)
	    			    throw new Exception("Got wrong answer");
	    			
    			    addValidationItem(name, description, true, "Test succeeded on " + address + ": " + response);
    			}
    			catch(Exception e)
    			{
    			    addValidationItem(name, description, false, "Test failed on " + address + ":" + e.getMessage());
    			}
    			i++;
    		}
        }
        catch(Exception e)
        {
            addValidationItem(name, description, false, "An error occurred: " + e.getMessage());
        }
    }

    /**
     * This method creates and deletes a Language.
     * 
     * @param db
     * @throws Exception
     */
    private void createAndDeleteLanguage(Database db) throws Exception
    {
        LanguageVO languageVO = new LanguageVO();
        languageVO.setCharset("utf8");
        languageVO.setLanguageCode("test");
        languageVO.setName("test");
        LanguageVO newLanguageVO = LanguageController.getController().create(languageVO);
        LanguageController.getController().delete(db, languageVO);
    }

    /**
     * This method creates a digital asset.
     * 
     * @param db
     * @throws Exception
     */
    private DigitalAssetVO createDigitalAsset(Database db) throws Exception
    {
        DigitalAssetVO digitalAssetVO = new DigitalAssetVO();
        
        InputStream is = null;
        
        try
        {
            String contextPath = CmsPropertyHandler.getProperty("contextRootPath");
            File file = new File(contextPath + "images" + File.separator + "workinganim.gif");
            
            is = new FileInputStream(file);
            
            digitalAssetVO.setAssetContentType("image/gif");
            digitalAssetVO.setAssetFileName(file.getName());
            digitalAssetVO.setAssetFilePath("dummyPath");
            digitalAssetVO.setAssetFileSize(new Integer((int)file.length()));
            digitalAssetVO.setAssetKey("validationAsset");
            
            DigitalAssetController.getController().create(db, digitalAssetVO, is);  
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            if(is != null)
                is.close();            
        }
        
        return digitalAssetVO;
    }


    /**
     * This method dumps a digital asset.
     * 
     * @param db
     * @throws Exception
     */
    
    private void dumpDigitalAsset(DigitalAssetVO digitalAssetVO, Database db) throws Exception
    {
        String url = DigitalAssetController.getController().getDigitalAssetUrl(digitalAssetVO, db);
        if(url == null || url.length() == 0)
        {
            throw new SystemException("The system could not dump the asset to the filesystem and get the url.");            
        }
    }

    /**
     * This method deletes a digital asset.
     * 
     * @param db
     * @throws Exception
     */
    
    private void deleteDigitalAsset(DigitalAssetVO digitalAssetVO, Database db) throws Exception
    {
        DigitalAssetController.getController().delete(digitalAssetVO.getId(), db);
    }

    
    private void validateSiteNodes() throws Exception
    {
        Database db = CastorDatabaseService.getDatabase();
        
        db.begin();

        try
        {
	
	        String name = "Metainfo content ids on cmSiteNode";
	        String description = "This many sitenodes did not have the new property set - bad for performance.";
	
	        try
	        {
	            List siteNodes = SiteNodeController.getController().getSiteNodesWithoutMetaInfoContentId(db);
	            Iterator siteNodesIterator = siteNodes.iterator();
	            
	            AvailableServiceBinding availableServiceBinding = AvailableServiceBindingController.getController().getAvailableServiceBindingWithName("Meta information", db, true);
    			Integer metaInfoAvailableServiceBindingId = null;
    			if(availableServiceBinding != null)
    			    metaInfoAvailableServiceBindingId = availableServiceBinding.getAvailableServiceBindingId();
	            
	            while(siteNodesIterator.hasNext())
	            {
	                SiteNode siteNode = (SiteNode)siteNodesIterator.next(); 
	                
	                if(siteNode.getMetaInfoContentId() == null || siteNode.getMetaInfoContentId().intValue() == -1)
	                {
	                    Language masterLanguage = LanguageController.getController().getMasterLanguage(db, siteNode.getRepository().getId());
		    			Integer languageId = masterLanguage.getLanguageId();
		    			
		    			Integer metaInfoContentId = null;
		    			
		    			SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getLatestActiveSiteNodeVersion(db, siteNode.getId());
		    			
		    			boolean hasMetaInfo = false;
		    			
		    			Collection serviceBindings = siteNodeVersion.getServiceBindings();
		    			Iterator serviceBindingIterator = serviceBindings.iterator();
		    			while(serviceBindingIterator.hasNext())
		    			{
		    				ServiceBinding serviceBinding = (ServiceBinding)serviceBindingIterator.next();
		    				if(serviceBinding.getAvailableServiceBinding().getId().intValue() == metaInfoAvailableServiceBindingId.intValue())
		    				{
		    					List boundContents = ContentController.getInTransactionBoundContents(db, serviceBinding.getServiceBindingId()); 			
		    					if(boundContents.size() > 0)
		    	    			{
		    	    				ContentVO content = (ContentVO)boundContents.get(0);
		    	    				metaInfoContentId = content.getId();

		    	    				hasMetaInfo = true;
		    	    				
		    	    				break;
		     	    			}                					
		    				}
		    			}
	    			
		    			if(!hasMetaInfo)
		    			{
		        		    //System.out.println("Creating a new meta info for " + siteNode.getName());
		        		    ContentVO contentVO = SiteNodeController.getController().createSiteNodeMetaInfoContent(db, siteNode, siteNode.getRepository().getId(), this.getInfoGluePrincipal(), null).getValueObject();
		        		    metaInfoContentId = contentVO.getId(); 
		    			}
		    			    
	    			    siteNode.setMetaInfoContentId(metaInfoContentId);
	                }
	            }
	            
	            addValidationItem(name, description, true, "Fixed " + siteNodes.size() + " siteNodes.");
	        }
	        catch(Exception e)
	        {
	            addValidationItem(name, description, false, "An error occurred: " + e.getMessage());
	        }
	        
	        db.commit();
        }
        catch(Exception e)
        {
            getLogger().error("An error occurred so we should not complete the transaction:" + e, e);
            db.rollback();
            throw new SystemException(e.getMessage());
        }
        finally
        {
            db.close();
        }

    }

    /**
     * This method just adds an validation item to the list.
     * @param name
     * @param status
     */

    private void addValidationItem(String name, String description, boolean status, String reason)
    {
        ValidationItem validationItem = new ValidationItem();
        validationItem.setName(name);
        validationItem.setDescription(description);
        validationItem.setValidationResult(status);
        validationItem.setReason(reason);
        
        this.validatedItems.add(validationItem);    
    }
    
    public Boolean getValidateDB()
    {
        return validateDB;
    }
    
    public void setValidateDB(Boolean validateDB)
    {
        this.validateDB = validateDB;
    }
    
    public Boolean getValidateEntities()
    {
        return validateEntities;
    }
    
    public void setValidateEntities(Boolean validateEntities)
    {
        this.validateEntities = validateEntities;
    }
    
    public Boolean getValidateFileSystem()
    {
        return validateFileSystem;
    }
    
    public void setValidateFileSystem(Boolean validateFileSystem)
    {
        this.validateFileSystem = validateFileSystem;
    }
    
    public Boolean getValidateSystemSettings()
    {
        return validateSystemSettings;
    }
    
    public void setValidateSystemSettings(Boolean validateSystemSettings)
    {
        this.validateSystemSettings = validateSystemSettings;
    }
    
    public List getValidatedItems()
    {
        return validatedItems;
    }
}

