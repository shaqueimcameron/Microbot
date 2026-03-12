package net.runelite.client.plugins.microbot.joatalcher;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcCache;
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grandexchange.models.WikiPrice;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.api.player.Rs2PlayerCache;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Performance test script for measuring GameObject composition retrieval speed.
 * <p>
 * This script runs every 5 seconds and performs the following:
 * - Gets all GameObjects in the scene
 * - Retrieves the ObjectComposition for each GameObject
 * - Measures and logs the total time taken
 * - Reports average time per object
 * <p>
 * Useful for performance profiling and optimization testing.
 */
@Slf4j
public class JoatAlcherScript extends Script {

    @Inject
    Rs2TileItemCache rs2TileItemCache;
    @Inject
    Rs2TileObjectCache rs2TileObjectCache;
    @Inject
    Rs2PlayerCache rs2PlayerCache;
    @Inject
    Rs2NpcCache rs2NpcCache;

    enum State {
        INITIALIZING,
        BANKING,
        BUYING,
        ALCHING
    }

    State state = State.INITIALIZING;
    WikiPrice wikiPrice;
    int maxBuyQuantity;
    String last_item = null;
    PurchasableItem pi;


    Rs2ItemModel coins;
    Rs2ItemModel nature_runes;

    Map<String, PurchasableItem> geTransactionData = new HashMap<>();
    List<String> alchItems = new ArrayList<>();
    int currentItemIndex = 0;

    public State getState() {

        switch(state) {
            case INITIALIZING:
                if(getEssentialItems()) {
                    state = State.BUYING;
                }
                break;
            case BANKING:
                //check if we have the essential items to start buying and alching
                if(coins != null && nature_runes != null && Rs2Equipment.isWearing("staff of fire")) {
                    state = State.BUYING;
                }
                break;
            case BUYING:
                //if we have the items in inventory, start alching
                if(pi != null && Rs2Inventory.contains(pi.getName())) {
                    state = State.ALCHING;
                } else {
                    //otherwise go back to banking to get more coins and nature runes
                    state = State.BANKING;
                }
                break;
            case ALCHING:
                //if we run out of the item we're alching, go back to banking to get more
                if(pi == null || pi.getName() == null || Rs2Inventory.count(pi.getName())==0) {
                    state = State.BANKING;
                }
                break;

        }



        return state;
    }

    public boolean getEssentialItems() {

        if(Rs2Inventory.contains("coins") && Rs2Inventory.contains("nature rune")
                && Rs2Inventory.count()==2 && Rs2Equipment.isWearing("staff of fire")) {
            return true;
        }


        depositEverything(true);
        getItemFromBankInInventory("nature rune", false, -1, false);
        getItemFromBankInInventory("Coins", false, -1, false);

        if(Rs2Equipment.isWearing("staff of fire")) {
            equipItem("staff of fire");
        }

        coins = Rs2Inventory.get("Coins");
        nature_runes = Rs2Inventory.get("nature rune");

        if(coins==null || nature_runes==null) {
            log.info("Failed to get coins or nature runes from bank!");
            return false;
        }

        if(!openBank()) {
            log.info("Failed to open bank");
        }
        return true;
    }


    public boolean equipItem(String itemName) {
        if (Rs2Equipment.isWearing(itemName)) {
            return true;
        }
        if (Rs2Inventory.contains(itemName)) {
            Rs2Inventory.wield(itemName);
            return true;
        } else {
            if (!openBank()) {
                return false;
            }
            if (Rs2Bank.findBankItem(itemName) == null) {
                log.info("No " + itemName + " in bank!");
                Rs2Bank.closeBank();
                return false;
            }
            if (!Rs2Bank.withdrawAndEquip(itemName)) {
                log.info("Failed to withdraw and equip staff of fire");
                Rs2Bank.closeBank();
                return false;
            }
            Rs2Bank.closeBank();
            return true;

        }
    }

    public boolean depositEverything(boolean depositCoreItems) {
        if(!openBank()) {
            return false;
        }

        boolean return_val = Rs2Bank.depositEquipment() && (depositCoreItems ? Rs2Bank.depositAll()
                : Rs2Bank.depositAllExcept("Coins", "nature rune") && Rs2Bank.wearItem("staff of fire"));
        Rs2Bank.closeBank();
        return return_val;
    }
    
    public boolean openBank() {
        return Rs2Bank.isOpen() || Rs2Bank.openBank();
    }

