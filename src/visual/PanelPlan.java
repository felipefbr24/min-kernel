package visual;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PanelPlan extends JPanel {
    private final List<EntradaPlan> schedule;

    public PanelPlan(List<EntradaPlan> schedule) {
        this.schedule = schedule;
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int padding = 10;
        int width = getWidth() - padding * 2;
        int height = getHeight() - padding * 2;
        if (width <= 0 || height <= 0) {
            return;
        }
        int maxTicks = Math.max(10, Math.min(60, schedule.size()));
        int start = Math.max(0, schedule.size() - maxTicks);
        int visible = schedule.size() - start;
        int barWidth = Math.max(8, width / Math.max(1, visible));
        int x = padding;
        int y = padding + 10;
        g2.drawString("Últimos " + visible + " ciclos", padding, y - 2);
        for (int i = start; i < schedule.size(); i++) {
            EntradaPlan entry = schedule.get(i);
            Color color = getColorForPid(entry.pid);
            g2.setColor(color);
            g2.fillRoundRect(x, y, barWidth - 2, height - 20, 8, 8);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y, barWidth - 2, height - 20, 8, 8);
            if (barWidth > 40) {
                String text = entry.label;
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (barWidth - fm.stringWidth(text)) / 2;
                int ty = y + (height - 20 + fm.getAscent()) / 2 - 2;
                g2.drawString(text, tx, ty);
            }
            x += barWidth;
        }
    }

    private Color getColorForPid(int pid) {
        if (pid < 0) {
            return new Color(200, 200, 200);
        }
        int hash = Integer.hashCode(pid);
        int r = 80 + Math.abs(hash * 31) % 150;
        int g = 80 + Math.abs(hash * 17) % 150;
        int b = 80 + Math.abs(hash * 11) % 150;
        return new Color(r, g, b);
    }
}
