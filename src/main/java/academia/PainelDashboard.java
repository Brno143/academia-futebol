package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Etapa 06 - Dashboard estatístico integrado.
 * Tudo aqui é calculado a partir do banco, respeitando os filtros de categoria e período:
 * indicadores, medidas estatísticas (média, mediana, moda, variância, desvio padrão)
 * e 10 gráficos (linha, pizza, barras, histograma, dispersão e radar).
 */
public class PainelDashboard extends JPanel {

    /** Variáveis numéricas analisadas (nome mostrado -> índice usado nos mapas). */
    private static final String[] VARIAVEIS = {"VO2 máx", "% gordura", "IMC", "Peso (kg)", "Altura (cm)",
            "Idade na avaliação", "Gols pró por jogo", "Gols contra por jogo", "Minutos por escalação"};
    /** Variáveis que podem entrar no gráfico de dispersão (todas vêm da avaliação física). */
    private static final String[] VARIAVEIS_CORRELACAO = {"VO2 máx", "% gordura", "IMC", "Peso (kg)", "Altura (cm)", "Idade na avaliação"};

    private final BarraFiltros filtros = new BarraFiltros(this::carregar);
    private final JComboBox<String> variavel = new JComboBox<>(VARIAVEIS);
    private final JComboBox<String> corrX = new JComboBox<>(VARIAVEIS_CORRELACAO);
    private final JComboBox<String> corrY = new JComboBox<>(VARIAVEIS_CORRELACAO);
    private final JLabel resumoFiltro = new JLabel(" ");

    private final JLabel[] kAtletas, kJogos, kAprov, kGols, kSaldo, kPresenca, kVo2, kImc;
    private final JTable tabelaEstat = Ui.novaTabela();

    private final GraficoLinha gGols = new GraficoLinha("Tendência: gols pró x gols contra por jogo");
    private final GraficoLinha gAprovAcum = new GraficoLinha("Tendência: aproveitamento acumulado (%)");
    private final GraficoPizza gResultados = new GraficoPizza("Distribuição dos resultados");
    private final GraficoBarras gAprovCat = new GraficoBarras("Aproveitamento por categoria (%)");
    private final GraficoBarras gHistograma = new GraficoBarras("Histograma");
    private final GraficoDispersao gCorrelacao = new GraficoDispersao("Correlação");
    private final GraficoRadar gRadar = new GraficoRadar("Perfil das categorias (radar, % do maior valor)");
    private final GraficoBarras gPresenca = new GraficoBarras("Frequência nos treinos por categoria");
    private final GraficoPizza gSessoes = new GraficoPizza("Minutos de treino por tipo de sessão");
    private final GraficoBarras gSetor = new GraficoBarras("Minutos jogados por setor do campo");

    public PainelDashboard() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        corrY.setSelectedIndex(1);
        filtros.extra("Variável:", variavel);
        filtros.extra("Correlação:", corrX);
        filtros.extra("x", corrY);
        variavel.addActionListener(e -> { if (variavel.isFocusOwner()) carregar(); });
        corrX.addActionListener(e -> { if (corrX.isFocusOwner()) carregar(); });
        corrY.addActionListener(e -> { if (corrY.isFocusOwner()) carregar(); });

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(Ui.titulo("Dashboard da academia"), BorderLayout.WEST);
        resumoFiltro.setForeground(new Color(0x4A5A50));
        barra.add(resumoFiltro, BorderLayout.EAST);

        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.add(barra, BorderLayout.NORTH);
        topo.add(filtros, BorderLayout.CENTER);
        add(topo, BorderLayout.NORTH);

        // ---- conteúdo rolável ----
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        JPanel cards = new JPanel(new GridLayout(2, 4, 10, 10));
        kAtletas = Ui.cartao(cards, "Atletas");
        kJogos = Ui.cartao(cards, "Jogos no período");
        kAprov = Ui.cartao(cards, "Aproveitamento");
        kGols = Ui.cartao(cards, "Média de gols pró por jogo");
        kSaldo = Ui.cartao(cards, "Saldo de gols");
        kPresenca = Ui.cartao(cards, "Presença nos treinos");
        kVo2 = Ui.cartao(cards, "VO2 máx médio");
        kImc = Ui.cartao(cards, "IMC médio");
        conteudo.add(secao("Indicadores", cards));

