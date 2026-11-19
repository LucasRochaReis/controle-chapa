package br.com.techfiles.controlepapel.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PlateProductionForm {

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate entryDate;

    @NotBlank(message = "Informe a OP.")
    private String opCode;

    @NotBlank(message = "Informe o cliente.")
    private String clientName;

    @NotNull(message = "Informe a quantidade.")
    @Min(value = 0, message = "A quantidade deve ser maior ou igual a zero.")
    private Integer quantity;

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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
