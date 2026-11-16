package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Consultas da Etapa 03. Lista as consultas; ao escolher uma, mostra o SQL e executa no banco. */
public class PainelConsultas extends JPanel {

    private final JList<Consultas.Consulta> lista = new JList<>(Consultas.TODAS.toArray(new Consultas.Consulta[0]));
    private final JLabel titulo = new JLabel();
    private final JLabel descricao = new JLabel();
    private final JLabel conceitos = new JLabel();
    private final JTextArea sql = new JTextArea();
    private final JTable tabela = Ui.novaTabela();
    private final JLabel total = new JLabel();

    public PainelConsultas() {
        super(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFixedCellHeight(30);
        lista.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) executar(); });
        JScrollPane esquerda = new JScrollPane(lista);
        esquerda.setPreferredSize(new Dimension(390, 0));

        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        conceitos.setForeground(new Color(0x4A5A50));
        sql.setEditable(false);
        sql.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sql.setBackground(new Color(0xF6F8F6));
        JScrollPane sqlScroll = new JScrollPane(sql);
        sqlScroll.setPreferredSize(new Dimension(0, 190));
        sqlScroll.setBorder(BorderFactory.createTitledBorder("SQL executado"));

        JPanel cabecalho = new JPanel(new GridLayout(0, 1, 0, 4));
        cabecalho.add(titulo);
        cabecalho.add(descricao);
        cabecalho.add(conceitos);

        JPanel topo = new JPanel(new BorderLayout(0, 8));
        topo.add(cabecalho, BorderLayout.NORTH);
        topo.add(sqlScroll, BorderLayout.CENTER);

        JPanel direita = new JPanel(new BorderLayout(0, 8));
        direita.add(topo, BorderLayout.NORTH);
        direita.add(new JScrollPane(tabela), BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout());
        JPanel busca = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        busca.add(new JLabel("Buscar no resultado:"));
        busca.add(Ui.filtroTabela(tabela));
        rodape.add(busca, BorderLayout.WEST);
        rodape.add(total, BorderLayout.EAST);
        direita.add(rodape, BorderLayout.SOUTH);

        add(esquerda, BorderLayout.WEST);
        add(direita, BorderLayout.CENTER);
        lista.setSelectedIndex(0);
    }

    private void executar() {
        Consultas.Consulta c = lista.getSelectedValue();
        if (c == null) return;
        titulo.setText(c.titulo());
        descricao.setText(c.descricao());
        conceitos.setText("Conceitos: " + c.conceitos());
        sql.setText(c.sql().strip());
        sql.setCaretPosition(0);
        try {
            Db.Resultado r = Db.consultar(c.paraJdbc());
            Ui.preencherTabela(tabela, r);
            total.setText(r.linhas().size() + " linha(s)");
        } catch (SQLException e) {
            Ui.erro(this, e);
        }
    }
}
