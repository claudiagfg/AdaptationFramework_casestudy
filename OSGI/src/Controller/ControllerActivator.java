package Controller;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleActivator;
//import org.apache.felix.framework.ext.FelixBundleContext;
import org.osgi.framework.wiring.*;
import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkEvent;
//import org.apache.felix.framework.Felix;
import org.osgi.framework.dto.*;
import org.osgi.framework.launch.*;


public class ControllerActivator implements BundleActivator { 
	private BundleContext my_context;
	public long newBndId = 0;
	public BundleContext newBndCtxt = null;
	private Timer t = new Timer();  
	public void start(BundleContext context) throws Exception {
		
		my_context = context;
		
		TimerTask tt = new TimerTask() {  
		    @Override  
		    public void run() {  
		        /* This is a periodic task that's run every 1 second with a delay of 0.5 seconds
		         * First, it analyzes the bundles that exist in the framework and it makes a list of their
		         * bundle objects and ids.
		         */
		        ArrayList<Long> bndids = new ArrayList<Long>(); 
				ArrayList<Bundle> bnds = new ArrayList<Bundle>();
				for (final Bundle bundle : my_context.getBundles()) {
					Long bndid = bundle.getBundleId();  
					if (!bndids.contains(bndid)) {
						bndids.add(bundle.getBundleId());
						bnds.add(bundle);
					}
				}
				/* Then, the list of bundles is combed through, paying attention to their states. If the
				 * state of one bundle is "INSTALLED", which would mean it has just been installed in the
				 * framework, the framework restarts all bundles, in hopes that the newly installed bundle
				 * resolves some of the already installed bundles' dependencies.
				 * */
				for (Bundle bundle : bnds) {
					if (bundle.getState() == Bundle.INSTALLED) { 
						//System.out.println(bundle.getBundleId() + " is installed");
						for (Bundle b: bnds) {
							if ( (b.getBundleId() != my_context.getBundle().getBundleId()) && (b.getState() != Bundle.RESOLVED)) {
								/* The controller has no way of knowing which bundles have unresolved dependencies depending on the new bundle. it restarts ALL 
								 * bundles in the framework, except for the RESOLVED ones. This means that if one bundle is stopped but isn't uninstalled and 
								 * its dependencies are all resolved, its state will change to RESOLVED (because of the osgi lifecycle), and as soon as the 
								 * controller triggers the start of the bundles (because it detects that some new bundle has been installed), it will skip that 
								 * stopped bundle because it will be able to understand that all its dependencies are resolved so it could not possibly benefit 
								 * from the newly installed bundle.  
								 * 
								 * In addition, the controller must also skip starting itself, it would make no sense.
								 * */
								try {
									b.start();
								}catch(Exception e) {
									//e.printStackTrace();
								}
							}
						}
					break;// It's enough for one bundle to have the "installed" state for the framework to try restart all bundles
					}
				}   
		        
		    };
		};  
		System.out.println("Starting to listen for bundles.");
		t.scheduleAtFixedRate(tt,500,1000);  
		/* This task is run at such a rate that whenever a new bundle is installed, we are sure that in no longer than 1 second,
		 * any bundle that depends on it to get resolved, will be started and will run its corresponding service. If the newly installed 
		 * bundle is the one that has unresolved dependencies and thus can not be started, this controller will continuously try to 
		 * start it, but this will not affect the behavior of the framework or any of the other bundles, and the bundle it will be able to 
		 * start and run automatically once its required bundle is installed.
		 * */
		
    }
	
    public void stop(BundleContext context) throws Exception {
    	t.cancel();
    	System.out.println("Stopped listening for bundles.");
    }

 
}
   
