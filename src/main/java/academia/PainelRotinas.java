package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 06 - Integração com as funções, procedimentos e triggers da Etapa 05.
 * Permite executar cada rotina pela interface e ver o efeito (tabela + gráfico).
 */
public class PainelRotinas extends JPanel {

    // ---- funções ----
    private final JTable tAprov = Ui.novaTabela();
    private final GraficoBarras gAprov = new GraficoBarras("fn_aproveitamento_categoria(cod_categoria)");
    private final BarraFiltros filtros = new BarraFiltros(this::carregar);
    private final JTable tFreq = Ui.novaTabela();
    private final GraficoPizza gFreq = new GraficoPizza("Situação de frequência (fn_situacao_frequencia)");

    // ---- procedimentos ----
    private final JComboBox<Ui.Item> jogoPlacar = new JComboBox<>();
    private final JSpinner golsPro = Ui.campoNumero(0, -5, 50);
    private final JSpinner golsContra = Ui.campoNumero(0, -5, 50);
    private final JTable tSeq = Ui.novaTabela();
    private final GraficoBarras gSeq = new GraficoBarras("sp_sequencia_invicta() — jogos sem perder");

    // ---- triggers ----
    private final JTable tLog = Ui.novaTabela();
    private final JTable tLogProc = Ui.novaTabela();
    private final JComboBox<Ui.Item> atletaTeste = new JComboBox<>();
    private final JComboBox<Ui.Item> jogoTeste = new JComboBox<>();
    private final JLabel resultadoTeste = new JLabel(" ");
    private boolean carregandoCombos = false;

