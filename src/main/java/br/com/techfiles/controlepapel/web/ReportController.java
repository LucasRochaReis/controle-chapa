package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.model.PlateDefectEntry;
import br.com.techfiles.controlepapel.model.PlateProductionEntry;
import br.com.techfiles.controlepapel.repo.PlateDefectEntryRepository;
import br.com.techfiles.controlepapel.repo.PlateProductionEntryRepository;
import br.com.techfiles.controlepapel.service.AppUserService;
import br.com.techfiles.controlepapel.service.LookupService;
import br.com.techfiles.controlepapel.service.PlateDefectService;
import br.com.techfiles.controlepapel.service.PlateProductionService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/relatorios")
public class ReportController {

    private static final DateTimeFormatter MES_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM / yyyy", new Locale("pt", "BR"));
    private static final DateTimeFormatter MES_VALOR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int DASH_RECENTES_LIMITE = 10;
    private static final DateTimeFormatter DASH_DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final YearMonth PISO_MINIMO_MES = YearMonth.of(2025, 3);
    private static final LocalDate PISO_MINIMO_DATA = PISO_MINIMO_MES.atDay(1);

    private final PlateProductionEntryRepository productionRepo;
    private final PlateDefectEntryRepository defectRepo;
    private final PlateProductionService plateProductionService;
    private final PlateDefectService plateDefectService;
    private final LookupService lookupService;
    private final AppUserService appUserService;

    public ReportController(
            PlateProductionEntryRepository productionRepo,
            PlateDefectEntryRepository defectRepo,
            PlateProductionService plateProductionService,
            PlateDefectService plateDefectService,
            LookupService lookupService,
            AppUserService appUserService
    ) {
        this.productionRepo = productionRepo;
        this.defectRepo = defectRepo;
        this.plateProductionService = plateProductionService;
        this.plateDefectService = plateDefectService;
        this.lookupService = lookupService;
        this.appUserService = appUserService;
    }

    @ModelAttribute("shiftOptions")
    public Object shiftOptions() {
        return lookupService.listActive(LookupCategory.SHIFT);
    }

    @ModelAttribute("machineOptions")
    public Object machineOptions() {
        return lookupService.listActive(LookupCategory.MACHINE);
    }

    @ModelAttribute("ctpOptions")
    public Object ctpOptions() {
        return lookupService.listActive(LookupCategory.CTP);
    }

    @ModelAttribute("kindOptions")
    public ReportEntryKind[] kindOptions() {
        return ReportEntryKind.values();
    }

