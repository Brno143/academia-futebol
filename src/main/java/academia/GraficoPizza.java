package academia;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de pizza (rosca) com percentuais e legenda. */
public class GraficoPizza extends Grafico {

    private List<String> rotulos = new ArrayList<>();
    private double[] valores = new double[0];
    private Color[] cores = null;

    public GraficoPizza(String titulo) { super(titulo); }

    public void setDados(List<String> rotulos, double[] valores, Color... cores) {
        this.rotulos = rotulos;
        this.valores = valores;
        this.cores = cores.length > 0 ? cores : null;
        repaint();
    }

    /** A partir de um SELECT com 2 colunas (rótulo, valor). */
    public void setDados(Db.Resultado r, Color... cores) {
        List<String> rot = new ArrayList<>();
        double[] v = new double[r.linhas().size()];
        for (int i = 0; i < v.length; i++) { rot.add(String.valueOf(r.valor(i, 0))); v[i] = Ui.decimal(r.valor(i, 1)); }
        setDados(rot, v, cores);
    }

    private Color corFatia(int i) { return cores != null ? cores[i % cores.length] : cor(i); }

    private double total() { double t = 0; for (double v : valores) t += v; return t; }

    @Override protected boolean vazio() { return rotulos.isEmpty() || total() <= 0; }

    @Override
    protected void desenhar(Graphics2D g, Rectangle a) {
        double total = total();
        int legW = 190;
        int d = Math.min(a.width - legW - 40, a.height - 24);
        if (d < 40) d = 40;
        int cx = 24, cy = a.y + (a.height - d) / 2;
        double inicio = 90;
        for (int i = 0; i < valores.length; i++) {
            double ang = 360 * valores[i] / total;
            Arc2D arco = new Arc2D.Double(cx, cy, d, d, inicio, -ang, Arc2D.PIE);
            g.setColor(corFatia(i));
            g.fill(arco);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.draw(arco);
            String pct = String.format("%.1f%%", 100 * valores[i] / total);
            dica(arco, rotulos.get(i) + ": " + formatar(valores[i]) + " (" + pct + ")");
            if (ang >= 18) {
                double meio = Math.toRadians(inicio - ang / 2);
                int tx = (int) (cx + d / 2.0 + Math.cos(meio) * d * 0.36);
                int ty = (int) (cy + d / 2.0 - Math.sin(meio) * d * 0.36);
                g.setFont(getFont().deriveFont(Font.BOLD, 11f));
                int tw = g.getFontMetrics().stringWidth(pct);
                g.drawString(pct, tx - tw / 2, ty + 4);
            }
            inicio -= ang;
        }
        g.setStroke(new BasicStroke(1f));
        // furo da rosca
        g.setColor(Color.WHITE);
        int furo = (int) (d * 0.42);
        g.fillOval(cx + (d - furo) / 2, cy + (d - furo) / 2, furo, furo);
        g.setColor(new Color(0x1C2420));
        g.setFont(getFont().deriveFont(Font.BOLD, 15f));
        String t = formatar(total);
        g.drawString(t, cx + d / 2 - g.getFontMetrics().stringWidth(t) / 2, cy + d / 2 + 2);
        g.setFont(getFont().deriveFont(10f));
        g.setColor(Color.GRAY);
        g.drawString("total", cx + d / 2 - g.getFontMetrics().stringWidth("total") / 2, cy + d / 2 + 15);

        // legenda
        g.setFont(getFont().deriveFont(11.5f));
        int lx = cx + d + 24, ly = a.y + Math.max(10, (a.height - valores.length * 20) / 2);
        for (int i = 0; i < valores.length; i++) {
            g.setColor(corFatia(i));
            g.fillRect(lx, ly + i * 20, 12, 12);
            g.setColor(Color.DARK_GRAY);
            String txt = cortar(g, rotulos.get(i), getWidth() - lx - 80) + ":  " + formatar(valores[i])
                    + String.format(" (%.0f%%)", 100 * valores[i] / total);
            g.drawString(txt, lx + 18, ly + i * 20 + 11);
        }
    }
}
