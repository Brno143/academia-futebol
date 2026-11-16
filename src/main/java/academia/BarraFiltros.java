package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Barra de filtros reutilizada no Painel, nas Views e nas Rotinas:
 * categoria (ou todas) e período (de / até). Gera os trechos de WHERE com "?"
 * e a lista de parâmetros para o PreparedStatement.
 */
public class BarraFiltros extends JPanel {

    private final JComboBox<Ui.Item> categoria = new JComboBox<>();
    private final JSpinner de = Ui.campoData(LocalDate.now());
    private final JSpinner ate = Ui.campoData(LocalDate.now());
    private final JLabel lblDe = new JLabel("De:");
    private final JLabel lblAte = new JLabel("Até:");
    private final JButton aplicar = new JButton("Aplicar filtros");
    private final JButton limpar = new JButton("Limpar");
    private LocalDate menorData = LocalDate.now(), maiorData = LocalDate.now();

    public BarraFiltros(Runnable aoAplicar) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setBorder(BorderFactory.createTitledBorder("Filtros"));
        add(new JLabel("Categoria:"));
        add(categoria);
        add(lblDe);
        add(de);
        add(lblAte);
        add(ate);
        add(aplicar);
        add(limpar);
        aplicar.addActionListener(e -> aoAplicar.run());
        categoria.addActionListener(e -> { if (categoria.isFocusOwner()) aoAplicar.run(); });
        limpar.addActionListener(e -> { reiniciar(); aoAplicar.run(); });
        recarregarOpcoes();
    }

    /** Lê as categorias e o intervalo de datas existente no banco (jogos, treinos e avaliações). */
    public void recarregarOpcoes() {
        try {
            Object atual = categoria.getSelectedItem() == null ? null : Ui.idSelecionado(categoria);
            categoria.removeAllItems();
            categoria.addItem(new Ui.Item(null, "Todas"));
            for (Object[] l : Db.consultar("SELECT cod_categoria, nome_categoria FROM CATEGORIA ORDER BY idade_min").linhas())
                categoria.addItem(new Ui.Item(l[0], String.valueOf(l[1])));
            if (atual != null) Ui.selecionar(categoria, atual);

            Object[] d = Db.consultar("""
                    SELECT MIN(d), MAX(d) FROM (
                        SELECT data_jogo AS d FROM JOGO
                        UNION ALL SELECT data_treino FROM TREINO
                        UNION ALL SELECT data_avaliacao FROM AVALIACAOFISICA) datas""").linhas().get(0);
            boolean primeiraVez = menorData.equals(maiorData);
            if (d[0] != null) { menorData = Ui.paraLocalDate(d[0]); maiorData = Ui.paraLocalDate(d[1]); }
            if (primeiraVez) reiniciarDatas();
        } catch (SQLException e) {
            Ui.erro(this, e);
        }
    }

    private void reiniciarDatas() {
        de.setValue(java.util.Date.from(menorData.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        ate.setValue(java.util.Date.from(maiorData.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
    }

    public void reiniciar() {
        categoria.setSelectedIndex(0);
        reiniciarDatas();
    }

    /** Esconde o período quando a tela não usa datas. */
    public void usarPeriodo(boolean usar) {
        lblDe.setEnabled(usar); de.setEnabled(usar); lblAte.setEnabled(usar); ate.setEnabled(usar);
    }

    public void usarCategoria(boolean usar) { categoria.setEnabled(usar); }

    /** Adiciona um componente extra (ex.: combo de variável) na barra. */
    public void extra(String rotulo, JComponent c) {
        remove(aplicar); remove(limpar);
        if (rotulo != null) add(new JLabel(rotulo));
        add(c);
        add(aplicar); add(limpar);
    }

    public Integer codCategoria() {
        Object id = categoria.getSelectedItem() == null ? null : Ui.idSelecionado(categoria);
        return id == null ? null : ((Number) id).intValue();
    }

    public String nomeCategoria() {
        return codCategoria() == null ? null : categoria.getSelectedItem().toString();
    }

    public LocalDate de() { return Ui.data(de); }
    public LocalDate ate() { return Ui.data(ate); }

    public String descricao() {
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return (codCategoria() == null ? "Todas as categorias" : nomeCategoria()) + " · " + f.format(de()) + " a " + f.format(ate());
    }

    /** " AND coluna = ?" quando há categoria escolhida. */
    public String sqlCategoria(String coluna) { return codCategoria() == null ? "" : " AND " + coluna + " = ?"; }

    public List<Object> paramCategoria() {
        List<Object> p = new ArrayList<>();
        if (codCategoria() != null) p.add(codCategoria());
        return p;
    }

    /** " AND coluna BETWEEN ? AND ?" */
    public String sqlPeriodo(String coluna) { return " AND " + coluna + " BETWEEN ? AND ?"; }

    public List<Object> paramPeriodo() { return List.of(de(), ate()); }
}
