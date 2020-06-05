package town.phases;

import town.persons.Person;

//Trial occurs when the Town agrees to put someone under suspicion. They are given this phase, a small window,
//to defend themselves without any outside noise. After this, their fate is judged by the town.
public class Trial extends Phase
{
	Person defendant;
	
	public Trial(PhaseManager pm, Person p)
	{
		super(pm);
		defendant = p;
	}

	//begins the phase. sends out a message, and opens up text channels and voice chat.
	@Override
	public void start()
	{
		getGame().sendMessageToTextChannel("daytime_discussion", defendant.getNickName() + " has been put on trial!");
		//TODO: Lock the chat for all users except the defendant. Mute all participants in VC except the defendant.
	}

	//ends the phase, sending out a global message of this fact.
	@Override
	public void end()
	{
		//		System.out.println("Ending day...");
	}

	//After the defendant has spoken, players briefly discuss what to do and their fate is voted upon
	@Override
	public Phase getNextPhase(PhaseManager pm)
	{
		return new Judgment(pm, defendant);
	}

	//Duration: 20-30 seconds
	@Override
	public int getDurationInSeconds()
	{
		return 15;
	}
}