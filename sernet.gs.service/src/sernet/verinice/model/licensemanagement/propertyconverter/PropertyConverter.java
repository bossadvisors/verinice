/*******************************************************************************
 * Copyright (c) 2016 Sebastian Hagedorn.
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
 *     Sebastian Hagedorn sh[at]sernet.de - initial API and implementation
 ******************************************************************************/
package sernet.verinice.model.licensemanagement.propertyconverter;

import java.util.Date;

import org.apache.commons.beanutils.ConvertUtils;

import sernet.verinice.interfaces.licensemanagement.IPropertyConverter;

/**
 * This class tries to convert Objects to an expected type
 * use-case for this is the cast of properties that are 
 * protected by licensemanagement and stored in the database
 * encrypted as strings (base64). Decryption process is not
 * aware of stored datatype, so the developer needs a tool to
 * cast to the right datatype. This is implemented here.
 * 
 * this class makes usage of the converters defined in:
 * org.apache.commons.beanutils
 * 
 * @author Sebastian Hagedorn sh[at]sernet.de
 *
 */
public class PropertyConverter implements IPropertyConverter {

    @Override
    public String convertToString(Object property) {
        return (String)ConvertUtils.convert(property, String.class);
    }

    @Override
    public Integer convertToInteger(Object property){
        return (Integer) ConvertUtils.lookup(Integer.class).convert(Integer.class, property);
    }

    @Override
    public Date convertToDate(Object property){
        // ensure property is of type long
        if(!(property instanceof Long)){
            property = convertToLong(property);
        }
        return (Date)ConvertUtils.lookup(Date.class).convert(Date.class, property);
    }

    @Override
    public Long convertToLong(Object property) {
        return (Long) ConvertUtils.lookup(Long.class).convert(Long.class, property);
    }

}
