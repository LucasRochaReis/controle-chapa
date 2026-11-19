package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.AppUserRole;
import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.model.PlateProductionEntry;
import br.com.techfiles.controlepapel.repo.LookupOptionRepository;
import br.com.techfiles.controlepapel.repo.PlateProductionEntryRepository;
import br.com.techfiles.controlepapel.web.PlateProductionForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PlateProductionService {

    private final PlateProductionEntryRepository plateProductionEntryRepository;
    private final LookupOptionRepository lookupOptionRepository;
    private final ClienteService clienteService;

    public PlateProductionService(
            PlateProductionEntryRepository plateProductionEntryRepository,
            LookupOptionRepository lookupOptionRepository,
            ClienteService clienteService
    ) {
        this.plateProductionEntryRepository = plateProductionEntryRepository;
        this.lookupOptionRepository = lookupOptionRepository;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<PlateProductionEntry> search(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            String opCode,
            String clientName
    ) {
        return plateProductionEntryRepository.findAll(
                buildSpecification(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, clientName),
                Sort.by(
                        Sort.Order.desc("entryDate"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<PlateProductionEntry> searchPaginated(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            String opCode,
            String clientName,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(
                        Sort.Order.desc("entryDate"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        return plateProductionEntryRepository.findAll(
                buildSpecification(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, clientName),
                pageable
        );
    }

    @Transactional
    public void create(PlateProductionForm form, AppUser currentUser) {
        PlateProductionEntry entry = new PlateProductionEntry();
        applyForm(entry, form, currentUser);
        entry.setCreatedBy(currentUser.getUsername());
        plateProductionEntryRepository.save(entry);
    }

    @Transactional
    public void createByLabels(
            LocalDate entryDate,
            String opCode,
            String clientName,
            Integer quantity,
            String shiftLabel,
            String machineLabel,
            String operatorLabel,
            String plateLabel,
            String ctpLabel,
            String observations,
            AppUser currentUser
    ) {
        LookupOption shift = obterOuCriarLookup(LookupCategory.SHIFT, shiftLabel);
        LookupOption machine = obterOuCriarLookup(LookupCategory.MACHINE, machineLabel);
        LookupOption plate = obterOuCriarLookup(LookupCategory.PLATE, plateLabel);
        LookupOption ctp = obterOuCriarLookup(LookupCategory.CTP, ctpLabel);
        LookupOption operator = obterOuCriarLookup(LookupCategory.OPERATOR, operatorLabel);

        if (entryDate == null) throw new IllegalArgumentException("Data nao informada.");
        if (opCode == null || opCode.trim().isBlank()) throw new IllegalArgumentException("OP nao informada.");
        if (clientName == null || clientName.trim().isBlank()) throw new IllegalArgumentException("Cliente nao informado.");
        if (quantity == null || quantity < 0) throw new IllegalArgumentException("Quantidade invalida.");

        PlateProductionEntry entry = new PlateProductionEntry();
        entry.setEntryDate(entryDate);
        entry.setOpCode(opCode.trim());
        entry.setClientName(clienteService.garantirCliente(clientName));
        entry.setQuantity(quantity);
        entry.setMachine(machine);
        entry.setOperatorName(operator.getLabel() != null ? operator.getLabel().trim() : "");
        entry.setShift(shift);
        entry.setPlate(plate.getLabel());
        entry.setCtp(ctp.getLabel());
        entry.setObservations(normalize(observations));
        entry.setCreatedBy(currentUser.getUsername());
        plateProductionEntryRepository.save(entry);
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizarLabel(String value) {
        String s = trimOrEmpty(value);
        if (s.isEmpty()) return "";
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.toUpperCase();
        s = s.replace("_", "-");
        s = s.replace("–", "-").replace("—", "-");
        s = s.replaceAll("\\s+", " ");
        s = s.replaceAll("[^A-Z0-9\\- /]", "");
        s = s.trim();
        return s;
    }

    private LookupOption obterOuCriarLookup(LookupCategory category, String value) {
        String original = trimOrEmpty(value);
        if (original.isEmpty()) {
            throw new IllegalArgumentException("Valor vazio para categoria " + category.name() + ".");
        }

        java.util.Optional<LookupOption> existente = resolveLookupFlexivel(category, original);
        if (existente.isPresent()) {
            return existente.get();
        }

        String novoLabel = normalizarLabel(original);
        if (novoLabel.isEmpty()) {
            throw new IllegalArgumentException("Valor invalido para categoria " + category.name() + ": " + original);
        }

        java.util.Optional<LookupOption> aposNormalizacao = resolveLookupFlexivel(category, novoLabel);
        if (aposNormalizacao.isPresent()) {
            return aposNormalizacao.get();
        }

        LookupOption novo = new LookupOption();
        novo.setCategory(category);
        novo.setLabel(novoLabel);
        novo.setActive(true);

        int maxSort = lookupOptionRepository.findByCategory(category).stream()
                .mapToInt(LookupOption::getSortOrder)
                .max()
                .orElse(0);
        novo.setSortOrder(maxSort + 10);

        return lookupOptionRepository.save(novo);
    }

    private java.util.Optional<LookupOption> resolveLookupFlexivel(LookupCategory category, String value) {
        String original = trimOrEmpty(value);
        if (original.isEmpty()) return java.util.Optional.empty();

        java.util.Optional<LookupOption> exact = lookupOptionRepository
                .findByCategoryAndLabelIgnoreCase(category, original)
                .filter(LookupOption::isActive);
        if (exact.isPresent()) return exact;

        String normalized = normalizarLabel(original);
        if (!normalized.isEmpty() && !normalized.equalsIgnoreCase(original)) {
            java.util.Optional<LookupOption> normalizedExact = lookupOptionRepository
                    .findByCategoryAndLabelIgnoreCase(category, normalized)
                    .filter(LookupOption::isActive);
            if (normalizedExact.isPresent()) return normalizedExact;
        }

        String simplified = original
                .replaceAll("[°ºª]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (!simplified.equals(original)) {
            java.util.Optional<LookupOption> simplifiedExact = lookupOptionRepository
                    .findByCategoryAndLabelIgnoreCase(category, simplified)
                    .filter(LookupOption::isActive);
            if (simplifiedExact.isPresent()) return simplifiedExact;
        }

        String digitsOnly = normalized.replaceAll("\\D+", "").trim();
        String noDigitsLower = normalized.replaceAll("\\d+", "").trim().toLowerCase();

        List<LookupOption> candidates = lookupOptionRepository.findByCategory(category).stream()
                .filter(LookupOption::isActive)
                .toList();

        for (LookupOption opt : candidates) {
            String label = opt.getLabel() == null ? "" : opt.getLabel().trim();
            String labelNormalizado = normalizarLabel(label);
            if (!normalized.isEmpty() && labelNormalizado.equalsIgnoreCase(normalized)) {
                return java.util.Optional.of(opt);
            }
            if (label.equalsIgnoreCase(original) || label.equalsIgnoreCase(simplified)) {
                return java.util.Optional.of(opt);
            }
        }

        if (!digitsOnly.isEmpty()) {
            for (LookupOption opt : candidates) {
                String label = opt.getLabel() == null ? "" : opt.getLabel().trim();
                String labelDigits = label.replaceAll("\\D+", "").trim();
                String labelWords = normalizarLabel(label).toLowerCase();
                if (labelDigits.equals(digitsOnly) &&
                        (noDigitsLower.isEmpty() || labelWords.contains(noDigitsLower))) {
                    return java.util.Optional.of(opt);
                }
                if (!noDigitsLower.isEmpty()) {
                    String simpleLower = simplified.toLowerCase();
                    String labelLower = label.toLowerCase();
                    if (labelLower.startsWith(simpleLower + " ") || labelLower.contains(" " + simpleLower)) {
                        return java.util.Optional.of(opt);
                    }
                }
                if (labelDigits.equals(digitsOnly)) {
                    return java.util.Optional.of(opt);
                }
            }
        }

        for (LookupOption opt : candidates) {
            String label = opt.getLabel() == null ? "" : normalizarLabel(opt.getLabel()).toLowerCase();
            String src = normalized.toLowerCase();
            if (!src.isEmpty() && (label.startsWith(src + " ") || label.endsWith(" " + src) || label.contains(" " + src + " "))) {
                return java.util.Optional.of(opt);
            }
        }

        return java.util.Optional.empty();
    }

    @Transactional(readOnly = true)
    public PlateProductionEntry getAccessibleById(Long id, AppUser currentUser) {
        PlateProductionEntry entry = plateProductionEntryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lancamento de chapa nao encontrado."));
        validateMachineAccess(entry, currentUser);
        return entry;
    }

    @Transactional
    public void update(Long id, PlateProductionForm form, AppUser currentUser) {
        PlateProductionEntry entry = getAccessibleById(id, currentUser);
        applyForm(entry, form, currentUser);
        plateProductionEntryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        PlateProductionEntry entry = getAccessibleById(id, currentUser);
        plateProductionEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public long countToday(AppUser currentUser) {
        return plateProductionEntryRepository.count(
                buildSpecification(currentUser, LocalDate.now(), LocalDate.now(), null, null, null, null)
        );
    }

    @Transactional(readOnly = true)
    public Long resolveSelectableMachineId(AppUser currentUser) {
        return null;
    }

    @Transactional(readOnly = true)
    public long count(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            String opCode,
            String clientName
    ) {
        return plateProductionEntryRepository.count(
                buildSpecification(currentUser, dateFrom, dateTo, shiftId, machineId, opCode, clientName)
        );
    }

    private void applyForm(PlateProductionEntry entry, PlateProductionForm form, AppUser currentUser) {
        LookupOption shift = lookupOptionRepository.findById(form.getShiftId())
                .filter(option -> option.getCategory() == LookupCategory.SHIFT && option.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Turno invalido."));
        LookupOption machine = lookupOptionRepository.findById(resolveMachineId(form, currentUser))
                .filter(option -> option.getCategory() == LookupCategory.MACHINE && option.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Maquina invalida."));
        LookupOption plate = lookupOptionRepository.findById(form.getPlateId())
                .filter(option -> option.getCategory() == LookupCategory.PLATE && option.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Chapa invalida."));
        LookupOption ctp = lookupOptionRepository.findById(form.getCtpId())
                .filter(option -> option.getCategory() == LookupCategory.CTP && option.isActive())
                .orElseThrow(() -> new IllegalArgumentException("CTP invalido."));
        LookupOption operator = lookupOptionRepository.findById(form.getOperatorId())
                .filter(option -> option.getCategory() == LookupCategory.OPERATOR && option.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Operador invalido."));

        entry.setEntryDate(form.getEntryDate());
        entry.setOpCode(form.getOpCode().trim());
        entry.setClientName(clienteService.garantirCliente(form.getClientName()));
        entry.setQuantity(form.getQuantity());
        entry.setMachine(machine);
        entry.setOperatorName(operator.getLabel() != null ? operator.getLabel().trim() : "");
        entry.setShift(shift);
        entry.setPlate(plate.getLabel());
        entry.setCtp(ctp.getLabel());
        entry.setObservations(normalize(form.getObservations()));
    }

    private Long resolveMachineId(PlateProductionForm form, AppUser currentUser) {
        Long restrictedMachineId = resolveSelectableMachineId(currentUser);
        return restrictedMachineId != null ? restrictedMachineId : form.getMachineId();
    }

    private Specification<PlateProductionEntry> buildSpecification(
            AppUser currentUser,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long shiftId,
            Long machineId,
            String opCode,
            String clientName
    ) {
        Specification<PlateProductionEntry> specification = Specification.where(null);

        if (dateFrom != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("entryDate"), dateFrom));
        }
        if (dateTo != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("entryDate"), dateTo));
        }
        if (shiftId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("shift").get("id"), shiftId));
        }
        Long effectiveMachineId = resolveSelectableMachineId(currentUser);
        if (effectiveMachineId == null) {
            effectiveMachineId = machineId;
        }
        if (effectiveMachineId != null) {
            Long finalMachineId = effectiveMachineId;
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("machine").get("id"), finalMachineId));
        }
        if (normalize(opCode) != null) {
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("opCode")), toLikePattern(opCode)));
        }
        if (normalize(clientName) != null) {
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("clientName")), toLikePattern(clientName)));
        }
        return specification;
    }

    private void validateMachineAccess(PlateProductionEntry entry, AppUser currentUser) {
        Long restrictedMachineId = resolveSelectableMachineId(currentUser);
        if (restrictedMachineId == null) {
            return;
        }
        if (entry.getMachine() == null || !restrictedMachineId.equals(entry.getMachine().getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Voce nao tem acesso a este lancamento.");
        }
    }

    private String toLikePattern(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
