package academia;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de radar: cada série é um polígono sobre os eixos (valores de 0 a 100). */
public class GraficoRadar extends Grafico {

    private List<String> eixos = new ArrayList<>();
    private List<String> series = new ArrayList<>();
    private double[][] valores = new double[0][0];   // [serie][eixo], já normalizados 0..100
    private String[][] originais = new String[0][0]; // texto do valor real para o tooltip

    public GraficoRadar(String titulo) { super(titulo); }

    public void setDados(List<String> eixos, List<String> series, double[][] valores, String[][] originais) {
        this.eixos = eixos;
        this.series = series;
        this.valores = valores;
        this.originais = originais;
        repaint();
    }

    @Override protected boolean vazio() { return eixos.size() < 3 || series.isEmpty(); }

    @Override
    protected void desenhar(Graphics2D g, Rectangle a) {
        int w = getWidth();
        int topo = a.y + 6;
        topo += legenda(g, series, 14, topo, w - 28) + 4;
        int altura = a.y + a.height - topo;
        int raio = Math.min(w / 2 - 90, altura / 2 - 22);
        if (raio < 30) return;
        int cx = w / 2, cy = topo + altura / 2;
        int n = eixos.size();

        // teia
        for (int nivel = 1; nivel <= 4; nivel++) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                double ang = Math.PI / 2 - 2 * Math.PI * i / n;
                double x = cx + Math.cos(ang) * raio * nivel / 4, y = cy - Math.sin(ang) * raio * nivel / 4;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            g.setColor(new Color(0xE3E9E5));
            g.draw(p);
        }
        for (int i = 0; i < n; i++) {
            double ang = Math.PI / 2 - 2 * Math.PI * i / n;
            int x = (int) (cx + Math.cos(ang) * raio), y = (int) (cy - Math.sin(ang) * raio);
            g.setColor(new Color(0xD5DDD8));
            g.drawLine(cx, cy, x, y);
            g.setColor(Color.DARK_GRAY);
            String r = eixos.get(i);
            int tw = g.getFontMetrics().stringWidth(r);
            int lx = (int) (cx + Math.cos(ang) * (raio + 12));
            int ly = (int) (cy - Math.sin(ang) * (raio + 12));
            if (Math.cos(ang) < -0.2) lx -= tw; else if (Math.abs(Math.cos(ang)) <= 0.2) lx -= tw / 2;
            if (Math.sin(ang) < -0.2) ly += 10;
            g.drawString(r, lx, ly);
        }

        for (int s = 0; s < series.size(); s++) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                double ang = Math.PI / 2 - 2 * Math.PI * i / n;
                double v = Math.max(0, Math.min(100, valores[s][i])) / 100.0;
                double x = cx + Math.cos(ang) * raio * v, y = cy - Math.sin(ang) * raio * v;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            Color c = cor(s);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
            g.fill(p);
            g.setColor(c);
            g.setStroke(new BasicStroke(2f));
            g.draw(p);
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < n; i++) {
                double ang = Math.PI / 2 - 2 * Math.PI * i / n;
                double v = Math.max(0, Math.min(100, valores[s][i])) / 100.0;
                double x = cx + Math.cos(ang) * raio * v, y = cy - Math.sin(ang) * raio * v;
                g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
                dica(new Ellipse2D.Double(x - 7, y - 7, 14, 14), series.get(s) + " — " + eixos.get(i) + ": " + originais[s][i]);
            }
        }
    }
}
