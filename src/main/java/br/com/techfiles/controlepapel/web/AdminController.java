package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.AppUserRole;
import br.com.techfiles.controlepapel.model.Cliente;
import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.repo.AppUserRepository;
import br.com.techfiles.controlepapel.repo.ClienteRepository;
import br.com.techfiles.controlepapel.repo.LookupOptionRepository;
import br.com.techfiles.controlepapel.repo.PlateDefectEntryRepository;
import br.com.techfiles.controlepapel.repo.PlateProductionEntryRepository;
import br.com.techfiles.controlepapel.service.ClienteService;
import br.com.techfiles.controlepapel.service.LookupMergeService;
import br.com.techfiles.controlepapel.service.LookupService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AppUserRepository appUserRepository;
    private final LookupOptionRepository lookupOptionRepository;
    private final ClienteRepository clienteRepository;
    private final PlateProductionEntryRepository plateProductionEntryRepository;
    private final PlateDefectEntryRepository plateDefectEntryRepository;
    private final LookupService lookupService;
    private final ClienteService clienteService;
    private final LookupMergeService lookupMergeService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            AppUserRepository appUserRepository,
            LookupOptionRepository lookupOptionRepository,
            ClienteRepository clienteRepository,
            PlateProductionEntryRepository plateProductionEntryRepository,
            PlateDefectEntryRepository plateDefectEntryRepository,
            LookupService lookupService,
            ClienteService clienteService,
            LookupMergeService lookupMergeService,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.lookupOptionRepository = lookupOptionRepository;
        this.clienteRepository = clienteRepository;
        this.plateProductionEntryRepository = plateProductionEntryRepository;
        this.plateDefectEntryRepository = plateDefectEntryRepository;
        this.lookupService = lookupService;
        this.clienteService = clienteService;
        this.lookupMergeService = lookupMergeService;
        this.passwordEncoder = passwordEncoder;
    }

    @ModelAttribute("roleOptions")
    public AppUserRole[] roleOptions() {
        return AppUserRole.values();
    }

    @ModelAttribute("lookupCategories")
    public LookupCategory[] lookupCategories() {
        return Arrays.stream(LookupCategory.values()).toArray(LookupCategory[]::new);
    }

    @GetMapping("/usuarios")
    public String users(Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new AdminUserForm());
        }
        model.addAttribute("users", appUserRepository.findAllByOrderByNameAsc());
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("activeMenu", "admin-users");
        return "admin/users";
    }

    @PostMapping("/usuarios")
    public String createUser(
            @Valid @ModelAttribute("userForm") AdminUserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String normalizedUsername = normalize(form.getUsername());
        if (normalizedUsername != null && appUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            bindingResult.rejectValue("username", "duplicate", "Este username ja esta em uso.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", appUserRepository.findAllByOrderByNameAsc());
            model.addAttribute("pageTitle", "Usuarios");
            model.addAttribute("activeMenu", "admin-users");
            return "admin/users";
        }

        AppUser user = new AppUser();
        user.setName(form.getName().trim());
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setMachine(null);
        user.setEnabled(true);
        user.setMustChangePassword(form.isMustChangePassword());
        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Usuario cadastrado com sucesso.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/excluir")
    public String deleteUser(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        if (authentication != null && user.getUsername().equalsIgnoreCase(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Nao e permitido excluir o usuario logado.");
            return "redirect:/admin/usuarios";
        }

        appUserRepository.delete(user);
        redirectAttributes.addFlashAttribute("success", "Usuario excluido com sucesso.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/cadastros")
    public String options(
            @RequestParam(name = "clientePage", defaultValue = "0") Integer clientePage,
            @RequestParam(name = "clienteSize", defaultValue = "50") Integer clienteSize,
            @RequestParam(name = "clienteSearch", required = false) String clienteSearch,
            Model model
    ) {
        if (!model.containsAttribute("lookupForm")) {
            model.addAttribute("lookupForm", new LookupOptionForm());
        }
        if (!model.containsAttribute("clienteForm")) {
            model.addAttribute("clienteForm", new ClienteForm());
        }
        loadOptionLists(model, clientePage, clienteSize, clienteSearch);
        model.addAttribute("pageTitle", "Cadastros");
        model.addAttribute("activeMenu", "admin-options");
        return "admin/lookups";
    }

    @GetMapping("/cadastros/cliente/{id}/editar")
    public String editCliente(@PathVariable Long id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        ClienteForm form = new ClienteForm();
        form.setId(cliente.getId());
        form.setNome(cliente.getNome());
        model.addAttribute("clienteForm", form);
        if (!model.containsAttribute("lookupForm")) {
            model.addAttribute("lookupForm", new LookupOptionForm());
        }
        loadOptionLists(model, 0, 50, null);
        model.addAttribute("pageTitle", "Cadastros");
        model.addAttribute("activeMenu", "admin-options");
        return "admin/lookups";
    }
    @PostMapping("/cadastros/cliente")
    public String saveCliente(
            @Valid @ModelAttribute("clienteForm") ClienteForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String normalizado = clienteService.normalizar(form.getNome());
        if (normalizado == null || normalizado.isBlank()) {
            bindingResult.rejectValue("nome", "blank", "Informe o nome do cliente.");
        } else {
            Cliente duplicado = clienteRepository.findByNome(normalizado).orElse(null);
            if (duplicado != null && (form.getId() == null || !duplicado.getId().equals(form.getId()))) {
                bindingResult.rejectValue("nome", "duplicate", "Este cliente ja esta cadastrado.");
            }
        }

        if (bindingResult.hasErrors()) {
            if (!model.containsAttribute("lookupForm")) {
                model.addAttribute("lookupForm", new LookupOptionForm());
            }
            loadOptionLists(model, 0, 50, null);
            model.addAttribute("pageTitle", "Cadastros");
            model.addAttribute("activeMenu", "admin-options");
            return "admin/lookups";
        }

        Cliente cliente = form.getId() != null
                ? clienteRepository.findById(form.getId()).orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."))
                : new Cliente();
        cliente.setNome(normalizado);
        clienteRepository.save(cliente);

        redirectAttributes.addFlashAttribute("success", form.getId() == null
                ? "Cliente cadastrado com sucesso."
                : "Cliente atualizado com sucesso.");
        return "redirect:/admin/cadastros";
    }

    @PostMapping("/cadastros/cliente/{id}/excluir")
    public String deleteCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ClienteService.ExclusaoResultado resultado = clienteService.excluir(id);
        if (resultado.sucesso()) {
            redirectAttributes.addFlashAttribute("success", resultado.mensagem());
        } else {
            redirectAttributes.addFlashAttribute("error", resultado.mensagem());
        }
        return "redirect:/admin/cadastros";
    }

    @GetMapping("/cadastros/{id}/editar")
    public String editOption(@PathVariable Long id, Model model) {
        LookupOption option = lookupOptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro nao encontrado."));

        LookupOptionForm form = new LookupOptionForm();
        form.setId(option.getId());
        form.setCategory(option.getCategory());
        form.setLabel(option.getLabel());
        form.setActive(option.isActive());
        model.addAttribute("lookupForm", form);
        if (!model.containsAttribute("clienteForm")) {
            model.addAttribute("clienteForm", new ClienteForm());
        }
        loadOptionLists(model, 0, 50, null);
        model.addAttribute("pageTitle", "Cadastros");
        model.addAttribute("activeMenu", "admin-options");
        return "admin/lookups";
    }

    @PostMapping("/cadastros")    public String createOption(
            @Valid @ModelAttribute("lookupForm") LookupOptionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String normalizedLabel = normalize(form.getLabel());
        LookupOption existing = form.getId() != null
                ? lookupOptionRepository.findById(form.getId()).orElseThrow(() -> new IllegalArgumentException("Cadastro nao encontrado."))
                : null;

        if (form.getCategory() != null && normalizedLabel != null) {
            LookupOption duplicate = lookupOptionRepository.findByCategoryAndLabelIgnoreCase(form.getCategory(), normalizedLabel).orElse(null);
            if (duplicate != null && (existing == null || !duplicate.getId().equals(existing.getId()))) {
                bindingResult.rejectValue("label", "duplicate", "Este valor ja existe nesse cadastro.");
            }
        }

        if (bindingResult.hasErrors()) {
            loadOptionLists(model, 0, 50, null);
            model.addAttribute("pageTitle", "Cadastros");
            model.addAttribute("activeMenu", "admin-options");
            return "admin/lookups";
        }

        LookupOption option = existing != null ? existing : new LookupOption();
        LookupCategory previousCategory = option.getCategory();
        option.setCategory(form.getCategory());
        option.setLabel(normalizedLabel);
        option.setActive(form.isActive());
        if (existing == null) {
            option.setSortOrder(lookupService.listActive(form.getCategory()).size() + 1);
        } else if (previousCategory != form.getCategory()) {
            option.setSortOrder(lookupService.listActive(form.getCategory()).size() + 1);
        }
        lookupOptionRepository.save(option);

        redirectAttributes.addFlashAttribute("success", existing == null
                ? "Cadastro salvo com sucesso."
                : "Cadastro atualizado com sucesso.");
        return "redirect:/admin/cadastros";
    }

    @PostMapping("/cadastros/{id}/excluir")
    public String deleteOption(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LookupOption option = lookupOptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro nao encontrado."));

        String usageMessage = buildUsageMessage(option);
        if (usageMessage != null) {
            redirectAttributes.addFlashAttribute("error", usageMessage);
            return "redirect:/admin/cadastros";
        }

        try {
            lookupOptionRepository.delete(option);
            lookupOptionRepository.flush();
            redirectAttributes.addFlashAttribute("success", "Cadastro excluido com sucesso.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "Nao foi possivel excluir este cadastro porque ele ja esta em uso.");
        }
        return "redirect:/admin/cadastros";
    }

    @PostMapping("/cadastros/{id}/mesclar")
    public String mergeOption(
            @PathVariable Long id,
            @RequestParam("targetId") Long targetId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            LookupMergeService.MergeResult result = lookupMergeService.merge(id, targetId);
            StringBuilder sb = new StringBuilder();
            sb.append("Cadastro unido com sucesso: \"").append(result.sourceLabel)
                    .append("\" -> \"").append(result.targetLabel).append("\".");
            int total = result.total();
            if (total == 0) {
                sb.append(" Nenhum registro precisou ser atualizado.");
            } else {
                sb.append(" Registros atualizados: ").append(total).append(" (");
                boolean first = true;
                for (var e : result.summary().entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(e.getValue()).append(" ").append(e.getKey());
                    first = false;
                }
                sb.append(").");
            }
            redirectAttributes.addFlashAttribute("success", sb.toString());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/cadastros";
    }

    private String buildUsageMessage(LookupOption option) {
        if (option.getId() == null) {
            return null;
        }

        if (option.getCategory() == LookupCategory.MACHINE) {
            long plateProductionEntries = plateProductionEntryRepository.countByMachine_Id(option.getId());
            long plateDefectEntries = plateDefectEntryRepository.countByMachine_Id(option.getId());
            long users = appUserRepository.countByMachine_Id(option.getId());

            if (plateProductionEntries == 0 && plateDefectEntries == 0 && users == 0) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Não foi possível excluir a máquina porque ela está vinculada a ");
            boolean first = true;
            if (plateProductionEntries > 0) { sb.append(plateProductionEntries).append(" lançamento(s)"); first = false; }
            if (plateDefectEntries > 0) { if (!first) sb.append(", "); sb.append(plateDefectEntries).append(" lançamento(s) de ocorrência de chapa"); first = false; }
            if (users > 0) { if (!first) sb.append(", "); sb.append(users).append(" usuário(s)"); }
            sb.append(".");
            return sb.toString();
        }

        if (option.getCategory() == LookupCategory.SHIFT) {
            long plateProductionEntries = plateProductionEntryRepository.countByShift_Id(option.getId());
            long plateDefectEntries = plateDefectEntryRepository.countByShift_Id(option.getId());

            if (plateProductionEntries == 0 && plateDefectEntries == 0) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Não foi possível excluir o turno porque ele está vinculado a ");
            boolean first = true;
            if (plateProductionEntries > 0) { sb.append(plateProductionEntries).append(" lançamento(s)"); first = false; }
            if (plateDefectEntries > 0) { if (!first) sb.append(", "); sb.append(plateDefectEntries).append(" lançamento(s) de ocorrência de chapa"); }
            sb.append(".");
            return sb.toString();
        }

        return null;
    }

    private void loadOptionLists(Model model, Integer clientePage, Integer clienteSize, String clienteSearch) {
        List<LookupOption> allOptions = lookupOptionRepository.findAllByOrderByCategoryAscSortOrderAscLabelAsc();

        List<LookupGroup> optionGroups = new ArrayList<>();
        for (LookupCategory category : LookupCategory.values()) {
            List<LookupOption> options = allOptions.stream()
                    .filter(option -> option.getCategory() == category)
                    .toList();
            optionGroups.add(new LookupGroup(category, options));
        }
        model.addAttribute("optionGroups", optionGroups);

        int p = 0;
        int sz = 50;
        if (clientePage != null && clientePage >= 0) p = clientePage;
        if (clienteSize != null) {
            if (clienteSize < 1) sz = 1;
            else sz = Math.min(clienteSize, 500);
        }
        String search = clienteSearch != null ? clienteSearch.trim() : "";

        Pageable pageable = PageRequest.of(p, sz, Sort.by(Sort.Order.asc("nome").ignoreCase()));
        Page<Cliente> page;
        if (search.isEmpty()) {
            page = clienteRepository.findAll(pageable);
        } else {
            Specification<Cliente> spec = (root, q, cb) ->
                    cb.like(cb.lower(root.get("nome")), "%" + search.toLowerCase() + "%");
            page = clienteRepository.findAll(spec, pageable);
        }
        int totalPages = page.getTotalPages() > 0 ? page.getTotalPages() : 1;
        if (page.getNumber() >= totalPages) {
            pageable = PageRequest.of(totalPages - 1, sz, pageable.getSort());
            if (search.isEmpty()) page = clienteRepository.findAll(pageable);
            else {
                Specification<Cliente> spec = (root, q, cb) ->
                        cb.like(cb.lower(root.get("nome")), "%" + search.toLowerCase() + "%");
                page = clienteRepository.findAll(spec, pageable);
            }
        }

        model.addAttribute("allClientes", page.getContent());
        model.addAttribute("clienteSearch", search);
        model.addAttribute("clienteCurrentPage", page.getNumber());
        model.addAttribute("clientePageSize", page.getSize());
        model.addAttribute("clienteTotalElements", page.getTotalElements());
        model.addAttribute("clienteTotalPages", totalPages);
        model.addAttribute("clienteHasPrevious", page.hasPrevious());
        model.addAttribute("clienteHasNext", page.hasNext());
        model.addAttribute("clientePaginationWindow", buildPaginationWindow(page.getNumber(), totalPages));
    }

    private List<Integer> buildPaginationWindow(int currentPage, int totalPages) {
        List<Integer> out = new ArrayList<>();
        int window = 3;
        int start = Math.max(0, currentPage - window);
        int end = Math.min(totalPages - 1, currentPage + window);
        for (int i = start; i <= end; i++) {
            out.add(i);
        }
        return out;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
