package academia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Conexão com o MySQL via JDBC. Todo SQL é escrito explicitamente e enviado com PreparedStatement. */
public class Db {

    // ======== CONFIGURAÇÃO DO MYSQL: coloquem aqui o usuário e a senha de vocês ========
    private static final String URL = System.getProperty("db.url",
            "jdbc:mysql://localhost:3306/academia_futebol?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Recife");
    private static final String USUARIO = System.getProperty("db.user", "root");
    private static final String SENHA = System.getProperty("db.password", "sua-senha-aqui");
    // ===================================================================================

    /** Resultado de um SELECT: nomes das colunas + linhas. */
    public record Resultado(List<String> colunas, List<Object[]> linhas) {
        public Object valor(int linha, int coluna) { return linhas.get(linha)[coluna]; }
        public boolean vazio() { return linhas.isEmpty(); }
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    /** Executa um SELECT com parâmetros (?) e devolve o resultado. */
    public static Resultado consultar(String sql, Object... params) throws SQLException {
        try (Connection con = conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, params);
            try (ResultSet rs = ps.executeQuery()) { return ler(rs); }
        }
    }

    /** Executa INSERT, UPDATE ou DELETE. Devolve quantas linhas foram afetadas. */
    public static int executar(String sql, Object... params) throws SQLException {
        try (Connection con = conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Chama um procedimento (CALL) e devolve o resultado do SELECT que ele produz (ou vazio). */
    public static Resultado chamar(String sql, Object... params) throws SQLException {
        try (Connection con = conectar(); CallableStatement cs = con.prepareCall(sql)) {
            preencher(cs, params);
            boolean temResultado = cs.execute();
            while (!temResultado && cs.getUpdateCount() != -1) temResultado = cs.getMoreResults();
            if (!temResultado) return new Resultado(List.of(), List.of());
            try (ResultSet rs = cs.getResultSet()) { return ler(rs); }
        }
    }

    private static Resultado ler(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        List<String> colunas = new ArrayList<>();
        for (int i = 1; i <= n; i++) colunas.add(md.getColumnLabel(i));
        List<Object[]> linhas = new ArrayList<>();
        while (rs.next()) {
            Object[] linha = new Object[n];
            for (int i = 1; i <= n; i++) linha[i - 1] = rs.getObject(i);
            linhas.add(linha);
        }
        return new Resultado(colunas, linhas);
    }

    /** Junta parâmetros (listas e valores soltos) num array só para o PreparedStatement. */
    public static Object[] params(Object... partes) {
        List<Object> todos = new ArrayList<>();
        for (Object p : partes) {
            if (p instanceof List<?> l) todos.addAll(l); else todos.add(p);
        }
        return todos.toArray();
    }

    public static void preencher(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) ps.setNull(i + 1, Types.NULL);
            else ps.setObject(i + 1, params[i]);
        }
    }

    /** Traduz os erros mais comuns do MySQL pra uma mensagem clara. */
    public static String mensagemErro(SQLException e) {
        return switch (e.getErrorCode()) {
            case 1062 -> "Já existe um registro com esse valor (ex.: camisa repetida na categoria ou clube já cadastrado).";
            case 1451 -> "Não dá pra excluir: existem registros ligados a esse item (restrição de chave estrangeira).";
            case 1452 -> "Referência inválida: o item escolhido não existe mais.";
            case 3819, 4025 -> "Valor fora das regras do banco (CHECK). Confira os campos.";
            // SIGNAL SQLSTATE '45000' dos procedimentos e triggers: mostra a mensagem que o banco mandou
            case 1644 -> e.getMessage();
            case 1406 -> "Algum texto passou do tamanho máximo permitido.";
            default -> "Erro do banco: " + e.getMessage();
        };
    }
}
