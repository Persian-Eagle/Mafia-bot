package town.mafia.persons;

import java.util.List;

import town.DiscordGame;
import town.events.MurderTownEvent;
import town.mafia.phases.Night;
import town.persons.Person;
import town.roles.GameRole;

// A Serial Killer can kill a person each night.
public class SerialKiller extends Person
{
	public SerialKiller(DiscordGame game, int refNum, long id)
	{
		super(game, refNum, id, GameRole.SERIAL_KILLER);
	}

	@Override
	public String ability(List<Person> references)
	{
		if (!isAlive())
			return "Can't kill if you're dead.";
		if (references.isEmpty())
			return "There's no person to kill. `!ability 1` to kill the first person shown in `!party`.";
		if (references.size() > 1)
			return "Cannot kill more than one person at once. `!ability 1` to kill the first person show in `!party`.";
		if (!(getGame().getCurrentPhase() instanceof Night))
			return "Serial Killers can only kill during the night.";

		// You can't kill yourself
		if (references.get(0) == this)
			return "You can't kill yourself.";

		if (!references.get(0).isAlive())
			return "You can't kill a dead guy.";

		String msg = "";

		if (event != null)
		{
			msg += "You've changed your mind.\n";
			cancel();
		}

		event = new MurderTownEvent(getGame(), this, references.get(0));
		getGame().addEvent(event);

		return msg + String.format("You will kill <@%d> tonight.", references.get(0).getID());
	}

	@Override
	public List<Person> getPossibleTargets()
	{
		List<Person> targets = getGame().getAlivePlayers();
		targets.remove(this);
		return targets;
	}

	@Override
	public String getHelp()
	{
		return "SERIAL KILLER (SK)\n" +
				"Serial Killer wins with other serial killers. His goal is to kill anyone who isn't an SK.\n" +
				"Ability: Can kill one person every night. Ex: `!ability 2` kills person number two. Check a person's number with !party";
	}
}