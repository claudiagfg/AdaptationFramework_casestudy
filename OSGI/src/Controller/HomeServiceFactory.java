package Controller;

import org.osgi.framework.*;

//import helloservice.HelloService;
//import helloservice.HelloServiceImpl;

public class HomeServiceFactory implements ServiceFactory {
	private int usageCounter = 0;
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        System.out.println("Create object of HelloService for " + bundle.getSymbolicName());
        usageCounter++;
        System.out.println("Number of bundles using service " + usageCounter);
        //HelloService helloService = new HelloServiceImpl();
        //return helloService;
        return "hi";
    }
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        System.out.println("Release object of HelloService for " + bundle.getSymbolicName());
        usageCounter--;
        System.out.println("Number of bundles using service " + usageCounter);
    }
}
