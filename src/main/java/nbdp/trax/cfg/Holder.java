package nbdp.trax.cfg;

import java.util.Map;

/**
 * A class allowing Spring to inject a map of keys and values.
 * 
 * @author mboyd
 *
 */
public class Holder {

	private Map config = null;

	public Map getConfig() {
		return config;
	}

	public void setConfig(Map config) {
		this.config = config;
	}
	
}
