
/*
 * 
 */

package de.sernet.service.vna_service;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.2.9
 * Wed Sep 04 20:46:19 CEST 2013
 * Generated source version: 2.2.9
 * 
 */


@WebServiceClient(name = "vna-service", 
                  wsdlLocation = "file:src/sernet/verinice/service/vna/vna-service.wsdl",
                  targetNamespace = "http://www.sernet.de/service/vna-service") 
public class VnaService_Service extends Service {

    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://www.sernet.de/service/vna-service", "vna-service");
    public final static QName VnaService = new QName("http://www.sernet.de/service/vna-service", "vna-service");
    static {
        URL url = null;
        try {
            url = new URL("file:src/sernet/verinice/service/vna/vna-service.wsdl");
        } catch (MalformedURLException e) {
            System.err.println("Can not initialize the default wsdl from file:src/sernet/verinice/service/vna/vna-service.wsdl");
            // e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public VnaService_Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public VnaService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public VnaService_Service() {
        super(WSDL_LOCATION, SERVICE);
    }
    

    /**
     * 
     * @return
     *     returns VnaService
     */
    @WebEndpoint(name = "vna-service")
    public VnaService getVnaService() {
        return super.getPort(VnaService, VnaService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns VnaService
     */
    @WebEndpoint(name = "vna-service")
    public VnaService getVnaService(WebServiceFeature... features) {
        return super.getPort(VnaService, VnaService.class, features);
    }

}