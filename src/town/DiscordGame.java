package town;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.RoleManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction.RoleData;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import town.events.TownEvent;
import town.persons.DiscordGamePerson;
import town.persons.Person;
import town.phases.Phase;
import town.phases.PhaseManager;
import town.roles.Faction;
import town.roles.Role;


//// This represents an ongoing deception game. It's instantiated with pg.startParty
public class DiscordGame
{
	private final JDA jda;
	private final DiscordGameConfig config;

	private long gameGuildId;

	// Important channels (Name : id)
	private HashMap<String, Long> channels = new HashMap<>();
	private HashMap<String, Long> roles = new HashMap<>();
	private HashSet<Faction> wonTownRoles = new HashSet<Faction>();
	private PriorityQueue<TownEvent> events = new PriorityQueue<>();
	private PhaseManager phaseManager = new PhaseManager();

	private LinkedList<DiscordGamePerson> savedForMorning = new LinkedList<>();
	private ArrayList<DiscordGamePerson> players = new ArrayList<>();

	private int dayNum = 1;

	boolean initiated = false;
	boolean ended = false;

	private DiscordGame(JDA jda, DiscordGameConfig config)
	{
		this.jda = jda;
		this.config = config;
	}

	public JDA getJDA()
	{
		return jda;
	}

	public String getPrefix()
	{
		return "!";
	}

	public DiscordGameConfig getConfig()
	{
		return config;
	}

	public ArrayList<DiscordGamePerson> getPlayersCache()
	{
		return players;
	}

	public static DiscordGame createServer(GameParty party, long identifier)
	{
		DiscordGame game = new DiscordGame(party.getJDA(), party.getConfig());
		game.createServer(identifier);
		return game;
	}

	public void createServer(long identifier)
	{
		getConfig().getGameMode().build(this, getConfig().isRandom());
		GuildAction ga = getJDA().createGuild(config.getGameMode().getName());
		createNewChannels(ga);
		ga.newRole().setName("" + identifier);
		ga.queue();
	}

	public void createNewChannels(GuildAction g)
	{
		// this channel used for general game updates
		for (Role role : getConfig().getGameMode().getTownRoles())
			g.newRole().setName(role.getName()).setPermissionsRaw(0l);

		g.newRole().setName("Bot").addPermissions(Permission.ADMINISTRATOR).setColor(Color.YELLOW);

		g.newRole().setName("Player").setColor(Color.CYAN)
				.setPermissionsRaw(QP.readPermissions() | QP.writePermissions() | QP.speakPermissions());

		RoleData deadPlayerRoleData = g.newRole().setName("Dead").setColor(Color.GRAY)
				.setPermissionsRaw(QP.readPermissions());

		RoleData defendantRoleData = g.newRole().setName("Defendant").setColor(Color.GREEN)
				.setPermissionsRaw(QP.speakPermissions() | QP.writePermissions() | QP.readPermissions());

		// FIXME: WARNING: Unable to load JDK7 types (java.nio.file.Path): no Java7 type support added
		// Is being caued by the addPermissionOverride method.

		// players discussing during the day
		g.newChannel(ChannelType.TEXT, "daytime_discussion")
		.setPosition(0)
		.addPermissionOverride(g.getPublicRole(), QP.readPermissions(), QP.writePermissions())
		.addPermissionOverride(defendantRoleData, QP.readPermissions() | QP.writePermissions(), 0);

		for (Person p : getPlayersCache())
		{
			g.newChannel(ChannelType.TEXT, "private")
			.setPosition(1)
			.addPermissionOverride(g.getPublicRole(), 0, QP.readPermissions() | QP.writePermissions())
			.setTopic(p.getRealName()); // This will be used as an identifier
		}

		//for dead players
		g.newChannel(ChannelType.TEXT, "the_afterlife")
		.setPosition(2)
		.addPermissionOverride(g.getPublicRole(), 0, QP.readPermissions() | QP.writePermissions())
		.addPermissionOverride(deadPlayerRoleData, QP.readPermissions() | QP.writePermissions(), 0);
	}


//	public void processMessage(Message message)
//	{
//		processMessage(getPrefix(), message);
//	}
//
//	public void processMessage(String prefix, Message message)
//	{
//		boolean fromGuild = isMessageFromGameGuild(message);
//
//		if (!executeCommand(this, new GlobalCommands(), prefix, message))
//			if (!fromGuild)
//				executeCommand(this, new PartyCommands(), prefix, message);
//			else if (initiated)
//				executeCommand(this, config.getGameMode().getCommands(), prefix, message);
//	}
//
//	private boolean isMessageFromGameGuild(Message message)
//	{
//		if (getGameGuild() != null)
//			return message.getGuild().getIdLong() == getGameGuild().getIdLong();
//		else
//			return false;
//	}
//

//	public void getNewGuild(Guild guild)
//	{
//		guild.getChannels().get(0).createInvite().queue((invite) -> persons.forEach((person) -> sendDMTo(person, invite.getUrl()).queue()));
//		gameGuildId = guild.getIdLong();
//		guild.getChannels(true).forEach((channel) -> assignChannel(channel));
//		getGameGuild().addRoleToMember(jda.getSelfUser().getIdLong(), guild.getRolesByName("Bot", false).get(0)).queue();
//
//		List<Role> guildRoles = getGameGuild().getRoles();
//		for (int x = 0; x < config.getGameMode().getTownRoles().size(); ++x)
//		{
//			Role townRole = guildRoles.get(guildRoles.size() - x - 2);
//			roles.put(townRole.getName().toLowerCase(), townRole.getIdLong());
//		}
//
//		roles.put("player", guild.getRolesByName("Player", false).get(0).getIdLong());
//		roles.put("dead", guild.getRolesByName("Dead", false).get(0).getIdLong());
//		roles.put("defendant", guild.getRolesByName("Defendant", false).get(0).getIdLong());
//
//		for (Person p : getPlayers())
//		{
//			p.sendMessage("Your role is " + p.getType().getName());
//			p.sendMessage(p.getHelp());
//		}
//
//		guild.getPublicRole().getManager().reset().queue();
//		sendMessageToTextChannel("daytime_discussion", "Waiting for players...").queue();
//	}

