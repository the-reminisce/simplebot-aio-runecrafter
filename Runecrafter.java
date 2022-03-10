package me.remie.osrsps.runecrafting;

import net.runelite.api.coords.WorldPoint;
import simple.hooks.filters.SimpleBank;
import simple.hooks.filters.SimpleSkills.Skills;
import simple.hooks.scripts.Category;
import simple.hooks.scripts.ScriptManifest;
import simple.hooks.simplebot.ChatMessage;
import simple.hooks.simplebot.Game.Tab;
import simple.hooks.simplebot.teleporter.Teleporter;
import simple.hooks.wrappers.SimpleNpc;
import simple.hooks.wrappers.SimpleObject;
import simple.hooks.wrappers.SimpleWidget;
import simple.robot.script.Script;
import simple.robot.utils.WorldArea;

import java.awt.*;
import java.util.regex.Pattern;

/**
 * @author Seth Davis
 * @Email <sethdavis321@gmail.com>
 * @Discord Reminisce#1707
 */
@ScriptManifest(author = "Seth", category = Category.RUNECRAFTING, description = "Crafts all different types of runes that are supported from the home teleporter!\n"
        + "Just have the talisman that you want to craft in your inventory hit start!", name = "RAuto Runecrafter", version = "1.0",
        discord = "Reminisce#1707", servers = {"OSRSPS"})
public class Runecrafter extends Script {

    private Teleporter teleporter;

	private long startTime;
	private int startExp, gainedRunes;
	private static final WorldArea HOME_AREA = new WorldArea(new WorldPoint(3072, 3521, 0), new WorldPoint(3072, 3464, 0), new WorldPoint(3137, 3474, 0), new WorldPoint(3137, 3521, 0));

	@Override
    public void onExecute() {
        teleporter = new Teleporter(ctx);
        startTime = System.currentTimeMillis();
        startExp = ctx.skills.experience(Skills.RUNECRAFT);
    }

    @Override
    public void onProcess() {
        if (HOME_AREA.containsPoint(ctx.players.getLocal().getLocation())) {
            ctx.game.tab(Tab.INVENTORY);
            if (ctx.inventory.populate().filter("Pure essence").population() >= 27) {
                if (ctx.bank.bankOpen()) {
                    ctx.bank.closeBank();
                } else {
                    String type = ctx.inventory.populate().filter(Pattern.compile(".*? talisman")).next().getName().replace(" talisman", "");
                    if (teleporter.open()) {
                        teleporter.teleportStringPath("Skilling", "Runecrafting", type + " altar");
                        if (ctx.dialogue.dialogueOpen()) {
                            ctx.dialogue.populate().filterContains("Yes").next().click();
                        }
                    }
                }
            } else {
                if (ctx.bank.bankOpen()) {
                    ctx.bank.depositAllExcept(t -> t.getName().contains("talisman"));
                    ctx.bank.withdraw(7936, SimpleBank.Amount.ALL);
                } else {
                    final SimpleNpc banker = ctx.npcs.populate().filter("Banker").filterHasAction("Bank").nearest().next();
                    if (banker != null && banker.validateInteractable()) {
                        if (banker.click("Bank")) {
                            ctx.onCondition(() -> ctx.bank.bankOpen(), 250, 5);
                        }
                    }
                }
            }
        } else {
            if (ctx.inventory.populate().filter(Pattern.compile(".*? rune")).population(true) >= 26) {
                teleportHome();
            } else {
                if (!ctx.objects.populate().filter("Mysterious ruins").isEmpty()) {
                    WorldPoint cached = ctx.players.getLocal().getLocation();
                    if (ctx.inventory.itemSelectionState() != 1) {
                        ctx.inventory.populate().filter(Pattern.compile(".*? talisman")).next().click("Use");
                    }
                    ctx.objects.next().click("Use");
                    ctx.onCondition(() -> ctx.players.getLocal().getLocation().distanceTo(cached) >= 20, 500, 8);
                }
                if (!ctx.objects.populate().filter("Altar").filterHasAction("Craft-rune").isEmpty()) {
                    if (ctx.inventory.itemSelectionState() == 1) {
                        ctx.inventory.populate().filter(Pattern.compile(".*? talisman")).next().click(0);
                    }
                    int count = ctx.inventory.populate().population();
                    SimpleObject obj = ctx.objects.nearest().next();
                    if (obj != null && obj.validateInteractable()) {
                        if (obj.click("Craft-rune")) {
                            if (ctx.onCondition(() -> count > ctx.inventory.populate().population(), 500, 8)) {
                                gainedRunes += (ctx.inventory.populate().population(true) - 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void teleportHome() {
        if (!HOME_AREA.containsPoint(ctx.players.getLocal().getLocation())) {
            if (ctx.game.tab(Tab.MAGIC)) {
                final SimpleWidget w = ctx.widgets.getWidget(218, 6);
                if (w != null && !w.isHidden()) {
                    if (w.click(1)) {
                        ctx.onCondition(() -> HOME_AREA.containsPoint(ctx.players.getLocal().getLocation()), 250, 8);
                    }
                }
            }
        }
    }

    @Override
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setColor(Color.BLACK);
        g.fillRect(5, 2, 192, 72);
        g.setColor(Color.decode("#b8dbff"));
        g.drawRect(5, 2, 192, 72);
        g.drawLine(8, 24, 194, 24);

        g.setColor(Color.decode("#5ff8d0"));
        g.drawString("RAuto RuneCrafter                  v. " + "0.1", 12, 20);
        g.drawString("Time: " + ctx.paint.formatTime(System.currentTimeMillis() - startTime), 14, 42);
        int totalExp = ctx.skills.experience(Skills.RUNECRAFT) - startExp;
        g.drawString("XP: " + totalExp + " (" + ctx.paint.valuePerHour(totalExp, startTime) + ")", 14, 56);
        g.drawString("Runes: " + gainedRunes + " (" + ctx.paint.valuePerHour(gainedRunes, startTime) + ")", 14, 70);
    }

    @Override
    public void onChatMessage(ChatMessage e) {
    }

    @Override
    public void onTerminate() {

    }

}