    public PainelRotinas() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.add(Ui.titulo("Funções, procedimentos e triggers (Etapa 05)"), BorderLayout.NORTH);
        topo.add(filtros, BorderLayout.CENTER);
        add(topo, BorderLayout.NORTH);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Funções", funcoes());
        abas.addTab("Procedimentos", procedimentos());
        abas.addTab("Triggers", triggers());
        add(abas, BorderLayout.CENTER);
    }

    private static JTextArea sqlBox(String sql) {
        JTextArea a = new JTextArea(sql);
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        a.setBackground(new Color(0xF6F8F6));
        a.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return a;
    }

    private static JPanel bloco(String titulo, String explicacao) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titulo),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        JLabel e = new JLabel("<html>" + explicacao + "</html>");
        e.setForeground(new Color(0x4A5A50));
        p.add(e, BorderLayout.NORTH);
        return p;
    }

    // =========================================================================
    // FUNÇÕES
    // =========================================================================
    private JComponent funcoes() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 10));

        JPanel b1 = bloco("fn_aproveitamento_categoria",
                "Recebe a categoria e devolve o aproveitamento nos jogos (pontos ganhos / pontos possíveis, em %). "
                        + "A função é chamada dentro do SELECT, uma vez por categoria.");
        String sql1 = "SELECT nome_categoria AS categoria,\n       fn_aproveitamento_categoria(cod_categoria) AS aproveitamento_pct\n  FROM CATEGORIA ORDER BY idade_min";
        JButton exec1 = new JButton("Executar função");
        exec1.addActionListener(e -> carregarAproveitamento());
        JPanel c1 = new JPanel(new BorderLayout(0, 4));
        c1.add(sqlBox(sql1), BorderLayout.NORTH);
        c1.add(new JScrollPane(tAprov), BorderLayout.CENTER);
        c1.add(exec1, BorderLayout.SOUTH);
        JSplitPane s1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, c1, gAprov);
        s1.setResizeWeight(0.45);
        b1.add(s1, BorderLayout.CENTER);

        JPanel b2 = bloco("fn_situacao_frequencia",
                "Recebe o atleta e classifica a frequência nos treinos: Regular (≥ 75%), Atenção (50% a 74,9%), "
                        + "Crítica (< 50%) ou Sem registro. Usa a categoria escolhida nos filtros.");
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton exec2 = new JButton("Executar função");
        exec2.addActionListener(e -> carregarFrequencia());
        filtro.add(exec2);
        JTextField busca = Ui.filtroTabela(tFreq);
        filtro.add(new JLabel("  Buscar:"));
        filtro.add(busca);
        JPanel c2 = new JPanel(new BorderLayout(0, 4));
        c2.add(filtro, BorderLayout.NORTH);
        c2.add(new JScrollPane(tFreq), BorderLayout.CENTER);
        JSplitPane s2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, c2, gFreq);
        s2.setResizeWeight(0.55);
        b2.add(s2, BorderLayout.CENTER);

        p.add(b1);
        p.add(b2);
        return p;
    }

    private void carregarAproveitamento() {
        try {
            Db.Resultado r = Db.consultar("""
                    SELECT nome_categoria AS categoria,
                           fn_aproveitamento_categoria(cod_categoria) AS aproveitamento_pct
                      FROM CATEGORIA
                     WHERE (? IS NULL OR cod_categoria = ?)
                     ORDER BY idade_min""", filtros.codCategoria(), filtros.codCategoria());
            Ui.preencherTabela(tAprov, r);
            gAprov.setDados(r);
            gAprov.setSufixo("%");
            List<Double> v = new ArrayList<>();
            for (Object[] l : r.linhas()) if (l[1] != null) v.add(Ui.decimal(l[1]));
            if (!v.isEmpty()) gAprov.referencia("média", Math.round(Estatistica.media(v) * 10) / 10.0);
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private void carregarFrequencia() {
        try {
            Object cat = filtros.codCategoria();
            Db.Resultado r = Db.consultar("""
                    SELECT p.nome AS atleta, c.nome_categoria AS categoria, f.pct_presenca,
                           fn_situacao_frequencia(a.cod_pessoa) AS situacao
                      FROM ATLETA a
                      JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
                      JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                      JOIN vw_ficha_atleta f ON f.cod_pessoa = a.cod_pessoa
                     WHERE (? IS NULL OR a.cod_categoria = ?)
                     ORDER BY c.idade_min, p.nome""", cat, cat);
            Ui.preencherTabela(tFreq, r);
            String[] ordem = {"Regular", "Atenção", "Crítica", "Sem registro"};
            double[] q = new double[4];
            for (Object[] l : r.linhas()) for (int i = 0; i < 4; i++) if (ordem[i].equals(l[3])) q[i]++;
            List<String> rot = new ArrayList<>();
            List<Double> val = new ArrayList<>();
            List<Color> cores = new ArrayList<>();
            Color[] c = {Ui.VERDE, Ui.AMARELO, Ui.VERMELHO, Color.GRAY};
            for (int i = 0; i < 4; i++) if (q[i] > 0) { rot.add(ordem[i]); val.add(q[i]); cores.add(c[i]); }
            gFreq.setDados(rot, val.stream().mapToDouble(d -> d).toArray(), cores.toArray(new Color[0]));
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    // =========================================================================
    // PROCEDIMENTOS
    // =========================================================================
    private JComponent procedimentos() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 10));

        JPanel b1 = bloco("sp_atualizar_placar(cod_jogo, gols_pro, gols_contra)",
                "Lança ou corrige o placar de um jogo. O procedimento valida se o jogo existe e se o placar não é negativo "
                        + "(SIGNAL). Ao alterar o placar, o trigger trg_log_placar grava o antes/depois na tabela LOG_PLACAR "
                        + "(veja na aba Triggers). Tente um valor negativo para ver a validação.");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(new JLabel("Jogo:"));
        form.add(jogoPlacar);
        form.add(new JLabel("Gols pró:"));
        form.add(golsPro);
        form.add(new JLabel("Gols contra:"));
        form.add(golsContra);
        JButton call = new JButton("CALL sp_atualizar_placar");
        call.addActionListener(e -> atualizarPlacar());
        form.add(call);
        jogoPlacar.addActionListener(e -> preencherPlacarAtual());
        JPanel c1 = new JPanel(new BorderLayout(0, 6));
        c1.add(form, BorderLayout.NORTH);
        c1.add(sqlBox("CALL sp_atualizar_placar(?, ?, ?);   -- enviado com CallableStatement"), BorderLayout.CENTER);
        JScrollPane sLog = new JScrollPane(tLogProc);
        sLog.setBorder(BorderFactory.createTitledBorder("Efeito: LOG_PLACAR (preenchido pelo trigger trg_log_placar)"));
        c1.add(sLog, BorderLayout.SOUTH);
        sLog.setPreferredSize(new Dimension(0, 150));
        b1.add(c1, BorderLayout.CENTER);

        JPanel b2 = bloco("sp_sequencia_invicta()  — com CURSOR",
                "Percorre os jogos de cada categoria em ordem de data com um cursor: vitória ou empate soma 1 na sequência, "
                        + "derrota zera. Mostra a maior sequência invicta e a sequência atual de cada categoria.");
        JButton exec = new JButton("CALL sp_sequencia_invicta()");
        exec.addActionListener(e -> carregarSequencia());
        JPanel c2 = new JPanel(new BorderLayout(0, 4));
        c2.add(new JScrollPane(tSeq), BorderLayout.CENTER);
        c2.add(exec, BorderLayout.SOUTH);
        JSplitPane s2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, c2, gSeq);
        s2.setResizeWeight(0.45);
        b2.add(s2, BorderLayout.CENTER);

        p.add(b1);
        p.add(b2);
        return p;
    }

    private void preencherPlacarAtual() {
        if (carregandoCombos || jogoPlacar.getSelectedItem() == null) return;
        try {
            Db.Resultado r = Db.consultar("SELECT gols_pro, gols_contra FROM JOGO WHERE cod_jogo = ?", Ui.idSelecionado(jogoPlacar));
            if (!r.vazio()) { golsPro.setValue(Ui.inteiro(r.valor(0, 0))); golsContra.setValue(Ui.inteiro(r.valor(0, 1))); }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private void atualizarPlacar() {
        Object jogo = Ui.idSelecionado(jogoPlacar);
        try {
            int logsAntes = Ui.inteiro(Db.consultar("SELECT COUNT(*) FROM LOG_PLACAR").valor(0, 0));
            Db.chamar("{CALL sp_atualizar_placar(?, ?, ?)}", jogo, golsPro.getValue(), golsContra.getValue());
            int logsDepois = Ui.inteiro(Db.consultar("SELECT COUNT(*) FROM LOG_PLACAR").valor(0, 0));
            Ui.ok(this, "Placar do jogo " + jogo + " atualizado para " + golsPro.getValue() + " x " + golsContra.getValue() + ".\n"
                    + (logsDepois > logsAntes ? "O trigger trg_log_placar registrou a alteração em LOG_PLACAR."
                    : "O placar não mudou, então o trigger não registrou nada."));
            carregarCombos();
            carregarLog();
            carregarSequencia();
            carregarAproveitamento();
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private void carregarSequencia() {
        try {
            Db.Resultado r = Db.chamar("{CALL sp_sequencia_invicta()}");
            Ui.preencherTabela(tSeq, r);
            List<String> cats = new ArrayList<>();
            double[][] v = new double[2][r.linhas().size()];
            for (int i = 0; i < r.linhas().size(); i++) {
                cats.add(String.valueOf(r.valor(i, 0)));
                v[0][i] = Ui.decimal(r.valor(i, 1));
                v[1][i] = Ui.decimal(r.valor(i, 2));
            }
            gSeq.setDados(cats, List.of("Maior sequência invicta", "Sequência atual"), v, Ui.VERDE, Ui.AZUL);
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    // =========================================================================
    // TRIGGERS
    // =========================================================================
    private JComponent triggers() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 10));

        JPanel b1 = bloco("trg_log_placar  (AFTER UPDATE ON JOGO)",
                "Toda vez que o placar de um jogo muda (pela aba Jogos ou pelo procedimento sp_atualizar_placar), "
                        + "o trigger grava o placar antigo, o novo, o usuário e a data em LOG_PLACAR.");
        JButton atualizar = new JButton("Atualizar log");
        atualizar.addActionListener(e -> carregarLog());
        JPanel c1 = new JPanel(new BorderLayout(0, 4));
        c1.add(new JScrollPane(tLog), BorderLayout.CENTER);
        c1.add(atualizar, BorderLayout.SOUTH);
        b1.add(c1, BorderLayout.CENTER);

        JPanel b2 = bloco("trg_valida_categoria_escalacao  (BEFORE INSERT ON ESCALACAO)",
                "Um atleta só pode ser escalado em jogo da categoria dele. Escolha um atleta e um jogo e clique em testar: "
                        + "o INSERT é feito dentro de uma transação e desfeito com ROLLBACK, então o teste não grava nada. "
                        + "Para gravar de verdade, use a aba Escalações.");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(new JLabel("Atleta:"));
        form.add(atletaTeste);
        form.add(new JLabel("Jogo:"));
        form.add(jogoTeste);
        JButton testar = new JButton("Testar INSERT em ESCALACAO");
        testar.addActionListener(e -> testarTrigger());
        form.add(testar);
        resultadoTeste.setFont(resultadoTeste.getFont().deriveFont(Font.BOLD, 14f));
        JPanel c2 = new JPanel(new BorderLayout(0, 8));
        c2.add(form, BorderLayout.NORTH);
        c2.add(resultadoTeste, BorderLayout.CENTER);
        c2.add(sqlBox("INSERT INTO ESCALACAO (cod_pessoa, cod_jogo, cod_posicao, minutos_jogados, c_amarelo, c_vermelho, titular)\n"
                + "VALUES (?, ?, 1, 0, FALSE, FALSE, FALSE);   -- seguido de ROLLBACK"), BorderLayout.SOUTH);
        b2.add(c2, BorderLayout.CENTER);

        p.add(b1);
        p.add(b2);
        return p;
    }

    private void carregarLog() {
        try {
            Db.Resultado r = Db.consultar("""
                    SELECT l.cod_log, l.cod_jogo, v.categoria, v.adversario,
                           CONCAT(l.gols_pro_antes, ' x ', l.gols_contra_antes)   AS placar_antes,
                           CONCAT(l.gols_pro_depois, ' x ', l.gols_contra_depois) AS placar_depois,
                           l.usuario, DATE_FORMAT(l.data_alteracao, '%d/%m/%Y %H:%i:%s') AS quando
                      FROM LOG_PLACAR l
                      LEFT JOIN vw_resultado_jogos v ON v.cod_jogo = l.cod_jogo
                     WHERE v.data_jogo BETWEEN ? AND ?
                       AND (? IS NULL OR v.cod_categoria = ?)
                     ORDER BY l.cod_log DESC""",
                    filtros.de(), filtros.ate(), filtros.codCategoria(), filtros.codCategoria());
            Ui.preencherTabela(tLog, r);
            Ui.preencherTabela(tLogProc, r);
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private void testarTrigger() {
        Object atleta = Ui.idSelecionado(atletaTeste), jogo = Ui.idSelecionado(jogoTeste);
        try (Connection con = Db.conectar()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO ESCALACAO (cod_pessoa, cod_jogo, cod_posicao, minutos_jogados, c_amarelo, c_vermelho, titular)
                    VALUES (?, ?, 1, 0, FALSE, FALSE, FALSE)""")) {
                Db.preencher(ps, atleta, jogo);
                ps.executeUpdate();
                resultadoTeste.setForeground(Ui.VERDE);
                resultadoTeste.setText("✔ Permitido: atleta e jogo são da mesma categoria. (teste desfeito com ROLLBACK)");
            } catch (SQLException e) {
                resultadoTeste.setForeground(Ui.VERMELHO);
                resultadoTeste.setText("✖ Bloqueado pelo banco: " + Db.mensagemErro(e));
            } finally {
                con.rollback();
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    // =========================================================================
    public void carregar() {
        filtros.recarregarOpcoes();
        carregarCombos();
        carregarAproveitamento();
        carregarFrequencia();
        carregarSequencia();
        carregarLog();
    }

    private void carregarCombos() {
        carregandoCombos = true;
        try {
            Object j = jogoPlacar.getSelectedItem() == null ? null : Ui.idSelecionado(jogoPlacar);
            Object a = atletaTeste.getSelectedItem() == null ? null : Ui.idSelecionado(atletaTeste);
            Object jt = jogoTeste.getSelectedItem() == null ? null : Ui.idSelecionado(jogoTeste);
            String sqlJogos = """
                    SELECT cod_jogo, CONCAT('#', cod_jogo, ' · ', DATE_FORMAT(data_jogo, '%d/%m'), ' · ', categoria,
                                            ' x ', adversario, '  (', gols_pro, ' x ', gols_contra, ')')
                      FROM vw_resultado_jogos
                     WHERE data_jogo BETWEEN ? AND ?
                       AND (? IS NULL OR cod_categoria = ?)
                     ORDER BY data_jogo, cod_jogo""";
            Object[] p = {filtros.de(), filtros.ate(), filtros.codCategoria(), filtros.codCategoria()};
            trocar(jogoPlacar, Db.consultar(sqlJogos, p), false);
            trocar(jogoTeste, Db.consultar(sqlJogos, p), false);
            trocar(atletaTeste, Db.consultar("""
                    SELECT a.cod_pessoa, CONCAT(p.nome, ' (', c.nome_categoria, ')')
                      FROM ATLETA a JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                      JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                     ORDER BY c.idade_min, p.nome"""), false);
            if (j != null) Ui.selecionar(jogoPlacar, j);
            if (a != null) Ui.selecionar(atletaTeste, a);
            if (jt != null) Ui.selecionar(jogoTeste, jt);
        } catch (SQLException e) {
            Ui.erro(this, e);
        } finally {
            carregandoCombos = false;
        }
        preencherPlacarAtual();
    }

    private static void trocar(JComboBox<Ui.Item> combo, Db.Resultado r, boolean comTodas) {
        combo.removeAllItems();
        if (comTodas) combo.addItem(new Ui.Item(null, "Todas"));
        for (Object[] l : r.linhas()) combo.addItem(new Ui.Item(l[0], String.valueOf(l[1])));
    }
}