	public Member getMemberFromGame(Person person)
	{
		return getMemberFromGame(person.getID());
	}

	public Member getMemberFromGame(Long memberID)
	{
		return getGuild().getMemberById(memberID);
	}

//	public void assignChannel(GuildChannel channel)
//	{
//		if (!channel.getName().contentEquals("private")) channels.put(channel.getName(), channel.getIdLong());
//		else
//			for (Person p : getPlayers())
//			{
//				TextChannel textChannel = (TextChannel)channel;
//				String topic = textChannel.getTopic();
//				if (topic != null && topic.contains(p.getRealName()))
//				{
//					p.assignPrivateChannel(textChannel.getIdLong());
//					return;
//				}
//			}
//	}
//
//	public void endGame()
//	{
//		ended = true;
//		phaseManager.end();
//		phaseManager.start(this, new End(this, phaseManager));
//	}
//
//	public boolean hasEnded()
//	{
//		return ended;
//	}
//
//	public boolean hasInitiated()
//	{
//		return initiated;
//	}

	public void transferOrDelete()
	{
		if (!transfer()) deleteServer();
	}

	public void deleteServer()
	{
		phaseManager.end();
		getGuild().delete().queue();
	}

	public boolean transfer()
	{
		phaseManager.end();
		for (DiscordGamePerson p : getPlayersCache())
		{
			Member member = getMemberFromGame(p);
			if (member == null) continue;
			getGuild().transferOwnership(member).reason("The game has ended").queue();
			return true;
		}
		return false;
	}

	public DiscordGamePerson getPerson(Member member)
	{
		return getPerson(member.getIdLong());
	}

//	public Person getPerson(int refNum)
//	{
//		for (Person person : persons)
//			if (person.getNum() == refNum)
//				return person;
//		return null;
//	}

	public DiscordGamePerson getPerson(long id)
	{
		for (DiscordGamePerson person : getPlayersCache())
			if (person.getID() == id)
				return person;
		return null;
	}

	public ArrayList<DiscordGamePerson> getAlivePlayers()
	{
		ArrayList<DiscordGamePerson> alive = new ArrayList<>();
		getPlayersCache().stream().filter(p -> p.isAlive()).forEach(p -> alive.add(p));
		return alive;
	}

//	public List<Person> getDeadPlayers()
//	{
//		ArrayList<Person> dead = new ArrayList<>();
//		persons.stream().filter(p -> !p.isAlive()).forEach(p -> dead.add(p));
//		return dead;
//	}
//
//	public GuildChannel getGuildChannel(String channelName)
//	{
//		Long channelID = channels.get(channelName);
//		return getGuildChannel(channelID);
//	}
//
//	public GuildChannel getGuildChannel(Long channelID)
//	{
//		if (channelID != null)
//			return getGameGuild().getGuildChannelById(channelID);
//		return null;
//	}
//
	public TextChannel getTextChannel(String channelName)
	{
		Long channelID = channels.get(channelName);
		return getTextChannel(channelID);
	}

