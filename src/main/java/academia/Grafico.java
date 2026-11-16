package academia;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Base dos gráficos: título, subtítulo, área branca e dica (tooltip) ao passar o mouse. */
public abstract class Grafico extends JPanel {

    protected String titulo;
    protected String subtitulo = "";
    /** Áreas "sensíveis" ao mouse registradas no último desenho (para o tooltip). */
    private final List<Shape> areas = new ArrayList<>();
    private final List<String> dicas = new ArrayList<>();

    protected Grafico(String titulo) {
        this.titulo = titulo;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(520, 320));
        setBorder(BorderFactory.createLineBorder(new Color(0xDDE5DF)));
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(100);
    }

    public void setTitulo(String t) { titulo = t; repaint(); }
    public void setSubtitulo(String s) { subtitulo = s == null ? "" : s; repaint(); }

    @Override
    protected final void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1C2420));
        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g.drawString(titulo, 14, 24);
        int topo = 34;
        if (!subtitulo.isEmpty()) {
            g.setFont(getFont().deriveFont(11.5f));
            g.setColor(new Color(0x5A6A60));
            g.drawString(subtitulo, 14, 42);
            topo = 50;
        }
        areas.clear();
        dicas.clear();
        g.setFont(getFont().deriveFont(11f));
        if (vazio()) {
            g.setColor(Color.GRAY);
            g.drawString("Sem dados para os filtros selecionados.", 14, topo + 30);
            return;
        }
        desenhar(g, new Rectangle(0, topo, getWidth(), getHeight() - topo));
    }

    protected abstract boolean vazio();

    /** Desenha o conteúdo do gráfico dentro de "area" (abaixo do título). */
    protected abstract void desenhar(Graphics2D g, Rectangle area);

    protected void dica(Shape area, String texto) { areas.add(area); dicas.add(texto); }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = areas.size() - 1; i >= 0; i--)
            if (areas.get(i).contains(e.getPoint())) return dicas.get(i);
        return null;
    }

    protected static Color cor(int i) { return Ui.PALETA[i % Ui.PALETA.length]; }

    protected static String formatar(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    /** Arredonda o topo do eixo para um número "redondo" divisível em 4 partes. */
    protected static double escalaMaxima(double max) {
        if (max <= 0) return 4;
        double passoBruto = max * 1.1 / 4;
        double potencia = Math.pow(10, Math.floor(Math.log10(passoBruto)));
        double[] opcoes = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
        for (double o : opcoes) if (o * potencia >= passoBruto) return o * potencia * 4;
        return 10 * potencia * 4;
    }

    /** Legenda em linha, a partir de (x, y). Devolve a altura usada. */
    protected int legenda(Graphics2D g, List<String> nomes, int x, int y, int largura) {
        int x0 = x, linha = 0;
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < nomes.size(); i++) {
            int w = 18 + fm.stringWidth(nomes.get(i)) + 12;
            if (x + w > x0 + largura) { x = x0; linha++; }
            g.setColor(cor(i));
            g.fillRect(x, y + linha * 16, 11, 11);
            g.setColor(Color.DARK_GRAY);
            g.drawString(nomes.get(i), x + 15, y + linha * 16 + 10);
            x += w;
        }
        return (linha + 1) * 16;
    }

    protected static String cortar(Graphics2D g, String s, int largura) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= largura) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > largura) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    protected static Rectangle2D ret(double x, double y, double w, double h) { return new Rectangle2D.Double(x, y, w, h); }
}
