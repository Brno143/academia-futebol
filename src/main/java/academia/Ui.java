package academia;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Utilidades de interface usadas pelas telas. */
public class Ui {

    public static final Color VERDE = new Color(0x1F7A3E);
    public static final Color VERMELHO = new Color(0xC8553D);
    public static final Color AMARELO = new Color(0xE0A526);
    public static final Color AZUL = new Color(0x2E6FAD);
    /** Paleta usada nos gráficos com várias séries/fatias. */
    public static final Color[] PALETA = {
            new Color(0x1F7A3E), new Color(0x2E6FAD), new Color(0xE0A526), new Color(0xC8553D),
            new Color(0x7A4FA3), new Color(0x2A9D8F), new Color(0x8C6D46), new Color(0x6B7B72)};

    /** Item de combo: guarda o código (id) e mostra o texto. */
    public record Item(Object id, String texto) {
        @Override public String toString() { return texto; }
    }

    /** Monta um JComboBox a partir de um SELECT com (id, texto). */
    public static JComboBox<Item> combo(Db.Resultado r, boolean comNenhum) {
        JComboBox<Item> c = new JComboBox<>();
        if (comNenhum) c.addItem(new Item(null, "Nenhum"));
        for (Object[] l : r.linhas()) c.addItem(new Item(l[0], String.valueOf(l[1])));
        return c;
    }

    public static void selecionar(JComboBox<Item> c, Object id) {
        for (int i = 0; i < c.getItemCount(); i++) {
            Object atual = c.getItemAt(i).id();
            if (atual != null && id != null && atual.toString().equals(id.toString())) { c.setSelectedIndex(i); return; }
        }
    }

    public static Object idSelecionado(JComboBox<Item> c) {
        return ((Item) c.getSelectedItem()).id();
    }

    /** Coloca o resultado de um SELECT numa JTable (somente leitura). */
    public static void preencherTabela(JTable tabela, Db.Resultado r) {
        DefaultTableModel m = new DefaultTableModel(r.colunas().toArray(), 0) {
            @Override public boolean isCellEditable(int l, int c) { return false; }
        };
        for (Object[] linha : r.linhas()) m.addRow(linha);
        tabela.setModel(m);
        tabela.setRowSorter(new javax.swing.table.TableRowSorter<>(m));
    }

    public static JTable novaTabela() {
        JTable t = new JTable();
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        return t;
    }

    /** Valor da coluna "col" na linha selecionada (ou null se nada selecionado). */
    public static Object valorSelecionado(JTable t, int col) {
        int linha = t.getSelectedRow();
        if (linha < 0) return null;
        return t.getModel().getValueAt(t.convertRowIndexToModel(linha), col);
    }

    /** Formulário simples: rótulo à esquerda, campo à direita. */
    public static JPanel formulario(Object... rotulosECampos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < rotulosECampos.length; i += 2) {
            g.gridy = i / 2;
            g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
            p.add(new JLabel((String) rotulosECampos[i]), g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            Component campo = (Component) rotulosECampos[i + 1];
            if (campo instanceof JTextField tf) tf.setColumns(22);
            p.add(campo, g);
        }
        return p;
    }

    /** Abre o formulário num diálogo. Devolve true se clicou em OK. */
    public static boolean dialogo(Component pai, String titulo, JPanel form) {
        return JOptionPane.showConfirmDialog(pai, form, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static JSpinner campoData(LocalDate inicial) {
        Date d = Date.from(inicial.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner s = new JSpinner(new SpinnerDateModel(d, null, null, java.util.Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        return s;
    }

    public static LocalDate data(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDate paraLocalDate(Object valor) {
        if (valor instanceof LocalDate ld) return ld;
        if (valor instanceof java.sql.Date sd) return sd.toLocalDate();
        return LocalDate.parse(valor.toString());
    }

    public static JSpinner campoNumero(int valor, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(valor, min, max, 1));
    }

    public static JSpinner campoDecimal(double valor, double min, double max, double passo) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(valor, min, max, passo));
        s.setEditor(new JSpinner.NumberEditor(s, "0.00"));
        return s;
    }

    /** Título grande de tela. */
    public static JLabel titulo(String texto) {
        JLabel t = new JLabel(texto);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));
        return t;
    }

    /** Cartão de indicador (rótulo, valor grande e detalhe). Devolve {valor, detalhe} para atualizar depois. */
    public static JLabel[] cartao(JPanel destino, String rotulo) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(new Color(0xEEF4EF));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, VERDE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel r = new JLabel(rotulo);
        r.setForeground(new Color(0x4A5A50));
        JLabel v = new JLabel("-");
        v.setFont(v.getFont().deriveFont(Font.BOLD, 22f));
        v.setForeground(VERDE);
        JLabel d = new JLabel(" ");
        d.setForeground(new Color(0x6B7B72));
        d.setFont(d.getFont().deriveFont(11f));
        card.add(r, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        card.add(d, BorderLayout.SOUTH);
        destino.add(card);
        return new JLabel[]{v, d};
    }

    /** Campo de texto que filtra as linhas de uma JTable enquanto o usuário digita. */
    public static JTextField filtroTabela(JTable tabela) {
        JTextField f = new JTextField(18);
        f.setToolTipText("Digite para filtrar as linhas da tabela");
        f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void aplicar() {
                if (!(tabela.getRowSorter() instanceof javax.swing.table.TableRowSorter<?> rs)) return;
                String t = f.getText().trim();
                @SuppressWarnings("unchecked")
                javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter =
                        (javax.swing.table.TableRowSorter<javax.swing.table.TableModel>) rs;
                sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t)));
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicar(); }
        });
        return f;
    }

    public static String texto(JTextField f) {
        String t = f.getText().trim();
        return t.isEmpty() ? null : t;
    }

    public static void erro(Component pai, SQLException e) {
        JOptionPane.showMessageDialog(pai, Db.mensagemErro(e), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void erro(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void ok(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Pronto", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirmar(Component pai, String msg) {
        return JOptionPane.showConfirmDialog(pai, msg, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static int inteiro(Object o) { return o == null ? 0 : ((Number) o).intValue(); }

    public static double decimal(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
}
