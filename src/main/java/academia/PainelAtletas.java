package academia;

import javax.swing.*;
import java.sql.*;
import java.time.LocalDate;

/** CRUD de atletas: mexe em PESSOA, ATLETA e TELEFONE (especialização). */
public class PainelAtletas extends PainelCrud {

    public PainelAtletas() { super("Atletas", "atleta"); }

    @Override
    protected Db.Resultado listar() throws SQLException {
        return Db.consultar("""
                SELECT a.cod_pessoa AS codigo, c.nome_categoria AS categoria, a.num_camisa AS camisa,
                       p.nome, DATE_FORMAT(p.data_nasc, '%d/%m/%Y') AS nascimento,
                       a.pe_dominante AS pe, p.bairro, ps.nome AS reserva_de
                FROM ATLETA a
                JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                LEFT JOIN PESSOA ps ON ps.cod_pessoa = a.substituto_de
                ORDER BY c.idade_min, a.num_camisa""");
    }

    private static JComboBox<Ui.Item> comboCategorias() throws SQLException {
        return Ui.combo(Db.consultar("SELECT cod_categoria, nome_categoria FROM CATEGORIA ORDER BY idade_min"), false);
    }

    /** Atletas da categoria que ainda não têm reserva (substituto_de é UNIQUE). */
    private static void carregarReservas(JComboBox<Ui.Item> reserva, Object categoria) {
        reserva.removeAllItems();
        reserva.addItem(new Ui.Item(null, "Nenhum"));
        try {
            Db.Resultado r = Db.consultar("""
                    SELECT a.cod_pessoa, CONCAT('#', a.num_camisa, ' ', p.nome)
                    FROM ATLETA a
                    JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                    WHERE a.cod_categoria = ?
                      AND a.cod_pessoa NOT IN (SELECT substituto_de FROM ATLETA WHERE substituto_de IS NOT NULL)
                    ORDER BY a.num_camisa""", categoria);
            for (Object[] l : r.linhas()) reserva.addItem(new Ui.Item(l[0], String.valueOf(l[1])));
        } catch (SQLException e) {
            // se falhar, fica só "Nenhum"
        }
    }

