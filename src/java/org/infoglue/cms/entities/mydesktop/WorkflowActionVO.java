/* ===============================================================================
*
* Part of the InfoGlue Content Management Platform (www.infoglue.org)
*
* ===============================================================================
*
* Copyright (C) Mattias Bogeblad
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

package org.infoglue.cms.entities.mydesktop;

import java.util.Map;

import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.util.ConstraintExceptionBuffer;

/**
 * This is the general action description object. Can be used by any workflow engine hopefully.
 *
 * @author Mattias Bogeblad
 */

public class WorkflowActionVO implements BaseEntityVO
{
	private Integer id;
	private Long workflowId;
	private WorkflowStepVO step;
	private String name;
	private String view;
	private boolean autoExecute;
	private Map metaAttributes;

	public Integer getId()
	{
		return this.id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public Long getWorkflowId()
	{
		return workflowId;
	}

	public void setWorkflowId(Long workflowId)
	{
		this.workflowId = workflowId;
	}

	public WorkflowStepVO getStep()
	{
		return step;
	}

	public void setStep(WorkflowStepVO step)
	{
		this.step = step;
	}

	/**
	 * Convenience method to retrn the name of the associated step.
	 * @return the name of the step that this action is part of.
	 */
	public String getStepName()
	{
		return (step == null) ? null : step.getName();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getView()
	{
		return view;
	}

	public void setView(String view)
	{
		this.view = view;
	}

	public boolean isAutoExecute()
	{
		return autoExecute;
	}

	public void setAutoExecute(boolean autoExecute)
	{
		this.autoExecute = autoExecute;
	}

	public Map getMetaAttributes()
	{
		return metaAttributes;
	}

	public void setMetaAttributes(Map metaAttributes)
	{
		this.metaAttributes = metaAttributes;
	}

	public ConstraintExceptionBuffer validate()
	{
		return new ConstraintExceptionBuffer();
	}

	public boolean hasView()
	{
		return view != null && view.length() > 0;
	}

	public String toString()
	{
		return new StringBuffer(getClass().getName())
				.append(" id=").append(id)
				.append(" name=").append(name)
				.append(" step=").append(getStepName())
				.append(" view=").append(view)
				.append(" workflowId=").append(workflowId).toString();
	}
}
