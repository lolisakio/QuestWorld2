package me.mrCookieSlime.QuestWorld.quests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Variable;
import me.mrCookieSlime.CSCoreLibPlugin.general.Chat.TellRawMessage;
import me.mrCookieSlime.CSCoreLibPlugin.general.Chat.TellRawMessage.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Chat.TellRawMessage.HoverAction;
import me.mrCookieSlime.QuestWorld.QuestWorld;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Party {
	
	UUID leader;
	Set<UUID> members;
	QuestManager manager;
	Set<UUID> pending;

	public Party(UUID uuid) {
		this.leader = uuid;
		this.manager = QuestWorld.getInstance().getManager(Bukkit.getOfflinePlayer(uuid));
		members = new HashSet<UUID>();
		pending = new HashSet<UUID>();
		if (manager.toConfig().contains("party.members")) {
			for (String member: manager.toConfig().getStringList("party.members")) {
				members.add(UUID.fromString(member));
			}
		}
		if (manager.toConfig().contains("party.pending-requests")) {
			for (String request: manager.toConfig().getStringList("party.pending-requests")) {
				pending.add(UUID.fromString(request));
			}
		}
	}
	
	public static Party create(Player p) {
		QuestWorld.getInstance().getManager(p).toConfig().setValue("party.associated", p.getUniqueId().toString());
		return new Party(p.getUniqueId());
	}
	
	public void addPlayer(Player p) {
		for (UUID member: getPlayers()) {
			Player player = Bukkit.getPlayer(member);
			if (player != null) QuestWorld.getInstance().getLocalization().sendTranslation(player, "party.join", true, new Variable("%name%", p.getName()));
		}
		
		this.members.add(p.getUniqueId());
		QuestWorld.getInstance().getLocalization().sendTranslation(p, "party.joined", true, new Variable("%name%", Bukkit.getOfflinePlayer(leader).getName()));
		QuestWorld.getInstance().getManager(p).toConfig().setValue("party.associated", leader.toString());
		if (pending.contains(p.getUniqueId())) pending.remove(p.getUniqueId());
		save();
	}
	
	@SuppressWarnings("deprecation")
	public void removePlayer(String name) {
		for (UUID member: getPlayers()) {
			Player p = Bukkit.getPlayer(member);
			if (p != null) QuestWorld.getInstance().getLocalization().sendTranslation(p, "party.kicked", true, new Variable("%name%", name));
		}
		
		OfflinePlayer player = Bukkit.getOfflinePlayer(name);
		members.remove(player.getUniqueId());
		QuestWorld.getInstance().getManager(player).toConfig().setValue("party.associated", null);
		save();
	}
	
	public void abandon() {
		for (UUID member: members) {
			members.remove(member);
			QuestWorld.getInstance().getManager(Bukkit.getOfflinePlayer(member)).toConfig().setValue("party.associated", null);
		}
		manager.toConfig().setValue("party.associated", null);
		save();
	}

	public List<UUID> getPlayers() {
		List<UUID> players = new ArrayList<UUID>();
		players.add(leader);
		players.addAll(members);
		return players;
	}

	public int getSize() {
		return getPlayers().size();
	}
	
	public void save() {
		List<String> list = new ArrayList<String>();
		for (UUID member: members) {
			list.add(member.toString());
		}
		manager.toConfig().setValue("party.members", list);
		
		List<String> invitations = new ArrayList<String>();
		for (UUID p: pending) {
			invitations.add(p.toString());
		}
		manager.toConfig().setValue("party.pending-requests", invitations);
	}

	public boolean isLeader(OfflinePlayer player) {
		return player.getUniqueId().equals(leader);
	}

	public void invite(Player p) throws Exception {
		p.sendMessage("");
		QuestWorld.getInstance().getLocalization().sendTranslation(p, "party.invitation", false, new Variable("%name%", Bukkit.getOfflinePlayer(leader).getName()));
		
		new TellRawMessage()
		.addText("�a�lACCEPT")
		.addHoverEvent(HoverAction.SHOW_TEXT, "�7Click to accept this Invitation")
		.addClickEvent(ClickAction.RUN_COMMAND, "/quests accept " + leader)
		.addText(" �4�lDENY")
		.addHoverEvent(HoverAction.SHOW_TEXT, "�7Click to deny this Invitation")
		.send(p);
		
		p.sendMessage("");
		
		pending.add(p.getUniqueId());
		save();
	}
	
	public boolean hasInvited(Player p) {
		return pending.contains(p.getUniqueId());
	}

}
