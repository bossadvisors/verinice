/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak@sernet.de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.ui.rcp.main.service.crudcommands;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import sernet.gs.ui.rcp.main.bsi.model.BausteinUmsetzung;
import sernet.gs.ui.rcp.main.bsi.model.MassnahmenUmsetzung;
import sernet.gs.ui.rcp.main.bsi.risikoanalyse.model.FinishedRiskAnalysis;
import sernet.gs.ui.rcp.main.bsi.risikoanalyse.model.GefaehrdungsUmsetzung;
import sernet.gs.ui.rcp.main.common.model.CnATreeElement;
import sernet.gs.ui.rcp.main.common.model.HydratorUtil;
import sernet.gs.ui.rcp.main.connect.IBaseDao;
import sernet.gs.ui.rcp.main.connect.RetrieveInfo;
import sernet.gs.ui.rcp.main.service.commands.GenericCommand;

@SuppressWarnings("serial")
public class LoadChildrenForExpansion extends GenericCommand {
	
	private static final Logger log = Logger.getLogger(LoadChildrenForExpansion.class);

	private CnATreeElement parent;
	private Integer dbId;
	private Class<? extends CnATreeElement> clazz;
	
	private Set<Class<?>> filteredClasses;

	public LoadChildrenForExpansion(CnATreeElement parent) {
		this(parent, new HashSet<Class<?>>());
	}

	public LoadChildrenForExpansion(CnATreeElement parent, Set<Class<?>> filteredClasses) {
		// slim down for transfer:
		dbId = parent.getDbId();
		clazz = parent.getClass();
		this.parent = null;
		this.filteredClasses = filteredClasses;
	}
	
	public void execute() {
		IBaseDao<? extends CnATreeElement, Serializable> dao = getDaoFactory().getDAO(clazz);
		
		RetrieveInfo ri = new RetrieveInfo();
		ri.setChildren(true).setChildrenProperties(true).setProperties(true).setLinksDown(false).setLinksUp(false);
		parent = dao.retrieve(dbId,ri);
		if(parent!=null) {
			hydrate(parent);
			
			if (log.isDebugEnabled() && !filteredClasses.isEmpty())
				log.debug("Skipping the following model classes: " + filteredClasses);
			
			Set<CnATreeElement> children = parent.getChildren();
			for (CnATreeElement child : children) {
				if (!filteredClasses.contains(child.getClass()))
					hydrate(child);
			}
		}
	}

	private void hydrate(CnATreeElement element) {
		if (element == null)
			return;
		
		//log.debug("Hydrate element of type: " + element.getClass().getSimpleName());
		
		if (element instanceof MassnahmenUmsetzung) {
			MassnahmenUmsetzung mn = (MassnahmenUmsetzung) element;
			mn.getKapitelValue();
			mn.getTitel();	
			mn.getUmsetzung();
			mn.getUrl();
			mn.getStand();
			return;
		}
		
		RetrieveInfo ri = null;
		ri = new RetrieveInfo();
		ri.setChildren(true).setLinksDown(false);
		if (element instanceof BausteinUmsetzung) {
			ri.setChildrenProperties(true).setInnerJoin(true);
		}
		
		HydratorUtil.hydrateElement(getDaoFactory().getDAO(element.getClass()), element, ri);
		
		// initialize all children:
		if (element instanceof FinishedRiskAnalysis
				|| element instanceof GefaehrdungsUmsetzung) {
			Set<CnATreeElement> children = element.getChildren();
			for (CnATreeElement child : children) {
				hydrate(child);
			}
		}
		
		if (element instanceof BausteinUmsetzung) {
			IBaseDao<? extends BausteinUmsetzung, Serializable> dao = getDaoFactory().getDAO(BausteinUmsetzung.class);
			
			BausteinUmsetzung bst = (BausteinUmsetzung) element;
			bst.getKapitel();
			Set<CnATreeElement> massnahmen = bst.getChildren();
			for (CnATreeElement massnahme : massnahmen) {
				hydrate(massnahme);
			}
		}
		
		/*
		log.debug("Hydrating links down...");
		if (element.getLinksDown().size() > 0 ) {
			Set<CnALink> links = element.getLinks().getChildren();
			for (CnALink link : links) {
				link.getTitle();
				link.getDependency().getUuid();
			}
		}
		log.debug("Links down hydrated");
		*/
		
	}
	
	public CnATreeElement getElementWithChildren() {
		return parent;
	}
}