    @Override
    protected void novo() {
        try {
            JTextField nome = new JTextField(), rua = new JTextField(), numero = new JTextField(),
                    bairro = new JTextField(), cidade = new JTextField("Recife"), cep = new JTextField(),
                    telefone = new JTextField();
            JSpinner nasc = Ui.campoData(LocalDate.of(2010, 1, 1));
            JComboBox<String> pe = new JComboBox<>(new String[]{"Direito", "Esquerdo"});
            JSpinner camisa = Ui.campoNumero(1, 1, 99);
            JComboBox<Ui.Item> categoria = comboCategorias();
            JComboBox<Ui.Item> reserva = new JComboBox<>();
            carregarReservas(reserva, Ui.idSelecionado(categoria));
            categoria.addActionListener(e -> carregarReservas(reserva, Ui.idSelecionado(categoria)));

            JPanel form = Ui.formulario("Nome completo", nome, "Nascimento", nasc, "Categoria", categoria,
                    "Camisa", camisa, "Pé dominante", pe, "É reserva de", reserva, "Rua", rua, "Número", numero,
                    "Bairro", bairro, "Cidade", cidade, "CEP", cep, "Telefone", telefone);

            while (Ui.dialogo(this, "Cadastrar atleta", form)) {
                if (Ui.texto(nome) == null) { Ui.erro(this, "Preencha o nome."); continue; }
                // Transação: PESSOA -> ATLETA -> TELEFONE. Se algo falhar, nada é gravado.
                try (Connection con = Db.conectar()) {
                    con.setAutoCommit(false);
                    try {
                        long cod;
                        try (PreparedStatement ps = con.prepareStatement("""
                                INSERT INTO PESSOA (nome, data_nasc, rua, numero, bairro, cidade, cep)
                                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
                            Db.preencher(ps, Ui.texto(nome), Ui.data(nasc), Ui.texto(rua), Ui.texto(numero),
                                    Ui.texto(bairro), Ui.texto(cidade), Ui.texto(cep));
                            ps.executeUpdate();
                            try (ResultSet chave = ps.getGeneratedKeys()) { chave.next(); cod = chave.getLong(1); }
                        }
                        try (PreparedStatement ps = con.prepareStatement("""
                                INSERT INTO ATLETA (cod_pessoa, pe_dominante, num_camisa, cod_categoria, substituto_de)
                                VALUES (?, ?, ?, ?, ?)""")) {
                            Db.preencher(ps, cod, pe.getSelectedItem(), camisa.getValue(),
                                    Ui.idSelecionado(categoria), Ui.idSelecionado(reserva));
                            ps.executeUpdate();
                        }
                        if (Ui.texto(telefone) != null) {
                            try (PreparedStatement ps = con.prepareStatement(
                                    "INSERT INTO TELEFONE (telefone, cod_pessoa) VALUES (?, ?)")) {
                                Db.preencher(ps, Ui.texto(telefone), cod);
                                ps.executeUpdate();
                            }
                        }
                        con.commit();
                        Ui.ok(this, Ui.texto(nome) + " cadastrado.");
                        return;
                    } catch (SQLException e) {
                        con.rollback();
                        throw e;
                    }
                } catch (SQLException e) { Ui.erro(this, e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    @Override
    protected void editar(Object id) {
        try {
            Object[] a = Db.consultar("""
                    SELECT p.nome, p.bairro, p.cidade, a.pe_dominante, a.num_camisa, a.cod_categoria
                    FROM ATLETA a JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                    WHERE a.cod_pessoa = ?""", id).linhas().get(0);
            JTextField nome = new JTextField((String) a[0]);
            JTextField bairro = new JTextField(a[1] == null ? "" : (String) a[1]);
            JTextField cidade = new JTextField(a[2] == null ? "" : (String) a[2]);
            JComboBox<String> pe = new JComboBox<>(new String[]{"Direito", "Esquerdo"});
            pe.setSelectedItem(a[3]);
            JSpinner camisa = Ui.campoNumero(Ui.inteiro(a[4]), 1, 99);
            JComboBox<Ui.Item> categoria = comboCategorias();
            Ui.selecionar(categoria, a[5]);

            JPanel form = Ui.formulario("Nome", nome, "Bairro", bairro, "Cidade", cidade,
                    "Pé dominante", pe, "Camisa", camisa, "Categoria", categoria);

            while (Ui.dialogo(this, "Alterar atleta", form)) {
                if (Ui.texto(nome) == null) { Ui.erro(this, "Preencha o nome."); continue; }
                try (Connection con = Db.conectar()) {
                    con.setAutoCommit(false);
                    try (PreparedStatement p1 = con.prepareStatement(
                                 "UPDATE PESSOA SET nome = ?, bairro = ?, cidade = ? WHERE cod_pessoa = ?");
                         PreparedStatement p2 = con.prepareStatement(
                                 "UPDATE ATLETA SET pe_dominante = ?, num_camisa = ?, cod_categoria = ? WHERE cod_pessoa = ?")) {
                        Db.preencher(p1, Ui.texto(nome), Ui.texto(bairro), Ui.texto(cidade), id);
                        Db.preencher(p2, pe.getSelectedItem(), camisa.getValue(), Ui.idSelecionado(categoria), id);
                        p1.executeUpdate();
                        p2.executeUpdate();
                        con.commit();
                        Ui.ok(this, "Dados de " + Ui.texto(nome) + " atualizados.");
                        return;
                    } catch (SQLException e) {
                        con.rollback();
                        throw e;
                    }
                } catch (SQLException e) { Ui.erro(this, e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    @Override
    protected void excluir(Object id) {
        try {
            Object[] v = Db.consultar("""
                    SELECT p.nome,
                           (SELECT COUNT(*) FROM ESCALACAO WHERE cod_pessoa = ?),
                           (SELECT COUNT(*) FROM PARTICIPA WHERE cod_pessoa = ?),
                           (SELECT COUNT(*) FROM AVALIACAOFISICA WHERE cod_pessoa = ?),
                           (SELECT pr.nome FROM ATLETA r JOIN PESSOA pr ON pr.cod_pessoa = r.cod_pessoa
                             WHERE r.substituto_de = ?)
                    FROM PESSOA p WHERE p.cod_pessoa = ?""", id, id, id, id, id).linhas().get(0);

            StringBuilder msg = new StringBuilder("Excluir " + v[0] + "?\n\nTambém serão apagados (ON DELETE CASCADE): ")
                    .append(Ui.inteiro(v[1])).append(" escalação(ões), ")
                    .append(Ui.inteiro(v[2])).append(" registro(s) de treino e ")
                    .append(Ui.inteiro(v[3])).append(" avaliação(ões) física(s).");
            if (v[4] != null)
                msg.append("\n\n").append(v[4]).append(" é reserva desse atleta. O campo substituto_de dele vira NULL (ON DELETE SET NULL).");
            if (!Ui.confirmar(this, msg.toString())) return;

            // Apagar a PESSOA apaga o ATLETA e os telefones em cascata
            Db.executar("DELETE FROM PESSOA WHERE cod_pessoa = ?", id);
            Ui.ok(this, v[0] + " excluído.");
        } catch (SQLException e) { Ui.erro(this, e); }
    }
}
