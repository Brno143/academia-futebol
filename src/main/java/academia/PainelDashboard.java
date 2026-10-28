package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tela inicial: indicadores gerais + 4 gráficos com dados do banco. */
public class PainelDashboard extends JPanel {

    private final JLabel[] valores = new JLabel[5];
    private final GraficoBarras gVo2 = new GraficoBarras("VO2 máx médio por categoria");
    private final GraficoBarras gMando = new GraficoBarras("Resultados em casa x fora");
    private final GraficoBarras gSetor = new GraficoBarras("Minutos jogados por setor");
    private final GraficoBarras gTreino = new GraficoBarras("Minutos de treino por tipo de sessão");

    public PainelDashboard() {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        String[] rotulos = {"Atletas", "Comissão técnica", "Jogos", "Vitórias / Empates / Derrotas", "Gols pró x contra"};
        JPanel cards = new JPanel(new GridLayout(1, 5, 10, 0));
        for (int i = 0; i < 5; i++) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(0xEEF4EF));
            card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            JLabel r = new JLabel(rotulos[i]);
            r.setForeground(new Color(0x4A5A50));
            valores[i] = new JLabel("-");
            valores[i].setFont(valores[i].getFont().deriveFont(Font.BOLD, 24f));
            valores[i].setForeground(Ui.VERDE);
            card.add(r, BorderLayout.NORTH);
            card.add(valores[i], BorderLayout.CENTER);
            cards.add(card);
        }

        JPanel topo = new JPanel(new BorderLayout(0, 10));
        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> carregar());
        JPanel barra = new JPanel(new BorderLayout());
        JLabel titulo = new JLabel("Painel da academia");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        barra.add(titulo, BorderLayout.WEST);
        barra.add(atualizar, BorderLayout.EAST);
        topo.add(barra, BorderLayout.NORTH);
        topo.add(cards, BorderLayout.CENTER);

        JPanel graficos = new JPanel(new GridLayout(2, 2, 12, 12));
        graficos.add(gVo2);
        graficos.add(gMando);
        graficos.add(gSetor);
        graficos.add(gTreino);

        add(topo, BorderLayout.NORTH);
        add(graficos, BorderLayout.CENTER);
    }

    public void carregar() {
        try {
            Db.Resultado r = Db.consultar("""
                    SELECT (SELECT COUNT(*) FROM ATLETA) AS atletas,
                           (SELECT COUNT(*) FROM MEMBROCOMISSAO) AS comissao,
                           COUNT(*) AS jogos,
                           SUM(gols_pro > gols_contra) AS vitorias,
                           SUM(gols_pro = gols_contra) AS empates,
                           SUM(gols_pro < gols_contra) AS derrotas,
                           SUM(gols_pro) AS gols_pro,
                           SUM(gols_contra) AS gols_contra
                    FROM JOGO""");
            Object[] l = r.linhas().get(0);
            valores[0].setText(String.valueOf(Ui.inteiro(l[0])));
            valores[1].setText(String.valueOf(Ui.inteiro(l[1])));
            valores[2].setText(String.valueOf(Ui.inteiro(l[2])));
            valores[3].setText(Ui.inteiro(l[3]) + " / " + Ui.inteiro(l[4]) + " / " + Ui.inteiro(l[5]));
            valores[4].setText(Ui.inteiro(l[6]) + " x " + Ui.inteiro(l[7]));

            simples(gVo2, Db.consultar("""
                    SELECT c.nome_categoria, ROUND(AVG(af.vo2_max), 1)
                    FROM AVALIACAOFISICA af
                    JOIN ATLETA a ON a.cod_pessoa = af.cod_pessoa
                    JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                    GROUP BY c.cod_categoria, c.nome_categoria
                    ORDER BY c.cod_categoria"""));

            Db.Resultado mando = Db.consultar("""
                    SELECT IF(mandante, 'Casa', 'Fora') AS mando,
                           SUM(gols_pro > gols_contra), SUM(gols_pro = gols_contra), SUM(gols_pro < gols_contra)
                    FROM JOGO
                    GROUP BY mando
                    ORDER BY mando""");
            List<String> cats = new ArrayList<>();
            double[][] v = new double[3][mando.linhas().size()];
            for (int i = 0; i < mando.linhas().size(); i++) {
                Object[] m = mando.linhas().get(i);
                cats.add(String.valueOf(m[0]));
                for (int s = 0; s < 3; s++) v[s][i] = Ui.decimal(m[s + 1]);
            }
            gMando.setDados(cats, List.of("Vitórias", "Empates", "Derrotas"), v, Ui.VERDE, Ui.AMARELO, Ui.VERMELHO);

            simples(gSetor, Db.consultar("""
                    SELECT po.setor, SUM(e.minutos_jogados)
                    FROM ESCALACAO e
                    JOIN POSICAO po ON po.cod_posicao = e.cod_posicao
                    GROUP BY po.setor
                    ORDER BY 2 DESC"""));

            simples(gTreino, Db.consultar("""
                    SELECT tipo_sessao, SUM(duracao_min)
                    FROM SESSAOTREINO
                    GROUP BY tipo_sessao
                    ORDER BY 2 DESC"""));
        } catch (SQLException e) {
            Ui.erro(this, e);
        }
    }

    /** Resultado com 2 colunas (rótulo, valor) vira um gráfico de uma série. */
    private static void simples(GraficoBarras g, Db.Resultado r) {
        List<String> cats = new ArrayList<>();
        double[][] v = new double[1][r.linhas().size()];
        for (int i = 0; i < r.linhas().size(); i++) {
            cats.add(String.valueOf(r.valor(i, 0)));
            v[0][i] = Ui.decimal(r.valor(i, 1));
        }
        g.setDados(cats, List.of(""), v, Ui.VERDE);
    }
}
