/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.report.service.impl.dynamictable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Creates a table out of a map. The key of the map is a set of db-ids of
 * elements linked with dots. Data format for the table is: 
 * List<List<String>>.
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class TableGenerator {
    
    private static final Logger LOG = Logger.getLogger(TableGenerator.class);
    
    /**
     * Creates a table by converting the data of map allRowMap.
     * 
     * Each entry of the parameter allRowMap map holds the data of one cell of the result table.
     * The key of the map is a path of db-ids followed by the index of the column:
     * <DB-ID>[.<DB-ID>]#<COLUMN-INDEX>
     * 
     * The result can be used as a data the in BIRT reports.
     * 
     * @param allRowMap A map with all data
     * @return A table with all data
     */
    public static final List<List<String>> createTable(Map<String, String[]> allRowMap) {
        log(allRowMap);
        
        allRowMap = cleanUpRows(allRowMap);  
        
        log(allRowMap);
        
        List<List<String>> resultTable = new LinkedList<List<String>>();
        List<String> keyList =  new LinkedList<String>(allRowMap.keySet());        
        Collections.sort(keyList);
        for (String key : keyList) {
            resultTable.add(Arrays.asList(allRowMap.get(key)));
        }  
        return resultTable;
    }
    
    private static Map<String, String[]> cleanUpRows(Map<String, String[]> allRowMap) {
        Map<String, String[]> cleanMap = new HashMap<String, String[]>();
        List<String> keyList =  new LinkedList<String>(allRowMap.keySet());       
        Collections.sort(keyList);
        Iterator<String> keyIterator = keyList.iterator();
        String key1 = null;
        String[] row1 = null;
        String key2 =null;
        String[] row2= null;
        boolean merged = false;
        
        while(keyIterator.hasNext()) {            
            if(!merged) { 
                if(key1!=null) {
                    cleanMap.put(key1, row1); 
                }
                key1 = (key2!=null) ? key2 : keyIterator.next();  
                row1 = (row2!=null) ? row2 : allRowMap.get(key1);
                if(!keyIterator.hasNext()) {
                    cleanMap.put(key1, row1); 
                    break;
                }
            } else {
                key1 = key2;
            }
            key2 = keyIterator.next();
            row2= allRowMap.get(key2);
            merged = checkRows(key1, row1, key2, row2);
        }    
        cleanMap.put(key1, row1);  
        if(!merged && key2!=null) {
            cleanMap.put(key2, row2);
        }
        return cleanMap;
    }

  
    private static boolean checkRows(String key1, String[] row1, String key2, String[] row2) {
        boolean merged = false;
        if(startsWith(key2, key1)) {
            merge(row2,row1);
            merged = true;
        } 
        return merged;
    }

    private static boolean startsWith(String key2, String key1) {
        String keyClean2 = GenericDataModel.removeRowNumer(key2);
        String keyClean1 = GenericDataModel.removeRowNumer(key1);
        return keyClean2.startsWith(keyClean1);
    }

    private static void merge(String[] rowFrom, String[] rowTo) {
        for (int i = 0; i < rowTo.length; i++) {
            if(rowTo[i]==null) {
                rowTo[i] = rowFrom[i];
            }
        }      
    }

    private static void log(Map<String, String[]> valueMap) {
        if (LOG.isDebugEnabled()) {
            List<String> keyList =  new LinkedList<String>(valueMap.keySet());
          
            Collections.sort(keyList);
            for (String key : keyList) {
                LOG.debug(key + ":" + Arrays.toString(valueMap.get(key)));
            }
        }
    }
    
}
