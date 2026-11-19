package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.model.AppUser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlateDefectImportService {

    private final PlateDefectService plateDefectService;

    public PlateDefectImportService(PlateDefectService plateDefectService) {
        this.plateDefectService = plateDefectService;
    }

    @Transactional
    public PlateDefectImportResult importFromExcel(MultipartFile file, AppUser currentUser) throws IOException {
        PlateDefectImportResult result = new PlateDefectImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.addError(0, "Nenhuma planilha encontrada no arquivo.");
                return result;
            }

            int rowCount = sheet.getPhysicalNumberOfRows();
            if (rowCount < 2) {
                result.addError(0, "Planilha vazia ou sem linhas de dados (necessario cabecalho + pelo menos 1 linha).");
                return result;
            }

            Row headerRow = sheet.getRow(0);
            Map<String, List<Integer>> columnMap = buildColumnMap(headerRow);

            validateRequiredColumns(columnMap, result);
            if (result.hasErrors()) {
                return result;
            }

            int dataRowCount = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) continue;

                int rowNumber = i + 1;

                try {
                    LocalDate entryDate = readDate(row, columnMap, "DATA");
                    String opCode = readString(row, columnMap, "OP");
                    String clientName = readString(row, columnMap, "CLIENTE");

                    if (isPlaceholder(opCode) || isPlaceholder(clientName)) {
                        continue;
                    }

                    dataRowCount++;
                    Integer lossQuantity = readInteger(row, columnMap, "PERDA");
                    String defectCode = readString(row, columnMap, "CODIGO");
                    String shiftLabel = readString(row, columnMap, "TURNO");
                    String machineLabel = readString(row, columnMap, "MAQUINA");
                    String operatorLabel = readString(row, columnMap, "OPERADOR");
                    String plateLabel = readString(row, columnMap, "CHAPA");
                    String ctpLabel = readString(row, columnMap, "CTP");
                    String sector = readString(row, columnMap, "SETOR");
                    String observations = readStringNullable(row, columnMap, "OBSERVACOES");

                    plateDefectService.createByLabels(
                            entryDate, opCode, clientName, lossQuantity,
                            defectCode, shiftLabel, machineLabel, operatorLabel,
                            plateLabel, ctpLabel, sector, observations, currentUser
                    );
                    result.incrementSuccess();
                } catch (Exception e) {
                    result.addError(rowNumber, e.getMessage());
                }
            }

            result.setTotalRows(dataRowCount);
        }

        return result;
    }

    private Map<String, List<Integer>> buildColumnMap(Row headerRow) {
        Map<String, List<Integer>> map = new HashMap<>();
        if (headerRow == null) return map;

        DataFormatter formatter = new DataFormatter();
        for (int col = 0; col < headerRow.getLastCellNum(); col++) {
            Cell cell = headerRow.getCell(col);
            String raw = formatter.formatCellValue(cell);
            String normalized = normalizeHeader(raw);
            if (!normalized.isEmpty()) {
                map.computeIfAbsent(normalized, k -> new ArrayList<>()).add(col);
            }
        }
        return map;
    }

    private String normalizeHeader(String raw) {
        if (raw == null) return "";
        String upper = raw.trim().toUpperCase();
        upper = upper.replaceAll("[ÁÀÂÃ]", "A");
        upper = upper.replaceAll("[ÉÈÊ]", "E");
        upper = upper.replaceAll("[ÍÌÎ]", "I");
        upper = upper.replaceAll("[ÓÒÔÕ]", "O");
        upper = upper.replaceAll("[ÚÙÛ]", "U");
        upper = upper.replaceAll("Ç", "C");
        upper = upper.replaceAll("[^A-Z0-9 ]", "");
        upper = upper.replaceAll("\\s+", " ").trim();

        if (upper.equals("DATA") || upper.equals("DATA HORARIO") || upper.equals("DATA DO LANCAMENTO") || upper.equals("ENTRY DATE")) return "DATA";
        if (upper.equals("OP") || upper.equals("CODIGO OP") || upper.equals("OP CODE") || upper.equals("NUMERO OP") || upper.equals("OPCODE")) return "OP";
        if (upper.equals("CLIENTE") || upper.equals("NOME CLIENTE") || upper.equals("CLIENTE NAME") || upper.equals("CLIENT NAME")) return "CLIENTE";
        if (upper.equals("PERDA") || upper.equals("QTDE PERDA") || upper.equals("QUANTIDADE PERDA") || upper.equals("LOSS QUANTITY") || upper.equals("LOSS") || upper.equals("QTD PERDA") || upper.equals("QT PERDA") || upper.equals("PERCA")) return "PERDA";
        if (upper.equals("CODIGO") || upper.equals("CODIGO OCORRENCIA") || upper.equals("DEFECT CODE") || upper.equals("COD") || upper.equals("CODIGO DEFEITO") || upper.equals("COD DEFEITO")) return "CODIGO";
        if (upper.equals("TURNO") || upper.equals("SHIFT")) return "TURNO";
        if (upper.equals("MAQUINA") || upper.equals("MACHINE") || upper.equals("MAQ")) return "MAQUINA";
        if (upper.equals("OPERADOR") || upper.equals("NOME OPERADOR") || upper.equals("OPERATOR") || upper.equals("OPERADOR NAME") || upper.equals("NOME")) return "OPERADOR";
        if (upper.equals("CHAPA") || upper.equals("TIPO CHAPA") || upper.equals("PLATE")) return "CHAPA";
        if (upper.equals("CTP")) return "CTP";
        if (upper.equals("SETOR") || upper.equals("SECTOR") || upper.equals("SETORES")) return "SETOR";
        if (upper.equals("OBSERVACOES") || upper.equals("OBS") || upper.equals("OBSERVACAO") || upper.equals("OBSERVATIONS") || upper.equals("NOTES")) return "OBSERVACOES";

        return upper;
    }

    private void validateRequiredColumns(Map<String, List<Integer>> columnMap, PlateDefectImportResult result) {
        String[] required = {"DATA", "OP", "CLIENTE", "PERDA", "CODIGO", "TURNO", "MAQUINA", "OPERADOR", "CHAPA", "CTP", "SETOR"};
        for (String col : required) {
            if (!columnMap.containsKey(col)) {
                result.addError(0, "Coluna obrigatoria nao encontrada no cabecalho: " + col);
            }
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = cell.toString().trim();
                if (!v.isEmpty()) return false;
            }
        }
        return true;
    }

    private List<Integer> getColumns(Map<String, List<Integer>> map, String key) {
        List<Integer> cols = map.get(key);
        if (cols == null || cols.isEmpty()) return Collections.emptyList();
        List<Integer> reversed = new ArrayList<>(cols);
        Collections.reverse(reversed);
        return reversed;
    }

    private LocalDate readDate(Row row, Map<String, List<Integer>> map, String key) {
        for (int col : getColumns(map, key)) {
            Cell cell = row.getCell(col);
            if (cell == null) continue;

            try {
                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    LocalDate val = cell.getDateCellValue()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    if (val != null) return val;
                }
                if (cell.getCellType() == CellType.NUMERIC) {
                    double val = cell.getNumericCellValue();
                    LocalDate parsed = DateUtil.getJavaDate(val)
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    if (parsed != null) return parsed;
                }
                if (cell.getCellType() == CellType.STRING) {
                    String text = cell.getStringCellValue().trim();
                    if (text.isEmpty()) continue;
                    text = text.split(" ")[0];
                    LocalDate parsed = null;
                    if (text.contains("/")) {
                        String[] parts = text.split("/");
                        if (parts.length == 3) {
                            int d = Integer.parseInt(parts[0]);
                            int m = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            if (y < 100) y += 2000;
                            parsed = LocalDate.of(y, m, d);
                        }
                    } else if (text.contains("-")) {
                        String[] parts = text.split("-");
                        if (parts.length == 3) {
                            int y = Integer.parseInt(parts[0]);
                            int m = Integer.parseInt(parts[1]);
                            int d = Integer.parseInt(parts[2]);
                            parsed = LocalDate.of(y, m, d);
                        }
                    }
                    if (parsed != null) return parsed;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String readString(Row row, Map<String, List<Integer>> map, String key) {
        DataFormatter formatter = new DataFormatter();
        for (int col : getColumns(map, key)) {
            Cell cell = row.getCell(col);
            if (cell == null) continue;
            String value = formatter.formatCellValue(cell).trim();
            if (!value.isEmpty()) return value;
        }
        return null;
    }

    private String readStringNullable(Row row, Map<String, List<Integer>> map, String key) {
        if (!map.containsKey(key)) return null;
        return readString(row, map, key);
    }

    private Integer readInteger(Row row, Map<String, List<Integer>> map, String key) {
        for (int col : getColumns(map, key)) {
            Cell cell = row.getCell(col);
            if (cell == null) continue;

            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return (int) Math.round(cell.getNumericCellValue());
                }
                if (cell.getCellType() == CellType.STRING) {
                    String text = cell.getStringCellValue().trim();
                    if (text.isEmpty()) continue;
                    text = text.replace(".", "").replace(",", "");
                    return Integer.parseInt(text);
                }
                if (cell.getCellType() == CellType.FORMULA) {
                    return (int) Math.round(cell.getNumericCellValue());
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isPlaceholder(String value) {
        if (value == null) return true;
        String s = value.trim().toUpperCase();
        return s.isEmpty()
                || s.equals("0")
                || s.equals("N/A")
                || s.equals("NULL")
                || s.equals("-")
                || s.equals("—");
    }
}
