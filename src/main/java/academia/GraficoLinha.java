package academia;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de linhas (tendência no tempo), uma ou mais séries. */
public class GraficoLinha extends Grafico {

    private List<String> eixoX = new ArrayList<>();
    private List<String> series = new ArrayList<>();
    private double[][] valores = new double[0][0]; // [serie][ponto]
    private Color[] cores = null;
    private String sufixo = "";
    private Double maxFixo = null;

    public GraficoLinha(String titulo) { super(titulo); }

    public void setDados(List<String> eixoX, List<String> series, double[][] valores, Color... cores) {
        this.eixoX = eixoX;
        this.series = series;
        this.valores = valores;
        this.cores = cores.length > 0 ? cores : null;
        repaint();
    }

    public void setSufixo(String s) { sufixo = s; }
    public void setMaximo(Double m) { maxFixo = m; }

    private Color corSerie(int s) { return cores != null ? cores[s % cores.length] : cor(s); }

    @Override protected boolean vazio() { return eixoX.isEmpty(); }

    @Override
    protected void desenhar(Graphics2D g, Rectangle a) {
        int w = getWidth();
        int esq = 50, dir = 18, base = 30;
        int topo = a.y + 8;
        if (series.size() > 1) topo += legenda(g, series, 14, topo, w - 28) + 6;
        int areaW = w - esq - dir, areaH = a.y + a.height - topo - base;
        if (areaH < 20) return;

        double max = 0;
        for (double[] s : valores) for (double v : s) max = Math.max(max, v);
        max = maxFixo != null ? maxFixo : escalaMaxima(max);

        for (int i = 0; i <= 4; i++) {
            int y = topo + areaH - (int) (areaH * i / 4.0);
            g.setColor(new Color(0xEEF1EF));
            g.drawLine(esq, y, w - dir, y);
            g.setColor(Color.GRAY);
            String rot = formatar(max * i / 4.0) + sufixo;
            g.drawString(rot, esq - 6 - g.getFontMetrics().stringWidth(rot), y + 4);
        }

        int n = eixoX.size();
        double passo = n > 1 ? areaW / (double) (n - 1) : 0;
        // rótulos do eixo X sem sobrepor
        int larguraRotulo = g.getFontMetrics().stringWidth("00/00") + 10;
        int pula = Math.max(1, (int) Math.ceil(larguraRotulo / Math.max(passo, 1)));
        for (int i = 0; i < n; i += pula) {
            int x = (int) (esq + (n > 1 ? i * passo : areaW / 2.0));
            g.setColor(Color.DARK_GRAY);
            String r = eixoX.get(i);
            g.drawString(r, x - g.getFontMetrics().stringWidth(r) / 2, topo + areaH + 16);
        }

        for (int s = 0; s < series.size(); s++) {
            g.setColor(corSerie(s));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int px = -1, py = -1;
            for (int i = 0; i < n; i++) {
                int x = (int) (esq + (n > 1 ? i * passo : areaW / 2.0));
                int y = topo + areaH - (int) (areaH * valores[s][i] / max);
                if (px >= 0) g.drawLine(px, py, x, y);
                px = x; py = y;
            }
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < n; i++) {
                int x = (int) (esq + (n > 1 ? i * passo : areaW / 2.0));
                int y = topo + areaH - (int) (areaH * valores[s][i] / max);
                g.setColor(Color.WHITE);
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setColor(corSerie(s));
                g.drawOval(x - 4, y - 4, 8, 8);
                String nome = series.get(s).isEmpty() ? "" : series.get(s) + ": ";
                dica(new Ellipse2D.Double(x - 7, y - 7, 14, 14), eixoX.get(i) + " — " + nome + formatar(valores[s][i]) + sufixo);
            }
        }
        g.setColor(Color.GRAY);
        g.drawLine(esq, topo + areaH, w - dir, topo + areaH);
    }
}
