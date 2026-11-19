package br.com.techfiles.controlepapel.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PlateDefectForm {

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate entryDate;

    @NotBlank(message = "Informe a OP.")
    private String opCode;

    @NotBlank(message = "Informe o cliente.")
    private String clientName;

    @NotNull(message = "Informe a perca.")
    @Min(value = 0, message = "A perca deve ser maior ou igual a zero.")
    private Integer lossQuantity;

    @NotNull(message = "Selecione o codigo de ocorrencia.")
    private Long defectCodeId;

    @NotNull(message = "Selecione a maquina.")
    private Long machineId;

    @NotNull(message = "Selecione o operador.")
    private Long operatorId;

    @NotNull(message = "Selecione o turno.")
    private Long shiftId;

    @NotNull(message = "Selecione a chapa.")
    private Long plateId;

    @NotNull(message = "Selecione o CTP.")
    private Long ctpId;

    @NotBlank(message = "Informe o setor.")
    private String sector;

    private String observations;

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getOpCode() {
        return opCode;
    }

    public void setOpCode(String opCode) {
        this.opCode = opCode;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Integer getLossQuantity() {
        return lossQuantity;
    }

    public void setLossQuantity(Integer lossQuantity) {
        this.lossQuantity = lossQuantity;
    }

    public Long getDefectCodeId() {
        return defectCodeId;
    }

    public void setDefectCodeId(Long defectCodeId) {
        this.defectCodeId = defectCodeId;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Long getPlateId() {
        return plateId;
    }

    public void setPlateId(Long plateId) {
        this.plateId = plateId;
    }

    public Long getCtpId() {
        return ctpId;
    }

    public void setCtpId(Long ctpId) {
        this.ctpId = ctpId;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
