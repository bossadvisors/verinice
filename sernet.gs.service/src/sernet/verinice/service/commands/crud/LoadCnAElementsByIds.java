/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service.commands.crud;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import sernet.verinice.interfaces.GenericCommand;
import sernet.verinice.interfaces.IBaseDao;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.HydratorUtil;

public class LoadCnAElementsByIds<T extends CnATreeElement> extends GenericCommand {

	private Class<T> clazz;
	private List<Integer> dbIDs;
	private List<T> foundItems;


	public LoadCnAElementsByIds(Class<T> clazz, List<Integer> dbIDs) {
		this.clazz = clazz;
		this.dbIDs = dbIDs;
	}

	public void execute() {
		IBaseDao<T, Serializable> dao = getDaoFactory().getDAO(clazz);
		foundItems = new ArrayList<T>();
		for (Integer id : dbIDs) {
			T found = dao.findById(id);
			foundItems.add(found);
			HydratorUtil.hydrateElement(dao, found, false);
		}
	}

	public List<T> getFoundItems() {
		return foundItems;
	}

	
	

}
