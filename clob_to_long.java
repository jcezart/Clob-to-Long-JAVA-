create or replace and compile java source named "UtilClobToLong" as
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import oracle.jdbc.driver.OracleDriver;

public class ClobtoLong {
    private static String limparTexto(String textoOriginal) {
        if (textoOriginal == null) return "";

        // Remove HTML
        textoOriginal = textoOriginal.replaceAll("<[^>]+>", "");

        // Remove imagens Markdown e links
        textoOriginal = textoOriginal.replaceAll("!\\[.*?\\]\\(.*?\\)", ""); // imagens
        textoOriginal = textoOriginal.replaceAll("\\[.*?\\]\\(.*?\\)", "");  // links markdown
        textoOriginal = textoOriginal.replaceAll("[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+", ""); // URLs e domínios

        // Remove texto de confidencialidade
        textoOriginal = textoOriginal.replaceAll("Esta mensagem, incluindo seus anexos, pode conter informação confidencial.*?Agradecemos sua cooperação\\.", "");
        textoOriginal = textoOriginal.replaceAll("This message, including its attachments, may contain confidential.*?We appreciate your cooperation\\.", "");

        // Remove cabeçalhos estilo Markdown e outros padrões
        textoOriginal = textoOriginal.replaceAll("(?m)^#+\\s*", "");
        textoOriginal = textoOriginal.replaceAll("\\\\n\\\\n--\\\\n", " ");
        textoOriginal = textoOriginal.replaceAll("\\\\n\\\\n######", " ");
        textoOriginal = textoOriginal.replaceAll("\\\\n \\\\n \\\\n", " ");
        textoOriginal = textoOriginal.replaceAll("\\\\n\\\\n", " ");
        textoOriginal = textoOriginal.replaceAll("\\\\n", " ");
        textoOriginal = textoOriginal.replaceAll("[*]+", ""); // Remove sequências de *
        textoOriginal = textoOriginal.replaceAll("[#]+", ""); // Remove #
        textoOriginal = textoOriginal.replaceAll("(?m)^--\\s*", " "); // Remove -- no início de linhas
        textoOriginal = textoOriginal.replaceAll("#####", "");

        // Remove todas as tabulações e sequências mistas
        textoOriginal = textoOriginal.replaceAll("[\\t\\s]+", " "); // Captura \t, espaços e misturas

        // Adiciona espaço após pontuação, se necessário
        textoOriginal = textoOriginal.replaceAll("([,.?!])([^\\s])", "$1 $2");

        // Corrige | sem espaço
        textoOriginal = textoOriginal.replaceAll("\\|([^\\s])", "| $1");

        return textoOriginal.trim();
    }

    private static String extractAndCleanAllBodies(Clob entrada) throws SQLException {
        try {
            // Converte o CLOB para String
            String jsonString = entrada.getSubString(1, (int) entrada.length());
            StringBuilder result = new StringBuilder();

            // Localiza todos os campos "body" no array "comments"
            String bodyMarker = "\"body\":\"";
            int startIndex = 0;

            while (true) {
                // Encontra o próximo "body"
                startIndex = jsonString.indexOf(bodyMarker, startIndex);
                if (startIndex == -1) {
                    break; // Não há mais campos "body"
                }
                startIndex += bodyMarker.length();

                // Encontra o fim do campo "body"
                int endIndex = jsonString.indexOf("\",", startIndex);
                if (endIndex == -1) {
                    endIndex = jsonString.indexOf("\"}", startIndex);
                }
                if (endIndex == -1) {
                    break; // Fim do campo não encontrado
                }

                // Extrai o conteúdo do campo "body"
                String bodyContent = jsonString.substring(startIndex, endIndex);

                // Decodifica caracteres de escape
                bodyContent = bodyContent.replace("\\n", "\n")
                                         .replace("\\r", "\n")
                                         .replace("\\t", "\n")
                                         .replace("\\\"", "\"")
                                         .replace("\\\\", "\\");

                // Limpa o texto usando a função limparTexto
                bodyContent = limparTexto(bodyContent);

                // Adiciona ao resultado com um espaço entre comentários
                if (bodyContent.length() > 0) {
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(bodyContent);
                }

                // Avança o índice para o próximo "body"
                startIndex = endIndex;
            }

            if (result.length() == 0) {
                throw new SQLException("Nenhum campo 'body' encontrado no JSON");
            }

            return result.toString();
        } catch (Exception e) {
            throw new SQLException("Erro ao processar o JSON: " + e.getMessage());
        }
    }

    public static void UpdatePlsHistLong(Clob entrada, int chave) throws SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = new OracleDriver().defaultConnection();

            // Extrai e limpa todos os campos "body"
            String texto = extractAndCleanAllBodies(entrada);

            String sql = "UPDATE PLS_ATENDIMENTO_HISTORICO PLH " +
                        "SET PLH.DS_HISTORICO_LONG = ? " +
                        "WHERE PLH.NR_SEQUENCIA = ?";

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, texto);
            pstmt.setInt(2, chave);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) {}
            }
            if (con != null) {
                try { con.close(); } catch (SQLException e) {}
            }
        }
    }

    public static void UpdateSacBoletimOcorrencia(Clob entrada, int chave) throws SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = new OracleDriver().defaultConnection();

            // Extrai e limpa todos os campos "body"
            String texto = extractAndCleanAllBodies(entrada);

            String sql = "UPDATE SAC_BOLETIM_OCORRENCIA SBO " +
                        "SET SBO.DS_OCORRENCIA_LONGA = ? " +
                        "WHERE SBO.NR_SEQUENCIA = ?";

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, texto);
            pstmt.setInt(2, chave);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) {}
            }
            if (con != null) {
                try { con.close(); } catch (SQLException e) {}
            }
        }
    }
}
