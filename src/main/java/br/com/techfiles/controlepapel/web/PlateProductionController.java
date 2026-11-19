package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.model.PlateProductionEntry;
import br.com.techfiles.controlepapel.service.AppUserService;
import br.com.techfiles.controlepapel.service.LookupService;
import br.com.techfiles.controlepapel.service.PlateProductionService;
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

import java.util.List;

@Controller
@RequestMapping("/chapas")
public class PlateProductionController {

    private final PlateProductionService plateProductionService;
    private final LookupService lookupService;
    private final AppUserService appUserService;

    public PlateProductionController(
            PlateProductionService plateProductionService,
            LookupService lookupService,
            AppUserService appUserService
    ) {
        this.plateProductionService = plateProductionService;
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

    @ModelAttribute("operatorOptions")
    public List<LookupOption> operatorOptions() {
        return lookupService.listAll(LookupCategory.OPERATOR);
    }

    @GetMapping
    public String list() {
        return "redirect:/lancamentos";
    }

    @GetMapping("/novo")
    public String newForm(Model model, Authentication authentication) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode cadastrar lançamentos.");
        }
        if (!model.containsAttribute("form")) {
            PlateProductionForm form = new PlateProductionForm();
            form.setEntryDate(java.time.LocalDate.now());
            model.addAttribute("form", form);
        }
        model.addAttribute("formAction", "/controle_chapa/chapas/novo");
        model.addAttribute("formTitle", "Novo lançamento");
        model.addAttribute("submitLabel", "Salvar lançamento");
        model.addAttribute("isEditMode", false);
        model.addAttribute("cancelUrl", "/controle_chapa/lancamentos");
        model.addAttribute("canChooseMachine", true);
        model.addAttribute("pageTitle", "Novo lançamento");
        model.addAttribute("activeMenu", "new-entry");
        return "plates/production/form";
    }

    @PostMapping("/novo")
    public String create(
            @Valid @ModelAttribute("form") PlateProductionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode cadastrar lançamentos.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/controle_chapa/chapas/novo");
            model.addAttribute("formTitle", "Novo lançamento");
            model.addAttribute("submitLabel", "Salvar lançamento");
            model.addAttribute("isEditMode", false);
            model.addAttribute("cancelUrl", "/controle_chapa/lancamentos");
            model.addAttribute("canChooseMachine", true);
            model.addAttribute("pageTitle", "Novo lançamento");
            model.addAttribute("activeMenu", "new-entry");
            return "plates/production/form";
        }
        plateProductionService.create(form, currentUser);
        redirectAttributes.addFlashAttribute("success", "Lançamento cadastrado com sucesso.");
        return "redirect:/lancamentos";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        PlateProductionEntry entry = plateProductionService.getAccessibleById(id, currentUser);
        model.addAttribute("entry", entry);
        model.addAttribute("canEdit", !isVisualizador(authentication));
        model.addAttribute("pageTitle", "Detalhes do lançamento");
        model.addAttribute("activeMenu", "entries");
        return "plates/production/details";
    }

    @GetMapping("/{id}/editar")
    public String editForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode editar lançamentos.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        PlateProductionEntry entry = plateProductionService.getAccessibleById(id, currentUser);
        if (!model.containsAttribute("form")) {
            PlateProductionForm form = toForm(entry);
            model.addAttribute("form", form);
        }
        model.addAttribute("formAction", "/controle_chapa/chapas/" + id + "/editar");
        model.addAttribute("formTitle", "Editar lançamento");
        model.addAttribute("submitLabel", "Salvar alterações");
        model.addAttribute("isEditMode", true);
        model.addAttribute("cancelUrl", "/controle_chapa/chapas/" + id);
        model.addAttribute("canChooseMachine", true);
        model.addAttribute("pageTitle", "Editar lançamento");
        model.addAttribute("activeMenu", "entries");
        return "plates/production/form";
    }

    @PostMapping("/{id}/editar")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") PlateProductionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode editar lançamentos.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        plateProductionService.getAccessibleById(id, currentUser);
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/controle_chapa/chapas/" + id + "/editar");
            model.addAttribute("formTitle", "Editar lançamento");
            model.addAttribute("submitLabel", "Salvar alterações");
            model.addAttribute("isEditMode", true);
            model.addAttribute("cancelUrl", "/controle_chapa/chapas/" + id);
            model.addAttribute("canChooseMachine", true);
            model.addAttribute("pageTitle", "Editar lançamento");
            model.addAttribute("activeMenu", "entries");
            return "plates/production/form";
        }
        plateProductionService.update(id, form, currentUser);
        redirectAttributes.addFlashAttribute("success", "Lançamento atualizado com sucesso.");
        return "redirect:/chapas/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String delete(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (isVisualizador(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário visualizador não pode excluir lançamentos.");
        }
        AppUser currentUser = appUserService.getRequiredCurrentUser(authentication);
        plateProductionService.delete(id, currentUser);
        redirectAttributes.addFlashAttribute("success", "Lançamento excluído com sucesso.");
        return "redirect:/lancamentos";
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

    private PlateProductionForm toForm(PlateProductionEntry entry) {
        PlateProductionForm form = new PlateProductionForm();
        form.setEntryDate(entry.getEntryDate());
        form.setShiftId(entry.getShift() != null ? entry.getShift().getId() : null);
        form.setOpCode(entry.getOpCode());
        form.setClientName(entry.getClientName());
        form.setQuantity(entry.getQuantity());
        form.setMachineId(entry.getMachine() != null ? entry.getMachine().getId() : null);
        form.setOperatorId(lookupService.findByCategoryAndLabel(LookupCategory.OPERATOR, entry.getOperatorName()).map(LookupOption::getId).orElse(null));
        form.setPlateId(lookupService.findByCategoryAndLabel(LookupCategory.PLATE, entry.getPlate()).map(LookupOption::getId).orElse(null));
        form.setCtpId(lookupService.findByCategoryAndLabel(LookupCategory.CTP, entry.getCtp()).map(LookupOption::getId).orElse(null));
        form.setObservations(entry.getObservations());
        return form;
    }
}
