package cn.davidma.tinymobfarm.core;

import javafx.beans.value.ObservableBooleanValue;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.nio.file.attribute.UserDefinedFileAttributeView;

@Config(modid = Reference.MOD_ID, name = Reference.MOD_ID)
public class ConfigTinyMobFarm {

	@Config.RequiresMcRestart
	@Name("Lasso Durability")
	public static int LASSO_DURABILITY = 256;

	@Config.RequiresMcRestart
	@Name("Mob Farm Rate")
	public static double[] MOB_FARM_SPEED = {50.0, 40.0, 30.0, 20.0, 10.0, 5.0, 2.5, 0.5};

	@Config.RequiresMcRestart
	@Comment("Blacklist of mobs that cannot be captured (i.e. minecraft:cow for cow).")
	@Name("Mob Blacklist")
	public static String[] MOB_BLACKLIST = {};

	@Config.RequiresMcRestart
	@Comment("Check is there is a chest or if the chest has space, keeps items from falling to the ground, Server friendly option.")
	@Name("Chest & Space checker")
	public static boolean CHEST_SPACE_CHECK = true;
}