        JScrollPane sEstat = new JScrollPane(tabelaEstat);
        sEstat.setPreferredSize(new Dimension(0, 250));
        conteudo.add(secao("Medidas estatísticas (tendência central e dispersão)", sEstat));

        JPanel grade = new JPanel(new GridLayout(0, 2, 12, 12));
        for (Grafico g : new Grafico[]{gGols, gAprovAcum, gResultados, gAprovCat, gHistograma,
                gCorrelacao, gRadar, gPresenca, gSessoes, gSetor}) grade.add(g);
        conteudo.add(secao("Gráficos (passe o mouse sobre barras, pontos e fatias para ver os valores)", grade));

        JScrollPane scroll = new JScrollPane(conteudo);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    private static JPanel secao(String titulo, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
        t.setForeground(new Color(0x1F5A34));
        p.add(t, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    public void carregar() {
        filtros.recarregarOpcoes();
        resumoFiltro.setText("Filtro: " + filtros.descricao());
        try {
            Map<String, List<Double>> dados = new LinkedHashMap<>();
            for (String v : VARIAVEIS) dados.put(v, new ArrayList<>());

            // ===== Jogos (view vw_resultado_jogos da Etapa 04) =====
            Db.Resultado jogos = Db.consultar("""
                    SELECT data_jogo, gols_pro, gols_contra, pontos, resultado, categoria
                      FROM vw_resultado_jogos
                     WHERE 1 = 1""" + filtros.sqlCategoria("cod_categoria") + filtros.sqlPeriodo("data_jogo")
                    + " ORDER BY data_jogo, cod_jogo",
                    Db.params(filtros.paramCategoria(), filtros.paramPeriodo()));
            indicadoresJogos(jogos, dados);

            // ===== Avaliações físicas =====
            Db.Resultado av = Db.consultar("""
                    SELECT p.nome, c.nome_categoria, af.vo2_max, af.percentual_gordura,
                           ROUND(af.peso / POWER(af.altura / 100, 2), 2) AS imc, af.peso, af.altura,
                           TIMESTAMPDIFF(YEAR, p.data_nasc, af.data_avaliacao) AS idade
                      FROM AVALIACAOFISICA af
                      JOIN ATLETA a    ON a.cod_pessoa = af.cod_pessoa
                      JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
                      JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                     WHERE 1 = 1""" + filtros.sqlCategoria("a.cod_categoria") + filtros.sqlPeriodo("af.data_avaliacao")
                    + " ORDER BY c.idade_min, p.nome",
                    Db.params(filtros.paramCategoria(), filtros.paramPeriodo()));
            for (Object[] l : av.linhas()) {
                for (int i = 0; i < 6; i++) if (l[i + 2] != null) dados.get(VARIAVEIS[i]).add(Ui.decimal(l[i + 2]));
            }
            Estatistica.Resumo rVo2 = Estatistica.resumo(dados.get("VO2 máx"));
            Estatistica.Resumo rImc = Estatistica.resumo(dados.get("IMC"));
            kVo2[0].setText(Estatistica.fmt(rVo2.media()));
            kVo2[1].setText("mediana " + Estatistica.fmt(rVo2.mediana()) + " · DP " + Estatistica.fmt(rVo2.desvio()));
            kImc[0].setText(Estatistica.fmt(rImc.media()));
            kImc[1].setText("mediana " + Estatistica.fmt(rImc.mediana()) + " · DP " + Estatistica.fmt(rImc.desvio()));
            correlacao(av);

            // ===== Minutos por escalação =====
            for (Object[] l : Db.consultar("""
                    SELECT e.minutos_jogados
                      FROM ESCALACAO e
                      JOIN JOGO j ON j.cod_jogo = e.cod_jogo
                     WHERE 1 = 1""" + filtros.sqlCategoria("j.cod_categoria") + filtros.sqlPeriodo("j.data_jogo"),
                    Db.params(filtros.paramCategoria(), filtros.paramPeriodo())).linhas())
                dados.get("Minutos por escalação").add(Ui.decimal(l[0]));

            // ===== Atletas =====
            Object[] at = Db.consultar("""
                    SELECT COUNT(*), ROUND(AVG(TIMESTAMPDIFF(YEAR, p.data_nasc, CURDATE())), 1)
                      FROM ATLETA a JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                     WHERE 1 = 1""" + filtros.sqlCategoria("a.cod_categoria"),
                    Db.params(filtros.paramCategoria())).linhas().get(0);
            kAtletas[0].setText(String.valueOf(Ui.inteiro(at[0])));
            kAtletas[1].setText("idade média " + Estatistica.fmt(Ui.decimal(at[1])) + " anos");

            tabelaEstatisticas(dados);
            histograma(dados);
            presenca();
            aproveitamentoPorCategoria();
            radar();

            gSessoes.setDados(Db.consultar("""
                    SELECT s.tipo_sessao, SUM(s.duracao_min)
                      FROM SESSAOTREINO s
                      JOIN TREINO t ON t.cod_treino = s.cod_treino
                     WHERE 1 = 1""" + filtros.sqlCategoria("t.cod_categoria") + filtros.sqlPeriodo("t.data_treino")
                    + " GROUP BY s.tipo_sessao ORDER BY 2 DESC",
                    Db.params(filtros.paramCategoria(), filtros.paramPeriodo())));

            gSetor.setDados(Db.consultar("""
                    SELECT po.setor, SUM(e.minutos_jogados)
                      FROM ESCALACAO e
                      JOIN POSICAO po ON po.cod_posicao = e.cod_posicao
                      JOIN JOGO j     ON j.cod_jogo = e.cod_jogo
                     WHERE 1 = 1""" + filtros.sqlCategoria("j.cod_categoria") + filtros.sqlPeriodo("j.data_jogo")
                    + " GROUP BY po.setor ORDER BY FIELD(po.setor, 'Defesa', 'Meio', 'Ataque')",
                    Db.params(filtros.paramCategoria(), filtros.paramPeriodo())));
            gSetor.setSufixo(" min");
        } catch (SQLException e) {
            Ui.erro(this, e);
        }
    }

    /** Cartões de jogos, linhas de tendência, pizza de resultados e variáveis de gols. */
    private void indicadoresJogos(Db.Resultado jogos, Map<String, List<Double>> dados) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        int n = jogos.linhas().size(), pontos = 0, gp = 0, gc = 0, v = 0, e = 0, d = 0;
        List<String> datas = new ArrayList<>();
        double[][] gols = new double[2][n];
        double[][] acum = new double[1][n];
        for (int i = 0; i < n; i++) {
            Object[] l = jogos.linhas().get(i);
            int pro = Ui.inteiro(l[1]), contra = Ui.inteiro(l[2]), pts = Ui.inteiro(l[3]);
            datas.add(f.format(Ui.paraLocalDate(l[0])));
            gols[0][i] = pro; gols[1][i] = contra;
            pontos += pts; gp += pro; gc += contra;
            acum[0][i] = Math.round(1000.0 * pontos / (3 * (i + 1))) / 10.0;
            String res = String.valueOf(l[4]);
            if (res.equals("Vitória")) v++; else if (res.equals("Empate")) e++; else d++;
            dados.get("Gols pró por jogo").add((double) pro);
            dados.get("Gols contra por jogo").add((double) contra);
        }
        kJogos[0].setText(String.valueOf(n));
        kJogos[1].setText(v + " V · " + e + " E · " + d + " D");
        if (n == 0) {
            kAprov[0].setText("-"); kAprov[1].setText(" ");
            kGols[0].setText("-"); kGols[1].setText(" ");
            kSaldo[0].setText("-"); kSaldo[1].setText(" ");
        } else {
            double aprov = 100.0 * pontos / (3 * n);
            kAprov[0].setText(String.format("%.1f%%", aprov));
            // tendência: compara a 1ª metade dos jogos do período com a 2ª metade
            int meio = n / 2, p1 = 0, p2 = 0;
            for (int i = 0; i < n; i++) if (i < meio) p1 += Ui.inteiro(jogos.valor(i, 3)); else p2 += Ui.inteiro(jogos.valor(i, 3));
            if (meio > 0) {
                double a1 = 100.0 * p1 / (3 * meio), a2 = 100.0 * p2 / (3 * (n - meio));
                double dif = a2 - a1;
                kAprov[1].setText(String.format("%s %+.1f p.p. na 2ª metade do período", dif >= 0 ? "▲" : "▼", dif));
                kAprov[0].setForeground(dif >= 0 ? Ui.VERDE : Ui.VERMELHO);
            } else kAprov[1].setText(pontos + " pontos de " + 3 * n);
            kGols[0].setText(String.format("%.2f", gp / (double) n));
            kGols[1].setText(String.format("sofridos: %.2f por jogo", gc / (double) n));
            kSaldo[0].setText((gp - gc >= 0 ? "+" : "") + (gp - gc));
            kSaldo[1].setText(gp + " marcados x " + gc + " sofridos");
        }
        gGols.setDados(datas, List.of("Gols pró", "Gols contra"), gols, Ui.VERDE, Ui.VERMELHO);
        gAprovAcum.setMaximo(100.0);
        gAprovAcum.setSufixo("%");
        gAprovAcum.setDados(datas, List.of(""), acum, Ui.AZUL);
        gResultados.setDados(List.of("Vitórias", "Empates", "Derrotas"), new double[]{v, e, d}, Ui.VERDE, Ui.AMARELO, Ui.VERMELHO);
    }

    private void tabelaEstatisticas(Map<String, List<Double>> dados) {
        String[] col = {"Variável", "n", "Média", "Mediana", "Moda", "Variância", "Desvio padrão", "CV (%)", "Mínimo", "Máximo"};
        List<Object[]> linhas = new ArrayList<>();
        for (var e : dados.entrySet()) {
            Estatistica.Resumo r = Estatistica.resumo(e.getValue());
            linhas.add(new Object[]{e.getKey(), r.n(), Estatistica.fmt(r.media()), Estatistica.fmt(r.mediana()), r.moda(),
                    Estatistica.fmt(r.variancia()), Estatistica.fmt(r.desvio()), Estatistica.fmt(r.cv()),
                    Estatistica.fmt(r.min()), Estatistica.fmt(r.max())});
        }
        Ui.preencherTabela(tabelaEstat, new Db.Resultado(List.of(col), linhas));
        tabelaEstat.getColumnModel().getColumn(0).setPreferredWidth(170);
        tabelaEstat.getColumnModel().getColumn(4).setPreferredWidth(140);
    }

    private void histograma(Map<String, List<Double>> dados) {
        String nome = (String) variavel.getSelectedItem();
        List<Double> v = dados.get(nome);
        Estatistica.Classes cl = Estatistica.classes(v);
        Estatistica.Resumo r = Estatistica.resumo(v);
        gHistograma.setTitulo("Histograma de " + nome + " (frequência por classe)");
        gHistograma.setDados(cl.rotulos(), List.of("Frequência"), new double[][]{cl.freq()}, Ui.AZUL);
        gHistograma.setSufixo(" registro(s)");
        gHistograma.setSubtitulo("n = " + r.n() + " · média " + Estatistica.fmt(r.media()) + " · mediana "
                + Estatistica.fmt(r.mediana()) + " · moda " + r.moda() + " · DP " + Estatistica.fmt(r.desvio()));
    }

    private void correlacao(Db.Resultado av) {
        int ix = corrX.getSelectedIndex() + 2, iy = corrY.getSelectedIndex() + 2;
        List<Double> x = new ArrayList<>(), y = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        List<Integer> grupos = new ArrayList<>();
        List<String> nomesGrupos = new ArrayList<>();
        for (Object[] l : av.linhas()) {
            if (l[ix] == null || l[iy] == null) continue;
            String cat = String.valueOf(l[1]);
            if (!nomesGrupos.contains(cat)) nomesGrupos.add(cat);
            x.add(Ui.decimal(l[ix])); y.add(Ui.decimal(l[iy]));
            nomes.add(l[0] + " (" + cat + ")");
            grupos.add(nomesGrupos.indexOf(cat));
        }
        String rx = (String) corrX.getSelectedItem(), ry = (String) corrY.getSelectedItem();
        double r = Estatistica.pearson(x, y);
        double[] reta = Estatistica.regressao(x, y);
        gCorrelacao.setTitulo("Correlação: " + rx + " x " + ry);
        gCorrelacao.setSubtitulo(Double.isNaN(r) ? "dados insuficientes"
                : String.format("r de Pearson = %.3f (%s) · r² = %.3f · reta: y = %.2f %+.2f·x",
                r, Estatistica.forca(r), r * r, reta[0], reta[1]));
        gCorrelacao.setDados(x, y, nomes, grupos, nomesGrupos, rx, ry);
    }

    private void presenca() throws SQLException {
        Db.Resultado r = Db.consultar("""
                SELECT c.nome_categoria, SUM(pa.presente), SUM(NOT pa.presente)
                  FROM PARTICIPA pa
                  JOIN TREINO t    ON t.cod_treino = pa.cod_treino
                  JOIN CATEGORIA c ON c.cod_categoria = t.cod_categoria
                 WHERE 1 = 1""" + filtros.sqlCategoria("t.cod_categoria") + filtros.sqlPeriodo("t.data_treino")
                + " GROUP BY c.cod_categoria, c.nome_categoria, c.idade_min ORDER BY c.idade_min",
                Db.params(filtros.paramCategoria(), filtros.paramPeriodo()));
        List<String> cats = new ArrayList<>();
        double[][] v = new double[2][r.linhas().size()];
        double pres = 0, tot = 0;
        for (int i = 0; i < r.linhas().size(); i++) {
            cats.add(String.valueOf(r.valor(i, 0)));
            v[0][i] = Ui.decimal(r.valor(i, 1));
            v[1][i] = Ui.decimal(r.valor(i, 2));
            pres += v[0][i]; tot += v[0][i] + v[1][i];
        }
        gPresenca.setDados(cats, List.of("Presenças", "Faltas"), v, Ui.VERDE, Ui.VERMELHO);
        kPresenca[0].setText(tot == 0 ? "-" : String.format("%.1f%%", 100 * pres / tot));
        kPresenca[1].setText(tot == 0 ? " " : (int) pres + " presenças em " + (int) tot + " registros");
    }

    private void aproveitamentoPorCategoria() throws SQLException {
        // compara as categorias entre si, por isso usa só o filtro de período
        Db.Resultado r = Db.consultar("""
                SELECT categoria, ROUND(100 * SUM(pontos) / (3 * COUNT(*)), 1)
                  FROM vw_resultado_jogos
                 WHERE 1 = 1""" + filtros.sqlPeriodo("data_jogo")
                + " GROUP BY cod_categoria, categoria ORDER BY cod_categoria",
                Db.params(filtros.paramPeriodo()));
        gAprovCat.setDados(r);
        gAprovCat.setSufixo("%");
        List<Double> vals = new ArrayList<>();
        for (Object[] l : r.linhas()) vals.add(Ui.decimal(l[1]));
        if (!vals.isEmpty()) {
            gAprovCat.referencia("média", Math.round(Estatistica.media(vals) * 10) / 10.0);
            gAprovCat.setSubtitulo("Compara todas as categorias no período · desvio padrão entre categorias: "
                    + Estatistica.fmt(Math.sqrt(Estatistica.variancia(vals))) + " p.p.");
        }
    }

    /** Radar: médias de cada categoria em 6 dimensões, normalizadas pelo maior valor de cada eixo. */
    private void radar() throws SQLException {
        List<Object> per = filtros.paramPeriodo();
        Db.Resultado r = Db.consultar("""
                SELECT c.nome_categoria,
                       (SELECT AVG(af.vo2_max) FROM AVALIACAOFISICA af JOIN ATLETA a ON a.cod_pessoa = af.cod_pessoa
                         WHERE a.cod_categoria = c.cod_categoria AND af.data_avaliacao BETWEEN ? AND ?) AS vo2,
                       (SELECT AVG(af.percentual_gordura) FROM AVALIACAOFISICA af JOIN ATLETA a ON a.cod_pessoa = af.cod_pessoa
                         WHERE a.cod_categoria = c.cod_categoria AND af.data_avaliacao BETWEEN ? AND ?) AS gordura,
                       (SELECT AVG(af.peso / POWER(af.altura / 100, 2)) FROM AVALIACAOFISICA af JOIN ATLETA a ON a.cod_pessoa = af.cod_pessoa
                         WHERE a.cod_categoria = c.cod_categoria AND af.data_avaliacao BETWEEN ? AND ?) AS imc,
                       (SELECT 100 * AVG(pa.presente) FROM PARTICIPA pa JOIN TREINO t ON t.cod_treino = pa.cod_treino
                         WHERE t.cod_categoria = c.cod_categoria AND t.data_treino BETWEEN ? AND ?) AS presenca,
                       (SELECT 100 * SUM(pontos) / (3 * COUNT(*)) FROM vw_resultado_jogos v
                         WHERE v.cod_categoria = c.cod_categoria AND v.data_jogo BETWEEN ? AND ?) AS aproveitamento,
                       (SELECT AVG(gols_pro) FROM JOGO j
                         WHERE j.cod_categoria = c.cod_categoria AND j.data_jogo BETWEEN ? AND ?) AS gols
                  FROM CATEGORIA c
                 ORDER BY c.idade_min""",
                Db.params(per, per, per, per, per, per));
        List<String> eixos = List.of("VO2 máx", "% gordura", "IMC", "Presença %", "Aproveitamento %", "Gols pró/jogo");
        int nEixos = eixos.size(), nCat = r.linhas().size();
        double[] max = new double[nEixos];
        double[] soma = new double[nEixos];
        for (Object[] l : r.linhas())
            for (int k = 0; k < nEixos; k++) { max[k] = Math.max(max[k], Ui.decimal(l[k + 1])); soma[k] += Ui.decimal(l[k + 1]); }

        List<String> series = new ArrayList<>();
        List<double[]> brutos = new ArrayList<>();
        Integer cod = filtros.codCategoria();
        for (Object[] l : r.linhas()) {
            if (cod != null && !String.valueOf(l[0]).equals(filtros.nomeCategoria())) continue;
            series.add(String.valueOf(l[0]));
            double[] b = new double[nEixos];
            for (int k = 0; k < nEixos; k++) b[k] = Ui.decimal(l[k + 1]);
            brutos.add(b);
        }
        if (cod != null && nCat > 0) {
            series.add("Média das categorias");
            double[] b = new double[nEixos];
            for (int k = 0; k < nEixos; k++) b[k] = soma[k] / nCat;
            brutos.add(b);
        }
        double[][] norm = new double[series.size()][nEixos];
        String[][] txt = new String[series.size()][nEixos];
        for (int s = 0; s < series.size(); s++)
            for (int k = 0; k < nEixos; k++) {
                norm[s][k] = max[k] == 0 ? 0 : 100 * brutos.get(s)[k] / max[k];
                txt[s][k] = Estatistica.fmt(Math.round(brutos.get(s)[k] * 100) / 100.0);
            }
        gRadar.setDados(eixos, series, norm, txt);
    }
}
