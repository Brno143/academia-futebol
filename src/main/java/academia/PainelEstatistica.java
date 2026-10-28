package academia;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/** Mostra os gráficos da disciplina de estatística salvos na pasta graficos_estatistica/. */
public class PainelEstatistica extends JPanel {

    private static final File PASTA = new File("graficos_estatistica");
    private final JPanel grade = new JPanel(new GridLayout(0, 2, 12, 12));

    public PainelEstatistica() {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel titulo = new JLabel("Gráficos da disciplina de estatística");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        JButton recarregar = new JButton("Recarregar");
        recarregar.addActionListener(e -> carregar());
        JPanel barra = new JPanel(new BorderLayout());
        barra.add(titulo, BorderLayout.WEST);
        barra.add(recarregar, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(grade);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void carregar() {
        grade.removeAll();
        File[] arquivos = PASTA.listFiles((d, n) -> n.toLowerCase().matches(".*\\.(png|jpg|jpeg)"));
        if (arquivos == null || arquivos.length == 0) {
            grade.add(new JLabel("Salve os gráficos de estatística (png ou jpg) na pasta "
                    + PASTA.getAbsolutePath() + " e clique em Recarregar."));
        } else {
            Arrays.sort(arquivos);
            for (File f : arquivos) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    int largura = 560;
                    int altura = img.getHeight() * largura / img.getWidth();
                    JLabel l = new JLabel(new ImageIcon(img.getScaledInstance(largura, altura, Image.SCALE_SMOOTH)));
                    l.setText(f.getName().replaceFirst("\\.[^.]+$", "").replace('_', ' '));
                    l.setHorizontalTextPosition(SwingConstants.CENTER);
                    l.setVerticalTextPosition(SwingConstants.BOTTOM);
                    grade.add(l);
                } catch (Exception ex) {
                    grade.add(new JLabel("Não foi possível abrir " + f.getName()));
                }
            }
        }
        grade.revalidate();
        grade.repaint();
    }
}
