package com.slayeritemreminders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

final class TaskCatalog
{
	private static final Map<String, TaskDefinition> TASKS;

	static
	{
		Map<String, TaskDefinition> tasks = new HashMap<>();
		wiki(tasks, "Aberrant spectres", "Aberrant spectre");
		wiki(tasks, "Abyssal demons", "Abyssal demon");
		wiki(tasks, "The Abyssal Sire", "Abyssal Sire");
		wiki(tasks, "The Alchemical Hydra", "Alchemical Hydra");
		wiki(tasks, "Ankou", "Ankou");
		wiki(tasks, "Aquanites", "Aquanite");
		wiki(tasks, "Araxxor", "Araxxor");
		wiki(tasks, "Araxytes", "Araxyte");
		wiki(tasks, "Aviansies", "Aviansie");
		wiki(tasks, "Banshees", "Banshee");
		wiki(tasks, "Barrows Brothers", "Barrows brothers");
		wiki(tasks, "Basilisks", "Basilisk");
		wiki(tasks, "Black demons", "Black demon");
		wiki(tasks, "Black dragons", "Black dragon");
		wiki(tasks, "Bloodveld", "Bloodveld");
		wiki(tasks, "Blue dragons", "Blue dragon");
		wiki(tasks, "Brine rats", "Brine rat");
		wiki(tasks, "Callisto", "Callisto");
		wiki(tasks, "Catablepon", "Catablepon");
		wiki(tasks, "Cave bugs", "Cave bug");
		wiki(tasks, "Cave crawlers", "Cave crawler");
		wiki(tasks, "Cave horrors", "Cave horror");
		wiki(tasks, "Cave kraken", "Cave kraken");
		wiki(tasks, "Cave slimes", "Cave slime");
		wiki(tasks, "Cerberus", "Cerberus");
		wiki(tasks, "Chaos druids", "Chaos druid");
		wiki(tasks, "The Chaos Elemental", "Chaos Elemental");
		wiki(tasks, "The Chaos Fanatic", "Chaos Fanatic");
		wiki(tasks, "Cockatrice", "Cockatrice");
		wiki(tasks, "Crawling hands", "Crawling Hand");
		wiki(tasks, "Crazy Archaeologists", "Crazy archaeologist");
		wiki(tasks, "Dagannoth", "Dagannoth");
		wiki(tasks, "Dagannoth Kings", "Dagannoth Kings");
		wiki(tasks, "Dark beasts", "Dark beast");
		wiki(tasks, "Deranged Archaeologist", "Deranged archaeologist");
		wiki(tasks, "Drakes", "Drake");
		wiki(tasks, "Duke Sucellus", "Duke Sucellus");
		wiki(tasks, "Dust devils", "Dust devil");
		wiki(tasks, "Earth warriors", "Earth warrior");
		wiki(tasks, "Fever spiders", "Fever spider");
		wiki(tasks, "Fire giants", "Fire giant");
		wiki(tasks, "Fleshcrawlers", "Flesh Crawler");
		wiki(tasks, "Frost dragons", "Frost dragon");
		wiki(tasks, "General Graardor", "General Graardor");
		wiki(tasks, "The Giant Mole", "Giant Mole");
		wiki(tasks, "Greater demons", "Greater demon");
		wiki(tasks, "Green dragons", "Green dragon");
		wiki(tasks, "The Grotesque Guardians", "Grotesque Guardians");
		wiki(tasks, "Gryphons", "Gryphon");
		wiki(tasks, "Harpie bug swarms", "Harpie Bug Swarm");
		wiki(tasks, "Hellhounds", "Hellhound");
		wiki(tasks, "Hydras", "Hydra");
		wiki(tasks, "Icefiends", "Icefiend");
		wiki(tasks, "Ice giants", "Ice giant");
		wiki(tasks, "Ice warriors", "Ice warrior");
		wiki(tasks, "Infernal mages", "Infernal Mage");
		wiki(tasks, "TzTok-Jad", "TzTok-Jad");
		wiki(tasks, "Jellies", "Jelly");
		wiki(tasks, "Jungle horrors", "Jungle horror");
		wiki(tasks, "The Kalphite Queen", "Kalphite Queen");
		wiki(tasks, "Killerwatts", "Killerwatt");
		wiki(tasks, "The King Black Dragon", "King Black Dragon");
		wiki(tasks, "The Cave Kraken Boss", "Kraken");
		wiki(tasks, "Kree'arra", "Kree'arra");
		wiki(tasks, "K'ril Tsutsaroth", "K'ril Tsutsaroth");
		wiki(tasks, "Kurask", "Kurask");
		wiki(tasks, "Lava Dragons", "Lava dragon");
		wiki(tasks, "Lesser demons", "Lesser demon");
		wiki(tasks, "Lesser Nagua", "Lesser Nagua");
		wiki(tasks, "Lizardmen", "Lizardman shaman");
		wiki(tasks, "The Maggot King", "Maggot King");
		wiki(tasks, "Magic axes", "Magic axe");
		wiki(tasks, "Mammoths", "Mammoth");
		wiki(tasks, "Minotaurs", "Minotaur");
		wiki(tasks, "Mogres", "Mogre");
		wiki(tasks, "Molanisks", "Molanisk");
		wiki(tasks, "Mutated zygomites", "Mutated zygomite");
		wiki(tasks, "Nechryael", "Nechryael");
		wiki(tasks, "Otherworldly beings", "Otherworldly being");
		wiki(tasks, "The Phantom Muspah", "Phantom Muspah");
		wiki(tasks, "Pyrefiends", "Pyrefiend");
		wiki(tasks, "Red dragons", "Red dragon");
		wiki(tasks, "Revenants", "Revenant");
		wiki(tasks, "Rockslugs", "Rockslug");
		wiki(tasks, "Sarachnis", "Sarachnis");
		wiki(tasks, "Scorpia", "Scorpia");
		wiki(tasks, "Sea snakes", "Sea snake");
		wiki(tasks, "Shadow warriors", "Shadow warrior");
		wiki(tasks, "The Shellbane Gryphon", "Shellbane Gryphon");
		wiki(tasks, "Skeletal wyverns", "Skeletal Wyvern");
		wiki(tasks, "Smoke devils", "Smoke devil");
		wiki(tasks, "Sourhogs", "Sourhog");
		wiki(tasks, "Suqahs", "Suqah");
		wiki(tasks, "Terror dogs", "Terror dog");
		wiki(tasks, "The Leviathan", "The Leviathan");
		wiki(tasks, "The Whisperer", "The Whisperer");
		wiki(tasks, "The Thermonuclear Smoke Devil", "Thermonuclear smoke devil");
		wiki(tasks, "Turoth", "Turoth");
		wiki(tasks, "Vardorvis", "Vardorvis");
		wiki(tasks, "Venenatis", "Venenatis");
		wiki(tasks, "Vet'ion", "Vet'ion");
		wiki(tasks, "Vorkath", "Vorkath");
		wiki(tasks, "Wall beasts", "Wall beast");
		wiki(tasks, "Waterfiends", "Waterfiend");
		wiki(tasks, "Wyrms", "Wyrm");
		wiki(tasks, "Commander Zilyana", "Commander Zilyana");
		wiki(tasks, "TzKal-Zuk", "TzKal-Zuk");
		wiki(tasks, "Zulrah", "Zulrah");

		tasks.put(key("Gargoyles"), new TaskDefinition("Gargoyle", Collections.singletonList(
			new ReminderItem("Rock hammer or rock thrownhammer",
				ItemID.SLAYER_ROCK_HAMMER, ItemID.SLAYER_ROCK_THROWNHAMMER))));
		tasks.put(key("Lizards"), new TaskDefinition("Desert lizard", Collections.singletonList(
			new ReminderItem("Ice cooler", ItemID.SLAYER_ICY_WATER))));
		TASKS = Collections.unmodifiableMap(tasks);
	}

	private TaskCatalog()
	{
	}

	static TaskDefinition get(String taskName)
	{
		return TASKS.get(key(taskName));
	}

	private static void wiki(Map<String, TaskDefinition> tasks, String taskName, String wikiPage)
	{
		tasks.put(key(taskName), new TaskDefinition(wikiPage, Collections.emptyList()));
	}

	private static String key(String taskName)
	{
		return taskName.toLowerCase(Locale.ENGLISH);
	}
}
