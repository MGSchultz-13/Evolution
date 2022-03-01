package tgw.evolution.init;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

public final class EvolutionStyles {

    public static final Style DARK_RED = Style.EMPTY.applyFormat(ChatFormatting.DARK_RED);
    public static final Style DAMAGE = DARK_RED;
    public static final Style GREEN = Style.EMPTY.applyFormat(ChatFormatting.GREEN);
    public static final Style SPEED = GREEN;
    public static final Style MINING = Style.EMPTY.applyFormat(ChatFormatting.AQUA);
    public static final Style REACH = Style.EMPTY.applyFormat(ChatFormatting.YELLOW);
    public static final Style WHITE = Style.EMPTY.applyFormat(ChatFormatting.WHITE);
    public static final Style INFO = Style.EMPTY.applyFormat(ChatFormatting.BLUE);
    public static final Style COLD = INFO;
    public static final Style HEAT = Style.EMPTY.applyFormat(ChatFormatting.RED);
    public static final Style PROPERTY = Style.EMPTY.applyFormat(ChatFormatting.GOLD);
    public static final Style LIGHT_GREY = Style.EMPTY.applyFormat(ChatFormatting.GRAY);
    public static final Style DURABILITY = LIGHT_GREY;
    public static final Style MASS = Style.EMPTY.applyFormat(ChatFormatting.DARK_GREEN);
    public static final Style LORE = Style.EMPTY.applyFormat(ChatFormatting.LIGHT_PURPLE).withItalic(true);
    public static final Style EFFECTS = Style.EMPTY.applyFormat(ChatFormatting.DARK_AQUA);
    public static final Style CONFIG = Style.EMPTY.applyFormat(ChatFormatting.YELLOW).withBold(true);

    private EvolutionStyles() {
    }
}