    public boolean getItemFromBankInInventory(String itemName, boolean noted, int amount, boolean closeBankAfter) {
        if(Rs2Inventory.contains(itemName)) {
            return true;
        } else {
            if(!openBank()) {
                return false;
            }
            if(Rs2Bank.findBankItem(itemName) == null) {
                log.info("No "+itemName+ " in bank!");
                Rs2Bank.closeBank();
                return false;
            }
            if(noted) {
                Rs2Bank.setWithdrawAsNote();
            }
            if (amount<=0) {
                Rs2Bank.withdrawAll(itemName);
            } else if (amount==1) {
                Rs2Bank.withdrawOne(itemName);
            } else {
                Rs2Bank.withdrawX(itemName, amount);
            }
            if(closeBankAfter)
                Rs2Bank.closeBank();
            return true;

        }
    }

    public List<String> getAlchItems() {
        List<String> items = new ArrayList<>();
        items.add("Rune warhammer");
        items.add("Ruby necklace");
        items.add("Rune platebody");
        items.add("Rune platelegs");
        items.add("Rune kiteshield");
        items.add("Rune pickaxe");
        items.add("Rune plateskirt");
        items.add("Rune full helm");
        items.add("Rune scimitar");
        items.add("Rune longsword");
        items.add("Rune med helm");
        items.add("Rune chainbody");
        items.add("Rune battleaxe");
        items.add("Rune mace");
        items.add("Rune sq shield");
        items.add("Rune sword");
        items.add("Rune dagger");
        items.add("Rune 2h sword");
        return items;
    }

    String currentAlch;

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;

                if (currentAlch == null || pi == null) {
                    for (String itemName : getAlchItems()) {
                        if (Rs2Inventory.contains(itemName)) {
                            currentAlch = itemName;
                            log.info("Now alching: " + currentAlch);
                            break;
                        }
                    }
                    pi = new PurchasableItem(currentAlch, 0, 0, 0);
                }
                if(currentAlch==null || pi == null || pi.getName()==null) {
                    log.info("No more " + pi.getName() + " left, going back to banking state");
                    Microbot.stopPlugin(JoatAlcherPlugin.class);
                    return;
                }
                if(Rs2Inventory.contains("nature rune") && Rs2Equipment.isWearing("staff of fire"))
                    alchingState();
                else {
                    Microbot.stopPlugin(JoatAlcherPlugin.class);
                    log.info("You are not wearing staff of fire or dont have nature runes.");
                    return;
                }
                if(!Rs2Inventory.contains(pi.getName())) {
                    log.info("No more " + pi.getName() + " left, going back to banking state");
                    currentAlch = null;
                    pi = null;
                    Microbot.stopPlugin(JoatAlcherPlugin.class);
                }


            } catch (Exception ex) {
                log.error("Error in performance test loop", ex);
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        return true;
    }


