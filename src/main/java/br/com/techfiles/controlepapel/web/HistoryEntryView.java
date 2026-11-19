package br.com.techfiles.controlepapel.web;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HistoryEntryView {

    private final String entryKindLabel;
    private final LocalDate entryDate;
    private final LocalDateTime createdAt;
    private final String shiftLabel;
    private final String machineLabel;
    private final String opCode;
    private final String clientName;
    private final Integer quantity;
    private final String operatorName;
    private final String plate;
    private final String ctp;
    private final Integer lossQuantity;
    private final String defectCode;
    private final String defectDescription;
    private final String sector;
    private final String openUrl;
    private final String observations;

    public HistoryEntryView(
            String entryKindLabel,
            LocalDate entryDate,
            LocalDateTime createdAt,
            String shiftLabel,
            String machineLabel,
            String opCode,
            String clientName,
            Integer quantity,
            String operatorName,
            String plate,
            String ctp,
            Integer lossQuantity,
            String defectCode,
            String sector,
            String openUrl
    ) {
        this(entryKindLabel, entryDate, createdAt, shiftLabel, machineLabel, opCode, clientName,
                quantity, operatorName, plate, ctp, lossQuantity, defectCode, null, sector, openUrl, null);
    }

    public HistoryEntryView(
            String entryKindLabel,
            LocalDate entryDate,
            LocalDateTime createdAt,
            String shiftLabel,
            String machineLabel,
            String opCode,
            String clientName,
            Integer quantity,
            String operatorName,
            String plate,
            String ctp,
            Integer lossQuantity,
            String defectCode,
            String defectDescription,
            String sector,
            String openUrl
    ) {
        this(entryKindLabel, entryDate, createdAt, shiftLabel, machineLabel, opCode, clientName,
                quantity, operatorName, plate, ctp, lossQuantity, defectCode, defectDescription, sector, openUrl, null);
    }

    public HistoryEntryView(
            String entryKindLabel,
            LocalDate entryDate,
            LocalDateTime createdAt,
            String shiftLabel,
            String machineLabel,
            String opCode,
            String clientName,
            Integer quantity,
            String operatorName,
            String plate,
            String ctp,
            Integer lossQuantity,
            String defectCode,
            String defectDescription,
            String sector,
            String openUrl,
            String observations
    ) {
        this.entryKindLabel = entryKindLabel;
        this.entryDate = entryDate;
        this.createdAt = createdAt;
        this.shiftLabel = shiftLabel;
        this.machineLabel = machineLabel;
        this.opCode = opCode;
        this.clientName = clientName;
        this.quantity = quantity;
        this.operatorName = operatorName;
        this.plate = plate;
        this.ctp = ctp;
        this.lossQuantity = lossQuantity;
        this.defectCode = defectCode;
        this.defectDescription = defectDescription;
        this.sector = sector;
        this.openUrl = openUrl;
        this.observations = observations;
    }

    public String getEntryKindLabel() {
        return entryKindLabel;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public String getMachineLabel() {
        return machineLabel;
    }

    public String getOpCode() {
        return opCode;
    }

    public String getClientName() {
        return clientName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getPlate() {
        return plate;
    }

    public String getCtp() {
        return ctp;
    }

    public Integer getLossQuantity() {
        return lossQuantity;
    }

    public String getDefectCode() {
        return defectCode;
    }

    public String getDefectDescription() {
        return defectDescription;
    }

    public String getSector() {
        return sector;
    }

    public String getOpenUrl() {
        return openUrl;
    }

    public String getObservations() {
        return observations;
    }
}
