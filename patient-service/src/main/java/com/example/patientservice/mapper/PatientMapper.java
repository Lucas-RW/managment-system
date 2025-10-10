package com.example.patientservice.mapper;

import com.example.patientservice.dto.PatientReponseDTO;
import com.example.patientservice.dto.PatientRequestDTO;
import com.example.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

    public static PatientReponseDTO toDTO(Patient patient) {

        PatientReponseDTO patientDTO = new PatientReponseDTO();
        patientDTO.setId(patient.getId().toString());
        patientDTO.setName(patient.getName());
        patientDTO.setAddress(patient.getAddress());
        patientDTO.setEmail(patient.getEmail());
        patientDTO.setDateOfBirth(patient.getDateOfBirth().toString());

        return patientDTO;

    }
    public static Patient toModel(PatientRequestDTO patientRequestDTO) {

        Patient patient = new Patient();
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setRegisteredDate(LocalDate.parse(patientRequestDTO.getRegisteredDate()));
        return patient;

    }

}
