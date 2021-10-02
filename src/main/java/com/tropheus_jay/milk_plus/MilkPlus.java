package com.tropheus_jay.milk_plus;

import com.tropheus_jay.milk_plus.fluid.MilkFluid;
import com.tropheus_jay.milk_plus.milk_holders.MilkBowlItem;
import com.tropheus_jay.milk_plus.milk_holders.MilkCauldron;
import com.tropheus_jay.milk_plus.milk_holders.potion.MilkAreaEffectCloudEntity;
import com.tropheus_jay.milk_plus.milk_holders.potion.arrow.MilkTippedArrowItem;
import com.tropheus_jay.milk_plus.milk_holders.potion.bottle.LingeringMilkBottle;
import com.tropheus_jay.milk_plus.milk_holders.potion.bottle.MilkBottle;
import com.tropheus_jay.milk_plus.milk_holders.potion.bottle.SplashMilkBottle;
import com.tropheus_jay.milk_plus.mixin.BrewingRecipeRegistryAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.*;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.item.Items.*;

public class MilkPlus implements ModInitializer {
	public static final String ID = "milk_plus";
	public static FlowableFluid STILL_MILK;
	public static FlowableFluid FLOWING_MILK;
	public static Block MILK;
	public static Block MILK_CAULDRON;
	public static Item MILK_BOTTLE;
	public static Item SPLASH_MILK_BOTTLE;
	public static Item LINGERING_MILK_BOTTLE;
	public static Item MILK_ARROW;
	public static Item MILK_BOWL;
	public static EntityType<MilkAreaEffectCloudEntity> MILK_EFFECT_CLOUD_ENTITY_TYPE;
	
	@Override
	public void onInitialize() {
		STILL_MILK = Registry.register(Registry.FLUID, id("milk"), new MilkFluid.Still());
		FLOWING_MILK = Registry.register(Registry.FLUID, id("flowing_milk"), new MilkFluid.Flowing());
		MILK = Registry.register(Registry.BLOCK, id("milk_block"),
				new FluidBlock(STILL_MILK, FabricBlockSettings.copy(Blocks.WATER).mapColor(MapColor.WHITE)){});
		MILK_BOTTLE = Registry.register(Registry.ITEM, id("milk_bottle"),
				new MilkBottle(new Item.Settings().recipeRemainder(GLASS_BOTTLE).maxCount(1).group(ItemGroup.BREWING)));
		SPLASH_MILK_BOTTLE = Registry.register(Registry.ITEM, id("splash_milk_bottle"),
				new SplashMilkBottle((new FabricItemSettings()).maxCount(1).group(ItemGroup.BREWING)));
		LINGERING_MILK_BOTTLE = Registry.register(Registry.ITEM, id("lingering_milk_bottle"),
				new LingeringMilkBottle((new Item.Settings()).maxCount(1).group(ItemGroup.BREWING)));
		MILK_ARROW = Registry.register(Registry.ITEM, id("milk_arrow"),
				new MilkTippedArrowItem((new FabricItemSettings()).group(ItemGroup.COMBAT)));
		MILK_BOWL = Registry.register(Registry.ITEM, id("milk_bowl"),
				new MilkBowlItem((new FabricItemSettings()).maxCount(1).group(ItemGroup.FOOD).food(FoodComponents.MUSHROOM_STEW)));
		
		BrewingRecipeRegistryAccessor.invokeRegisterPotionType(MILK_BOTTLE);
		BrewingRecipeRegistryAccessor.invokeRegisterPotionType(SPLASH_MILK_BOTTLE);
		BrewingRecipeRegistryAccessor.invokeRegisterPotionType(LINGERING_MILK_BOTTLE);
		BrewingRecipeRegistryAccessor.invokeRegisterItemRecipe(MILK_BOTTLE, GUNPOWDER, SPLASH_MILK_BOTTLE);
		BrewingRecipeRegistryAccessor.invokeRegisterItemRecipe(SPLASH_MILK_BOTTLE, DRAGON_BREATH, LINGERING_MILK_BOTTLE);
		
		MILK_EFFECT_CLOUD_ENTITY_TYPE = Registry.register(Registry.ENTITY_TYPE, id("milk_area_effect_cloud"),
				FabricEntityTypeBuilder.<MilkAreaEffectCloudEntity>create()
						.fireImmune()
						.dimensions(EntityDimensions.fixed(6.0F, 0.5F))
						.trackRangeChunks(10)
						.trackedUpdateRate(Integer.MAX_VALUE)
						.build());
		
		CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.put(MILK_BUCKET, MilkCauldron.FILL_FROM_BUCKET);
		CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.put(MILK_BOTTLE, MilkCauldron.FILL_FROM_BOTTLE);
		
		MILK_CAULDRON = Registry.register(Registry.BLOCK, id("milk_cauldron"),
				new MilkCauldron(FabricBlockSettings.copy(Blocks.CAULDRON)));
		
		initFluidApi();
	}
	
	@SuppressWarnings({"UnstableApiUsage", "deprecation"})
	public void initFluidApi() {
		// filled
		FluidStorage.combinedItemApiProvider(MILK_BUCKET).register(context ->
				new FullItemFluidStorage(context, bucket -> ItemVariant.of(BUCKET), FluidVariant.of(STILL_MILK), FluidConstants.BUCKET)
		);
		FluidStorage.combinedItemApiProvider(MILK_BOWL).register(context ->
				new FullItemFluidStorage(context, bowl -> ItemVariant.of(BOWL), FluidVariant.of(STILL_MILK), FluidConstants.BOTTLE)
		);
		FluidStorage.combinedItemApiProvider(MILK_BOTTLE).register(context ->
				new FullItemFluidStorage(context, bottle -> ItemVariant.of(GLASS_BOTTLE), FluidVariant.of(STILL_MILK), FluidConstants.BOTTLE)
		);
		// empty
		FluidStorage.combinedItemApiProvider(BUCKET).register(context ->
				new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(MILK_BUCKET), STILL_MILK, FluidConstants.BUCKET)
		);
		FluidStorage.combinedItemApiProvider(BOWL).register(context ->
				new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(MILK_BOWL), STILL_MILK, FluidConstants.BOTTLE)
		);
		FluidStorage.combinedItemApiProvider(GLASS_BOTTLE).register(context ->
				new EmptyItemFluidStorage(context, bottle -> ItemVariant.of(MILK_BOTTLE), STILL_MILK, FluidConstants.BOTTLE)
		);
		// cauldron
		CauldronFluidContent.registerCauldron(MILK_CAULDRON, STILL_MILK, FluidConstants.BOTTLE, LeveledCauldronBlock.LEVEL);
	}
	
	public static Identifier id(String path) {
		return new Identifier(ID, path);
	}
}
