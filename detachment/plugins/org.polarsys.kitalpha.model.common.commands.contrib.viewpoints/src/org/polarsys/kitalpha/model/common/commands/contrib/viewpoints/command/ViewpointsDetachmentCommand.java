/*******************************************************************************
 * Copyright (c) 2013 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    THALES GLOBAL SERVICES - Initial API and implementation
 *******************************************************************************/
package org.polarsys.kitalpha.model.common.commands.contrib.viewpoints.command;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.polarsys.kitalpha.model.common.commands.action.ModelCommand;
import org.polarsys.kitalpha.model.common.commands.contrib.viewpoints.Messages;
import org.polarsys.kitalpha.model.common.commands.exception.ModelCommandException;
import org.polarsys.kitalpha.model.common.scrutiny.analyzer.Scrutineer;
import org.polarsys.kitalpha.model.common.scrutiny.analyzer.ModelScrutinyException;
import org.polarsys.kitalpha.model.common.scrutiny.contrib.viewpoints.scrutinizes.UsedAFViewpoints;
import org.polarsys.kitalpha.model.common.scrutiny.contrib.viewpoints.trees.ViewpointTreeContainer;
import org.polarsys.kitalpha.model.common.scrutiny.interfaces.IScrutinize;
import org.polarsys.kitalpha.model.common.scrutiny.registry.ModelScrutinyRegistry.RegistryElement;

/**
 * @author Faycal Abka
 */
public class ViewpointsDetachmentCommand extends ModelCommand {
	
	Logger LOGGER = Logger.getLogger(ViewpointsDetachmentCommand.class);

	public ViewpointsDetachmentCommand() {
	}

	@Override
	public void exec(Resource resource, IProgressMonitor monitor) 
			throws ModelCommandException {
		RegistryElement regElt;
		try {
			
			monitor.beginTask(Messages.REMOVE_VIEWPOINTS, 1);
			
			SubMonitor subMonitor = SubMonitor.convert(monitor);
			
			//TODO check finder id if it is null
			regElt = Scrutineer.getRegistryElement(getModelAnalysisID());
			UsedAFViewpoints usedVpFinder = getUsedAFFinder(regElt);
			
			if (usedVpFinder != null){
				ViewpointTreeContainer container = usedVpFinder.getAnalysisResult();
				Collection<String> unSelectedUri = container.getUriToRemove();
				cleanUnselectedUris(resource, unSelectedUri, subMonitor);
				container.dispose();
			}
			subMonitor.worked(1);
			subMonitor.done();
			
		} catch (ModelScrutinyException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
		}
		
	}

	private void cleanUnselectedUris(Resource resource,
			Collection<String> unSelectedUri, IProgressMonitor monitor) {
		
		Collection<Resource> allResources = resource.getResourceSet().getResources();
		
		for (Resource resource2 : allResources) 
		{
			
			TreeIterator<EObject> it = resource2.getAllContents();
			
			Collection<EObject> eObjectToRemove = new HashSet<EObject>();
			
			while (it.hasNext())
			{
				EObject eObject = it.next();
				String current_uri = eObject.eClass().getEPackage().getNsURI();
				
				if (unSelectedUri.contains(current_uri))
				{
					
					eObjectToRemove.add(eObject);
				}
			}
			
			if (eObjectToRemove != null && !eObjectToRemove.isEmpty())
			{
				for (EObject eObject2 : eObjectToRemove) 
				{
					monitor.subTask(Messages.bind(Messages.REMOVE_OBJECT, new Object[] { EcoreUtil.getURI(eObject2), eObject2.eClass().getEPackage().getNsURI() }));
					EcoreUtil.delete(eObject2, false);
				}
			}
		}
		
	}

	@SuppressWarnings("rawtypes")
	private UsedAFViewpoints getUsedAFFinder(RegistryElement regElt) {
		
		Collection<IScrutinize> finders = regElt.getFinders();
		
		for (IScrutinize iFinder : finders) {
			if (iFinder instanceof UsedAFViewpoints){
				//There are one finder of viewpoints
				return (UsedAFViewpoints)iFinder;
			}
		}
		return null;
	}

}