	public TextChannel getTextChannel(Long channelID)
	{
		if (channelID != null)
			return getGuild().getTextChannelById(channelID);
		return null;
	}

	public VoiceChannel getVoiceChannel(String channelName)
	{
		return getGuild().getVoiceChannelsByName(channelName, false).get(0);
	}

	public void addEvent(TownEvent event)
	{
		events.add(event);
	}

	public void removeEvent(TownEvent event)
	{
		events.remove(event);
	}

	public void dispatchEvents()
	{
		if (events.size() == 0) return;
		TownEvent event = events.remove();
		for (DiscordGamePerson person : getPlayersCache())
			person.onEvent(event);

		event.postDispatch();
		dispatchEvents();
	}

	public Phase getCurrentPhase()
	{
		return phaseManager.getCurrentPhase();
	}

//	public User getUser(Person person)
//	{
//		return jda.getUserById(person.getID());
//	}
//
	public long getGuildId()
	{
		return gameGuildId;
	}

	public Guild getGuild()
	{
		return jda.getGuildById(getGuildId());
	}

	public net.dv8tion.jda.api.entities.Role getRole(String roleName)
	{
		return getGuild().getRoleById(getRoleID(roleName));
	}

	private long getRoleID(String roleName)
	{
		if (!initiated) throw new IllegalStateException("Game has not started, can't get roles");
		Long id = roles.get(roleName.toLowerCase());
		if (id == null)
			throw new IllegalArgumentException("Role " + roleName + " not found");
		return id;
	}

	private RestAction<Message> sendDMTo(Person person, String msg)
	{
		return jda.getUserById(person.getID()).openPrivateChannel()
		.flatMap((channel) -> channel.sendMessage(msg));
	}

	public MessageAction sendMessageToTextChannel(String channelName, String msg)
	{
		return getTextChannel(channelName).sendMessage(msg);
	}

	public MessageAction sendMessageToTextChannel(Long channelID, String msg)
	{
		return getTextChannel(channelID).sendMessage(msg);
	}

	public MessageAction sendMessageToTextChannel(String channelName, MessageEmbed embed)
	{
		return getTextChannel(channelName).sendMessage(embed);
	}

	public MessageAction sendMessageToTextChannel(Long channelID, MessageEmbed embed)
	{
		return getTextChannel(channelID).sendMessage(embed);
	}

	public RestAction<Message> getMessage(String channelName, long messageID)
	{
		return getTextChannel(channelName).retrieveMessageById(messageID);
	}

//	public void startGame()
//	{
//		config.getGameMode().start(this, phaseManager);
//	}
//
//	// Where the game ACTUALLY starts
//	public void gameGuildJoin(Member member)
//	{
//		// Check if member was in the lobby
//		boolean shouldKick = true;
//		for (Person p : getPlayers())
//			if (p.getID() == member.getUser().getIdLong())
//			{
//				getGameGuild().addRoleToMember(member, getRole("player")).queue();
//				TextChannel textChannel = getTextChannel(p.getChannelID());
//				textChannel.putPermissionOverride(getMemberFromGame(p)).setAllow(QP.readPermissions() | QP.writePermissions()).queue();
//				shouldKick = false;
//				if (getPlayers().size() == getGameGuild().getMemberCount() - 1) // -1 since Bot counts as a member
//					startGame();
//				break;
//			}
//
//		if (shouldKick)
//			member.kick("He was not part of the lobby").queue();
//	}

	public RestAction<?> toggleVC(String channelName, boolean show)
	{
		if (!show)
			return getVoiceChannel(channelName).delete();
		else
			return getGuild().createVoiceChannel("Daytime");
	}

	public PermissionOverrideAction setChannelVisibility(String roleName, String channelName, boolean read, boolean write)
	{
		return setChannelVisibility(getRole(roleName), channelName, read, write);
	}

	public void openPrivateChannels()
	{
		getPlayersCache().forEach(p -> setChannelVisibility(getGuild().getPublicRole(), p.getPrivateChannel(), true, false).queue());
	}

	public PermissionOverrideAction setChannelVisibility(DiscordGamePerson p, String channelName, boolean read, boolean write)
	{
		if (p.isDisconnected()) return null;
		Member member = getMemberFromGame(p);
		if (member == null) throw new IllegalArgumentException("Invalid person.");
		return setChannelVisibility(member, channelName, read, write);
	}

