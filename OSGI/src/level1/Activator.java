package level1;

import level1.stoveService;
import java.util.Hashtable;
import org.osgi.framework.*;

public class Activator {

	public void start(BundleContext context) {
		Hashtable<String,String> props = new Hashtable<String,String>();
		props.put("Stove", "A");
		context.registerService(stoveService.class.getName(), new stoveServiceImpl(), props);
	}
	public void stop(BundleContext context) {
		
	}
	
	private static class stoveServiceImpl implements stoveService{
		public void SwitchOn() {
			System.out.println("switching on");
		}
		public void SwitchOff() {
			System.out.println("switching off");
		}
	}

}