    @GetMapping
    public String page(
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "shiftId", required = false) Long shiftId,
            @RequestParam(name = "machineId", required = false) Long machineId,
            @RequestParam(name = "kind", required = false) String kind,
            @RequestParam(name = "ctp", required = false) String ctp,
            @RequestParam(name = "opCode", required = false) String opCode,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "100") Integer size,
            @RequestParam(name = "gerar", required = false) Boolean gerar,
            Authentication authentication,
            HttpServletRequest request,
            Model model
    ) {
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        ReportEntryKind entryKind = ReportEntryKind.fromParam(kind);

        if (dateFrom != null && dateFrom.isBefore(PISO_MINIMO_DATA)) dateFrom = PISO_MINIMO_DATA;
        if (dateTo != null && dateTo.isBefore(PISO_MINIMO_DATA)) dateTo = PISO_MINIMO_DATA;
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) dateFrom = dateTo;

        int p = 0;
        int sz = 100;
        if (page != null && page >= 0) p = page;
        if (size != null) {
            if (size < 1) sz = 1;
            else sz = Math.min(size, 500);
        }

        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("shiftId", shiftId);
        model.addAttribute("machineId", machineId);
        model.addAttribute("kind", entryKind.name());
        model.addAttribute("ctp", ctp);
        model.addAttribute("opCode", opCode);
        model.addAttribute("pageTitle", "Relatorios");
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("reportGenerated", Boolean.TRUE.equals(gerar));

        if (!Boolean.TRUE.equals(gerar)) {
            model.addAttribute("entries", new ArrayList<HistoryEntryView>());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("plateProductionCount", 0);
            model.addAttribute("plateDefectCount", 0);
            model.addAttribute("totalPlateQuantity", 0L);
            model.addAttribute("totalPlateLoss", 0L);
            model.addAttribute("selectedKindLabel", entryKind.getLabel());
            model.addAttribute("excelUrl", "");
            model.addAttribute("showPlateProductionStats", entryKind == ReportEntryKind.ALL || entryKind == ReportEntryKind.PLATE_PRODUCTION);
            model.addAttribute("showPlateDefectStats", entryKind == ReportEntryKind.ALL || entryKind == ReportEntryKind.PLATE_DEFECT);
            model.addAttribute("currentPage", 0);
            model.addAttribute("pageSize", sz);
            model.addAttribute("totalPages", 1);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("hasNext", false);
            model.addAttribute("startEntry", 0L);
            model.addAttribute("endEntry", 0L);
            return "reports/index";
        }

        ReportResult result = buildReport(currentUser, dateFrom, dateTo, shiftId, machineId, entryKind, ctp, opCode);
        List<HistoryEntryView> allEntries = result.entries();
        long totalCount = allEntries.size();
        int totalPages = (int) Math.max(1, Math.ceil((double) totalCount / sz));
        if (p >= totalPages) p = totalPages - 1;
        int fromIdx = p * sz;
        int toIdx = Math.min(fromIdx + sz, (int) totalCount);
        List<HistoryEntryView> pageEntries = (totalCount == 0 || fromIdx >= totalCount)
                ? new ArrayList<>()
                : new ArrayList<>(allEntries.subList(fromIdx, toIdx));

        model.addAttribute("entries", pageEntries);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("plateProductionCount", result.plateProductionCount());
        model.addAttribute("plateDefectCount", result.plateDefectCount());
        model.addAttribute("totalPlateQuantity", result.totalPlateQuantity());
        model.addAttribute("totalPlateLoss", result.totalPlateLoss());
        model.addAttribute("selectedKindLabel", entryKind.getLabel());
        model.addAttribute("generatedAt", LocalDateTime.now());
        model.addAttribute("excelUrl", buildExcelUrl(request, dateFrom, dateTo, shiftId, machineId, entryKind, ctp, opCode));
        model.addAttribute("showPlateProductionStats", entryKind == ReportEntryKind.ALL || entryKind == ReportEntryKind.PLATE_PRODUCTION);
        model.addAttribute("showPlateDefectStats", entryKind == ReportEntryKind.ALL || entryKind == ReportEntryKind.PLATE_DEFECT);

        model.addAttribute("currentPage", p);
        model.addAttribute("pageSize", sz);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrevious", p > 0);
        model.addAttribute("hasNext", p + 1 < totalPages);
        model.addAttribute("startEntry", totalCount == 0 ? 0L : (long) fromIdx + 1);
        model.addAttribute("endEntry", (long) toIdx);
        List<Integer> paginationWindow = buildPaginationWindow(p, totalPages);
        model.addAttribute("paginationWindow", paginationWindow);
        model.addAttribute("firstPageEllipsis", totalPages > 7 && !paginationWindow.isEmpty() && paginationWindow.get(0) > 1);
        model.addAttribute("lastPageEllipsis", totalPages > 7 && !paginationWindow.isEmpty() && paginationWindow.get(paginationWindow.size() - 1) < totalPages - 2);
        model.addAttribute("showLastPage", totalPages > 1 && !paginationWindow.isEmpty() && paginationWindow.get(paginationWindow.size() - 1) < totalPages - 1);
        return "reports/index";
    }

    private List<Integer> buildPaginationWindow(int currentPage, int totalPages) {
        List<Integer> out = new ArrayList<>();
        int window = 3;
        int start = Math.max(0, currentPage - window);
        int end = Math.min(totalPages - 1, currentPage + window);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }

    @GetMapping("/excel")
    public void excel(
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "shiftId", required = false) Long shiftId,
            @RequestParam(name = "machineId", required = false) Long machineId,
            @RequestParam(name = "kind", required = false) String kind,
            @RequestParam(name = "ctp", required = false) String ctp,
            @RequestParam(name = "opCode", required = false) String opCode,
            Authentication authentication,
            jakarta.servlet.http.HttpServletResponse response
    ) throws IOException {
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        ReportEntryKind entryKind = ReportEntryKind.fromParam(kind);

        if (dateFrom != null && dateFrom.isBefore(PISO_MINIMO_DATA)) dateFrom = PISO_MINIMO_DATA;
        if (dateTo != null && dateTo.isBefore(PISO_MINIMO_DATA)) dateTo = PISO_MINIMO_DATA;
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) dateFrom = dateTo;

        ReportResult result = buildReport(currentUser, dateFrom, dateTo, shiftId, machineId, entryKind, ctp, opCode);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-controle-chapa.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Relatorio");
            createExcelContent(workbook, sheet, result, dateFrom, dateTo, entryKind, opCode);
            workbook.write(response.getOutputStream());
        }
    }

    private ReportResult buildReport(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            ReportEntryKind kind,
            String ctp,
            String opCode
    ) {
        List<HistoryEntryView> history = new ArrayList<>();

        boolean includePlateProduction = kind == ReportEntryKind.ALL || kind == ReportEntryKind.PLATE_PRODUCTION;
        boolean includePlateDefect = kind == ReportEntryKind.ALL || kind == ReportEntryKind.PLATE_DEFECT;

        int plateProductionCount = 0;
        int plateDefectCount = 0;
        long totalPlateQuantity = 0;
        long totalPlateLoss = 0;

        if (includePlateProduction) {
            List<PlateProductionEntry> entries = plateProductionService.search(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, null);
            for (PlateProductionEntry entry : entries) {
                if (!matchesCtp(entry.getCtp(), ctp)) continue;
                plateProductionCount++;
                history.add(toHistoryView(entry));
                if (entry.getQuantity() != null) {
                    totalPlateQuantity += entry.getQuantity();
                }
            }
        }

        if (includePlateDefect) {
            List<PlateDefectEntry> entries = plateDefectService.search(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, null);
            for (PlateDefectEntry entry : entries) {
                if (!matchesCtp(entry.getCtp(), ctp)) continue;
                plateDefectCount++;
                history.add(toHistoryView(entry));
                if (entry.getLossQuantity() != null) {
                    totalPlateLoss += entry.getLossQuantity();
                }
            }
        }

        history.sort(Comparator
                .comparing(HistoryEntryView::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HistoryEntryView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return new ReportResult(history, history.size(),
                plateProductionCount, plateDefectCount,
                totalPlateQuantity, totalPlateLoss);
    }

    private boolean matchesCtp(String entryCtp, String filterCtp) {
        if (filterCtp == null || filterCtp.trim().isBlank()) return true;
        if (entryCtp == null || entryCtp.isBlank()) return false;
        return entryCtp.trim().equalsIgnoreCase(filterCtp.trim());
    }

    private HistoryEntryView toHistoryView(PlateProductionEntry entry) {
        return new HistoryEntryView(
                "Lançamento",
                entry.getEntryDate(),
                entry.getCreatedAt(),
                entry.getShift().getLabel(),
                entry.getMachine() != null ? entry.getMachine().getLabel() : "-",
                entry.getOpCode(),
                entry.getClientName(),
                entry.getQuantity(),
                entry.getOperatorName(),
                entry.getPlate(),
                entry.getCtp(),
                null,
                null,
                null,
                null,
                entry.getObservations()
        );
    }

    private String resolveDefectDescription(String defectNumber) {
        if (defectNumber == null || defectNumber.isBlank()) return null;
        String prefix = defectNumber.trim() + " - ";
        return lookupService.listActive(LookupCategory.DEFECT_CODE).stream()
                .filter(opt -> opt.getLabel() != null && opt.getLabel().startsWith(prefix))
                .map(opt -> {
                    String[] parts = opt.getLabel().split(" - ", 2);
                    return parts.length >= 2 ? parts[1].trim() : opt.getLabel();
                })
                .findFirst()
                .orElse(null);
    }

    private HistoryEntryView toHistoryView(PlateDefectEntry entry) {
        return new HistoryEntryView(
                "Ocorrência",
                entry.getEntryDate(),
                entry.getCreatedAt(),
                entry.getShift().getLabel(),
                entry.getMachine() != null ? entry.getMachine().getLabel() : "-",
                entry.getOpCode(),
                entry.getClientName(),
                null,
                entry.getOperatorName(),
                entry.getPlate(),
                entry.getCtp(),
                entry.getLossQuantity(),
                entry.getDefectCode(),
                resolveDefectDescription(entry.getDefectCode()),
                entry.getSector(),
                null,
                entry.getObservations()
        );
    }

    private void createExcelContent(
            XSSFWorkbook workbook,
            Sheet sheet,
            ReportResult result,
            LocalDate dateFrom,
            LocalDate dateTo,
            ReportEntryKind kind,
            String opCode
    ) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);

        String[] headers;
        switch (kind) {
            case PLATE_DEFECT:
                headers = new String[]{
                        "Data/Horario",
                        "OP",
                        "Cliente",
                        "Perca",
                        "Codigo",
                        "Descricao ocorrencia",
                        "Maquina",
                        "Operador",
                        "Turno",
                        "Chapa",
                        "Setor",
                        "CTP",
                        "Observacao"
                };
                break;
            case PLATE_PRODUCTION:
                headers = new String[]{
                        "Data/Horario",
                        "OP",
                        "Cliente",
                        "Quantidade",
                        "Maquina",
                        "Operador",
                        "Turno",
                        "Chapa",
                        "CTP",
                        "Observacao"
                };
                break;
            case ALL:
            default:
                headers = new String[]{
                        "Data/Horario",
                        "Tipo",
                        "Turno",
                        "Maquina",
                        "OP",
                        "Cliente",
                        "Operador",
                        "Quantidade",
                        "Perca",
                        "Codigo",
                        "Descricao ocorrencia",
                        "Setor",
                        "Chapa",
                        "CTP",
                        "Observacao"
                };
                break;
        }

        int rowIndex = 0;
        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        for (HistoryEntryView entry : result.entries()) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;
            switch (kind) {
                case PLATE_DEFECT:
                    row.createCell(col++).setCellValue(formatDateTime(entry));
                    row.createCell(col++).setCellValue(defaultValue(entry.getOpCode()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getClientName()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getLossQuantity()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getDefectCode()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getDefectDescription()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getMachineLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getOperatorName()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getShiftLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getPlate()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getSector()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getCtp()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getObservations()));
                    row.getCell(3).setCellStyle(numberStyle);
                    break;
                case PLATE_PRODUCTION:
                    row.createCell(col++).setCellValue(formatDateTime(entry));
                    row.createCell(col++).setCellValue(defaultValue(entry.getOpCode()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getClientName()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getQuantity()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getMachineLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getOperatorName()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getShiftLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getPlate()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getCtp()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getObservations()));
                    row.getCell(3).setCellStyle(numberStyle);
                    break;
                case ALL:
                default:
                    row.createCell(col++).setCellValue(formatDateTime(entry));
                    row.createCell(col++).setCellValue(defaultValue(entry.getEntryKindLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getShiftLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getMachineLabel()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getOpCode()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getClientName()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getOperatorName()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getQuantity()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getLossQuantity()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getDefectCode()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getDefectDescription()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getSector()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getPlate()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getCtp()));
                    row.createCell(col++).setCellValue(defaultValue(entry.getObservations()));
                    row.getCell(7).setCellStyle(numberStyle);
                    row.getCell(8).setCellStyle(numberStyle);
                    break;
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 700, 12000));
        }
    }

    private String buildExcelUrl(
            HttpServletRequest request,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            ReportEntryKind entryKind,
            String ctp,
            String opCode
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getContextPath() + "/relatorios/excel")
                .queryParam("kind", entryKind.name());
        if (dateFrom != null) {
            builder.queryParam("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            builder.queryParam("dateTo", dateTo);
        }
        if (shiftId != null) {
            builder.queryParam("shiftId", shiftId);
        }
        if (machineId != null) {
            builder.queryParam("machineId", machineId);
        }
        if (!isBlank(ctp)) {
            builder.queryParam("ctp", ctp);
        }
        if (!isBlank(opCode)) {
            builder.queryParam("opCode", opCode);
        }
        return builder.build().encode().toUriString();
    }

    private String formatDateTime(HistoryEntryView entry) {
        String date = entry.getEntryDate() != null ? entry.getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
        String time = entry.getCreatedAt() != null ? entry.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "-";
        return date + " " + time;
    }

    private String defaultValue(String value) {
        return isBlank(value) ? "-" : value;
    }

    private String defaultValue(Number value) {
        return value == null ? "-" : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @GetMapping("/controle-mensal")
    public String controleMensal(
            @RequestParam(value = "mes", required = false) String mes,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication,
            Model model
    ) {
        boolean modoIntervalo = false;
        LocalDate dataInicio;
        LocalDate dataFim;
        String mesValor = null;
        String nomeMesAno;

        if (dateFrom != null || dateTo != null) {
            modoIntervalo = true;
            LocalDate df = dateFrom == null ? PISO_MINIMO_DATA : dateFrom;
            LocalDate dt = dateTo == null ? LocalDate.now() : dateTo;
            if (df.isBefore(PISO_MINIMO_DATA)) df = PISO_MINIMO_DATA;
            if (dt.isBefore(PISO_MINIMO_DATA)) dt = PISO_MINIMO_DATA;
            if (df.isAfter(dt)) df = dt;
            dataInicio = df;
            dataFim = dt;
            mesValor = "";
            nomeMesAno = dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " at\u00e9 " + dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else {
            YearMonth ym = resolverMes(mes, authentication);
            dataInicio = ym.atDay(1);
            dataFim = ym.atEndOfMonth();
            mesValor = ym.format(MES_VALOR_FORMATTER);
            nomeMesAno = capitalizarMes(ym.format(MES_LABEL_FORMATTER));
        }

        List<PlateProductionEntry> producoes = productionRepo.findAll((Specification<PlateProductionEntry>) (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dataInicio));
            preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), dataFim));
            return cb.and(preds.toArray(new Predicate[0]));
        });

        List<PlateDefectEntry> ocorrencias = defectRepo.findAll((Specification<PlateDefectEntry>) (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dataInicio));
            preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), dataFim));
            return cb.and(preds.toArray(new Predicate[0]));
        });

        DecimalFormatSymbols symPtBr = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symPtBr.setGroupingSeparator('.');
        symPtBr.setDecimalSeparator(',');
        DecimalFormat fmtInteiro = new DecimalFormat("#,##0", symPtBr);
        fmtInteiro.setGroupingUsed(true);
        DecimalFormat fmtPercentual3 = new DecimalFormat("#,##0.000", symPtBr);
        fmtPercentual3.setGroupingUsed(true);

        MonthlyDefectSummary defectSummary = buildMonthlyDefectSummary(ocorrencias, fmtInteiro);
        long quantSemPerda = producoes.stream().mapToLong(p -> p.getQuantity() == null ? 0L : p.getQuantity()).sum();
        long perda = defectSummary.totalPerdaGeral();
        long soma = quantSemPerda + perda;
        BigDecimal pctPerda = percentual3Casas(perda, quantSemPerda);

        MonthlyControlData resumo = new MonthlyControlData(
                nomeMesAno,
                mesValor,
                quantSemPerda, fmtInteiro.format(quantSemPerda),
                perda, fmtInteiro.format(perda),
                soma, fmtInteiro.format(soma),
                pctPerda, fmtPercentual3.format(pctPerda)
        );

        List<MonthlyDefectCodeRow> tabelaCodigos = defectSummary.rows();
        String totalPerdasGeralFmt = defectSummary.totalPerdaGeralFmt();
        long maxLinhaTabela = defectSummary.maxLinhaTabela();

        List<DashboardMonthOption> opcoesMeses = montarOpcoesMeses();
        boolean isGestorOuAdmin = isGestorOuAdmin(authentication);
        boolean isVisualizador = isVisualizador(authentication);
        boolean permiteRelatorios = isGestorOuAdmin || isVisualizador || isUsuario(authentication);

        model.addAttribute("resumo", resumo);
        model.addAttribute("tabelaCodigos", tabelaCodigos);
        model.addAttribute("totalPerdasGeralFmt", totalPerdasGeralFmt);
        model.addAttribute("maxLinhaTabela", maxLinhaTabela);
        model.addAttribute("opcoesMeses", opcoesMeses);
        model.addAttribute("isGestorOuAdmin", isGestorOuAdmin);
        model.addAttribute("isVisualizador", isVisualizador);
        model.addAttribute("permiteRelatorios", permiteRelatorios);
        model.addAttribute("modoIntervalo", modoIntervalo);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("mes", mesValor);
        model.addAttribute("pageTitle", "Controle Mensal");
        model.addAttribute("activeMenu", "controle-mensal");
        String anoStr;
        String periodoCurto;
        if (modoIntervalo) {
            anoStr = dataInicio.format(DateTimeFormatter.ofPattern("yyyy")) +
                    (dataInicio.getYear() != dataFim.getYear() ? "\u2013" + dataFim.format(DateTimeFormatter.ofPattern("yyyy")) : "");
            periodoCurto = nomeMesAno;
        } else {
            YearMonth ym = YearMonth.parse(mesValor);
            anoStr = String.valueOf(ym.getYear());
            periodoCurto = capitalizarMes(ym.format(DateTimeFormatter.ofPattern("MMMM", new Locale("pt", "BR"))));
        }
        model.addAttribute("anoStr", anoStr);
        model.addAttribute("periodoCurto", periodoCurto);
        return "reports/controle-mensal";
    }

    @GetMapping("/controle-mensal/excel")
    public void controleMensalExcel(
            @RequestParam(value = "mes", required = false) String mes,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication,
            HttpServletResponse response
    ) throws IOException {
        LocalDate dataInicio;
        LocalDate dataFim;
        String nomeMesAno;
        String apenasMes;
        String anoStr;
        String sufixoArquivo;

        if (dateFrom != null || dateTo != null) {
            LocalDate df = dateFrom == null ? PISO_MINIMO_DATA : dateFrom;
            LocalDate dt = dateTo == null ? LocalDate.now() : dateTo;
            if (df.isBefore(PISO_MINIMO_DATA)) df = PISO_MINIMO_DATA;
            if (dt.isBefore(PISO_MINIMO_DATA)) dt = PISO_MINIMO_DATA;
            if (df.isAfter(dt)) df = dt;
            dataInicio = df;
            dataFim = dt;
            nomeMesAno = dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " a " + dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            apenasMes = nomeMesAno;
            anoStr = dataInicio.format(DateTimeFormatter.ofPattern("yyyy")) + "-" + dataFim.format(DateTimeFormatter.ofPattern("yyyy"));
            sufixoArquivo = dataInicio.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-a-" + dataFim.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else {
            YearMonth ym = resolverMes(mes, authentication);
            dataInicio = ym.atDay(1);
            dataFim = ym.atEndOfMonth();
            nomeMesAno = capitalizarMes(ym.format(MES_LABEL_FORMATTER));
            apenasMes = capitalizarMes(ym.format(DateTimeFormatter.ofPattern("MMMM", new Locale("pt", "BR"))));
            anoStr = String.valueOf(ym.getYear());
            sufixoArquivo = ym.format(MES_VALOR_FORMATTER);
        }

        List<PlateProductionEntry> producoes = productionRepo.findAll((Specification<PlateProductionEntry>) (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dataInicio));
            preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), dataFim));
            return cb.and(preds.toArray(new Predicate[0]));
        });

        List<PlateDefectEntry> ocorrencias = defectRepo.findAll((Specification<PlateDefectEntry>) (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dataInicio));
            preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), dataFim));
            return cb.and(preds.toArray(new Predicate[0]));
        });

        DecimalFormatSymbols symPtBrIntOnly = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symPtBrIntOnly.setGroupingSeparator('.');
        symPtBrIntOnly.setDecimalSeparator(',');
        DecimalFormat fmtInteiroOnly = new DecimalFormat("#,##0", symPtBrIntOnly);
        fmtInteiroOnly.setGroupingUsed(true);
        MonthlyDefectSummary defectSummary = buildMonthlyDefectSummary(ocorrencias, fmtInteiroOnly);
        long quantSemPerda = producoes.stream().mapToLong(p -> p.getQuantity() == null ? 0L : p.getQuantity()).sum();
        long perda = defectSummary.totalPerdaGeral();
        long soma = quantSemPerda + perda;
        BigDecimal pctPerda = percentual3Casas(perda, quantSemPerda);
        List<MonthlyDefectCodeRow> tabelaCodigos = defectSummary.rows();
        long totalPerdaGeral = defectSummary.totalPerdaGeral();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=controle-mensal-" + sufixoArquivo + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            criarAbaConsumoMensal(workbook, nomeMesAno, apenasMes, anoStr, quantSemPerda, perda, soma, pctPerda);
            criarAbaPerdas(workbook, nomeMesAno, tabelaCodigos, totalPerdaGeral);
            workbook.write(response.getOutputStream());
        }
    }

    private void criarAbaConsumoMensal(XSSFWorkbook workbook, String nomeMesAno, String apenasMes, String anoStr,
                                       long producoes, long perdas, long total, BigDecimal pctPerda) {
        XSSFSheet sheet = workbook.createSheet("Consumo Mensal");

        CellStyle tituloEstilo = workbook.createCellStyle();
        Font tituloFonte = workbook.createFont();
        tituloFonte.setBold(true);
        tituloFonte.setFontHeightInPoints((short) 16);
        tituloEstilo.setFont(tituloFonte);
        tituloEstilo.setAlignment(HorizontalAlignment.CENTER);
        tituloEstilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

        CellStyle headerEstilo = workbook.createCellStyle();
        Font headerFonte = workbook.createFont();
        headerFonte.setBold(true);
        headerFonte.setFontHeightInPoints((short) 10);
        headerEstilo.setFont(headerFonte);
        headerEstilo.setAlignment(HorizontalAlignment.CENTER);
        headerEstilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        headerEstilo.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerEstilo.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerEstilo.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerEstilo.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        CellStyle dadosEstilo = workbook.createCellStyle();
        dadosEstilo.setAlignment(HorizontalAlignment.CENTER);
        dadosEstilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        dadosEstilo.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        dadosEstilo.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        dadosEstilo.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        dadosEstilo.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        Font dadosFont = workbook.createFont();
        dadosFont.setFontHeightInPoints((short) 10);
        dadosFont.setBold(true);
        dadosEstilo.setFont(dadosFont);

        DataFormat poiDataFmt = workbook.createDataFormat();
        short fmtIntPtBr = poiDataFmt.getFormat("#,##0");
        short fmt3DecPtBr = poiDataFmt.getFormat("#,##0.000");

        CellStyle dadosIntEstilo = workbook.createCellStyle();
        dadosIntEstilo.cloneStyleFrom(dadosEstilo);
        dadosIntEstilo.setDataFormat(fmtIntPtBr);
        CellStyle dados3DecEstilo = workbook.createCellStyle();
        dados3DecEstilo.cloneStyleFrom(dadosEstilo);
        dados3DecEstilo.setDataFormat(fmt3DecPtBr);

        int r = 0;
        Row titulo = sheet.createRow(r++);
        titulo.createCell(0).setCellValue("CONTROLE DE CHAPAS " + anoStr);
        titulo.getCell(0).setCellStyle(tituloEstilo);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, 4));
        titulo.setHeightInPoints(26);

        Row linhaVazia = sheet.createRow(r++);
        linhaVazia.setHeightInPoints(8);

        int headerRowIdx = r++;
        Row headerRow = sheet.createRow(headerRowIdx);
        String[] headers = {"Mês", "Produções", "Perdas", "Total", "Porcentagem de perda"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerEstilo);
        }
        headerRow.setHeightInPoints(20);

        int dadosRowIdx = r++;
        Row dadosRow = sheet.createRow(dadosRowIdx);
        dadosRow.createCell(0).setCellValue(apenasMes);
        dadosRow.getCell(0).setCellStyle(dadosEstilo);

        dadosRow.createCell(1).setCellValue((double) producoes);
        dadosRow.getCell(1).setCellStyle(dadosIntEstilo);

        dadosRow.createCell(2).setCellValue((double) perdas);
        dadosRow.getCell(2).setCellStyle(dadosIntEstilo);

        dadosRow.createCell(3).setCellValue((double) total);
        dadosRow.getCell(3).setCellStyle(dadosIntEstilo);

        BigDecimal pctBruto3Dec = pctPerda.setScale(3, RoundingMode.HALF_UP);
        dadosRow.createCell(4).setCellValue(pctBruto3Dec.doubleValue());
        dadosRow.getCell(4).setCellStyle(dados3DecEstilo);
        dadosRow.setHeightInPoints(18);

        sheet.setColumnWidth(0, 5500);
        sheet.setColumnWidth(1, 6200);
        sheet.setColumnWidth(2, 6200);
        sheet.setColumnWidth(3, 6200);
        sheet.setColumnWidth(4, 9200);

        r = r + 2;
        int graficoInicioRow = r;
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                0, 0, 0, 0,
                0, graficoInicioRow, 7, graficoInicioRow + 30
        );

        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText(apenasMes);
        chart.setTitleOverlay(false);

        org.apache.poi.xddf.usermodel.chart.XDDFChartLegend legenda = chart.getOrAddLegend();
        legenda.setPosition(org.apache.poi.xddf.usermodel.chart.LegendPosition.BOTTOM);

        org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.BOTTOM);

        org.apache.poi.xddf.usermodel.chart.XDDFValueAxis leftValueAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.LEFT);
        leftValueAxis.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.AUTO_ZERO);

        org.apache.poi.xddf.usermodel.chart.XDDFValueAxis rightValueAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.RIGHT);
        rightValueAxis.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.MAX);

        String[] catLabels = new String[]{
                "Produções",
                "Perdas",
                "Total",
                "Porcentagem de perda"
        };
        org.apache.poi.xddf.usermodel.chart.XDDFDataSource<String> categorias =
                org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromArray(catLabels);

        Double[] valoresQtds = new Double[]{
                (double) producoes,
                (double) perdas,
                (double) total,
                null
        };
        Double[] valorPctApenas = new Double[]{
                null,
                null,
                null,
                pctBruto3Dec.doubleValue()
        };

        org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> quantidades =
                org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromArray(valoresQtds);
        org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> soPct =
                org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromArray(valorPctApenas);

        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData barDataLeft =
                (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData) chart.createData(
                        org.apache.poi.xddf.usermodel.chart.ChartTypes.BAR, bottomAxis, leftValueAxis
                );
        barDataLeft.setBarDirection(org.apache.poi.xddf.usermodel.chart.BarDirection.COL);
        barDataLeft.setGapWidth(200);
        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series sQuant =
                (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series) barDataLeft.addSeries(categorias, quantidades);
        sQuant.setTitle("Quantidade", null);
        chart.plot(barDataLeft);

        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData barDataRight =
                (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData) chart.createData(
                        org.apache.poi.xddf.usermodel.chart.ChartTypes.BAR, bottomAxis, rightValueAxis
                );
        barDataRight.setBarDirection(org.apache.poi.xddf.usermodel.chart.BarDirection.COL);
        barDataRight.setGapWidth(200);
        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series sPct =
                (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series) barDataRight.addSeries(categorias, soPct);
        sPct.setTitle("% Perda", null);
        chart.plot(barDataRight);

        aplicarFontesGrafico(chart, 16, 13);
    }

    private void criarAbaPerdas(XSSFWorkbook workbook, String nomeMesAno,
                                List<MonthlyDefectCodeRow> tabelaCodigos, long totalPerdaGeral) {
        XSSFSheet sheet = workbook.createSheet("Perdas");

        CellStyle headerMergeEstilo = workbook.createCellStyle();
        Font headerMergeFont = workbook.createFont();
        headerMergeFont.setBold(true);
        headerMergeFont.setFontHeightInPoints((short) 10);
        headerMergeEstilo.setFont(headerMergeFont);
        headerMergeEstilo.setAlignment(HorizontalAlignment.LEFT);
        headerMergeEstilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        headerMergeEstilo.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerMergeEstilo.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerMergeEstilo.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerMergeEstilo.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        CellStyle headerTotalEstilo = workbook.createCellStyle();
        Font headerTotalFont = workbook.createFont();
        headerTotalFont.setBold(true);
        headerTotalFont.setFontHeightInPoints((short) 10);
        headerTotalEstilo.setFont(headerTotalFont);
        headerTotalEstilo.setAlignment(HorizontalAlignment.CENTER);
        headerTotalEstilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        headerTotalEstilo.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerTotalEstilo.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerTotalEstilo.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerTotalEstilo.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        CellStyle cellEstilo = workbook.createCellStyle();
        cellEstilo.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        cellEstilo.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        cellEstilo.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        cellEstilo.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        Font cellFont = workbook.createFont();
        cellFont.setFontHeightInPoints((short) 9);
        cellEstilo.setFont(cellFont);
        cellEstilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

        CellStyle cellCentroEstilo = workbook.createCellStyle();
        cellCentroEstilo.cloneStyleFrom(cellEstilo);
        cellCentroEstilo.setAlignment(HorizontalAlignment.CENTER);

        CellStyle totalLabelEstilo = workbook.createCellStyle();
        totalLabelEstilo.cloneStyleFrom(cellEstilo);
        Font totalLabelFont = workbook.createFont();
        totalLabelFont.setBold(true);
        totalLabelFont.setFontHeightInPoints((short) 9);
        totalLabelEstilo.setFont(totalLabelFont);

        CellStyle totalValorEstilo = workbook.createCellStyle();
        totalValorEstilo.cloneStyleFrom(cellCentroEstilo);
        Font totalValorFont = workbook.createFont();
        totalValorFont.setBold(true);
        totalValorFont.setFontHeightInPoints((short) 9);
        totalValorEstilo.setFont(totalValorFont);

        DataFormat poiDataFmt = workbook.createDataFormat();
        short fmtIntPtBr = poiDataFmt.getFormat("#,##0");
        CellStyle numIntPtBr = workbook.createCellStyle();
        numIntPtBr.cloneStyleFrom(cellCentroEstilo);
        numIntPtBr.setDataFormat(fmtIntPtBr);
        CellStyle totalValorInt = workbook.createCellStyle();
        totalValorInt.cloneStyleFrom(totalValorEstilo);
        totalValorInt.setDataFormat(fmtIntPtBr);

        int r = 0;
        int headerRowIdx = r++;
        Row headerRow = sheet.createRow(headerRowIdx);
        headerRow.createCell(0).setCellValue("Tabela de Codigos");
        headerRow.getCell(0).setCellStyle(headerMergeEstilo);
        headerRow.createCell(1).setCellStyle(headerMergeEstilo);
        headerRow.createCell(2).setCellValue("Total");
        headerRow.getCell(2).setCellStyle(headerTotalEstilo);
        headerRow.setHeightInPoints(18);

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(headerRowIdx, headerRowIdx, 0, 1));

        int primeiraLinhaIdx = r;
        for (MonthlyDefectCodeRow lin : tabelaCodigos) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(lin.label());
            row.getCell(0).setCellStyle(cellEstilo);
            row.createCell(1).setCellStyle(cellEstilo);
            row.createCell(2).setCellValue((double) lin.total());
            row.getCell(2).setCellStyle(numIntPtBr);
            row.setHeightInPoints(15);
        }
        int ultimaLinhaIdx = r - 1;

        int totalLinhaIdx = r++;
        Row totalRow = sheet.createRow(totalLinhaIdx);
        totalRow.createCell(0).setCellValue("Total");
        totalRow.getCell(0).setCellStyle(totalLabelEstilo);
        totalRow.createCell(1).setCellStyle(totalLabelEstilo);
        Cell totalFormulaCell = totalRow.createCell(2);
        char colLetra = (char) ('A' + 2);
        String formula = "SUM(" + colLetra + (primeiraLinhaIdx + 1) + ":" + colLetra + (ultimaLinhaIdx + 1) + ")";
        totalFormulaCell.setCellFormula(formula);
        totalFormulaCell.setCellValue((double) totalPerdaGeral);
        totalFormulaCell.setCellStyle(totalValorInt);
        totalRow.setHeightInPoints(18);

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(totalLinhaIdx, totalLinhaIdx, 0, 1));

        sheet.setColumnWidth(0, 9800);
        sheet.setColumnWidth(1, 1200);
        sheet.setColumnWidth(2, 4500);

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        int chartPrimeiraLinha = 0;
        int chartUltimaLinha = tabelaCodigos.size() + 11;
        int chartPrimeiraCol = 4;
        int chartUltimaCol = 22;
        XSSFClientAnchor anchor = drawing.createAnchor(
                0, 0, 0, 0,
                chartPrimeiraCol, chartPrimeiraLinha, chartUltimaCol, chartUltimaLinha
        );

        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText("Chapas Descartadas x Ocorrências " + nomeMesAno + ".");
        chart.setTitleOverlay(false);

        org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis leftAxis = chart.createCategoryAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.LEFT);
        org.apache.poi.xddf.usermodel.chart.XDDFValueAxis bottomAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.BOTTOM);
        bottomAxis.setCrosses(org.apache.poi.xddf.usermodel.chart.AxisCrosses.MIN);

        org.apache.poi.xddf.usermodel.chart.XDDFDataSource<String> categorias = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new org.apache.poi.ss.util.CellRangeAddress(primeiraLinhaIdx, ultimaLinhaIdx, 0, 0));
        org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> valores = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new org.apache.poi.ss.util.CellRangeAddress(primeiraLinhaIdx, ultimaLinhaIdx, 2, 2));

        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData dadosGrafico =
                (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData) chart.createData(
                        org.apache.poi.xddf.usermodel.chart.ChartTypes.BAR, leftAxis, bottomAxis
                );
        dadosGrafico.setBarDirection(org.apache.poi.xddf.usermodel.chart.BarDirection.BAR);
        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series serie =
                (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series) dadosGrafico.addSeries(categorias, valores);
        serie.setTitle("TOTAL", null);
        chart.plot(dadosGrafico);

        aplicarFontesGrafico(chart, 15, 13);
    }

    private void aplicarFontesGrafico(XSSFChart chart, int tituloPts, int legendaEixoPts) {
        try {
            org.openxmlformats.schemas.drawingml.x2006.chart.CTChart ctChart = chart.getCTChart();
            long tituloSz = (long) tituloPts * 100;
            long legendaSz = (long) legendaEixoPts * 100;
            if (ctChart.getTitle() != null) {
                org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle ctTitle = ctChart.getTitle();
                if (ctTitle.getTx() != null) {
                    org.openxmlformats.schemas.drawingml.x2006.chart.CTTx ctTx = ctTitle.getTx();
                    if (ctTx.getRich() != null) {
                        aplicarSzEmTextBody(ctTx.getRich(), tituloSz);
                    }
                }
                if (ctTitle.getTxPr() != null) {
                    aplicarSzEmTextBody(ctTitle.getTxPr(), tituloSz);
                }
            }
            if (ctChart.getLegend() != null) {
                org.openxmlformats.schemas.drawingml.x2006.chart.CTLegend ctLegend = ctChart.getLegend();
                if (ctLegend.getTxPr() != null) {
                    aplicarSzEmTextBody(ctLegend.getTxPr(), legendaSz);
                }
            }
            if (ctChart.getPlotArea() != null) {
                org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea ctPlotArea = ctChart.getPlotArea();
                for (org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx catAx : ctPlotArea.getCatAxList()) {
                    if (catAx.getTxPr() != null) aplicarSzEmTextBody(catAx.getTxPr(), legendaSz);
                }
                for (org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx valAx : ctPlotArea.getValAxList()) {
                    if (valAx.getTxPr() != null) aplicarSzEmTextBody(valAx.getTxPr(), legendaSz);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void aplicarSzEmTextBody(org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun[] runs, long sz) {
        if (runs == null) return;
        for (org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun run : runs) {
            if (run == null) continue;
            if (run.getRPr() != null) {
                run.getRPr().setSz((int) sz);
            } else {
                org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties rpr = run.addNewRPr();
                rpr.setSz((int) sz);
            }
        }
    }

    private void aplicarSzEmTextBody(org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody tb, long sz) {
        if (tb == null) return;
        for (org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph p : tb.getPList()) {
            if (p == null) continue;
            if (p.getEndParaRPr() != null) {
                p.getEndParaRPr().setSz((int) sz);
            }
            org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun[] runs = p.getRArray();
            if (runs != null) aplicarSzEmTextBody(runs, sz);
            org.openxmlformats.schemas.drawingml.x2006.main.CTTextField[] fields = p.getFldArray();
            if (fields != null) {
                for (org.openxmlformats.schemas.drawingml.x2006.main.CTTextField f : fields) {
                    if (f == null) continue;
                    if (f.getRPr() != null) f.getRPr().setSz((int) sz);
                }
            }
        }
        if (tb.getBodyPr() != null) {
        }
        if (tb.getLstStyle() != null) {
        }
    }

    private YearMonth resolverMes(String mes, Authentication authentication) {
        YearMonth mesCorrente = YearMonth.now();
        if (!permiteNavegarMesesPassados(authentication)) {
            return mesCorrente;
        }
        if (mes == null || mes.isBlank()) {
            return mesCorrente;
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(mes.trim());
        } catch (Exception ignored) {
            ym = mesCorrente;
        }
        if (ym.isBefore(PISO_MINIMO_MES)) return PISO_MINIMO_MES;
        if (ym.isAfter(mesCorrente)) return mesCorrente;
        return ym;
    }

    private boolean isGestorOuAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isAdmin(user) || appUserService.isGestor(user);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVisualizador(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isVisualizador(user);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isUsuario(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isUsuario(user);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean permiteNavegarMesesPassados(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.hasRelatoriosAccess(user);
        } catch (Exception e) {
            return false;
        }
    }

    private List<DashboardMonthOption> montarOpcoesMeses() {
        YearMonth atual = YearMonth.now();
        YearMonth primeiroMesComDados = encontrarPrimeiroMesComDados();
        List<DashboardMonthOption> opcoes = new ArrayList<>();
        YearMonth cursor = atual;
        while (!cursor.isBefore(primeiroMesComDados)) {
            String value = cursor.format(MES_VALOR_FORMATTER);
            String label = capitalizarMes(cursor.format(MES_LABEL_FORMATTER));
            opcoes.add(new DashboardMonthOption(value, label));
            cursor = cursor.minusMonths(1);
        }
        if (opcoes.isEmpty()) {
            String value = atual.format(MES_VALOR_FORMATTER);
            String label = capitalizarMes(atual.format(MES_LABEL_FORMATTER));
            opcoes.add(new DashboardMonthOption(value, label));
        }
        return opcoes;
    }

    private String capitalizarMes(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private String[] splitDefectLabel(String label) {
        if (label == null) return new String[]{"", ""};
        int idx = label.indexOf(" - ");
        if (idx < 0) return new String[]{label.trim(), ""};
        String num = label.substring(0, idx).trim();
        String desc = label.substring(idx + " - ".length()).trim();
        return new String[]{num, desc};
    }

    private int extrairNumeroCodigo(String numero) {
        if (numero == null || numero.isBlank()) return Integer.MAX_VALUE;
        try {
            StringBuilder sb = new StringBuilder();
            for (char c : numero.toCharArray()) {
                if (Character.isDigit(c)) sb.append(c);
            }
            if (sb.isEmpty()) return Integer.MAX_VALUE;
            return Integer.parseInt(sb.toString());
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private MonthlyDefectSummary buildMonthlyDefectSummary(List<PlateDefectEntry> ocorrencias, DecimalFormat fmtInteiro) {
        Map<String, Long> somaPorCodigo = new HashMap<>();
        long semCodigo = 0L;

        for (PlateDefectEntry ocorrencia : ocorrencias) {
            long valor = ocorrencia.getLossQuantity() == null ? 0L : ocorrencia.getLossQuantity();
            String codigo = canonicalizarCodigoDefeitoMensal(ocorrencia.getDefectCode());
            if (codigo == null) {
                semCodigo += valor;
                continue;
            }
            somaPorCodigo.merge(codigo, valor, Long::sum);
        }

        String[][] codigosPadrao = new String[][]{
                {"1", "1", "erro no ctp"},
                {"2", "2", "defeito de chapa"},
                {"3", "3", "desgaste de chapa"},
                {"4", "4", "correção de fotolito"},
                {"5", "5", "erro de fotolito"},
                {"6", "6", "falta de energia"},
                {"7", "7", "troca de máquina"},
                {"8", "8", "recurso de maquina"},
                {"9", "9", "retorno em maquina"},
                {"10", "10", "problema na Processadora"},
                {"11", "11", "Chapa reserva"},
                {"12", "12", "Estragou em maquina"},
                {"13", "13", "Risco na chapa"},
                {"14", "14", "Erro PCP"},
                {"15", "15-A", "A especificar"}
        };

        List<MonthlyDefectCodeRow> tabelaCodigos = new ArrayList<>();
        long totalPerdaGeral = 0L;

        for (String[] codigoPadrao : codigosPadrao) {
            String chave = codigoPadrao[0];
            String numeroExibicao = codigoPadrao[1];
            String descricao = codigoPadrao[2];
            long total = somaPorCodigo.getOrDefault(chave, 0L);
            somaPorCodigo.remove(chave);
            totalPerdaGeral += total;
            tabelaCodigos.add(new MonthlyDefectCodeRow(
                    numeroExibicao,
                    descricao.startsWith("A ") ? numeroExibicao + " " + descricao : numeroExibicao + "-" + descricao,
                    total,
                    fmtInteiro.format(total)
            ));
        }

        List<Map.Entry<String, Long>> codigosExtras = new ArrayList<>(somaPorCodigo.entrySet());
        codigosExtras.sort(Comparator
                .comparingInt((Map.Entry<String, Long> entry) -> extrairNumeroCodigo(entry.getKey()))
                .thenComparing(Map.Entry::getKey));

        for (Map.Entry<String, Long> extra : codigosExtras) {
            String codigo = extra.getKey();
            long total = extra.getValue();
            String descricao = resolveDefectDescription(codigo);
            String label = (descricao == null || descricao.isBlank() || descricao.equalsIgnoreCase(codigo))
                    ? codigo
                    : codigo + "-" + descricao;
            totalPerdaGeral += total;
            tabelaCodigos.add(new MonthlyDefectCodeRow(codigo, label, total, fmtInteiro.format(total)));
        }

        if (semCodigo > 0) {
            totalPerdaGeral += semCodigo;
            tabelaCodigos.add(new MonthlyDefectCodeRow("SC", "Sem código", semCodigo, fmtInteiro.format(semCodigo)));
        }

        long maxLinhaTabela = tabelaCodigos.stream().mapToLong(MonthlyDefectCodeRow::total).max().orElse(1L);
        if (maxLinhaTabela <= 0) maxLinhaTabela = 1L;

        return new MonthlyDefectSummary(tabelaCodigos, totalPerdaGeral, fmtInteiro.format(totalPerdaGeral), maxLinhaTabela);
    }

    private String canonicalizarCodigoDefeitoMensal(String codigoOriginal) {
        if (codigoOriginal == null || codigoOriginal.isBlank()) return null;
        String codigo = codigoOriginal.trim().toUpperCase(Locale.ROOT);
        StringBuilder digitos = new StringBuilder();
        for (char c : codigo.toCharArray()) {
            if (Character.isDigit(c)) {
                digitos.append(c);
            } else if (!digitos.isEmpty()) {
                break;
            }
        }
        if (digitos.isEmpty()) return codigo;
        try {
            return String.valueOf(Integer.parseInt(digitos.toString()));
        } catch (NumberFormatException ex) {
            return digitos.toString();
        }
    }

    private BigDecimal percentual3Casas(long numerador, long denominador) {
        if (denominador <= 0) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominador), 3, RoundingMode.HALF_UP);
    }

    record MonthlyControlData(
            String nomeMesAno,
            String mesValor,
            long quantSemPerda, String quantSemPerdaFmt,
            long perda, String perdaFmt,
            long soma, String somaFmt,
            BigDecimal pctPerda, String pctPerdaFmt
    ) {}

    record MonthlyDefectCodeRow(
            String numero,
            String label,
            long total,
            String totalFmt
    ) {}

    record MonthlyDefectSummary(
            List<MonthlyDefectCodeRow> rows,
            long totalPerdaGeral,
            String totalPerdaGeralFmt,
            long maxLinhaTabela
    ) {}

    private record ReportResult(
            List<HistoryEntryView> entries,
            int totalCount,
            int plateProductionCount,
            int plateDefectCount,
            long totalPlateQuantity,
            long totalPlateLoss
    ) {
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "mes", required = false) String mes,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication,
            Model model
    ) {
        boolean modoIntervalo = false;
        LocalDate dataInicio;
        LocalDate dataFim;
        String mesValor = null;
        String nomeMesAno;
        int diasNoIntervalo;

        if (dateFrom != null || dateTo != null) {
            modoIntervalo = true;
            LocalDate df = dateFrom == null ? PISO_MINIMO_DATA : dateFrom;
            LocalDate dt = dateTo == null ? LocalDate.now() : dateTo;
            if (df.isBefore(PISO_MINIMO_DATA)) df = PISO_MINIMO_DATA;
            if (dt.isBefore(PISO_MINIMO_DATA)) dt = PISO_MINIMO_DATA;
            if (df.isAfter(dt)) df = dt;
            dataInicio = df;
            dataFim = dt;
            diasNoIntervalo = (int) (dataFim.toEpochDay() - dataInicio.toEpochDay() + 1L);
            nomeMesAno = dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " até " + dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else {
            YearMonth ym = dashResolverMes(mes, authentication);
            dataInicio = ym.atDay(1);
            dataFim = ym.atEndOfMonth();
            mesValor = ym.format(MES_VALOR_FORMATTER);
            nomeMesAno = capitalizarMes(ym.format(MES_LABEL_FORMATTER));
            diasNoIntervalo = ym.lengthOfMonth();
        }

        List<PlateProductionEntry> producoes = productionRepo.findAll((Specification<PlateProductionEntry>) (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dataInicio));
            preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), dataFim));
            return cb.and(preds.toArray(new Predicate[0]));
        });

        List<PlateDefectEntry> ocorrencias = defectRepo.findAll((Specification<PlateDefectEntry>) (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dataInicio));
            preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), dataFim));
            return cb.and(preds.toArray(new Predicate[0]));
        });

        Map<String, String> defectDescMap = dashBuildDefectDescMap();

        long totalProducoes = producoes.size();
        long totalOcorrencias = ocorrencias.size();
        long totalLancamentos = totalProducoes + totalOcorrencias;
        long totalChapasProduzidas = producoes.stream().mapToLong(p -> p.getQuantity() == null ? 0 : p.getQuantity()).sum();
        long totalChapasPerdidas = ocorrencias.stream().mapToLong(p -> p.getLossQuantity() == null ? 0 : p.getLossQuantity()).sum();

        BigDecimal pctProducoes = dashPercentual(totalProducoes, totalLancamentos);
        BigDecimal pctOcorrencias = dashPercentual(totalOcorrencias, totalLancamentos);
        BigDecimal pctPerdas = dashPercentual(totalChapasPerdidas, totalChapasProduzidas);
        BigDecimal pctProduzidas = BigDecimal.valueOf(100)
                .subtract(pctPerdas)
                .setScale(1, RoundingMode.HALF_UP);

        int progressoPerdaPct = dashPercentualInteiro(totalChapasPerdidas, totalChapasProduzidas);
        int perdaPctInteiro = dashPercentualInteiro(totalOcorrencias, totalLancamentos);
        int ocorrenciaPctInteiro = perdaPctInteiro;

        DecimalFormatSymbols symPtBr = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symPtBr.setGroupingSeparator('.');
        symPtBr.setDecimalSeparator(',');
        DecimalFormat fmtInteiro = new DecimalFormat("#,##0", symPtBr);
        fmtInteiro.setGroupingUsed(true);
        DecimalFormat fmtPercentual = new DecimalFormat("#,##0.0", symPtBr);
        fmtPercentual.setGroupingUsed(true);

        String totalLancamentosFmt = fmtInteiro.format(totalLancamentos);
        String totalProducoesFmt = fmtInteiro.format(totalProducoes);
        String totalOcorrenciasFmt = fmtInteiro.format(totalOcorrencias);
        String totalChapasProduzidasFmt = fmtInteiro.format(totalChapasProduzidas);
        String totalChapasPerdidasFmt = fmtInteiro.format(totalChapasPerdidas);
        String percentualProducoesFmt = fmtPercentual.format(pctProducoes);
        String percentualOcorrenciasFmt = fmtPercentual.format(pctOcorrencias);
        String percentualPerdasFmt = fmtPercentual.format(pctPerdas);
        String percentualProduzidasFmt = fmtPercentual.format(pctProduzidas);
        String perdaCentroDonutFmt = fmtPercentual.format(pctPerdas);

        DashboardData data = new DashboardData(
                totalLancamentos,
                totalProducoes,
                totalOcorrencias,
                totalChapasProduzidas,
                totalChapasPerdidas,
                pctProducoes,
                pctOcorrencias,
                pctPerdas,
                pctProduzidas,
                diasNoIntervalo,
                progressoPerdaPct,
                perdaPctInteiro,
                ocorrenciaPctInteiro,
                nomeMesAno,
                mesValor == null ? "" : mesValor,
                totalLancamentosFmt,
                totalProducoesFmt,
                totalOcorrenciasFmt,
                totalChapasProduzidasFmt,
                totalChapasPerdidasFmt,
                percentualProducoesFmt,
                percentualOcorrenciasFmt,
                percentualPerdasFmt,
                percentualProduzidasFmt,
                perdaCentroDonutFmt
        );

        List<DashboardRecentEntry> recentes = dashMontarRecentes(producoes, ocorrencias, defectDescMap);
        List<DashboardMonthOption> opcoesMeses = dashMontarOpcoesMeses();
        boolean isGestorOuAdmin = dashIsGestorOuAdmin(authentication);
        boolean isVisualizador = dashIsVisualizador(authentication);
        boolean permiteRelatorios = isGestorOuAdmin || isVisualizador;

        model.addAttribute("dashboard", data);
        model.addAttribute("recentes", recentes);
        model.addAttribute("opcoesMeses", opcoesMeses);
        model.addAttribute("isGestorOuAdmin", isGestorOuAdmin);
        model.addAttribute("isVisualizador", isVisualizador);
        model.addAttribute("permiteRelatorios", permiteRelatorios);
        model.addAttribute("dataInicio", dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("dataFim", dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("modoIntervalo", modoIntervalo);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("mes", mesValor);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeMenu", "reports-dashboard");
        return "dashboard/index";
    }

    private YearMonth dashResolverMes(String mes, Authentication authentication) {
        YearMonth mesCorrente = YearMonth.now();
        if (!dashPermiteNavegarMesesPassados(authentication)) return mesCorrente;
        if (mes == null || mes.isBlank()) return mesCorrente;
        YearMonth ym;
        try { ym = YearMonth.parse(mes.trim()); } catch (Exception ignored) { ym = mesCorrente; }
        if (ym.isBefore(PISO_MINIMO_MES)) return PISO_MINIMO_MES;
        if (ym.isAfter(mesCorrente)) return mesCorrente;
        return ym;
    }

    private boolean dashIsGestorOuAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isAdmin(user) || appUserService.isGestor(user);
        } catch (Exception e) { return false; }
    }

    private boolean dashIsVisualizador(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isVisualizador(user);
        } catch (Exception e) { return false; }
    }

    private boolean dashPermiteNavegarMesesPassados(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isAdminOrGestorOrVisualizador(user);
        } catch (Exception e) { return false; }
    }

    private List<DashboardMonthOption> dashMontarOpcoesMeses() {
        YearMonth atual = YearMonth.now();
        YearMonth primeiroMesComDados = encontrarPrimeiroMesComDados();
        List<DashboardMonthOption> opcoes = new ArrayList<>();
        YearMonth cursor = atual;
        while (!cursor.isBefore(primeiroMesComDados)) {
            String value = cursor.format(MES_VALOR_FORMATTER);
            String label = capitalizarMes(cursor.format(MES_LABEL_FORMATTER));
            opcoes.add(new DashboardMonthOption(value, label));
            cursor = cursor.minusMonths(1);
        }
        if (opcoes.isEmpty()) {
            String value = atual.format(MES_VALOR_FORMATTER);
            String label = capitalizarMes(atual.format(MES_LABEL_FORMATTER));
            opcoes.add(new DashboardMonthOption(value, label));
        }
        return opcoes;
    }

    private YearMonth encontrarPrimeiroMesComDados() {
        LocalDate minProd = productionRepo.findMinEntryDate();
        LocalDate minDef = defectRepo.findMinEntryDate();
        LocalDate min = null;
        if (minProd != null) min = minProd;
        if (minDef != null && (min == null || minDef.isBefore(min))) min = minDef;
        YearMonth atual = YearMonth.now();
        YearMonth primeiroMesComDados;
        if (min == null) {
            primeiroMesComDados = atual.minusMonths(11);
        } else {
            primeiroMesComDados = YearMonth.from(min);
            if (primeiroMesComDados.isAfter(atual)) primeiroMesComDados = atual;
        }
        if (primeiroMesComDados.isBefore(PISO_MINIMO_MES)) {
            return PISO_MINIMO_MES;
        }
        return primeiroMesComDados;
    }

    private List<DashboardRecentEntry> dashMontarRecentes(
            List<PlateProductionEntry> producoes,
            List<PlateDefectEntry> ocorrencias,
            Map<String, String> defectDescMap
    ) {
        List<DashboardRecentEntry> combinados = new ArrayList<>();
        for (PlateProductionEntry p : producoes) {
            String maquina = p.getMachine() != null && p.getMachine().getLabel() != null ? p.getMachine().getLabel() : "-";
            String detalhe = p.getClientName() != null ? p.getClientName() : "";
            java.time.LocalDateTime dataRef = p.getCreatedAt() != null ? p.getCreatedAt() : java.time.LocalDateTime.of(p.getEntryDate(), java.time.LocalTime.MIN);
            combinados.add(new DashboardRecentEntry(p.getId(), "Producao", dataRef, dashFormatar(dataRef), p.getPlate(), p.getCtp(), maquina, p.getQuantity(), null, p.getCreatedBy(), detalhe, "/chapas/" + p.getId()));
        }
        for (PlateDefectEntry d : ocorrencias) {
            String maquina = d.getMachine() != null && d.getMachine().getLabel() != null ? d.getMachine().getLabel() : "-";
            String defectCode = d.getDefectCode() != null ? d.getDefectCode() : "";
            String descricao = defectDescMap.getOrDefault(defectCode, defectCode);
            String detalhe = (defectCode.isBlank() ? "" : defectCode + " - ") + descricao;
            java.time.LocalDateTime dataRef = d.getCreatedAt() != null ? d.getCreatedAt() : java.time.LocalDateTime.of(d.getEntryDate(), java.time.LocalTime.MIN);
            combinados.add(new DashboardRecentEntry(d.getId(), "Ocorrencia", dataRef, dashFormatar(dataRef), d.getPlate(), d.getCtp(), maquina, null, d.getLossQuantity(), d.getCreatedBy(), detalhe, "/ocorrencias/" + d.getId()));
        }
        return combinados.stream()
                .sorted(Comparator.comparing(DashboardRecentEntry::dataHora, Comparator.reverseOrder()))
                .limit(DASH_RECENTES_LIMITE)
                .toList();
    }

    private String dashFormatar(java.time.LocalDateTime dt) {
        if (dt == null) return "-";
        return dt.format(DASH_DT_FORMATTER);
    }

    private Map<String, String> dashBuildDefectDescMap() {
        Map<String, String> map = new HashMap<>();
        try {
            List<LookupOption> opcoes = lookupService.listAll(LookupCategory.DEFECT_CODE);
            if (opcoes != null) {
                for (LookupOption o : opcoes) {
                    if (o == null || o.getLabel() == null) continue;
                    String[] partes = splitDefectLabel(o.getLabel());
                    if (partes[0] != null && !partes[0].isBlank()) {
                        map.put(partes[0].trim(), partes[1] == null ? "" : partes[1].trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private BigDecimal dashPercentual(long numerador, long denominador) {
        if (denominador <= 0) return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(numerador).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominador), 1, RoundingMode.HALF_UP);
    }

    private int dashPercentualInteiro(long numerador, long denominador) {
        if (denominador <= 0) return 0;
        return BigDecimal.valueOf(numerador).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominador), 0, RoundingMode.HALF_UP).intValueExact();
    }
}
