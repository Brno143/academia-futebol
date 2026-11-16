package academia;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de barras (simples ou agrupado) desenhado com Graphics2D, sem biblioteca externa. */
public class GraficoBarras extends Grafico {

    private List<String> categorias = new ArrayList<>();
    private List<String> series = new ArrayList<>();
    private double[][] valores = new double[0][0]; // [serie][categoria]
    private Color[] cores = null;
    /** Linhas de referência horizontais opcionais (ex.: média). */
    private final List<String> refNomes = new ArrayList<>();
    private final List<Double> refValores = new ArrayList<>();
    private String sufixo = "";

    public GraficoBarras(String titulo) { super(titulo); }

    public void setDados(List<String> categorias, List<String> series, double[][] valores, Color... cores) {
        this.categorias = categorias;
        this.series = series;
        this.valores = valores;
        this.cores = cores.length > 0 ? cores : null;
        refNomes.clear();
        refValores.clear();
        repaint();
    }

    /** Uma série só, a partir de um SELECT com 2 colunas (rótulo, valor). */
    public void setDados(Db.Resultado r) {
        List<String> cats = new ArrayList<>();
        double[][] v = new double[1][r.linhas().size()];
        for (int i = 0; i < r.linhas().size(); i++) {
            cats.add(String.valueOf(r.valor(i, 0)));
            v[0][i] = Ui.decimal(r.valor(i, 1));
        }
        setDados(cats, List.of(""), v);
    }

    public void setSufixo(String s) { sufixo = s; repaint(); }

    public void referencia(String nome, double valor) { refNomes.add(nome); refValores.add(valor); repaint(); }

    private Color corSerie(int s) { return cores != null ? cores[s % cores.length] : cor(s); }

    @Override protected boolean vazio() { return categorias.isEmpty(); }

    @Override
    protected void desenhar(Graphics2D g, Rectangle a) {
        int w = getWidth();
        int esq = 50, dir = 14, base = 30;
        int topo = a.y + 8;
        if (series.size() > 1 || !refNomes.isEmpty()) {
            List<String> nomes = new ArrayList<>(series.size() > 1 ? series : List.of());
            int x = 14;
            if (series.size() > 1) {
                for (int s = 0; s < series.size(); s++) {
                    g.setColor(corSerie(s));
                    g.fillRect(x, topo, 11, 11);
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(series.get(s), x + 15, topo + 10);
                    x += 28 + g.getFontMetrics().stringWidth(series.get(s));
                }
            }
            for (int i = 0; i < refNomes.size(); i++) {
                g.setColor(i == 0 ? Ui.VERMELHO : Ui.AZUL);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 3}, 0));
                g.drawLine(x, topo + 6, x + 16, topo + 6);
                g.setStroke(new BasicStroke(1f));
                g.setColor(Color.DARK_GRAY);
                String rot = refNomes.get(i) + " = " + formatar(refValores.get(i));
                g.drawString(rot, x + 20, topo + 10);
                x += 34 + g.getFontMetrics().stringWidth(rot);
            }
            nomes.clear();
            topo += 22;
        }
        int areaW = w - esq - dir, areaH = a.y + a.height - topo - base;
        if (areaH < 20) return;

        double max = 0;
        for (double[] s : valores) for (double v : s) max = Math.max(max, v);
        for (double r : refValores) max = Math.max(max, r);
        max = escalaMaxima(max);

        for (int i = 0; i <= 4; i++) {
            int y = topo + areaH - (int) (areaH * i / 4.0);
            g.setColor(new Color(0xEEF1EF));
            g.drawLine(esq, y, w - dir, y);
            g.setColor(Color.GRAY);
            String rot = formatar(max * i / 4.0);
            g.drawString(rot, esq - 6 - g.getFontMetrics().stringWidth(rot), y + 4);
        }

        int nCat = categorias.size(), nSer = series.size();
        double larguraGrupo = areaW / (double) nCat;
        double larguraBarra = Math.min(48, larguraGrupo * 0.75 / nSer);
        boolean mostrarValores = larguraBarra >= 18;
        for (int c = 0; c < nCat; c++) {
            double inicio = esq + c * larguraGrupo + (larguraGrupo - larguraBarra * nSer) / 2;
            for (int s = 0; s < nSer; s++) {
                double v = valores[s][c];
                int bh = (int) (areaH * v / max);
                int x = (int) (inicio + s * larguraBarra);
                int y = topo + areaH - bh;
                g.setColor(corSerie(s));
                g.fillRect(x + 1, y, (int) larguraBarra - 2, bh);
                String nomeSerie = series.get(s).isEmpty() ? "" : series.get(s) + ": ";
                dica(ret(x, topo, larguraBarra, areaH), categorias.get(c) + " — " + nomeSerie + formatar(v) + sufixo);
                if (mostrarValores) {
                    g.setColor(Color.DARK_GRAY);
                    String rot = formatar(v);
                    int tw = g.getFontMetrics().stringWidth(rot);
                    g.drawString(rot, x + (int) (larguraBarra - tw) / 2, y - 4);
                }
            }
            g.setColor(Color.DARK_GRAY);
            String cat = cortar(g, categorias.get(c), (int) larguraGrupo - 4);
            int tw = g.getFontMetrics().stringWidth(cat);
            g.drawString(cat, (int) (esq + c * larguraGrupo + (larguraGrupo - tw) / 2), topo + areaH + 16);
        }
        g.setColor(Color.GRAY);
        g.drawLine(esq, topo + areaH, w - dir, topo + areaH);

        for (int i = 0; i < refValores.size(); i++) {
            int y = topo + areaH - (int) (areaH * refValores.get(i) / max);
            g.setColor(i == 0 ? Ui.VERMELHO : Ui.AZUL);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 3}, 0));
            g.drawLine(esq, y, w - dir, y);
            g.setStroke(new BasicStroke(1f));
        }
    }
}
