package net.mcshockwave.scatter;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map.Entry;

public class SimpleScatter extends JavaPlugin {

	public static Scoreboard	score		= null;

	public boolean				useDelay	= true;
	public long					delayTicks	= 1;

	public void onEnable() {
		score = Bukkit.getScoreboardManager().getMainScoreboard();

		saveDefaultConfig();

		useDelay = getConfig().getBoolean("time-delay-enabled");
		delayTicks = getConfig().getLong("time-delay-ticks");
	}

	public static void spreadPlayers(World world, int spreadDistance, final boolean delay, final boolean instant,
			final boolean teams) {
		spreadPlayers(world, spreadDistance, delay, instant, -2, false, teams);
	}

	public static void spreadPlayers(World world, int spreadDistance, final boolean delay, final boolean instant,
			final long time, final boolean resuming, final boolean teams) {
		SchedulerUtils util = SchedulerUtils.getNew();
		Bukkit.broadcastMessage("§cGetting scatter locations...");
		final Location[] locs = ScatterManager.getScatterLocations(world, spreadDistance, getScatterAmount(teams));
		util.add(delay ? 10 : 0);
		util.add("§aLoading chunks... (this may take a while)");
		for (final Location l : locs) {
			util.add(new Runnable() {
				public void run() {
					l.getChunk().load(true);
				}
			});
			util.add(delay ? 5 : 0);
		}
		util.add(delay ? 9 : 0);
		util.add("§eLoading locations...");
		if (score.getTeams().size() > 0) {
			util.add(new Runnable() {
				public void run() {
					int index = 0;
					for (Team t : score.getTeams()) {
						if (index >= locs.length) {
							continue;
						}

						final Location l = locs[index];
						for (OfflinePlayer p : t.getPlayers()) {
							ScatterManager.scatterLocs.put(p.getName(), l);
						}
						index++;
					}
					for (Player p : Bukkit.getOnlinePlayers()) {
						if (score.getTeam(p.getName()) == null) {
							if (index >= locs.length) {
								continue;
							}

							ScatterManager.scatterLocs.put(p.getName(), locs[index]);
							index++;
						}
					}
				}
			});
		} else {
			util.add(new Runnable() {
				public void run() {
					int index = 0;
					for (Player p : Bukkit.getOnlinePlayers()) {
						if (index >= locs.length) {
							continue;
						}

						ScatterManager.scatterLocs.put(p.getName(), locs[index]);
						index++;
					}
				}
			});
		}
		util.add(delay ? 10 : 0);
		util.add("§dStarting spread!");
		util.add(new Runnable() {
			public void run() {
				SchedulerUtils sc = SchedulerUtils.getNew();

				for (final Entry<String, Location> ent : ScatterManager.scatterLocs.entrySet()) {
					sc.add(new Runnable() {
						public void run() {
							if (Bukkit.getPlayer(ent.getKey()) != null) {
								Player p = Bukkit.getPlayer(ent.getKey());
								p.teleport(ent.getValue());
								Bukkit.broadcastMessage("§aScattering: §6"
										+ p.getName()
										+ " §8[§7"
										+ (score.getTeam(p.getName()) != null ? score.getTeam(p.getName()).getName()
												: "Solo") + "§8]");
								// for (Player pl : Bukkit.getOnlinePlayers()) {
								// pl.playSound(pl.getLocation(),
								// Sound.NOTE_PLING, 10, 2);
								// }
							}
						}
					});
					sc.add(instant ? 0 : 2);
				}

				sc.add("§e§lDone scattering!");

				sc.execute();
			}
		});

		util.execute();
	}

	public static int getScatterAmount(boolean teams) {
		int ret = 0;
		if (teams) {
			ret += score.getTeams().size();
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (teams) {
				if (score.getTeam(p.getName()) != null) {
					continue;
				}
				ret++;
			} else {
				ret++;
			}
		}

		return ret;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender.isOp()) {
			if (args.length == 0) {
				return false;
			}

			World w;
			int rad;
			boolean teams = false;
			try {
				rad = Integer.parseInt(args[0]);
			} catch (Exception e) {
				sender.sendMessage("§cRadius must be an integer!");
				return true;
			}

			if (args.length == 1) {
				if (sender instanceof Player) {
					w = ((Player) sender).getWorld();
				} else {
					sender.sendMessage("You must specify a world!");
					return true;
				}
			} else {
				w = Bukkit.getWorld(args[1]);
				if (w == null) {
					sender.sendMessage("§cThat is not a valid world!");
					return true;
				}
			}

			if (args.length >= 2) {
				teams = Boolean.valueOf(args[2]);
			}

			spreadPlayers(w, rad, useDelay, false, delayTicks, false, teams);
		}
		return false;
	}

}
