package tgw.evolution.client.gui.controls;

import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public enum SortOrder {
    NONE(entries -> {
    }),
    AZ(entries -> {
        entries.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(((ListKeyBinds.KeyEntry) o1).getName().getString(),
                                                                       ((ListKeyBinds.KeyEntry) o2).getName().getString()));
    }),
    ZA(entries -> {
        entries.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(((ListKeyBinds.KeyEntry) o2).getName().getString(),
                                                                       ((ListKeyBinds.KeyEntry) o1).getName().getString()));
    });

    public static final SortOrder[] VALUES = values();
    private final ISort sorter;

    SortOrder(ISort sorter) {
        this.sorter = sorter;
    }

    public SortOrder cycle() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public String getName() {
        switch (this) {
            default:
            case NONE: {
                return I18n.get("evolution.gui.controls.none");
            }
            case AZ: {
                return I18n.get("evolution.gui.controls.az");
            }
            case ZA: {
                return I18n.get("evolution.gui.controls.za");
            }
        }
    }

    public void sort(List<ListKeyBinds.Entry> list) {
        this.sorter.sort(list);
    }
}
