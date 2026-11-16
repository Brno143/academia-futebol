package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Base das telas de cadastro: tabela + botões Cadastrar, Alterar, Excluir. A 1ª coluna da tabela é o código. */
public abstract class PainelCrud extends JPanel {

    protected final JTable tabela = Ui.novaTabela();

    protected PainelCrud(String titulo, String nomeItem) {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));

        JButton novo = new JButton("Cadastrar " + nomeItem);
        JButton editar = new JButton("Alterar selecionado");
        JButton excluir = new JButton("Excluir selecionado");
        novo.addActionListener(e -> { novo(); carregar(); });
        editar.addActionListener(e -> {
            Object id = Ui.valorSelecionado(tabela, 0);
            if (id == null) { Ui.erro(this, "Selecione uma linha na tabela."); return; }
            editar(id);
            carregar();
        });
        excluir.addActionListener(e -> {
            Object id = Ui.valorSelecionado(tabela, 0);
            if (id == null) { Ui.erro(this, "Selecione uma linha na tabela."); return; }
            excluir(id);
            carregar();
        });

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.add(new JLabel("Buscar:"));
        botoes.add(Ui.filtroTabela(tabela));
        botoes.add(novo);
        botoes.add(editar);
        botoes.add(excluir);

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(t, BorderLayout.WEST);
        barra.add(botoes, BorderLayout.EAST);

        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editar.doClick();
            }
        });

        add(barra, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    public void carregar() {
        try {
            Ui.preencherTabela(tabela, listar());
        } catch (SQLException e) {
            Ui.erro(this, e);
        }
    }

    protected abstract Db.Resultado listar() throws SQLException;
    protected abstract void novo();
    protected abstract void editar(Object id);
    protected abstract void excluir(Object id);
}
