package academia;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de barras (simples ou agrupado) desenhado com Graphics2D, sem biblioteca externa. */
public class GraficoBarras extends JPanel {

    private final String titulo;
    private List<String> categorias = new ArrayList<>();
    private List<String> series = new ArrayList<>();
    private double[][] valores = new double[0][0]; // [serie][categoria]
    private Color[] cores = {Ui.VERDE};

    public GraficoBarras(String titulo) {
        this.titulo = titulo;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(420, 280));
        setBorder(BorderFactory.createLineBorder(new Color(0xDDE5DF)));
    }

    public void setDados(List<String> categorias, List<String> series, double[][] valores, Color... cores) {
        this.categorias = categorias;
        this.series = series;
        this.valores = valores;
        if (cores.length > 0) this.cores = cores;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        g.setColor(new Color(0x1C2420));
        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g.drawString(titulo, 14, 24);

        if (categorias.isEmpty()) return;

        int esq = 50, dir = 14, topo = 48, base = 36;
        if (series.size() > 1) topo = 64;
        int areaW = w - esq - dir, areaH = h - topo - base;

        double max = 0;
        for (double[] s : valores) for (double v : s) max = Math.max(max, v);
        max = escalaMaxima(max);

        // linhas de grade
        g.setFont(getFont().deriveFont(11f));
        for (int i = 0; i <= 4; i++) {
            int y = topo + areaH - (int) (areaH * i / 4.0);
            g.setColor(new Color(0xEEF1EF));
            g.drawLine(esq, y, w - dir, y);
            g.setColor(Color.GRAY);
            String rot = formatar(max * i / 4.0);
            g.drawString(rot, esq - 6 - g.getFontMetrics().stringWidth(rot), y + 4);
        }

        // legenda
        if (series.size() > 1) {
            int x = 14;
            for (int s = 0; s < series.size(); s++) {
                g.setColor(cores[s % cores.length]);
                g.fillRect(x, 36, 12, 12);
                g.setColor(Color.DARK_GRAY);
                g.drawString(series.get(s), x + 16, 46);
                x += 28 + g.getFontMetrics().stringWidth(series.get(s));
            }
        }

        int nCat = categorias.size(), nSer = series.size();
        double larguraGrupo = areaW / (double) nCat;
        double larguraBarra = Math.min(48, larguraGrupo * 0.7 / nSer);
        for (int c = 0; c < nCat; c++) {
            double inicio = esq + c * larguraGrupo + (larguraGrupo - larguraBarra * nSer) / 2;
            for (int s = 0; s < nSer; s++) {
                double v = valores[s][c];
                int bh = (int) (areaH * v / max);
                int x = (int) (inicio + s * larguraBarra);
                int y = topo + areaH - bh;
                g.setColor(cores[s % cores.length]);
                g.fillRect(x + 1, y, (int) larguraBarra - 2, bh);
                g.setColor(Color.DARK_GRAY);
                String rot = formatar(v);
                int tw = g.getFontMetrics().stringWidth(rot);
                g.drawString(rot, x + (int) (larguraBarra - tw) / 2, y - 4);
            }
            g.setColor(Color.DARK_GRAY);
            String cat = categorias.get(c);
            int tw = g.getFontMetrics().stringWidth(cat);
            g.drawString(cat, (int) (esq + c * larguraGrupo + (larguraGrupo - tw) / 2), h - base + 18);
        }
        g.setColor(Color.GRAY);
        g.drawLine(esq, topo + areaH, w - dir, topo + areaH);
    }

    /** Arredonda o topo do eixo pra um número "redondo" divisível em 4 partes (ex.: 2190 -> 2800). */
    private static double escalaMaxima(double max) {
        if (max <= 0) return 4;
        double passoBruto = max * 1.1 / 4;
        double potencia = Math.pow(10, Math.floor(Math.log10(passoBruto)));
        double[] opcoes = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
        for (double o : opcoes) if (o * potencia >= passoBruto) return o * potencia * 4;
        return 10 * potencia * 4;
    }

    private static String formatar(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }
}