	private PermissionOverrideAction setChannelVisibility(IPermissionHolder holder, String channelName, boolean read, boolean write)
	{
		return setChannelVisibility(holder, getTextChannel(channelName), read, write);
	}

	private PermissionOverrideAction setChannelVisibility(IPermissionHolder holder, long channelId, boolean read, boolean write)
	{
		return setChannelVisibility(holder, getTextChannel(channelId), read, write);
	}

	private PermissionOverrideAction setChannelVisibility(IPermissionHolder holder, TextChannel channel, boolean read, boolean write)
	{
		if (channel == null) throw new IllegalArgumentException("Channel name doesn't exist");
		PermissionOverrideAction action = null;
		if (channel.getType().equals(ChannelType.TEXT))
		{
			if (read && !write)
				action = channel.putPermissionOverride(holder).reset().setPermissions(QP.readPermissions(), QP.writePermissions());
			else if (read && write)
				action = channel.putPermissionOverride(holder).reset().setAllow(QP.readPermissions() | QP.writePermissions());
			else
				action = channel.putPermissionOverride(holder).reset().setDeny(QP.readPermissions() | QP.writePermissions());
		}

		return action;
	}

	public RoleManager muteAllInRole(String roleName, boolean shouldMute)
	{
		net.dv8tion.jda.api.entities.Role role = getRole(roleName);
		List<Member> members = getGuild().getMembersWithRoles(role);
		for (Member member : members)
			getPerson(member).mute(shouldMute);

		if (shouldMute)
			return role.getManager().revokePermissions(Permission.VOICE_SPEAK);
		return role.getManager().setPermissions(Permission.VOICE_SPEAK);
	}

	public RoleManager muteAllInRoleExcept(String roleName, boolean shouldMute, DiscordGamePerson p)
	{
		net.dv8tion.jda.api.entities.Role role = getRole(roleName);
		List<Member> members = getGuild().getMembersWithRoles(role);
		for (Member member : members)
		{
			DiscordGamePerson person = getPerson(member);
			if (person != p)
				person.mute(shouldMute);
		}
		if (shouldMute)
			return role.getManager().revokePermissions(Permission.VOICE_SPEAK);
		return role.getManager().setPermissions(Permission.VOICE_SPEAK);
	}

//	public void gameGuildVoiceJoin(Member m, VoiceChannel channel)
//	{
//		if (!initiated) return;
//		if (channel.getGuild().getIdLong() != getGameID()) return;
//
//		Person person = getPerson(m);
//		m.mute(person.isMuted()).queue();
//	}
//
//	public void winTownFaction(Faction faction)
//	{
//		wonTownRoles.add(faction);
//	}
//
	public boolean hasTownFactionWon(Faction faction)
	{
		return wonTownRoles.contains(faction);
	}

	public void saveForMorning(DiscordGamePerson p)
	{
		savedForMorning.add(p);
	}

	public DiscordGamePerson getDeathForMorning()
	{
		if (savedForMorning.isEmpty())
			return null;
		return savedForMorning.pop();
	}

	public DiscordGamePerson peekDeathForMorning()
	{
		return savedForMorning.peek();
	}

//	public void memberLeftGameGuild(Member member)
//	{
//		Person person = getPerson(member);
//		if (person != null)
//			person.disconnect();
//	}

	public int getDayNum()
	{
		return dayNum;
	}

	public void startNextDay()
	{
		dayNum++;
	}

	public RestAction<Void> modifyMemberRoles(Person person, String... roleNames)
	{
		net.dv8tion.jda.api.entities.Role[] roles = new net.dv8tion.jda.api.entities.Role[roleNames.length];
		for (int x = 0; x < roleNames.length; ++x)
		{
			net.dv8tion.jda.api.entities.Role role = getRole(roleNames[x]);
			if (role == null) throw new IllegalArgumentException("Role name does not exist");
			roles[x] = role;
		}
		return getGuild().modifyMemberRoles(getMemberFromGame(person), roles);
	}

	// Quick Permissions
	private static class QP
	{
		public static long readPermissions()
		{
			return Permission.getRaw(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.VIEW_CHANNEL);
		}

		public static long writePermissions()
		{
			return Permission.getRaw(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION);
		}

		public static long speakPermissions()
		{
			return Permission.getRaw(Permission.VOICE_SPEAK, Permission.PRIORITY_SPEAKER);
		}
	}
}
