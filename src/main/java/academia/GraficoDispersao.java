package academia;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de dispersão (correlação entre duas variáveis) com reta de regressão. */
public class GraficoDispersao extends Grafico {

    private List<Double> x = new ArrayList<>(), y = new ArrayList<>();
    private List<String> nomes = new ArrayList<>();
    private List<Integer> grupos = new ArrayList<>();
    private List<String> nomesGrupos = new ArrayList<>();
    private String rotX = "", rotY = "";
    private double[] reta = null;

    public GraficoDispersao(String titulo) { super(titulo); }

    public void setDados(List<Double> x, List<Double> y, List<String> nomes, List<Integer> grupos,
                         List<String> nomesGrupos, String rotX, String rotY) {
        this.x = x; this.y = y; this.nomes = nomes; this.grupos = grupos; this.nomesGrupos = nomesGrupos;
        this.rotX = rotX; this.rotY = rotY;
        reta = x.size() >= 2 ? Estatistica.regressao(x, y) : null;
        repaint();
    }

    @Override protected boolean vazio() { return x.isEmpty(); }

    @Override
    protected void desenhar(Graphics2D g, Rectangle a) {
        int w = getWidth();
        int topo = a.y + 6;
        topo += legenda(g, nomesGrupos, 14, topo, w - 28) + 6;
        int esq = 56, dir = 18, base = 40;
        int areaW = w - esq - dir, areaH = a.y + a.height - topo - base;
        if (areaH < 20) return;

        double minX = x.stream().mapToDouble(d -> d).min().orElse(0), maxX = x.stream().mapToDouble(d -> d).max().orElse(1);
        double minY = y.stream().mapToDouble(d -> d).min().orElse(0), maxY = y.stream().mapToDouble(d -> d).max().orElse(1);
        double fx = (maxX - minX) * 0.08 + 0.5, fy = (maxY - minY) * 0.08 + 0.5;
        minX -= fx; maxX += fx; minY -= fy; maxY += fy;

        for (int i = 0; i <= 4; i++) {
            int yy = topo + areaH - (int) (areaH * i / 4.0);
            int xx = esq + (int) (areaW * i / 4.0);
            g.setColor(new Color(0xEEF1EF));
            g.drawLine(esq, yy, w - dir, yy);
            g.drawLine(xx, topo, xx, topo + areaH);
            g.setColor(Color.GRAY);
            String ry = formatar(minY + (maxY - minY) * i / 4.0);
            g.drawString(ry, esq - 6 - g.getFontMetrics().stringWidth(ry), yy + 4);
            String rx = formatar(minX + (maxX - minX) * i / 4.0);
            g.drawString(rx, xx - g.getFontMetrics().stringWidth(rx) / 2, topo + areaH + 15);
        }
        g.setColor(Color.DARK_GRAY);
        g.drawString(rotX, esq + areaW / 2 - g.getFontMetrics().stringWidth(rotX) / 2, topo + areaH + 32);
        Graphics2D gr = (Graphics2D) g.create();
        gr.rotate(-Math.PI / 2);
        gr.drawString(rotY, -(topo + areaH / 2 + gr.getFontMetrics().stringWidth(rotY) / 2), 14);
        gr.dispose();

        final double mnX = minX, mxX = maxX, mnY = minY, mxY = maxY;
        final int t0 = topo;
        java.util.function.DoubleUnaryOperator px = v -> esq + areaW * (v - mnX) / (mxX - mnX);
        java.util.function.DoubleUnaryOperator py = v -> t0 + areaH - areaH * (v - mnY) / (mxY - mnY);

        if (reta != null) {
            g.setColor(Ui.VERMELHO);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0));
            Shape antes = g.getClip();
            g.clipRect(esq, topo, areaW, areaH);
            g.drawLine((int) px.applyAsDouble(minX), (int) py.applyAsDouble(reta[0] + reta[1] * minX),
                    (int) px.applyAsDouble(maxX), (int) py.applyAsDouble(reta[0] + reta[1] * maxX));
            g.setClip(antes);
            g.setStroke(new BasicStroke(1f));
        }
        for (int i = 0; i < x.size(); i++) {
            double cx = px.applyAsDouble(x.get(i)), cy = py.applyAsDouble(y.get(i));
            Color c = cor(grupos.get(i));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 190));
            g.fill(new Ellipse2D.Double(cx - 4.5, cy - 4.5, 9, 9));
            dica(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14),
                    nomes.get(i) + " — " + rotX + ": " + formatar(x.get(i)) + ", " + rotY + ": " + formatar(y.get(i)));
        }
        g.setColor(Color.GRAY);
        g.drawLine(esq, topo + areaH, w - dir, topo + areaH);
        g.drawLine(esq, topo, esq, topo + areaH);
    }
}
