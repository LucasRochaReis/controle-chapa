package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.model.PlateDefectEntry;
import br.com.techfiles.controlepapel.service.AppUserService;
import br.com.techfiles.controlepapel.service.LookupService;
import br.com.techfiles.controlepapel.service.PlateDefectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ocorrencias")
public class PlateDefectController {

    private final PlateDefectService plateDefectService;
    private final LookupService lookupService;
    private final AppUserService appUserService;

    public PlateDefectController(
            PlateDefectService plateDefectService,
            LookupService lookupService,
            AppUserService appUserService
    ) {
        this.plateDefectService = plateDefectService;
        this.lookupService = lookupService;
        this.appUserService = appUserService;
    }

    @ModelAttribute("shiftOptions")
    public Object shiftOptions() {
        return lookupService.listActive(LookupCategory.SHIFT);
    }

    @ModelAttribute("machineOptions")
    public Object machineOptions(Authentication authentication) {
        return lookupService.listActive(LookupCategory.MACHINE);
    }

    @ModelAttribute("plateOptions")
    public Object plateOptions() {
        return lookupService.listActive(LookupCategory.PLATE);
    }

    @ModelAttribute("ctpOptions")
    public Object ctpOptions() {
        return lookupService.listActive(LookupCategory.CTP);
    }

    @ModelAttribute("defectCodeOptions")
    public List<LookupOption> defectCodeOptions() {
        return lookupService.listActive(LookupCategory.DEFECT_CODE);
    }

    @ModelAttribute("operatorOptions")
    public List<LookupOption> operatorOptions() {
        return lookupService.listAll(LookupCategory.OPERATOR);
    }

    @GetMapping
    public String list() {
        return "redirect:/historico-ocorrencias";
    }

