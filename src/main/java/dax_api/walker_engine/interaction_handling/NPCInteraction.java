package dax_api.walker_engine.interaction_handling;

import dax_api.Ctx;
import dax_api.shared.helpers.InterfaceHelper;
import dax_api.walker_engine.WaitFor;
import net.runelite.api.NPC;
import net.runelite.rsb.utils.Filter;
import net.runelite.rsb.util.StdRandom;
import rsb_api.wrappers.RSCharacter;
import rsb_api.wrappers.RSNPC;
import rsb_api.wrappers.RSPlayer;
import rsb_api.wrappers.RSWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NPCInteraction {

    public static String[] GENERAL_RESPONSES = {"Sorry, I'm a bit busy.", "OK then.", "Yes.", "Okay..."};

    private static final int
            ITEM_ACTION_INTERFACE_WINDOW = 193,
            NPC_TALKING_INTERFACE_WINDOW = 231,
            PLAYER_TALKING_INTERFACE_WINDOW = 217,
            SELECT_AN_OPTION_INTERFACE_WINDOW = 219,
            SINGLE_OPTION_DIALOGUE_WINDOW = 229;

    private static final int[] ALL_WINDOWS = new int[]{ITEM_ACTION_INTERFACE_WINDOW,
													   NPC_TALKING_INTERFACE_WINDOW,
													   PLAYER_TALKING_INTERFACE_WINDOW,
													   SELECT_AN_OPTION_INTERFACE_WINDOW,
													   SINGLE_OPTION_DIALOGUE_WINDOW};


    private static NPCInteraction instance;

    private NPCInteraction(){

    }

    private static NPCInteraction getInstance(){
        return instance != null ? instance : (instance = new NPCInteraction());
    }

    /**
     *
     * @param rsnpcFilter
     * @param talkOptions
     * @param replyAnswers
     * @return
     */
    public static boolean talkTo(Filter<RSNPC> rsnpcFilter, String[] talkOptions, String[] replyAnswers) {
        if (!clickNpcAndWaitChat(rsnpcFilter, talkOptions)){
            return false;
        }
        handleConversation(replyAnswers);
        return true;
    }

    /**
     *
     * @param rsnpcFilter
     * @param options
     * @return
     */
    public static boolean clickNpcAndWaitChat(Filter<RSNPC> rsnpcFilter, String... options) {
        return clickNpc(rsnpcFilter, options) && waitForConversationWindow();
    }

    public static boolean clickNpc(Filter<RSNPC> rsnpcFilter, String... options) {
        RSNPC npc = Ctx.ctx.npcs.getNearest(rsnpcFilter);
        if (npc != null) {
            log.info("Cannot find NPC.");
        }
;
        return InteractionHelper.click(npc, options);
    }

    public static boolean waitForConversationWindow(){
        RSPlayer player = Ctx.ctx.players.getMyPlayer();
        RSCharacter rsCharacter = null;
        if (player != null){
            rsCharacter = new RSNPC(Ctx.ctx, (NPC) player.getInteracting());
        }
        return WaitFor.condition(rsCharacter != null ? WaitFor.getMovementRandomSleep(rsCharacter) : 10000, () -> {
            if (isConversationWindowUp()) {
                return WaitFor.Return.SUCCESS;
            }
            return WaitFor.Return.IGNORE;
        }) == WaitFor.Return.SUCCESS;
    }

    public static boolean isConversationWindowUp(){
        return Arrays.stream(ALL_WINDOWS).anyMatch(Ctx.ctx.interfaces::isValid);
    };

    public static void handleConversationRegex(String regex){
        while (true){
            if (WaitFor.condition(StdRandom.uniform(650, 800), () -> isConversationWindowUp() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS){
                break;
            }

            if (getClickHereToContinue() != null){
                clickHereToContinue();
                continue;
            }

            List<RSWidget> selectableOptions = getAllOptions(regex);
            if (selectableOptions == null || selectableOptions.size() == 0){
                WaitFor.milliseconds(100);
                continue;
            }

            Ctx.ctx.sleep((int) StdRandom.gaussian(350, 2250, 775, 350));
            log.info("Replying with option: " + selectableOptions.get(0).getText());
            Ctx.ctx.keyboard.sendText(selectableOptions.get(0).getIndex() + "", true);
            waitForNextOption();
        }
    }

    public static void handleConversation(String... options){
        log.info("Handling... " + Arrays.asList(options));
        List<String> blackList = new ArrayList<>();
        int limit = 0;
        while (limit++ < 50){
            if (WaitFor.condition(StdRandom.uniform(650, 800), () -> isConversationWindowUp() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS){
                log.info("Conversation window not up.");
                break;
            }

            if (getClickHereToContinue() != null){
                clickHereToContinue();
                limit = 0;
                continue;
            }

            List<RSWidget> selectableOptions = getAllOptions(options);
            if (selectableOptions == null || selectableOptions.size() == 0){
                WaitFor.milliseconds(150);
                continue;
            }

            for (RSWidget selected : selectableOptions){
                if(blackList.contains(selected.getText())){
                    continue;
                }
                Ctx.ctx.sleep((int) StdRandom.gaussian(350, 2250, 775, 350));
                log.info("Replying with option: " + selected.getText());
                blackList.add(selected.getText());
                Ctx.ctx.keyboard.sendText(selected.getIndex() + "", true);
                waitForNextOption();
                limit = 0;
                break;
            }
            Ctx.ctx.sleep(StdRandom.uniform(10,20));
        }
        if(limit > 50){
            log.info("Reached conversation limit.");
        }
    }

    /**
     *
     * @return Click here to continue conversation interface
     */
    private static RSWidget getClickHereToContinue(){
        List<RSWidget> list = getConversationDetails();
        if (list == null) {
            return null;
        }

        Optional<RSWidget> optional = list.stream().filter(rsInterface -> rsInterface.getText().equals("Click here to continue")).findAny();
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * Presses space bar
     */
    private static void clickHereToContinue(){
        log.info("Clicking continue.");
        Ctx.ctx.keyboard.sendText(" ", true);
        waitForNextOption();
    }

    /**
     * Waits for chat conversation text change.
     */
    private static void waitForNextOption(){
        List<String> interfaces = getAllInterfaces().stream().map(RSWidget::getText).collect(Collectors.toList());
        WaitFor.condition(5000, () -> {
            if (!interfaces.equals(getAllInterfaces().stream().map(RSWidget::getText).collect(Collectors.toList()))){
               return WaitFor.Return.SUCCESS;
            }
            return WaitFor.Return.IGNORE;
        });
    }

    /**
     *
     * @return List of all reply-able interfaces that has valid text.
     */
    private static List<RSWidget> getConversationDetails(){
        for (int window : ALL_WINDOWS){
            List<RSWidget> details = InterfaceHelper.getAllInterfaces(window).stream().filter(rsInterfaceChild -> {
                if (rsInterfaceChild.getSpriteId() != -1) {
                    return false;
                }
                String text = rsInterfaceChild.getText();
                return text != null && text.length() > 0;
            }).collect(Collectors.toList());
            if (details.size() > 0) {
                log.info("Conversation Options: [" + details.stream().map(RSWidget::getText).collect(
		                Collectors.joining(", ")) + "]");
                return details;
            }
        }
        return null;
    }

    /**
     *
     * @return List of all Chat interfaces
     */
    private static List<RSWidget> getAllInterfaces(){
        ArrayList<RSWidget> interfaces = new ArrayList<>();
        for (int window : ALL_WINDOWS) {
            interfaces.addAll(InterfaceHelper.getAllInterfaces(window));
        }
        return interfaces;
    }

    /**
     *
     * @param regex
     * @return list of conversation clickable options that matches {@code regex}
     */
    private static List<RSWidget> getAllOptions(String regex){
        List<RSWidget> list = getConversationDetails();
        return list != null ? list.stream().filter(rsInterface -> rsInterface.getText().matches(regex)).collect(
		        Collectors.toList()) : null;
    }

    /**
     *
     * @param options
     * @return list of conversation clickable options that is contained in options.
     */
    private static List<RSWidget> getAllOptions(String... options){
        final List<String> optionList = Arrays.stream(options).map(String::toLowerCase).collect(Collectors.toList());
        List<RSWidget> list = getConversationDetails();
        return list != null ? list.stream().filter(rsInterface -> optionList.contains(rsInterface.getText().trim().toLowerCase())).collect(
		        Collectors.toList()) : null;
    }

}
