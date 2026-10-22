package io.github.sudabake.appliedagristics;

import appeng.api.AECapabilities;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

@Mod(Agristics.ID)
public final class Agristics {
    public static final String ID = "appliedagristics";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,ID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>,net.minecraft.world.item.crafting.RecipeType<io.github.sudabake.appliedagristics.logic.PlantingRecipe>> PLANTING_TYPE=RECIPE_TYPES.register("planting",()->new net.minecraft.world.item.crafting.RecipeType<>() { @Override public String toString() { return ID+":planting"; } });
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,io.github.sudabake.appliedagristics.logic.PlantingRecipe.Serializer> PLANTING_SERIALIZER=RECIPES.register("planting",io.github.sudabake.appliedagristics.logic.PlantingRecipe.Serializer::new);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, io.github.sudabake.appliedagristics.logic.EmptyMachineRecipe.Serializer> EMPTY_MACHINE_RECIPE = RECIPES.register("empty_machine_upgrade", io.github.sudabake.appliedagristics.logic.EmptyMachineRecipe.Serializer::new);
    public static final DeferredRegister<net.minecraft.core.component.DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<net.minecraft.resources.ResourceLocation>> BIO_ID = COMPONENTS.register("bio_identity", () -> net.minecraft.core.component.DataComponentType.<net.minecraft.resources.ResourceLocation>builder().persistent(net.minecraft.resources.ResourceLocation.CODEC).networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC).build());
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<io.github.sudabake.appliedagristics.biology.BioSeedRecipe>> BIO_SEED_RECIPE = RECIPES.register("bio_seed", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(io.github.sudabake.appliedagristics.biology.BioSeedRecipe::new));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<io.github.sudabake.appliedagristics.biology.BioLootRecipe>> BIO_LOOT_RECIPE = RECIPES.register("bio_loot", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(io.github.sudabake.appliedagristics.biology.BioLootRecipe::new));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredBlock<MachineBlock> EARLY = block("early_planter", MachineBlock.Kind.EARLY);
    public static final DeferredBlock<MachineBlock> SEEDS = block("seed_storage", MachineBlock.Kind.SEEDS);
    public static final DeferredBlock<MachineBlock> NETWORK = block("network_planter", MachineBlock.Kind.NETWORK);
    public static final DeferredBlock<MachineBlock> CULTIVATOR = block("cell_cultivator", MachineBlock.Kind.CULTIVATOR);
    public static final DeferredBlock<MachineBlock> NETWORK_ACCESS = block("me_network_access", MachineBlock.Kind.ACCESS);
    public static final DeferredBlock<MachineBlock> ME_INFUSION = block("me_infusion_altar", MachineBlock.Kind.ME_INFUSION);
    public static final DeferredBlock<MachineBlock> INPUT_PORT = block("planter_input_port", MachineBlock.Kind.INPUT_PORT);
    public static final DeferredBlock<MachineBlock> OUTPUT_PORT = block("planter_output_port", MachineBlock.Kind.OUTPUT_PORT);
    public static final DeferredBlock<MachineBlock> ESSENCE_VAULT = block("essence_vault", MachineBlock.Kind.ESSENCE_VAULT);
    public static final DeferredBlock<PlanterPartBlock> PLANTER_FRAME = part("planter_frame", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.FRAME);
    public static final DeferredBlock<PlanterPartBlock> CULTIVATION_CHAMBER = part("cultivation_chamber", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.CHAMBER);
    public static final DeferredBlock<PlanterPartBlock> SPEED_MODULE = part("planter_speed_module", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.SPEED);
    public static final DeferredBlock<PlanterPartBlock> PARALLEL_MODULE = part("planter_parallel_module", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.PARALLEL);
    public static final DeferredBlock<PlanterPartBlock> CONVERSION_MODULE = part("planter_conversion_module", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.CONVERSION);
    public static final DeferredItem<Item> BIO_SEED = ITEMS.register("bio_seed", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final DeferredItem<Item> BIO_ESSENCE = ITEMS.register("bio_essence", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final DeferredItem<Item> SAMPLER = ITEMS.register("bio_sampler", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final DeferredItem<Item> CULTURE = ITEMS.register("mature_culture", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final DeferredItem<Item> EXTRACT = ITEMS.register("bio_extract", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final DeferredItem<Item> CAPSULE = ITEMS.registerSimpleItem("culture_capsule");
    public static final DeferredItem<Item> NUTRIENT = ITEMS.registerSimpleItem("culture_nutrient");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EarlyPlanter>> EARLY_BE = ENTITIES.register("early_planter", () -> BlockEntityType.Builder.of(EarlyPlanter::new, EARLY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeedStorage>> SEED_BE = ENTITIES.register("seed_storage", () -> BlockEntityType.Builder.of(SeedStorage::new, SEEDS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkPlanter>> NETWORK_BE = ENTITIES.register("network_planter", () -> BlockEntityType.Builder.of(NetworkPlanter::new, NETWORK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CellCultivator>> CULTIVATOR_BE = ENTITIES.register("cell_cultivator", () -> BlockEntityType.Builder.of(CellCultivator::new, CULTIVATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkAccess>> ACCESS_BE = ENTITIES.register("me_network_access", () -> BlockEntityType.Builder.of(NetworkAccess::new, NETWORK_ACCESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEInfusionAltar>> INFUSION_BE = ENTITIES.register("me_infusion_altar", () -> BlockEntityType.Builder.of(MEInfusionAltar::new, ME_INFUSION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPort>> INPUT_BE = ENTITIES.register("planter_input_port", () -> BlockEntityType.Builder.of((pos,state) -> new ItemPort(pos,state,true), INPUT_PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPort>> OUTPUT_BE = ENTITIES.register("planter_output_port", () -> BlockEntityType.Builder.of((pos,state) -> new ItemPort(pos,state,false), OUTPUT_PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceVault>> VAULT_BE = ENTITIES.register("essence_vault", () -> BlockEntityType.Builder.of(EssenceVault::new, ESSENCE_VAULT.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MENU = MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));
    static {
        TABS.register("agriculture", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.appliedagristics"))
            .icon(() -> new ItemStack(EARLY.get())).displayItems((params, out) -> ITEMS.getEntries().stream()
                .forEach(i -> out.accept(i.get()))).build());
    }
    private static DeferredBlock<MachineBlock> block(String name, MachineBlock.Kind kind) {
        var block = BLOCKS.register(name, () -> new MachineBlock(kind, BlockBehaviour.Properties.of().strength(3.5f).noOcclusion()));
        ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }
    private static DeferredBlock<PlanterPartBlock> part(String name, io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role role) {
        var block = BLOCKS.register(name, () -> new PlanterPartBlock(role, BlockBehaviour.Properties.of().strength(3.5f).noOcclusion()));
        ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }
    public Agristics(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus); RECIPES.register(bus); RECIPE_TYPES.register(bus); COMPONENTS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, Balance.SPEC);
        bus.addListener(this::capabilities);
        bus.addListener(Messages::register);
    }
    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CULTIVATOR_BE.get(), (be, side) -> be.automation(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CULTIVATOR_BE.get(), (be, side) -> be.water);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CULTIVATOR_BE.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EARLY_BE.get(), (be, side) -> be.automation(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EARLY_BE.get(), (be, side) -> be.water);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SEED_BE.get(), (be, side) -> be.port);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ACCESS_BE.get(), (be, side) -> be.connectedStructure() ? be : null);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, INFUSION_BE.get(), (be, side) -> be);
        event.registerBlockEntity(AECapabilities.CRAFTING_MACHINE,INFUSION_BE.get(),(be,side)->be);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INFUSION_BE.get(), (be, side) -> be.automation(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INPUT_BE.get(), (be, side) -> be.automation(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OUTPUT_BE.get(), (be, side) -> be.automation(side));
    }
}
