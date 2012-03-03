package org.digiplex.bukkitplugin.blink;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.TrapDoor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.digiplex.bukkitplugin.blink.BlinkConfig.ConfigException;
import org.digiplex.bukkitplugin.common.permissions.Authority;
import org.digiplex.bukkitplugin.common.permissions.OpAuthority;

public class BlinkPlugin extends JavaPlugin {
	private static final Logger LOG = Logger.getLogger("Minecraft");
	
	public static final String PERM_ALL = "blink.*";
	public static final String PERM_COMMAND_ALL = "blink.core.command.*";
	public static final String PERM_COMMAND_SETTINGS = "blink.core.command.poptions";
	public static final String PERM_COMMAND_BLINK = "blink.core.command.blink";
	public static final String PERM_COMMAND_BACK = "blink.core.command.back";
	public static final String PERM_ITEM = "blink.core.item";
	public static final String PERM_NOCONSUME = "blink.perks.noconsume";
	public static final String PERM_DEBUG = "blink.debug";
	
	//////////////////////////////// Plugin Bookkeeping ////////////////////////////////
	
	//private static String CONFIGDIR = "plugins/Blink";
	protected BlinkConfig config;
	protected Authority permissionAuthority;
	protected final BlinkPlugin myplugin = this;
	private static boolean DEBUGMODE = false;
	
	@Override public void onEnable() {
		PluginManager pm = this.getServer().getPluginManager();
		try {
			config = new BlinkConfig(this);
			
			try {
				permissionAuthority = config.getAuthorityToUse().newInstance();
				permissionAuthority.setupAuthority(this);
			} catch (Authority.AuthorityInitializationException ex){
				LOG.severe("[Blink] Error initializing PermissionAuthority: "+ex.getMessage()+" Defaulting to Op permissions.");
				permissionAuthority = new OpAuthority();
			}
		} catch (IOException ex){
			LOG.severe("[Blink] Error loading config file. Disabling plugin.");
			ex.printStackTrace();
			pm.disablePlugin(this); return;
		} catch (InstantiationException e) {
			LOG.severe("[Blink] Error instantiating PermissionAuthority. Disabling plugin.");
			e.printStackTrace();
			pm.disablePlugin(this); return;
		} catch (IllegalAccessException e) {
			LOG.severe("[Blink] Illegal Access instantiating PermissionAuthority. Disabling plugin.");
			e.printStackTrace();
			pm.disablePlugin(this); return;
		} catch (ConfigException e) {
			//config exceptions have already been handled by the BlinkConfig, just disable now
			pm.disablePlugin(this); return;
		}
		
		{ //create and populate the option list
			Player[] currplayers = this.getServer().getOnlinePlayers();
			for (int i = 0; i < currplayers.length; i++){
				playerblinkmode.put(currplayers[i], new BlinkPrefs());
			}
		}
		PlayerInteractListener pil = new PlayerInteractListener();
		
		pm.registerEvent(Event.Type.PLAYER_INTERACT,pil, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, 	pil, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, 	pil, Event.Priority.Monitor, this);
		
		LOG.info(super.getDescription().getFullName()+" Enabled");
	}

	@Override public void onDisable() {
		playerblinkmode.clear();
		LOG.info("[Blink] Disabled");
	}
	
	private static final String LOCAL_REGEX = "(?i)local|l";
	private static final String COLUMN_REGEX = "(?i)column|col|c";
	private static final String THRU_REGEX = "(?i)thru|through|th|t";
	
