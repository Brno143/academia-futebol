package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Etapa 06 - Views e consultas avançadas da Etapa 04, acessíveis na interface.
 * Cada item tem: SQL (mostrado na tela), filtros que se aplicam, 4 indicadores e um gráfico.
 */
public class PainelViews extends JPanel {

    /** Um item da lista (view ou consulta). */
    private abstract class Item {
        final String titulo, descricao;
        final boolean usaCategoria, usaPeriodo;
        Item(String titulo, String descricao, boolean usaCategoria, boolean usaPeriodo) {
            this.titulo = titulo; this.descricao = descricao; this.usaCategoria = usaCategoria; this.usaPeriodo = usaPeriodo;
        }
        @Override public String toString() { return titulo; }
        /** SQL com os "?" já no lugar dos filtros. */
        abstract String sql();
        abstract Object[] params();
        /** Preenche indicadores e gráfico a partir do resultado. */
        abstract void visual(Db.Resultado r);
    }

    private final BarraFiltros filtros = new BarraFiltros(this::executar);
    private final JComboBox<String> resultado = new JComboBox<>(new String[]{"Todos", "Vitória", "Empate", "Derrota"});
    private final JList<Item> lista;
    private final JLabel titulo = new JLabel();
    private final JLabel descricao = new JLabel();
    private final JTextArea sql = new JTextArea();
    private final JTable tabela = Ui.novaTabela();
    private final JLabel total = new JLabel();
    private final JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
    private final JLabel[][] k = new JLabel[4][];
    private final JLabel[] kRotulo = new JLabel[4];
    private final JPanel areaGrafico = new JPanel(new BorderLayout());

    public PainelViews() {
        super(new BorderLayout(12, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        filtros.extra("Resultado (view de jogos):", resultado);
        resultado.addActionListener(e -> { if (resultado.isFocusOwner()) executar(); });

        lista = new JList<>(itens().toArray(new Item[0]));
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFixedCellHeight(30);
        lista.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) executar(); });
        JScrollPane esquerda = new JScrollPane(lista);
        esquerda.setPreferredSize(new Dimension(330, 0));
        esquerda.setBorder(BorderFactory.createTitledBorder("Views e consultas (Etapa 04)"));

        for (int i = 0; i < 4; i++) {
            k[i] = Ui.cartao(cards, "");
            kRotulo[i] = (JLabel) ((JPanel) cards.getComponent(i)).getComponent(0);
        }

        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        descricao.setForeground(new Color(0x4A5A50));
        sql.setEditable(false);
        sql.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sql.setBackground(new Color(0xF6F8F6));
        JScrollPane sqlScroll = new JScrollPane(sql);
        sqlScroll.setBorder(BorderFactory.createTitledBorder("SQL executado"));

        JPanel cab = new JPanel(new GridLayout(0, 1, 0, 2));
        cab.add(titulo);
        cab.add(descricao);

        JPanel topoDir = new JPanel(new BorderLayout(0, 8));
        topoDir.add(cab, BorderLayout.NORTH);
        topoDir.add(cards, BorderLayout.CENTER);

        JTextField busca = Ui.filtroTabela(tabela);
        JPanel rodape = new JPanel(new BorderLayout());
        JPanel pBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pBusca.add(new JLabel("Buscar na tabela:"));
        pBusca.add(busca);
        rodape.add(pBusca, BorderLayout.WEST);
        rodape.add(total, BorderLayout.EAST);

        JPanel pTabela = new JPanel(new BorderLayout(0, 4));
        pTabela.add(new JScrollPane(tabela), BorderLayout.CENTER);
        pTabela.add(rodape, BorderLayout.SOUTH);

