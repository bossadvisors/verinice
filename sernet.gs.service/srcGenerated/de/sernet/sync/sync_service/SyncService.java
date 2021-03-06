package de.sernet.sync.sync_service;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.2.9
 * Mon Aug 16 12:47:04 CEST 2010
 * Generated source version: 2.2.9
 * 
 */
 
@WebService(targetNamespace = "http://www.sernet.de/sync/sync-service", name = "sync-service")
@XmlSeeAlso({de.sernet.sync.data.ObjectFactory.class, de.sernet.sync.mapping.ObjectFactory.class, de.sernet.sync.sync.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface SyncService {

    @WebResult(name = "syncResponse", targetNamespace = "http://www.sernet.de/sync/sync", partName = "response")
    @WebMethod(action = "http://www.sernet.de/sync/sync")
    public de.sernet.sync.sync.SyncResponse sync(
        @WebParam(partName = "request", name = "syncRequest", targetNamespace = "http://www.sernet.de/sync/sync")
        de.sernet.sync.sync.SyncRequest request
    );
}
