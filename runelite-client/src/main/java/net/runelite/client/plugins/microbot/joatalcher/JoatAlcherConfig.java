package net.runelite.client.plugins.microbot.joatalcher;

import lombok.Getter;
import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.questhelper.panel.questorders.QuestOrders;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestDetails;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestHelper;
import net.runelite.client.util.Text;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ConfigGroup("joatalcher")
public interface JoatAlcherConfig extends Config
{

	@ConfigItem(
		keyName = "alchableItems",
		name = "Alchable Items",
		description = "Items to alch csv eg Rune 2h sword,Rune pickaxe"
	)
	default String autoStartQuests()
	{
		return "Rune 2h sword";
	}


}