        JSplitPane baixo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pTabela, areaGrafico);
        baixo.setResizeWeight(0.55);
        JSplitPane meio = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlScroll, baixo);
        meio.setResizeWeight(0.25);

        JPanel direita = new JPanel(new BorderLayout(0, 8));
        direita.add(topoDir, BorderLayout.NORTH);
        direita.add(meio, BorderLayout.CENTER);

        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.add(Ui.titulo("Views e consultas avançadas"), BorderLayout.NORTH);
        topo.add(filtros, BorderLayout.CENTER);

        add(topo, BorderLayout.NORTH);
        add(esquerda, BorderLayout.WEST);
        add(direita, BorderLayout.CENTER);
    }

    public void carregar() {
        filtros.recarregarOpcoes();
        if (lista.getSelectedIndex() < 0) lista.setSelectedIndex(0); else executar();
    }

    private void executar() {
        Item it = lista.getSelectedValue();
        if (it == null) return;
        filtros.usarCategoria(it.usaCategoria);
        filtros.usarPeriodo(it.usaPeriodo);
        resultado.setEnabled(it.titulo.startsWith("View 2"));
        titulo.setText(it.titulo);
        descricao.setText("<html><i>Filtros aplicados: "
                + (it.usaCategoria ? (filtros.nomeCategoria() == null ? "todas as categorias" : filtros.nomeCategoria()) : "—")
                + (it.usaPeriodo ? " · período " + filtros.descricao().replaceFirst("^.*· ", "") : "") + "</i></html>");
        sql.setText(it.sql().strip());
        sql.setCaretPosition(0);
        for (int i = 0; i < 4; i++) { kRotulo[i].setText(" "); k[i][0].setText("-"); k[i][1].setText(" "); }
        try {
            Db.Resultado r = Db.consultar(it.sql(), it.params());
            Ui.preencherTabela(tabela, r);
            total.setText(r.linhas().size() + " linha(s)");
            it.visual(r);
        } catch (SQLException e) {
            Ui.erro(this, e);
        }
        areaGrafico.revalidate();
        areaGrafico.repaint();
    }

    // ===================== utilidades dos itens =====================

    private void card(int i, String rotulo, String valor, String detalhe) {
        kRotulo[i].setText(rotulo);
        k[i][0].setText(valor);
        k[i][1].setText(detalhe == null ? " " : detalhe);
    }

    private void mostrar(Grafico g) {
        areaGrafico.removeAll();
        areaGrafico.add(g, BorderLayout.CENTER);
    }

    private List<Double> coluna(Db.Resultado r, String nome) {
        int c = r.colunas().indexOf(nome);
        List<Double> v = new ArrayList<>();
        for (Object[] l : r.linhas()) if (l[c] != null) v.add(Ui.decimal(l[c]));
        return v;
    }

    /** Conta quantas linhas têm cada valor numa coluna de texto (em ordem de aparição). */
    private Map<String, Integer> contar(Db.Resultado r, String nome) {
        int c = r.colunas().indexOf(nome);
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Object[] l : r.linhas()) m.merge(String.valueOf(l[c]), 1, Integer::sum);
        return m;
    }

    /** Média de "valor" agrupada por "grupo" (mantém a ordem de aparição). */
    private Db.Resultado mediaPor(Db.Resultado r, String grupo, String valor) {
        int g = r.colunas().indexOf(grupo), v = r.colunas().indexOf(valor);
        Map<String, double[]> m = new LinkedHashMap<>();
        for (Object[] l : r.linhas()) {
            if (l[v] == null) continue;
            double[] s = m.computeIfAbsent(String.valueOf(l[g]), x -> new double[2]);
            s[0] += Ui.decimal(l[v]); s[1]++;
        }
        List<Object[]> linhas = new ArrayList<>();
        for (var e : m.entrySet()) linhas.add(new Object[]{e.getKey(), Math.round(10 * e.getValue()[0] / e.getValue()[1]) / 10.0});
        return new Db.Resultado(List.of(grupo, valor), linhas);
    }

    private static String primeiroNome(Object nome) {
        String s = String.valueOf(nome);
        int i = s.indexOf(' ');
        return i > 0 ? s.substring(0, i) : s;
    }

    private String filtroCatNome(String coluna) { return filtros.nomeCategoria() == null ? "" : "\n   AND " + coluna + " = ?"; }

    private List<Object> paramCatNome() {
        return filtros.nomeCategoria() == null ? List.of() : List.of(filtros.nomeCategoria());
    }

    // ===================== os itens =====================

    private List<Item> itens() {
        List<Item> l = new ArrayList<>();

        l.add(new Item("View 1 · vw_ficha_atleta",
                "Ficha completa de cada atleta: categoria, reserva de quem, última avaliação física, frequência e jogos.",
                true, false) {
            String sql() {
                return """
                        SELECT atleta, categoria, camisa, idade, reserva_de, vo2_max, imc,
                               pct_presenca, jogos, minutos_jogados, cartoes_amarelos
                          FROM vw_ficha_atleta
                         WHERE 1 = 1""" + filtroCatNome("categoria") + "\n ORDER BY cod_categoria, camisa";
            }
            Object[] params() { return Db.params(paramCatNome()); }
            void visual(Db.Resultado r) {
                Estatistica.Resumo vo2 = Estatistica.resumo(coluna(r, "vo2_max"));
                Estatistica.Resumo pres = Estatistica.resumo(coluna(r, "pct_presenca"));
                double min = coluna(r, "minutos_jogados").stream().mapToDouble(d -> d).sum();
                card(0, "Atletas", String.valueOf(r.linhas().size()), null);
                card(1, "VO2 máx médio", Estatistica.fmt(vo2.media()), "DP " + Estatistica.fmt(vo2.desvio()));
                card(2, "Presença média", Estatistica.fmt(pres.media()) + "%", "mediana " + Estatistica.fmt(pres.mediana()) + "%");
                card(3, "Minutos jogados", Estatistica.fmt(min), null);
                GraficoBarras g = new GraficoBarras("");
                if (filtros.nomeCategoria() == null) {
                    g.setTitulo("Presença média (%) por categoria");
                    g.setDados(mediaPor(r, "categoria", "pct_presenca"));
                } else {
                    g.setTitulo("Presença (%) por atleta — " + filtros.nomeCategoria());
                    List<Object[]> lin = new ArrayList<>();
                    for (Object[] x : r.linhas()) lin.add(new Object[]{primeiroNome(x[0]), x[7]});
                    g.setDados(new Db.Resultado(List.of("a", "b"), lin));
                }
                g.setSufixo("%");
                if (pres.n() > 0) g.referencia("média", Math.round(pres.media() * 10) / 10.0);
                mostrar(g);
            }
        });

        l.add(new Item("View 2 · vw_resultado_jogos",
                "Súmula de cada partida: placar, resultado, saldo, pontos, atletas escalados e cartões.",
                true, true) {
            String sql() {
                return """
                        SELECT cod_jogo, DATE_FORMAT(data_jogo, '%d/%m/%Y') AS data, categoria, adversario,
                               mando, gols_pro, gols_contra, saldo, resultado, pontos,
                               atletas_escalados, cartoes_amarelos
                          FROM vw_resultado_jogos
                         WHERE data_jogo BETWEEN ? AND ?""" + filtroCatNome("categoria")
                        + (resultado.getSelectedIndex() > 0 ? "\n   AND resultado = ?" : "") + "\n ORDER BY data_jogo";
            }
            Object[] params() {
                return Db.params(filtros.paramPeriodo(), paramCatNome(),
                        resultado.getSelectedIndex() > 0 ? List.of(resultado.getSelectedItem()) : List.of());
            }
            void visual(Db.Resultado r) {
                int n = r.linhas().size();
                double pts = coluna(r, "pontos").stream().mapToDouble(d -> d).sum();
                double saldo = coluna(r, "saldo").stream().mapToDouble(d -> d).sum();
                Estatistica.Resumo gp = Estatistica.resumo(coluna(r, "gols_pro"));
                card(0, "Jogos", String.valueOf(n), null);
                card(1, "Pontos", Estatistica.fmt(pts), n == 0 ? null : String.format("aproveitamento %.1f%%", 100 * pts / (3 * n)));
                card(2, "Saldo de gols", (saldo >= 0 ? "+" : "") + Estatistica.fmt(saldo), null);
                card(3, "Gols pró por jogo", Estatistica.fmt(gp.media()), "moda " + gp.moda());
                Map<String, Integer> res = contar(r, "resultado");
                List<String> rot = new ArrayList<>();
                List<Color> cores = new ArrayList<>();
                List<Double> val = new ArrayList<>();
                for (String s : List.of("Vitória", "Empate", "Derrota"))
                    if (res.containsKey(s)) {
                        rot.add(s); val.add((double) res.get(s));
                        cores.add(s.equals("Vitória") ? Ui.VERDE : s.equals("Empate") ? Ui.AMARELO : Ui.VERMELHO);
                    }
                GraficoPizza g = new GraficoPizza("Resultados dos jogos filtrados");
                g.setDados(rot, val.stream().mapToDouble(d -> d).toArray(), cores.toArray(new Color[0]));
                mostrar(g);
            }
        });

        l.add(new Item("View 2 (uso) · Aproveitamento por categoria",
                "Exemplo de uso da vw_resultado_jogos: pontos ganhos / pontos possíveis por categoria no período.",
                false, true) {
            String sql() {
                return """
                        SELECT categoria,
                               COUNT(*)    AS jogos,
                               SUM(pontos) AS pontos,
                               ROUND(100 * SUM(pontos) / (3 * COUNT(*)), 1) AS aproveitamento_pct
                          FROM vw_resultado_jogos
                         WHERE data_jogo BETWEEN ? AND ?
                         GROUP BY cod_categoria, categoria
                         ORDER BY aproveitamento_pct DESC""";
            }
            Object[] params() { return Db.params(filtros.paramPeriodo()); }
            void visual(Db.Resultado r) {
                List<Double> ap = coluna(r, "aproveitamento_pct");
                Estatistica.Resumo s = Estatistica.resumo(ap);
                card(0, "Categorias", String.valueOf(r.linhas().size()), null);
                card(1, "Melhor", r.vazio() ? "-" : String.valueOf(r.valor(0, 0)), r.vazio() ? null : r.valor(0, 3) + "%");
                card(2, "Pior", r.vazio() ? "-" : String.valueOf(r.valor(r.linhas().size() - 1, 0)),
                        r.vazio() ? null : r.valor(r.linhas().size() - 1, 3) + "%");
                card(3, "Média entre categorias", Estatistica.fmt(s.media()) + "%", "DP " + Estatistica.fmt(s.desvio()) + " p.p.");
                GraficoBarras g = new GraficoBarras("Aproveitamento (%) por categoria");
                List<Object[]> lin = new ArrayList<>();
                for (Object[] x : r.linhas()) lin.add(new Object[]{x[0], x[3]});
                g.setDados(new Db.Resultado(List.of("a", "b"), lin));
                g.setSufixo("%");
                if (s.n() > 0) g.referencia("média", Math.round(s.media() * 10) / 10.0);
                mostrar(g);
            }
        });

        l.add(new Item("Consulta 1 · Anti join: treinos sem sessões",
                "Treinos cadastrados que ainda não têm nenhuma sessão planejada (LEFT JOIN ... IS NULL).",
                true, true) {
            String sql() {
                return """
                        SELECT t.cod_treino, t.data_treino, t.turno, t.local, c.nome_categoria AS categoria
                          FROM TREINO t
                          JOIN CATEGORIA c ON c.cod_categoria = t.cod_categoria
                          LEFT JOIN SESSAOTREINO s ON s.cod_treino = t.cod_treino
                         WHERE s.cod_treino IS NULL
                           AND t.data_treino BETWEEN ? AND ?""" + filtroCatNome("c.nome_categoria")
                        + "\n ORDER BY t.data_treino, c.idade_min";
            }
            Object[] params() { return Db.params(filtros.paramPeriodo(), paramCatNome()); }
            void visual(Db.Resultado r) {
                int totalTreinos;
                try {
                    totalTreinos = Ui.inteiro(Db.consultar("SELECT COUNT(*) FROM TREINO t WHERE t.data_treino BETWEEN ? AND ?"
                                    + (filtros.codCategoria() == null ? "" : " AND t.cod_categoria = ?"),
                            Db.params(filtros.paramPeriodo(), filtros.paramCategoria())).valor(0, 0));
                } catch (SQLException e) { totalTreinos = 0; }
                int n = r.linhas().size();
                card(0, "Treinos sem sessão", String.valueOf(n), null);
                card(1, "Treinos no filtro", String.valueOf(totalTreinos), null);
                card(2, "% sem planejamento", totalTreinos == 0 ? "-" : String.format("%.1f%%", 100.0 * n / totalTreinos), null);
                card(3, "Com planejamento", String.valueOf(totalTreinos - n), null);
                GraficoPizza g = new GraficoPizza("Treinos com e sem sessões planejadas");
                g.setDados(List.of("Com sessões", "Sem sessões"), new double[]{totalTreinos - n, n}, Ui.VERDE, Ui.VERMELHO);
                mostrar(g);
            }
        });

        l.add(new Item("Consulta 2 · Full outer join: relatório disciplinar",
                "Faltas em treino e cartões em jogo: quem tem só falta, só cartão ou os dois (LEFT + RIGHT + UNION).",
                true, false) {
            String sql() {
                return """
                        WITH faltas AS (
                            SELECT cod_pessoa, COUNT(*) AS qtd_faltas
                              FROM PARTICIPA
                             WHERE presente = FALSE
                             GROUP BY cod_pessoa
                        ),
                        cartoes AS (
                            SELECT cod_pessoa, SUM(c_amarelo) AS amarelos, SUM(c_vermelho) AS vermelhos
                              FROM ESCALACAO
                             WHERE c_amarelo = TRUE OR c_vermelho = TRUE
                             GROUP BY cod_pessoa
                        ),
                        disciplina AS (
                            SELECT f.cod_pessoa AS cod_pessoa, f.qtd_faltas, ca.amarelos, ca.vermelhos
                              FROM faltas f LEFT JOIN cartoes ca ON ca.cod_pessoa = f.cod_pessoa
                            UNION
                            SELECT ca.cod_pessoa AS cod_pessoa, f.qtd_faltas, ca.amarelos, ca.vermelhos
                              FROM faltas f RIGHT JOIN cartoes ca ON ca.cod_pessoa = f.cod_pessoa
                        )
                        SELECT p.nome AS atleta, c.nome_categoria AS categoria,
                               COALESCE(d.qtd_faltas, 0) AS faltas,
                               COALESCE(d.amarelos, 0)   AS cartoes_amarelos,
                               COALESCE(d.vermelhos, 0)  AS cartoes_vermelhos,
                               CASE
                                   WHEN d.qtd_faltas IS NOT NULL AND d.amarelos IS NOT NULL THEN 'Falta e cartão'
                                   WHEN d.qtd_faltas IS NOT NULL THEN 'Só falta'
                                   ELSE 'Só cartão'
                               END AS situacao
                          FROM disciplina d
                          JOIN ATLETA a    ON a.cod_pessoa = d.cod_pessoa
                          JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
                          JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                         WHERE 1 = 1""" + filtroCatNome("c.nome_categoria") + "\n ORDER BY situacao, c.idade_min, p.nome";
            }
            Object[] params() { return Db.params(paramCatNome()); }
            void visual(Db.Resultado r) {
                Map<String, Integer> s = contar(r, "situacao");
                card(0, "Atletas no relatório", String.valueOf(r.linhas().size()), null);
                card(1, "Só falta", String.valueOf(s.getOrDefault("Só falta", 0)), null);
                card(2, "Só cartão", String.valueOf(s.getOrDefault("Só cartão", 0)), null);
                card(3, "Falta e cartão", String.valueOf(s.getOrDefault("Falta e cartão", 0)), null);
                GraficoPizza g = new GraficoPizza("Situação disciplinar");
                g.setDados(new ArrayList<>(s.keySet()), s.values().stream().mapToDouble(i -> i).toArray());
                mostrar(g);
            }
        });

        l.add(new Item("Consulta 3 · Subconsulta ANY: candidatos a subir",
                "Atletas com VO2 máx maior que o de pelo menos um atleta da categoria de cima (> ANY).",
                true, false) {
            String sql() {
                return """
                        SELECT c.nome_categoria AS categoria_atual, p.nome AS atleta, af.vo2_max,
                               (SELECT c3.nome_categoria FROM CATEGORIA c3
                                 WHERE c3.idade_min > c.idade_min ORDER BY c3.idade_min LIMIT 1) AS categoria_acima
                          FROM AVALIACAOFISICA af
                          JOIN ATLETA a    ON a.cod_pessoa = af.cod_pessoa
                          JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
                          JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                         WHERE af.vo2_max > ANY (
                                 SELECT af2.vo2_max
                                   FROM AVALIACAOFISICA af2
                                   JOIN ATLETA a2 ON a2.cod_pessoa = af2.cod_pessoa
                                  WHERE a2.cod_categoria = (SELECT c2.cod_categoria FROM CATEGORIA c2
                                                             WHERE c2.idade_min > c.idade_min
                                                             ORDER BY c2.idade_min LIMIT 1))""" + filtroCatNome("c.nome_categoria")
                        + "\n ORDER BY c.idade_min, af.vo2_max DESC";
            }
            Object[] params() { return Db.params(paramCatNome()); }
            void visual(Db.Resultado r) {
                Estatistica.Resumo s = Estatistica.resumo(coluna(r, "vo2_max"));
                card(0, "Candidatos", String.valueOf(r.linhas().size()), null);
                card(1, "VO2 máx médio", Estatistica.fmt(s.media()), "DP " + Estatistica.fmt(s.desvio()));
                card(2, "Maior VO2", Estatistica.fmt(s.max()), null);
                card(3, "Categorias com candidatos", String.valueOf(contar(r, "categoria_atual").size()), null);
                GraficoBarras g = new GraficoBarras("");
                if (filtros.nomeCategoria() == null) {
                    g.setTitulo("Candidatos a subir por categoria");
                    List<Object[]> lin = new ArrayList<>();
                    for (var e : contar(r, "categoria_atual").entrySet()) lin.add(new Object[]{e.getKey(), e.getValue()});
                    g.setDados(new Db.Resultado(List.of("a", "b"), lin));
                    g.setSufixo(" atleta(s)");
                } else {
                    g.setTitulo("VO2 máx dos candidatos — " + filtros.nomeCategoria());
                    List<Object[]> lin = new ArrayList<>();
                    for (Object[] x : r.linhas()) lin.add(new Object[]{primeiroNome(x[1]), x[2]});
                    g.setDados(new Db.Resultado(List.of("a", "b"), lin));
                }
                mostrar(g);
            }
        });

        l.add(new Item("Consulta 4 · Subconsultas no SELECT: raio-x",
                "Uma linha por categoria: elenco, comissão, atletas por membro, treinos, jogos e faltas.",
                true, false) {
            String sql() {
                return """
                        SELECT c.nome_categoria AS categoria,
                               (SELECT COUNT(*) FROM ATLETA a WHERE a.cod_categoria = c.cod_categoria) AS atletas,
                               (SELECT COUNT(*) FROM ORIENTA o WHERE o.cod_categoria = c.cod_categoria) AS membros_comissao,
                               ROUND((SELECT COUNT(*) FROM ATLETA a WHERE a.cod_categoria = c.cod_categoria)
                                   / NULLIF((SELECT COUNT(*) FROM ORIENTA o WHERE o.cod_categoria = c.cod_categoria), 0), 1)
                                                                                                       AS atletas_por_membro,
                               (SELECT COUNT(*) FROM TREINO t WHERE t.cod_categoria = c.cod_categoria) AS treinos,
                               (SELECT COUNT(*) FROM JOGO j WHERE j.cod_categoria = c.cod_categoria)   AS jogos,
                               (SELECT COUNT(*) FROM PARTICIPA pa JOIN ATLETA a ON a.cod_pessoa = pa.cod_pessoa
                                 WHERE a.cod_categoria = c.cod_categoria AND pa.presente = FALSE)       AS faltas_em_treino
                          FROM CATEGORIA c
                         WHERE 1 = 1""" + filtroCatNome("c.nome_categoria") + "\n ORDER BY c.idade_min";
            }
            Object[] params() { return Db.params(paramCatNome()); }
            void visual(Db.Resultado r) {
                double atl = coluna(r, "atletas").stream().mapToDouble(d -> d).sum();
                double tr = coluna(r, "treinos").stream().mapToDouble(d -> d).sum();
                double jg = coluna(r, "jogos").stream().mapToDouble(d -> d).sum();
                double fa = coluna(r, "faltas_em_treino").stream().mapToDouble(d -> d).sum();
                card(0, "Atletas", Estatistica.fmt(atl), null);
                card(1, "Treinos", Estatistica.fmt(tr), null);
                card(2, "Jogos", Estatistica.fmt(jg), null);
                card(3, "Faltas em treino", Estatistica.fmt(fa), atl == 0 ? null : String.format("%.2f por atleta", fa / atl));
                GraficoBarras g = new GraficoBarras("Raio-x por categoria");
                List<String> cats = new ArrayList<>();
                double[][] v = new double[4][r.linhas().size()];
                for (int i = 0; i < r.linhas().size(); i++) {
                    Object[] x = r.linhas().get(i);
                    cats.add(String.valueOf(x[0]));
                    v[0][i] = Ui.decimal(x[1]); v[1][i] = Ui.decimal(x[4]); v[2][i] = Ui.decimal(x[5]); v[3][i] = Ui.decimal(x[6]);
                }
                g.setDados(cats, List.of("Atletas", "Treinos", "Jogos", "Faltas"), v);
                mostrar(g);
            }
        });
        return l;
    }
}
