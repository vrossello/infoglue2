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

package org.infoglue.cms.util;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.infoglue.deliver.invokers.ComponentBasedHTMLPageInvoker;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.deliver.util.DeliverContextListener;
import org.infoglue.deliver.util.LiveInstanceMonitor;

import com.opensymphony.oscache.base.OSCacheUtility;



/**
 * This class functions as the entry-point for all initialization of the Cms-tool.
 * The class responds to the startup or reload of a whole context.
 */

public final class CmsContextListener implements ServletContextListener 
{
    private final static Logger logger = Logger.getLogger(CmsContextListener.class.getName());

	/**
	 * This method is called when the servlet context is 
	 * initialized(when the Web Application is deployed). 
	 * You can initialize servlet context related data here.
     */
	 
    public void contextInitialized(ServletContextEvent event) 
    {
		try
		{
			String contextRootPath = event.getServletContext().getRealPath("/");
			if(!contextRootPath.endsWith("/") && !contextRootPath.endsWith("\\")) 
				contextRootPath = contextRootPath + "/";
			
			System.out.println("\n**************************************");
			System.out.println("Initializing cms context for directory:" + contextRootPath);

			CmsPropertyHandler.setApplicationName("cms");
			// String up2datePath = event.getServletContext().getRealPath("/") + "ut2date" + File.separator;
			// CmsPropertyHandler.setProperty("up2datePath", up2datePath);
			
			CmsPropertyHandler.setContextRootPath(contextRootPath); 
			
			String logPath = CmsPropertyHandler.getLogPath();
			
			Enumeration enumeration = Logger.getLogger("org.infoglue.cms").getAllAppenders();
	        while(enumeration.hasMoreElements())
	        {
	        	RollingFileAppender appender = (RollingFileAppender)enumeration.nextElement();
	            if(appender.getName().equalsIgnoreCase("INFOGLUE-FILE"))
	            {
	            	appender.setFile(logPath);
	    			appender.activateOptions();
	            	Logger.getLogger("org.infoglue.deliver.invokers.ComponentBasedHTMLPageInvoker").addAppender(appender);
	            	break;
	            }
	        }

			String URIEncoding = CmsPropertyHandler.getURIEncoding();
			
			//Starting the cache-expire-thread
			String expireCacheAutomaticallyString = CmsPropertyHandler.getExpireCacheAutomatically();
			Boolean expireCacheAutomatically = false;
			if(expireCacheAutomaticallyString != null)
				expireCacheAutomatically = Boolean.parseBoolean(expireCacheAutomaticallyString);
			
			if(expireCacheAutomatically)
			{
				CacheController cacheController = new CacheController();
				cacheController.setExpireCacheAutomatically(expireCacheAutomatically);
	
				String intervalString = CmsPropertyHandler.getCacheExpireInterval();
				if(intervalString != null)
					cacheController.setCacheExpireInterval(Integer.parseInt(intervalString));
			
				if(cacheController.getExpireCacheAutomatically())
					cacheController.start();
			}

			new Thread(new Runnable() { public void run() {try {CacheController.preCacheCMSEntities();} catch (Exception e) {}}}).start();

			System.out.println("Starting deliver instance monitoring");
			LiveInstanceMonitor.getInstance();

			OSCacheUtility.setServletCacheParams(event.getServletContext());
			
			CmsPropertyHandler.setStartupTime(new Date()); 

			System.out.println("**************************************\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
    }

    /**
     * This method is invoked when the Servlet Context 
     * (the Web Application) is undeployed or 
     * WebLogic Server shuts down.
     */			    

    public void contextDestroyed(ServletContextEvent event) 
    {
		System.out.println("contextDestroyed....");

    }
}

