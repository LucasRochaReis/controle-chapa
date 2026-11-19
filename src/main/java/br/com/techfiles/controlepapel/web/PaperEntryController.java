package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.PlateProductionEntry;
import br.com.techfiles.controlepapel.service.AppUserService;
import br.com.techfiles.controlepapel.service.LookupService;
import br.com.techfiles.controlepapel.service.PlateProductionImportResult;
import br.com.techfiles.controlepapel.service.PlateProductionImportService;
import br.com.techfiles.controlepapel.service.PlateProductionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/lancamentos")
public class PaperEntryController {

    private final PlateProductionService plateProductionService;
    private final LookupService lookupService;
    private final AppUserService appUserService;
    private final PlateProductionImportService plateProductionImportService;

    public PaperEntryController(
            PlateProductionService plateProductionService,
            LookupService lookupService,
            AppUserService appUserService,
            PlateProductionImportService plateProductionImportService
    ) {
        this.plateProductionService = plateProductionService;
        this.lookupService = lookupService;
        this.appUserService = appUserService;
        this.plateProductionImportService = plateProductionImportService;
    }

    @ModelAttribute("shiftOptions")
    public Object shiftOptions() {
        return lookupService.listActive(LookupCategory.SHIFT);
    }

    @ModelAttribute("machineOptions")
    public Object machineOptions(Authentication authentication) {
        return lookupService.listActive(LookupCategory.MACHINE);
    }

    private static final int DEFAULT_PAGE_SIZE = 50;

    @GetMapping
    public String list(
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "shiftId", required = false) Long shiftId,
            @RequestParam(name = "machineId", required = false) Long machineId,
            @RequestParam(name = "opCode", required = false) String opCode,
            @RequestParam(name = "clientName", required = false) String clientName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Authentication authentication,
            HttpServletRequest request,
            Model model
    ) {
        if (size < 1) size = DEFAULT_PAGE_SIZE;
        if (size > 500) size = 500;
        if (page < 0) page = 0;
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("shiftId", shiftId);
        model.addAttribute("machineId", machineId);
        model.addAttribute("opCode", opCode);
        model.addAttribute("clientName", clientName);

        Page<HistoryEntryView> entriesPage = buildProductionHistory(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, clientName, page, size, request);
        model.addAttribute("entries", entriesPage.getContent());
        model.addAttribute("page", entriesPage);
        model.addAttribute("totalElements", entriesPage.getTotalElements());
        model.addAttribute("totalPages", entriesPage.getTotalPages());
        model.addAttribute("currentPage", entriesPage.getNumber());
        model.addAttribute("pageSize", entriesPage.getSize());
        model.addAttribute("hasPrevious", entriesPage.hasPrevious());
        model.addAttribute("hasNext", entriesPage.hasNext());
        model.addAttribute("firstPage", 0);
        model.addAttribute("lastPage", Math.max(0, entriesPage.getTotalPages() - 1));
        model.addAttribute("pagesToShow", buildPaginationWindow(entriesPage.getNumber(), entriesPage.getTotalPages()));

        model.addAttribute("todayCount", plateProductionService.countToday(currentUser));
        model.addAttribute("canFilterMachine", true);
        model.addAttribute("currentUserMachine", currentUser.getMachine());
        model.addAttribute("pageTitle", "Lan\u00e7amentos");
        model.addAttribute("activeMenu", "entries");
        return "entries/list";
    }

    @GetMapping("/novo")
    public String newEntryRedirect() {
        return "redirect:/chapas/novo";
    }

    @GetMapping("/importar")
    public String importPage(Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        model.addAttribute("pageTitle", "Importar Lancamentos");
        model.addAttribute("activeMenu", "entries");
        return "entries/import";
    }

    @PostMapping("/importar")
    public String importExcel(
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Selecione um arquivo Excel (.xlsx ou .xls) para importar.");
            model.addAttribute("pageTitle", "Importar Lancamentos");
            model.addAttribute("activeMenu", "entries");
            return "entries/import";
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            model.addAttribute("error", "Formato de arquivo invalido. Use apenas arquivos .xlsx (Excel 2007+) ou .xls (Excel 97-2003).");
            model.addAttribute("pageTitle", "Importar Lancamentos");
            model.addAttribute("activeMenu", "entries");
            return "entries/import";
        }

        try {
            AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
            PlateProductionImportResult result = plateProductionImportService.importFromExcel(file, currentUser);

            if (result.hasErrors()) {
                model.addAttribute("importResult", result);
                model.addAttribute("warning",
                        "Importacao concluida com erros. "
                        + result.getSuccessCount() + " de " + result.getTotalRows() + " linhas importadas com sucesso. "
                        + result.getErrorCount() + " linhas com erro.");
            } else {
                redirectAttributes.addFlashAttribute("success",
                        "Importacao concluida com sucesso! "
                        + result.getSuccessCount() + " lancamentos importados.");
                return "redirect:/lancamentos";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao processar o arquivo: " + e.getMessage());
        }

        model.addAttribute("pageTitle", "Importar Lancamentos");
        model.addAttribute("activeMenu", "entries");
        return "entries/import";
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) return false;
        try {
            AppUser user = appUserService.getRequiredCurrentUser(authentication);
            return appUserService.isAdmin(user);
        } catch (Exception e) {
            return false;
        }
    }

    private List<Integer> buildPaginationWindow(int currentPage, int totalPages) {
        List<Integer> pages = new ArrayList<>();
        if (totalPages <= 0) {
            pages.add(0);
            return pages;
        }
        int window = 2;
        int start = Math.max(0, currentPage - window);
        int end = Math.min(totalPages - 1, currentPage + window);
        if (start > 0) {
            pages.add(0);
            if (start > 1) pages.add(null);
        }
        for (int i = start; i <= end; i++) pages.add(i);
        if (end < totalPages - 1) {
            if (end < totalPages - 2) pages.add(null);
            pages.add(totalPages - 1);
        }
        return pages;
    }

    private List<HistoryEntryView> buildProductionHistory(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            String opCode,
            String clientName,
            HttpServletRequest request
    ) {
        List<HistoryEntryView> history = new ArrayList<>();

        plateProductionService.search(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, clientName)
                .stream()
                .map(entry -> toProductionHistoryView(entry, request))
                .forEach(history::add);

        history.sort(Comparator
                .comparing(HistoryEntryView::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HistoryEntryView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return history;
    }

    private Page<HistoryEntryView> buildProductionHistory(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            String opCode,
            String clientName,
            int page,
            int size,
            HttpServletRequest request
    ) {
        return plateProductionService.searchPaginated(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, clientName, page, size)
                .map(entry -> toProductionHistoryView(entry, request));
    }

    private HistoryEntryView toProductionHistoryView(PlateProductionEntry entry, HttpServletRequest request) {
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
                request.getContextPath() + "/chapas/" + entry.getId()
        );
    }
}