	@Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)){
			return consoleCommand(args);
		}
		//returns true if valid command, false otherwise. False will make craftbukkit print the command
		//format specified in plugin.yml
		if (cmd.getName().matches("(?i)blink|b")){
			switch (args.length){
			case 0:
				if (blinkCommandCheck((Player) sender)) return true;
				blink((Player) sender);
				return true;
			case 1:
				if (args[0].matches(LOCAL_REGEX)){
					if (blinkCommandCheck((Player) sender)) return true;
					blink((Player)sender, BlinkMode.Local);
					if (playerblinkmode.get((Player) sender).useSetsDefault)
						setDefaultMode((Player) sender, BlinkMode.Local);
					return true;
				} else if (args[0].matches(COLUMN_REGEX)){
					if (blinkCommandCheck((Player) sender)) return true;
					blink((Player)sender, BlinkMode.Column);
					if (playerblinkmode.get((Player) sender).useSetsDefault)
						setDefaultMode((Player) sender, BlinkMode.Column);
					return true;
				} else if (args[0].matches(THRU_REGEX)){
					if (blinkCommandCheck((Player) sender)) return true;
					blink((Player)sender, BlinkMode.Thru);
					if (playerblinkmode.get((Player) sender).useSetsDefault)
						setDefaultMode((Player) sender, BlinkMode.Thru);
					return true;
				} else if (args[0].matches("(?i)back|b|previous|prev")){
					blinkBack((Player)sender);
					return true;
				} else if (args[0].matches("(?i)help|\\?")){
					sender.sendMessage(this.getDescription().getFullName()+" "+ChatColor.GREEN+"by Tustin2121");
					sender.sendMessage("To use, simply point at any block and type \\blink or \\b, or use ");
					sender.sendMessage("the blink item (feather by default). There are three modes");
					sender.sendMessage("available. Switch between them \nwith \\blink set [mode]");
					sender.sendMessage("   "+((playerblinkmode.get((Player)sender).blinkmode == BlinkMode.Local)?ChatColor.YELLOW:ChatColor.GOLD)+
							"Local "+ChatColor.WHITE+" - Blink to the block you aimed at.");
					sender.sendMessage("   "+((playerblinkmode.get((Player)sender).blinkmode == BlinkMode.Column)?ChatColor.YELLOW:ChatColor.GOLD)+
							"Column"+ChatColor.WHITE+" - Blink to the top of a column of blocks.");
					sender.sendMessage("   "+((playerblinkmode.get((Player)sender).blinkmode == BlinkMode.Thru)?ChatColor.YELLOW:ChatColor.GOLD)+
							"Thru"+ChatColor.WHITE+" - Blink through a line of blocks.");
					sender.sendMessage("You can also use a mode directly with \\blink local (or \\bl),");
					sender.sendMessage("\\blink column (or \\bc) and \\blink thru (or \\bt).");
					return true;
				} else if (args[0].matches("(?i)use|useitem|item")){
					if (blinkSettingsCheck((Player)sender)) return true;
					playerblinkmode.get((Player) sender).useitem = !playerblinkmode.get((Player) sender).useitem;
					if (playerblinkmode.get((Player) sender).useitem){
						sender.sendMessage("[Blink] Use Item Enabled");
					} else {
						sender.sendMessage("[Blink] Use Item Disabled");
					}
					return true;
				} else if (args[0].matches("(?i)usesetsmode|usesetmode|setmode|setsmode|usm|sm|default|defaultswitch|ds|usesetsdefault|usd")){
					if (blinkSettingsCheck((Player)sender)) return true;
					playerblinkmode.get((Player) sender).useSetsDefault = !playerblinkmode.get((Player) sender).useSetsDefault;
					if (playerblinkmode.get((Player) sender).useSetsDefault){
						sender.sendMessage("[Blink] Immediate blink commands "+ChatColor.RED+"do"+ChatColor.WHITE+" set the enabled mode");
					} else {
						sender.sendMessage("[Blink] Immediate blink commands "+ChatColor.RED+"don't"+ChatColor.WHITE+" set the enabled mode");
					}
					return true;
				} else {
					return false;
				}
			case 2:
				if (args[0].equalsIgnoreCase("set")){
					if (args[1].matches(LOCAL_REGEX)){
						setDefaultMode((Player) sender, BlinkMode.Local);
					} else if (args[1].matches(COLUMN_REGEX)){
						setDefaultMode((Player) sender, BlinkMode.Column);
					} else if (args[1].matches(THRU_REGEX)){
						setDefaultMode((Player) sender, BlinkMode.Thru);
					} else {
						return false;
					}
					return true;
				} else if (args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("2121") 
						&& sender.isOp() && permissionAuthority.hasPermission((Player)sender, PERM_DEBUG)) {
					DEBUGMODE = !DEBUGMODE;
					if (DEBUGMODE){
						this.getServer().broadcastMessage("[Blink has entered debug mode, at the behest of "+((Player)sender).getDisplayName()+"]");
					} else {
						this.getServer().broadcastMessage("[Blink has exited debug mode, at the behest of "+((Player)sender).getDisplayName()+"]");
					}
					return true;
				} 
			}
		} else if (cmd.getName().equalsIgnoreCase("bl")){
			if (playerblinkmode.get((Player) sender).useSetsDefault)
				setDefaultMode((Player) sender, BlinkMode.Local);
			if (blinkCommandCheck((Player) sender)) return true;
			blink((Player)sender, BlinkMode.Local);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("bc")){
			if (playerblinkmode.get((Player) sender).useSetsDefault)
				setDefaultMode((Player) sender, BlinkMode.Column);
			if (blinkCommandCheck((Player) sender)) return true;
			blink((Player)sender, BlinkMode.Column);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("bt")){
			if (playerblinkmode.get((Player) sender).useSetsDefault)
				setDefaultMode((Player) sender, BlinkMode.Thru);
			if (blinkCommandCheck((Player) sender)) return true;
			blink((Player)sender, BlinkMode.Thru);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("bb")){
			blinkBack((Player)sender);
			return true;
		}
		return false;
	}
	
	private boolean consoleCommand(String[] args) {
		switch (args.length){
		case 1:
			if (args[0].matches("(?i)config")){
				config.printConfig();
				return true;
			} else if (args[0].equalsIgnoreCase("debug")){
				DEBUGMODE = !DEBUGMODE;
				if (DEBUGMODE){
					this.getServer().broadcastMessage("[Blink has entered debug mode, at the behest of the console]");
				} else {
					this.getServer().broadcastMessage("[Blink has exited debug mode, at the behest of the console]");
				}
				return true;
			} break;
		}
		return false;
	}

	private void setDefaultMode(Player player, BlinkMode bm){
		if (playerblinkmode.get(player).blinkmode == bm) return;
		playerblinkmode.get(player).blinkmode = bm;
		player.sendMessage("[Blink] Default now set to "+bm.toString()+" mode");
	}
	
	//////////////////////////////////////// Plugin Behavior /////////////////////////////////////////
	
	private static enum BlinkMode {
		/** Blink to the local block - checks to see if there's a blink place right above chosen block.
		 * Default Blink Mode */
		Local, 
		/** Go up the selected column to find a blink place */
		Column,
		/** Blink to that block face, instead of on top */
//		SameSide,
		/** Blink to the other side of a wall */
		Thru
	}
	private static final BlinkMode DEFAULTMODE = BlinkMode.Local;
	private class BlinkPrefs {
		public BlinkMode blinkmode = DEFAULTMODE;
		public boolean useitem = myplugin.config.doesUseItemGlobalSetting();
		public boolean useSetsDefault = myplugin.config.immedeateSetsDefault();
		public Location lastBlink = null;
	}
	
	private static Map<Player, BlinkPrefs> playerblinkmode = new HashMap<Player, BlinkPlugin.BlinkPrefs>();
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	
	public class PlayerInteractListener extends PlayerListener {
		@Override public void onPlayerInteract(PlayerInteractEvent event) {
			if (!permissionAuthority.hasPermission(event.getPlayer(), PERM_ITEM)){
				return; //no item blinking means no reaction from blink
			}
			
			if (	(event.getAction() == Action.RIGHT_CLICK_AIR || 
					event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
					playerblinkmode.get(event.getPlayer()).useitem &&
					event.getItem() != null &&
					event.getItem().getTypeId() == config.getBlinkItem())
			{
//				if (!permissionAuthority.hasPermission(event.getPlayer(), Authority.PERM_ITEM)){
//					event.getPlayer().sendMessage("[Blink] You don't have permission to blink by item.");
//					return;
//				}
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK && 
						isInteractBlock(event.getClickedBlock().getTypeId())) 
					return; //allow opening chests, workbench, etc
				
				blink(event.getPlayer());
				event.setCancelled(true);
			}
		}
		
		@Override public void onPlayerJoin(PlayerJoinEvent event) {
			playerblinkmode.put(event.getPlayer(), new BlinkPrefs());
		}
		
		@Override public void onPlayerQuit(PlayerQuitEvent event) {
			playerblinkmode.remove(event.getPlayer());
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	
	private boolean blinkCommandCheck(Player player){
		if (!permissionAuthority.hasPermission(player, PERM_COMMAND_BLINK)){
			player.sendMessage("[Blink] You don't have permission to blink by command.");
			return true;
		}
		return false;
	}
	
	private boolean blinkSettingsCheck(Player player){
		if (!permissionAuthority.hasPermission(player, PERM_COMMAND_SETTINGS)){
			player.sendMessage("[Blink] You don't have permission to set personal settings.");
			return true;
		}
		return false;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	
	public void blinkBack(Player player){
		if (!permissionAuthority.hasPermission(player, PERM_COMMAND_BACK)){
			player.sendMessage("[Blink] You don't have permission to blink back.");
			return;
		}
		Location last = playerblinkmode.get(player).lastBlink;
		if (last == null){
			player.sendMessage("[Blink] Can't blink! No last location!");
		} else {
			movePlayer(player, last);
		}
	}
	
	public void blink(Player player){
		blink(player, playerblinkmode.get(player).blinkmode);
	}
	
	public void blink(Player player, BlinkMode mode){
		Location starting = player.getLocation();
		int invslot = -1;
		if (config.doesConsumeItem() && !permissionAuthority.hasPermission(player, PERM_NOCONSUME)){
			invslot = player.getInventory().first(config.getBlinkItem());
			if (invslot == -1) {
				player.sendMessage("[Blink] You don't have the proper blink item!");
				return;
			}
			ItemStack blinkItemStack = player.getInventory().getItem(invslot); //have item, now determine if consumed
			if (blinkItemStack.getAmount() < config.getConsumeItemAmount()) {
				player.sendMessage("[Blink] You don't have enough of the blink item!");
				return;
			}
		}
		
		try {
			List<Block> last2 = player.getLastTwoTargetBlocks(config.getTransBlocks(), config.getMaxDistance());
			Block before = null, target;
			BlockFace fromFaceDir;
			
			if (last2.size() < 2){
				target = last2.get(0);
				fromFaceDir = BlockFace.UP; //can't determine from "previous block", because we have none
				if (DEBUGMODE) player.sendMessage(
						" target("+target.getX()+","+target.getY()+","+target.getZ()+")"+
						" before(null)");
			} else {
				before = last2.get(0); target = last2.get(1);
				fromFaceDir = target.getFace(before);
				if (DEBUGMODE) player.sendMessage(
						" target("+target.getX()+","+target.getY()+","+target.getZ()+")"+
						" before("+before.getX()+","+before.getY()+","+before.getZ()+")");
			}
			
			if (isTransparentBlock(target.getTypeId())){
				player.sendMessage("[Blink] Location too far away.");
				return;
			}
			
			switch(mode){
			case Local:{
					//player.sendMessage("Blink Local not yet implemented...");
					BlockFace[] dirsToTest = new BlockFace[6];
					
					if (fromFaceDir == null){
						player.sendMessage("[Blink] Can't Blink! Blinking to moon not implemented yet!");
						return;
					}
					
					//set up the dirs to test in priority based on direction we're coming from
					switch (fromFaceDir){
					case NORTH:
					case NORTH_WEST:
						dirsToTest[1] = BlockFace.NORTH;
						dirsToTest[2] = BlockFace.WEST;
						dirsToTest[3] = BlockFace.EAST;
						dirsToTest[4] = BlockFace.SOUTH;
						break;
					case SOUTH:
					case SOUTH_EAST:
						dirsToTest[1] = BlockFace.SOUTH;
						dirsToTest[2] = BlockFace.EAST;
						dirsToTest[3] = BlockFace.WEST;
						dirsToTest[4] = BlockFace.NORTH;
						break;
					case EAST:
					case NORTH_EAST:
						dirsToTest[1] = BlockFace.EAST;
						dirsToTest[2] = BlockFace.NORTH;
						dirsToTest[3] = BlockFace.SOUTH;
						dirsToTest[4] = BlockFace.WEST;
						break;
					case WEST: 
					case SOUTH_WEST:
						dirsToTest[1] = BlockFace.WEST;
						dirsToTest[2] = BlockFace.SOUTH;
						dirsToTest[3] = BlockFace.NORTH;
						dirsToTest[4] = BlockFace.EAST;
						break;
					default:
						dirsToTest[1] = BlockFace.NORTH;
						dirsToTest[2] = BlockFace.EAST;
						dirsToTest[3] = BlockFace.SOUTH;
						dirsToTest[4] = BlockFace.WEST;
						break;
					}
					dirsToTest[0] = BlockFace.UP; 
					dirsToTest[5] = BlockFace.DOWN;
					
					Block footing = findLocalLandingBlock(target, dirsToTest, config.getLocalRange());
					if (footing != null){
						LOG.fine("[Blink] Found footing=>"+footing+" mat="+footing.getType()+"  below=>"+footing.getRelative(BlockFace.DOWN).getType());
						movePlayer(player, footing);
					} else {
						player.sendMessage("[Blink: Local] Can't blink! No suitable landing location!");
					}
					
				} break;
			case Column:{
					boolean foundSuitable = false, atWorldEnd = false;
					Block footing, headroom, landon;
					if (target.getY() == 0){ //handle the specific case where they try to hit the sky
						if (before != null && before.getY() == 127){
							player.sendMessage("[Blink] Can't blink! Blinking to moon not implemented yet!");
							return;
						}
					}
					
					footing = target.getRelative(BlockFace.UP);
					StringBuilder sb = new StringBuilder("FootingCalc: ");
					
					while (!foundSuitable){
						sb.append(footing.getY()+",");
						if (footing.getY() == 127){
							//if the footing hits the top of the world, start special handling
							atWorldEnd = true;
							sb.append("atWorldEnd");
							break; 
						}
						if ( canLandHere(footing.getTypeId()) ) { //if the footing is good
							headroom = footing.getRelative(BlockFace.UP);
							if (headroom.getY() == 0) break; //at the top of the world
							
							if ( canLandHere(headroom.getTypeId()) ){ //if the headroom is good
								landon = footing.getRelative(BlockFace.DOWN);
								if (landon != null && !isAvoidBlock(landon.getTypeId())){
									foundSuitable = true;
									break;
									
								} else { //if the block under the footing is bad, air, or nothing
									footing = footing.getRelative(BlockFace.UP);
									continue;
								}
							} else {
								footing = footing.getRelative(BlockFace.UP);
								continue;
							}
						} else { //not good footing, iterate
							footing = footing.getRelative(BlockFace.UP);
							continue;
						}
					}
					//when we've reached here, we have a good location to land
					
					if (atWorldEnd){
						landon = footing.getRelative(BlockFace.DOWN);
						sb.append(" - footingid="+landon.getTypeId());
						if (!isAvoidBlock(landon.getTypeId()) && landon.getTypeId() != 0){
							//handle air specially in case someone gets the brilliant idea of removing it
							//in the config file
							movePlayer(player, footing);
						}
					} else if (!foundSuitable){
						player.sendMessage("[Blink: Column] Can't blink! No suitable landing location!");
					} else { 
						movePlayer(player, footing);
					}
					if (DEBUGMODE) player.sendMessage(sb.toString());
					
				} break;
			case Thru:{
					BlockFace otherside;
					switch(fromFaceDir){
					case UP:otherside = BlockFace.DOWN; break;
					case DOWN: otherside = BlockFace.UP; break;
					case NORTH: otherside = BlockFace.SOUTH; break;
					case SOUTH: otherside = BlockFace.NORTH; break;
					case EAST: otherside = BlockFace.WEST; break;
					case WEST: otherside = BlockFace.EAST; break;
					default: 
						player.sendMessage("[Blink: Thru] Error: Couldn't calculate other side of face! Should never happen!");
						return;
					}
					
					BlockFace[] dirs = new BlockFace[]{BlockFace.DOWN};
					
					Block potential = target.getRelative(otherside);
					Block footing = null;
					for (int i = 0; i < config.getThruLimit() && footing == null; i++){
						footing = findLocalLandingBlock(potential, dirs, 3);
						if (otherside == BlockFace.UP && //fix case where the tunnel is 2 high and can't go up
							footing != null && footing.getY() < target.getY()) footing = null;
						potential = potential.getRelative(otherside);
					}
					if (footing != null) {
						movePlayer(player, footing);
					} else {
						player.sendMessage("[Blink: Thru] Can't Blink! No suitable landing place!");
					}
					
				} break;
			}
		} catch (Exception ex){
			LOG.log(Level.SEVERE, "Blink has thrown an "+ex.getClass().getName()+" while calculating a blink!", ex);
			player.sendMessage(ChatColor.RED + "[Blink] An exception was thrown while attempting to blink. " +
					"Please see the server logs and contact "+this.getDescription().getAuthors().get(0)+" with " +
					"the error and where you were pointing when it happened. Press F2 to take a screenshot.");
		}
		
		if (config.doesConsumeItem() && !permissionAuthority.hasPermission(player, PERM_NOCONSUME)){
			ItemStack blinkItemStack = player.getInventory().getItem(invslot); //have item, now determine if consumed
			
			Location ending = player.getLocation();
			if (starting.distanceSquared(ending) > (config.getConsumeBuffer()*config.getConsumeBuffer())){		
				if (blinkItemStack.getAmount() > config.getConsumeItemAmount()){
					blinkItemStack.setAmount(blinkItemStack.getAmount()-config.getConsumeItemAmount());
				} else {
					player.getInventory().setItem(invslot, null);
					player.sendMessage("[Blink] You have just run out of blink fuel!");
				}
			}
		}
		
	}
	
	private void movePlayer(Player player, Location blockloc) { 
		playerblinkmode.get(player).lastBlink = player.getLocation();
		//player.sendMessage("Blink!");
		player.teleport(new Location(
				blockloc.getWorld(), 
				blockloc.getX(),
				blockloc.getY(),
				blockloc.getZ(),
				player.getEyeLocation().getYaw(),
				player.getEyeLocation().getPitch()));
		player.setFallDistance(0);
	}
	
	private void movePlayer(Player player, Block block) {
		playerblinkmode.get(player).lastBlink = player.getLocation();
		//player.sendMessage("Blink!");
		player.teleport(new Location(
				block.getWorld(), 
				block.getX()+0.5,
				getLandingY(block),
				block.getZ()+0.5,
				player.getEyeLocation().getYaw(),
				player.getEyeLocation().getPitch()));
		player.setFallDistance(0);
	}

	private Block findLocalLandingBlock(Block fromblock, BlockFace[] dirs, int depthleft){
		if (isGoodLandingPlace(fromblock)) return fromblock;
		
		depthleft--;
		if (depthleft < 0) return null;
		Block testblock;
		
		for (int i = 0; i < dirs.length; i++){
			testblock = findLocalLandingBlock(fromblock.getRelative(dirs[i]), dirs, depthleft);
			if (testblock != null) return testblock;
		}
		return null;
	}
	
	private boolean isGoodLandingPlace(Block footing){
//			LOG.info("footing=>"+footing.getType());
		if (!canLandHere(footing.getTypeId())) return false;
		Block headroom = footing.getRelative(BlockFace.UP);
//			LOG.info("headroom=>"+headroom.getType());
		if (!canLandHere(headroom.getTypeId()) && headroom.getY() != 0) return false;
		Block landon = footing.getRelative(BlockFace.DOWN);
//			LOG.info("landon=>"+headroom.getType());
		if (isAvoidBlock(landon.getTypeId())) return false;
		
		//special handling here
		switch (landon.getType()){ 
		case TRAP_DOOR:
			if (new TrapDoor(Material.TRAP_DOOR, landon.getData()).isOpen()) return false; break;
		}
//			LOG.info("specialcheck=> passed!");
		return true;
	}
	
	private double getLandingY(Block blockloc){
		//get y based on block under
		Block footing = blockloc.getRelative(BlockFace.DOWN);
		switch (footing.getType()){
		case STEP:
		case BED_BLOCK:
		case CAKE_BLOCK:
			return blockloc.getY()-0.5;
		case FENCE: return blockloc.getY()+0.5;
		}
		
		switch (blockloc.getType()){
		case STEP:
			return blockloc.getY()+0.5;
		case DIODE_BLOCK_OFF:
		case DIODE_BLOCK_ON:
		case TRAP_DOOR:
			return blockloc.getY()+0.2;
		default: 
			return blockloc.getY();
		}
		
	}
	
	private boolean isTransparentBlock(int typeId){
		if (config.getTransBlocks().contains( new Byte((byte)typeId) )){
			return true;
		}
		return false;
	}

	private boolean canLandHere(int typeId) {
		if (config.getLandBlocks().contains( new Byte((byte)typeId) )){
			return true;
		}
		return false;
	}
	
	private boolean isAvoidBlock(int typeId){
		if (config.getAvoidAboveBlocks().contains( new Byte((byte)typeId) )){
			return true;
		}
		return false;
	}
	
	private boolean isInteractBlock(int typeId){
		if (config.getInteractBlocks().contains( new Byte((byte)typeId) )){
			return true;
		}
		return false;
	}
	
}








