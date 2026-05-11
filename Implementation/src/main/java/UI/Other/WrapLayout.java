package UI.Other;

import java.awt.*;

public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }
    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            if (width == 0) width = Integer.MAX_VALUE;
            int x = getHgap(), rowH = 0, totalH = getVgap();
            for (Component c : target.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                if (x + d.width > width) { totalH += rowH + getVgap(); rowH = 0; x = getHgap(); }
                x += d.width + getHgap();
                rowH = Math.max(rowH, d.height);
            }
            totalH += rowH + getVgap();
            Insets in = target.getInsets();
            return new Dimension(width, totalH + in.top + in.bottom);
        }
    }
}