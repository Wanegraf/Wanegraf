import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFReaderRename {

    public static Map<String, String> eolicas = new HashMap<>();

    static {
        eolicas.put("12053787000210", "SANTA MARIA");
        eolicas.put("12053929000249", "SANTA HELENA");
        eolicas.put("14583703000102", "SANTO URIEL");
        eolicas.put("21216892000132", "SÃO BENTO DO NORTE I");
        eolicas.put("21216877000194", "SÃO BENTO DO NORTE II");
        eolicas.put("21216857000113", "SÃO BENTO DO NORTE III");
        eolicas.put("21216915000109", "SÃO MIGUEL I");
        eolicas.put("21216925000144", "SÃO MIGUEL II");
        eolicas.put("21216439000126", "SÃO MIGUEL III");
        eolicas.put("21957870000123", "GUAJIRU");
        eolicas.put("21957722000109", "JANGADA");
        eolicas.put("21957968000180", "POTIGUAR");
        eolicas.put("21917808000108", "CUTIA");
        eolicas.put("21909793000136", "MARIA HELENA");
        eolicas.put("21916951000185", "ESPERANÇA DO NORDESTE");
        eolicas.put("21909032000184", "PARAÍSO DOS VENTOS DO NORDESTE");
        eolicas.put("12723413000183", "BOA VISTA");
        eolicas.put("12723335000117", "FAROL");
        eolicas.put("12723444000134", "OLHO D'ÁGUA");
        eolicas.put("12723384000150", "SÃO BENTO DO NORTE");
        eolicas.put("12802855000115", "ASA BRANCA I");
        eolicas.put("12802844000135", "ASA BRANCA II");
        eolicas.put("12802835000144", "ASA BRANCA III");
        eolicas.put("12802866000103", "NOVA EURUS IV");
        eolicas.put("35742218000104", "FDA");
        eolicas.put("31449173000115", "VILA MARANHÃO III");
        eolicas.put("30097726000155", "VILA MARANHÃO I");
        eolicas.put("31004703000111", "VILA MARANHÃO II");
        eolicas.put("31478575000148", "VILA CEARÁ I");
        eolicas.put("34109229000180", "VILA MATO GROSSO I");
        eolicas.put("35823538000261", "JANDAIRA I");
        eolicas.put("35823536000272", "JANDAIRA III");
        eolicas.put("35823577000269", "JANDAIRA IV");
        eolicas.put("31478575000148", "VILA PARAIBA IV");
        eolicas.put("04370282000170", "COPEL");
    }

    public static void main(String[] args) {
        ReadPdf();
    }

    static String getText(File pdfFile) throws IOException {
        PDDocument doc = PDDocument.load(pdfFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String PdfText = stripper.getText(doc);
        doc.close();
        return PdfText;
    }

    private static void ReadPdf() {
        AtomicInteger count = new AtomicInteger();
        try {
            Files.walk(Paths.get(System.getProperty("user.dir")))
                    .filter(p -> p.toString().toUpperCase().endsWith(".PDF"))
                    .forEach(p -> {
                        try {
                            String text = getText(p.toFile());
                            Pattern pattern = Pattern.compile(
                                    "(.*)(\\d{5}[.\\s]*\\d{5}[.\\s]*\\d{5}[.\\s]*\\d{6}[.\\s]*\\d{5}[.\\s]*\\d{6}[.\\s]*\\d[.\\s]*\\d{14})(.*)",
                                    Pattern.DOTALL);
                            Matcher matcher = pattern.matcher(text);

                            String textNumbers = text.replaceAll("[^0-9]", "");
                            String cnpjRegex = String.join("|", eolicas.keySet());
                            Pattern cnpjPattern = Pattern.compile(cnpjRegex, Pattern.DOTALL);
                            Matcher cnpjMatcher = cnpjPattern.matcher(textNumbers);

                            if (matcher.matches()) {
                                String codigo = matcher.group(2);
                                String numerocodigo = codigo.replaceAll("[^0-9]+", "");
                                String valornumero = numerocodigo.substring(numerocodigo.length() - 10)
                                        .replaceFirst("^0+(?!$)", "");
                                String valor = new StringBuilder(valornumero).insert(valornumero.length() - 2, ",")
                                        .toString();

                                String eolicaText = "";
                                if (cnpjMatcher.find()) {
                                    String cnpj = cnpjMatcher.group();
                                    String eolica = eolicas.get(cnpj);
                                    if (eolica != null) {
                                        eolicaText = eolica + " - ";
                                    }
                                }

                                String oldPath = p.toAbsolutePath().toString();
                                String newPath = oldPath.replaceFirst(Pattern.quote(p.getFileName().toString()),
                                        eolicaText + valor + ".pdf");
                                if (oldPath.compareTo(newPath) != 0) {
                                    count.getAndIncrement();
                                    System.out.println(count + ": " + oldPath + " -> " + newPath + "\n");
                                    RenamePdf(oldPath, newPath);
                                }
                            } else {
                                Pattern patternNota =
                                        Pattern.compile("([\\d.]*\\d+[,]\\d{2})(?!\\s*%)", Pattern.DOTALL);
                                Matcher matcherNota = patternNota.matcher(text);
                                double valorNota = 0.00;
                                String valorTotal = "";
                                while (matcherNota.find()) {
                                    String valorSemPonto = matcherNota.group(1).replaceAll("\\.", "");
                                    double valorEncontrado = Double.parseDouble(valorSemPonto.replaceAll(",", "."));
                                    if (valorEncontrado > valorNota) {
                                        valorNota = valorEncontrado;
                                        valorTotal = valorSemPonto;
                                    }
                                }
                                if (valorNota > 0) {
                                    String oldPath = p.toAbsolutePath().toString();
                                    String oldName = p.getFileName().toString();
                                    String newPath = oldPath.replaceFirst(oldName, valorTotal + ".pdf");
                                    if (oldPath.compareTo(newPath) != 0) {
                                        count.getAndIncrement();
                                        System.out.println(count + ": " + oldPath + " -> " + newPath + "\n");
                                        RenamePdf(oldPath, newPath);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TOTAL: " + count);
    }

    private static void RenamePdf(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);

        int count = 1;
        while (newFile.exists()) {
            newFile = new File(new StringBuilder(newPath).insert(newPath.length() - 4, "-" + count).toString());
            count++;
        }

        try {
            Files.move(oldFile.toPath(), newFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