//    public boolean run() {
//        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
//            try {
//                if (!Microbot.isLoggedIn()) return;
//
//                switch(state) {
//                    case BANKING:
//                    default:
//                        bankingState();
//                        break;
//                    case BUYING:
//                        buyingState();
//                        break;
//                    case ALCHING:
//                        alchingState();
//                        break;
//                }
//
//
//            } catch (Exception ex) {
//                log.error("Error in performance test loop", ex);
//            }
//        }, 0, 1, TimeUnit.MILLISECONDS);
//        return true;
//    }

    private void alchingState() {

        if(pi == null || pi.getName() == null || currentAlch == null) {
            log.info("Null Names!");
            Microbot.stopPlugin(JoatAlcherPlugin.class);
            return;
        }

        if(Rs2Inventory.count(pi.getName())==0) {
            log.info("No more " + pi.getName() + " left, going back to banking state");
            Microbot.stopPlugin(JoatAlcherPlugin.class);
            return;
        }

        if(Rs2Random.nextInt(0,200,0.5,false)<3) {
            log.info("Taking a break...");
            //int delay = random.nextInt(5000, 10000);
            sleep(Rs2Random.nextInt(5000, 120000, 0.5, false));
        }

        if(Rs2Tab.isCurrentTab(InterfaceTab.MAGIC)) {
            Widget highAlch = Rs2Widget.findWidget(MagicAction.HIGH_LEVEL_ALCHEMY.getName());
            if (highAlch == null || highAlch.getSpriteId() != 41) {
                log.info("Stupid glitch.. need to relog!");
                Microbot.stopPlugin(JoatAlcherPlugin.class);
                Rs2Player.logout();
                return;
            }
        }
        Rs2Magic.alch(pi.getName(),0,100);
        while(!Rs2Player.waitForXpDrop(Skill.MAGIC)) {
            sleep(Rs2Random.nextInt(200, 2000, 2, false));
        }
        sleep(Rs2Random.nextInt(0, 700, 2, false));

    }

    private void buyingState() {
        if(Rs2GrandExchange.openExchange()) {

            if(wikiPrice == null) {
                log.info("WikiPriceNull?");
                state = State.BANKING;
                return;
            }
            if(pi == null) {
                log.info("PiNull?");
                state = State.BANKING;
                return;
            }
            if(coins==null) {
                log.info("CoinsNull?");
                state = State.BANKING;
                return;
            } else {
                log.info("Coins: " + coins.getQuantity());
            }


            log.info("GE Open");
            if (Rs2GrandExchange.buyItem(pi.getName(), wikiPrice.buyPrice, maxBuyQuantity)) {
                log.info("Buy offer placed for " + pi.getName());
                GrandExchangeSlots o = Rs2GrandExchange.findSlotForItem(pi.getName(), false);
                if (o == null) {
                    log.info("Failed to find GE slot for " + pi.getName() + "... That's weird af");
                    Rs2GrandExchange.closeExchange();
                    state = State.BANKING;
                    return;
                }
                log.info("Waiting for the GE offer to buy.....");
                while (!Rs2GrandExchange.hasBoughtOffer(o)) {
                    sleep(200);
                }
                Rs2GrandExchange.collectAllToInventory();
                Rs2GrandExchange.closeExchange();
            }
            state = State.ALCHING;
        }
    }

    private void bankingState() {
        PurchasableItem a = new PurchasableItem("Ruby necklace", 1660, 1200, 70);
        geTransactionData.putIfAbsent(a.getName(), a);

        coins = Rs2Inventory.get("Coins");

        if(coins == null) {
            log.info("Opening bank...");
            while(!openBank()) {
                sleep(100);
            }
            sleep(1000);
            coins = Rs2Bank.findBankItem("Coins");
            sleep(2000);
            if(coins == null) {
                log.info("No coins avaialble!");
                this.mainScheduledFuture.cancel(true);
                return;
            }

            Rs2Bank.withdrawAll("Coins");
            Rs2Bank.closeBank();
        }

        log.info("Coins available: " + coins.getQuantity());
        log.info("Count: " + geTransactionData.size());
        sleep(Rs2Random.nextInt(0,1000,0.5,false));

        boolean next =false;
        for(Map.Entry<String, PurchasableItem> pi : geTransactionData.entrySet()) {

            if (last_item == null || next || geTransactionData.size()==1) {
                last_item = pi.getKey();
                this.pi = pi.getValue();
                wikiPrice = Rs2GrandExchange.getRealTimePrices(pi.getValue().getItemId());
                if (wikiPrice == null) {
                    log.info("Failed to get price for " + pi.getKey());
                    continue;
                }
                log.info("Buy price for " + pi.getKey() + ": " + wikiPrice.buyPrice + " | Sell price: " + wikiPrice.sellPrice);
                if (wikiPrice.buyPrice > pi.getValue().getMax_price()) {
                    log.info("WikiPrice>MaxPrice..... skipping!");
                    continue;
                }
                maxBuyQuantity = Math.min(pi.getValue().getGelimit(), coins.getQuantity() / wikiPrice.buyPrice);
                log.info("Going to buy " + maxBuyQuantity + " of " + pi.getKey() + " based on GE limit and coins available");

                state = State.BUYING;
                sleep(1000);
            } else if (last_item.equals(pi.getKey()) && geTransactionData.size() >1) {
                next = true;
                log.info("Already alched " + pi.getKey() + " skipping...");
                continue;
            }
        }


    }

    private void initializeItems() {
        alchItems = new ArrayList<String>();
        PurchasableItem a = new PurchasableItem("Ruby necklace", 1660, 1200, 70);
        geTransactionData.putIfAbsent(a.getName(), a);
//        junkItems.add("gold ring");
//        junkItems.add("sapphire ring");
//        junkItems.add("emerald ring");
//        junkItems.add("ruby ring");
//        junkItems.add("diamond ring");
//        junkItems.add("casket");
//        junkItems.add("oyster pearl");
//        junkItems.add("oyster pearls");
//        junkItems.add("teak logs");
//        junkItems.add("steel nails");
//        junkItems.add("mithril nails");
//        junkItems.add("giant seaweed");
//        junkItems.add("mithril cannonball");
//        junkItems.add("adamant cannonball");
//        junkItems.add("elkhorn frag");
//        junkItems.add("plank");
//        junkItems.add("oak plank");
//        junkItems.add("hemp seed");
//        junkItems.add("flax seed");
//        junkItems.add("ruby bracelet");
//        junkItems.add("emerald bracelet");
//        junkItems.add("mithril scimitar");
//        junkItems.add("mahogany repair kit");
//        junkItems.add("teak repair kit");
//        junkItems.add("rum");
//        junkItems.add("diamond bracelet");
//        junkItems.add("sapphire ring");
//        junkItems.add("emerald ring");
//        junkItems.add("emerald bracelet");
//        Rs2Inventory.dropAll( junkItems.toArray(new String[0]));
    }
}