    @GetMapping("/novo")
    public String newForm(Model model, Authentication authentication) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode cadastrar ocorrências.");
        }
        if (!model.containsAttribute("form")) {
            PlateDefectForm form = new PlateDefectForm();
            form.setEntryDate(java.time.LocalDate.now());
            model.addAttribute("form", form);
        }
        model.addAttribute("formAction", "/controle_chapa/ocorrencias/novo");
        model.addAttribute("formTitle", "Nova ocorrência");
        model.addAttribute("submitLabel", "Salvar ocorrência");
        model.addAttribute("isEditMode", false);
        model.addAttribute("cancelUrl", "/controle_chapa/historico-ocorrencias");
        model.addAttribute("pageTitle", "Nova ocorrência");
        model.addAttribute("activeMenu", "new-defect");
        return "plates/defects/form";
    }

    @PostMapping("/novo")
    public String create(
            @Valid @ModelAttribute("form") PlateDefectForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode cadastrar ocorrências.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/controle_chapa/ocorrencias/novo");
            model.addAttribute("formTitle", "Nova ocorrência");
            model.addAttribute("submitLabel", "Salvar ocorrência");
            model.addAttribute("isEditMode", false);
            model.addAttribute("cancelUrl", "/controle_chapa/historico-ocorrencias");
            model.addAttribute("pageTitle", "Nova ocorrência");
            model.addAttribute("activeMenu", "new-defect");
            return "plates/defects/form";
        }
        plateDefectService.create(form, currentUser);
        redirectAttributes.addFlashAttribute("success", "Ocorrência cadastrada com sucesso.");
        return "redirect:/historico-ocorrencias";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        PlateDefectEntry entry = plateDefectService.getAccessibleById(id, currentUser);
        model.addAttribute("entry", entry);
        model.addAttribute("defectFullLabel", resolveDefectFullLabel(entry.getDefectCode()));
        model.addAttribute("canEdit", !isVisualizador(authentication));
        model.addAttribute("pageTitle", "Detalhes da ocorrência");
        model.addAttribute("activeMenu", "occurrence-history");
        return "plates/defects/details";
    }

    @GetMapping("/{id}/editar")
    public String editForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode editar ocorrências.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        PlateDefectEntry entry = plateDefectService.getAccessibleById(id, currentUser);
        if (!model.containsAttribute("form")) {
            PlateDefectForm form = toForm(entry);
            model.addAttribute("form", form);
        }
        model.addAttribute("formAction", "/controle_chapa/ocorrencias/" + id + "/editar");
        model.addAttribute("formTitle", "Editar ocorrência");
        model.addAttribute("submitLabel", "Salvar alterações");
        model.addAttribute("isEditMode", true);
        model.addAttribute("cancelUrl", "/controle_chapa/ocorrencias/" + id);
        model.addAttribute("pageTitle", "Editar ocorrência");
        model.addAttribute("activeMenu", "occurrence-history");
        return "plates/defects/form";
    }

    @PostMapping("/{id}/editar")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") PlateDefectForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode editar ocorrências.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        plateDefectService.getAccessibleById(id, currentUser);
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/controle_chapa/ocorrencias/" + id + "/editar");
            model.addAttribute("formTitle", "Editar ocorrência");
            model.addAttribute("submitLabel", "Salvar alterações");
            model.addAttribute("isEditMode", true);
            model.addAttribute("cancelUrl", "/controle_chapa/ocorrencias/" + id);
            model.addAttribute("pageTitle", "Editar ocorrência");
            model.addAttribute("activeMenu", "occurrence-history");
            return "plates/defects/form";
        }
        plateDefectService.update(id, form, currentUser);
        redirectAttributes.addFlashAttribute("success", "Ocorrência atualizada com sucesso.");
        return "redirect:/ocorrencias/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String delete(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode excluir ocorrências.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        plateDefectService.delete(id, currentUser);
        redirectAttributes.addFlashAttribute("success", "Ocorrência excluída com sucesso.");
        return "redirect:/historico-ocorrencias";
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

    private String resolveDefectFullLabel(String defectNumber) {
        if (defectNumber == null || defectNumber.isBlank()) return null;
        String prefix = defectNumber.trim() + " - ";
        return lookupService.listActive(LookupCategory.DEFECT_CODE).stream()
                .filter(opt -> opt.getLabel() != null && opt.getLabel().startsWith(prefix))
                .map(LookupOption::getLabel)
                .findFirst()
                .orElse(defectNumber);
    }

    private Map<String, String> buildDefectFullLabelMap() {
        Map<String, String> map = new HashMap<>();
        for (LookupOption opt : lookupService.listActive(LookupCategory.DEFECT_CODE)) {
            if (opt.getLabel() == null) continue;
            if (opt.getLabel().contains(" - ")) {
                String[] parts = opt.getLabel().split(" - ", 2);
                map.put(parts[0].trim(), opt.getLabel());
            } else {
                map.put(opt.getLabel().trim(), opt.getLabel());
            }
        }
        return map;
    }

    private Long resolveDefectCodeId(String defectNumber) {
        if (defectNumber == null || defectNumber.isBlank()) return null;
        String prefix = defectNumber.trim() + " - ";
        return lookupService.listActive(LookupCategory.DEFECT_CODE).stream()
                .filter(opt -> opt.getLabel() != null && opt.getLabel().startsWith(prefix))
                .map(LookupOption::getId)
                .findFirst()
                .orElse(null);
    }

    private PlateDefectForm toForm(PlateDefectEntry entry) {
        PlateDefectForm form = new PlateDefectForm();
        form.setEntryDate(entry.getEntryDate());
        form.setShiftId(entry.getShift() != null ? entry.getShift().getId() : null);
        form.setOpCode(entry.getOpCode());
        form.setClientName(entry.getClientName());
        form.setLossQuantity(entry.getLossQuantity());
        form.setDefectCodeId(resolveDefectCodeId(entry.getDefectCode()));
        form.setMachineId(entry.getMachine() != null ? entry.getMachine().getId() : null);
        form.setOperatorId(lookupService.findByCategoryAndLabel(LookupCategory.OPERATOR, entry.getOperatorName()).map(LookupOption::getId).orElse(null));
        form.setPlateId(lookupService.findByCategoryAndLabel(LookupCategory.PLATE, entry.getPlate()).map(LookupOption::getId).orElse(null));
        form.setCtpId(lookupService.findByCategoryAndLabel(LookupCategory.CTP, entry.getCtp()).map(LookupOption::getId).orElse(null));
        form.setSector(entry.getSector());
        form.setObservations(entry.getObservations());
        return form;
    }
}
