package org.digiplex.bukkitplugin.blink;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Logger;

import org.digiplex.bukkitplugin.common.permissions.Authority;
import org.digiplex.bukkitplugin.common.permissions.BukkitAuthority;
import org.digiplex.bukkitplugin.common.permissions.EmptyAuthority;
import org.digiplex.bukkitplugin.common.permissions.OpAuthority;
import org.digiplex.bukkitplugin.common.permissions.Permissions3Authority;
import org.digiplex.bukkitplugin.common.permissions.PermissionsExAuthority;

public class BlinkConfig {
	public static final String PERMOPT_NONE = "(?i)none";
	public static final String PERMOPT_OPS = "(?i)op|ops";
	public static final String PERMOPT_BUKKIT = "(?i)bukkit";
	public static final String PERMOPT_PERMISSIONS3 = "(?i)permissions3|permissions|perm3|perm";
	public static final String PERMOPT_PERMISSIONSEX = "(?i)permissionsex|permex|pex";
	
	private static final Logger LOG = Logger.getLogger("Minecraft");
	
	private File configFile;
	private Properties configProps;
	private BlinkPlugin myplugin;
	
	public BlinkConfig(BlinkPlugin blinkPlugin) throws IOException, ConfigException{
		myplugin = blinkPlugin;
		configProps = getDefaultConfig();
		
		myplugin.getDataFolder().mkdir(); //creates directory if not present
		configFile = new File(myplugin.getDataFolder().toString()+"/config.properties");
		
		//check existence and create anew if not present
		if (configFile.createNewFile()){ //return true if created
			FileOutputStream fis = new FileOutputStream(configFile);
			configProps.store(fis, "Config file for the Blink plugin.");
			fis.flush(); fis.close();
			
		} else {
			FileInputStream fis = new FileInputStream(configFile);
			configProps.load(fis);
			fis.close();
		}
		if (!parseConfigProperties()){
			throw new ConfigException();
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	public void printConfig() {
		LOG.info("configProps ==> "+configProps.toString());
		StringBuffer sb = new StringBuffer();
		sb
		.append("transBlocks=").append(transBlocks.toString())
		.append(", landBlocks=").append(landBlocks.toString())
		.append(", avoidBlocks=").append(avoidAboveBlocks.toString())
		.append(", blinkItem=").append(blinkItem)
		.append(", localRange=").append(localrange)
		.append(", thruLimit=").append(thruLimit)
		.append(", maxDistance=").append(maxDistance)
		.append(", consumeBuffer=").append(consumeBuffer)
		.append(", consumeItem=").append(consumeItem)
		.append(", consumeItemAmount=").append(consumeItemAmount)
		.append(", useItem=").append(globalUseItem)
		.append(", immedeateSetsDefault=").append(immedeateSetsDefault)
		.append(", usePermision=").append(usePermission.toString());
		
		LOG.info("loaded Props => "+sb.toString());
	}
	////////////////////////////////////////////////////////////////////////////////////
	
	private int configVersion;
	
	/**
	 * a comma-separated list of block type ids which are considered "transparent" when picking 
	 * what block is being looked at (air (0) is assumed). It is highly encouraged that water 
	 * (8,9) stay in this list.
	 */
	private HashSet<Byte> transBlocks = new HashSet<Byte>();
	public HashSet<Byte> getTransBlocks() {return transBlocks;}
	
	/**
	 * a comma-separated list of block type ids which can be landed inside (air (0) is assumed).
	 */
	private HashSet<Byte> landBlocks = new HashSet<Byte>();
	public HashSet<Byte> getLandBlocks() {return landBlocks;}
	
	/**
	 * a comma-separated list of block type ids which are not suitable to land upon. Air is not 
	 * assumed in this case, but it is highly encouraged that it not be removed from the list.
	 */
	private HashSet<Byte> avoidAboveBlocks = new HashSet<Byte>();
	public HashSet<Byte> getAvoidAboveBlocks() {return avoidAboveBlocks;}
	
	/**
	 * a comma-separated list of block type ids which blink will not blink to when they are within
	 * interaction range.
	 */
	private HashSet<Byte> interactBlocks = new HashSet<Byte>();
	public HashSet<Byte> getInteractBlocks() {return interactBlocks;}
	
	/**
	 * an item id that identifies the item used for right-click blinking. Defaults to the feather.
	 */
	private int blinkItem;
	public int getBlinkItem() {return blinkItem;}
	
	/**
	 * the maximum radius from the target block the plugin looks for a suitable landing location 
	 * when in Local mode. Defaults to 3.
	 */
	private int localrange;
	public int getLocalRange() {return localrange;}
	
	/**
	 * the maximum number of blocks into the surface the plugin looks for a suitable landing location 
	 * when in Thru mode. Defaults to 35.
	 */
	private int thruLimit;
	public int getThruLimit() {return thruLimit;}
	
	/**
	 * the maximum "sight-line" distance the plugin uses when determining what block the player is 
	 * aiming at. Defaults to 60.
	 */
	private int maxDistance;
	public int getMaxDistance() {return maxDistance;}
	
	/**
	 * if the player blinks within this radius, it will not consume the blink item (when 
	 * consumeItem = true). This is to prevent accidental "double blinks" or blinks to the 
	 * same location from consuming items.   
	 */
	private int consumeBuffer;
	public int getConsumeBuffer() {return consumeBuffer;}
	
	/**
	 * whether blinking consumes the blink item. Defaults to false.
	 */
	private boolean consumeItem;
	public boolean doesConsumeItem() {return consumeItem;}
	
	/**
	 * the number of the blink item to consume.
	 */
	private int consumeItemAmount;
	public int getConsumeItemAmount() {return consumeItemAmount;}
	
	/**
	 * the default setting for /blink useitem. Defaults to true.
	 */
	private boolean globalUseItem;
	public boolean doesUseItemGlobalSetting() {return globalUseItem;}
	
	/**
	 * the default setting for /blink usesetsmode. Defaults to true.
	 */
	private boolean immedeateSetsDefault;
	public boolean immedeateSetsDefault() {return immedeateSetsDefault;}
	
	/**
	 * which permissions system to use. Currently supported are 'none', 'op', 'bukkit' (untested), or 'permissions3'. Defaults to 'none'.
	 */
	private Class<? extends Authority> usePermission;
	public Class<? extends Authority> getAuthorityToUse() {return usePermission;}
	
	private Properties getDefaultConfig() {
		Properties p = new Properties();
		p.put("version", "1"); //actually "2", but 1 because of possibility of not being there
		p.put("blinkItem", "288");
		p.put("useItem", "true");
		p.put("localRange", "3");
		p.put("thruLimit", "35");
		p.put("landableBlocks", "6,8,9,30,31,32,37,38,39,40,50,55,63,65,68,70,72,75,76,77,78,83,93,94,96");
		p.put("passthroughBlocks", "6,8,9,10,11,30,31,32,37,38,39,40,50,51,55,59,65,68,70,72,75,76,77,83,90,93,94");
		p.put("dontLandAbove", "0,6,10,11,26,27,28,30,31,32,37,38,39,40,50,51,55,59,63,64,66,68,69,70,71,72,75,76,77,83,90");
		p.put("interactBlocks", "23,25,26,54,58,61,62,84");
		p.put("maxDistance", "60");
		p.put("consumeBuffer", "2");
		p.put("consumeItem", "false");
		p.put("immedeateSetsDefault", "true");
		p.put("usePermission", "none");
		return p;
	}
	private boolean parseConfigProperties(){
		try {
			String verstr = configProps.getProperty("version");
			if (verstr == null) configVersion = 1;
			else configVersion = Integer.parseInt(verstr);
			
			switch(configVersion){
			case 1:
				LOG.info("[Blink] Detected old config version. Updating.");
				parseConfigVersion1(); 
				upgradeConfig(1); //if it is an old config, copy this to the old entry
				break; 
			case 2:
				//LOG.info("[Blink] Detected old config version. Updating.");
				parseConfigVersion2(); 
				//upgradeConfig(2); 
				break;
			}
			
		} catch (NumberFormatException ex){
			LOG.severe("[Blink] Error reading config file: NumberFormatException. Please fix or delete the config file and reload.");
			return false;
		} catch (NullPointerException ex){
			LOG.severe("[Blink] Error reading config file: NullPointerException. This should never happen...");
			ex.printStackTrace();
			return false;
		} catch (ConfigException e) {
			LOG.severe("[Blink] Error parsing config file: "+e.getMessage());
			LOG.severe("[Blink] Please fix or delete the config file and reload.");
			return false;
		}
		return true;
	}
	
	private void upgradeConfig(int configver){
		switch (configver){
		case 1: //updates from config version 1 include
			configProps.put("landableBlocks", "6,8,9,30,31,32,37,38,39,40,50,55,63,65,68,70,72,75,76,77,78,83,93,94,96");
			Byte[] newland = new Byte[]{65};
			landBlocks.addAll(Arrays.asList(newland));
			
			configProps.put("passthroughBlocks", "6,8,9,10,11,30,31,32,37,38,39,40,50,51,55,59,65,68,70,72,75,76,77,83,90,93,94");
			Byte[] newpass = new Byte[]{65};
			transBlocks.addAll(Arrays.asList(newpass));
			
			configProps.put("dontLandAbove", "0,6,10,11,26,27,28,30,31,32,37,38,39,40,50,51,55,59,63,64,66,68,69,70,71,72,75,76,77,83,90");
			Byte[] newavoid = new Byte[]{6, 26, 27, 28, 30, 31, 32, 37, 38, 39, 40, 50, 55, 59, 63, 64, 66, 68, 69, 70, 71, 72, 75, 76, 77, 83, 90};
			avoidAboveBlocks.addAll(Arrays.asList(newavoid));
			
			configProps.put("interactBlocks", "23,25,26,54,58,61,62,84");
			Byte[] newinteract = new Byte[]{23, 25, 26, 54, 58, 61, 62, 84};
			interactBlocks.addAll(Arrays.asList(newinteract));
			
			configProps.put("usePermission", "none");
			usePermission = EmptyAuthority.class;
			
			configProps.put("consumeBuffer", "2");
			consumeBuffer = 2;
			
			configProps.put("version", "2");
			configVersion = 2;
			LOG.info("[Blink] Updated through config version 1.");
			//fall through
		}
		
		try {
			FileOutputStream fis = new FileOutputStream(configFile);
			configProps.store(fis, "Config file for the Blink plugin.");
			fis.flush(); fis.close();
		} catch(IOException ex){
			LOG.info("[Blink] Unable to save updated config file:"+ex.getLocalizedMessage());
		}
		LOG.info("[Blink] Update complete.");
		//upgrade complete
	}
	
	///////////////////////////// Previous Config Versions //////////////////////
	private void parseConfigVersion2() throws ConfigException{
		blinkItem = Integer.parseInt(configProps.getProperty("blinkItem"));
		localrange = Integer.parseInt(configProps.getProperty("localRange"));
		thruLimit = Integer.parseInt(configProps.getProperty("thruLimit"));
		maxDistance = Integer.parseInt(configProps.getProperty("maxDistance"));
		consumeBuffer = Integer.parseInt(configProps.getProperty("consumeBuffer"));
		consumeItemAmount = parseBooleanOrInt(configProps.getProperty("consumeItem"));
		consumeItem = (consumeItemAmount > 0);
		globalUseItem = Boolean.parseBoolean(configProps.getProperty("useItem"));
		immedeateSetsDefault = Boolean.parseBoolean(configProps.getProperty("immedeateSetsDefault"));
		
		String[] blocklist = configProps.getProperty("landableBlocks").split(",");
		landBlocks.add((byte)0);
		for (int i = 0; i < blocklist.length; i++){
			landBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
		
		blocklist = configProps.getProperty("passthroughBlocks").split(",");
		transBlocks.add((byte)0);
		for (int i = 0; i < blocklist.length; i++){
			transBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
		
		blocklist = configProps.getProperty("dontLandAbove").split(",");
		//avoidAboveBlocks.add((byte)0);
		for (int i = 0; i < blocklist.length; i++){
			avoidAboveBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
		
		blocklist = configProps.getProperty("interactBlocks").split(",");
		for (int i = 0; i < blocklist.length; i++){
			interactBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
		
		if (configProps.getProperty("usePermission").matches(PERMOPT_NONE)){
			usePermission = EmptyAuthority.class;
		} else if (configProps.getProperty("usePermission").matches(PERMOPT_OPS)){
			usePermission = OpAuthority.class;
		} else if (configProps.getProperty("usePermission").matches(PERMOPT_BUKKIT)){
			usePermission = BukkitAuthority.class;
		} else if (configProps.getProperty("usePermission").matches(PERMOPT_PERMISSIONS3)){
			usePermission = Permissions3Authority.class;
		} else if (configProps.getProperty("usePermission").matches(PERMOPT_PERMISSIONSEX)){
			usePermission = PermissionsExAuthority.class;
		} else {
			throw new ConfigException("Unknown Permission System:"+configProps.getProperty("usePermission")+
					". Valid options are 'none', 'ops', 'bukkit', 'permissions3', or 'pex'.");
		}
	}
	
	private void parseConfigVersion1() throws ConfigException{
		blinkItem = Integer.parseInt(configProps.getProperty("blinkItem"));
		localrange = Integer.parseInt(configProps.getProperty("localRange"));
		thruLimit = Integer.parseInt(configProps.getProperty("thruLimit"));
		maxDistance = Integer.parseInt(configProps.getProperty("maxDistance"));
		//consumeItem = Boolean.parseBoolean(configProps.getProperty("consumeItem"));
		consumeItemAmount = parseBooleanOrInt(configProps.getProperty("consumeItem"));
		consumeItem = (consumeItemAmount > 0);
		globalUseItem = Boolean.parseBoolean(configProps.getProperty("useItem"));
		immedeateSetsDefault = Boolean.parseBoolean(configProps.getProperty("immedeateSetsDefault"));
		
		String[] blocklist = configProps.getProperty("landableBlocks").split(",");
		landBlocks.add((byte)0);
		for (int i = 0; i < blocklist.length; i++){
			landBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
		
		blocklist = configProps.getProperty("passthroughBlocks").split(",");
		transBlocks.add((byte)0);
		for (int i = 0; i < blocklist.length; i++){
			transBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
		
		blocklist = configProps.getProperty("dontLandAbove").split(",");
		//avoidAboveBlocks.add((byte)0);
		for (int i = 0; i < blocklist.length; i++){
			avoidAboveBlocks.add(Byte.valueOf(blocklist[i].trim()));
		}
	}
	
	//////////////////////////////////////////////////////////////
	
	public static class ConfigException extends Exception {
		private static final long serialVersionUID = -6138416020846558084L;
		public ConfigException() {super();}
		public ConfigException(String message, Throwable cause) {super(message, cause);}
		public ConfigException(String message) {super(message);}
		public ConfigException(Throwable cause) {super(cause);}
	}
	
	private static int parseBooleanOrInt(String str){
		if (str.matches("(?i)false")) return 0;
		if (str.matches("(?i)true")) return 1;
		int i = Integer.parseInt(str);
		if (i < 0) return 0;
		return i;
	}
}
