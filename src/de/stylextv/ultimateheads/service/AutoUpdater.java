package de.stylextv.ultimateheads.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.Inflater;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import de.stylextv.ultimateheads.lang.LanguageManager;
import de.stylextv.ultimateheads.main.Main;
import de.stylextv.ultimateheads.main.Variables;
import de.stylextv.ultimateheads.version.VersionUtil;

public class AutoUpdater {
	
	private static String CHAT_LINE;
	static {
		if(VersionUtil.getMcVersion()<VersionUtil.MC_1_13) {
			CHAT_LINE = "§8§m----------------------------------------";
		} else {
			CHAT_LINE = "§8§m⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯";
		}
	}
	
	private Main plugin;
	
	private String updateRequest;
	private boolean inUpdateCheck;
	
	public AutoUpdater(Main plugin) {
		this.plugin = plugin;
	}
	
	public void startAutoUpdater() {
		if(updateRequest!=null) try {
			URL url = new URL("https://github.com/StylexTV/UltimateHeads/raw/main/version/"+updateRequest+".dat");
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		    try {
		        byte[] chunk = new byte[4096];
		        int bytesRead;
		        InputStream stream = url.openStream();
		        
		        while ((bytesRead = stream.read(chunk)) > 0) {
		            outputStream.write(chunk, 0, bytesRead);
		        }
		        
		    } catch (IOException ex) {
		        ex.printStackTrace();
		    }
		    byte[] bytes = outputStream.toByteArray();
		    
		    Inflater inflater = new Inflater();
			inflater.setInput(bytes);
			FileOutputStream fos = new FileOutputStream(plugin.getPluginFile());
			byte[] buffer = new byte[bytes.length*2];
			while(!inflater.finished()) {
				int count = inflater.inflate(buffer);
				fos.write(buffer, 0, count);
			}
			fos.close();
			inflater.end();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter("plugins/UltimateHeads/au-result"));
			writer.write(Variables.VERSION);
		    writer.close();
		} catch(Exception ex) {ex.printStackTrace();}
	}
	public void checkAutoUpdater() {
		File f=new File("plugins/UltimateHeads/au-result");
		if(f.exists()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					String past=null;
					try {
						BufferedReader reader = new BufferedReader(new FileReader(f));
						past=reader.readLine();
						reader.close();
					} catch (IOException ex) {ex.printStackTrace();}
					f.delete();
					
					final String pastF=past;
					new BukkitRunnable() {
						@Override
						public void run() {
							Bukkit.getConsoleSender().sendMessage(Variables.PREFIX_CONSOLE+"A new update has been §ainstalled§r. Version: "+pastF+" -> "+Variables.VERSION);
							Bukkit.getConsoleSender().sendMessage(Variables.PREFIX_CONSOLE+"The changelog can be found here:");
							Bukkit.getConsoleSender().sendMessage(Variables.PREFIX_CONSOLE+"https://www.spigotmc.org/resources/85797/updates");
						}
					}.runTask(plugin);
				}
			}.runTaskLaterAsynchronously(plugin, 5);
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					int currentVersion=(int) (Double.valueOf(Variables.VERSION)*10);
					int future=1;
					String found=null;
					while(future<100) {
						int i=currentVersion+future;
						try {
							String fileUrl=i/10+"."+i%10;
							URL url = new URL("https://github.com/StylexTV/UltimateHeads/raw/main/version/"+fileUrl+".dat");
							ReadableByteChannel rbc = Channels.newChannel(url.openStream());
							rbc.close();
							found=fileUrl;
						} catch(Exception ex) {
							break;
						}
					    
					    future++;
					}
					
					if(found!=null) {
						final String foundF=found;
						new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.getConsoleSender().sendMessage(Variables.PREFIX_CONSOLE+"A new §aupdate§r has been found. Version: "+foundF);
								Bukkit.getConsoleSender().sendMessage(Variables.PREFIX_CONSOLE+"Enter §e/uh update§r into your ingame chat to install the update.");
							}
						}.runTask(plugin);
					}
				} catch(Exception ex) {
					new BukkitRunnable() {
						@Override
						public void run() {
							Bukkit.getConsoleSender().sendMessage(Variables.PREFIX_CONSOLE+"An §cexception§r occurred while checking for new updates:");
							ex.printStackTrace();
						}
					}.runTask(plugin);
				}
			}
		}.runTaskLaterAsynchronously(plugin, 5);
	}
	
	public void runAutoUpdater(Player p) {
		if(updateRequest!=null) {
			p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.error.alreadyfound1", updateRequest));
			p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.error.alreadyfound2"));
		} else if(inUpdateCheck) {
			p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.error.alreadysearching"));
		} else {
			inUpdateCheck=true;
			p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.begin"));
			BukkitTask runnable=new BukkitRunnable() {
				@Override
				public void run() {
					p.sendMessage(Variables.PREFIX+"§7...");
				}
			}.runTaskTimerAsynchronously(plugin, 120, 120);
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						int currentVersion=(int) (Double.valueOf(Variables.VERSION)*10);
						int future=1;
						String found=null;
						boolean noConnection=false;
						while(future<100) {
							int i=currentVersion+future;
							try {
								String fileUrl=i/10+"."+i%10;
								URL url = new URL("https://github.com/StylexTV/UltimateHeads/raw/main/version/"+fileUrl+".dat");
								ReadableByteChannel rbc = Channels.newChannel(url.openStream());
								rbc.close();
								found=fileUrl;
							} catch(Exception ex) {
								if(!(ex instanceof FileNotFoundException)) {
									noConnection=true;
								}
								break;
							}
						    
						    future++;
						}
						
						if(found!=null) {
							updateRequest=found;
							p.sendMessage(
									Variables.PREFIX+CHAT_LINE+"§r\n"+
									Variables.PREFIX+LanguageManager.parseMsg("autoupdate.result.found1", found)+"§r\n"+
									Variables.PREFIX+LanguageManager.parseMsg("autoupdate.result.found2")+"§r\n"+
									Variables.PREFIX+LanguageManager.parseMsg("autoupdate.result.found3")+"§r\n"+
									Variables.PREFIX+CHAT_LINE
							);
						} else if(noConnection) {
							p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.error.noconnection"));
						} else {
							p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.result.noupdate", Variables.NAME));
						}
					} catch(Exception ex) {
						p.sendMessage(Variables.PREFIX+LanguageManager.parseMsg("autoupdate.error.unknown"));
						ex.printStackTrace();
					}
					inUpdateCheck=false;
					runnable.cancel();
				}
			}.runTaskAsynchronously(plugin);
		}
	}
	
}
