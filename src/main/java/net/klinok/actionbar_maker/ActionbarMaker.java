package net.klinok.actionbar_maker;

import com.mojang.logging.LogUtils;
import net.klinok.actionbar_maker.network.AbmNetwork;
import net.klinok.actionbar_maker.server.AbmCommands;
import net.klinok.actionbar_maker.server.AbmStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ActionbarMaker.MODID)
public class ActionbarMaker {
    public static final String MODID = "actionbar_maker";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ActionbarMaker() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(AbmNetwork::register);
        AbmStorage.ensureGlobalDirectories();
        LOGGER.info("Actionbar Maker loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AbmCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        AbmStorage.ensureGlobalDirectories();
        AbmStorage.ensureWorldDirectories(event.getServer());
    }
}
